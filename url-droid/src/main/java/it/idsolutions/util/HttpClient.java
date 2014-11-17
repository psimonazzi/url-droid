package it.idsolutions.util;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;


/**
 * A wrapper around HttpURLConnection, which is the API to make HTTP requests on
 * Android officially recommended by Google.
 * <p>
 * Supports the JSON format for serializing/deserializing content, Gzip
 * compression, Basic auth, HTTPS, HTTP and SOCKS proxies.
 * <p>
 * Each request should use its own instance of this class, unless all settings
 * are exactly the same.
 *
 * @author ps
 */
public class HttpClient {
    public static String VERSION = "";
    static {
        try {
            Properties props = new Properties();
            props.load(HttpClient.class.getClassLoader().getResourceAsStream("app.properties"));
            VERSION = props.getProperty("application.version");
        } catch (Exception ex) {
            VERSION = "";
        }
    }
    public static final String APPLICATION_JSON_UTF8 =
            "application/json; charset=UTF-8";
    public static final String APPLICATION_FORM_URLENCODED_UTF8 =
            "application/x-www-form-urlencoded; charset=UTF-8";
    public static final int DEFAULT_TIMEOUT_MS = 20000;
    public static final String MULTIPART_BOUNDARY =
            "----------------------------443d18e49926jdiGHidf9E830fDid834675j5yhdf8Cs";

    private URL url;
    private Map<String, String> queryParams;
    private Map<String, String> bodyParams;
    private Map<String, String> pathParams;
    private Map<String, MultiPartParam> multiPartParams;
    private Map<String, String> headers;
    private String entity;
    private HttpURLConnection conn;
    private Integer timeoutMillis = DEFAULT_TIMEOUT_MS;
    private int responseCode;
    private Object responseContent;
    private String responseReasonPhrase;
    private Map<String, List<String>> responseHeaders;
    private String rawContent;
    private Class<?> deserializedResponseType;
    private boolean noExceptionOnServerError = false;
    private String user;
    private String password;
    private String proxyUser;
    private String proxyPassword;
    private Proxy proxy;
    private SSLContext sslContext;
    private HostnameVerifier hostnameVerifier;
    private DataAdapter deserializeAdapter;


    /**
     * Returns a new instance bound to the specified URL. The URL must be
     * already encoded (according to RFC3986 or the obsoleted RFC2396 for the
     * path part and the application/x-www-form-urlencoded format for the query
     * part).
     * <p>
     * The URL can contain path params, which can then be set using
     * #addPathParam(String,String). Path params are specified as '{name}',
     * i.e.: <code>'/resource/{id}/1'</code>.
     *
     * @param url
     *            The request URL, alredy encoded
     */
    public HttpClient(String url) {
        try {
            this.url = new URL(url);
        } catch (MalformedURLException ex) {
            throw new RuntimeException(ex);
        }
    }


    /**
     * Execute a POST HTTP request.
     *
     * @return Self for chaining
     *
     */
    public final HttpClient post() {
        execute("POST");
        return this;
    }


    /**
     * Execute a GET HTTP request.
     *
     * @return Self for chaining
     *
     */
    public final HttpClient get() {
        execute("GET");
        return this;
    }


    /**
     * Execute a PUT HTTP request.
     *
     * @return Self for chaining
     *
     */
    public final HttpClient put() {
        execute("PUT");
        return this;
    }


    /**
     * Execute a DELETE HTTP request.
     *
     * @return Self for chaining
     *
     */
    public final HttpClient delete() {
        execute("DELETE");
        return this;
    }


