package fish.payara.demo.cfp;

import java.util.List;

/**
 * The program committee's review of a submission, as produced by the LLM.
 *
 * @param score      1 (reject) to 5 (must accept)
 * @param strengths  what works in the proposal
 * @param weaknesses what should be improved
 * @param comment    a short, constructive comment to the speaker
 */
public record Review(int score, List<String> strengths, List<String> weaknesses, String comment) {
}
