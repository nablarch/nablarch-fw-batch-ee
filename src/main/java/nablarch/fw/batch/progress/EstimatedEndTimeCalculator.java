package nablarch.fw.batch.progress;

import java.util.Date;
import java.util.concurrent.TimeUnit;

import nablarch.core.util.annotation.Published;

/**
 * 推定終了時間を求めるクラス。
 *
 * @author siosio
 */
@Published(tag = "architect")
public class EstimatedEndTimeCalculator {

    /**
     * 推定終了時間を求める。
     * @param inputCount 処理対象件数
     * @param processedCount 処理済み件数
     * @param tps TPS
     * @return 推定終了時間
     */
    public Date calculate(final long inputCount, final long processedCount, final double tps) {
        verifyParameter(inputCount, processedCount, tps);
        
        final long remainsCount = inputCount - processedCount;
        final long remainsSeconds = (long) (remainsCount / tps);
        return new Date(System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(remainsSeconds));
    }

    /**
     * 終了時間を求めるための情報が有効な値であることを検証する。
     * @param inputCount 処理対象件数
     * @param processedCount 処理済み件数
     * @param tps TPS
     */
    private static void verifyParameter(final long inputCount, final long processedCount, final double tps) {
        if (inputCount <= 0L) {
            throw new IllegalArgumentException("input count is invalid. input count must set 1 or more.");
        }
        if (processedCount <= 0L) {
            throw new IllegalArgumentException("processed count is invalid. processed count must set 1 or more.");
        }
        if (inputCount < processedCount) {
            throw new IllegalArgumentException("processed count is invalid. processed count must set less than input count.");
        }
        if (tps <= 0.0D) {
            throw new IllegalArgumentException("tps is invalid. tps must set 1 or more.");
        }
    }
}
