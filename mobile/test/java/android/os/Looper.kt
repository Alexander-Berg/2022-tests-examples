package android.os

class Looper(private val quitAllowed: Boolean) {
    companion object {
        @JvmStatic
        fun getMainLooper(): Looper {
            return Looper(false)
        }
    }
}
