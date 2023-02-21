package nablarch.fw.batch.ee.listener.step;

import jakarta.batch.runtime.BatchStatus;

import nablarch.core.transaction.Transaction;
import nablarch.core.transaction.TransactionContext;
import nablarch.core.transaction.TransactionFactory;
import nablarch.core.util.annotation.Published;
import nablarch.fw.batch.ee.listener.NablarchListenerContext;

/**
 * Stepレベルのトランザクション制御を行う{@link NablarchStepListener}実装クラス。
 *
 * @author Hisaaki Shioiri
 */
@Published(tag = "architect")
public class StepTransactionManagementListener extends AbstractNablarchStepListener {

    /** トランザクションファクトリ */
    private TransactionFactory transactionFactory;

    /** トランザクション名 */
    private String transactionName = TransactionContext.DEFAULT_TRANSACTION_CONTEXT_KEY;

    /**
     * トランザクションファクトリを設定する。
     *
     * @param transactionFactory トランザクションファクトリ
     */
    public void setTransactionFactory(final TransactionFactory transactionFactory) {
        this.transactionFactory = transactionFactory;
    }

    /**
     * トランザクション名
     *
     * @param transactionName トランザクション名
     */
    public void setTransactionName(final String transactionName) {
        this.transactionName = transactionName;
    }

    /**
     * 新しいトランザクションを生成し、コンテキストに設定する。
     */
    @Override
    public void beforeStep(final NablarchListenerContext context) {
        final Transaction transaction = transactionFactory.getTransaction(transactionName);
        TransactionContext.setTransaction(transactionName, transaction);
        transaction.begin();
    }

    /**
     * トランザクションを終了しコンテキストから削除する。
     * <p/>
     * ステップの実行に失敗した場合({@link jakarta.batch.runtime.context.StepContext#getException()}が設定されている場合や
     * {@link jakarta.batch.runtime.context.JobContext#getBatchStatus()}が{@link BatchStatus#FAILED}の場合)には、
     * トランザクションをロールバックする。
     */
    @Override
    public void afterStep(final NablarchListenerContext context) {
        if (!TransactionContext.containTransaction(transactionName)) {
            // トランザクションが開始されていない場合(beforeが失敗した場合)は何もしない
            return;
        }
        final Transaction transaction = TransactionContext.getTransaction(transactionName);
        TransactionContext.removeTransaction(transactionName);
        if (isStepCompleted(context)) {
            transaction.commit();
        } else {
            transaction.rollback();
        }
    }

    /**
     * ステップの処理が完了しているか否か。
     *
     * @param context {@link NablarchListenerContext}
     * @return 正常に完了している場合は{@code true}
     */
    protected boolean isStepCompleted(final NablarchListenerContext context) {
        return context.isStepProcessSucceeded();
    }
}
