---
title: io.github.thomsontomy.testrule
---
//[Retrofit Test Rule](../../index.html)/[io.github.thomsontomy.testrule](index.html)

# Package io.github.thomsontomy.testrule

## Types

| Name | Summary |
|---|---|
| [LogLevel](-log-level/index.html) | [jvm]<br>enum [LogLevel](-log-level/index.html) : [Enum](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-enum/index.html)&lt;[LogLevel](-log-level/index.html)&gt; <br>Level of log output from the rule |
| [RequestValidations](-request-validations/index.html) | [jvm]<br>annotation class [RequestValidations](-request-validations/index.html)(path: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), method: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), headers: [Array](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-array/index.html)&lt;Header&gt;, bodyText: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), bodyFile: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html))<br>Validations to be done on the captured HTTP request Child annotation class to be added in [RestRequest](-rest-request/index.html) |
| [ResponseOptions](-response-options/index.html) | [jvm]<br>annotation class [ResponseOptions](-response-options/index.html)(code: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html), bodyText: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), bodyFile: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html))<br>Response configurations Child annotation class to be added in [RestRequest](-rest-request/index.html) |
| [RestRequest](-rest-request/index.html) | [jvm]<br>annotation class [RestRequest](-rest-request/index.html)(validations: [RequestValidations](-request-validations/index.html), responseOptions: [ResponseOptions](-response-options/index.html))<br>Top level annotation class for unit tests |
| [RetrofitTestRule](-retrofit-test-rule/index.html) | [jvm]<br>class [RetrofitTestRule](-retrofit-test-rule/index.html)&lt;[T](-retrofit-test-rule/index.html)&gt;(clazz: [Class](https://docs.oracle.com/javase/8/docs/api/java/lang/Class.html)&lt;[T](-retrofit-test-rule/index.html)&gt;, logLevel: [LogLevel](-log-level/index.html), converterFactories: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;Converter.Factory&gt;, callAdapterFactories: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;CallAdapter.Factory&gt;) : TestRule<br>JUnit test rule for testing Retrofit services |

