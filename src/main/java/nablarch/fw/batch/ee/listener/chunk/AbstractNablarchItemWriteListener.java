package nablarch.fw.batch.ee.listener.chunk;

import java.util.List;

import nablarch.core.util.annotation.Published;
import nablarch.fw.batch.ee.listener.NablarchListenerContext;

/**
 * {@link NablarchItemWriteListener}を実装した抽象クラス。
 * <p/>
 * 本クラスの実装では処理を何も行わない。サブクラス側で必要なメソッドをオーバライドし処理を追加すること。
 *
 * @author Hisaaki sioiri
 */
@Published(tag = "architect")
public abstract class AbstractNablarchItemWriteListener implements NablarchItemWriteListener {
    @Override
    public void beforeWrite(NablarchListenerContext context, List<Object> items) {
        // nop
    }

    @Override
    public void afterWrite(NablarchListenerContext context, List<Object> items) {
        // nop
    }

    @Override
    public void onWriteError(NablarchListenerContext context, List<Object> items, Exception ex) {
        // nop
    }
}
