package nablarch.fw.batch.ee.progress;

import javax.batch.api.AbstractBatchlet;
import javax.batch.runtime.context.JobContext;
import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.inject.Named;

/**
 * 進捗ログの出力確認用のBatchlet
 */
@Named
@Dependent
public class ProgressBatchlet extends AbstractBatchlet {

    private final JobContext jobContext;

    private final ProgressManager progressManager;

    @Inject
    public ProgressBatchlet(final JobContext jobContext, ProgressManager progressManager) {
        this.jobContext = jobContext;
        this.progressManager = progressManager;
    }

    @Override
    public String process() throws Exception {
        System.out.println("jobContext.getProperties() = " + jobContext.getProperties());
        if (!jobContext.getProperties()
                       .get("key1")
                       .equals("value1")) {
            throw new IllegalStateException("propertiesの値が想定外です。");
        }
        progressManager.setInputCount(100);

        for (int i = 1; i <= 100; i++) {
            if (i % 10 == 0) {
                progressManager.outputProgressInfo(i);
            }
        }
        return "SUCCESS";
    }
}