    private HttpClient execute(String method) {
        responseCode = 0;
        responseContent = null;
        responseReasonPhrase = null;
        responseHeaders = null;

        String actualUrl = url.toString();
        if (pathParams != null) {
            for (Map.Entry<String, String> e : pathParams.entrySet()) {
                actualUrl = actualUrl.replace("{" + e.getKey() + "}",
                        e.getValue());
            }
        }

        if (queryParams != null) {
            String query = "";
            for (Map.Entry<String, String> e : queryParams.entrySet())
                query += e.getKey() + "=" + e.getValue() + "&";
            query = query.replaceFirst("&$", "");
            if (!query.equals(""))
                actualUrl += "?" + query;
        }

        if (entity == null) {
            if (bodyParams != null) {
                entity = "";
                for (Map.Entry<String, String> e : bodyParams.entrySet())
                    entity += e.getKey() + "=" + e.getValue() + "&";
                entity = entity.replaceFirst("&$", "");
                if (entity.equals(""))
                    entity = null;
            }
        }

        // HTTP Authentication
        if (user == null)
            user = System.getProperty("http.user");
        if (password == null)
            password = System.getProperty("http.password");
        if (user != null && password != null) {
            String base64Encoded = Base64.encodeString(
                    user + ":" + password).trim();
            setHeader("Authorization", "Basic " + base64Encoded);
        }
        // Or use global auth for this url
        /*
         * Authenticator.setDefault(new Authenticator() { protected
         * PasswordAuthentication getPasswordAuthentication() { return new
         * PasswordAuthentication(user, password.toCharArray()); } });
         */

        // Use proxy if needed
        // By default, HttpURLConnection class will connect directly to the
        // origin server (RFC2616).
        // Both HTTP and SOCKS proxies are supported, using HTTP by default
        if (proxy == null) {
            String proxyHost = System.getProperty("http.proxyHost");
            String proxyPortString = System.getProperty("http.proxyPort");
            if (proxyHost != null && !proxyHost.equals("")
                    && proxyPortString != null && !proxyPortString.equals("")) {
                int proxyPort = Integer.parseInt(proxyPortString);
                proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(
                        proxyHost, proxyPort));
            }
        }
        if (proxy != null) {
            // Proxy authorization
            if (proxyUser == null)
                proxyUser = System.getProperty("http.proxyUser");
            if (proxyPassword == null)
                proxyPassword = System.getProperty("http.proxyPassword");
            if (proxyUser != null && !proxyUser.equals("")
                    && proxyPassword != null && !proxyPassword.equals("")) {
                String base64Encoded = Base64.encodeString(
                        proxyUser + ":" + proxyPassword).trim();
                // http://freesoft.org/CIE/RFC/2068/195.htm
                setHeader("Proxy-Authorization", "Basic " + base64Encoded);
            }
        }

