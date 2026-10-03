Establish the rules

Before any analysis, the AI must follow these rules:

Do not modify, refactor, reorganize, or rewrite anything yet.

Do not infer architecture from folder names.

Do not assume my description of the engine is correct.

Trace the actual code, callers, dependencies, ownership, data flow, and runtime flow.

When something is uncertain, mark it as Unknown and identify the exact code that must be inspected to determine it.

Separate:

Observed: directly confirmed from source code.
Inferred: strongly implied by the code.
Unknown: cannot currently be established.

Do not recommend a new architecture until the current architecture has been documented.