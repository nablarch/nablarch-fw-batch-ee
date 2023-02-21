package nablarch.fw.batch.ee.cdi;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import jakarta.enterprise.context.spi.Contextual;

/**
 * ステップ単位で共有する値を保持するクラス。
 *
 * @author Naoki Yamamoto
 */
public class StepScopedHolder {

    /** ステップ単位で共有する値を持つMap */
    private final ConcurrentMap<Contextual<?>, Object> holder = new ConcurrentHashMap<Contextual<?>, Object>();

    /**
     * 値を取得する。
     *
     * @param contextual contextual
     * @param <T> 取得する値の型
     * @return 値
     */
    @SuppressWarnings("unchecked")
    public <T> T get(final Contextual<?> contextual) {
        return (T) holder.get(contextual);
    }

    /**
     * 値を設定する。
     *
     * @param contextual contextual
     * @param value 値
     * @param <T> 設定する値の型
     * @return StepScopeなBean
     */
    @SuppressWarnings("unchecked")
    public <T> T add(final Contextual<?> contextual, final T value) {
        final Object existsBean = holder.putIfAbsent(contextual, value);
        return existsBean != null ? (T) existsBean : value;
    }
}
