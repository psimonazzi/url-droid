package it.idsolutions.util;

import com.google.gson.*;

import java.lang.reflect.Type;
import java.util.Date;


public class GsonAdapter implements HttpClient.DataAdapter {
    @Override
    public String serialize(Object content, Type type) {
        // Serialize dates as Unix timestamp notation, for interoperability
        // with Jackson-RS
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

    @Override
    public Object deserialize(String content, Type type) {
        // Deserialize dates as Unix timestamp notation, for
        // interoperability with Jackson-RS
        Gson gson = new GsonBuilder().registerTypeAdapter(Date.class,
                new JsonDeserializer<Date>() {
                    @Override
                    public Date deserialize(JsonElement je, Type type,
                                            JsonDeserializationContext context)
                            throws JsonParseException {
                        return new Date(je.getAsLong());
                    }
                }).create();
        return gson.fromJson(content, type);
    }
}
