package nablarch.fw.batch.ee.integration.app;

import javax.batch.api.AbstractBatchlet;
import javax.enterprise.context.Dependent;
import javax.inject.Named;

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
