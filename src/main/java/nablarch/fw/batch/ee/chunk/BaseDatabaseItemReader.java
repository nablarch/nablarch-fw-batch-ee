package nablarch.fw.batch.ee.chunk;

import java.io.Serializable;

import jakarta.batch.api.chunk.AbstractItemReader;

import nablarch.core.db.connection.AppDbConnection;
import nablarch.core.db.connection.ConnectionFactory;
import nablarch.core.db.connection.DbConnectionContext;
import nablarch.core.db.connection.TransactionManagerConnection;
import nablarch.core.repository.SystemRepository;
import nablarch.core.transaction.TransactionContext;
import nablarch.core.util.annotation.Published;

/**
 * データベースを入力とする{@link jakarta.batch.api.chunk.ItemReader}の抽象クラス。
 * <p/>
 * 本リーダを継承することで、リーダ専用のコネクションを使用してデータを読み込むことができる。
 * <p/>
 * DB製品によっては、トランザクション制御時にカーソルが閉じられてしまうため、リーダ専用のコネクションを使用して読み込みを行っている。
 *
 * @author Naoki Yamamoto
 */
@Published
public abstract class BaseDatabaseItemReader extends AbstractItemReader {

    /** リーダ専用のコネクション */
    private TransactionManagerConnection connection;

    @Override
    public final void open(final Serializable checkpoint) throws Exception {
        
        // 元々設定されていたデフォルトのデータベース接続を退避する
        AppDbConnection originalConnection = null;
        if (DbConnectionContext.containConnection(TransactionContext.DEFAULT_TRANSACTION_CONTEXT_KEY)) {
            originalConnection = DbConnectionContext.getConnection();
            DbConnectionContext.removeConnection();
        }
        try {
            // リーダのopen用に新しい接続をデフォルトの接続とする。
            final ConnectionFactory connectionFactory = SystemRepository.get("connectionFactory");
            if (connectionFactory == null) {
                throw new IllegalStateException("ConnectionFactory was not found." 
                        + " must be set ConnectionFactory(component name=connectionFactory).");
            }
            connection = connectionFactory.getConnection(TransactionContext.DEFAULT_TRANSACTION_CONTEXT_KEY);
            DbConnectionContext.setConnection(connection);
            doOpen(checkpoint);
        } finally {
            // 退避した接続を元に戻す
            if (originalConnection != null) {
                DbConnectionContext.removeConnection();
                DbConnectionContext.setConnection(originalConnection);
            }
        }
    }

    @Override
    public final void close() throws Exception {
        try {
            doClose();
        } finally {
            if (connection != null) {
                connection.terminate();
            }
        }
    }

    /**
     * データベースからのデータ読み込みを行う。
     * @param checkpoint チェックポイント
     * @throws Exception 発生した例外
     */
    protected abstract void doOpen(final Serializable checkpoint) throws Exception;
    
    /**
     * リーダの終了処理（リソースの解放など）を行う。
     * @throws Exception 発生した例外
     */
    protected void doClose() throws Exception {
        // nop
    }
}
