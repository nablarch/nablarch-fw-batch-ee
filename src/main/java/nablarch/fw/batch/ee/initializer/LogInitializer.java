package nablarch.fw.batch.ee.initializer;

import nablarch.core.log.app.FailureLogUtil;
import nablarch.core.log.app.LogInitializationHelper;

/**
 * ログの初期化を行うクラス。
 *
 * @author Naoki Yamamoto
 */
public class LogInitializer {

    /** 初期化済みフラグ */
    static boolean isInitialized = false;

    /** 本クラスはインスタンスを作成しない。 */
private LogInitializer() {
    }

    /**
     * ログの初期化を行う。<br />
     * 別のジョブの実行等により既にログの初期化が行われている場合は何もしない。
     */
    public static synchronized void initialize() {
        if (!isInitialized) {
            FailureLogUtil.initialize();
            LogInitializationHelper.initialize();
            isInitialized = true;
        }
    }


}
