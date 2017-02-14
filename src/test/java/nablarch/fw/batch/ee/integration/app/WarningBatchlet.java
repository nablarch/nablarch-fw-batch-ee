package nablarch.fw.batch.ee.integration.app;

import javax.batch.api.AbstractBatchlet;
import javax.batch.runtime.context.JobContext;
import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.inject.Named;

/**
 * ワーニング終了するBatchlet
 */
@Named
@Dependent
public class WarningBatchlet extends AbstractBatchlet {
    @Inject
    JobContext jobContext;

    @Override
    public String process() throws Exception {
        jobContext.setExitStatus("WARNING");
        return null;
    }
}
