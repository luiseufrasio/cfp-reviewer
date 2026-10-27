package fish.payara.demo.cfp;

import jakarta.ai.agent.Agent;
import jakarta.ai.agent.Decision;
import jakarta.ai.agent.LargeLanguageModel;
import jakarta.ai.agent.Result;
import jakarta.ai.agent.Trigger;

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

    private static final String SCOPE_PROMPT = """
            You screen talk submissions for a Java User Group.
            The JUG accepts talks about Java, the JVM, Jakarta EE, MicroProfile \
            and the tools and practices Java developers use.
            Is the following submission in scope?
            Answer YES or NO on the first line, then give the reason in one sentence on the second line.
            Submission: {}
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
     */
    @Decision
    Result inScope(Submission submission, LargeLanguageModel llm) {
        String answer = llm.query(SCOPE_PROMPT, submission).strip();
        String verdict = answer.lines().findFirst().orElse("");
        String reason = answer.lines().skip(1).collect(Collectors.joining(" ")).strip();

        boolean inScope = verdict.toUpperCase().contains("YES");
        LOGGER.info(() -> "[DECISION] in scope: " + inScope + " - " + reason);
        return new Result(inScope, reason);
    }
}
