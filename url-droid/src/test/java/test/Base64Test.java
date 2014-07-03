package test;

import static org.junit.Assert.*;
import it.idsolutions.util.Base64;

import org.junit.Test;

public class Base64Test {

    @Test
    public void testEncode() {
        String toEncode = "Admin" + ":" + "!_xXx_!";
        assertEquals("QWRtaW46IV94WHhfIQ==", Base64.encodeString(toEncode).trim());
        
        toEncode = "a1" + ":" + "1234567890";
        assertEquals("YTE6MTIzNDU2Nzg5MA==", Base64.encodeString(toEncode).trim());
    }

}
