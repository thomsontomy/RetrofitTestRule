package io.github.thomsontomy.testrule

/**
 * Amount of detail logged for every request captured by the rule.
 *
 * Failure messages always include the full request regardless of this setting, so logging is only
 * useful while writing or debugging a test.
 */
enum class LogLevel {

    /** Do not log anything. */
    NONE,

    /** Log the request line only. */
    BASIC,

    /** Log the request line and headers. */
    HEADERS,

    /** Log the request line, headers and body. */
    BODY,
}

/**
 * Sink used by [RetrofitTestRule] and [RetrofitTestExtension] for request logging.
 *
 * Defaults to [STDOUT]. Plug in a framework specific logger (for example one backed by
 * `java.util.logging` or SLF4J) when running in a shared CI environment:
 *
 * ```kotlin
 * val rule = RetrofitTestRule(
 *     GitHubService::class.java,
 *     logLevel = LogLevel.BODY,
 *     logger = TestLogger { message -> LOGGER.info(message) },
 * )
 * ```
 */
fun interface TestLogger {

    /** Writes [message] to the sink. */
    fun log(message: String)

    companion object {

        /** Writes to standard output. */
        @JvmField
        val STDOUT: TestLogger = TestLogger { message -> println(message) }

        /** Discards every message. */
        @JvmField
        val NONE: TestLogger = TestLogger { }
    }
}
