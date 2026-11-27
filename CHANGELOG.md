Change Log
==========

Version 0.5.0 *(2026-11-27)*
----------------------------

A full rewrite of the rule. See the README for the complete API reference and the migration table.

* **Breaking:** `RequestValidations` is now `ExpectRequest`, `ResponseOptions` is now `RespondWith`,
  and `@RestRequest` takes `expect`/`respond` instead of `validations`/`responseOptions`
* **Breaking:** `method` is a `HttpMethod` enum, and the `"#"` magic null sentinel is gone
* **Breaking:** `RespondWith.bodyText` defaults to an empty body instead of `{}`
* Repeatable `@RestRequest` annotations for sequential request/response workflows
* Programmatic DSL: `enqueueResponse`, `enqueueResponseFromFile`, `enqueue`, `enqueueNetworkError`,
  `takeRequest`, `verifyRequest`, `assertAllRequestsConsumed`, `assertNoPendingResponses`
* Structural JSON comparison of request bodies, with a pluggable `BodyMatcher`
* Custom `OkHttpClient`, `Retrofit.Builder` and `(HttpUrl) -> T` service factories
* Response headers, status codes, body delays and socket level failure simulation
* Request bodies loaded from resources relative to the test class, or from the file system
* Failure messages include the full captured request; requests are reported as `RequestSnapshot`
  instead of OkHttp's `RecordedRequest`
* Request logging through a pluggable `TestLogger`
* JUnit 5 `RetrofitTestExtension`
* Fixed dependency scopes: JUnit, Retrofit, OkHttp and MockWebServer are now `api` dependencies
* Fixed the Kotlin/Java JVM target mismatch with an explicit toolchain (JVM 17)
* Fixed header values containing `:` being truncated
* Fixed query string validation being order dependent
* Fixed resource lookups resolving against the service interface instead of the test class
* Fixed the mock server only starting when an annotation was present
* Added a test suite for the library itself

Version 0.4.0 *(2022-07-16)*
----------------------------

* Update kotlin and other library versions

Version 0.3.0 *(2022-03-30)*
----------------------------

* Update annotation targets
* Fix the issue with validation default values

Version 0.2.0 *(2022-03-27)*
----------------------------

* Update documentation

Version 0.1.0 *(2022-03-20)*
----------------------------
Initial release