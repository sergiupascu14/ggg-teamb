# AI Agent Quick Reference - Desk Allocation System

## TL;DR for AI Agents

This is a desk allocation tracking system for a multi-floor office with two buildings. The Excel file contains desk assignments linked to employee records.

## Core Concepts

### Desk ID Format
```
{Building}{Floor}-{Zone}{Row}-{DeskNum}
Examples: T5-A1-08, T4-B2-15, R3-C1-03
```

- **Building**: T=Tower, R=Riviera
- **Floor**: 3, 4, 5, or 6
- **Zone**: A, B, C, D (letter)
- **Row**: 1, 2, 3 (number)
- **DeskNum**: 00-99 (two digits)

### Buildings & Floors
- **Tower**: Floors 3, 4, 5, 6
- **Riviera**: Floors 3, 4, 5

## Excel Structure

### Primary Sheets (User Edits)

**Floor Sheets**: `Tower_3`, `Tower_4`, `Tower_5`, `Tower_6`, `Riviera_3`, `Riviera_4`, `Riviera_5`

| Column | Name | Type | Purpose |
|--------|------|------|---------|
| A | Building | Static | `T` or `R` |
| B | Floor | Static | Floor number |
| C | Area | Static | Zone letter |
| D | Desk Number | Static | 2-digit desk # |
| E | Row | Static | Row number |
| F | Desk number | Formula | Full desk ID (auto) |
| **G** | **Associate Staff ID** | **INPUT** | **← Primary input field** |
| H | Associate Name | Formula | Auto from lookup |
| I | 1st line Supervisor | Formula | Auto from lookup |
| J | Desk Status | Formula | Auto calculated |

**All Associates Sheet**: Master employee list

| Column | Name | Type | Purpose |
|--------|------|------|---------|
| **A** | **Marca (Staff ID)** | **PRIMARY KEY** | Unique employee ID |
| B | Nume si prenume | Data | Full name |
| C | Stare | Data | Status (Activ/Suspendat) |
| D-G | Supervisors & Dept | Data | Org hierarchy |
| H | Sub CC | Data | Cost center |
| I | Check Allocation | Formula | Count of assignments |
| J | Status Check | Formula | Validation status |

### Lookup Mechanism

```
1. User enters Staff ID in floor sheet Column G
2. XLOOKUP formula in Column H looks up Staff ID in "All Associates" Column A
3. Returns name from "All Associates" Column B
4. Similar for supervisor in Column I
```

## Data Validation

**Check for issues:**
1. **Duplicates**: Staff ID appears in multiple desks → Check "All Associates" Column J for "Duplicate"
2. **Not Allocated**: Employee has no desk → Column J shows "Not Allocated"
3. **Invalid Staff ID**: No matching record in "All Associates" → Name cells show error

## For Android App Integration

### Recommended Query Pattern

```python
# To get all desk assignments:
for each floor_sheet in ['Tower_3', 'Tower_4', ...]:
    for each row in sheet (starting row 2):
        if row[G] is not None:  # Has staff ID
            desk_assignment = {
                'desk_id': row[F],
                'staff_id': row[G],
                'name': row[H],
                'supervisor': row[I],
                'status': row[J]
            }

# To get employee info:
for each row in 'All Associates' (starting row 2):
    employee = {
        'staff_id': row[A],
        'name': row[B],
        'status': row[C],
        'supervisor': row[D],
        'cost_center': row[H]
    }
```

### Key Fields for Mobile App

**Desk List View:**
- Desk ID (Column F)
- Status (Column J): "Allocated" / "Available"
- Assigned Name (Column H)

**Employee Search:**
- Staff ID (All Associates Column A)
- Name (All Associates Column B)
- Assigned Desk (lookup where staff_id appears in floor sheets Column G)

**Filter Options:**
- Building (T/R)
- Floor (3-6)
- Zone (A-D)
- Status (Allocated/Available)

## Quick Actions for AI

### To find where an employee sits:
1. Get Staff ID
2. Search Column G across all floor sheets
3. Return corresponding Desk ID from Column F

### To find who sits at a desk:
1. Parse desk ID to get building/floor
2. Open corresponding sheet (e.g., T4 → Tower_4)
3. Find row where Column F matches desk ID
4. Return Staff ID from Column G and Name from Column H

### To assign a desk:
1. Verify Staff ID exists in "All Associates" Column A
2. Check status is "Activ" in Column C
3. Verify target desk's Column G is empty
4. Write Staff ID to floor sheet Column G
5. Formulas auto-update Columns H, I, J

### To unassign a desk:
1. Find desk assignment in floor sheet
2. Clear Column G (Staff ID)
3. Formulas auto-clear Columns H, I, J

## Data Integrity Rules

✓ **One desk per employee** (enforced by "All Associates" Column J)
✓ **One employee per desk** (business rule, not enforced by Excel)
✓ **Only active employees** can have desks (Status = "Activ")
✓ **Staff ID must exist** in "All Associates" before assignment

## File Information

- **Original**: `Desk_Allocation.xlsx` (real names - DO NOT COMMIT)
- **Anonymized**: `Desk_Allocation_Anonymized.xlsx` (fake names - safe for demos/testing)
- **Floor Plan**: `floor_plan_T4.png` (visual reference for Tower Floor 4)

## Common Queries

**Q: How many desks are available on Tower Floor 4?**
```python
count(Tower_4.Column[J] where value == "Available")
```

**Q: Who is Staff ID 1234's supervisor?**
```python
lookup("All Associates", Column A == 1234, return Column D)
```

**Q: What desks are in Zone A on Tower Floor 5?**
```python
filter(Tower_5, Column C == "A")
```

**Q: Which employees have no desk?**
```python
filter("All Associates", Column J == "Not Allocated")
```

## Error Handling

| Error | Cause | Solution |
|-------|-------|----------|
| `#N/A` in name column | Invalid Staff ID | Check "All Associates" for that ID |
| "Duplicate" status | Staff ID in multiple desks | Remove extra assignments |
| Empty desk ID | Missing formula | Copy formula from row above |
| Wrong building code | Manual entry error | Should be only T or R |

## Performance Notes

- **Total Associates**: ~648 employees
- **Total Desks**: ~400-500 desks across all floors
- **Sheets**: 12 sheets total (7 floor, 1 master, 4 summary)
- **Formula Dependencies**: Formulas in floor sheets depend on "All Associates"

---

**For detailed documentation**: See `DESK_ALLOCATION_FORMAT.md`
**For technical questions**: Refer to Excel formulas in Column F-J of any floor sheet
