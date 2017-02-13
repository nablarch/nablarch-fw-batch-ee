package nablarch.fw.batch.ee.progress;

import java.util.List;
import javax.batch.api.chunk.AbstractItemWriter;
import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.inject.Named;

/**
 * 進捗ログの出力確認用Writer
 */
@Dependent
@Named
public class ProgressWriter extends AbstractItemWriter {

    @Override
    public void writeItems(List<Object> list) throws Exception {}
}
