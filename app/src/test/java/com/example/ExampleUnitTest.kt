package com.example

import com.example.calculator.AngleMode
import com.example.calculator.ScientificCalculatorEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class ExampleUnitTest {
    @Test
    fun testBasicArithmetic() {
        val res = ScientificCalculatorEngine.evaluate("2+3*4", AngleMode.DEGREE)
        assertTrue(res.isSuccess)
        assertEquals(14.0, res.getOrThrow(), 0.0001)
    }

    @Test
    fun testScientificTrigonometryDegree() {
        val res = ScientificCalculatorEngine.evaluate("sin(90)", AngleMode.DEGREE)
        assertTrue(res.isSuccess)
        assertEquals(1.0, res.getOrThrow(), 0.0001)
    }

    @Test
    fun testScientificTrigonometryRadian() {
        val res = ScientificCalculatorEngine.evaluate("cos(0)", AngleMode.RADIAN)
        assertTrue(res.isSuccess)
        assertEquals(1.0, res.getOrThrow(), 0.0001)
    }

    @Test
    fun testPowersAndRoots() {
        val resPower = ScientificCalculatorEngine.evaluate("2^3", AngleMode.DEGREE)
        assertTrue(resPower.isSuccess)
        assertEquals(8.0, resPower.getOrThrow(), 0.0001)

        val resSqrt = ScientificCalculatorEngine.evaluate("sqrt(16)", AngleMode.DEGREE)
        assertTrue(resSqrt.isSuccess)
        assertEquals(4.0, resSqrt.getOrThrow(), 0.0001)
    }

    @Test
    fun testDivisionByZeroHandledGracefully() {
        val res = ScientificCalculatorEngine.evaluate("10/0", AngleMode.DEGREE)
        assertTrue(res.isFailure)
    }

    @Test
    fun testFormatResultClean() {
        assertEquals("5", ScientificCalculatorEngine.formatResult(5.0))
        assertEquals("3.141593", ScientificCalculatorEngine.formatResult(3.14159265))
    }
}
