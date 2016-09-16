package nablarch.fw.batch.ee.listener.chunk;

import java.util.List;

import nablarch.core.transaction.Transaction;
import nablarch.core.transaction.TransactionContext;
import nablarch.fw.batch.ee.listener.NablarchListenerContext;

/**
 * {@link javax.batch.api.chunk.listener.ItemWriteListener}レベルでトランザクション制御を行う{@link NablarchItemWriteListener}の実装クラス。
 * <p/>
 * {@link TransactionContext}から{@link Transaction}を取得しトランザクション制御を行う。
 * {@link Transaction}は、前段のリスナーにて{@link TransactionContext}に設定しておく必要がある。
 * {@link #setTransactionName(String)}で設定するトランザクション名は、前段のリスナーで設定したトランザクション名と一致させる必要がある。
 * 複数のトランザクションを設定する必要がないのであれば、デフォルトのトランザクション名を使用することを推奨する。
 * (デフォルトのトランザクション名は、設定不要の場合に自動的に選択される。)
 * <p/>
 * {@link javax.batch.api.chunk.ItemWriter}が正常に終了した場合には、トランザクションの確定({@link Transaction#commit()})を実行し、
 * {@link javax.batch.api.chunk.ItemWriter}で{@link Exception}が発生した場合には、トランザクションの破棄({@link Transaction#rollback()}を行う。
 *
 * @author Hisaaki Shioiri
 */
public class ItemWriteTransactionManagementListener extends AbstractNablarchItemWriteListener {

    /** トランザクション名 */
    private String transactionName = TransactionContext.DEFAULT_TRANSACTION_CONTEXT_KEY;

    /**
     * トランザクション名を設定する。
     *
     * @param transactionName トランザクション名
     */
    public void setTransactionName(final String transactionName) {
        this.transactionName = transactionName;
    }

    /**
     * トランザクションを確定(commit)する。
     */
    @Override
    public void afterWrite(
            final NablarchListenerContext context,
            final List<Object> items) {
        final Transaction transaction = TransactionContext.getTransaction(transactionName);
        if (context.isProcessSucceeded()) {
            transaction.commit();
        }
    }

    /**
     * トランザクションを破棄(rollback)する。
     */
    @Override
    public void onWriteError(
            final NablarchListenerContext context,
            final List<Object> items,
            final Exception ex) {
        final Transaction transaction = TransactionContext.getTransaction(transactionName);
        transaction.rollback();
    }
}
