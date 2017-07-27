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

    /** 全体のTPS */
    private final double tps;

    /** 今回のTPS */
    private final double currentTps;

    /** 推定終了時間 */
    private final Date estimatedEndTime;

    /** 残り件数 */
    private final long remainingCount;

    /**
     * 全体のTPS、今回のTPS、推定終了時間、残り件数を元にオブジェクトを構築する。
     * @param tps 全体のTPS
     * @param currentTps 今回のTPS
     * @param estimatedEndTime 推定終了時間
     * @param remainingCount 残り件数
     */
    public Progress(final double tps, final double currentTps, final Date estimatedEndTime, final long remainingCount) {
        this.tps = tps;
        this.currentTps = currentTps;
        this.estimatedEndTime = estimatedEndTime;
        this.remainingCount = remainingCount;
    }

    /**
     * 全体のTPSを返す。
     *
     * @return 全体のTPS
     */
    public double getTps() {
        return tps;
    }

    /**
     * 今回のTPSを返す。
     *
     * @return 今回のTPS
     */
    public double getCurrentTps() {
        return currentTps;
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
