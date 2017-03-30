package nablarch.fw.batch.ee.listener.step;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.Collection;

import javax.batch.runtime.BatchStatus;
import javax.batch.runtime.context.JobContext;
import javax.batch.runtime.context.StepContext;

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

import mockit.Expectations;
import mockit.Mocked;
import mockit.Verifications;

/**
 * {@link StepTransactionManagementListener}のテストクラス
 */
@RunWith(Parameterized.class)
public class StepTransactionManagementListenerTest {

    /** テスト対象 */
    private final StepTransactionManagementListener sut = new StepTransactionManagementListener();

    // ------------------------------ mock object
    @Mocked
    private JobContext mockJobContext;

    @Mocked
    private StepContext mockStepContext;

    @Mocked
    private TransactionFactory mockTransactionFactory;

    @Mocked
    private Transaction mockTransaction;

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
        new Expectations() {{
            mockTransactionFactory.getTransaction(transactionName);
            result = mockTransaction;
        }};

        sut.beforeStep(new NablarchListenerContext(mockJobContext, mockStepContext));

        // トランザクションの開始処理が呼び出されていることを検証
        new Verifications() {{
            mockTransaction.begin();
            times = 1;
        }};

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
        new Expectations() {{
            mockTransactionFactory.getTransaction(transactionName);
            result = mockTransaction;
        }};

        sut.beforeStep(new NablarchListenerContext(mockJobContext, mockStepContext));
        sut.afterStep(new NablarchListenerContext(mockJobContext, mockStepContext));

        // トランザクションの確定処理(commit)が呼び出されていることを検証
        new Verifications() {{
            mockTransaction.commit();
            times = 1;
        }};

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
        new Expectations() {{
            mockTransactionFactory.getTransaction(transactionName);
            result = mockTransaction;

            mockStepContext.getException();
            returns(new RuntimeException("step error."));
        }};

        sut.beforeStep(new NablarchListenerContext(mockJobContext, mockStepContext));
        sut.afterStep(new NablarchListenerContext(mockJobContext, mockStepContext));

        // トランザクションの破棄処理(rollback)が呼び出されていることを検証する。
        new Verifications() {{
            mockTransaction.begin();
            times = 1;
            mockTransaction.rollback();
            times = 1;
        }};

        assertThat("トランザクションがコンテキストから削除されていること",
                TransactionContext.containTransaction(transactionName), is(false));
    }

    /**
     * {@link StepTransactionManagementListener#afterStep(NablarchListenerContext)}でトランザクションが破棄されること。
     */
    @Test
    public void testAfterStepJobFailed() throws Exception {
        new Expectations() {{
            mockTransactionFactory.getTransaction(transactionName);
            result = mockTransaction;

            mockJobContext.getBatchStatus();
            result = BatchStatus.FAILED;
        }};

        sut.beforeStep(new NablarchListenerContext(mockJobContext, mockStepContext));
        sut.afterStep(new NablarchListenerContext(mockJobContext, mockStepContext));

        new Verifications() {{
            mockTransaction.begin();
            times = 1;
            mockTransaction.rollback();
            times = 1;
            mockTransaction.commit();
            times = 0;
        }};
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
        new Expectations() {{
            mockTransactionFactory.getTransaction(transactionName);
            result = mockTransaction;

            mockStepContext.getBatchStatus();
            result = BatchStatus.FAILED;
        }};

        sut.beforeStep(new NablarchListenerContext(mockJobContext, mockStepContext));
        sut.afterStep(new NablarchListenerContext(mockJobContext, mockStepContext));

        new Verifications() {{
            mockTransaction.begin();
            times = 1;
            mockTransaction.rollback();
            times = 1;
            mockTransaction.commit();
            times = 0;
        }};

        assertThat("トランザクションがコンテキストから削除されていること",
                TransactionContext.containTransaction(transactionName), is(false));
    }

    /**
     * {@link StepTransactionManagementListener#beforeStep(NablarchListenerContext)}でトランザクション生成に失敗した場合のテスト。
     * <p/>
     * {@link StepTransactionManagementListener#afterStep(NablarchListenerContext)}処理では予期せぬ例外が発生しないこと。
     */
    @Test
    public void testGetTransactionFailed(@Mocked TransactionContext mockContext) throws Exception {
        new Expectations() {{
            mockTransactionFactory.getTransaction(transactionName);
            result = new IllegalArgumentException("invalid transaction name.");
        }};
    
        try {
            sut.beforeStep(new NablarchListenerContext(mockJobContext, mockStepContext));
            fail("例外が発生するのでここはとおらない");
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), is("invalid transaction name."));
    
        }
    
        sut.afterStep(new NablarchListenerContext(mockJobContext, mockStepContext));
    
        // トランザクションが開始されていないので、トランザクションの破棄処理は実行されないこと
        new Verifications() {{
            TransactionContext.removeTransaction(transactionName);
            times = 0;
        }};
    }
}

