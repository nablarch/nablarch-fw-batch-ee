package nablarch.fw.batch.ee.cdi;

import java.lang.annotation.Annotation;
import javax.batch.operations.BatchRuntimeException;
import javax.batch.runtime.context.StepContext;
import javax.enterprise.context.spi.Context;
import javax.enterprise.context.spi.Contextual;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.CDI;

/**
 * ステップ単位で値を共有するための{@link Context}実装クラス
 * <p/>
 * {@link StepContext}の一時領域を使用してステップ単位での値の共有を実現している。
 * そのため、バッチアプリケーション側で{@link StepContext#setTransientUserData(Object)}を直接使用することはできない点に注意すること。
 *
 * @author Naoki Yamamoto
 */
public class StepScopedContext implements Context {

    @Override
    public Class<? extends Annotation> getScope() {
        return StepScoped.class;
    }

    @Override
    public <T> T get(final Contextual<T> contextual, final CreationalContext<T> creationalContext) {
        final StepScopedHolder holder = getStepScopedHolder();
        final T bean = holder.get(contextual);
        if (bean != null) {
            return bean;
        } else {
            return holder.add(contextual, contextual.create(creationalContext));
        }
    }

    @Override
    public <T> T get(final Contextual<T> contextual) {
        final StepScopedHolder holder = getStepScopedHolder();
        return holder.get(contextual);
    }

    @Override
    public boolean isActive() {
        return getStepContext() != null;
    }

    /**
     * {@link StepContext}から{@link StepScopedHolder}を取得する。
     *
     * @return {@link StepScopedHolder}
     */
    private StepScopedHolder getStepScopedHolder() {
        final StepContext stepContext = getStepContext();
        final Object transientUserData = stepContext.getTransientUserData();

        if (transientUserData != null) {
            if (!(transientUserData instanceof StepScopedHolder)) {
                throw new BatchRuntimeException("TransientUserData of StepContext must be StepScopedHolder type.");
            }
            return (StepScopedHolder) transientUserData;
        } else {
            final StepScopedHolder stepScopedHolder = new StepScopedHolder();
            stepContext.setTransientUserData(stepScopedHolder);
            return stepScopedHolder;
        }
    }

    /**
     * ステップコンテキストを取得する。
     *
     * @return ステップコンテキスト
     */
    private StepContext getStepContext() {
        return CDI.current().select(StepContext.class).get();
    }
}
