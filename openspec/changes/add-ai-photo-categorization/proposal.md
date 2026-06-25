## Why

The feedback flow currently depends on the user to describe the issue and choose a category manually even when they already have a photo of the problem. Adding AI-assisted image interpretation makes reporting faster, reduces categorization errors, and creates a cleaner demo of end-to-end issue intake.

## What Changes

- Add a simplified photo-first issue reporting flow where the user uploads an image and the app generates a suggested issue description, detected issue label, and category.
- Introduce an image-analysis service abstraction that can be backed by an on-device model or a remote AI provider without changing the submission flow.
- Let the user review and override AI-generated results before submitting so the feature remains assistive rather than fully automatic.
- Preserve a non-blocking fallback path so users can still submit feedback manually when image analysis is unavailable or low-confidence.

## Capabilities

### New Capabilities
- `photo-issue-categorization`: Analyze an uploaded feedback photo, generate a suggested description, identify the likely issue, and map it to a feedback category using either a local model or a pluggable AI-backed service.

### Modified Capabilities
<!-- None — there are no mainline OpenSpec capabilities in openspec/specs yet. -->

## Impact

- **Affected code**: feedback submission UI, image upload/preview flow, ViewModels/use-cases that prepare feedback drafts, and repository/integration code for image analysis.
- **Dependencies**: an image-analysis abstraction plus one concrete implementation path (local/on-device model or remote AI service), and any model/runtime packaging needed for the selected implementation.
- **Systems**: local model assets and inference runtime or a networked AI provider, both hidden behind the same interface so the demo can switch implementations safely.
- **UX / data flow**: users move from manual-first reporting to a photo-assisted draft flow where generated description, issue, and category are editable before final submission.
