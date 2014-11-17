package it.idsolutions.util;

import org.json.JSONException;
import org.json.JSONObject;


public class JsonOrgAdapter implements HttpClient.DataAdapter {
    @Override
    public String serialize(Object content) {
        if (!(content instanceof JSONObject))
            throw new UnsupportedOperationException("Only JSONObject type is supported for serialization");
        return content.toString();
    }

    @Override
    public <T> T deserialize(String content, Class<T> type) {
        if (type != JSONObject.class)
            throw new UnsupportedOperationException("Only JSONObject type is supported for deserialization");
        try {
            return (T)new JSONObject(content);
        } catch (JSONException ex) {
            throw new RuntimeException(ex);
        }
    }
    
    @Override
    public <T> T deserialize(String content, Object type) {
        if (type != JSONObject.class)
            throw new UnsupportedOperationException("Only JSONObject type is supported for deserialization");
        try {
            return (T)new JSONObject(content);
        } catch (JSONException ex) {
            throw new RuntimeException(ex);
        }
    }
}
