package nablarch.fw.batch.ee.integration.app;

import java.io.Serializable;

import javax.batch.api.BatchProperty;
import javax.batch.api.chunk.AbstractItemReader;
import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.inject.Named;

/**
 * プロパティで指定された数までの連番を文字列で返すリーダー
 */
@Dependent
@Named
public class StringReader extends AbstractItemReader {

    @Inject
    @BatchProperty
    String max = "100";

    private long maxLong;

    private long index = 1;

    @Override
    public void open(Serializable checkpoint) throws Exception {
        if (checkpoint instanceof Long) {
            index = Long.class.cast(checkpoint).longValue();
        } else {
            index = 1;
        }
        maxLong = Long.parseLong(max);
    }

    @Override
    public Object readItem() throws Exception {
        if (index > maxLong) {
            return null;
        }
        String result = String.valueOf(index);
        index++;
        return result;
    }

    @Override
    public Serializable checkpointInfo() throws Exception {
        return index;
    }
}
