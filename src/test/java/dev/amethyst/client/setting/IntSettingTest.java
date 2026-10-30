package dev.amethyst.client.setting;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonPrimitive;

class IntSettingTest {
    @Test
    void clampsValuesToConfiguredRange() {
        IntSetting setting = new IntSetting("count", "Count", "Test setting", 5, 1, 10);
        setting.set(100);
        assertEquals(10, setting.get());
        setting.set(-100);
        assertEquals(1, setting.get());
    }

    @Test
    void loadsIntegerFromJson() {
        IntSetting setting = new IntSetting("count", "Count", "Test setting", 5, 1, 10);
        assertTrue(setting.fromJson(new JsonPrimitive(8)));
        assertEquals(8, setting.get());
    }

    @Test
    void rejectsInvalidJson() {
        IntSetting setting = new IntSetting("count", "Count", "Test setting", 5, 1, 10);
        assertFalse(setting.fromJson(null));
        assertEquals(5, setting.get());
    }
}
