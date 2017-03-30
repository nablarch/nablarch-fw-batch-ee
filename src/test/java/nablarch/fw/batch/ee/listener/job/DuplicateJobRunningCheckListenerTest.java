package nablarch.fw.batch.ee.listener.job;

import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import javax.batch.operations.BatchRuntimeException;
import javax.batch.runtime.context.JobContext;

import nablarch.fw.batch.ee.listener.NablarchListenerContext;
import nablarch.fw.handler.AlreadyProcessRunningException;
import nablarch.fw.handler.DuplicateProcessChecker;

import org.junit.Before;
import org.junit.Test;

import mockit.Expectations;
import mockit.Mocked;
import mockit.Verifications;

/**
 * {@link DuplicateJobRunningCheckListener}のテスト。
 */
public class DuplicateJobRunningCheckListenerTest {

    /** テスト対象 */
    private DuplicateJobRunningCheckListener sut = new DuplicateJobRunningCheckListener();

    @Mocked
    JobContext mockJobContext;

    @Mocked
    DuplicateProcessChecker mockDuplicateProcessChecker;

    @Before
    public void setUp() throws Exception {
        sut.setDuplicateProcessChecker(mockDuplicateProcessChecker);
    }

    /**
     * 多重起動ではない場合、正常に処理が終わること
     */
    @Test
    public void testBeforeAndAfter() throws Exception {
        new Expectations() {{
            mockJobContext.getJobName();
            result = "jobName";
        }};

        sut.beforeJob(new NablarchListenerContext(mockJobContext, null));
        sut.afterJob(new NablarchListenerContext(mockJobContext, null));

        // プロセスアクティブ化（チェック処理含む）が呼び出されること
        new Verifications() {{
            mockDuplicateProcessChecker.checkAndActive("jobName");
            mockDuplicateProcessChecker.inactive("jobName");
        }};
    }

    /**
     * 多重起動の場合、{@link nablarch.fw.handler.AlreadyProcessRunningException}が送出されること。
     */
    @Test
    public void testAlreadyRunning() throws Exception {
        new Expectations() {{
            mockJobContext.getJobName();
            result = "jobName";

            mockDuplicateProcessChecker.checkAndActive("jobName");
            result = new AlreadyProcessRunningException("already running.");

            mockJobContext.setExitStatus("JobAlreadyRunning");

            mockJobContext.getExitStatus();
            result = "JobAlreadyRunning";
        }};

        try {
            sut.beforeJob(new NablarchListenerContext(mockJobContext, null));
            fail("");
        } catch (BatchRuntimeException e) {
            assertThat(e.getMessage(), containsString("already running."));
        }

        sut.afterJob(new NablarchListenerContext(mockJobContext, null));

        new Verifications() {{
            // 多重起動なので、非活性化処理は呼び出されないこと
            mockDuplicateProcessChecker.inactive("jobName");
            times = 0;
        }};
    }
}

