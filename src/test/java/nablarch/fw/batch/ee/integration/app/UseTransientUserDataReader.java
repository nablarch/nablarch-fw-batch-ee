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
public class UseTransientUserDataReader extends AbstractItemReader {

    /** リーダの実行回数 */
    private int index = 0;

    private final StepContext stepContext;

    @Inject
    public UseTransientUserDataReader(StepContext stepContext) {
        this.stepContext = stepContext;
        stepContext.setTransientUserData("ユーザデータ");
    }

    @Override
    public Object readItem() throws Exception {
        index++;
        return index == 1 ? 1 : null;
    }
}
