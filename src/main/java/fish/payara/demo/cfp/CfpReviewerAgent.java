package fish.payara.demo.cfp;

import jakarta.ai.agent.Agent;
import jakarta.ai.agent.Trigger;

import java.util.logging.Logger;

/**
 * Reviews talk submissions for the call for papers.
 * <p>
 * An agent is a CDI bean. With no scope annotation it is
 * {@code @WorkflowScoped}: every submission gets its own instance.
 */
@Agent(name = "cfp-reviewer", description = "Reviews talk submissions for the call for papers.")
public class CfpReviewerAgent {

    private static final Logger LOGGER = Logger.getLogger(CfpReviewerAgent.class.getName());

    /**
     * Starts the workflow. The parameter type is the CDI event that triggers it:
     * every {@code Event<Submission>.fire(...)} starts a new workflow.
     */
    @Trigger
    void onSubmission(Submission submission) {
        LOGGER.info(() -> "[TRIGGER] agent@" + Integer.toHexString(System.identityHashCode(this))
                + " received \"" + submission.title() + "\" by " + submission.speaker());
    }
}
