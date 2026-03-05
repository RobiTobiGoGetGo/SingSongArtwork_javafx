package com.example.singsongartwork;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SortFieldTest {

    @Test
    void fromStringIsCaseInsensitiveAndTrimmed() {
        assertEquals(SortField.FILENAME, SortField.fromString(" filename "));
        assertEquals(SortField.TITLE, SortField.fromString("Title"));
        assertEquals(SortField.ARTIST, SortField.fromString("ARTIST"));
    }

    @Test
    void fromStringRejectsUnsupportedValueWithHelpfulMessage() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> SortField.fromString("foo"));

        assertTrue(ex.getMessage().contains("Invalid sort field"));
        assertTrue(ex.getMessage().contains("filename,title,artist"));
    }
}
