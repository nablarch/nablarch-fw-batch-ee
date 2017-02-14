package nablarch.fw.batch.ee.integration.app;

import java.util.List;

import javax.batch.api.chunk.AbstractItemWriter;
import javax.batch.runtime.context.JobContext;
import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.inject.Named;

/**
 * ワーニング終了するChunkのWriter
 */
@Dependent
@Named
public class WarningItemWriter extends AbstractItemWriter {

    @Inject
    JobContext jobContext;

    @Override
    public void writeItems(final List<Object> list) throws Exception {
        System.out.println("Run Warning Item Writer");
        for (Object o : list) {
            System.out.println((String) o);
        }
        jobContext.setExitStatus("WARNING");
    }
}
