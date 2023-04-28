package nablarch.fw.batch.ee;

import jakarta.batch.operations.JobOperator;
import jakarta.batch.runtime.BatchRuntime;
import jakarta.batch.runtime.BatchStatus;
import jakarta.batch.runtime.JobExecution;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.MockedStatic;

import java.lang.reflect.InvocationTargetException;
import java.util.Properties;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

/**
 * {@link JobExecutor}のテスト
 *
 * @author T.Shimoda
 */
public class JobExecutorTest {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    private final JobOperator jobOperator = mock(JobOperator.class, RETURNS_DEEP_STUBS);
    
    /**
     * ジョブの終了待ちの間に中断される場合のテスト
     */
    @Test
    public void testInterrupted() {
        final JobExecutor executor = new JobExecutor("job-interrupted", new Properties());

        try (final MockedStatic<BatchRuntime> mocked = mockStatic(BatchRuntime.class);) {
            mocked.when(BatchRuntime::getJobOperator).thenReturn(jobOperator);
            when(jobOperator.start(eq(executor.getJobXmlName()), any(Properties.class))).thenReturn(1L);
            when(jobOperator.getJobExecution(1L).getBatchStatus()).then((context) -> {
                throw new InterruptedException("test");
            });

            int exitCode = executor.execute();
            assertThat("終了コードが1か", exitCode, is(1));
        }
    }

    /**
     * ジョブがFAILEDで終わる場合
     */
    @Test
    public void testFailedEnd() {
        final JobExecutor executor = new JobExecutor("job-failed", new Properties());

        try (final MockedStatic<BatchRuntime> mocked = mockStatic(BatchRuntime.class)) {
            mocked.when(BatchRuntime::getJobOperator).thenReturn(jobOperator);
            when(jobOperator.start(eq(executor.getJobXmlName()), any(Properties.class))).thenReturn(1L);
            when(jobOperator.getJobExecution(1L).getBatchStatus()).thenReturn(BatchStatus.FAILED);

            int exitCode = executor.execute();
            assertThat("終了コードが1か", exitCode, is(1));
        }
    }

    /**
     * ジョブがSTOPPEDで終わる場合
     */
    @Test
    public void testStoppedEnd() {
        final JobExecutor executor = new JobExecutor("job-stopped", new Properties());

        try (final MockedStatic<BatchRuntime> mocked = mockStatic(BatchRuntime.class)) {
            mocked.when(BatchRuntime::getJobOperator).thenReturn(jobOperator);
            when(jobOperator.start(eq(executor.getJobXmlName()), any(Properties.class))).thenReturn(1L);
            when(jobOperator.getJobExecution(1L).getBatchStatus()).thenReturn(BatchStatus.STOPPED);

            int exitCode = executor.execute();
            assertThat("終了コードが1か", exitCode, is(1));
        }
    }

    /**
     * ジョブがABANDONEDで終わる場合
     */
    @Test
    public void testAbandonedEnd() {
        final Properties properties = new Properties();
        properties.put("key1", "key2");
        final JobExecutor executor = new JobExecutor("job-abandoned", properties);

        try (final MockedStatic<BatchRuntime> mocked = mockStatic(BatchRuntime.class)) {
            mocked.when(BatchRuntime::getJobOperator).thenReturn(jobOperator);
            when(jobOperator.start(eq(executor.getJobXmlName()), any(Properties.class))).thenReturn(1L);
            when(jobOperator.getJobExecution(1L).getBatchStatus()).thenReturn(BatchStatus.ABANDONED);

            int exitCode = executor.execute();
            assertThat("終了コードが1か", exitCode, is(1));
        }
    }

    /**
     * ジョブがCOMPLETEDで終わる場合
     */
    @Test
    public void testCompletedEnd() {
        final JobExecutor executor = new JobExecutor("job-completed", new Properties());

        try (final MockedStatic<BatchRuntime> mocked = mockStatic(BatchRuntime.class)) {
            mocked.when(BatchRuntime::getJobOperator).thenReturn(jobOperator);
            when(jobOperator.start(eq(executor.getJobXmlName()), any(Properties.class))).thenReturn(1L);
            when(jobOperator.getJobExecution(1L).getBatchStatus()).thenReturn(BatchStatus.COMPLETED);
            
            int exitCode = executor.execute();
            assertThat("終了コードが0か", exitCode, is(0));
        }
    }

