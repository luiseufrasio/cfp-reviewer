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
```

PowerShell:

```powershell
Invoke-RestMethod -Uri http://localhost:8080/api/submissions -Method Post `
  -InFile submissions\strong.json -ContentType application/json
```

## Project layout

```
src/main/java/fish/payara/demo/cfp/
├── CfpReviewerAgent.java     the @Agent
├── RestApplication.java      Jakarta REST under /api
├── Review.java               the review written by the LLM (record)
├── Submission.java           the talk proposal (record)
├── SubmissionResource.java   POST / GET /submissions, fires the trigger event
└── SubmissionStore.java      in-memory store for submissions and reviews
```
