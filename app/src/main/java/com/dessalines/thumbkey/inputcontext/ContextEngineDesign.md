# Context Engine design intent

The engine should remain a lightweight capability classifier, not a second text processor. Android editor state is captured once, translated into stable facts, and evaluated against user preferences. Keyboard features then consume those capabilities independently.
