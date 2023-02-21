package nablarch.fw.batch.ee.listener.step;

import nablarch.core.repository.SystemRepository;
import nablarch.fw.batch.ee.listener.NablarchListenerContext;
import nablarch.fw.batch.ee.listener.NablarchListenerExecutor;

import jakarta.batch.api.listener.StepListener;
import jakarta.batch.runtime.context.JobContext;
import jakarta.batch.runtime.context.StepContext;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.inject.Named;

/**
 * {@link StepListener}を実装したクラスで、{@link NablarchStepListener}を順次実行するクラス。
 * <p/>
 * 本クラスでは、{@link SystemRepository}から実行対象のリスナー({@link NablarchStepListener})のリストを取得する。
 * {@link SystemRepository}からリスナーリストを取得する方法は以下のとおり。
 * <ol>
 * <li>ジョブ名称 + ステップ名 + ".stepListeners"でリスナーリストが登録されている場合、そのリストを使用する。</li>
 * <li>ジョブ名称 + ".stepListeners"でリスナーリストが登録されている場合、そのリストを使用する。</li>
 * <li>stepListenersでリスナーリストが登録されている場合、そのリストを使用する。</li>
 * <li>上記に該当しない場合、このリスナーは何もしない。</li>
 * </ol>
 *
 * @author Hisaaki Shioiri
 */
@Named
@Dependent
public class NablarchStepListenerExecutor implements StepListener {

    /** {@link SystemRepository}からリスナーリストを取得する際のコンポーネント名 */
    private static final String LISTENER_LIST_NAME = "stepListeners";

    /** {@link JobContext } */
    @Inject
    private JobContext jobContext;

    /** {@link StepContext} */
    @Inject
    private StepContext stepContext;

    /** {@link NablarchListenerExecutor} */
    private NablarchListenerExecutor<NablarchStepListener> executor;

    /**
     * {@link NablarchStepListener#beforeStep(NablarchListenerContext)}を順次実行する。
     *
     * @throws Exception {@link NablarchStepListener#beforeStep(NablarchListenerContext)}実行時に送出された例外
     */
    @Override
    public void beforeStep() throws Exception {
        executor = new NablarchListenerExecutor<NablarchStepListener>(LISTENER_LIST_NAME, jobContext, stepContext);
        executor.executeBefore(new NablarchListenerExecutor.Runner<NablarchStepListener>() {
            @Override
            public void run(NablarchStepListener listener, NablarchListenerContext context) {
                listener.beforeStep(context);
            }
        });
    }

    /**
     * {@link NablarchStepListener#afterStep(NablarchListenerContext)}を逆順で実行する。
     * @throws Exception {@link NablarchStepListener#afterStep(NablarchListenerContext)}実行時に送出された例外
     */
    @Override
    public void afterStep() throws Exception {
        if (executor != null) {
            executor.executeAfter(new NablarchListenerExecutor.Runner<NablarchStepListener>() {
                @Override
                public void run(NablarchStepListener listener, NablarchListenerContext context) {
                    listener.afterStep(context);
                }
            });
        }
    }
}

