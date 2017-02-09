package nablarch.fw.batch.ee.integration.restapp;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

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
