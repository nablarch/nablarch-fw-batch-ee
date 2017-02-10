package nablarch.fw.batch.ee.integration.app;

import javax.batch.api.chunk.AbstractItemReader;
import javax.batch.runtime.context.StepContext;
import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.inject.Named;

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
