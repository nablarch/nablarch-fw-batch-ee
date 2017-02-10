package nablarch.fw.batch.progress;

import nablarch.core.log.Logger;
import nablarch.core.log.LoggerManager;

/**
 * 進捗状況をログに出力するロガー。
 *
 * @author siosio
 */
public final class ProgressLogger {

    /** ロガー */
    private static final Logger LOGGER = LoggerManager.get("progress");

    /** 隠蔽コンストラクタ */
    private ProgressLogger() {
    }

    /**
     * メッセージをログに出力する。
     * @param message メッセージ
     */
    public static void write(final String message) {
        LOGGER.logInfo(message);
    }
}
