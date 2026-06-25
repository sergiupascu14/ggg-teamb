## ADDED Requirements

### Requirement: Select identity from employee directory
The system SHALL present, during onboarding, the list of Garmin employees loaded from the bundled desk allocation dataset and SHALL require the user to select their own identity from it. Selecting an employee SHALL populate the user's Name, Staff ID (used as the `userId`), supervisor, and assigned desk on the local profile.

#### Scenario: User selects their identity
- **WHEN** the user picks their own entry from the employee list and continues
- **THEN** the system stores the selected employee's Name, Staff ID, supervisor, and assigned desk on the local profile

#### Scenario: Selection required
- **WHEN** the user attempts to continue without selecting an employee
- **THEN** the system blocks progress and shows a validation message

#### Scenario: Optional directory pre-highlight
- **WHEN** GarminAD is available and returns a matching profile
- **THEN** the system may pre-highlight the matching employee in the list for the user to confirm, without removing the ability to choose a different entry

### Requirement: Search the employee directory
The system SHALL provide a search field on the identity selection screen that filters the employee list by name or Staff ID as the user types.

#### Scenario: Search filters the list
- **WHEN** the user types into the search field
- **THEN** the system shows only employees whose name or Staff ID matches the query

#### Scenario: No matches
- **WHEN** the search query matches no employees
- **THEN** the system shows an empty-results state and lets the user clear the search

### Requirement: Set up an account password
The system SHALL let the user set a password for their account during onboarding, SHALL store it securely (hashed/encrypted, never in plaintext), and SHALL require it to access the app on subsequent launches.

#### Scenario: User sets a password
- **WHEN** the user enters and confirms a password during onboarding
- **THEN** the system stores the password securely and completes account setup

#### Scenario: Password mismatch
- **WHEN** the password and its confirmation do not match
- **THEN** the system blocks completion and shows a validation message

#### Scenario: Unlock with correct password
- **WHEN** a returning user enters their correct password on the login screen
- **THEN** the system unlocks the app and loads their profile

#### Scenario: Reject wrong password
- **WHEN** a returning user enters an incorrect password
- **THEN** the system denies access and shows an error without revealing the stored password

### Requirement: Sign out
The system SHALL provide a sign-out option that ends the current session and returns to the login screen, while preserving the stored account so the user can sign back in with their password.

#### Scenario: User signs out
- **WHEN** the user selects "Sign out"
- **THEN** the system ends the session and shows the login screen

#### Scenario: User signs back in
- **WHEN** a signed-out user enters their correct password
- **THEN** the system restores their session and profile without requiring re-onboarding

### Requirement: Capture office location
The system SHALL store the user's Building (office location) and Floor Number during onboarding, using the canonical desk allocation enumeration (Tower 3–6, Riviera 3–5).

#### Scenario: User selects building and floor
- **WHEN** the user picks a Building and Floor and continues
- **THEN** the system persists the Building and Floor Number to the local profile

#### Scenario: Required location missing
- **WHEN** the user attempts to continue without selecting a Building
- **THEN** the system blocks progress and shows a validation message

### Requirement: Record specific desk area
The system SHALL record the user's specific desk area identifier (e.g. "T6-C2-01"), validate it against the desk ID grammar `{Building}{Floor}-{Zone}{Row}-{DeskNum}`, and derive the building, floor, and zone from it. When the selected employee already has an assigned desk, the system SHALL pre-fill it.

#### Scenario: Desk area pre-filled from selection
- **WHEN** the selected employee has an assigned desk in the dataset
- **THEN** the system pre-fills the desk area and derives building, floor, and zone from it

#### Scenario: Desk area entered manually
- **WHEN** the user enters a desk area code matching the desk ID grammar and saves
- **THEN** the system stores the desk area string and the derived building, floor, and zone on the user profile

#### Scenario: Invalid desk code
- **WHEN** the user enters a desk code that does not match the grammar (wrong building letter, out-of-range floor, or malformed)
- **THEN** the system rejects it with a validation message and does not store it

### Requirement: Profile persistence
The system SHALL persist the completed profile locally so that onboarding is not shown again on subsequent launches; returning users are prompted for their password rather than re-onboarded.

#### Scenario: Returning user
- **WHEN** a user who has completed onboarding reopens the app
- **THEN** the system shows the login screen and, on correct password, loads the stored profile without re-running onboarding
