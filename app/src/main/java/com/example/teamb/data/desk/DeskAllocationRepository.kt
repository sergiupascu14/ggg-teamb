package com.example.teamb.data.desk

import android.content.Context
import com.example.teamb.data.model.Building
import com.example.teamb.data.model.Desk
import com.example.teamb.data.model.DeskId
import com.example.teamb.data.model.Employee
import org.json.JSONObject

/**
 * Canonical local source of truth for the office layout and employee directory,
 * loaded from the bundled (anonymized) desk allocation asset.
 *
 * The lookups are pure and constructed from plain lists so they unit-test without Android;
 * [fromAsset] performs the Android-only JSON read.
 */
class DeskAllocationRepository(
    val employees: List<Employee>,
    val desks: List<Desk>,
) {
    private val employeesById: Map<String, Employee> = employees.associateBy { it.staffId }
    private val deskByStaffId: Map<String, Desk> =
        desks.filter { it.staffId != null }.associateBy { it.staffId!! }

    fun employeeById(staffId: String): Employee? = employeesById[staffId]

    fun displayName(staffId: String?): String? =
        staffId?.let { employeesById[it]?.name }

    /** Desk assigned to the given employee in the dataset, if any. */
    fun deskForStaff(staffId: String): Desk? = deskByStaffId[staffId]

    /** Case-insensitive search over employee name or staff id. Blank query returns all (name-sorted). */
    fun searchEmployees(query: String): List<Employee> {
        val q = query.trim()
        if (q.isEmpty()) return employees
        val lower = q.lowercase()
        return employees.filter {
            it.name.lowercase().contains(lower) || it.staffId.contains(lower)
        }
    }

    /** Canonical building enumeration (Tower, Riviera). */
    fun buildings(): List<Building> = Building.entries

    /** Canonical floors for a building code (Tower 3-6, Riviera 3-5). */
    fun floorsFor(buildingCode: String): List<Int> =
        Building.fromCode(buildingCode)?.floors?.toList() ?: emptyList()

    val zones: List<String> = listOf("A", "B", "C", "D")

    fun parseDeskId(raw: String): DeskId? = DeskId.parse(raw)

    companion object {
        fun fromAsset(context: Context, assetName: String = "desk_allocation.json"): DeskAllocationRepository {
            val json = context.assets.open(assetName).bufferedReader().use { it.readText() }
            return fromJson(json)
        }

        fun fromJson(json: String): DeskAllocationRepository {
            val root = JSONObject(json)
            val empArr = root.getJSONArray("employees")
            val employees = ArrayList<Employee>(empArr.length())
            for (i in 0 until empArr.length()) {
                val o = empArr.getJSONObject(i)
                employees.add(
                    Employee(
                        staffId = o.getString("staffId"),
                        name = o.optString("name"),
                        status = o.optString("status"),
                        supervisor = o.optString("supervisor"),
                    )
                )
            }
            val deskArr = root.getJSONArray("desks")
            val desks = ArrayList<Desk>(deskArr.length())
            for (i in 0 until deskArr.length()) {
                val o = deskArr.getJSONObject(i)
                desks.add(
                    Desk(
                        deskId = o.getString("deskId"),
                        building = o.getString("building"),
                        floor = o.getInt("floor"),
                        zone = o.getString("zone"),
                        row = o.getInt("row"),
                        deskNum = o.getString("deskNum"),
                        staffId = if (o.isNull("staffId")) null else o.getString("staffId"),
                    )
                )
            }
            return DeskAllocationRepository(employees, desks)
        }
    }
}
