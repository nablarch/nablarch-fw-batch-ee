package nablarch.fw.batch.ee.cdi;

import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.Extension;

/**
 * JavaEEコンテナの初期化時に、
 * JavaBatchの仕様に準拠したバッチアプリケーション実行に必要な初期処理を行う{@link Extension}実装クラス。
 *
 * @author Naoki Yamamoto
 */
public class NablarchBatchCdiExtension implements Extension {

    /**
     * {@link AfterBeanDiscovery}のイベント
     *
     * @param afterBeanDiscovery Bean検索後のイベント
     */
    public void afterBeanDiscovery(@Observes final AfterBeanDiscovery afterBeanDiscovery) {
        afterBeanDiscovery.addContext(new StepScopedContext());
    }
}
