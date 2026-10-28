package fish.payara.demo.cfp;

import jakarta.ai.agent.Action;
import jakarta.ai.agent.Agent;
import jakarta.ai.agent.Decision;
import jakarta.ai.agent.HandleException;
import jakarta.ai.agent.LLMException;
import jakarta.ai.agent.LargeLanguageModel;
import jakarta.ai.agent.Outcome;
import jakarta.ai.agent.Result;
import jakarta.ai.agent.Trigger;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import jakarta.json.bind.JsonbException;

import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Reviews talk submissions for the call for papers.
 * <p>
 * An agent is a CDI bean. With no scope annotation it is
 * {@code @WorkflowScoped}: every submission gets its own instance.
 */
@Agent(name = "cfp-reviewer", description = "Reviews talk submissions for the call for papers.")
public class CfpReviewerAgent {

    private static final Logger LOGGER = Logger.getLogger(CfpReviewerAgent.class.getName());

    private static final Jsonb JSONB = JsonbBuilder.create();

    private static final String SCOPE_PROMPT = """
            You screen talk submissions for a Java User Group.
            The JUG accepts talks about Java, the JVM, Jakarta EE, MicroProfile \
            and the tools and practices Java developers use.
            Is the following submission in scope?
            Answer YES or NO on the first line, then give the reason in one sentence on the second line.
            Submission: {}
            """;

    private static final String REVIEW_PROMPT = """
            Now review that submission as a member of the program committee.
            Score it from 1 (reject) to 5 (must accept), judging clarity, depth and value for the audience.
            Be strict: a vague abstract without concrete content scores 1 or 2.
            Reply with JSON only, no markdown, in exactly this shape:
            {"score": 3, "strengths": ["..."], "weaknesses": ["..."], "comment": "a short, constructive comment to the speaker"}
            """;

    /**
     * Starts the workflow. The parameter type is the CDI event that triggers it:
     * every {@code Event<Submission>.fire(...)} starts a new workflow.
     */
    @Trigger
    void onSubmission(Submission submission) {
        LOGGER.info(() -> "[TRIGGER] agent@" + Integer.toHexString(System.identityHashCode(this))
                + " received \"" + submission.title() + "\" by " + submission.speaker());
    }

    /**
     * Is the talk in scope for a Java User Group? A {@code Result} with
     * {@code success == false} ends the workflow here.
     * <p>
     * The {@link Submission} comes from the workflow state and the
     * {@link LargeLanguageModel} is supplied by the runtime. The submission is
     * serialized into the {@code {}} placeholder with JSON Binding.
     * <p>
     * No later phase sees a negative {@code Result}, not even {@code @Outcome}.
     * So the rejection is published here, in plain Java, or the speaker would
     * never learn why.
     */
    @Decision
    Result inScope(Submission submission, LargeLanguageModel llm, SubmissionStore store) {
        String answer = llm.query(SCOPE_PROMPT, submission).strip();
        String verdict = answer.lines().findFirst().orElse("");
        String reason = answer.lines().skip(1).collect(Collectors.joining(" ")).strip();

        boolean inScope = verdict.toUpperCase().contains("YES");
        LOGGER.info(() -> "[DECISION] in scope: " + inScope + " - " + reason);
        if (!inScope) {
            store.saveReview(submission.id(), Review.outOfScope(reason));
        }
        return new Result(inScope, reason);
    }

    /**
     * Writes the review. The LLM keeps the conversation of this workflow, so
     * "that submission" refers to the one sent in {@link #inScope}. The returned
     * {@link Review} becomes part of the workflow state.
     * <p>
     * {@code llm.query(REVIEW_PROMPT, Review.class)} would map the reply directly,
     * but local models like to wrap JSON in markdown fences, which JSON Binding
     * rejects. So the JSON is cut out of the reply in plain Java first. A reply
     * that still is not a review is reported as an {@link LLMException}.
     */
    @Action
    Review review(LargeLanguageModel llm) {
        String reply = llm.query(REVIEW_PROMPT);
        Review review;
        try {
            review = JSONB.fromJson(jsonIn(reply), Review.class);
        } catch (JsonbException e) {
            throw new LLMException("The LLM reply is not a review: " + reply, e);
        }
        LOGGER.info(() -> "[ACTION] score " + review.score() + "/5 - " + review.comment());
        return review;
    }

    /**
     * Called when any phase fails with an {@link LLMException}: the model is
     * down, times out, or answers something unusable. Returning normally
     * recovers the workflow, which then closes with {@link #publish}.
     */
    @HandleException
    void llmFailed(LLMException e, Submission submission) {
        LOGGER.warning(() -> "[EXCEPTION] " + e.getMessage() + " - \"" + submission.title()
                + "\" goes to a human reviewer");
    }

    /**
     * Ends the workflow by publishing the review. {@link Submission} and
     * {@link Review} come from the workflow state; {@link SubmissionStore} is
     * not workflow state, so the runtime resolves it as a CDI bean.
     * <p>
     * Reached when every decision said yes, or after {@link #llmFailed}
     * recovered the workflow. In that case no review was produced and
     * {@code review} is {@code null}.
     */
    @Outcome
    void publish(Submission submission, Review review, SubmissionStore store) {
        Review published = review != null ? review : Review.forHumanReview();
        store.saveReview(submission.id(), published);
        LOGGER.info(() -> "[OUTCOME] review published for " + submission.id()
                + " (\"" + submission.title() + "\")");
    }

    private static String jsonIn(String reply) {
        int start = reply.indexOf('{');
        int end = reply.lastIndexOf('}');
        return start >= 0 && end > start ? reply.substring(start, end + 1) : reply;
    }
}
