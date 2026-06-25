## ADDED Requirements

### Requirement: Check food into the freezer
The system SHALL allow a user to check a food item into the shared freezer, recording the item and the check-in time.

#### Scenario: User checks in an item
- **WHEN** the user adds a food item with a label and confirms check-in
- **THEN** the system stores the item as "in freezer" with the check-in timestamp and owner

### Requirement: Check food out of the freezer
The system SHALL allow a user to check out (remove) one of their food items, marking it as removed.

#### Scenario: User checks out an item
- **WHEN** the user selects one of their in-freezer items and checks it out
- **THEN** the system marks the item as removed with a check-out timestamp and stops tracking it as present

### Requirement: View freezer items
The system SHALL display the user's currently checked-in freezer items with their check-in dates.

#### Scenario: User views their freezer items
- **WHEN** the user opens the freezer screen
- **THEN** the system lists each of the user's in-freezer items with its label and check-in date

### Requirement: Smart freezer cleanup reminders
The system SHALL automatically remind the user to clear out food items that have been in the freezer beyond a configured age threshold.

#### Scenario: Old item triggers reminder
- **WHEN** an in-freezer item's age exceeds the cleanup threshold
- **THEN** the system notifies the user to remove or check out that item
