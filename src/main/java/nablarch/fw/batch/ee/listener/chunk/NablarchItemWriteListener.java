package nablarch.fw.batch.ee.listener.chunk;

import nablarch.fw.batch.ee.listener.NablarchListenerContext;

import java.util.List;

/**
 * {@link jakarta.batch.api.chunk.ItemWriter#writeItems(List)}の前後に任意の処理を行うインタフェース。
 *
 * @author Hisaaki Shioiri
 */
public interface NablarchItemWriteListener {

    /**
     * {@link jakarta.batch.api.chunk.ItemWriter#writeItems(List)}の実行前の処理を行う。
     *
     * @param context {@link NablarchListenerContext}
     * @param items 書き込み対象のオブジェクト
     */
    void beforeWrite(NablarchListenerContext context, List<Object> items);

    /**
     * {@link jakarta.batch.api.chunk.ItemWriter#writeItems(List)}の実行後の処理を行う。
     *
     * @param context {@link NablarchListenerContext}
     * @param items 書き込み対象のオブジェクト
     */
    void afterWrite(NablarchListenerContext context, List<Object> items);

    /**
     * {@link jakarta.batch.api.chunk.ItemWriter#writeItems(List)}で{@link Exception}が発生した場合の処理を行う。
     * <p/>
     * 本メソッドは、Java Batch(JSR352)の仕様により、{@link Error}系が発生した場合はコールバックされない。
     *
     * @param context {@link NablarchListenerContext}
     * @param items 書き込み対象のオブジェクト
     * @param ex 発生した{@link Exception}
     */
    void onWriteError(NablarchListenerContext context, List<Object> items, Exception ex);
}
