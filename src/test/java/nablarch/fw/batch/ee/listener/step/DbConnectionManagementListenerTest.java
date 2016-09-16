package nablarch.fw.batch.ee.listener.step;

import javax.batch.runtime.context.JobContext;
import javax.batch.runtime.context.StepContext;

import nablarch.common.handler.DbConnectionManagementHandler;
import nablarch.core.db.connection.DbConnectionContext;
import nablarch.core.transaction.TransactionContext;

import nablarch.fw.batch.ee.listener.NablarchListenerContext;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import mockit.Mocked;
import mockit.Verifications;

/**
 * {@link DbConnectionManagementListener}のテストクラス。
 *
 * @author Hisaaki Shioiri
 */
public class DbConnectionManagementListenerTest {

    /** テスト対象 */
    private DbConnectionManagementListener sut = new DbConnectionManagementListener();

    // ---------------------------------------- Mock Objects
    @Mocked
    DbConnectionManagementHandler mockDbConnectionManagementHandler;

    @Mocked
    JobContext jobContext;

    @Mocked
    StepContext stepContext;

    @Before
    public void setUp() throws Exception {
        sut.setDbConnectionManagementHandler(mockDbConnectionManagementHandler);
    }

    @After
    public void tearDown() throws Exception {
        DbConnectionContext.removeConnection(TransactionContext.DEFAULT_TRANSACTION_CONTEXT_KEY);
    }

    /**
     * {@link DbConnectionManagementListener#beforeStep(NablarchListenerContext)}のテスト。
     *
     * {@link DbConnectionManagementHandler#before()}が呼び出されることを検証する。
     */
    @Test
    public void testBefore() throws Exception {
        sut.beforeStep(new NablarchListenerContext(jobContext, stepContext));

        new Verifications() {{
            mockDbConnectionManagementHandler.before();
            times = 1;
        }};
    }

    /**
     * {@link DbConnectionManagementListener#afterStep(NablarchListenerContext)}のテスト。
     *
     * {@link DbConnectionManagementHandler#after()}が呼び出されることを検証する。
     */
    @Test
    public void testAfter() throws Exception {
        sut.afterStep(new NablarchListenerContext(jobContext, stepContext));

        new Verifications() {{
            mockDbConnectionManagementHandler.after();
            times = 1;
        }};
    }
}

