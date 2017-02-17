package nablarch.fw.batch.ee.integration.app;

import javax.batch.api.AbstractBatchlet;
import javax.batch.runtime.context.StepContext;
import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.inject.Named;

/**
 * StepScopeの確認用のBatchlet
 */
@Named
@Dependent
public class StepScopedBatchlet extends AbstractBatchlet {

    @Inject
    StepScopedBean bean;

    @Inject
    StepContext stepContext;

    @Override
    public String process() throws Exception {

        if (bean.getName() != null) {
            throw new IllegalStateException("StepScopeになってないよ");
        }

        bean.setName(stepContext.getStepName());

        return "SUCCESS";
    }
}
