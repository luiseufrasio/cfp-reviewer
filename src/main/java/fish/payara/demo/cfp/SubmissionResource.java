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
 * For now a submission is only stored. Nobody reviews it yet.
 */
@Path("submissions")
@RequestScoped
@Produces(MediaType.APPLICATION_JSON)
public class SubmissionResource {

    @Inject
    SubmissionStore store;

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response submit(Submission submission, @Context UriInfo uri) {
        Submission stored = store.add(submission);
        return Response.created(uri.getAbsolutePathBuilder().path(stored.id()).build())
                .entity(stored)
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
}
