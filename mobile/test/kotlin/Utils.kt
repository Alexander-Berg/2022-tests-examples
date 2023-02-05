import java.io.File

fun getResourceFile(path: String): File = Thread.currentThread().contextClassLoader.getResource(path)?.let { File(it.path) }
    ?: throw IllegalStateException("Can't read $path file")
