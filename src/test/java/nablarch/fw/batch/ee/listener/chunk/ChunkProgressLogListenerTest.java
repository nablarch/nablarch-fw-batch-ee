package nablarch.fw.batch.ee.listener.chunk;

import javax.batch.runtime.context.JobContext;
import javax.batch.runtime.context.StepContext;

import mockit.Mocked;
import nablarch.fw.batch.ee.initializer.LogInitializer;
import nablarch.fw.batch.ee.listener.NablarchListenerContext;
import nablarch.test.support.log.app.OnMemoryLogWriter;

import org.junit.BeforeClass;
import org.junit.Test;

/**
 * {@link ChunkProgressLogListener}のテスト。
 */
public class ChunkProgressLogListenerTest {

    /** テスト対象 */
    private ChunkProgressLogListener sut = new ChunkProgressLogListener();

    @Mocked
    JobContext mockJobContext;

    @Mocked
    StepContext mockStepContext;

    @BeforeClass
    public static void setUp() throws Exception {
        LogInitializer.initialize();
    }

    /**
     * afterWrite()メソッドで進捗のログが出力されること。
     */
    @Test
    public void testAfterWrite() {
        sut.afterWrite(new NablarchListenerContext(mockJobContext, mockStepContext), null);

        OnMemoryLogWriter.assertLogContains("writer.appLog", "INFO PROGRESS chunk progress. write count=[0]");
    }

}
