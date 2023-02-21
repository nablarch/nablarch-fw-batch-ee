package nablarch.fw.batch.ee.progress;

import java.util.List;
import jakarta.batch.api.chunk.AbstractItemWriter;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.inject.Named;

/**
 * 進捗ログの出力確認用Writer
 */
@Dependent
@Named
public class ProgressWriter extends AbstractItemWriter {

    @Override
    public void writeItems(List<Object> list) throws Exception {}
}
