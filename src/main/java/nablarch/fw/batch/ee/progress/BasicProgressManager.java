package nablarch.fw.batch.ee.progress;

import jakarta.batch.operations.BatchRuntimeException;
import jakarta.batch.runtime.Metric;
import jakarta.batch.runtime.Metric.MetricType;
import jakarta.batch.runtime.context.JobContext;
import jakarta.batch.runtime.context.StepContext;
import jakarta.enterprise.inject.Typed;
import jakarta.inject.Inject;

import nablarch.fw.batch.ee.cdi.StepScoped;
import nablarch.fw.batch.progress.ProcessedCountBasedProgressCalculator;
import nablarch.fw.batch.progress.Progress;
import nablarch.fw.batch.progress.ProgressCalculator;
import nablarch.fw.batch.progress.ProgressLogPrinter;
import nablarch.fw.batch.progress.ProgressLogger;
import nablarch.fw.batch.progress.ProgressPrinter;

/**
 * 進捗を管理するステップスコープの{@link ProgressManager}実装クラス。
 *
 * @author siosio
 */
@StepScoped
@Typed(ProgressManager.class)
public class BasicProgressManager implements ProgressManager {

    /** ステップコンテキスト */
    private final StepContext stepContext;
    
    /** プロセス名 */
    private final JBatchProcessName processName;

    /** 進捗状況をログに出力する機能 */
    private final ProgressPrinter progressPrinter = new ProgressLogPrinter();

    /** 入力件数 */
    private long inputCount;

    /** 進捗状況を求める機能 */
    private ProgressCalculator calculator;

    /**
     * コンストラクタ。
     *
     * @param jobContext ジョブコンテキスト
     * @param stepContext ステップコンテキスト
     */
    @Inject
    public BasicProgressManager(final JobContext jobContext, final StepContext stepContext) {
        this.stepContext = stepContext;
        processName = new JBatchProcessName(jobContext.getJobName(), stepContext.getStepName());
    }

    @Override
    public void setInputCount(final long inputCount) {
        if (inputCount < 0L) {
            throw new IllegalArgumentException("invalid input count. must set 0 or more. "
                    + processName.formatProcessName() + " input count: [" + inputCount + ']');
        }
            
        this.inputCount = inputCount;
        calculator = new ProcessedCountBasedProgressCalculator(inputCount);
        ProgressLogger.write(processName.formatProcessName() + " input count: [" + inputCount + ']');
    }

    @Override
    public void outputProgressInfo() {
        outputProgressInfo(getReadCount());
    }

    @Override
    public void outputProgressInfo(final long processedCount) {
        verifyStatus(processedCount);
        // 入力件数が0の場合は進捗を出力する必要が無いので何もしない。
        if (inputCount == 0L) {
            return;
        }
        final Progress progress = calculator.calculate(processedCount);
        progressPrinter.print(processName, progress);
    }

    /**
     * 状態が正しいか検証する。
     * @param processedCount 処理済み件数
     */
    private void verifyStatus(final long processedCount) {
        if (calculator == null) {
            throw new IllegalStateException("input count is not set. must set input count. " + processName.formatProcessName());
        }
        if (inputCount == 0L && processedCount != 0L) {
            throw new IllegalArgumentException("invalid processed count. processed count must set 0 because input count is 0. "
                    + processName.formatProcessName() + " input count: [" + inputCount + "] processed count: [" + processedCount + ']');
        }
    }

    /**
     * ステップコンテキストから読み取った件数を取得する。
     *
     * @return 読み取った件数
     */
    protected long getReadCount() {
        for (final Metric metric : stepContext.getMetrics()) {
            if (metric.getType() == MetricType.READ_COUNT) {
                return metric.getValue();
            }
        }
        // 到達不能
        throw new BatchRuntimeException("failed to StepContext#getMetrics");
    }
}

