## Context

The office feedback app already includes photo attachments and a long-term direction for AI-assisted issue detection, but the reporting flow still assumes the user will describe and categorize the issue manually. This change adds a focused, photo-first draft experience that turns an uploaded image into a suggested description, issue label, and category before the user submits feedback.

The feature needs to work in the existing local-first Android architecture and stay demo-friendly. The analysis engine must be swappable so the app can use an on-device model, a remote vision provider, or a deterministic mock without changing the UI flow.

## Goals / Non-Goals

**Goals:**
- Let the user upload a photo and receive an AI-generated feedback draft with description, detected issue, and category suggestion.
- Keep the analysis path pluggable so the same feature supports a local model, a hosted AI service, or a mock implementation.
- Make AI output assistive and editable, not authoritative, so the user stays in control before submission.
- Fail gracefully when analysis is slow, unavailable, or low-confidence.

**Non-Goals:**
- Fully automatic submission without user review.
- Multi-image reasoning, video analysis, or generalized visual search.
- A permanent commitment to one model vendor or runtime in this change.
- Replacing the existing manual feedback path.

## Decisions

- **Introduce a dedicated photo categorization contract.** Add a domain interface that accepts an image reference and returns a structured result with generated description, detected issue label, category suggestion, confidence, and failure reason. This keeps UI and repositories independent from whether inference runs on-device or remotely. Alternative considered: calling a model directly from the ViewModel; rejected because it would couple the UI flow to one implementation and make testing harder.
- **Use a photo-first draft state in the feedback flow.** After image selection, the ViewModel requests analysis and exposes loading, success, low-confidence, and unavailable states. On success, the generated fields prefill the feedback form. Alternative considered: running analysis only at submit time; rejected because it hides latency and prevents the user from correcting output before submission.
- **Treat generated fields as suggestions that remain editable.** The app pre-populates description and category, but the user can override either before submitting. If confidence is below threshold, the app surfaces the generated description or issue hint while leaving category unset. Alternative considered: locking AI-generated category when confidence is high; rejected because it increases the risk of incorrect routing.
- **Separate issue detection from category mapping.** The analyzer can emit a free-form issue label, while a mapper converts that label into the app's canonical feedback categories. This keeps prompts/models flexible while preserving stable app-side categories such as Kitchen, Elevators, or Desk Area. Alternative considered: requiring the model to emit only app categories; rejected because it is more brittle across implementations.
- **Keep the feature non-blocking and local-first.** Submission remains available even if the analyzer fails or times out. The repository stores the original photo and the final user-approved fields, while generated metadata is additive. Alternative considered: making analysis required for photo-based reports; rejected because it would make the feature fragile during demos and offline use.

## Risks / Trade-offs

- **Model output is inaccurate or inconsistent** -> keep all generated values editable, use a confidence threshold, and leave category blank when mapping confidence is low.
- **On-device inference increases APK size or device latency** -> hide the runtime behind an interface so the app can switch to a lighter mock or hosted implementation for the demo build.
- **Remote AI adds privacy and connectivity concerns** -> pass only the minimum image payload through the integration boundary, keep it optional, and preserve the full manual path when the network is unavailable.
- **Category mapping drifts from product taxonomy** -> centralize the mapping rules in one component and test them against the supported feedback categories.

## Migration Plan

This is an additive feature. If the implementation introduces new local entities or fields for generated analysis data, use an additive Room migration and default old records to a "no analysis" state. Rollback is straightforward because the manual feedback flow remains intact: remove the analysis trigger and ignore generated metadata.

## Open Questions

- Which implementation should ship first for the demo: on-device model, hosted vision API, or deterministic mock?
- What confidence threshold should suppress automatic category prefilling?
- Should generated descriptions be one sentence only, or can they include a short remediation hint when the model is confident?
