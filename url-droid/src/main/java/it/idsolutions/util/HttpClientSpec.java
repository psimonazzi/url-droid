package it.idsolutions.util;

import java.io.InputStream;
import java.net.Proxy;
import java.util.List;
import java.util.Map;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;

/**
 * Extracted interface specification, in preparation
 * for different implementations (OkHttpClient, ...).
 * 
 * @author ps
 */
public interface HttpClientSpec {
    /**
     * Interface for serialize/deserialize adapters.
     */
    public interface DataAdapter {
        String serialize(Object content);
        <T> T deserialize(String content, Class<T> type);
        <T> T deserializeRef(String content, Object typeRef);
    }
    
    public interface RawStreamCallback {
        /**
         * Called on the response stream when it is received.
         * 
         * @param code The response code
         * @param in The response stream
         */
        void onRawStream(final int code, final InputStream in);
        
        /**
         * Called on the response stream when it is received, in case
         * the HTTP response status is an error.
         * 
         * @param code The response code
         * @param err The response stream
         */
        void onRawErrorStream(final int code, final InputStream err);
    }
    
    /**
     * Set the 'Accept' request header, which specifies the expected type
     * of the response content.
     *
     * @param type
     *            Header value
     * @return Self for chaining
     */
    HttpClient accept(String type);

    /**
     * Add a query param to send in the request body.
     * <p>
     * Used for POST or PUT requests. Standard does not forbid use with GET
     * requests, but it's not best practice.
     *
     * @param name
     *            Param name
     * @param value
     *            Param value, which will be encoded as
     *            application/x-www-form-urlencoded with charset UTF-8
     * @return Self for chaining
     */
    HttpClient addBodyParam(String name, String value);

    /**
     * Add a query param to send in the request body. Any special char will be left as is.
     * <p>
     * Used for POST or PUT requests. Standard does not forbid use with GET
     * requests, but it's not best practice.
     *
     * @param name Param name
     * @param value Param value, which will be used as is. Special chars will not be encoded
     * @return Self for chaining
     */
    HttpClient addBodyParamNoEncoding(String name, String value);

    /**
     * Set a text param in a multipart/form-data request.
     * <p>
     * The param will be added as a part in the request body.
     *
     * @param name
     *            Content-disposition name
     * @param value
     *            Text value that will be written to the request, without any
     *            encoding
     * @return Self for chaining
     */
    HttpClient addMultiPartParam(String name, String value);

    /**
     * Set a generic data param in a multipart/form-data request.
     * <p>
     * The param will be added as a part with content-disposition set to the
     * given values.
     *
     *
     * @param name
     *            Content-disposition name
     * @param filename
     *            Content-disposition filename. If null this field will be
     *            omitted
     * @param type
     *            Content-type of the part. If null, 'application/octet-stream'
     *            will be used
     * @param data
     *            Binary data that will be written to the request
     * @return Self for chaining
     */
    HttpClient addMultiPartParam(String name, String filename, String type, InputStream data);

    /**
     * Set a param in the URL path of the request.
     * <p>
     * The URL path will be modified by sustituting the param name with its
     * value.
     *
     * @param name Param name, specified in the path as '{name}'
     * @param value Param value, which will be encoded according to RFC3986 (which
     *     obsoletes RFC2396) with charset UTF-8
     * @return Self for chaining
     */
    HttpClient addPathParam(String name, String value);

    /**
     * Add a query param to send in the request URL.
     * <p>
     * Used for GET requests. Standard does not forbid use with POST or PUT
     * requests, but it's not best practice.
     *
     * @param name
     *            Param name
     * @param value
     *            Param value, which will be encoded as
     *            application/x-www-form-urlencoded with charset UTF-8
     * @return Self for chaining
     */
    HttpClient addQueryParam(String name, String value);

    /**
     * Returns the HTTP status code of the response. This method must be called
     * after the request has been executed.
     *
     * @return Self for chaining
     */
    int code();

    /**
     * Returns the content of the HTTP response. This method must be called
     * after the request has been executed.
     *
     * @return The response content as a deserialized object of the type
     *         specified by #returnType(Type), or the raw string
     */
    Object content();

    /**
     * Set the 'Content-Type' request header, which specifies the type of
     * the request entity.
     *
     * @param type
     *            Header value
     * @return Self for chaining
     */
    HttpClient contentType(String type);

