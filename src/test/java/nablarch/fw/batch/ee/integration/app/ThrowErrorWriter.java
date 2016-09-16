package nablarch.fw.batch.ee.integration.app;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.batch.api.chunk.AbstractItemWriter;
import javax.enterprise.context.Dependent;
import javax.inject.Named;

import nablarch.common.dao.UniversalDao;

/**
 * スキップ対象のエラーを送出するWriter。
 */
@Dependent
@Named
public class ThrowErrorWriter extends AbstractItemWriter {

    public static int[] skipIds = new int[0];

    public static int errorId = -1;

    public static Set<Integer> retryError = Collections.emptySet();

    @Override
    public void writeItems(List<Object> list) throws Exception {
        for (Object o : list) {
            BatchOutputEntity entity = (BatchOutputEntity) o;
            if (errorId == entity.getId()) {
                throw new OutOfMemoryError();
            }
            if (retryError.contains(entity.getId())) {
                retryError.remove(entity.getId());
                throw new NullPointerException("null(´∀｀*)ﾎﾟｯ");
            }
            if (Arrays.binarySearch(skipIds, entity.getId()) >= 0) {
                throw new SkipException();
            }
            UniversalDao.insert(entity);
        }
    }
}
