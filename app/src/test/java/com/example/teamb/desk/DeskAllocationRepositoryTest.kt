package com.example.teamb.desk

import com.example.teamb.data.desk.DeskAllocationRepository
import com.example.teamb.data.model.Desk
import com.example.teamb.data.model.Employee
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DeskAllocationRepositoryTest {

    private val employees = listOf(
        Employee("1001", "Alice Anderson", "active", "Carol Chief"),
        Employee("1002", "Bob Brown", "active", "Carol Chief"),
        Employee("2050", "Carol Chief", "active", ""),
    )

    private val desks = listOf(
        Desk("T6-C2-01", "T", 6, "C", 2, "01", "1001"),
        Desk("R3-A1-05", "R", 3, "A", 1, "05", "1002"),
        Desk("T4-B1-09", "T", 4, "B", 1, "09", null), // unassigned
    )

    private val repo = DeskAllocationRepository(employees, desks)

    @Test
    fun searchBlankReturnsAll() {
        assertEquals(employees, repo.searchEmployees(""))
        assertEquals(employees, repo.searchEmployees("   "))
    }

    @Test
    fun searchByNameCaseInsensitive() {
        assertEquals(listOf(employees[0]), repo.searchEmployees("alice"))
        assertEquals(listOf(employees[0]), repo.searchEmployees("ANDERSON"))
    }

    @Test
    fun searchByStaffId() {
        assertEquals(listOf(employees[1]), repo.searchEmployees("1002"))
    }

    @Test
    fun searchPartialMatchesMultiple() {
        // "100" matches 1001 and 1002 by id.
        val result = repo.searchEmployees("100")
        assertTrue(result.contains(employees[0]))
        assertTrue(result.contains(employees[1]))
    }

    @Test
    fun searchNoMatchReturnsEmpty() {
        assertTrue(repo.searchEmployees("zzz-nobody").isEmpty())
    }

    @Test
    fun employeeByIdResolves() {
        assertEquals(employees[2], repo.employeeById("2050"))
        assertNull(repo.employeeById("9999"))
    }

    @Test
    fun displayNameResolves() {
        assertEquals("Alice Anderson", repo.displayName("1001"))
        assertNull(repo.displayName(null))
        assertNull(repo.displayName("9999"))
    }

    @Test
    fun deskForStaffReturnsAssignedDesk() {
        assertEquals(desks[0], repo.deskForStaff("1001"))
        assertEquals(desks[1], repo.deskForStaff("1002"))
    }

    @Test
    fun deskForStaffNullWhenUnassigned() {
        // Carol has no desk row at all.
        assertNull(repo.deskForStaff("2050"))
    }

    @Test
    fun floorsForBuilding() {
        assertEquals(listOf(3, 4, 5, 6), repo.floorsFor("T"))
        assertEquals(listOf(3, 4, 5), repo.floorsFor("R"))
        assertTrue(repo.floorsFor("X").isEmpty())
    }

    @Test
    fun buildingsEnumerated() {
        assertEquals(2, repo.buildings().size)
    }

    @Test
    fun parseDeskIdDelegates() {
        assertEquals("T6-C2-01", repo.parseDeskId("t6-c2-01")!!.canonical)
        assertNull(repo.parseDeskId("nope"))
    }
}
