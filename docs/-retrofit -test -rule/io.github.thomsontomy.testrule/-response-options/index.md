---
title: ResponseOptions
---
//[Retrofit Test Rule](../../../index.html)/[io.github.thomsontomy.testrule](../index.html)
/[ResponseOptions](index.html)

# ResponseOptions

[jvm]\
annotation class [ResponseOptions](index.html)(
code: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html),
bodyText: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html),
bodyFile: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html))

Response configurations Child annotation class to be added
in [RestRequest](../-rest-request/index.html)

## Constructors

| | |
|---|---|
| [ResponseOptions](-response-options.html) | [jvm]<br>fun [ResponseOptions](-response-options.html)(code: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html) = 200, bodyText: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) = "{}", bodyFile: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) = "") |

## Properties

| Name | Summary |
|---|---|
| [bodyFile](body-file.html) | [jvm]<br>val [bodyFile](body-file.html): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)<br>Response data from resource file. File need to be in the same package inside the resources |
| [bodyText](body-text.html) | [jvm]<br>val [bodyText](body-text.html): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)<br>Response text. Default value is an empty json object string {} |
| [code](code.html) | [jvm]<br>val [code](code.html): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html) = 200<br>HTTP response code |

