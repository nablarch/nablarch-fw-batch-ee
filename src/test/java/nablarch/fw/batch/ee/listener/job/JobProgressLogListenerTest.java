package nablarch.fw.batch.ee.listener.job;

import javax.batch.runtime.BatchStatus;
import javax.batch.runtime.context.JobContext;

import mockit.Expectations;
import mockit.Mocked;
import nablarch.fw.batch.ee.initializer.LogInitializer;
import nablarch.fw.batch.ee.listener.NablarchListenerContext;
import nablarch.test.support.log.app.OnMemoryLogWriter;

import org.junit.BeforeClass;
import org.junit.Test;

/**
 * {@link JobProgressLogListener}のテスト。
 */
public class JobProgressLogListenerTest {

    /** テスト対象 */
    private JobProgressLogListener sut = new JobProgressLogListener();

    @Mocked
    JobContext mockJobContext;

    @BeforeClass
    public static void setUp() throws Exception {
        LogInitializer.initialize();
    }

    /**
     * JOB開始前のログが出力されること。
     */
    @Test
    public void testBeforeJob() throws Exception {
        new Expectations() {
            {
                mockJobContext.getJobName();
                result = "jobName";
            }
        };

        sut.beforeJob(new NablarchListenerContext(mockJobContext, null));

        OnMemoryLogWriter.assertLogContains("writer.appLog", "INFO PROGRESS start job. job name=[jobName]");

    }

    /**
     * JOB終了時のログが出力されること。
     */
    @Test
    public void testAfterJob() throws Exception {
        new Expectations() {
            {
                mockJobContext.getJobName();
                result = "jobName";

                mockJobContext.getBatchStatus();
                result = BatchStatus.COMPLETED;
            }
        };

        sut.afterJob(new NablarchListenerContext(mockJobContext, null));

        OnMemoryLogWriter
                .assertLogContains("writer.appLog", "INFO PROGRESS finish job. job name=[jobName], batch status=[COMPLETED]");
    }
}
