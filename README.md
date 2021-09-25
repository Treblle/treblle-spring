# Treblle for Spring Boot

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.treblle/treblle-spring-boot-starter/badge.svg?style=plastic)](https://maven-badges.herokuapp.com/maven-central/com.treblle/treblle-spring-boot-starter)
[![MIT Licence](https://img.shields.io/npm/l/treblle)](LICENSE.md)

Treblle makes it super easy to understand what’s going on with your APIs and the apps that use them. Just by adding Treblle to your API out of the box, you get:

- Real-time API monitoring and logging
- Auto-generated API docs with OAS support
- API analytics
- Quality scoring
- One-click testing
- API management on the go
- and more...

## Requirements

- Spring Boot 2.4.X
- Java 8+

## Installation

Add the following dependency:

```xml
<dependencies>
    <dependency>
        <groupId>com.treblle</groupId>
        <artifactId>treblle-spring-boot-starter</artifactId>
        <version>1.0.0</version>
    </dependency>
</dependencies>
```

## Getting started

Next, create a FREE account on <https://treblle.com> to get an API key and Project ID. After you have those simply initialize Treblle in your project like so:

Annotate the desired configuration with `@EnableTreblle`

```java
@EnableTreblle
@SpringBootApplication
public class MyApplication {
    ...
}
```

Configure the following properties:

```csv
treblle.apiKey=<API_KEY>
treblle.projectId=<PROJECT_ID>
```

That's it. Your API requests and responses are now being sent to your Treblle project. Now you get features like: auto-documentation, real-time request/response monitoring, error tracking and so much more.

### Running Treblle only for certain URL patterns

By default, Trebble applies its filter on the default URL pattern (/*). If you want to run Treblle only for certain URL patterns, you can define a list of patterns by using the `treblle.urlPatterns` configuration property like in the example below.

```csv
treblle.urlPatterns=/greeting,/goodbye
```

### Need to hide additional fields?

If you want to expand the list of fields you want to hide, you can pass field names you want to hide by using the `treblle.maskingKeywords` configuration property like in the example below.

```csv
treblle.maskingKeywords=secretField,highlySensitiveField
```

### Logging error bodies

The Spring default behavior is to handle errors via the `/error` endpoint. To allow Treblle to properly process your error response bodies you should switch to controller based exception handling, for example by using `@ControllerAdvice` and `@ExceptionHandler` ( https://spring.io/blog/2013/11/01/exception-handling-in-spring-mvc )


## Support

If you have problems of any kind feel free to reach out via <https://treblle.com> or email vedran@treblle.com and we'll do our best to help you out.

## License

Copyright 2021, Treblle Limited. Licensed under the MIT license:
http://www.opensource.org/licenses/mit-license.php