    /**
     * ジョブの実行中の間のステータスをすべて返すテスト
     */
    @Test
    public void testIntermediateStatuses() {
        final JobExecutor executor = new JobExecutor("job-intermediate-statues", new Properties());

        try (final MockedStatic<BatchRuntime> mocked = mockStatic(BatchRuntime.class)) {
            mocked.when(BatchRuntime::getJobOperator).thenReturn(jobOperator);
            when(jobOperator.start(eq(executor.getJobXmlName()), any(Properties.class))).thenReturn(1L);
            when(jobOperator.getJobExecution(1L).getBatchStatus())
                .thenReturn(BatchStatus.STARTING, BatchStatus.STARTED, BatchStatus.STOPPING, BatchStatus.STOPPED);

            int exitCode = executor.execute();
            assertThat("終了コードが1か", exitCode, is(1));
        }
    }

    /**
     * 警告終了する場合のテスト
     */
    @Test
    public void testWarning() {
        final JobExecutor executor = new JobExecutor("job-warning", new Properties());

        try (final MockedStatic<BatchRuntime> mocked = mockStatic(BatchRuntime.class)) {
            mocked.when(BatchRuntime::getJobOperator).thenReturn(jobOperator);
            when(jobOperator.start(eq(executor.getJobXmlName()), any(Properties.class))).thenReturn(1L);

            final JobExecution jobExecution = jobOperator.getJobExecution(1L);
            when(jobExecution.getBatchStatus()).thenReturn(BatchStatus.COMPLETED);
            when(jobExecution.getExitStatus()).thenReturn("WARNING");
            
            int exitCode = executor.execute();
            assertThat("終了コードが2か", exitCode, is(2));
        }
    }

    /**
     * getJobExecutionのテスト
     */
    @Test
    public void testJobExecution() {
        final JobExecutor executor = new JobExecutor("job-warning", new Properties());

        try (final MockedStatic<BatchRuntime> mocked = mockStatic(BatchRuntime.class)) {
            mocked.when(BatchRuntime::getJobOperator).thenReturn(jobOperator);
            when(jobOperator.start(eq(executor.getJobXmlName()), any(Properties.class))).thenReturn(3L);
            when(jobOperator.getJobExecution(3L).getBatchStatus()).thenReturn(BatchStatus.COMPLETED);

            assertNull("実行前はnull", executor.getJobExecution());
            int exitCode = executor.execute();
            assertThat("終了コードが0か", exitCode, is(0));
            assertNotNull("実行後はNotNull", executor.getJobExecution());
            assertNotNull("実行後のEndTimeはNotNull", executor.getJobExecution()
                    .getEndTime());
        }
    }

    /**
     * ジョブを2回以上実行した際のエラーを返すテスト
     */
    @Test
    public void testExecuteTwice() {
        expectedException.expect(IllegalStateException.class);
        expectedException.expectMessage(is("Job is already started. JobXmlName=[job-twice]"));
        final Properties properties = new Properties();
        final JobExecutor executor = new JobExecutor("job-twice", properties);

        try (final MockedStatic<BatchRuntime> mocked = mockStatic(BatchRuntime.class)) {
            mocked.when(BatchRuntime::getJobOperator).thenReturn(jobOperator);
            when(jobOperator.start(eq(executor.getJobXmlName()), any(Properties.class))).thenReturn(1L);
            when(jobOperator.getJobExecution(1L).getBatchStatus()).thenReturn(BatchStatus.COMPLETED);

            int exitCode = executor.execute();
            assertThat("1度目は終了する", exitCode, is(0));
            executor.execute();
        }
    }

    /**
     * 待機時間に1未満を指定したテスト
     */
    @Test
    public void testExecuteLessThanOne() {
        expectedException.expect(IllegalArgumentException.class);
        expectedException.expectMessage(is("mills must be greater than 0."));
        final JobExecutor executor = new JobExecutor("job-wait-time=0", new Properties());
        int exitCode = executor.execute(0);
    }

    /**
     * 待機時間にプラスを指定したテスト
     */
    @Test
    public void testExecutePlusMills() {
        final JobExecutor executor = new JobExecutor("job-wait-time=500", new Properties());

        try (final MockedStatic<BatchRuntime> mocked = mockStatic(BatchRuntime.class)) {
            mocked.when(BatchRuntime::getJobOperator).thenReturn(jobOperator);
            when(jobOperator.start(eq(executor.getJobXmlName()), any(Properties.class))).thenReturn(1L);
            when(jobOperator.getJobExecution(1L).getBatchStatus())
                    .thenReturn(BatchStatus.STARTING, BatchStatus.STARTED, BatchStatus.COMPLETED);

            int exitCode = executor.execute(500);
            assertThat("終了コードが0か", exitCode, is(0));
        }
    }
}
