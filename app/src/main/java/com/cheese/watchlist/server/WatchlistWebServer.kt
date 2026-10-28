package com.cheese.watchlist.server

import android.content.Context
import com.cheese.watchlist.data.database.AppDatabase
import com.cheese.watchlist.data.entities.GenreEntity
import com.cheese.watchlist.data.entities.SourceEntity
import com.cheese.watchlist.data.entities.WatchItem
import com.cheese.watchlist.data.entities.WatchStatus
import com.cheese.watchlist.data.entities.WatchType
import fi.iki.elonen.NanoHTTPD
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject

/**
 * "PC Manager" — a tiny embedded web server so the watchlist can be viewed and
 * edited from a desktop browser on the SAME local WiFi network. Not exposed to
 * the internet, not cloud-hosted; it reads/writes straight through the app's
 * existing Room database via the DAOs, so Android and PC always see the same data.
 *
 * Session auth is a single in-memory token issued on successful passcode login —
 * appropriate for a one-user, same-network convenience feature, not a general
 * multi-user auth system.
 */
class WatchlistWebServer(
    context: Context,
    port: Int,
    private val passcodeEnabled: Boolean,
    private val passcode: String
) : NanoHTTPD(port) {

    private val db = AppDatabase.getInstance(context.applicationContext)
    private val watchItemDao = db.watchItemDao()
    private val genreDao = db.genreDao()
    private val sourceDao = db.sourceDao()
    private var sessionToken: String? = null

    override fun serve(session: IHTTPSession): Response {
        val uri = session.uri
        return try {
            when {
                uri == "/login" && session.method == Method.POST -> handleLogin(session)
                uri == "/logout" -> {
                    sessionToken = null
                    redirect("/")
                }
                uri == "/api/watchlist" && session.method == Method.GET -> requireAuth(session) { jsonArrayResponse(watchlistJson()) }
                uri == "/api/watchlist" && session.method == Method.POST -> requireAuth(session) { createItem(session) }
                uri == "/api/genres" && session.method == Method.GET -> requireAuth(session) { jsonArrayResponse(genresJson()) }
                uri == "/api/sources" && session.method == Method.GET -> requireAuth(session) { jsonArrayResponse(sourcesJson()) }
                uri.matches(Regex("/api/watchlist/\\d+/episode")) && session.method == Method.PUT ->
                    requireAuth(session) { adjustEpisode(session, extractId(uri, "/episode")) }
                uri.matches(Regex("/api/watchlist/\\d+/favorite")) && session.method == Method.PUT ->
                    requireAuth(session) { toggleFavorite(extractId(uri, "/favorite")) }
                uri.matches(Regex("/api/watchlist/\\d+")) && session.method == Method.PUT ->
                    requireAuth(session) { updateItem(session, extractId(uri, "")) }
                uri.matches(Regex("/api/watchlist/\\d+")) && session.method == Method.DELETE ->
                    requireAuth(session) { deleteItem(extractId(uri, "")) }
                else -> {
                    if (passcodeEnabled && !isAuthenticated(session)) {
                        htmlResponse(WebPages.loginPage(error = null))
                    } else {
                        htmlResponse(WebPages.dashboardPage())
                    }
                }
            }
        } catch (e: Exception) {
            jsonErrorResponse(Response.Status.INTERNAL_ERROR, "Server error: ${e.message}")
        }
    }

    // ---------- auth ----------

    private fun requireAuth(session: IHTTPSession, block: () -> Response): Response {
        if (passcodeEnabled && !isAuthenticated(session)) {
            return jsonErrorResponse(Response.Status.UNAUTHORIZED, "unauthorized")
        }
        return block()
    }

    private fun handleLogin(session: IHTTPSession): Response {
        val body = readBody(session)
        val entered = try { JSONObject(body).optString("passcode", "") } catch (e: Exception) { "" }

        if (entered.isBlank()) return jsonErrorResponse(Response.Status.BAD_REQUEST, "empty")
        if (entered != passcode) return jsonErrorResponse(Response.Status.UNAUTHORIZED, "wrong")

        val token = PasscodeGenerator.generateSessionToken()
        sessionToken = token
        val response = jsonResponse("{\"ok\":true}")
        response.addHeader("Set-Cookie", "watchlist_session=$token; Path=/")
        return response
    }

    private fun isAuthenticated(session: IHTTPSession): Boolean {
        if (!passcodeEnabled) return true
        val cookie = session.cookies?.read("watchlist_session")
        return cookie != null && cookie == sessionToken
    }

    // ---------- watchlist CRUD ----------

    private fun watchlistJson(): JSONArray = runBlocking {
        val items = watchItemDao.getAll().first()
        val arr = JSONArray()
        items.forEach { arr.put(itemToJson(it)) }
        arr
    }

    private fun genresJson(): JSONArray = runBlocking {
        val arr = JSONArray()
        genreDao.getAll().first().forEach { arr.put(it.name) }
        arr
    }

    private fun sourcesJson(): JSONArray = runBlocking {
        val arr = JSONArray()
        sourceDao.getAll().first().forEach { arr.put(it.name) }
        arr
    }

    private fun createItem(session: IHTTPSession): Response = runBlocking {
        val json = JSONObject(readBody(session))
        val item = jsonToItem(json, existing = null)
        if (item.title.isBlank()) return@runBlocking jsonErrorResponse(Response.Status.BAD_REQUEST, "Title is required")
        val newId = watchItemDao.insert(item)
        jsonResponse(itemToJson(item.copy(id = newId)).toString())
    }

    private fun updateItem(session: IHTTPSession, id: Long?): Response = runBlocking {
        if (id == null) return@runBlocking jsonErrorResponse(Response.Status.BAD_REQUEST, "invalid id")
        val existing = watchItemDao.getById(id) ?: return@runBlocking jsonErrorResponse(Response.Status.NOT_FOUND, "not found")
        val json = JSONObject(readBody(session))
        val updated = jsonToItem(json, existing = existing)
        if (updated.title.isBlank()) return@runBlocking jsonErrorResponse(Response.Status.BAD_REQUEST, "Title is required")
        watchItemDao.update(updated)
        jsonResponse(itemToJson(updated).toString())
    }

    private fun deleteItem(id: Long?): Response = runBlocking {
        if (id == null) return@runBlocking jsonErrorResponse(Response.Status.BAD_REQUEST, "invalid id")
        val existing = watchItemDao.getById(id) ?: return@runBlocking jsonErrorResponse(Response.Status.NOT_FOUND, "not found")
        watchItemDao.delete(existing)
        jsonResponse("{\"ok\":true}")
    }

    private fun toggleFavorite(id: Long?): Response = runBlocking {
        if (id == null) return@runBlocking jsonErrorResponse(Response.Status.BAD_REQUEST, "invalid id")
        val item = watchItemDao.getById(id) ?: return@runBlocking jsonErrorResponse(Response.Status.NOT_FOUND, "not found")
        val updated = item.copy(favorite = !item.favorite, updatedAt = System.currentTimeMillis())
        watchItemDao.update(updated)
        jsonResponse(itemToJson(updated).toString())
    }

    private fun adjustEpisode(session: IHTTPSession, id: Long?): Response = runBlocking {
        if (id == null) return@runBlocking jsonErrorResponse(Response.Status.BAD_REQUEST, "invalid id")
        val item = watchItemDao.getById(id) ?: return@runBlocking jsonErrorResponse(Response.Status.NOT_FOUND, "not found")
        val delta = try { JSONObject(readBody(session)).optInt("delta", 0) } catch (e: Exception) { 0 }

        val newEpisode = (item.currentEpisode + delta).coerceIn(0, item.totalEpisodes)
        var newStatus = item.status
        if (newEpisode == item.totalEpisodes && item.totalEpisodes > 0) {
            newStatus = WatchStatus.WATCHED
        } else if (newEpisode > 0 && item.status == WatchStatus.TO_WATCH) {
            newStatus = WatchStatus.WATCHING
        }
        val updated = item.copy(currentEpisode = newEpisode, status = newStatus, updatedAt = System.currentTimeMillis())
        watchItemDao.update(updated)
        jsonResponse(itemToJson(updated).toString())
    }

    // ---------- mapping helpers ----------

    private fun itemToJson(item: WatchItem): JSONObject = JSONObject().apply {
        put("id", item.id)
        put("title", item.title)
        put("status", item.status.name)
        put("statusLabel", item.status.label)
        put("type", item.type.name)
        put("genre", item.genre)
        put("whereToWatch", item.whereToWatch)
        put("url", item.url)
        put("rating", item.rating)
        put("favorite", item.favorite)
        put("comments", item.comments)
        put("yearDate", item.yearDate)
        put("season", item.season)
        put("totalEpisodes", item.totalEpisodes)
        put("currentEpisode", item.currentEpisode)
    }

    private fun jsonToItem(json: JSONObject, existing: WatchItem?): WatchItem {
        val type = try { WatchType.valueOf(json.optString("type", existing?.type?.name ?: "MOVIE")) } catch (e: Exception) { WatchType.MOVIE }
        val status = try { WatchStatus.valueOf(json.optString("status", existing?.status?.name ?: "TO_WATCH")) } catch (e: Exception) { WatchStatus.TO_WATCH }
        return WatchItem(
            id = existing?.id ?: 0L,
            title = json.optString("title", existing?.title ?: "").trim(),
            status = status,
            type = type,
            genre = json.optString("genre", existing?.genre ?: ""),
            whereToWatch = json.optString("whereToWatch", existing?.whereToWatch ?: ""),
            url = json.optString("url", existing?.url ?: "").trim(),
            rating = json.optInt("rating", existing?.rating ?: 0).coerceIn(0, 5),
            favorite = json.optBoolean("favorite", existing?.favorite ?: false),
            comments = json.optString("comments", existing?.comments ?: ""),
            yearDate = json.optString("yearDate", existing?.yearDate ?: ""),
            season = json.optInt("season", existing?.season ?: 1).coerceAtLeast(1),
            totalEpisodes = if (type == WatchType.SERIES) json.optInt("totalEpisodes", existing?.totalEpisodes ?: 0) else 0,
            currentEpisode = if (type == WatchType.SERIES) json.optInt("currentEpisode", existing?.currentEpisode ?: 0) else 0,
            createdAt = existing?.createdAt ?: System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
    }

    private fun extractId(uri: String, suffix: String): Long? {
        val withoutSuffix = if (suffix.isNotEmpty()) uri.removeSuffix(suffix) else uri
        return withoutSuffix.removePrefix("/api/watchlist/").toLongOrNull()
    }

    private fun readBody(session: IHTTPSession): String {
        val files = HashMap<String, String>()
        session.parseBody(files)
        return files["postData"] ?: ""
    }

    // ---------- response helpers ----------

    private fun redirect(location: String): Response {
        val response = newFixedLengthResponse(Response.Status.REDIRECT, "text/html", "")
        response.addHeader("Location", location)
        return response
    }

    private fun htmlResponse(html: String): Response =
        newFixedLengthResponse(Response.Status.OK, "text/html; charset=utf-8", html)

    private fun jsonResponse(json: String): Response =
        newFixedLengthResponse(Response.Status.OK, "application/json; charset=utf-8", json)

    private fun jsonArrayResponse(arr: JSONArray): Response =
        jsonResponse(arr.toString())

    private fun jsonErrorResponse(status: Response.Status, message: String): Response =
        newFixedLengthResponse(status, "application/json; charset=utf-8", "{\"error\":${JSONObject.quote(message)}}")
}
