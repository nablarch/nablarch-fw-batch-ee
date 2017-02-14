package nablarch.fw.batch.ee.integration.app;

import java.util.List;

import javax.batch.api.BatchProperty;
import javax.batch.api.chunk.AbstractItemWriter;
import javax.batch.runtime.context.JobContext;
import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.inject.Named;

/**
 * 標準出力するChunkのWriter。isWarningプロパティが"true"の場合は、終了ステータスを"WARNING"にする。
 */
@Dependent
@Named
public class SystemOutItemWriter extends AbstractItemWriter {
    @Inject
    @BatchProperty
    String isWarning = "false";

    @Inject
    JobContext jobContext;

    @Override
    public void writeItems(final List<Object> list) throws Exception {
        System.out.println("Run SystemOut Test Item Writer" + (("true".equals(isWarning)) ? " [WarningMode]": ""));
        for (Object o : list) {
            System.out.println((String) o);
        }
        if( "true".equals(isWarning)) {
            jobContext.setExitStatus("WARNING");
        }
    }
}
