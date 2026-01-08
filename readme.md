# Portfolio Chatbot (v2) — Reference Implementation for SynapSys

**Author**: David Everly
**Languages**:

* **TypeScript / Node.js** — Everlybot (edge application)
* **Java / Spring Boot** — SynapSys (LLM broker & guard framework)

**Status**: Active
**Version**: v2 — security-hardened rewrite

---

## Overview

The **Portfolio Chatbot** is a production-style conversational system designed to answer questions about my professional background, projects, and experience.

More importantly, it serves as a **reference implementation for SynapSys**, demonstrating how a **failure-aware LLM orchestration framework** can be exposed to untrusted users while maintaining strict behavioral guarantees.

Version 2 is a deliberate redesign focused on:

* explicit trust boundaries
* strict separation of responsibilities
* deterministic guard behavior
* verified resistance to prompt leakage and instruction exfiltration

The priority is **reliability and containment**, not open-ended generation.

---

## Motivation

Most public-facing chatbots fail in predictable ways:

* hallucinated biographical or professional details
* leakage of system prompts or retrieval context
* reliance on informal prompt “rules” for safety
* fragile behavior under adversarial or malformed input

This project explores a more disciplined architecture:

* a **thin, untrusted edge application**
* **centralized orchestration and policy enforcement**
* **explicit guard stages before and after model execution**
* **deterministic fallback behavior on policy violation**

The goal is not novelty — it is **predictable failure modes**.

---

## High-Level Architecture

```
Browser / Client
       │
       ▼
 Everlybot (TypeScript, Edge App)
       │   (authenticated, minimal payload)
       ▼
 SynapSys (Java, Spring Boot)
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
* authenticate itself via a shared client key
* return SynapSys responses verbatim (no mutation)

Everlybot explicitly **does not**:

* select models
* load system prompts
* perform guard logic
* inject retrieval context

This enforces a hard trust boundary.

---

### SynapSys (Broker & Guard Framework)

SynapSys owns **all LLM-related intelligence**:

* provider and model selection via strategy pattern
* system instruction loading
* grounded context assembly
* pre- and post-execution guard pipelines
* response validation and controlled fallback

Everlybot cannot override or bypass this behavior.

---

## Guard Pipeline & Failure Awareness

All requests pass through **explicit, ordered guard stages**.

### Pre-LLM Guards

Executed before any model call:

* system prompt extraction attempts
* instruction replay or encoding attacks
* disallowed or illegal request classes
* malformed or abusive input

Violations short-circuit execution.

---

### Post-LLM Guards

Executed on model output:

* prompt or policy leakage
* unsafe or disallowed content
* hallucinated personal data
* malformed or non-conforming responses

Violations result in **deterministic fallback responses** — never raw model output.

---

## Grounded Retrieval (No Black-Box RAG)

Responses are grounded using **curated Markdown documents** covering:

* education
* professional history
* projects
* certifications

Context injection is **explicit and auditable**.
This avoids opaque vector databases or uncontrolled retrieval while preserving predictable behavior.

---

## Security Posture

The system is designed under the assumption that it is **publicly reachable and adversarially probed**.

### Network & Deployment

* deployed behind **Cloudflare Tunnel**
* no inbound ports exposed
* Everlybot is the only Internet-facing service

### Application-Level Controls

* strict CORS configuration in production
* request size limits
* minimal `/health` endpoints
* no sensitive data in logs or responses

### Secret Hygiene

* no secrets committed to the repository
* private keys and guard policies live outside the public codebase
* shared client keys are validated server-side only

---

## Abuse & Regression Testing

A dedicated test suite simulates:

* prompt injection
* system prompt exfiltration
* context poisoning
* roleplay privilege escalation
* illegal and medical requests
* partial model compliance under load

Tests run against a **deterministic stub LLM**, ensuring:

* zero real token usage
* reproducible guard behavior
* immediate detection of regressions

If a future refactor reintroduces leakage, tests fail loudly.

---

## Code Structure (High Level)

### Everlybot (TypeScript)

* `src/app.ts` — Express entrypoint
* `src/config` — private configuration loader
* `src/types.ts` — shared message types

### SynapSys (Java)

* `service.guard` — guard pipeline
* `service.llm` — provider abstraction
* `strategy` — model and provider selection
* `api` — broker interface

Private guard rules and system instructions are intentionally excluded from the public repository.

---

## What This Demonstrates

This project demonstrates:

* explicit trust boundaries around LLM usage
* centralized policy enforcement
* deterministic failure handling
* testable and auditable guard logic
* safe exposure of LLM systems to untrusted clients

These constraints mirror real-world requirements in **healthcare, security, and regulated environments**, where uncontrolled output introduces downstream risk.

---

## Limitations & Future Work

Current limitations:

* retrieval is non-vectorized
* guards are rule-based rather than statistical
* no session or memory persistence

Planned work:

* optional vector database integration
* expanded guard taxonomy
* containerized deployment and CI hardening

---

## Disclaimer

This project was developed independently on personal time.
It is not affiliated with, endorsed by, or representative of any employer.

All data used consists of publicly available information about the author.
