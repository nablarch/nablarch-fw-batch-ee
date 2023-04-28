package nablarch.fw.batch.ee.listener.job;

import jakarta.batch.operations.BatchRuntimeException;
import jakarta.batch.runtime.context.JobContext;
import nablarch.fw.batch.ee.listener.NablarchListenerContext;
import nablarch.fw.handler.AlreadyProcessRunningException;
import nablarch.fw.handler.DuplicateProcessChecker;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link DuplicateJobRunningCheckListener}のテスト。
 */
public class DuplicateJobRunningCheckListenerTest {

    /** テスト対象 */
    private DuplicateJobRunningCheckListener sut = new DuplicateJobRunningCheckListener();

    JobContext mockJobContext = mock(JobContext.class);

    DuplicateProcessChecker mockDuplicateProcessChecker = mock(DuplicateProcessChecker.class);

    @Before
    public void setUp() throws Exception {
        sut.setDuplicateProcessChecker(mockDuplicateProcessChecker);
    }

    /**
     * 多重起動ではない場合、正常に処理が終わること
     */
    @Test
    public void testBeforeAndAfter() throws Exception {
        when(mockJobContext.getJobName()).thenReturn("jobName");

        sut.beforeJob(new NablarchListenerContext(mockJobContext, null));
        sut.afterJob(new NablarchListenerContext(mockJobContext, null));

        // プロセスアクティブ化（チェック処理含む）が呼び出されること
        verify(mockDuplicateProcessChecker, atLeastOnce()).checkAndActive("jobName");
        verify(mockDuplicateProcessChecker, atLeastOnce()).inactive("jobName");
    }

    /**
     * 多重起動の場合、{@link nablarch.fw.handler.AlreadyProcessRunningException}が送出されること。
     */
    @Test
    public void testAlreadyRunning() throws Exception {
        when(mockJobContext.getJobName()).thenReturn("jobName");

        doThrow(new AlreadyProcessRunningException("already running.")).when(mockDuplicateProcessChecker).checkAndActive("jobName");


        when(mockJobContext.getExitStatus()).thenReturn("JobAlreadyRunning");

        try {
            sut.beforeJob(new NablarchListenerContext(mockJobContext, null));
            fail("");
        } catch (BatchRuntimeException e) {
            assertThat(e.getMessage(), containsString("already running."));
        }

        sut.afterJob(new NablarchListenerContext(mockJobContext, null));

        verify(mockJobContext).setExitStatus("JobAlreadyRunning");
        // 多重起動なので、非活性化処理は呼び出されないこと
        verify(mockDuplicateProcessChecker, never()).inactive("jobName");
    }
}