    /**
     * Set HTTP Basic authorization credentials.
     * <p>
     * Credentials can also be specified as system properties, but this method
     * will override them. If credentials are not set, the request will not
     * enable authorization
     *
     * @param user
     *            User (can also be specified as system property 'http.user')
     * @param password
     *            Password (can also be specified as system property
     *            'http.password')
     * @return Self for chaining
     */
    HttpClient credentials(String user, String password);

    /**
     * Execute a DELETE HTTP request.
     *
     * @return Self for chaining
     *
     */
    HttpClient delete();

    /**
     * Get the request entity, already encoded.
     * It will be non-null only after the request is made.
     *
     * @return Entity body as text
     */
    String encodedEntity();

    /**
     * Set the request entity.
     * <p>
     * This method is used for sending data serialized as JSON, XML, etc. To
     * send a query string in the request body, use #entityUrlEncode(String).
     *
     * @param data
     *            Entity as string
     * @return Self for chaining
     */
    HttpClient entity(String data);
    
    /**
     * Set the request entity.
     * <p>
     * This method is used for sending data serialized as JSON, XML, etc. To
     * send a query string in the request body, use #entityUrlEncode(String).
     *
     * @param data
     *            Entity as bytes, encoded in UTF-8
     * @return Self for chaining
     */
    HttpClient entity(byte[] data);

    /**
     * Set the request entity as serialized JSON.
     *
     * @param entity
     *            Entity object, which will be serialized as JSON
     * @param adapter
     *            Implementation of DataAdapter used for serialization
     * @return Self for chaining
     * @throws RuntimeException
     *             When the given object cannot be serialized
     */
    HttpClient entity(Object entity, DataAdapter adapter);

    /**
     * Set the request entity for requests of type
     * application/x-www-form-urlencoded.
     *
     * @param data
     *            Entity as string, which will be encoded as
     *            application/x-www-form-urlencoded with charset UTF-8
     * @return Self for chaining
     */
    HttpClient entityUrlEncode(String data);

    /**
     * Execute a GET HTTP request.
     *
     * @return Self for chaining
     *
     */
    HttpClient get();

    /**
     * Check if using a proxy is allowed for this url.
     * A proxy can be used only for hosts that are not in the
     * list of non-proxy hosts.
     *
     * @return True if using a proxy is allowed
     */
    boolean isProxyAllowed();

    /**
     * Specify that no exceptions be raised in case the response HTTP
     * status is an error (i.e. it's not 2XX).
     *
     * @return Self for chaining
     */
    HttpClient noExceptions();

    /**
     * Do not use a proxy, even if there is a system proxy set.
     *
     * @return Self for chaining
     */
    HttpClient noProxy();

    /**
     * Execute a POST HTTP request.
     *
     * @return Self for chaining
     *
     */
    HttpClient post();

    /**
     * Set an HTTP or SOCKS proxy to use for the request.
     * <p>
     * A proxy will also be used if the proxy host and port are specified as
     * system properties, respectively 'http.proxyHost' and 'http.proxyPort'. In
     * this case the proxy will be of type HTTP. If a proxy is specified in this
     * way, this method will override it.
     * <p>
     * If the proxy requires Basic authorization, the credentials can also be
     * set here, otherwise leave them null. Credentials can also be specified as
     * system properties, but this method will override them.
     *
     * @param proxy
     *            Proxy
     * @param proxyUser
     *            User (can also be specified as system property
     *            'http.proxyUser')
     * @param proxyPassword
     *            Password (can also be specified as system property
     *            'http.proxyPassword')
     * @return Self for chaining
     */
    HttpClient proxy(Proxy proxy, String proxyUser, String proxyPassword);

    /**
     * Set an HTTP or SOCKS proxy to use for the request.
     * <p>
     * A proxy will also be used if the proxy host and port are specified as
     * system properties, respectively 'http.proxyHost' and 'http.proxyPort'. In
     * this case the proxy will be of type HTTP. If a proxy is specified in this
     * way, this method will override it.
     * <p>
     * If the proxy requires Basic authorization, the credentials can also be
     * set here, otherwise leave them null. Credentials can also be specified as
     * system properties, but this method will override them.
     *
     * @param proxy
     *            Proxy
     * @param proxyUser
     *            User (can also be specified as system property
     *            'http.proxyUser')
     * @param proxyPassword
     *            Password (can also be specified as system property
     *            'http.proxyPassword')
     * @param nonProxyHosts
     *            List of hosts for which the proxy should not be used
     *            (can also be specified as system property
     *            'http.nonProxyHosts', with hosts separated by '|')
     * @return Self for chaining
     */
    HttpClient proxy(Proxy proxy, String proxyUser, String proxyPassword, String[] nonProxyHosts);

