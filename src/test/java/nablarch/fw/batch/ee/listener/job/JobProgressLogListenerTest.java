package nablarch.fw.batch.ee.listener.job;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertThat;

import javax.batch.runtime.context.JobContext;

import mockit.Expectations;
import mockit.Mocked;
import nablarch.fw.batch.ee.initializer.LogInitializer;
import nablarch.fw.batch.ee.integration.InMemoryAppender;
import nablarch.fw.batch.ee.listener.NablarchListenerContext;
import org.junit.Before;
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

    @Before
    public void setUp() throws Exception {
        InMemoryAppender.clear();
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
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

        assertThat(InMemoryAppender.getLogMessages("PROGRESS"), contains(startsWith("INFO progress start job. job name: [jobName]")));

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
            }
        };

        sut.afterJob(new NablarchListenerContext(mockJobContext, null));

        assertThat(InMemoryAppender.getLogMessages("PROGRESS"), contains(startsWith("INFO progress finish job. job name: [jobName]")));
    }
}
