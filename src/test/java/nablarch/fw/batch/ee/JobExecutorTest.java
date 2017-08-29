package nablarch.fw.batch.ee;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.Properties;

import javax.batch.operations.JobOperator;
import javax.batch.runtime.BatchRuntime;
import javax.batch.runtime.BatchStatus;
import javax.batch.runtime.JobExecution;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import mockit.Expectations;
import mockit.Mocked;

/**
 * {@link JobExecutor}のテスト
 *
 * @author T.Shimoda
 */
public class JobExecutorTest {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Mocked
    BatchRuntime runtime;

    /**
     * ジョブの終了待ちの間に中断される場合のテスト
     */
    @Test
    public void testInterrupted() {
        final JobExecutor executor = new JobExecutor("job-interrupted", new Properties());
        new Expectations() {{
            final JobOperator jobOperator = BatchRuntime.getJobOperator();
            jobOperator.start(executor.getJobXmlName(), new Properties());
            result = 1L;
            final JobExecution jobExecution = jobOperator.getJobExecution(1L);
            jobExecution.getBatchStatus();
            result = new InterruptedException("Interrupted.");
        }};
        int exitCode = executor.execute();
        assertThat("終了コードが1か", exitCode, is(1));
    }

    /**
     * ジョブがFAILEDで終わる場合
     */
    @Test
    public void testFailedEnd() {
        final JobExecutor executor = new JobExecutor("job-failed", new Properties());
        new Expectations() {{
            final JobOperator jobOperator = BatchRuntime.getJobOperator();
            jobOperator.start(executor.getJobXmlName(), new Properties());
            result = 1L;
            final JobExecution jobExecution = jobOperator.getJobExecution(1L);
            jobExecution.getBatchStatus();
            result = BatchStatus.FAILED;
        }};
        int exitCode = executor.execute();
        assertThat("終了コードが1か", exitCode, is(1));
    }

    /**
     * ジョブがSTOPPEDで終わる場合
     */
    @Test
    public void testStoppedEnd() {
        final JobExecutor executor = new JobExecutor("job-stopped", new Properties());
        new Expectations() {{
            final JobOperator jobOperator = BatchRuntime.getJobOperator();
            jobOperator.start(executor.getJobXmlName(), new Properties());
            result = 1L;
            final JobExecution jobExecution = jobOperator.getJobExecution(1L);
            jobExecution.getBatchStatus();
            result = BatchStatus.STOPPED;
        }};
        int exitCode = executor.execute();
        assertThat("終了コードが1か", exitCode, is(1));
    }

    /**
     * ジョブがABANDONEDで終わる場合
     */
    @Test
    public void testAbandonedEnd() {
        final Properties properties = new Properties();
        properties.put("key1", "key2");
        final JobExecutor executor = new JobExecutor("job-abandoned", properties);
        new Expectations() {{
            final JobOperator jobOperator = BatchRuntime.getJobOperator();
            jobOperator.start(executor.getJobXmlName(), properties);
            result = 1L;
            final JobExecution jobExecution = jobOperator.getJobExecution(1L);
            jobExecution.getBatchStatus();
            result = BatchStatus.ABANDONED;
        }};
        int exitCode = executor.execute();
        assertThat("終了コードが1か", exitCode, is(1));
    }

    /**
     * ジョブがCOMPLETEDで終わる場合
     */
    @Test
    public void testCompletedEnd() {
        final JobExecutor executor = new JobExecutor("job-completed", new Properties());
        new Expectations() {{
            final JobOperator jobOperator = BatchRuntime.getJobOperator();
            jobOperator.start(executor.getJobXmlName(), new Properties());
            result = 1L;
            final JobExecution jobExecution = jobOperator.getJobExecution(1L);
            jobExecution.getBatchStatus();
            result = BatchStatus.COMPLETED;
        }};
        int exitCode = executor.execute();
        assertThat("終了コードが0か", exitCode, is(0));
    }

    /**
     * ジョブの実行中の間のステータスをすべて返すテスト
     */
    @Test
    public void testIntermediateStatuses() {
        final JobExecutor executor = new JobExecutor("job-intermediate-statues", new Properties());
        new Expectations() {{
            final JobOperator jobOperator = BatchRuntime.getJobOperator();
            jobOperator.start(executor.getJobXmlName(), new Properties());
            result = 1L;
            final JobExecution jobExecution = jobOperator.getJobExecution(1L);
            jobExecution.getBatchStatus();
            returns(BatchStatus.STARTING, BatchStatus.STARTED, BatchStatus.STOPPING, BatchStatus.STOPPED);
        }};
        int exitCode = executor.execute();
        assertThat("終了コードが1か", exitCode, is(1));
    }

    /**
     * 警告終了する場合のテスト
     */
    @Test
    public void testWarning() {
        final JobExecutor executor = new JobExecutor("job-warning", new Properties());
        new Expectations() {{
            final JobOperator jobOperator = BatchRuntime.getJobOperator();
            jobOperator.start(executor.getJobXmlName(), new Properties());
            result = 1L;
            final JobExecution jobExecution = jobOperator.getJobExecution(1L);
            jobExecution.getBatchStatus();
            result = BatchStatus.COMPLETED;
            jobExecution.getExitStatus();
            result = "WARNING";
        }};
        int exitCode = executor.execute();
        assertThat("終了コードが2か", exitCode, is(2));
    }

    /**
     * getJobExecutionのテスト
     */
    @Test
    public void testJobExecution() {
        final JobExecutor executor = new JobExecutor("job-warning", new Properties());
        new Expectations() {{
            final JobOperator jobOperator = BatchRuntime.getJobOperator();
            jobOperator.start(executor.getJobXmlName(), new Properties());
            result = 3L;
            final JobExecution jobExecution = jobOperator.getJobExecution(3L);
            jobExecution.getBatchStatus();
            result = BatchStatus.COMPLETED;
        }};
        assertNull("実行前はnull", executor.getJobExecution());
        int exitCode = executor.execute();
        assertThat("終了コードが0か", exitCode, is(0));
        assertNotNull("実行後はNotNull", executor.getJobExecution());
        assertNotNull("実行後のEndTimeはNotNull", executor.getJobExecution()
                                                     .getEndTime());
    }

    /**
     * ジョブを2回以上実行した際のエラーを返すテスト
     */
    @Test
    public void testExecuteTwice() {
        expectedException.expect(IllegalStateException.class);
        expectedException.expectMessage(is("Job is already started. JobXmlName=[job-twice]"));
        final JobExecutor executor = new JobExecutor("job-twice", new Properties());
        new Expectations() {{
            final JobOperator jobOperator = BatchRuntime.getJobOperator();
            jobOperator.start(executor.getJobXmlName(), null);
            result = 1L;
            final JobExecution jobExecution = jobOperator.getJobExecution(1L);
            jobExecution.getBatchStatus();
            result = BatchStatus.COMPLETED;
        }};
        int exitCode = executor.execute();
        assertThat("1度目は終了する", exitCode, is(0));
        exitCode = executor.execute();
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
        new Expectations() {{
            final JobOperator jobOperator = BatchRuntime.getJobOperator();
            jobOperator.start(executor.getJobXmlName(), new Properties());
            result = 1L;
            final JobExecution jobExecution = jobOperator.getJobExecution(1L);
            jobExecution.getBatchStatus();
            returns(BatchStatus.STARTING, BatchStatus.STARTED, BatchStatus.COMPLETED);
        }};
        int exitCode = executor.execute(500);
        assertThat("終了コードが0か", exitCode, is(0));
    }
}
