package io.github.thomsontomy.testrule

import java.io.File
import java.io.IOException

/**
 * Resolves test resources used by request/response bodies.
 *
 * Lookup order:
 * 1. an existing file on disk (absolute, or relative to the working directory)
 * 2. a classpath resource relative to the test class package
 * 3. a classpath resource relative to the service interface package (kept for compatibility)
 * 4. a classpath resource at an absolute path, when the name starts with `/`
 *
 * Resolution failures raise an [AssertionError] that lists every attempted location, so a typo in
 * a file name is immediately obvious.
 */
internal class ResourceResolver(private val testClass: Class<*>?, private val serviceClass: Class<*>) {

    fun read(file: String): String {
        val attempts = mutableListOf<String>()

        val diskFile = File(file)
        attempts += "file system: ${diskFile.absolutePath}"
        if (diskFile.isFile) {
            return try {
                diskFile.readText()
            } catch (exception: IOException) {
                throw AssertionError("Could not read test resource file <${diskFile.absolutePath}>", exception)
            }
        }

        val classes = listOfNotNull(testClass, serviceClass).distinct()
        for (clazz in classes) {
            for (candidate in candidatesFor(file)) {
                attempts += "classpath: ${clazz.name} -> $candidate"
                val stream = clazz.getResourceAsStream(candidate) ?: continue
                return stream.use { it.readBytes().toString(Charsets.UTF_8) }
            }
        }

        // Last resort: the class loader itself, which also covers resources at the classpath root.
        val classLoader = classes.firstOrNull()?.classLoader
        for (candidate in candidatesFor(file)) {
            attempts += "classloader: ${candidate.removePrefix("/")}"
            val stream = classLoader?.getResourceAsStream(candidate.removePrefix("/")) ?: continue
            return stream.use { it.readBytes().toString(Charsets.UTF_8) }
        }

        throw AssertionError(
            buildString {
                append("Could not access test resource <").append(file).append(">.")
                append("\nTried:")
                attempts.forEach { append("\n  - ").append(it) }
                append("\n\nPlace the file in `src/test/resources` next to the test class, or pass an absolute path.")
            },
        )
    }

    private fun candidatesFor(file: String): List<String> =
        if (file.startsWith("/")) listOf(file) else listOf(file, "/$file")
}
