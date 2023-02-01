package nablarch.fw.batch.ee.integration.app;

import java.util.List;
import jakarta.batch.api.chunk.AbstractItemWriter;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.inject.Named;

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
