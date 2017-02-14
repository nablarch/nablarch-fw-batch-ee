package nablarch.fw.batch.ee.integration.app;

import javax.batch.api.chunk.ItemProcessor;
import javax.enterprise.context.Dependent;
import javax.inject.Named;

/**
 * 標準出力するChunkのProcessor
 */
@Dependent
@Named
public class SystemOutItemProcessor implements ItemProcessor {

    @Override
    public Object processItem(final Object o) throws Exception {
        return (String) o + " is processed.";
    }
}
