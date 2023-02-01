package nablarch.fw.batch.ee.listener;

import jakarta.batch.runtime.BatchStatus;
import jakarta.batch.runtime.Metric;
import jakarta.batch.runtime.Metric.MetricType;
import jakarta.batch.runtime.context.JobContext;
import jakarta.batch.runtime.context.StepContext;

import nablarch.core.util.annotation.Published;

/**
 * リスナー実行時のコンテキスト情報を保持するクラス。
 *
 * @author Kiyohito Itoh
 */
@Published(tag = "architect")
public class NablarchListenerContext {

    /** {@link JobContext } */
    private JobContext jobContext;

    /** {@link StepContext} */
    private StepContext stepContext;

    /** 処理結果 */
    private boolean processSucceeded = true;

    /**
     * コンストラクタ。
     * @param jobContext {@link JobContext}
     * @param stepContext {@link StepContext}
     */
    public NablarchListenerContext(final JobContext jobContext, final StepContext stepContext) {
        this.jobContext = jobContext;
        this.stepContext = stepContext;
    }

    /**
     * 後続するリスナーの処理が正常終了したかどうかを取得する。
     * @return 正常終了した場合 true
     */
    public boolean isProcessSucceeded() {
        return processSucceeded;
    }

    /**
     * STEPレベルのリスナーの処理が正常終了したかどうかを取得する。
     * @return 正常終了した場合 true
     */
    public boolean isStepProcessSucceeded() {
        return isProcessSucceeded()
                && stepContext.getException() == null
                && stepContext.getBatchStatus() != BatchStatus.FAILED
                && jobContext.getBatchStatus() != BatchStatus.FAILED;
    }

    /**
     * 処理が正常終了したかどうかを設定する。
     * @param processSucceeded 正常終了の場合 true
     * @return 自分自身
     */
    public NablarchListenerContext setProcessSucceeded(final boolean processSucceeded) {
        this.processSucceeded = processSucceeded;
        return this;
    }

    /**
     * ステップの終了ステータスを取得する。
     *
     * @return ステップの終了ステータス
     */
    public String getStepExitStatus() {
        return stepContext.getExitStatus();
    }

    /**
     * 終了ステータスを取得する。
     * @return 終了ステータス
     */
    public String getExitStatus() {
        return jobContext.getExitStatus();
    }

    /**
     * 終了ステータスを設定する。
     * @param exitStatus 終了ステータス
     */
    public NablarchListenerContext setExitStatus(String exitStatus) {
        jobContext.setExitStatus(exitStatus);
        return this;
    }

    /**
     * JOBのバッチステータスを取得する。
     * @return JOBのバッチステータス
     */
    public BatchStatus getJobBatchStatus() {
        return jobContext.getBatchStatus();
    }

    /**
     * ジョブ名を取得する。
     * @return ジョブ名
     */
    public String getJobName() {
        return jobContext.getJobName();
    }

    /**
     * ステップ名を取得する。
     * @return ステップ名
     */
    public String getStepName() {
        return stepContext.getStepName();
    }

    /**
     * chunk実行時のREAD_COUNTを取得する。
     * @return READ_COUNT
     */
    public long getReadCount() {
        return getValueFromMetrics(MetricType.READ_COUNT);
    }

    /**
     * {@link JobContext}を返す。
     *
     * @return JobContext
     */
    public JobContext getJobContext() {
        return jobContext;
    }

    /**
     * {@link StepContext}を返す。
     * <p>
     * ステップの情報が取れない場合には、{@code null}を返す。
     *
     * @return StepContext
     */
    public StepContext getStepContext() {
        return stepContext;
    }

    /**
     * stepContextのMetricからtypeの値を取得する。
     * @param type 取得したい値のタイプ
     * @return typeの値
     */
    private long getValueFromMetrics(MetricType type) {
        Metric[] metric = stepContext.getMetrics();
        for (Metric m : metric) {
            if (m.getType() == type) {
                return m.getValue();
            }
        }
        return 0;
    }
}
