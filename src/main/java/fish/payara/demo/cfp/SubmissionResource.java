package fish.payara.demo.cfp;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

import java.util.Collection;

/**
 * REST entry point of the CFP: speakers POST their submissions here.
 * <p>
 * Reviewing takes tens of seconds with a local model, so the POST only accepts
 * the submission and hands it to the {@link ReviewDispatcher}. The speaker
 * polls the review URL until the {@link CfpReviewerAgent} has published it.
 */
@Path("submissions")
@RequestScoped
@Produces(MediaType.APPLICATION_JSON)
public class SubmissionResource {

    @Inject
    SubmissionStore store;

    @Inject
    ReviewDispatcher dispatcher;

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response submit(Submission submission, @Context UriInfo uri) {
        Submission stored = store.add(submission);
        dispatcher.dispatch(stored);
        return Response.accepted(stored)
                .location(uri.getAbsolutePathBuilder().path(stored.id()).path("review").build())
                .build();
    }

    @GET
    public Collection<Submission> list() {
        return store.all();
    }

    @GET
    @Path("{id}")
    public Submission get(@PathParam("id") String id) {
        return store.find(id).orElseThrow(NotFoundException::new);
    }

    /**
     * 200 with the review once it is published, 202 while the agent is still
     * working on it, 404 for an unknown submission.
     */
    @GET
    @Path("{id}/review")
    public Response review(@PathParam("id") String id) {
        Submission submission = store.find(id).orElseThrow(NotFoundException::new);
        return store.findReview(submission.id())
                .map(review -> Response.ok(review).build())
                .orElseGet(() -> Response.accepted(new Pending(submission.id(), "under review")).build());
    }

    public record Pending(String id, String status) {
    }
}
