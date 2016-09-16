package nablarch.fw.batch.ee.listener.chunk;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import javax.batch.operations.BatchRuntimeException;
import javax.batch.runtime.context.JobContext;
import javax.batch.runtime.context.StepContext;

import nablarch.core.transaction.Transaction;
import nablarch.core.transaction.TransactionContext;

import nablarch.fw.batch.ee.listener.NablarchListenerContext;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import mockit.Mocked;
import mockit.Verifications;

/**
 * {@link ItemWriteTransactionManagementListener}のテストクラス。
 *
 * @author Hisaaki Shioiri
 */
@RunWith(Parameterized.class)
public class ItemWriteTransactionManagementListenerTest {

    /** テスト対象 */
    ItemWriteTransactionManagementListener sut = new ItemWriteTransactionManagementListener();

    /** テストで使用するトランザクション名 */
    String transactionName;
    
    // ------------------------------ Mock Objects
    @Mocked
    Transaction mockTransaction;

    @Mocked
    JobContext mockJobContext;

    @Mocked
    StepContext mockStepContext;

    @Parameters
    public static Collection<String[][]> parameters() {
        return Arrays.asList(
                new String[][] {{null}},
                new String[][] {{"test-tran"}}
        );
    }

    public ItemWriteTransactionManagementListenerTest(String[] params) {
        transactionName = params[0] == null ? TransactionContext.DEFAULT_TRANSACTION_CONTEXT_KEY : params[0];
    }

    @Before
    public void setUp() throws Exception {
        TransactionContext.setTransaction(transactionName, mockTransaction);
        if (!transactionName.equals(TransactionContext.DEFAULT_TRANSACTION_CONTEXT_KEY)) {
            // デフォルト名以外の場合にトランザクション名を設定する。
            sut.setTransactionName(transactionName);
        }
    }

    @After
    public void tearDown() throws Exception {
        TransactionContext.removeTransaction(transactionName);
    }

    /**
     * トランザクションの確定がされること。
     */
    @Test
    public void testCommit() throws Exception {
        sut.afterWrite(new NablarchListenerContext(mockJobContext, mockStepContext), Collections.emptyList());

        new Verifications() {{
            mockTransaction.commit();
            times = 1;
            mockTransaction.rollback();
            times = 0;
        }};
    }

    /**
     * トランザクションが破棄されること。
     */
    @Test
    public void testRollback() throws Exception {
        sut.onWriteError(new NablarchListenerContext(mockJobContext, mockStepContext), Collections.emptyList(),
                new BatchRuntimeException());

        new Verifications() {{
            mockTransaction.rollback();
            times = 1;
            mockTransaction.commit();
            times = 0;
        }};
    }
}
