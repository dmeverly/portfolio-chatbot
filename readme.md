<p align="center">
  <img src="src/assets/emblem-mono-light.png" width="84" alt="" />
</p>

# Everlybot (v2.1) — Reference Implementation for SynapSys

**Author**: David Everly  
**Languages**:
- **TypeScript / Node.js** — Everlybot (edge application)
- **Java / Spring Boot** — SynapSys (LLM broker & guard framework)  
**Status**: Active development  
Copyright © 2025 David Everly  

Everlybot is a **production-style reference implementation** demonstrating how
**untrusted user input can be safely exposed to large language models** using
**failure-aware orchestration and explicit trust boundaries**.

While the system answers questions about my professional background, its primary
purpose is to showcase **controlled LLM exposure**, not conversational novelty.

This repository contains the **public edge application**.
LLM orchestration, guard logic, and policy enforcement are delegated to SynapSys.

---

## Purpose  

Everlybot exists to demonstrate how a public-facing application can:

- accept untrusted user input
- expose LLM capabilities safely
- preserve deterministic system behavior
- fail predictably under adversarial conditions

It prioritizes **containment, separation of responsibility, and reliability**
over open-ended or creative generation.

---

## Motivation

Most public-facing chatbots fail in predictable ways:

- hallucinated biographical or professional details
- leakage of system prompts or retrieval context
- reliance on informal prompt “rules” for safety
- fragile behavior under malformed or adversarial input

This project explores a more disciplined architecture:

- a **thin, untrusted edge application**
- **centralized LLM orchestration and policy enforcement**
- **explicit guard stages before and after model execution**
- **deterministic fallback behavior on policy violation**

The goal is not novelty — it is **predictable failure modes**.

---

## High-Level Architecture

```
Browser / Client
       │
       ▼
 Everlybot 
       │   (authenticated, minimal payload)
       ▼
 SynapSys 
       │
       ├─ Pre-LLM Guards
       ├─ Strategy-Based Model Selection
       ├─ Prompt Assembly & Grounding
       ├─ LLM Execution
       └─ Post-LLM Guards
       │
       ▼
 Controlled Response / Safe Fallback
```

---

## Component Responsibilities

### Everlybot (Edge Application)

Everlybot is intentionally **thin and untrusted**.

Responsibilities:

* accept user input
* forward **only** `{ content, context }` to SynapSys
* authenticate itself using an HMAC-symmetric signing key
* return SynapSys responses

Everlybot explicitly **does not**:

- select LLM providers or models
- load system prompts
- apply guard logic
- inject retrieval context

End users are **not authenticated by SynapSys** — only upstream applications are.
This enforces a hard trust boundary.

---

### SynapSys (Broker & Guard Framework)

SynapSys owns **all LLM-related intelligence**:

- provider and model selection
- system instruction loading
- grounded context assembly
- pre- and post-execution guard pipelines
- response validation and controlled fallback

Everlybot cannot override or bypass this behavior.

---

## Guard Pipeline & Failure Awareness

All requests pass through **explicit, ordered guard stages**.

### Pre-LLM Guards

Executed before any model call:

- system prompt extraction attempts
- instruction replay or encoding attacks
- disallowed or illegal request classes
- malformed or abusive input

Violations short-circuit execution.

---

### Post-LLM Guards

Executed on model output:

- prompt or policy leakage
- unsafe or disallowed content
- hallucinated personal data
- malformed or non-conforming responses

Violations result in **deterministic fallback responses** —
raw model output is never returned on violation.

---

## Grounded Retrieval (No Black-Box RAG)

Responses are grounded using **curated Markdown documents** covering:

- education
- professional history
- projects
- certifications

Context injection is **explicit and auditable**.
This avoids opaque vector databases while preserving predictable behavior.

---

## Security Model

The system is designed under the assumption that it is
**publicly reachable and adversarially probed**.

### Network & Deployment

- deployed behind a managed reverse tunnel
- no inbound ports exposed
- Everlybot is the only Internet-facing service
- SynapSys binds to localhost and is not externally reachable

### Application-Level Controls

- strict request size limits
- minimal `/health` endpoints
- no sensitive data in logs or responses
- centralized authentication at the broker boundary

### Authentication

Everlybot authenticates to SynapSys using **HMAC request signing**.
Requests are bound to:

- HTTP method
- request path
- request body
- timestamp
- nonce

This prevents replay, tampering, and credential reuse.
No bearer secrets are transmitted over the wire.

---

## Abuse & Regression Testing

A dedicated test suite simulates:

- prompt injection
- system prompt exfiltration
- context poisoning
- role or privilege escalation
- illegal or unsafe requests
- partial model compliance under load

Tests run against a **deterministic stub LLM**, ensuring:

- zero real token usage
- reproducible guard behavior
- immediate detection of regressions

If a future refactor reintroduces leakage, tests fail loudly.

---

## Code Structure (High Level)

### Everlybot (TypeScript)

- `src/app.ts` — Express entrypoint
- `src/config` — private configuration loader
- `src/types.ts` — shared message contracts

### SynapSys (Java)

- `service.guard` — guard pipeline
- `service.llm` — provider abstraction
- `strategy` — model and provider selection
- `api` — broker interface

Private guard rules and system instructions are intentionally excluded.

---

## What This Demonstrates

This project demonstrates:

- explicit trust boundaries around LLM usage
- centralized, testable policy enforcement
- deterministic handling of unsafe or malformed output
- safe exposure of LLM systems to untrusted clients

These constraints mirror real-world requirements in
**healthcare, security, and regulated environments**,
where uncontrolled output introduces downstream risk.

---

## Limitations & Future Work

Current limitations:

- retrieval is non-vectorized
- guards are rule-based rather than statistical
- no session or memory persistence

Planned work:

- optional vector database integration
- expanded guard taxonomy
- containerized deployment and CI hardening

---

## License

This project is licensed under the **Apache License 2.0**.

You are free to use, modify, and distribute this software, including for
commercial purposes, subject to the terms of the license.