    /**
     * Execute a PUT HTTP request.
     *
     * @return Self for chaining
     *
     */
    HttpClient put();

    /**
     * Returns the raw content of the HTTP response. This method must be called
     * after the request has been executed.
     *
     * @return The response content as text
     */
    String rawContent();

    /**
     * Set the read timeout.
     * <p>
     * If a timeout is not explicitly set, default from HttpURLConnection will
     * be used.
     *
     * @param readTimeoutMillis
     *            The request timeout in milliseconds
     * @return Self for chaining
     */
    HttpClient readTimeout(int readTimeoutMillis);

    /**
     * Returns the HTTP reason phrase of the response, which is a textual
     * description of the status code.
     * <p>
     * This method must be called after the request has been executed.
     *
     * @return Self for chaining
     */
    String reasonPhrase();

    /**
     * Returns the HTTP headers of the response. This method must be called
     * after the request has been executed.
     *
     * @return Self for chaining
     */
    Map<String, List<String>> responseHeaders();

    /**
     * Set the expected type of the response content.
     *
     * @param type
     *            Object type, used as an hint for the deserializer.
     * @param adapter
     *            Implementation of DataAdapter used for deserialization.
     * @return Self for chaining
     */
    HttpClient returnType(Class<?> type, DataAdapter adapter);

    /**
     * Set the expected type of the response content.
     *
     * @param type
     *            Object type, used as an hint for the deserializer. Use this
     *            method to specify a type in case of type erasure (i.e. bare
     *            generics collections).
     * @param adapter
     *            Implementation of DataAdapter used for deserialization
     * @return Self for chaining
     */
    HttpClient returnType(Object type, DataAdapter adapter);
    
    /**
     * Set a callback that will be invoked on the raw response stream.
     *
     * @param callback Callback
     * @return Self for chaining
     */
    HttpClient rawStreamCallback(RawStreamCallback callback);
    
    /**
     * Enable or disable request compression.
     * When enabled, header 'Content-Encoding: gzip' will be added and the
     * request body will be gzipped.
     * 
     * @param compress
     *            Enable request compression
     *            
     * @return Self for chaining
     */
    HttpClient compressRequest(boolean compress);

    /**
     * Add a request header. If the header was already set it will be
     * overwritten.
     *
     * @param name
     *            Header name
     * @param value
     *            Header value
     * @return Self for chaining
     */
    HttpClient setHeader(String name, String value);

    /**
     * Set a SSLContext to use for HTTPS requests. The SSLContext must be
     * already initialized (with a call to init()).
     * <p>
     * If not set, any SSL certificate, even self-signed, will be trusted.
     *
     * @param sslContext
     *            SSLContext, already initialized
     * @return Self for chaining
     */
    HttpClient sslContext(SSLContext sslContext);

    /**
     * Set a SSL hostname verifier to use for HTTPS requests.
     * <p>
     * If not set, hostname verification will be skipped and any hostname will
     * be trusted.
     *
     * @param hostnameVerifier SSL hostname verifier
     * @return Self for chaining
     */
    HttpClient sslHostnameVerifier(HostnameVerifier hostnameVerifier);

    /**
     * Set the request timeout.
     * <p>
     * If a timeout is not explicitly set, {@code DEFAULT_TIMEOUT_MS} will be
     * used.
     *
     * @param timeoutMillis
     *            The request timeout in milliseconds
     * @return Self for chaining
     */
    HttpClient timeout(int timeoutMillis);

    /**
     * Returns the request URL bound to this instance. The URL will
     * include all path and query params set. Query params are ordered
     * alphabetically so the URL is deterministic.
     *
     * @return Self for chaining
     */
    String url();

    /**
     * Set user agent. If set, this value overrides the default
     * User Agent header (lib name + version).
     *
     * @param userAgent
     *            User agent
     * @return Self for chaining
     */
    HttpClient userAgent(String userAgent);
    
    /**
     * Build a string describing the full HTTP request for debugging /
     * logging purposes.
     * <br>
     * This request description is also automatically logged on each request
     * with logger 'it.idsolutions.util.HttpClient' at the FINE level.
     * 
     * @return HTTP request as String
     */
    String toRequestDebugString();
    
    /**
     * Build a string describing the full HTTP response for debugging /
     * logging purposes.
     * <br>
     * This response description is also automatically logged on each request
     * with logger 'it.idsolutions.util.HttpClient' at the FINE level.
     * 
     * @return HTTP response as String
     */
    String toResponseDebugString();
    
}
