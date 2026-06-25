## 1. Analysis contract and domain flow

- [x] 1.1 Add a photo categorization domain contract and result model for generated description, detected issue, category suggestion, confidence, and failure state
- [x] 1.2 Implement the feedback draft orchestration path so photo selection triggers analysis and exposes loading, success, low-confidence, and unavailable states
- [x] 1.3 Add unit tests for the analysis contract, category mapping, and fallback behavior

## 2. Pluggable analyzer implementation

- [x] 2.1 Add a pluggable image-analysis service abstraction with one concrete implementation path for the demo build (mock, local model, or remote AI adapter)
- [x] 2.2 Implement mapping from analyzer issue labels to supported feedback categories and leave category unset when confidence is insufficient
- [x] 2.3 Add tests covering successful analysis, low-confidence suggestions, and analyzer errors/timeouts

## 3. Feedback UI integration

- [x] 3.1 Update the photo upload/reporting flow to show analysis progress and prefill generated description and category suggestions
- [x] 3.2 Keep all AI-generated fields editable and ensure the final submission stores user-overridden values when changed
- [x] 3.3 Add UI/state tests for generated draft display, manual override, and manual fallback when suggestions are unavailable
