package com.dessalines.thumbkey.inputcontext

/**
 * Architectural guardrails for the next Context Engine increments.
 *
 * Keep policy pure and editor facts immutable. Runtime consumers should ask for capabilities,
 * then perform Android InputConnection work themselves. This keeps the engine testable and lets
 * Advanced Characters and the Tools surface share the same decisions without UI dependencies.
 */
internal object ContextEngineNotes
