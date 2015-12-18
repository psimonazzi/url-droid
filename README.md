UrlDroid
========

A super-lightweight HTTP client library for Android and the JVM, written in Java.

The library is basically a wrapper around `HttpURLConnection`, which is the API to make HTTP requests on
Android [officially recommended by Google](http://android-developers.blogspot.com/2011/09/androids-http-clients.html).
It provides convenient ways to craft GET, POST and other kinds of requests and get responses back. It can optionally use libraries for serializing/deserializing objects in JSON format.

If the [OkHttp library](http://square.github.io/okhttp) is available, UrlDroid will use its implementation of `HttpURLConnection`, otherwise it will fall back to the default system implementation.


## Features

- Encoding/decoding of query params, URL paths and request or response bodies
- Proxy support (HTTP and SOCKS) with host-exclusion lists
- HTTPS with SSL certificates
- Basic Authorization support
- Sending and receiving data in JSON format
- Gzip compression
- Response caching


## Building

The project is built with Gradle, but it should be trivial to integrate in other build systems and/or IDEs, as UrlDroid consists of a main module with two Java classes and no dependencies. There are also several optional modules for JSON serialization, each with a single Java class.

The project main module is `url-droid`: it provides the HttpClient class. It has no compile dependencies.

If you want to enable JSON serialization/deserialization you can add to your compile dependencies one (or more) of the following adapters:
- `url-droid-jsonorg`: Provides JSON support using Douglas Crockford's reference [JSON-Java library](https://github.com/douglascrockford/JSON-java). It depends on a jar included in the project.
- `url-droid-jackson`: Provides JSON support using [Jackson 2.x](https://github.com/FasterXML/jackson). It depends on the jars included in the project.

The [OkHttp library](http://square.github.io/okhttp) is an optional dependency at runtime.

Each module can be built with the standard Gradle task:

```sh
$ gradle build
```

Unit tests are in JUnit 4 style. You can run them with: 

```sh
$ gradle check
```


## Usage

### Simple GET request

```java
// Send a GET request for http://localhost:3000/test/1%202?p1=a+b
// with a custom header
// Path fragments and query params are encoded automatically
HttpClient c = new HttpClient("http://localhost:3000/test/{id}")
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

### multipart/form-data request for uploading files

```java
// Send a POST multipart request with a string field and a binary file
HttpClient c = new HttpClient("http://localhost:3000/post")
    .addMultiPartParam("file", "picture.jpg", "image/jpeg",
        new FileInputStream(new File("/tmp/picture.jpg")))
    .addMultiPartParam("id", "1337");
    .post();
```

### Handle HTTP status

```java
// If the response status is an error (i.e. not a 2XX code), the client will
// normally throw a RuntimeException.
// To disable this behaviour and do not throw exceptions for any status,
// use noExceptions()
int status = new HttpClient("http://localhost:3000")
    .noExceptions()
    .get()
    .code();  
```

### Get raw response as an InputStream

```java
// Implement a callback to receive the response data as a raw InputStream.
new HttpClient("http://localhost:3000")
    .rawStreamCallback(new HttpClient.RawStreamCallback() {
        @Override
        public void onRawStream(final InputStream in) {
            // use input stream...
        }
    })
    .get();  
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
// For example, on Android, if you have a trust store in BKS format in
// /raw/truststore.bks:
char[] passphrase = "TRUSTSTORE_PASSWORD".toCharArray();
KeyStore ksTrust = KeyStore.getInstance("BKS");
ksTrust.load(context.getResources()
    .openRawResource(R.raw.truststore), passphrase);
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
 
// ... or as a Java POJO, which will be serialized using Jackson
MyData data = new MyData();
data.setParam1("XYZ");
data.setParam2(1337);
data.setParam3(new String[] { true, false, true });
HttpClient c = new HttpClient("http://localhost:3000/test.json")
    .contentType(HttpClient.APPLICATION_JSON_UTF8)
    .entity(data, JacksonAdapter())
    .post();
 
// Read the response as a JSON object of an expected type "MyResponse"...
HttpClient c = new HttpClient("http://localhost:3000/test.json")
    .returnType(MyResponse.class, new JacksonAdapter())
    .get();
MyResponse r = (MyResponse)c.content();
 
// Read the response as a generic JSONObject (with the JSON Org adapter)
HttpClient c = new HttpClient("http://localhost:3000/test.json")
    .accept("application/json")
    .returnType(JSONObject.class, new JsonOrgAdapter())
    .get();
JSONObject r = (JSONObject)c.content();
```        

### Response cache

```java
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
