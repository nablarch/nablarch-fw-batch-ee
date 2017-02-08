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

    /**
     * TPSと推定終了時間を元にオブジェクトを構築する。
     *
     * @param tps TPS
     * @param estimatedEndTime 推定終了時間
     */
    public Progress(final double tps, final Date estimatedEndTime) {
        this.tps = tps;
        this.estimatedEndTime = estimatedEndTime;
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
}
