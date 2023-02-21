package nablarch.fw.batch.ee.integration.app;

import jakarta.batch.api.AbstractBatchlet;
import jakarta.batch.api.BatchProperty;
import jakarta.batch.runtime.context.JobContext;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.inject.Named;

/**
 * 標準出力するBatchlet。isWarningプロパティが"true"の場合は、終了ステータスを"WARNING"にする。
 */
@Named
@Dependent
public class SystemOutBatchletThrowError extends AbstractBatchlet {

    @Inject
    @BatchProperty
    String isWarning = "false";

    @Inject
    JobContext jobContext;

    @Override
    public String process() throws Exception {
        if ("true".equals(isWarning)) {
            jobContext.setExitStatus("WARNING");
        }
        throw new Exception("Exception for test." + (("true".equals(isWarning)) ? " [WarningMode]" : ""));
    }
}
