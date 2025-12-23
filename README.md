# Portfolio Chatbot — Reference Implementation for SynapSys

**Author**: David Everly  
**Language**: Java (Spring Boot)  
**Status**: Active

---

## Overview

The **Portfolio Chatbot** is a Spring Boot–based conversational application designed to answer questions about my
professional background, projects, and experience.

More importantly, it serves as a **reference implementation of SynapSys**, demonstrating how a failure-aware LLM
orchestration framework can be applied in a real, user-facing system.

The application prioritizes:

- factual grounding
- constrained scope
- predictable behavior over open-ended generation

---

## Why This Exists

Generic chatbot implementations frequently:

- hallucinate biographical details
- drift off-topic
- expose raw model output directly to users
- rely on informal prompt constraints

This project explores a more controlled approach:

- retrieval-augmented prompting from curated data
- explicit guard stages before and after model execution
- deterministic fallback behavior when policies are violated

The goal is **reliability**, not novelty.

---

## Architecture Summary

### Retrieval-Augmented Prompting

The chatbot loads structured Markdown files containing:

- education
- certifications
- employment history
- project summaries

This data is selectively injected into prompts to ground responses without relying on external vector databases.

---

### Pre- and Post-LLM Guard Pipeline

All interactions pass through guard stages **before and after** model execution.

- **Pre-LLM guards** evaluate user input and can short-circuit execution, preventing certain classes of requests from
  ever reaching the LLM.
- **Post-LLM guards** inspect generated responses and only return content to the user if it passes defined policy
  checks.
- If a response fails post-LLM checks, a **controlled fallback response** is returned instead.

The public release exposes a **minimal guard set** for demonstration and transparency.  
More comprehensive guard logic is implemented in a private repository and excluded from the public codebase by design.

---

### LLM Orchestration

All model interaction is delegated to **SynapSys**, which manages:

- client selection
- structured prompting
- parsing and validation
- bounded local repair of malformed output

This keeps the chatbot thin while exercising SynapSys under real usage.

---

## Code Structure

- `dev.everly.portfolio.agent` — application-level agent logic
- `dev.everly.portfolio.api` — REST interface
- `dev.everly.portfolio.guard` — guard pipeline and rules
- `dev.everly.portfolio.rag` — retrieval and data loading
- `resources/*.md` — curated knowledge base

---

## What This Demonstrates

This project demonstrates:

- explicit trust boundaries around LLM usage
- pre-execution short-circuiting to reduce attack surface and cost
- post-execution validation before user exposure
- integration of an orchestration framework into a production-style service

Together, these constraints mirror the design pressures present in healthcare and security-adjacent systems, where incorrect or uncontrolled output can introduce downstream risk.

---

## Current Limitations & Future Work

- Retrieval is in-memory and non-vectorized
- Guard rules are deterministic and rule-based
- No persistence or session modeling

Planned extensions include:

- vector database integration
- expanded guard coverage
- containerized deployment

---

## Disclaimer

This project was developed independently on personal time and is not affiliated with any employer.  
All data used is publicly available information about the author.
