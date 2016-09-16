package nablarch.fw.batch.ee.integration.app;

import java.util.List;

import javax.batch.api.chunk.AbstractItemWriter;
import javax.enterprise.context.Dependent;
import javax.inject.Named;

import nablarch.common.dao.UniversalDao;

/**
 * batch_outputに書き込む
 */
@Dependent
@Named
public class BatchOutputWriter extends AbstractItemWriter {


    @Override
    public void writeItems(List<Object> list) throws Exception {
        for (Object o : list) {
            UniversalDao.insert(o);
        }
    }
}
