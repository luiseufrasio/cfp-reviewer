package fish.payara.demo.cfp;

import ee.jakarta.tck.ai.agent.framework.stub.LargeLanguageModelStub;
import jakarta.ai.agent.LLMException;
import jakarta.ai.agent.Result;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * An agent is a plain Java class: its phases are tested by calling them
 * directly, with the TCK's scripted LLM instead of a real model. No server,
 * no Ollama.
 */
class CfpReviewerAgentTest {

    private final LargeLanguageModelStub llm = new LargeLanguageModelStub();
    private final SubmissionStore store = new SubmissionStore();
    private final CfpReviewerAgent agent = new CfpReviewerAgent();

    private final Submission submission = store.add(new Submission(null,
            "Sourdough at Scale", "Bob Example", "Jakarta EE", "Everything I learned about baking bread."));

    @Test
    void outOfScopeTalkEndsTheWorkflowAndTellsTheSpeakerWhy() {
        llm.enqueueResponse("NO\nThe talk is about baking, not Java.");

        Result result = agent.inScope(submission, llm, store);

        assertFalse(result.success());
        Review review = store.findReview(submission.id()).orElseThrow();
        assertTrue(review.comment().contains("The talk is about baking, not Java."));
    }

    @Test
    void submissionIsSentToTheModelAsJson() {
        llm.enqueueResponse("YES\nIt is about Java.");

        agent.inScope(submission, llm, store);

        assertTrue(llm.lastCall().effectivePrompt().contains("\"title\":\"Sourdough at Scale\""));
    }

    @Test
    void reviewIsReadEvenWhenTheModelWrapsItInMarkdown() {
        llm.enqueueResponse("""
                ```json
                {"score": 4, "strengths": ["clear"], "weaknesses": [], "comment": "Nice talk."}
                ```""");

        Review review = agent.review(llm);

        assertEquals(4, review.score());
        assertEquals("Nice talk.", review.comment());
    }

    @Test
    void replyThatIsNotAReviewIsAnLlmException() {
        llm.enqueueResponse("Sorry, I cannot review this talk.");

        assertThrows(LLMException.class, () -> agent.review(llm));
    }

    @Test
    void outcomePublishesAHumanReviewWhenNoReviewWasProduced() {
        agent.publish(submission, null, store);

        assertEquals(Review.forHumanReview(), store.findReview(submission.id()).orElseThrow());
    }
}
