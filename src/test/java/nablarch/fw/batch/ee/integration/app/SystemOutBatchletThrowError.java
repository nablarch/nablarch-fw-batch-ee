package nablarch.fw.batch.ee.integration.app;

import javax.batch.api.AbstractBatchlet;
import javax.batch.api.BatchProperty;
import javax.batch.runtime.context.JobContext;
import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.inject.Named;

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
