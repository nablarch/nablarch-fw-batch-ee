package nablarch.fw.batch.ee.listener.step;

import jakarta.batch.runtime.BatchStatus;
import jakarta.batch.runtime.context.JobContext;
import jakarta.batch.runtime.context.StepContext;
import nablarch.core.repository.SystemRepository;
import nablarch.core.transaction.Transaction;
import nablarch.core.transaction.TransactionContext;
import nablarch.core.transaction.TransactionFactory;
import nablarch.fw.batch.ee.listener.NablarchListenerContext;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.MockedStatic;

import java.util.Arrays;
import java.util.Collection;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link StepTransactionManagementListener}のテストクラス
 */
@RunWith(Parameterized.class)
public class StepTransactionManagementListenerTest {

    /** テスト対象 */
    private final StepTransactionManagementListener sut = new StepTransactionManagementListener();

    // ------------------------------ mock object
    private final JobContext mockJobContext = mock(JobContext.class);

    private final StepContext mockStepContext = mock(StepContext.class);

    private final TransactionFactory mockTransactionFactory = mock(TransactionFactory.class);

    private final Transaction mockTransaction = mock(Transaction.class);

    @Parameters
    public static Collection<String[][]> parameters() {
        return Arrays.asList(
                new String[][] {{null}},
                new String[][] {{"customTransactionName"}}
        );
    }

    /** テストで使用するトランザクション名 */
    private final String transactionName;

    public StepTransactionManagementListenerTest(String[] parameters) {
        transactionName = parameters[0] == null ? TransactionContext.DEFAULT_TRANSACTION_CONTEXT_KEY : parameters[0];
    }

    @Before
    public void setUp() throws Exception {
        sut.setTransactionFactory(mockTransactionFactory);

        if (!TransactionContext.DEFAULT_TRANSACTION_CONTEXT_KEY.equals(transactionName)) {
            // デフォルトのトランザクション名以外の場合、
            // テスト対象クラスのプロパティにトランザクション名を設定する。
            sut.setTransactionName(transactionName);
        }
    }

    @After
    public void tearDown() throws Exception {
        TransactionContext.removeTransaction(transactionName);
        SystemRepository.clear();
    }

    /**
     * {@link StepTransactionManagementListener#beforeStep(NablarchListenerContext)}でトランザクションが生成されContextに設定されること。
     * <p/>
     * また、トランザクション開始処理({@link Transaction#begin()})が呼び出されること。
     */
    @Test
    public void testBefore() throws Exception {
        when(mockTransactionFactory.getTransaction(transactionName)).thenReturn(mockTransaction);

        sut.beforeStep(new NablarchListenerContext(mockJobContext, mockStepContext));

        // トランザクションの開始処理が呼び出されていることを検証
        verify(mockTransaction).begin();

        assertThat("Contextにトランザクションが設定されていること",
                TransactionContext.getTransaction(transactionName), is(sameInstance(mockTransaction)));
    }

