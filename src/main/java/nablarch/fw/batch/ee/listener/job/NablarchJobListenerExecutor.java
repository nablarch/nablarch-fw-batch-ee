package nablarch.fw.batch.ee.listener.job;

import nablarch.core.repository.SystemRepository;
import nablarch.fw.batch.ee.initializer.LogInitializer;
import nablarch.fw.batch.ee.initializer.RepositoryInitializer;
import nablarch.fw.batch.ee.listener.NablarchListenerContext;
import nablarch.fw.batch.ee.listener.NablarchListenerExecutor;

import jakarta.batch.api.BatchProperty;
import jakarta.batch.api.listener.JobListener;
import jakarta.batch.runtime.context.JobContext;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.inject.Named;

/**
 * {@link JobListener}を実装したクラスで、{@link NablarchJobListener}を順次実行するクラス。
 * <p/>
 * 本クラスでは、{@link SystemRepository}から実行対象のリスナー({@link NablarchJobListener})のリストを取得する。
 * {@link SystemRepository}からリスナーリストを取得する方法は以下のとおり。
 * <ol>
 * <li>ジョブ名称 + ".jobListeners"でリスナーリストが登録されている場合、そのリストを使用する。</li>
 * <li>jobListenersでリスナーリストが登録されている場合、そのリストを使用する。</li>
 * <li>上記に該当しない場合、このリスナーは何もしない。</li>
 * </ol>
 *
 * @author Naoki Yamamoto
 */
@Named
@Dependent
public class NablarchJobListenerExecutor implements JobListener {

    /** {@link SystemRepository}からリスナーリストを取得する際のコンポーネント名 */
    private static final String LISTENER_LIST_NAME = "jobListeners";

    /** {@link JobContext} */
    @Inject
    private JobContext jobContext;

    /** コンポーネント設定ファイルパス */
    @Inject
    @BatchProperty
    private String diConfigFilePath = "batch-boot.xml";

    /** {@link NablarchListenerExecutor} */
    private NablarchListenerExecutor<NablarchJobListener> executor;


    /**
     * ジョブの実行前に、ログおよびコンポーネントの初期化処理を行い、
     * {@link NablarchJobListener#beforeJob(NablarchListenerContext)}を順次実行する。
     *
     * @throws Exception {@link NablarchJobListener#beforeJob(NablarchListenerContext)}実行時に送出された例外
     */
    @Override
    public void beforeJob() throws Exception {

        LogInitializer.initialize();
        RepositoryInitializer.initialize(diConfigFilePath);

        executor = new NablarchListenerExecutor<NablarchJobListener>(LISTENER_LIST_NAME, jobContext);
        executor.executeBefore(new NablarchListenerExecutor.Runner<NablarchJobListener>() {
            @Override
            public void run(NablarchJobListener listener, NablarchListenerContext context) {
                listener.beforeJob(context);
            }
        });
    }

    /**
     * {@link NablarchJobListener#afterJob(NablarchListenerContext)}を逆順で実行する。
     *
     * @throws Exception {@link NablarchJobListener#afterJob(NablarchListenerContext)}実行時に送出された例外
     */
    @Override
    public void afterJob() throws Exception {
        if (executor != null) {
            executor.executeAfter(new NablarchListenerExecutor.Runner<NablarchJobListener>() {
                @Override
                public void run(NablarchJobListener listener, NablarchListenerContext context) {
                    listener.afterJob(context);
                }
            });
        }
    }
}
