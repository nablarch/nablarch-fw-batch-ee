package nablarch.fw.batch.ee.listener.job;

import nablarch.fw.batch.ee.listener.NablarchListenerContext;

/**
 * {@link NablarchJobListener}を実装した抽象クラス。
 * <p/>
 * 本クラスの実装では処理を何も行わない。サブクラス側で必要なメソッドをオーバライドし処理を追加すること。
 *
 * @author Hisaaki sioiri
 */
public abstract class AbstractNablarchJobListener implements NablarchJobListener {

    @Override
    public void beforeJob(NablarchListenerContext context) {
        // nop
    }

    @Override
    public void afterJob(NablarchListenerContext context) {
        // nop
    }
}

