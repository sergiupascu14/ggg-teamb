## ADDED Requirements

### Requirement: Generate a feedback draft from an uploaded photo
The system SHALL analyze an uploaded feedback photo and generate a suggested issue description and detected issue label for the user before submission.

#### Scenario: Successful photo analysis
- **WHEN** the user uploads a photo in the AI-assisted reporting flow and analysis completes successfully
- **THEN** the system presents a generated description and detected issue label in the feedback draft

#### Scenario: Analysis is still running
- **WHEN** the user uploads a photo and the analysis request is in progress
- **THEN** the system shows that draft generation is running and keeps the user in the reporting flow until results or failure are available

### Requirement: Suggest a category from the detected issue
The system SHALL map the detected issue from photo analysis to a supported feedback category and prefill that category when confidence is sufficient.

#### Scenario: Confident category suggestion
- **WHEN** the analysis detects an issue with enough confidence to map it to a supported category
- **THEN** the system pre-selects the suggested category in the feedback draft

#### Scenario: Low-confidence category suggestion
- **WHEN** the analysis cannot confidently map the detected issue to a supported category
- **THEN** the system leaves the category unset and asks the user to choose one manually

### Requirement: Keep AI-generated output editable
The system SHALL let the user review and override any generated description, detected issue, or category before submitting feedback.

#### Scenario: User accepts generated output
- **WHEN** the user reviews the generated draft and submits without changes
- **THEN** the system records the AI-populated values as the submitted feedback content

#### Scenario: User overrides generated output
- **WHEN** the user edits the generated description or changes the suggested category before submitting
- **THEN** the system records the user-edited values instead of the original suggestions

### Requirement: Preserve manual fallback when analysis is unavailable
The system SHALL keep photo-based feedback submission available even when AI analysis fails, times out, or is disabled.

#### Scenario: Analysis unavailable
- **WHEN** the image-analysis service is unavailable or returns an error
- **THEN** the system informs the user that suggestions could not be generated and allows manual feedback entry with the uploaded photo still attached

#### Scenario: Analysis disabled for current implementation
- **WHEN** the active build is configured without a live AI model and the user uploads a photo
- **THEN** the system uses the configured fallback behavior and still lets the user continue the feedback flow
