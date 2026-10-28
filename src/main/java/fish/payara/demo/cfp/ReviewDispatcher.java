package fish.payara.demo.cfp;

import jakarta.annotation.Resource;
import jakarta.enterprise.concurrent.ManagedExecutorService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Starts the review workflow without blocking the HTTP request.
 * <p>
 * {@code Event.fireAsync(...)} is not an option: the runtime observes the
 * trigger event synchronously, so an async event would never reach the agent.
 * The synchronous {@code fire(...)} runs on a managed thread instead, plain
 * Jakarta Concurrency.
 */
@ApplicationScoped
public class ReviewDispatcher {

    private static final Logger LOGGER = Logger.getLogger(ReviewDispatcher.class.getName());

    @Resource
    ManagedExecutorService executor;

    @Inject
    Event<Submission> submitted;

    public void dispatch(Submission submission) {
        executor.execute(() -> {
            try {
                submitted.fire(submission);
            } catch (RuntimeException e) {
                LOGGER.log(Level.SEVERE, "Review workflow failed for " + submission.id(), e);
            }
        });
    }
}
