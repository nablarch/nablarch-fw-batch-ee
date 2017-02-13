package nablarch.fw.batch.ee.progress;

import javax.batch.operations.BatchRuntimeException;
import javax.batch.runtime.Metric;
import javax.batch.runtime.Metric.MetricType;
import javax.batch.runtime.context.JobContext;
import javax.batch.runtime.context.StepContext;
import javax.enterprise.inject.Typed;
import javax.inject.Inject;

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
        calculator = new ProcessedCountBasedProgressCalculator(inputCount);
        ProgressLogger.write(processName.formatProcessName() + " input count: [" + inputCount + ']');
    }

    @Override
    public void outputProgressInfo() {
        outputProgressInfo(getReadCount());
    }

    @Override
    public void outputProgressInfo(final long processedCount) {
        if (calculator == null) {
            throw new IllegalStateException("input count is not set. must set input count. " + processName.formatProcessName());
        }
        final Progress progress = calculator.calculate(processedCount);
        progressPrinter.print(processName, progress);
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

