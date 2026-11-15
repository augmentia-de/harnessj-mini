package de.augmentia.harness2.tools.web;

import de.augmentia.harness2.tools.HarnessService;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

@Path("/api")
public class HarnessResource {
    @Inject
    HarnessService service;

    @POST
    @Path("/run")
    @Produces(MediaType.TEXT_PLAIN)
    public String run(String task) {
        return service.getHarness().chat(task);
    }

    @GET
    @Path("/")
    @Produces(MediaType.TEXT_HTML)
    public String index() {
        return "<html><body><h1>Harness2 (Quarkus)</h1><p>POST /api/run with task body.</p></body></html>";
    }
}