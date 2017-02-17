package nablarch.fw.batch.ee.progress;

import nablarch.core.util.annotation.Published;

/**
 * 進捗を管理するインタフェース。
 *
 * @author Naoki Yamamoto
 */
@Published
public interface ProgressManager {

    /**
     * 処理対象の件数を設定する。
     *
     * @param inputCount 処理対象の件数
     */
    void setInputCount(long inputCount);

    /**
     * 進捗状況を出力する。
     */
    void outputProgressInfo();

    /**
     * 進捗状況を出力する。
     *
     * @param processedCount 処理済み件数
     */
    void outputProgressInfo(long processedCount);
}
