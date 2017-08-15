package nablarch.fw.batch.ee.listener.step;

import nablarch.core.util.annotation.Published;
import nablarch.fw.batch.ee.listener.NablarchListenerContext;

/**
 * {@link NablarchStepListener}を実装した抽象クラス。
 * <p/>
 * 本クラスの実装では処理を何も行わない。サブクラス側で必要なメソッドをオーバライドし処理を追加すること。
 *
 * @author Hisaaki sioiri
 */
@Published(tag = "architect")
public abstract class AbstractNablarchStepListener implements NablarchStepListener {

    @Override
    public void beforeStep(NablarchListenerContext context) {
        // nop
    }

    @Override
    public void afterStep(NablarchListenerContext context) {
        // nop
    }
}
