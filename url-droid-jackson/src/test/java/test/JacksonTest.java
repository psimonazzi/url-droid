package test;

import com.fasterxml.jackson.core.type.TypeReference;
import it.idsolutions.util.JacksonAdapter;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author ps
 */
public class JacksonTest {
    
    public JacksonTest() {
    }
    

    @Test
    public void testAdapter() {
        JacksonAdapter a = new JacksonAdapter();
        Test1 t = new Test1();
        t.i = 42;
        t.s = "test";
        Test2 t2 = new Test2();
        t2.l = 999999999L;
        t.o = t2;
        t.list = new ArrayList<String>() {
            {
                add("e1");
                add("e2");
            }
        };
        String json = a.serialize(t);
        Test1 r = a.deserialize(json, Test1.class);
        assertEquals(t.i, r.i);
        assertEquals(t.s, r.s);
        assertEquals(t.o.l, r.o.l);
        assertEquals(t.list.get(0), r.list.get(0));
        assertEquals(t.list.size(), r.list.size());
    }

    @Test
    public void testTypeErasure() {
        JacksonAdapter a = new JacksonAdapter();
        final Test1 t = new Test1();
        t.i = 42;
        t.s = "test";
        final Test1 t2 = new Test1();
        t2.i = 1;
        t2.s = "test2";
        List<Test1> list = new ArrayList<Test1>() {
            {
                add(t);
                add(t2);
            }
        };
        String json = a.serialize(list);
        List<Test1> r = a.deserializeRef(json,
                new TypeReference<List<Test1>>(){});
        assertEquals(2, r.size());
        assertEquals(42, r.get(0).i);
    }
    
    
    static class Test1 {
        public List<String> list;
        public int i;
        public String s;
        public Test2 o;
    }
    
    static class Test2 {
        public Long l;
    }
}
