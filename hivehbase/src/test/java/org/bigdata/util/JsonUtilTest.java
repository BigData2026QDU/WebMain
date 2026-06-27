package org.bigdata.util;

import org.example.Tool.JsonUtil;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class JsonUtilTest {

    @Test
    void toJson_withValidObject_shouldReturnJsonString() {
        TestObject obj = new TestObject("test", 123);
        String json = JsonUtil.toJson(obj);
        assertNotNull(json);
        assertTrue(json.contains("test"));
        assertTrue(json.contains("123"));
    }

    @Test
    void fromJson_withValidJson_shouldReturnObject() {
        String json = "{\"name\":\"test\",\"value\":123}";
        TestObject obj = JsonUtil.fromJson(json, TestObject.class);
        assertNotNull(obj);
        assertEquals("test", obj.name);
        assertEquals(123, obj.value);
    }

    @Test
    void fromJson_withInvalidJson_shouldThrowException() {
        assertThrows(RuntimeException.class, () -> {
            JsonUtil.fromJson("invalid json", TestObject.class);
        });
    }

    static class TestObject {
        public String name;
        public int value;

        public TestObject() {}

        public TestObject(String name, int value) {
            this.name = name;
            this.value = value;
        }
    }
}
