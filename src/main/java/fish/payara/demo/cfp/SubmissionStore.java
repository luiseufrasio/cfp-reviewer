package fish.payara.demo.cfp;

import jakarta.enterprise.context.ApplicationScoped;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory submission store. Plain Java on purpose: persistence is not what
 * the demo is about.
 */
@ApplicationScoped
public class SubmissionStore {

    private final Map<String, Submission> submissions = new ConcurrentHashMap<>();

    public Submission add(Submission submission) {
        Submission stored = submission.withId(UUID.randomUUID().toString().substring(0, 8));
        submissions.put(stored.id(), stored);
        return stored;
    }

    public Optional<Submission> find(String id) {
        return Optional.ofNullable(submissions.get(id));
    }

    public Collection<Submission> all() {
        return submissions.values();
    }
}
