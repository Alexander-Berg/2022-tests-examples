package android.os

class Handler(private val looper: Looper) {
    fun post(r: Runnable): Boolean {
        r.run()
        return true
    }
}