        try {
            // Get the HttpURLConnection object,
            // either the OkHttp implementation
            // (http://square.github.io/okhttp/)
            // if available, or the system default implementation
            Class<?> c;
            try {
                c = Class.forName("com.squareup.okhttp.OkHttpClient");
            } catch (Exception ex) {
                c = null;
            }
            if (c != null) {
                Object okHttp = c.newInstance();
                if (proxy != null)
                    c.getMethod("setProxy", Proxy.class)
                            .invoke(okHttp, proxy);
                conn = (HttpURLConnection) c.getMethod("open", URL.class)
                        .invoke(okHttp, new URL(actualUrl));
            }
            else {
                // this does no network IO
                if (proxy == null)
                    conn = (HttpURLConnection) new URL(actualUrl)
                            .openConnection();
                else
                    conn = (HttpURLConnection) new URL(actualUrl)
                            .openConnection(proxy);
            }

            conn.setConnectTimeout(timeoutMillis);
            conn.setRequestMethod(method);

            // HTTPS
            if (conn instanceof HttpsURLConnection) {
                if (sslContext == null) {
                    SSLContext sc = SSLContext.getInstance("TLS");
                    // Trust anyone
                    TrustManager tm = new X509TrustManager() {
                        @Override
                        public void checkClientTrusted(
                                X509Certificate[] chain, String authType)
                                throws CertificateException {
                        }


                        @Override
                        public void checkServerTrusted(
                                X509Certificate[] chain, String authType)
                                throws CertificateException {
                        }


                        @Override
                        public X509Certificate[] getAcceptedIssuers() {
                            return null;
                        }
                    };
                    sc.init(null, new TrustManager[] { tm }, null);
                    ((HttpsURLConnection) conn).setSSLSocketFactory(
                            sc.getSocketFactory());
                }
                else {
                    ((HttpsURLConnection) conn).setSSLSocketFactory(
                            sslContext.getSocketFactory());
                }

                if (hostnameVerifier == null) {
                    ((HttpsURLConnection) conn).setHostnameVerifier(
                            new HostnameVerifier() {
                                @Override
                                public boolean verify(
                                        String arg0, SSLSession arg1) {
                                    // Allow all
                                    return true;
                                }
                            });
                }
                else {
                    ((HttpsURLConnection) conn)
                            .setHostnameVerifier(hostnameVerifier);
                }
            }

            // Enable cache via HttpResponseCache (it's a no-op for
            // HttpUrlConnection?)
            conn.setUseCaches(true);

            setHeader("User-Agent", "UrlDroid/" +
                    conn.getClass().getName() + "/" + VERSION);

            if (multiPartParams != null && !multiPartParams.isEmpty() &&
                    "POST".equalsIgnoreCase(method)) {
                // override content-type if we have multipart data
                setHeader("Content-Type",
                        "multipart/form-data;boundary=" + MULTIPART_BOUNDARY);
            }

            if (headers != null) {
                for (Map.Entry<String, String> e : headers.entrySet())
                    conn.addRequestProperty(e.getKey(), e.getValue());
            }

            // If required by the HTTP method, send the body entity.
            // HTTP standard does not forbid to send a body entity with the GET
            // method, nor query params with the POST/PUT methods, but best
            // practice is to send query params in the body for POST/PUT
            // requests.
            if (entity != null &&
                    !entity.equals("") &&
                    ("POST".equalsIgnoreCase(method) ||
                            "PUT".equalsIgnoreCase(method))) {
                byte[] payload = entity.getBytes("UTF-8");
                // tells HUC that you're going to POST; still no IO
                conn.setDoOutput(true);
                // Set content length? Can cause problems in some 
                // configurations of java6 and web servers
                // conn.setFixedLengthStreamingMode(payload.length);
                // this opens a connection, then sends POST & headers, then
                // writes body entity
                try {
                    conn.getOutputStream().write(payload);
                } finally {
                    conn.getOutputStream().close();
                }
            }
            else if (multiPartParams != null && !multiPartParams.isEmpty() &&
                    "POST".equalsIgnoreCase(method)) {
                // Build a multipart/form-data request
                conn.setDoOutput(true);
                DataOutputStream os = new DataOutputStream(
                        conn.getOutputStream());
                final int maxBufferSize = 1024;
                for (MultiPartParam p : multiPartParams.values()) {
                    String multiPartHeaders =
                            "Content-Disposition:form-data;" +
                                    "name=\"" + p.getName() + "\"" +
                                    (p.getFilename() != null ?
                                            ";filename=\"" + p.getFilename()
                                                    + "\"" : "") +
                                    "\r\n" +
                                    "Content-Type: " +
                                    (p.getType() != null ?
                                            p.getType()
                                            : "application/octet-stream") +
                                    "\r\n";
                    os.writeBytes("--" + MULTIPART_BOUNDARY + "\r\n");
                    os.writeBytes(multiPartHeaders + "\r\n");
                    if (p.getData() == null && p.getValue() != null) {
                        // data is a string
                        os.writeBytes(p.getValue());
                    }
                    else {
                        // data is an inputstream, buffer write
                        int bytesAvailable = p.getData().available();
                        int bufferSize = Math
                                .min(bytesAvailable, maxBufferSize);
                        byte[] buffer = new byte[bufferSize];
                        int read = p.getData().read(buffer, 0, bufferSize);
                        while (read > 0) {
                            os.write(buffer, 0, bufferSize);
                            bytesAvailable = p.getData().available();
                            bufferSize = Math
                                    .min(bytesAvailable, maxBufferSize);
                            read = p.getData().read(buffer, 0, bufferSize);
                        }
                    }
                    os.writeBytes("\r\n");
                }
                os.writeBytes("--" + MULTIPART_BOUNDARY + "--\r\n");
                os.close();
            }

            // Try to get response content, if any
            // In case of POST/PUT the connection is already open, otherwise 
            // it will be opened here
            // GZipped content is handled transparently
            this.rawContent = null;
            try {
                InputStream in = conn.getInputStream();
                this.rawContent = getEntityAsString(in, conn.getContentEncoding());
            } catch (FileNotFoundException ex) {
                // That's OK: there was no response content
            } catch (IOException ioe) {
                // We could receive the HTTP response here
                this.responseCode = conn.getResponseCode();
                this.responseReasonPhrase = conn.getResponseMessage();
                this.responseHeaders = conn.getHeaderFields();
                if (!noExceptionOnServerError && (responseCode / 100 != 2)) {
                    throw new RuntimeException(responseCode + " " +
                            responseReasonPhrase);
                }
            }
            if (this.rawContent != null) {
                if (deserializedResponseType != null &&
                        deserializeAdapter != null) {
                    // Deserialize according to the expected type
                    try {
                        this.responseContent = deserializeAdapter.deserialize(this.rawContent, deserializedResponseType);
                    } catch (Exception ex) {
                        throw new RuntimeException(ex);
                    }
                } else {
                    // Do not deserialize, get response content as string
                    this.responseContent = this.rawContent;
                }
            }
            this.responseCode = conn.getResponseCode();
            this.responseReasonPhrase = conn.getResponseMessage();
            this.responseHeaders = conn.getHeaderFields();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        } finally {
            if (conn != null)
                conn.disconnect();
        }

