package it.idsolutions.util;

import java.io.IOException;
import java.lang.reflect.Type;
import org.codehaus.jackson.map.ObjectMapper;


public class JacksonAdapter implements HttpClient.DataAdapter {
    @Override
    public String serialize(Object content, Type type) {
        try {
            return new ObjectMapper().writeValueAsString(content);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public Object deserialize(String content, Type type) {
        try {
            return new ObjectMapper().readValue(content, type.getClass());
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }
}
