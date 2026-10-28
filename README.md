# CFP Reviewer Agent

A **Jakarta Agentic AI** agent that reviews conference talk submissions: REST in,
a local **Ollama** model deciding, a review comment out. It runs on **Payara Micro**,
the first runtime to implement the specification.

Built live at the JUG Oberpfalz session on 12 October 2026.

| | |
|---|---|
| Spec | Jakarta Agentic AI `1.0.0-M1` |
| Runtime | Payara Micro `7.2026.9` (first Micro release with Agentic AI) |
| LLM | Ollama, `gemma3:12b` |
| Java | 21 |

## Following along: one branch per step

Every branch builds on the previous one. Check out a branch to start a step,
and the next branch has that step finished.

| Branch | Step |
|---|---|
| `0-initial` | Plain Jakarta REST app: submissions are stored, nobody reviews them |
| `1-trigger` | `@Agent` + `@Trigger`: a CDI event starts the workflow |
| `2-decision` | `@Decision` + `LargeLanguageModel`: is the talk in scope? |
| `3-action` | `@Action`: the LLM writes a structured review (JSON-B) |
| `4-outcome` | `@Outcome`: the review comment is published |
| `5-exception` | `@HandleException`: what happens when the LLM is down |
| `6-plain-java` | What still ends up in plain Java |

```bash
git checkout 1-trigger
```

## Prerequisites

1. JDK 21 and Maven 3.9+.
2. [Ollama](https://ollama.com/download) with the model pulled:

   ```bash
   ollama pull gemma3:12b     # ~8 GB; lighter alternative: gemma3:4b
   ```

   The first call after the model has been idle is slow (cold load, around 20 s).

The LLM backend is configured in
[`microprofile-config.properties`](src/main/resources/META-INF/microprofile-config.properties):

```properties
payara.agentic.llm.provider=ollama
payara.agentic.llm.model=gemma3:12b
payara.agentic.llm.ollama.base-url=http://localhost:11434
```

## Run

```bash
mvn clean package payara-micro:start
```

The build downloads Payara Micro into your local Maven repository. The app is
deployed at the root context, under `http://localhost:8080/api`.

## Try it

Three sample submissions live in [`submissions/`](submissions): a strong one,
a weak one and an off-topic one.

```bash
curl -s -X POST http://localhost:8080/api/submissions \
     -H "Content-Type: application/json" -d @submissions/strong.json

curl -s http://localhost:8080/api/submissions
curl -s http://localhost:8080/api/submissions/{id}
curl -s http://localhost:8080/api/submissions/{id}/review
```

From `6-plain-java` on, the POST answers `202 Accepted` right away, with a
`Location` header pointing at the review. That URL answers `202` with
`{"status": "under review"}` while the agent works, then `200` with the review.

PowerShell:

```powershell
Invoke-RestMethod -Uri http://localhost:8080/api/submissions -Method Post `
  -InFile submissions\strong.json -ContentType application/json
```

### When the LLM fails

Stop Ollama and post a submission again: the `@HandleException` method takes
over, and the review says a person will step in.

```powershell
Stop-Process -Name "ollama*"      # Windows: or quit Ollama from the tray
ollama serve                      # start it again afterwards
```

To simulate the outage without touching Ollama, point the app at a port where
nothing listens. System properties override `microprofile-config.properties`:

```bash
mvn clean package
java -Dpayara.agentic.llm.ollama.base-url=http://localhost:11999 \
     -jar ~/.m2/repository/fish/payara/extras/payara-micro/7.2026.9/payara-micro-7.2026.9.jar \
     --deploy target/cfp-reviewer.war --contextroot /
```

### Tests

The agent is a plain Java class, so its phases are unit-tested by calling them
directly, with the scripted `LargeLanguageModelStub` from the Jakarta Agentic AI
TCK in place of a real model. No server, no Ollama:

```bash
mvn test
```

## What still ends up in plain Java

Branch `6-plain-java` collects what the specification does not cover (yet):

| Concern | Why it is plain Java |
|---|---|
| Telling a rejected speaker why | No phase receives a negative `Result`, not even `@Outcome`, so the `@Decision` publishes the rejection itself |
| Not blocking the HTTP request | The runtime observes the trigger event synchronously, so `Event.fireAsync` never reaches the agent; `ReviewDispatcher` fires it on a `ManagedExecutorService` |
| Reading JSON from a local model | Models wrap JSON in markdown fences, so `llm.query(prompt, Review.class)` fails; the JSON is cut out before JSON Binding maps it |
| A fallback review after a failure | `@HandleException` must return `void`, so the replacement review is chosen in `@Outcome` |
| Storage | `SubmissionStore` is an in-memory map |

## Project layout

```
src/main/java/fish/payara/demo/cfp/
├── CfpReviewerAgent.java     the @Agent
├── ReviewDispatcher.java     runs the workflow off the HTTP thread
├── RestApplication.java      Jakarta REST under /api
├── Review.java               the review written by the LLM (record)
├── Submission.java           the talk proposal (record)
├── SubmissionResource.java   POST / GET /submissions
└── SubmissionStore.java      in-memory store for submissions and reviews

src/test/java/fish/payara/demo/cfp/
└── CfpReviewerAgentTest.java the phases, tested with the TCK LLM stub
```
