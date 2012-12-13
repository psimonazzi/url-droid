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
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

public class HttpClientTest {
    HttpServer httpServer;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
    }


    @AfterClass
    public static void tearDownAfterClass() throws Exception {
    }


    @Before
    public void setUp() throws Exception {
        InetSocketAddress address = new InetSocketAddress(3000);
        if (httpServer != null)
            httpServer.stop(0);
        httpServer = HttpServer.create(address, 0);
        /*httpServer.createContext("/", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                System.out.println("Request: ");
                System.out.println(exchange.getRequestMethod() + " "
                        + exchange.getRequestURI());
                Headers requestHeaders = exchange.getRequestHeaders();
                for (Entry<String, List<String>> header : requestHeaders
                        .entrySet()) {
                    System.out.println(header.getKey() + ": "
                            + header.getValue());
                }
                Reader reader = new BufferedReader(new InputStreamReader(
                        exchange.getRequestBody(), "UTF-8"), 8192);
                StringWriter writer = new StringWriter();
                String r;
                int l;
                char[] buf = new char[8192];
                while ((l = reader.read(buf)) != -1) {
                    writer.write(buf, 0, l);
                }
                r = writer.toString();
                System.out.println();
                System.out.println(r);
                exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, 0);
                exchange.close();
            }
        });
        httpServer.start();*/
    }


    @After
    public void tearDown() throws Exception {
        if (httpServer != null)
            httpServer.stop(0);
    }


    @Test
    public void testPathParam() throws InterruptedException {
        httpServer.createContext("/test", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                assertEquals("GET", exchange.getRequestMethod());
                assertEquals("/test/1 2", exchange.getRequestURI().getPath());
                assertEquals("p1=a+b", exchange.getRequestURI().getQuery());
                assertEquals(HttpClient.APPLICATION_JSON_UTF8, 
                        exchange.getRequestHeaders().get("Content-Type").get(0));

                exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, 0);
                exchange.close();
            }
        });
        httpServer.start();
        Thread.sleep(500);
        
        HttpClient c = new HttpClient("http://localhost:3000/test/{id}")
                .contentType(HttpClient.APPLICATION_JSON_UTF8).addPathParam(
                        "id", "1 2")
                .addQueryParam("p1", "a b")
                .get();
        assertTrue(c.code() == HttpURLConnection.HTTP_OK);
    }
    
    
    @Test
    public void testPOST() throws InterruptedException {
        httpServer.createContext("/testPOST", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
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
                assertEquals("id=1+2+3&param2=%E2%82%AC", requestEncoded);
                String request = "";
                if (requestEncoded.length() > 0)
                    request = URLDecoder.decode(requestEncoded, "UTF-8");
                assertEquals("id=1 2 3&param2=\u20AC", request);
                
                exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, 0);
                
                String response = "true";
                exchange.getResponseBody().write(response.getBytes());
                exchange.getResponseBody().close();
                exchange.close();
            }
        });
        httpServer.start();
        Thread.sleep(1000);
        
        HttpClient c = new HttpClient("http://localhost6:3000/testPOST")
                .contentType(HttpClient.APPLICATION_FORM_URLENCODED_UTF8)
                .addBodyParam("id", "1 2 3")
                .addBodyParam("param2", "\u20AC")
                .post();
        assertTrue(c.code() == HttpURLConnection.HTTP_OK);
        assertEquals("true", (String)c.content());
    }

}
