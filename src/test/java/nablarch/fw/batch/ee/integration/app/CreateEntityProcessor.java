package nablarch.fw.batch.ee.integration.app;


import javax.batch.api.chunk.ItemProcessor;
import javax.enterprise.context.Dependent;
import javax.inject.Named;

/**
 * 入力値をEntityに変換する。
 */
@Dependent
@Named
public class CreateEntityProcessor implements ItemProcessor {

    @Override
    public Object processItem(Object o) throws Exception {
        final String s = o.toString();
        return new BatchOutputEntity(Integer.valueOf(s), "name_" + s);
    }
}
