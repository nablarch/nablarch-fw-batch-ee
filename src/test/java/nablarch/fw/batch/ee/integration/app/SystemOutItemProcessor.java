package nablarch.fw.batch.ee.integration.app;

import jakarta.batch.api.chunk.ItemProcessor;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Named;

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
