# Keywi Context Engine

The Context Engine is the shared policy layer for features that need to understand the Android editor they are operating inside.

`InputContext` captures facts from `EditorInfo` and the active selection. `ContextEnginePolicy` converts those facts plus user preferences into capabilities such as suggestion eligibility, selection-transform eligibility, structured-token handling, newline support, and IME-action preference.

Consumers should ask the policy for capabilities instead of re-implementing password, field-type, or selection checks locally. This keeps Suggestions, Advanced Characters, and the ✨ Tools surface consistent as the engine grows.
