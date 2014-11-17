package it.idsolutions.util;

import com.fasterxml.jackson.core.type.TypeReference;
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
    
    /**
     * Deserialize implementation. Use this method in case of type erasure.
     * 
     * @param <T> The expected result type.
     * @param content JSON string
     * @param type Must be a TypeReference (i.e.: 
     *          <code>new TypeReference&lt;List&lt;MyObject&gt;&gt;(){}</code>)
     * @return 
     */
    @Override
    public <T> T deserialize(String content, Object type) {
        try {
            return new ObjectMapper().readValue(content, (TypeReference)type);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }
}
