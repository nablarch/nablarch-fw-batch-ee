package nablarch.fw.batch.ee.integration.app;

import java.io.Serializable;
import java.util.Iterator;
import javax.batch.api.chunk.AbstractItemReader;
import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.inject.Named;

import nablarch.common.dao.UniversalDao;
import nablarch.fw.batch.ee.progress.ProgressManager;

/**
 * バッチアウトプットからデータを読み込むリーダー実装クラス。
 */
@Dependent
@Named
public class BatchOutputReader extends AbstractItemReader {

    private Iterator<BatchOutputEntity> reader;

    private final ProgressManager progressManager;

    @Inject
    public BatchOutputReader(ProgressManager progressManager) {
        this.progressManager = progressManager;
    }

    @Override
    public void open(Serializable checkpoint) throws Exception {
        reader = UniversalDao.defer()
                .findAll(BatchOutputEntity.class)
                .iterator();
        progressManager.setInputCount(
                UniversalDao.countBySqlFile(BatchOutputEntity.class, "FIND_ALL"));
    }

    @Override
    public Object readItem() throws Exception {
        if (reader.hasNext()) {
            return reader.next();
        }
        return null;
    }
}
