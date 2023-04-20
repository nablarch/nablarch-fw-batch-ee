package nablarch.fw.batch.ee.listener.step;

import jakarta.batch.runtime.context.JobContext;
import jakarta.batch.runtime.context.StepContext;
import nablarch.common.handler.DbConnectionManagementHandler;
import nablarch.core.db.connection.DbConnectionContext;
import nablarch.core.transaction.TransactionContext;
import nablarch.fw.batch.ee.listener.NablarchListenerContext;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * {@link DbConnectionManagementListener}のテストクラス。
 *
 * @author Hisaaki Shioiri
 */
public class DbConnectionManagementListenerTest {

    /** テスト対象 */
    private DbConnectionManagementListener sut = new DbConnectionManagementListener();

    // ---------------------------------------- Mock Objects
    DbConnectionManagementHandler mockDbConnectionManagementHandler = mock(DbConnectionManagementHandler.class);

    JobContext jobContext = mock(JobContext.class);

    StepContext stepContext = mock(StepContext.class);

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

        verify(mockDbConnectionManagementHandler).before();
    }

    /**
     * {@link DbConnectionManagementListener#afterStep(NablarchListenerContext)}のテスト。
     *
     * {@link DbConnectionManagementHandler#after()}が呼び出されることを検証する。
     */
    @Test
    public void testAfter() throws Exception {
        sut.afterStep(new NablarchListenerContext(jobContext, stepContext));

        verify(mockDbConnectionManagementHandler).after();
    }
}

