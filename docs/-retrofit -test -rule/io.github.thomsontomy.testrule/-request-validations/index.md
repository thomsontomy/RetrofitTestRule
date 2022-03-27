---
title: RequestValidations
---
//[Retrofit Test Rule](../../../index.html)/[io.github.thomsontomy.testrule](../index.html)
/[RequestValidations](index.html)

# RequestValidations

[jvm]\
annotation class [RequestValidations](index.html)(
path: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html),
method: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html),
headers: [Array](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-array/index.html)&lt;Header&gt
;, bodyText: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html),
bodyFile: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html))

Validations to be done on the captured HTTP request Child annotation class to be added
in [RestRequest](../-rest-request/index.html)

## Constructors

| | |
|---|---|
| [RequestValidations](-request-validations.html) | [jvm]<br>fun [RequestValidations](-request-validations.html)(path: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) = "", method: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) = "", headers: [Array](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-array/index.html)&lt;Header&gt; = [], bodyText: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) = "", bodyFile: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) = "") |

## Properties

| Name | Summary |
|---|---|
| [bodyFile](body-file.html) | [jvm]<br>val [bodyFile](body-file.html): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)<br>Expected body text from resource file. File need to be in the same package inside the resources |
| [bodyText](body-text.html) | [jvm]<br>val [bodyText](body-text.html): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)<br>Expected body text |
| [headers](headers.html) | [jvm]<br>val [headers](headers.html): [Array](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-array/index.html)&lt;Header&gt;<br>Array of Retrofit Headers |
| [method](method.html) | [jvm]<br>val [method](method.html): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)<br>HTTP Request method |
| [path](path.html) | [jvm]<br>val [path](path.html): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)<br>Request path(from the base url) |

