package nablarch.fw.batch.ee.integration.app;

import javax.batch.api.chunk.AbstractItemReader;
import javax.enterprise.context.Dependent;
import javax.inject.Named;

/**
 * ワーニング終了するChunkのReader
 */
@Dependent
@Named
public class WarningItemReader extends AbstractItemReader {

    private int count = 0;

    @Override
    public Object readItem() throws Exception {
        count++;
        return (count <= 10) ? "Warning Item " + count : null;
    }
}
