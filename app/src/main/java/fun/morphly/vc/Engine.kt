package `fun`.morphly.vc

enum class Engine(val label: String) {
    XMAX("XMax · Plus"),
    VIDU("Vidu · Pro")
}

/** Contract for authenticated, short-lived sessions from Morphly's own backend.
 * There is deliberately no API key or fake provider integration in this alpha build.
 */
interface RealtimeProvider {
    val engine: Engine
    suspend fun connect(): Result<Unit>
    fun disconnect()
}
