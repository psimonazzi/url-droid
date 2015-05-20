package test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import it.idsolutions.util.HttpClient;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.URLDecoder;

import org.junit.After;
import org.junit.Test;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URL;

public class HttpClientTest {
    private HttpServer httpServer;
    
    @After
    public void after() {
        if (httpServer != null)
            httpServer.stop(0);
    }

    @Test
    public void testVersion() {
        assertTrue(!HttpClient.VERSION.isEmpty());
    }

    @Test
    public void testPathParam() throws Exception {
        InetSocketAddress address = new InetSocketAddress(3000);
        httpServer = HttpServer.create(address, 0);
        
        httpServer.createContext("/test", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                assertEquals("GET", exchange.getRequestMethod());
                assertEquals("/test/1 2", exchange.getRequestURI().getPath());
                assertEquals("p1=a+b", exchange.getRequestURI().getQuery());
                assertEquals(HttpClient.APPLICATION_JSON_UTF8, 
                        exchange.getRequestHeaders().get("Content-Type").get(0));

                exchange.getResponseHeaders().add("Server", "Test");
                exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, 0);
                exchange.close();
            }
        });
        httpServer.start();
        
        HttpClient c = new HttpClient("http://localhost:" + 3000 + "/test/{id}")
                .contentType(HttpClient.APPLICATION_JSON_UTF8).addPathParam(
                        "id", "1 2")
                .addQueryParam("p1", "a b")
                .get();
        assertEquals(HttpURLConnection.HTTP_OK, c.code());
        assertEquals("Test", c.responseHeaders().get("Server").get(0));
        
        Thread.sleep(200);
        
    }
    
    
    @Test
    public void testPOST() throws Exception {
        InetSocketAddress address = new InetSocketAddress(3001);
        httpServer = HttpServer.create(address, 0);
        
        httpServer.createContext("/testPOST", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                /*System.out.println("Request: ");
                System.out.println(exchange.getRequestMethod() + " "
                        + exchange.getRequestURI());
                Headers requestHeaders = exchange.getRequestHeaders();
                for (Entry<String, List<String>> header : requestHeaders.entrySet()) {
                    System.out.println(header.getKey() + ": "
                            + header.getValue());
                }*/
                assertEquals("POST", exchange.getRequestMethod());
                assertEquals("/testPOST", exchange.getRequestURI().getPath());
                assertEquals(HttpClient.APPLICATION_FORM_URLENCODED_UTF8, 
                        exchange.getRequestHeaders().get("Content-Type").get(0));
                int b;
                StringBuilder buf = new StringBuilder();
                InputStream is = exchange.getRequestBody();
                while ((b = is.read()) != -1) {
                    buf.append((char) b);
                }
                is.close();

                String requestEncoded = "";
                if (buf.length() > 0)
                    requestEncoded = buf.toString();
                
                int length = 0;
                try {
                    length = Integer.parseInt(
                            exchange.getRequestHeaders().get("Content-Length").get(0));
                } catch (Exception ex) {
                    
                }
                assertEquals(requestEncoded.getBytes("UTF-8").length, length);
                
                assertEquals("id=1+2+3&param2=%E2%82%AC", requestEncoded);
                String request = "";
                if (requestEncoded.length() > 0)
                    request = URLDecoder.decode(requestEncoded, "UTF-8");
                assertEquals("id=1 2 3&param2=\u20AC", request);

                exchange.sendResponseHeaders(HttpURLConnection.HTTP_CREATED, 0);
                
                String response = "true";
                exchange.getResponseBody().write(response.getBytes());
                exchange.getResponseBody().close();
                exchange.close();
            }
        });
        httpServer.start();
        
        HttpClient c = new HttpClient("http://localhost:" + 3001 + "/testPOST")
                .contentType(HttpClient.APPLICATION_FORM_URLENCODED_UTF8)
                .addBodyParam("id", "1 2 3")
                .addBodyParam("param2", "\u20AC")
                .post();
        assertEquals(HttpURLConnection.HTTP_CREATED, c.code());
        assertEquals("true", (String)c.content());
        
        Thread.sleep(200);
    }
    
    
    @Test
    public void testPOSTJson() throws Exception {
        InetSocketAddress address = new InetSocketAddress(3002);
        httpServer = HttpServer.create(address, 0);
        
        httpServer.createContext("/testPOST", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                /*System.out.println("Request: ");
                System.out.println(exchange.getRequestMethod() + " "
                        + exchange.getRequestURI());
                Headers requestHeaders = exchange.getRequestHeaders();
                for (Entry<String, List<String>> header : requestHeaders.entrySet()) {
                    System.out.println(header.getKey() + ": "
                            + header.getValue());
                }*/
                assertEquals("POST", exchange.getRequestMethod());
                assertEquals("/testPOST", exchange.getRequestURI().getPath());
                assertEquals(HttpClient.APPLICATION_JSON_UTF8, 
                        exchange.getRequestHeaders().get("Content-Type").get(0));
                int b;
                StringBuilder buf = new StringBuilder();
                InputStream is = exchange.getRequestBody();
                while ((b = is.read()) != -1) {
                    buf.append((char) b);
                }
                is.close();

                String requestEncoded = "";
                if (buf.length() > 0)
                    requestEncoded = buf.toString();
                
                int length = 0;
                try {
                    length = Integer.parseInt(
                            exchange.getRequestHeaders().get("Content-Length").get(0));
                } catch (Exception ex) {
                    
                }
                assertEquals(requestEncoded.getBytes("UTF-8").length, length);

                exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, 0);
                
                String response = "true";
                exchange.getResponseBody().write(response.getBytes());
                exchange.getResponseBody().close();
                exchange.close();
            }
        });
        httpServer.start();
        
        HttpClient c = new HttpClient("http://localhost:" + 3002 + "/testPOST")
                .contentType(HttpClient.APPLICATION_JSON_UTF8)
                .entity("{\"s\":\"test\",\"i\":1360665127000}")
                .post();
        assertEquals(HttpURLConnection.HTTP_OK, c.code());
        assertEquals("true", (String)c.content());
        
        Thread.sleep(200);
    }
    
    
    @Test
    public void testErrorStatus() throws Exception {
        InetSocketAddress address = new InetSocketAddress(3003);
        httpServer = HttpServer.create(address, 0);
        
        httpServer.createContext("/testError", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                /*System.out.println("Request: ");
                System.out.println(exchange.getRequestMethod() + " "
                        + exchange.getRequestURI());
                Headers requestHeaders = exchange.getRequestHeaders();
                for (Entry<String, List<String>> header : requestHeaders.entrySet()) {
                    System.out.println(header.getKey() + ": "
                            + header.getValue());
                }*/
                assertEquals("PUT", exchange.getRequestMethod());
                assertEquals("/testError", exchange.getRequestURI().getPath());
                assertEquals(HttpClient.APPLICATION_JSON_UTF8, 
                        exchange.getRequestHeaders().get("Content-Type").get(0));
                int b;
                StringBuilder buf = new StringBuilder();
                InputStream is = exchange.getRequestBody();
                while ((b = is.read()) != -1) {
                    buf.append((char) b);
                }
                is.close();

                exchange.sendResponseHeaders(HttpURLConnection.HTTP_BAD_METHOD, 0);
                exchange.close();
            }
        });
        httpServer.start();
        
        HttpClient c = new HttpClient("http://localhost:" + 3003 + "/testError")
                .contentType(HttpClient.APPLICATION_JSON_UTF8)
                .entity("{\"test\":true}")
                .noExceptions()
                .put();
        assertEquals(HttpURLConnection.HTTP_BAD_METHOD, c.code());
        
        Thread.sleep(200);
    }
    
    
    @Test
    public void testMultipart() throws Exception {
        InetSocketAddress address = new InetSocketAddress(3004);
        httpServer = HttpServer.create(address, 0);
        
        httpServer.createContext("/multipart", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                assertEquals("POST", exchange.getRequestMethod());
                assertEquals("/multipart", exchange.getRequestURI().getPath());
                assertEquals("multipart/form-data;boundary=" + HttpClient.MULTIPART_BOUNDARY, 
                        exchange.getRequestHeaders().get("Content-Type").get(0));
                int b;
                StringBuilder buf = new StringBuilder();
                InputStream is = exchange.getRequestBody();
                while ((b = is.read()) != -1) {
                    buf.append((char) b);
                }
                is.close();
                
                assertTrue(
                        buf.toString().contains("--" + HttpClient.MULTIPART_BOUNDARY + "\r\n" +
                        "Content-Disposition:form-data;name=\"x\"\r\nContent-Type: application/octet-stream\r\n\r\ny\r\n"));
                assertTrue(
                        buf.toString().contains("--" + HttpClient.MULTIPART_BOUNDARY + "\r\n" +
                        "Content-Disposition:form-data;name=\"a\"\r\nContent-Type: application/octet-stream\r\n\r\n1\r\n"));
                assertTrue(buf.toString().startsWith("--" + HttpClient.MULTIPART_BOUNDARY + "\r\n"));
                assertTrue(buf.toString().endsWith("--" + HttpClient.MULTIPART_BOUNDARY + "--\r\n"));
                
                
                exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, 0);
                
                String response = "true";
                exchange.getResponseBody().write(response.getBytes());
                exchange.getResponseBody().close();
                exchange.close();
            }
        });
        httpServer.start();
        
        HttpClient c = new HttpClient("http://localhost:" + 3004 + "/multipart")
                .addMultiPartParam("x", "y")
                .addMultiPartParam("a", "1")
                .post();
        assertEquals(HttpURLConnection.HTTP_OK, c.code());
        
        Thread.sleep(200);
    }
    
    
    @Test
    public void testReturnStream() throws Exception {
        InetSocketAddress address = new InetSocketAddress(3005);
        httpServer = HttpServer.create(address, 0);
        
        httpServer.createContext("/stream", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                assertEquals("GET", exchange.getRequestMethod());
                int b;
                StringBuilder buf = new StringBuilder();
                InputStream is = exchange.getRequestBody();
                while ((b = is.read()) != -1) {
                    buf.append((char) b);
                }
                is.close();
                
                exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, 0);
                
                String response = "streaming";
                exchange.getResponseBody().write(response.getBytes());
                exchange.getResponseBody().close();
                exchange.close();
            }
        });
        httpServer.start();
        
        HttpClient c = new HttpClient("http://localhost:" + 3005 + "/stream")
                .rawStreamCallback(new HttpClient.RawStreamCallback() {
                    @Override
                    public void onRawStream(final InputStream in) {
                        try {
                            Writer writer = null;
                            Reader reader = null;
                            try {
                                reader = new BufferedReader(new InputStreamReader(in, "UTF-8"), 8192);
                                writer = new StringWriter();
                                int l;
                                char[] buf = new char[8192];
                                while ((l = reader.read(buf)) != -1) {
                                    writer.write(buf, 0, l);
                                }
                                assertEquals("streaming", writer.toString());
                            } finally {
                                if (writer != null)
                                    writer.close();
                                if (reader != null)
                                    reader.close();
                            }
                        } catch (Exception ex) {
                            throw new RuntimeException(ex);
                        }
                    }
                })
                .get();
        assertEquals(HttpURLConnection.HTTP_OK, c.code());
        
        Thread.sleep(200);
    }
    
    
    @Test
    public void testUrl() throws Exception {
        HttpClient c = new HttpClient("http://example.com/path/{p1}")
                .addPathParam("p1", "_")
                .addQueryParam("x", "y")
                .addQueryParam("a", "1");
        String url = c.url();
        assertEquals("http://example.com/path/_?a=1&x=y", url);
        c.addQueryParam("z", "2 3");
        String url2 = c.url();
        assertEquals("http://example.com/path/_?a=1&x=y&z=2+3", url2);
    }

}
