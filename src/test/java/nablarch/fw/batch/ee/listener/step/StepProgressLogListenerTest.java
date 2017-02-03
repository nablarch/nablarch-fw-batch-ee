package nablarch.fw.batch.ee.listener.step;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertThat;

import javax.batch.runtime.BatchStatus;
import javax.batch.runtime.context.JobContext;
import javax.batch.runtime.context.StepContext;

import mockit.Expectations;
import mockit.Mocked;
import nablarch.fw.batch.ee.initializer.LogInitializer;
import nablarch.fw.batch.ee.integration.InMemoryAppender;
import nablarch.fw.batch.ee.listener.NablarchListenerContext;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * {@link StepProgressLogListener}のテスト。
 */
public class StepProgressLogListenerTest {

    /** テスト対象 */
    StepProgressLogListener sut = new StepProgressLogListener();

    @Mocked
    StepContext mockStepContext;

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
     * ステップ開始時のログが出力されること。
     */
    @Test
    public void testBeforeStep() {
        new Expectations() {
            {
                mockStepContext.getStepName();
                result = "step1";
            }
        };
        sut.beforeStep(new NablarchListenerContext(mockJobContext, mockStepContext));

        assertThat(InMemoryAppender.getLogMessages("PROGRESS"), contains(startsWith("INFO PROGRESS start step. step name=[step1]")));
    }

    /**
     * ステップ終了時のログが出力されること。
     */
    @Test
    public void testAfterStep() {
        new Expectations() {
            {
                mockStepContext.getStepName();
                result = "step1";

                mockStepContext.getBatchStatus();
                result = BatchStatus.COMPLETED;
            }
        };
        sut.afterStep(new NablarchListenerContext(mockJobContext, mockStepContext));

        assertThat(InMemoryAppender.getLogMessages("PROGRESS"), contains(startsWith("INFO PROGRESS finish step. step name=[step1], step status=[SUCCEEDED]")));
    }
}
