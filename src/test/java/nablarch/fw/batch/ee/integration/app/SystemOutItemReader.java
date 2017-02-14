package nablarch.fw.batch.ee.integration.app;

import javax.batch.api.chunk.AbstractItemReader;
import javax.enterprise.context.Dependent;
import javax.inject.Named;

/**
 * 標準出力するChunkのReader
 */
@Dependent
@Named
public class SystemOutItemReader extends AbstractItemReader {

    private int count = 0;

    @Override
    public Object readItem() throws Exception {
        count++;
        return (count <= 10) ? "SystemOut Test Item " + count : null;
    }
}
