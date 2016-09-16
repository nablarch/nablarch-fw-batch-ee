package nablarch.fw.batch.ee.listener.step;

import nablarch.common.handler.DbConnectionManagementHandler;
import nablarch.fw.batch.ee.listener.NablarchListenerContext;

/**
 * バッチ処理で必要となるデータベース接続をスレッドローカル上で管理する{@link javax.batch.api.listener.StepListener}実装クラス。
 * <p/>
 *
 * @author Hisaaki Shioiri
 */
public class DbConnectionManagementListener extends AbstractNablarchStepListener {

    /** データベース接続ハンドラ */
    private DbConnectionManagementHandler dbConnectionManagementHandler;

    /**
     * データベース接続ハンドラを設定する。
     * @param dbConnectionManagementHandler データベース接続ハンドラ
     */
    public void setDbConnectionManagementHandler(
            DbConnectionManagementHandler dbConnectionManagementHandler) {
        this.dbConnectionManagementHandler = dbConnectionManagementHandler;
    }

    /**
     * {@link DbConnectionManagementHandler}を使用してデータベース接続を{@link nablarch.core.db.connection.DbConnectionContext}に登録する。
     *
     */
    @Override
    public void beforeStep(NablarchListenerContext context) {
        dbConnectionManagementHandler.before();
    }

    /**
     * {@link DbConnectionManagementHandler}を使用してデータベース接続を開放する。
     */
    @Override
    public void afterStep(NablarchListenerContext context) {
        dbConnectionManagementHandler.after();
    }
}

