package nablarch.fw.batch.progress;

import java.util.Date;

import nablarch.core.util.annotation.Published;

/**
 * 進捗状況を保持するクラス。
 *
 * @author siosio
 */
@Published(tag = "architect")
public class Progress {

    /** TPS */
    private final double tps;

    /** 推定終了時間 */
    private final Date estimatedEndTime;

    /** 残り件数 */
    private final long remainingCount;

    /**
     * TPSと推定終了時間を元にオブジェクトを構築する。
     * 
     * @param tps TPS
     * @param estimatedEndTime 推定終了時間
     * @param remainingCount 残り件数
     */
    public Progress(final double tps, final Date estimatedEndTime, final long remainingCount) {
        this.tps = tps;
        this.estimatedEndTime = estimatedEndTime;
        this.remainingCount = remainingCount;
    }

    /**
     * TPSを返す。
     *
     * @return TPS
     */
    public double getTps() {
        return tps;
    }

    /**
     * 終了予測時間を返す。
     *
     * @return 終了予測時間
     */
    public Date getEstimatedEndTime() {
        return estimatedEndTime;
    }

    /**
     * 残り件数を返す。
     *
     * @return 残り件数
     */
    public long getRemainingCount() {
        return remainingCount;
    }
}
