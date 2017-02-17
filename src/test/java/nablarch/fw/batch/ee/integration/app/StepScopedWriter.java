package nablarch.fw.batch.ee.integration.app;

import java.util.List;
import javax.batch.api.chunk.AbstractItemWriter;
import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.inject.Named;

/**
 * StepScopeの確認用Writer
 */
@Named
@Dependent
public class StepScopedWriter extends AbstractItemWriter {

    @Inject
    StepScopedBean bean;

    @Override
    public void writeItems(List<Object> list) throws Exception {
        if (!"chunk".equals(bean.getName())) {
            throw new IllegalStateException("リーダで設定した値が取得できないのでエラー");
        }
    }
}
