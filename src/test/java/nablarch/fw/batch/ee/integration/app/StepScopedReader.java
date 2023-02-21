package nablarch.fw.batch.ee.integration.app;

import jakarta.batch.api.chunk.AbstractItemReader;
import jakarta.batch.runtime.context.StepContext;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.inject.Named;

/**
 * StepScopeの確認用Reader
 */
@Named
@Dependent
public class StepScopedReader extends AbstractItemReader {

    /** リーダの実行回数 */
    private int index = 0;

    private final StepScopedBean bean;

    private final StepContext stepContext;

    @Inject
    public StepScopedReader(StepScopedBean bean, StepContext stepContext) {
        this.bean = bean;
        this.stepContext = stepContext;
        if (bean.getName() != null) {
            throw new IllegalStateException("StepScopeになってないよ");
        }
    }

    @Override
    public Object readItem() throws Exception {
        bean.setName(stepContext.getStepName());
        index++;
        return index == 1 ? 1 : null;
    }
}
