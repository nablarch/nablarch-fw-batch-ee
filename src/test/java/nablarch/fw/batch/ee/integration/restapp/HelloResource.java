package nablarch.fw.batch.ee.integration.restapp;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

@Path("hello")
@Dependent
public class HelloResource {

    private final HelloBean helloBean;

    @Inject
    public HelloResource(final HelloBean helloBean) {
        this.helloBean = helloBean;
    }

    @GET
    public String get() {
        return helloBean.get();
    }
}
