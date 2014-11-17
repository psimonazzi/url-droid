package test;

import it.idsolutions.util.JsonOrgAdapter;
import java.util.ArrayList;
import org.json.JSONObject;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author ps
 */
public class JsonOrgTest {
    
    public JsonOrgTest() {
    }
    

    @Test
    public void testAdapter() throws Exception {
        JsonOrgAdapter a = new JsonOrgAdapter();
        JSONObject t = new JSONObject();
        t.put("i", 42);
        t.put("s", "test");
        JSONObject t2 = new JSONObject();
        t2.put("l", 999999999L);
        t.put("o", t2);
        t.put("list", new ArrayList<String>() {
            {
                add("e1");
                add("e2");
            }
        });
        String json = a.serialize(t);
        JSONObject r = a.deserialize(json, JSONObject.class);
        assertEquals(r.getInt("i"), 42);
        assertEquals(r.getString("s"), "test");
        assertEquals(r.getJSONObject("o").getLong("l"), 999999999L);
        assertEquals(r.getJSONArray("list").get(0), "e1");
        assertEquals(r.getJSONArray("list").length(), 2);
    }
}
