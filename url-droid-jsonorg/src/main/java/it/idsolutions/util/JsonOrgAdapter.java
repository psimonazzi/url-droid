package it.idsolutions.util;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Type;


public class JsonOrgAdapter implements HttpClient.DataAdapter {
    @Override
    public String serialize(Object content, Type type) {
        if (type != JSONObject.class || !(content instanceof JSONObject))
            throw new UnsupportedOperationException("Only JSONObject type is supported for serialization");
        return content.toString();
    }

    @Override
    public Object deserialize(String content, Type type) {
        try {
            return new JSONObject(content);
        } catch (JSONException ex) {
            throw new RuntimeException(ex);
        }
    }
}
