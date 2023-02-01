package nablarch.fw.batch.ee.listener.chunk;

import nablarch.core.repository.SystemRepository;
import nablarch.fw.batch.ee.listener.NablarchListenerContext;
import nablarch.fw.batch.ee.listener.NablarchListenerExecutor;
import nablarch.fw.batch.ee.listener.NablarchListenerExecutor.Runner;

import jakarta.batch.api.chunk.listener.ItemWriteListener;
import jakarta.batch.runtime.context.JobContext;
import jakarta.batch.runtime.context.StepContext;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.util.List;

/**
 * {@link ItemWriteListener}を実装したクラスで、{@link NablarchItemWriteListener}を順次実行するクラス。
 * <p/>
 * 本クラスでは、{@link SystemRepository}から実行対象のリスナー({@link NablarchItemWriteListener})のリストを取得する。
 * {@link SystemRepository}からリスナーリストを取得する方法は以下のとおり。
 * <ol>
 * <li>ジョブ名称 + ステップ名 + ".itemWriteListeners"でリスナーリストが登録されている場合、そのリストを使用する。</li>
 * <li>ジョブ名称 + ".itemWriteListeners"でリスナーリストが登録されている場合、そのリストを使用する。</li>
 * <li>itemWriteListenersでリスナーリストが登録されている場合、そのリストを使用する。</li>
 * <li>上記に該当しない場合、このリスナーは何もしない。</li>
 * </ol>
 *
 * @author Naoki Yamamoto
 */
@Named
@Dependent
public class NablarchItemWriteListenerExecutor implements ItemWriteListener {

    /** {@link SystemRepository}からリスナーリストを取得する際のコンポーネント名 */
    private static final String LISTENER_LIST_NAME = "itemWriteListeners";

    /** {@link JobContext } */
    @Inject
    private JobContext jobContext;

    /** {@link StepContext} */
    @Inject
    private StepContext stepContext;

    /** {@link NablarchListenerExecutor} */
    private NablarchListenerExecutor<NablarchItemWriteListener> executor;

    /**
     * {@link NablarchItemWriteListener#beforeWrite(NablarchListenerContext, List)}を順次実行する。
     *
     * @param items 書き込み処理対象の{@link Object}
     * @throws Exception {@link NablarchItemWriteListener#beforeWrite(NablarchListenerContext, List)} 実行時に送出された例外
     */
    @Override
    public void beforeWrite(final List<Object> items) throws Exception {
        executor = new NablarchListenerExecutor<NablarchItemWriteListener>(LISTENER_LIST_NAME, jobContext, stepContext);
        executor.executeBefore(new Runner<NablarchItemWriteListener>() {
            @Override
            public void run(NablarchItemWriteListener listener, NablarchListenerContext context) {
                listener.beforeWrite(context, items);
            }
        });
    }

    /**
     * {@link NablarchItemWriteListener#afterWrite(NablarchListenerContext, List)}を逆順で実行する。
     *
     * @param items 書き込み処理対象の{@link Object}
     * @throws Exception {@link NablarchItemWriteListener#afterWrite(NablarchListenerContext, List)} 実行時に送出された例外
     */
    @Override
    public void afterWrite(final List<Object> items) throws Exception {
        if (executor != null) {
            executor.executeAfter(new Runner<NablarchItemWriteListener>() {
                @Override
                public void run(NablarchItemWriteListener listener, NablarchListenerContext context) {
                    listener.afterWrite(context, items);
                }
            });
        }
    }

    /**
     * {@link NablarchItemWriteListener#onWriteError(NablarchListenerContext, List, Exception)} を逆順で実行する。
     *
     * @param items 書き込み処理対象の{@link Object}
     * @param ex 書き込み処理時に発生した{@link Exception}
     * @throws Exception {@link NablarchItemWriteListener#onWriteError(NablarchListenerContext, List, Exception)} 実行時に送出された例外
     */
    @Override
    public void onWriteError(final List<Object> items, final Exception ex) throws Exception {
        if (executor != null) {
            executor.executeOnError(new Runner<NablarchItemWriteListener>() {
                @Override
                public void run(NablarchItemWriteListener listener, NablarchListenerContext context) {
                    listener.onWriteError(context, items, ex);
                }
            });
        }
    }
}