    /**
     * {@link StepTransactionManagementListener#afterStep(NablarchListenerContext)}でトランザクションが破棄されること。
     * <p/>
     * ステップ処理が正常に終了している場合には、トランザクションがコミットされること。
     */
    @Test
    public void testAfterNormalEnd() throws Exception {
        when(mockTransactionFactory.getTransaction(transactionName)).thenReturn(mockTransaction);

        sut.beforeStep(new NablarchListenerContext(mockJobContext, mockStepContext));
        sut.afterStep(new NablarchListenerContext(mockJobContext, mockStepContext));

        // トランザクションの確定処理(commit)が呼び出されていることを検証
        verify(mockTransaction).commit();

        // トランザクションコンテキストから削除されていることを検証する。
        try {
            TransactionContext.getTransaction(transactionName);
            fail("ここは通らない。");
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), containsString("specified transaction name is not register in thread local."));
        }
    }

    /**
     * {@link StepTransactionManagementListener#afterStep(NablarchListenerContext)}でトランザクションが破棄されること。
     * <p/>
     * ステップ処理で例外が発生している場合には、トランザクションのロールバックが行われること。
     */
    @Test
    public void testAfterStepException() throws Exception {
        when(mockTransactionFactory.getTransaction(transactionName)).thenReturn(mockTransaction);

        when(mockStepContext.getException())
                .thenReturn(new RuntimeException("step error."))
                .thenReturn(null);

        sut.beforeStep(new NablarchListenerContext(mockJobContext, mockStepContext));
        sut.afterStep(new NablarchListenerContext(mockJobContext, mockStepContext));

        // トランザクションの破棄処理(rollback)が呼び出されていることを検証する。
        verify(mockTransaction).begin();
        verify(mockTransaction).rollback();

        assertThat("トランザクションがコンテキストから削除されていること",
                TransactionContext.containTransaction(transactionName), is(false));
    }

    /**
     * {@link StepTransactionManagementListener#afterStep(NablarchListenerContext)}でトランザクションが破棄されること。
     */
    @Test
    public void testAfterStepJobFailed() throws Exception {
        when(mockTransactionFactory.getTransaction(transactionName)).thenReturn(mockTransaction);
        when(mockJobContext.getBatchStatus()).thenReturn(BatchStatus.FAILED);

        sut.beforeStep(new NablarchListenerContext(mockJobContext, mockStepContext));
        sut.afterStep(new NablarchListenerContext(mockJobContext, mockStepContext));

        verify(mockTransaction).begin();
        verify(mockTransaction).rollback();
        verify(mockTransaction, never()).commit();
        
        assertThat("トランザクションがコンテキストから削除されていること",
                TransactionContext.containTransaction(transactionName), is(false));
    }

    /**
     * {@link StepTransactionManagementListener#beforeStep(NablarchListenerContext)}でトランザクションが破棄されること。
     *
     * {@link StepContext#getBatchStatus()}がfailedの場合、トランザクションがロールバックされること。
     */
    @Test
    public void testAfterStepStepFailed() throws Exception {
        when(mockTransactionFactory.getTransaction(transactionName)).thenReturn(mockTransaction);
        when(mockStepContext.getBatchStatus()).thenReturn(BatchStatus.FAILED);

        sut.beforeStep(new NablarchListenerContext(mockJobContext, mockStepContext));
        sut.afterStep(new NablarchListenerContext(mockJobContext, mockStepContext));

        verify(mockTransaction).begin();
        verify(mockTransaction).rollback();
        verify(mockTransaction, never()).commit();

        assertThat("トランザクションがコンテキストから削除されていること",
                TransactionContext.containTransaction(transactionName), is(false));
    }

    /**
     * {@link StepTransactionManagementListener#beforeStep(NablarchListenerContext)}でトランザクション生成に失敗した場合のテスト。
     * <p/>
     * {@link StepTransactionManagementListener#afterStep(NablarchListenerContext)}処理では予期せぬ例外が発生しないこと。
     */
    @Test
    public void testGetTransactionFailed() throws Exception {
        when(mockTransactionFactory.getTransaction(transactionName)).thenThrow(new IllegalArgumentException("invalid transaction name."));
        try(final MockedStatic<TransactionContext> mocked = mockStatic(TransactionContext.class)) {
            try {
                sut.beforeStep(new NablarchListenerContext(mockJobContext, mockStepContext));
                fail("例外が発生するのでここはとおらない");
            } catch (IllegalArgumentException e) {
                assertThat(e.getMessage(), is("invalid transaction name."));

            }

            sut.afterStep(new NablarchListenerContext(mockJobContext, mockStepContext));

            // トランザクションが開始されていないので、トランザクションの破棄処理は実行されないこと
            mocked.verify(() -> TransactionContext.removeTransaction(transactionName), never());
        }
    }
}

