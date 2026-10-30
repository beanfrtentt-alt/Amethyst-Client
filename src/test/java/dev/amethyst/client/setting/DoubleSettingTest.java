package dev.amethyst.client.setting;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonPrimitive;

class DoubleSettingTest {
    @Test
    void clampsAndSnapsToStep() {
        DoubleSetting setting = new DoubleSetting("amount", "Amount", "Test setting", 1.0, 0.0, 5.0, 0.5, 1);
        setting.set(2.24);
        assertEquals(2.0, setting.get(), 0.0001);
        setting.set(99.0);
        assertEquals(5.0, setting.get(), 0.0001);
    }

    @Test
    void loadsDoubleFromJson() {
        DoubleSetting setting = new DoubleSetting("amount", "Amount", "Test setting", 1.0, 0.0, 5.0, 0.5, 1);
        assertTrue(setting.fromJson(new JsonPrimitive(3.5)));
        assertEquals(3.5, setting.get(), 0.0001);
    }
}
