package com.example

import com.example.util.RutUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RutValidationUnitTest {

    @Test
    fun testRutClean() {
        assertEquals("123456785", RutUtils.cleanRut("12.345.678-5"))
        assertEquals("15000005K", RutUtils.cleanRut("15.000.005-k"))
        assertEquals("111111111", RutUtils.cleanRut(" 11.111.111 - 1 "))
    }

    @Test
    fun testRutFormatting() {
        assertEquals("12.345.678-5", RutUtils.formatRut("123456785"))
        assertEquals("15.000.005-K", RutUtils.formatRut("15000005k"))
        assertEquals("1.111.111-1", RutUtils.formatRut("11111111"))
    }

    @Test
    fun testRutDvCalculation() {
        assertEquals('5', RutUtils.calculateDv("12345678"))
        assertEquals('1', RutUtils.calculateDv("11111111"))
        assertEquals('0', RutUtils.calculateDv("19876543"))
        assertEquals('K', RutUtils.calculateDv("15000005"))
    }

    @Test
    fun testValidRuts() {
        assertTrue(RutUtils.isValid("12.345.678-5"))
        assertTrue(RutUtils.isValid("12345678-5"))
        assertTrue(RutUtils.isValid("19.876.543-0"))
        assertTrue(RutUtils.isValid("15.000.005-K"))
        assertTrue(RutUtils.isValid("11.111.111-1"))
    }

    @Test
    fun testInvalidRuts() {
        assertFalse(RutUtils.isValid("12.345.678-0")) // wrong DV (should be 5)
        assertFalse(RutUtils.isValid("123"))          // too short
        assertFalse(RutUtils.isValid(""))             // empty
        assertFalse(RutUtils.isValid("12345678-9"))   // wrong DV
        assertFalse(RutUtils.isValid("19.876.543-2")) // wrong DV (should be 0)
    }
}
