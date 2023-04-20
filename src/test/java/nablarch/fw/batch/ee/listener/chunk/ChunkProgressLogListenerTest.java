package nablarch.fw.batch.ee.listener.chunk;

import jakarta.batch.runtime.Metric;
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
 * {@link ChunkProgressLogListener}のテスト。
 */
public class ChunkProgressLogListenerTest {

    /** テスト対象 */
    private ChunkProgressLogListener sut = new ChunkProgressLogListener();

    JobContext mockJobContext = mock(JobContext.class);

    StepContext mockStepContext = mock(StepContext.class);

    @BeforeClass
    public static void setUpClass() throws Exception {
        LogInitializer.initialize();
    }

    @Before
    public void setUp() throws Exception {
        InMemoryAppender.clear();
    }

    /**
     * afterWrite()メソッドで進捗のログが出力されること。
     */
    @Test
    public void testAfterWrite() {
        when(mockStepContext.getMetrics()).thenReturn(new Metric[0]);
        
        sut.afterWrite(new NablarchListenerContext(mockJobContext, mockStepContext), null);

        assertThat(InMemoryAppender.getLogMessages("PROGRESS"), contains(startsWith("INFO progress chunk progress. write count=[0]")));
    }

}
