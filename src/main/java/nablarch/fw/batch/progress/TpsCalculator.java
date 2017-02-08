package nablarch.fw.batch.progress;

import java.util.concurrent.TimeUnit;

import nablarch.core.util.annotation.Published;

/**
 * TPSを算出するクラス。
 *
 * @author siosio
 */
@Published(tag = "architect")
public class TpsCalculator {

    /**
     * 処理開始時間と処理済み件数からTPSを求める。
     *
     * @param startTime 処理開始時間(ナノ秒)
     * @param processedCount 処理済み件数
     * @return 求めたTPS
     */
    public double calculate(final long startTime, final long processedCount) {
        if (processedCount <= 0L) {
            throw new IllegalArgumentException("processed count is invalid. processing count must set 1 or more.");
        }
        final long processedTime = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);
        if (processedTime <= 0L) {
            throw new IllegalArgumentException("start time is invalid. start time must set past time.");
        }
        final double tps = (double) processedCount / processedTime;
        return tps * 1000;
    }
}
