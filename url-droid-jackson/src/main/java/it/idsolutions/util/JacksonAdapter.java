package it.idsolutions.util;

import java.io.IOException;
import com.fasterxml.jackson.databind.ObjectMapper;


public class JacksonAdapter implements HttpClient.DataAdapter {
    @Override
    public String serialize(Object content) {
        try {
            return new ObjectMapper().writeValueAsString(content);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public <T> T deserialize(String content, Class<T> type) {
        try {
            return new ObjectMapper().readValue(content, type);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }
}
