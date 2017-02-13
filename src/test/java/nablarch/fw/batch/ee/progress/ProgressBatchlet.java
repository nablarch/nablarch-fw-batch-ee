package nablarch.fw.batch.ee.progress;

import javax.batch.api.AbstractBatchlet;
import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.inject.Named;

/**
 * 進捗ログの出力確認用のBatchlet
 */
@Named
@Dependent
public class ProgressBatchlet extends AbstractBatchlet {

    private final ProgressManager progressManager;

    @Inject
    public ProgressBatchlet(ProgressManager progressManager) {
        this.progressManager = progressManager;
    }

    @Override
    public String process() throws Exception {
        progressManager.setInputCount(100);

        for (int i = 1; i <= 100; i++) {
            if (i % 10 == 0) {
                progressManager.outputProgressInfo(i);
            }
        }
        return "SUCCESS";
    }
}
