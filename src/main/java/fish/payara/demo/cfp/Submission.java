package fish.payara.demo.cfp;

/**
 * A talk proposal sent to the call for papers.
 *
 * @param id       assigned by the server when the submission is received
 * @param title    talk title
 * @param speaker  speaker name
 * @param track    the track the speaker is applying to (e.g. "Jakarta EE", "Cloud Native")
 * @param summary  the abstract, as the speaker wrote it
 */
public record Submission(String id, String title, String speaker, String track, String summary) {

    public Submission withId(String newId) {
        return new Submission(newId, title, speaker, track, summary);
    }
}
