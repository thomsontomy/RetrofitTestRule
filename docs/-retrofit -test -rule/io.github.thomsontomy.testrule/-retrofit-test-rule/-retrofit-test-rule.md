---
title: RetrofitTestRule
---
//[Retrofit Test Rule](../../../index.html)/[io.github.thomsontomy.testrule](../index.html)
/[RetrofitTestRule](index.html)/[RetrofitTestRule](-retrofit-test-rule.html)

# RetrofitTestRule

[jvm]\
fun &lt;[T](index.html)&gt; [RetrofitTestRule](-retrofit-test-rule.html)(
clazz: [Class](https://docs.oracle.com/javase/8/docs/api/java/lang/Class.html)&lt;[T](index.html)
&gt;, logLevel: [LogLevel](../-log-level/index.html) = LogLevel.NONE,
converterFactories: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;Converter.Factory&gt
; = listOf(
GsonConverterFactory.create(
Gson()
)
),
callAdapterFactories: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;CallAdapter.Factory&gt
; = emptyList())

Creates an instance of [RetrofitTestRule](index.html)

## Parameters

jvm

| | |
|---|---|
| T | Retrofit service interface |




