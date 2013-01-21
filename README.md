UrlDroid
========

A super-lightweight HTTP client library for Android and the JVM, written in Java.

The library is basically a wrapper around `HttpURLConnection`, which is the API to make HTTP requests on
Android [officially recommended by Google](http://android-developers.blogspot.com/2011/09/androids-http-clients.html).
It provides convenient ways to craft GET, POST and other kinds of requests, and get responses back. It also integrates the GSON library for serializing/deserializing objects in JSON format.


## Features

- Encoding/decoding of query params, URL paths and request or response bodies
- Proxy support (HTTP and SOCKS)
- HTTPS and Basic Authorization support
- Sending and receiving data in JSON format
- Gzip compression and response caching


## Building

The project is in Eclipse format, but it is trivial to integrate in other build systems or IDEs, as UrlDroid consists of a single Java class.

The only dependencies are the [Google GSON library](https://code.google.com/p/google-gson/) and Douglas Crockford's reference [JSON-Java library](https://github.com/douglascrockford/JSON-java). Both are included in the project as jars.

Unit tests are in JUnit 4 style.


## Usage

TODO Usage examples


## License

Licensed under the MIT License (see `LICENSE` file). Uses the Base64 implementation by [Christian d'Heureuse](http://www.source-code.biz/base64coder/java/).
