package nablarch.fw.batch.ee.integration.restapp;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

import org.jboss.weld.context.ContextNotActiveException;

public class CustomExceptionMapper implements ExceptionMapper<ContextNotActiveException> {

    @Override
    public Response toResponse(final ContextNotActiveException e) {
        System.out.println("e.getMessage() = " + e.getMessage());
        return Response.status(500)
                       .entity(e.getMessage())
                       .build();
    }
}
