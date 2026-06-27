package org.bigdata.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SecurityUtilTest {

    @Test
    void hashPassword_withValidPassword_shouldReturnNonNull() {
        String hash = SecurityUtil.hashPassword("test123");
        assertNotNull(hash);
        assertEquals(64, hash.length());
    }

    @Test
    void hashPassword_withNull_shouldReturnNull() {
        assertNull(SecurityUtil.hashPassword(null));
    }

    @Test
    void hashPassword_sameInput_shouldReturnSameHash() {
        String hash1 = SecurityUtil.hashPassword("password");
        String hash2 = SecurityUtil.hashPassword("password");
        assertEquals(hash1, hash2);
    }

    @Test
    void hashPassword_differentInput_shouldReturnDifferentHash() {
        String hash1 = SecurityUtil.hashPassword("password1");
        String hash2 = SecurityUtil.hashPassword("password2");
        assertNotEquals(hash1, hash2);
    }

    @Test
    void hashPassword_emptyString_shouldReturnHash() {
        String hash = SecurityUtil.hashPassword("");
        assertNotNull(hash);
        assertEquals(64, hash.length());
    }
}
