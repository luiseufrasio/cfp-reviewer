package fish.payara.demo.cfp;

import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.event.Event;
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
 * Each stored submission is fired as a CDI event, which triggers the
 * {@link CfpReviewerAgent} workflow. The workflow runs synchronously, so the
 * review is already published when the response is built.
 */
@Path("submissions")
@RequestScoped
@Produces(MediaType.APPLICATION_JSON)
public class SubmissionResource {

    @Inject
    SubmissionStore store;

    @Inject
    Event<Submission> submitted;

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response submit(Submission submission, @Context UriInfo uri) {
        Submission stored = store.add(submission);
        submitted.fire(stored);   // synchronous: the workflow runs before we respond
        Review review = store.findReview(stored.id()).orElse(null);   // null: out of scope
        return Response.created(uri.getAbsolutePathBuilder().path(stored.id()).build())
                .entity(new ReviewedSubmission(stored, review))
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

    @GET
    @Path("{id}/review")
    public Review review(@PathParam("id") String id) {
        return store.findReview(id).orElseThrow(NotFoundException::new);
    }

    public record ReviewedSubmission(Submission submission, Review review) {
    }
}
