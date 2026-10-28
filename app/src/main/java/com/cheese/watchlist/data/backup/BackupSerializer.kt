package com.cheese.watchlist.data.backup

import com.cheese.watchlist.data.entities.WatchItem
import com.cheese.watchlist.data.entities.WatchStatus
import com.cheese.watchlist.data.entities.WatchType
import com.cheese.watchlist.data.entities.GenreEntity
import com.cheese.watchlist.data.entities.SourceEntity
import org.json.JSONArray
import org.json.JSONObject

data class BackupData(
    val items: List<WatchItem>,
    val genres: List<String>,
    val sources: List<String>
)

/** Serializes/deserializes the watchlist to a portable, human-readable JSON backup file. */
object BackupSerializer {

    fun export(items: List<WatchItem>, genres: List<GenreEntity>, sources: List<SourceEntity>): String {
        val root = JSONObject()
        root.put("version", 1)
        root.put("exportedAt", System.currentTimeMillis())

        val itemsArr = JSONArray()
        items.forEach { itemsArr.put(itemToJson(it)) }
        root.put("items", itemsArr)

        val genresArr = JSONArray()
        genres.forEach { genresArr.put(it.name) }
        root.put("genres", genresArr)

        val sourcesArr = JSONArray()
        sources.forEach { sourcesArr.put(it.name) }
        root.put("sources", sourcesArr)

        return root.toString(2)
    }

    fun import(json: String): BackupData {
        val root = JSONObject(json)

        val items = mutableListOf<WatchItem>()
        val itemsArr = root.optJSONArray("items") ?: JSONArray()
        for (i in 0 until itemsArr.length()) {
            items.add(jsonToItem(itemsArr.getJSONObject(i)))
        }

        val genres = mutableListOf<String>()
        val genresArr = root.optJSONArray("genres") ?: JSONArray()
        for (i in 0 until genresArr.length()) genres.add(genresArr.getString(i))

        val sources = mutableListOf<String>()
        val sourcesArr = root.optJSONArray("sources") ?: JSONArray()
        for (i in 0 until sourcesArr.length()) sources.add(sourcesArr.getString(i))

        return BackupData(items, genres, sources)
    }

    private fun itemToJson(item: WatchItem): JSONObject = JSONObject().apply {
        put("title", item.title)
        put("status", item.status.name)
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

    private fun jsonToItem(json: JSONObject): WatchItem {
        val type = try {
            WatchType.valueOf(json.optString("type", "MOVIE"))
        } catch (e: Exception) { WatchType.MOVIE }
        val status = try {
            WatchStatus.valueOf(json.optString("status", "TO_WATCH"))
        } catch (e: Exception) { WatchStatus.TO_WATCH }

        return WatchItem(
            title = json.optString("title", ""),
            status = status,
            type = type,
            genre = json.optString("genre", ""),
            whereToWatch = json.optString("whereToWatch", ""),
            url = json.optString("url", ""),
            rating = json.optInt("rating", 0).coerceIn(0, 5),
            favorite = json.optBoolean("favorite", false),
            comments = json.optString("comments", ""),
            yearDate = json.optString("yearDate", ""),
            season = json.optInt("season", 1).coerceAtLeast(1),
            totalEpisodes = json.optInt("totalEpisodes", 0),
            currentEpisode = json.optInt("currentEpisode", 0)
        )
    }
}
