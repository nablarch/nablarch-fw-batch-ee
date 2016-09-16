package nablarch.fw.batch.ee.listener.step;

import javax.batch.runtime.BatchStatus;
import javax.batch.runtime.context.JobContext;
import javax.batch.runtime.context.StepContext;

import mockit.Expectations;
import mockit.Mocked;
import nablarch.fw.batch.ee.initializer.LogInitializer;
import nablarch.fw.batch.ee.listener.NablarchListenerContext;
import nablarch.test.support.log.app.OnMemoryLogWriter;

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

    @BeforeClass
    public static void setUp() throws Exception {
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

        OnMemoryLogWriter.assertLogContains("writer.appLog", "INFO PROGRESS start step. step name=[step1]");
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

        OnMemoryLogWriter.assertLogContains("writer.appLog",
                "INFO PROGRESS finish step. step name=[step1], step status=[SUCCEEDED]");
    }
}
