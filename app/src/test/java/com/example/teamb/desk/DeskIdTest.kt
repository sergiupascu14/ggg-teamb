package com.example.teamb.desk

import com.example.teamb.data.model.DeskId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DeskIdTest {

    @Test
    fun parsesValidTowerCode() {
        val id = DeskId.parse("T6-C2-01")
        assertEquals(DeskId("T", 6, "C", 2, "01"), id)
        assertEquals("T6-C2-01", id!!.canonical)
    }

    @Test
    fun parsesValidRivieraCode() {
        val id = DeskId.parse("R3-A1-05")
        assertEquals(DeskId("R", 3, "A", 1, "05"), id)
    }

    @Test
    fun trimsAndUppercasesInput() {
        val id = DeskId.parse("  t4-d3-09  ")
        assertEquals(DeskId("T", 4, "D", 3, "09"), id)
    }

    @Test
    fun towerFloorSixIsValid() {
        assertEquals(6, DeskId.parse("T6-A1-01")!!.floor)
    }

    @Test
    fun rivieraFloorSixIsRejected() {
        assertNull(DeskId.parse("R6-A1-01"))
    }

    @Test
    fun rivieraFloorsThreeToFiveAreValid() {
        assertEquals(3, DeskId.parse("R3-A1-01")!!.floor)
        assertEquals(4, DeskId.parse("R4-A1-01")!!.floor)
        assertEquals(5, DeskId.parse("R5-A1-01")!!.floor)
    }

    @Test
    fun floorBelowThreeIsRejected() {
        assertNull(DeskId.parse("T2-A1-01"))
        assertNull(DeskId.parse("T1-A1-01"))
    }

    @Test
    fun floorAboveSixIsRejected() {
        assertNull(DeskId.parse("T7-A1-01"))
    }

    @Test
    fun unknownBuildingIsRejected() {
        assertNull(DeskId.parse("X6-A1-01"))
    }

    @Test
    fun extendedZonesAreAccepted() {
        // Real data has zones up to H (not just A-D).
        assertEquals("E", DeskId.parse("R3-E1-01")!!.zone)
        assertEquals("F", DeskId.parse("R4-F6-04")!!.zone)
    }

    @Test
    fun nonLetterZoneIsRejected() {
        assertNull(DeskId.parse("T6-12-01"))
    }

    @Test
    fun extendedRowsAreAccepted() {
        // Real data has rows up to 7 (not just 1-3).
        assertEquals(6, DeskId.parse("R4-F6-04")!!.row)
        assertEquals(7, DeskId.parse("T5-B7-01")!!.row)
    }

    @Test
    fun deskNumIsOneOrTwoDigits() {
        assertEquals("1", DeskId.parse("T6-A1-1")!!.deskNum)
        assertEquals("01", DeskId.parse("T6-A1-01")!!.deskNum)
        assertNull(DeskId.parse("T6-A1-100"))
        assertNull(DeskId.parse("T6-A1-XY"))
    }

    @Test
    fun malformedShapesAreRejected() {
        assertNull(DeskId.parse(""))
        assertNull(DeskId.parse("T6"))
        assertNull(DeskId.parse("T6-A1"))
        assertNull(DeskId.parse("T6A101"))
        assertNull(DeskId.parse("T6_A1_01"))
        assertNull(DeskId.parse("T6-A1-01-extra"))
    }

    @Test
    fun canonicalRoundTrips() {
        val raw = "T5-B3-12"
        assertEquals(raw, DeskId.parse(raw)!!.canonical)
    }
}
