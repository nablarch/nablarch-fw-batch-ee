package nablarch.fw.batch.ee.progress;

import java.io.Serializable;
import javax.batch.api.chunk.AbstractItemReader;
import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.inject.Named;

/**
 * 進捗ログの出力確認用リーダ
 */
@Named
@Dependent
public class ProgressReader extends AbstractItemReader {

    private final ProgressManager progressManager;

    private int index = 0;

    @Inject
    public ProgressReader(ProgressManager progressManager) {
        this.progressManager = progressManager;
    }

    @Override
    public void open(Serializable checkpoint) throws Exception {
        progressManager.setInputCount(100);
    }

    @Override
    public Object readItem() throws Exception {
        while (index < 100) {
            index++;
            return index;
        }
        return null;
    }
}
