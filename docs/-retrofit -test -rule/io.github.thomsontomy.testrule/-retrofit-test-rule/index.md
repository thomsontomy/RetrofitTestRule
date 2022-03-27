---
title: RetrofitTestRule
---
//[Retrofit Test Rule](../../../index.html)/[io.github.thomsontomy.testrule](../index.html)
/[RetrofitTestRule](index.html)

# RetrofitTestRule

[jvm]\
class [RetrofitTestRule](index.html)&lt;[T](index.html)&gt;(
clazz: [Class](https://docs.oracle.com/javase/8/docs/api/java/lang/Class.html)&lt;[T](index.html)
&gt;, logLevel: [LogLevel](../-log-level/index.html),
converterFactories: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;Converter.Factory&gt
;,
callAdapterFactories: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;CallAdapter.Factory&gt;) :
TestRule

JUnit test rule for testing Retrofit services

#### Author

Thomson Thomas

## Parameters

jvm

| | |
|---|---|
| T | Retrofit service interface |

## Constructors

| | |
|---|---|
| [RetrofitTestRule](-retrofit-test-rule.html) | [jvm]<br>fun &lt;[T](index.html)&gt; [RetrofitTestRule](-retrofit-test-rule.html)(clazz: [Class](https://docs.oracle.com/javase/8/docs/api/java/lang/Class.html)&lt;[T](index.html)&gt;, logLevel: [LogLevel](../-log-level/index.html) = LogLevel.NONE, converterFactories: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;Converter.Factory&gt; = listOf(         GsonConverterFactory.create(             Gson()         )     ), callAdapterFactories: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;CallAdapter.Factory&gt; = emptyList())<br>Creates an instance of [RetrofitTestRule](index.html) |

## Functions

| Name | Summary |
|---|---|
| [apply](apply.html) | [jvm]<br>open override fun [apply](apply.html)(base: Statement, description: Description): Statement |
| [getService](get-service.html) | [jvm]<br>fun [getService](get-service.html)(): [T](index.html)<br>Returns the service instance which was passed in while creating [RetrofitTestRule](index.html). To be used inside the unit test to test the service. |

