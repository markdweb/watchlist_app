package com.cheese.watchlist.server

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.IOException

data class PcManagerState(
    val isRunning: Boolean = false,
    val url: String? = null,
    val error: String? = null
)

/**
 * Owns the lifecycle of the PC Manager local web server. A singleton so the
 * server (and its running/URL state) survives Compose navigation between
 * screens, while still being fully stoppable/disposable by the user.
 */
object WebServerManager {
    private const val PORT = 8080

    private var server: WatchlistWebServer? = null

    private val _state = MutableStateFlow(PcManagerState())
    val state: StateFlow<PcManagerState> = _state.asStateFlow()

    fun start(context: Context, passcodeEnabled: Boolean, passcode: String) {
        if (_state.value.isRunning) return

        if (!NetworkUtils.isWifiConnected(context)) {
            _state.value = PcManagerState(
                isRunning = false,
                error = "Connect your Android device to Wi-Fi to start PC Manager."
            )
            return
        }

        val ip = NetworkUtils.getWifiIpAddress(context)
        if (ip == null) {
            _state.value = PcManagerState(
                isRunning = false,
                error = "Couldn't detect your WiFi address. Try reconnecting to WiFi."
            )
            return
        }

        try {
            val newServer = WatchlistWebServer(context, PORT, passcodeEnabled, passcode)
            newServer.start(60_000, false)
            server = newServer
            _state.value = PcManagerState(
                isRunning = true,
                url = "http://$ip:$PORT",
                error = null
            )
        } catch (e: IOException) {
            _state.value = PcManagerState(
                isRunning = false,
                error = "Couldn't start the server (port may be busy). Try again."
            )
        }
    }

    fun stop() {
        server?.stop()
        server = null
        _state.value = PcManagerState(isRunning = false, url = null, error = null)
    }
}
