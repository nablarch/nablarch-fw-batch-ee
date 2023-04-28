package nablarch.fw.batch.ee.listener.step;

import jakarta.batch.runtime.context.JobContext;
import jakarta.batch.runtime.context.StepContext;
import nablarch.fw.batch.ee.initializer.LogInitializer;
import nablarch.fw.batch.ee.integration.InMemoryAppender;
import nablarch.fw.batch.ee.listener.NablarchListenerContext;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * {@link StepProgressLogListener}のテスト。
 */
public class StepProgressLogListenerTest {

    /** テスト対象 */
    StepProgressLogListener sut = new StepProgressLogListener();

    StepContext mockStepContext = mock(StepContext.class);

    JobContext mockJobContext = mock(JobContext.class);

    @Before
    public void setUp() throws Exception {
        InMemoryAppender.clear();
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
        LogInitializer.initialize();
    }

    /**
     * ステップ開始時のログが出力されること。
     */
    @Test
    public void testBeforeStep() {
        when(mockJobContext.getJobName()).thenReturn("job1");
        when(mockStepContext.getStepName()).thenReturn("step1");
        
        sut.beforeStep(new NablarchListenerContext(mockJobContext, mockStepContext));

        assertThat(InMemoryAppender.getLogMessages("PROGRESS"), contains(startsWith("INFO progress start step. job name: [job1] step name: [step1]")));
    }

    /**
     * ステップ終了時のログが出力されること。
     */
    @Test
    public void testAfterStep() {
        when(mockJobContext.getJobName()).thenReturn("job1");
        when(mockStepContext.getStepName()).thenReturn("step1");
        when(mockStepContext.getExitStatus()).thenReturn("SUCCESS");
        
        sut.afterStep(new NablarchListenerContext(mockJobContext, mockStepContext));

        assertThat(InMemoryAppender.getLogMessages("PROGRESS"), contains(startsWith("INFO progress finish step. job name: [job1] step name: [step1] step status: [SUCCESS]")));
    }
}
