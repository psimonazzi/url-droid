package it.idsolutions.util;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.json.JSONObject;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

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
    public static final String VERSION = "0.2.1";
    public static final String APPLICATION_JSON_UTF8 = "application/json; charset=UTF-8";
    public static final String APPLICATION_FORM_URLENCODED_UTF8 = "application/x-www-form-urlencoded; charset=UTF-8";
    public static final int DEFAULT_TIMEOUT_MS = 20000;

    private URL url;
    private Map<String, String> queryParams;
    private Map<String, String> bodyParams;
    private Map<String, String> pathParams;
    private Map<String, String> headers;
    private String entity;
    private HttpURLConnection conn;
    private Integer timeoutMillis = DEFAULT_TIMEOUT_MS;
    private int responseCode;
    private Object responseContent;
    private String responseReasonPhrase;
    private Map<String, List<String>> responseHeaders;
    private Type deserializedResponseType;
    private boolean noExceptionOnServerError = false;
    private String user;
    private String password;
    private String proxyUser;
    private String proxyPassword;
    private Proxy proxy;
    private SSLContext sslContext;
    private HostnameVerifier hostnameVerifier;


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
     * @param url The request URL, alredy encoded
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
     */
    public final HttpClient post() {
        execute("POST");
        return this;
    }


    /**
     * Execute a GET HTTP request.
     * 
     */
    public final HttpClient get() {
        execute("GET");
        return this;
    }


    /**
     * Execute a PUT HTTP request.
     * 
     */
    public final HttpClient put() {
        execute("PUT");
        return this;
    }


    /**
     * Execute a DELETE HTTP request.
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
        if (user != null && !user.equals("") && password != null
                && !password.equals("")) {
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
                setHeader("Proxy-Authorization", "Basic " + base64Encoded); // http://freesoft.org/CIE/RFC/2068/195.htm
            }
        }

        try {
            // this does no network IO
            if (proxy == null)
                conn = (HttpURLConnection) new URL(actualUrl).openConnection();
            else
                conn = (HttpURLConnection) new URL(actualUrl)
                        .openConnection(proxy);
            conn.setConnectTimeout(timeoutMillis);
            conn.setRequestMethod(method);

            // HTTPS
            if (conn instanceof HttpsURLConnection) {
                if (sslContext == null) {
                    SSLContext sc = SSLContext.getInstance("TLS");
                    // Trust anyone
                    TrustManager tm = new X509TrustManager() {
                        public void checkClientTrusted(X509Certificate[] chain,
                                String authType) throws CertificateException {
                        }
    
    
                        public void checkServerTrusted(X509Certificate[] chain,
                                String authType) throws CertificateException {
                        }
    
    
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
                                public boolean verify(String arg0, SSLSession arg1) {
                                    // Allow all
                                    return true;
                                }
                            });
                }
                else {
                    ((HttpsURLConnection) conn).setHostnameVerifier(hostnameVerifier);
                }
            }

            setHeader("User-Agent", "UrlDroid/" + VERSION);

            if (headers != null) {
                for (Map.Entry<String, String> e : headers.entrySet())
                    conn.addRequestProperty(e.getKey(), e.getValue());
            }

            // If required by the HTTP method, send the body entity
            // HTTP standard does not forbid to send a body entity with the GET
            // method,
            // nor query params with the POST/PUT methods, but we try to follow
            // best practices here
            // and enforce sending query params in the body for POST/PUT
            // requests.
            if (entity != null
                    && !entity.equals("")
                    && ("POST".equalsIgnoreCase(method) || "PUT"
                            .equalsIgnoreCase(method))) {
                byte[] payload = entity.getBytes("UTF-8");
                // tells HUC that you're going to POST; still no IO
                conn.setDoOutput(true);
                // Set content length? Can cause problems in some configurations of java6 and web servers
                //conn.setFixedLengthStreamingMode(payload.length);
                // this opens a connection, then sends POST & headers, then
                // writes body entity
                conn.getOutputStream().write(payload);
                conn.getOutputStream().close();
                // responseCode = conn.getResponseCode();
            }

            // Try to get response content, if any
            // In case of POST/PUT the connection is already open, otherwise it
            // will be opened here
            // GZipped content is handled transparently
            String content = null;
            try {
                InputStream in = conn.getInputStream();
                content = getEntityAsString(in, conn.getContentEncoding());
            } catch (FileNotFoundException ex) {
                // That's OK: there was no response content, only a HTTP status
            } catch (IOException ioe) {
                // We could receive the HTTP response here
                this.responseCode = conn.getResponseCode();
                this.responseReasonPhrase = conn.getResponseMessage();
                this.responseHeaders = conn.getHeaderFields();
                if (!noExceptionOnServerError && (responseCode / 100 != 2)) {
                    throw new RuntimeException(responseCode + " "
                            + responseReasonPhrase);
                }
            }
            if (content != null) {
                if (deserializedResponseType != null) {
                    // Deserialize according to the expected type
                    try {
                        this.responseContent = deserialize(content);
                    } catch (Exception ex) {
                        throw new RuntimeException(ex);
                    }
                } else {
                    // Do not deserialize, get response content as string
                    this.responseContent = content;
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
     * @return
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
     * @return
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
     * @return
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
     * Add a request header. If the header was already set it will be
     * overwritten.
     * 
     * @param name
     *            Header name
     * @param value
     *            Header value
     * @return
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
     * @return
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
     * @return
     */
    public final HttpClient entity(String data) {
        entity = data;
        return this;
    }


    /**
     * Set the request entity as serialized JSON.
     * 
     * @param data
     *            Entity object, which will be serialized as JSON
     * @return
     * @throws RuntimeException
     *             When the given object cannot be serialized
     */
    public final HttpClient entity(Object entity) {
        return entity(entity, null);
    }


    /**
     * Set the request entity as serialized JSON.
     * 
     * @param data
     *            Entity object, which will be serialized as JSON
     * @param type
     *            Object type, used as an hint for the serializer. This may be
     *            needed when sending bare generics collections, i.e.
     *            <code>new TypeToken&lt;Collection&lt;myDataType&gt;&gt;(){}.getType()</code>
     * @return
     * @throws RuntimeException
     *             When the given object cannot be serialized
     */
    public final HttpClient entity(Object entity, Type type) {
        String data;
        try {
            data = serialize(entity, type);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }

        setHeader("Content-Type", APPLICATION_JSON_UTF8);
        return entity(data);
    }


    /**
     * Set the 'Accept' request header, which specifies the expected type of the
     * response content.
     * 
     * @param type
     *            Header value
     * @return
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
     * @return
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
     * @return
     */
    public final HttpClient returnType(Type type) {
        deserializedResponseType = type;
        return this;
    }


    /**
     * Specify that no exceptions be raised in case the response HTTP status is
     * an error (i.e. it's not 2XX).
     * 
     * @return
     */
    public final HttpClient noExceptions() {
        this.noExceptionOnServerError = true;
        return this;
    }


    /**
     * Set the request timeout.
     * <p>
     * If a timeout is not explicitly set, {@code DEFAULT_TIMEOUT_MS} will be used.
     * 
     * @param timeoutMillis
     *            The request timeout in milliseconds
     * @return
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
     * @return
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
     * @return
     */
    public final HttpClient proxy(Proxy proxy, String proxyUser,
            String proxyPassword) {
        this.proxy = proxy;
        this.proxyUser = proxyUser;
        this.proxyPassword = proxyPassword;
        return this;
    }
    
    
    /**
     * Set a SSLContext to use for HTTPS requests. 
     * The SSLContext must be already initialized (with a call to init()).
     * <p>
     * If not set, any SSL certificate, even self-signed, will be trusted.
     * 
     * @param sslContext
     *            SSLContext, already initialized
     * @return
     */
    public final HttpClient sslContext(SSLContext sslContext) {
        this.sslContext = sslContext;
        return this;
    }
    
    
    /**
     * Set a SSL hostname verifier to use for HTTPS requests.
     * <p>
     * If not set, hostname verification will be skipped and any hostname will be trusted.
     *  
     * @param hostnameVerifier
     * @return
     */
    public final HttpClient sslHostnameVerifier(HostnameVerifier hostnameVerifier) {
        this.hostnameVerifier = hostnameVerifier;
        return this;
    }


    /**
     * Deserialize the given JSON string. Either Douglas Crockford's JSON or
     * GSON will be used according to the specified return type.
     * 
     */
    private Object deserialize(String content) throws Exception {
        if (deserializedResponseType == JSONObject.class) {
            // deserialize as a generic JSONObject
            return new JSONObject(content);
        } else {
            // Deserialize dates as Unix timestamp notation, for
            // interoperability with Jackson-RS
            Gson gson = new GsonBuilder().registerTypeAdapter(Date.class,
                    new JsonDeserializer<Date>() {
                        @Override
                        public Date deserialize(JsonElement je, Type type,
                                JsonDeserializationContext jdc)
                                throws JsonParseException {
                            return new Date(je.getAsLong());
                        }
                    }).create();
            return gson.fromJson(content, deserializedResponseType);
        }
    }


    /**
     * Serialize the given object to a JSON string.
     * 
     */
    private String serialize(Object content, Type type) throws Exception {
        // Serialize dates as Unix timestamp notation, for interoperability with
        // Jackson-RS
        Gson gson = new GsonBuilder()
        // .serializeNulls()
        // .setDateFormat(DateFormat.LONG)
                .registerTypeAdapter(Date.class, new JsonSerializer<Date>() {
                    @Override
                    public JsonElement serialize(Date src, Type typeOfSrc,
                            JsonSerializationContext context) {
                        return src == null ? null : new JsonPrimitive(src
                                .getTime());
                    }
                }).create();
        if (type == null)
            return gson.toJson(content);
        else
            return gson.toJson(content, type);
    }


    /**
     * Returns the HTTP status code of the response.
     * This method must be called after the request has been executed.
     * 
     * @return
     */
    public final int code() {
        return responseCode;
    }


    /**
     * Returns the content of the HTTP response.
     * This method must be called after the request has been executed.
     * 
     * @return The response content as a deserialized object of the type
     *         specified by #returnType(Type), or the raw string
     */
    public final Object content() {
        return responseContent;
    }


    /**
     * Returns the HTTP reason phrase of the response, which is a textual
     * description of the status code.
     * <p>
     * This method must be called after the request has been executed.
     * 
     * @return
     */
    public final String reasonPhrase() {
        return responseReasonPhrase;
    }


    /**
     * Returns the request URL bound to this instance.
     * 
     * @return
     */
    public final URL url() {
        return url;
    }
    
    
    /**
     * Returns the HTTP headers of the response.
     * This method must be called after the request has been executed.
     * 
     * @return
     */
    public final Map<String, List<String>> responseHeaders() {
        return responseHeaders;
    }


    /**
     * Read the response content in a string. This method tries to decode the
     * content according to the specified encoding, which is UTF-8 by default.
     * 
     * @return
     * @throws Exception
     *             When the content cannot be read or decoded
     */
    private String getEntityAsString(InputStream responseEntity, String encoding)
            throws Exception {
        String r = null;
        InputStream istream = null;
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
            if (istream != null)
                istream.close();
        }
        return r;
    }

}
