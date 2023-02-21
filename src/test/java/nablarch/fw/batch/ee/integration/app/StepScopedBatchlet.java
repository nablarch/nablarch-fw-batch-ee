package nablarch.fw.batch.ee.integration.app;

import jakarta.batch.api.AbstractBatchlet;
import jakarta.batch.runtime.context.StepContext;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.inject.Named;

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
