package nablarch.fw.batch.progress;

import java.util.Date;

/**
 * 処理対象件数及び処理済みの件数を元に進捗を求めるクラス。
 *
 * @author siosio
 */
public class ProcessedCountBasedProgressCalculator implements ProgressCalculator {
    
    /** TPSを求めるオブジェクト */
    private final TpsCalculator tpsCalculator = new TpsCalculator();
    
    /** 推定終了時間を求めるオブジェクト */
    private final EstimatedEndTimeCalculator estimatedEndTimeCalculator = new EstimatedEndTimeCalculator();

    /** 処理対象件数 */
    private final long inputCount;

    /** 処理開始時間 */
    private final long startTime;

    /**
     * 処理対象件数を元にオブジェクトを構築する。
     *
     * @param inputCount 処理対象件数
     */
    public ProcessedCountBasedProgressCalculator(final long inputCount) {
        this.inputCount = inputCount;
        startTime = System.nanoTime();
    }

    /**
     * 処理済み件数を元に進捗を求める。
     *
     * @param processedCount 処理済み件数
     * @return 進捗
     */
    @Override
    public Progress calculate(final long processedCount) {
        // 処理済み件数がゼロの場合は終了予測不能
        if (processedCount == 0L) {
            return new Progress(0.0, null, inputCount);
        }
        final double tps = tpsCalculator.calculate(startTime, processedCount);
        final Date estimatedEndTime = estimatedEndTimeCalculator.calculate(inputCount, processedCount, tps);
        return new Progress(tps, estimatedEndTime, inputCount - processedCount);
    }
}
