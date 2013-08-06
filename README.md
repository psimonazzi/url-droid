UrlDroid
========

A super-lightweight HTTP client library for Android and the JVM, written in Java.

The library is basically a wrapper around `HttpURLConnection`, which is the API to make HTTP requests on
Android [officially recommended by Google](http://android-developers.blogspot.com/2011/09/androids-http-clients.html).
It provides convenient ways to craft GET, POST and other kinds of requests, and get responses back. It also integrates the GSON library for serializing/deserializing objects in JSON format.

If the [OkHttp library](http://square.github.io/okhttp) is available, UrlDroid will use its implementation of `HttpURLConnection`, otherwise it will fall back to the default system implementation.


## Features

- Encoding/decoding of query params, URL paths and request or response bodies
- Proxy support (HTTP and SOCKS)
- HTTPS and Basic Authorization support
- Sending and receiving data in JSON format
- Gzip compression
- Response caching


## Building

The project is in Eclipse format, but it is trivial to integrate in other build systems or IDEs, as UrlDroid consists of a single Java class.

The only dependencies are the [Google GSON library](https://code.google.com/p/google-gson/) and Douglas Crockford's reference [JSON-Java library](https://github.com/douglascrockford/JSON-java). Both are included in the project as jars. The [OkHttp library](http://square.github.io/okhttp) is an optional dependency at runtime.

Unit tests are in JUnit 4 style.


## Usage

### Simple GET request

```java
// Send a GET request for http://localhost:3000/test/1%202?p1=a+b
// with a custom header
// Path fragments and query params are encoded automatically
HttpClient c = new HttpClient("http://localhost:3000/test/{id}")
    .contentType(HttpClient.APPLICATION_JSON_UTF8)
    .addPathParam("id", "1 2")
    .addQueryParam("p1", "a b")
    .setHeader("X-Client", "url-droid")
    .get();
 
// HTTP status code, e.g. 200
int status = c.code();
// Response content as a raw string
String content = (String)c.content();
```

### POST request with form params

```java
// Send a POST request with two query params in the request body: "p1=X&p2=Y"
HttpClient c = new HttpClient("http://localhost:3000/post")
    .contentType(HttpClient.APPLICATION_FORM_URLENCODED_UTF8)
    .addBodyParam("p1", "X")
    .addBodyParam("p2", "Y");
    .post();
```

### Handle HTTP status

```java
// If the response status is an error (i.e. not a 2XX code), the client will
// normally throw a RuntimeException.
// To disable this behaviour and do not throw exceptions for any status, use noExceptions()
int status = new HttpClient("http://localhost:3000")
    .noExceptions()
    .get()
    .code();  
```
    
### HTTPS and Basic Auth
    
```java
// Set Basic Auth credentials
// When the request url protocol is HTTPS, any certificate and host will be
// trusted by default, even if self-signed!
int status = new HttpClient("https://localhost:3000/test")
    .credentials("user", "password")
    .get();
 
// To provide a trusted certificate you can create a SSLContext
// For example, on Android, if you have a trust store in BKS format in /raw/truststore.bks:
char[] passphrase = "TRUSTSTORE_PASSWORD".toCharArray();
KeyStore ksTrust = KeyStore.getInstance("BKS");
ksTrust.load(context.getResources().openRawResource(R.raw.truststore), passphrase);
TrustManagerFactory tmf = TrustManagerFactory.getInstance(
    KeyManagerFactory.getDefaultAlgorithm());
tmf.init(ksTrust);
SSLContext sslContext = SSLContext.getInstance("TLS");
sslContext.init(null, tmf.getTrustManagers(), new SecureRandom());
 
// Finally pass the SSLContext to the client
int status = new HttpClient("https://localhost:3000/test2")
    .sslContext(sslContext)
    .get();
```

### JSON serialization / deserialization

```java
// Post a JSON object as a string...
HttpClient c = new HttpClient("http://localhost:3000/test.json")
    .contentType(HttpClient.APPLICATION_JSON_UTF8)
    .entity("{\"s\":\"test\",\"i\":1360665127000}")
    .post();
 
// ... or as a Java POJO, which will be serialized
MyData data = new MyData();
data.setParam1("XYZ");
data.setParam2(1337);
data.setParam3(new String[] { true, false, true });
HttpClient c = new HttpClient("http://localhost:3000/test.json")
    .contentType(HttpClient.APPLICATION_JSON_UTF8)
    .entity(data, MyData.class)
    .post();
 
// Read the response as a JSON object of an expected type "MyResponse"...
HttpClient c = new HttpClient("http://localhost:3000/test.json")
    .returnType(MyResponse.class)
    .get();
MyResponse r = (MyResponse)c.content();
 
// Read the response as a generic JSONObject
HttpClient c = new HttpClient("http://localhost:3000/test.json")
    .accept("application/json")
    .returnType(JSONObject.class)
    .get();
JSONObject r = (JSONObject)c.content();
```        

### Response cache

```
// Setup cache. You need to do this one time only, before making requests
com.squareup.okhttp.HttpResponseCache cache;
File cacheDir = new File(
        System.getProperty("java.io.tmpdir"), // or getCacheDir() on Android
        "http-" + UUID.randomUUID().toString());
cache = new HttpResponseCache(cacheDir, 10 * 1024 * 1024);
ResponseCache.setDefault(cache);
 
// ...
 
// HTTP and HTTPS GET responses will be cached according to RFC 2068
new HttpClient("http://localhost:1337").get();
 
// ...
 
// Uninstall the cache when it's not needed
ResponseCache.setDefault(null);
cache.delete();
```


## License

Licensed under the MIT License (see `LICENSE` file). Uses the Base64 implementation by [Christian d'Heureuse](http://www.source-code.biz/base64coder/java/).
