package nablarch.fw.batch.progress;

import nablarch.core.util.annotation.Published;

/**
 * 進捗状況を出力するインタフェース。
 *
 * @author siosio
 */
@Published(tag = "architect")
public interface ProgressPrinter {

    /**
     * 進捗状況を出力する。
     *
     * @param processName プロセス名
     * @param progress 進捗状況
     */
    void print(final ProcessName processName, Progress progress);
}