        // Throw an exception if the HTTP status was not 2XX and the user has
        // not opted to suppress the exception
        // If cache support is enabled we may get a 304 status, which should be
        // handled in the exception catch
        if (!noExceptionOnServerError && (responseCode / 100 != 2)) {
            throw new RuntimeException(responseCode + " "
                    + responseReasonPhrase);
        }

        return this;
    }


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
    public HttpClient addQueryParam(String name, String value) {
        if (queryParams == null)
            queryParams = new HashMap<String, String>();
        try {
            queryParams.put(name, URLEncoder.encode(value, "UTF-8"));
        } catch (Exception ex) {
            // We use utf-8 hardcoded, should never throw
            throw new RuntimeException(ex);
        }
        return this;
    }


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
    public final HttpClient addBodyParam(String name, String value) {
        if (bodyParams == null)
            bodyParams = new HashMap<String, String>();
        try {
            bodyParams.put(name, URLEncoder.encode(value, "UTF-8"));
        } catch (Exception ex) {
            // We use utf-8 hardcoded, should never throw
            throw new RuntimeException(ex);
        }
        return this;
    }


    /**
     * Set a param in the URL path of the request.
     * <p>
     * The URL path will be modified by sustituting the param name with its
     * value.
     *
     * @param name
     *            Param name, specified in the path as '{name}'
     * @param value
     *            Param value, which will be encoded according to RFC3986 (which
     *            obsoletes RFC2396) with charset UTF-8
     * @return Self for chaining
     */
    public final HttpClient addPathParam(String name, String value) {
        if (pathParams == null)
            pathParams = new HashMap<String, String>();
        try {
            pathParams.put(name,
                    new URI(null, null, value, null).toASCIIString());
        } catch (Exception ex) {
            // We use utf-8 hardcoded, should never throw
            throw new RuntimeException(ex);
        }
        return this;
    }


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
    public final HttpClient addMultiPartParam(String name, String value) {
        if (multiPartParams == null)
            multiPartParams = new HashMap<String, MultiPartParam>();
        try {
            MultiPartParam p = new MultiPartParam();
            p.setName(name);
            p.setValue(value);
            multiPartParams.put(name, p);
        } catch (Exception ex) {
            // We use utf-8 hardcoded, should never throw
            throw new RuntimeException(ex);
        }
        return this;
    }


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
    public final HttpClient addMultiPartParam(String name, String filename,
                                              String type, InputStream data) {
        if (multiPartParams == null)
            multiPartParams = new HashMap<String, MultiPartParam>();
        try {
            MultiPartParam p = new MultiPartParam();
            p.setName(name);
            p.setFilename(filename);
            p.setType(type);
            p.setData(data);
            multiPartParams.put(name, p);
        } catch (Exception ex) {
            // We use utf-8 hardcoded, should never throw
            throw new RuntimeException(ex);
        }
        return this;
    }


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
    public final HttpClient setHeader(String name, String value) {
        if (headers == null)
            headers = new HashMap<String, String>();
        headers.put(name, value);
        return this;
    }


    /**
     * Set the request entity for requests of type
     * application/x-www-form-urlencoded.
     *
     * @param data
     *            Entity as string, which will be encoded as
     *            application/x-www-form-urlencoded with charset UTF-8
     * @return Self for chaining
     */
    public final HttpClient entityUrlEncode(String data) {
        // Need to encode the entity only if content-type is
        // application/x-www-form-urlencoded
        if (data != null && !data.equals("")) {
            try {
                entity = URLEncoder.encode(data, "UTF-8");
            } catch (Exception ex) {
                // We use utf-8 hardcoded, should never throw
                throw new RuntimeException(ex);
            }
        }
        return this;
    }


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
    public final HttpClient entity(String data) {
        entity = data;
        return this;
    }


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
    public final HttpClient entity(Object entity, DataAdapter adapter) {
        String data;
        try {
            data = adapter.serialize(entity);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }

        setHeader("Content-Type", APPLICATION_JSON_UTF8);
        return entity(data);
    }


    /**
     * Set the 'Accept' request header, which specifies the expected type of
     * the response content.
     *
     * @param type
     *            Header value
     * @return Self for chaining
     */
    public final HttpClient accept(String type) {
        if (type != null && !type.equals(""))
            setHeader("Accept", type);
        return this;
    }


    /**
     * Set the 'Content-Type' request header, which specifies the type of the
     * request entity.
     *
     * @param type
     *            Header value
     * @return Self for chaining
     */
    public final HttpClient contentType(String type) {
        if (type != null && !type.equals(""))
            setHeader("Content-Type", type);
        return this;
    }


    /**
     * Set the expected type of the response content.
     * <p>
     * In the special case that the type is JSONObject.class, Douglas
     * Crockford's JSON deserializer will be used. In all other cases the GSON
     * deserializer will be used.
     *
     * @param type
     *            Object type, used as an hint for the deserializer. This may be
     *            needed with bare generics collections, i.e.
     *            <code>new TypeToken&lt;Collection&lt;myDataType&gt;&gt;(){}.getType()</code>
     * @param adapter
     *            Implementation of DataAdapter used for serialization
     * @return Self for chaining
     */
    public final HttpClient returnType(Class<?> type, DataAdapter adapter) {
        deserializedResponseType = type;
        this.deserializeAdapter = adapter;
        return this;
    }


    /**
     * Specify that no exceptions be raised in case the response HTTP status is
     * an error (i.e. it's not 2XX).
     *
     * @return Self for chaining
     */
    public final HttpClient noExceptions() {
        this.noExceptionOnServerError = true;
        return this;
    }


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
    public final HttpClient timeout(int timeoutMillis) {
        this.timeoutMillis = timeoutMillis;
        return this;
    }


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
    public final HttpClient credentials(String user, String password) {
        this.user = user;
        this.password = password;
        return this;
    }


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
    public final HttpClient proxy(Proxy proxy, String proxyUser,
                                  String proxyPassword) {
        this.proxy = proxy;
        this.proxyUser = proxyUser;
        this.proxyPassword = proxyPassword;
        return this;
    }


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
    public final HttpClient sslContext(SSLContext sslContext) {
        this.sslContext = sslContext;
        return this;
    }


    /**
     * Set a SSL hostname verifier to use for HTTPS requests.
     * <p>
     * If not set, hostname verification will be skipped and any hostname will
     * be trusted.
     *
     * @param hostnameVerifier SSL hostname verifier
     * @return Self for chaining
     */
    public final HttpClient sslHostnameVerifier(
            HostnameVerifier hostnameVerifier) {
        this.hostnameVerifier = hostnameVerifier;
        return this;
    }


    /**
     * Returns the HTTP status code of the response. This method must be called
     * after the request has been executed.
     *
     * @return Self for chaining
     */
    public final int code() {
        return responseCode;
    }


    /**
     * Returns the content of the HTTP response. This method must be called
     * after the request has been executed.
     *
     * @return The response content as a deserialized object of the type
     *         specified by #returnType(Type), or the raw string
     */
    public final Object content() {
        return responseContent;
    }


    /**
     * Returns the raw content of the HTTP response. This method must be called
     * after the request has been executed.
     *
     * @return The response content as text
     */
    public final String rawContent() {
        return rawContent;
    }


    /**
     * Returns the HTTP reason phrase of the response, which is a textual
     * description of the status code.
     * <p>
     * This method must be called after the request has been executed.
     *
     * @return Self for chaining
     */
    public final String reasonPhrase() {
        return responseReasonPhrase;
    }


    /**
     * Returns the request URL bound to this instance.
     *
     * @return Self for chaining
     */
    public final URL url() {
        return url;
    }


    /**
     * Returns the HTTP headers of the response. This method must be called
     * after the request has been executed.
     *
     * @return Self for chaining
     */
    public final Map<String, List<String>> responseHeaders() {
        return responseHeaders;
    }


    /**
     * Read the response content in a string. This method tries to decode the
     * content according to the specified encoding, which is UTF-8 by default.
     *
     * @return Self for chaining
     * @throws Exception
     *             When the content cannot be read or decoded
     */
    private String getEntityAsString(InputStream responseEntity,
                                     String encoding) throws Exception {
        String r = null;
        Writer writer = null;
        Reader reader = null;
        try {
            // Stream length could be greater than the response Content-Length,
            // because the stream will unzip content transparently
            reader = new BufferedReader(new InputStreamReader(responseEntity,
                    encoding == null ? "UTF-8" : encoding), 8192);
            writer = new StringWriter();
            int l;
            char[] buf = new char[8192];
            while ((l = reader.read(buf)) != -1) {
                writer.write(buf, 0, l);
            }
            r = writer.toString();
        } finally {
            if (writer != null)
                writer.close();
            if (reader != null)
                reader.close();
        }
        return r;
    }


    private static class MultiPartParam {
        private String name;
        private String filename;
        private String type;
        private InputStream data;
        private String value;


        public MultiPartParam() {

        }


        public String getName() {
            return name;
        }


        public void setName(String name) {
            this.name = name;
        }


        public String getFilename() {
            return filename;
        }


        public void setFilename(String filename) {
            this.filename = filename;
        }


        public String getType() {
            return type;
        }


        public void setType(String type) {
            this.type = type;
        }


        public InputStream getData() {
            return data;
        }


        public void setData(InputStream data) {
            this.data = data;
        }


        public String getValue() {
            return value;
        }


        public void setValue(String value) {
            this.value = value;
        }
    }


    public interface DataAdapter {
        String serialize(Object content);
        <T> T deserialize(String content, Class<T> type);
    }

}