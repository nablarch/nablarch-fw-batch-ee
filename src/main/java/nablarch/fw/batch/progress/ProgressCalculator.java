package nablarch.fw.batch.progress;

/**
 * バッチの進捗状況を求めるインタフェース。
 *
 * @author siosio
 */
public interface ProgressCalculator {

    /**
     * 処理済み件数からバッチの進捗状況を求める。
     *
     * @param processedCount 処理済み件数
     * @return 進捗状況
     */
    Progress calculate(long processedCount);
}
