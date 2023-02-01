package nablarch.fw.batch.ee.integration.app;

import jakarta.batch.api.AbstractBatchlet;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Named;

/**
 * 何もしないBatchlet
 */
@Named
@Dependent
public class NothingBatchlet extends AbstractBatchlet {

    @Override
    public String process() throws Exception {
        return "SUCCESS";
    }
}
