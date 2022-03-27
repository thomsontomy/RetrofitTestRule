---
title: RestRequest
---
//[Retrofit Test Rule](../../../index.html)/[io.github.thomsontomy.testrule](../index.html)
/[RestRequest](index.html)

# RestRequest

[jvm]\
annotation class [RestRequest](index.html)(
validations: [RequestValidations](../-request-validations/index.html),
responseOptions: [ResponseOptions](../-response-options/index.html))

Top level annotation class for unit tests

## Constructors

| | |
|---|---|
| [RestRequest](-rest-request.html) | [jvm]<br>fun [RestRequest](-rest-request.html)(validations: [RequestValidations](../-request-validations/index.html) = RequestValidations(), responseOptions: [ResponseOptions](../-response-options/index.html) = ResponseOptions()) |

## Properties

| Name | Summary |
|---|---|
| [responseOptions](response-options.html) | [jvm]<br>val [responseOptions](response-options.html): [ResponseOptions](../-response-options/index.html)<br>Response configurations |
| [validations](validations.html) | [jvm]<br>val [validations](validations.html): [RequestValidations](../-request-validations/index.html)<br>Validations need to be done on the captured request |

