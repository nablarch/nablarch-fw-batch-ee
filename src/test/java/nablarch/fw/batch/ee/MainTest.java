package nablarch.fw.batch.ee;

import jakarta.batch.operations.JobOperator;
import jakarta.batch.runtime.BatchRuntime;
import jakarta.batch.runtime.BatchStatus;
import jakarta.batch.runtime.JobExecution;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.MockedStatic;

import java.util.Properties;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

/**
 * バッチアプリケーションのメインクラスのテスト。
 * <p/>
 * {@link Main#main(String...)}は終了コードを{@link System#exit(int)}で返すため、
 * 終了コードの検証はmainメソッドが委譲している{@link JobExecutor#execute()}の戻り値に対して行う。
 * (SecurityManagerはJava 18以降で非推奨となり、Java 24以降では有効化できないため、
 * {@link System#exit(int)}を捕捉する方式のテストは行わない。)
 *
 * @author T.Shimoda
 */
public class MainTest {

    @Rule
    public final ExpectedException expectedException = ExpectedException.none();

    private final JobOperator jobOperator = mock(JobOperator.class, RETURNS_DEEP_STUBS);
    
    /**
     * 引数の指定がない場合のテスト
     */
    @Test
    public void testMainNoArgument() {
        expectedException.expect(IllegalArgumentException.class);
        expectedException.expectMessage(is("Please specify JOB XML name as the first argument."));
        Main.main();
    }


    /**
     * 引数が複数指定された場合のテスト
     */
    @Test
    public void testMainNotOneArgument() {
        expectedException.expect(IllegalArgumentException.class);
        expectedException.expectMessage(is("command line args is unsupported. specify only the command line option.(example: --name1 value1 --name2 value2)"));
        Main.main("arg1", "arg2");
    }

    /**
     * 空の引数が指定された場合のテスト
     */
    @Test
    public void testMainEmptyArgument() {
        expectedException.expect(IllegalArgumentException.class);
        expectedException.expectMessage(is("Please specify JOB XML name as the first argument."));
        Main.main("");
    }

    /**
     * 正常終了する場合のテスト
     */
    @Test
    public void testMainSuccess() {
        try (final MockedStatic<BatchRuntime> mocked = mockStatic(BatchRuntime.class)) {
            mocked.when(BatchRuntime::getJobOperator).thenReturn(jobOperator);
            when(jobOperator.start(eq("main-test-Job1"), any(Properties.class))).thenReturn(1L);
            when(jobOperator.getJobExecution(1L).getBatchStatus()).thenReturn(BatchStatus.COMPLETED);

            assertThat(new JobExecutor("main-test-Job1", new Properties()).execute(), is(0));
        }
    }

    /**
     * 異常終了する場合のテスト
     */
    @Test
    public void testMainError() {
        try (final MockedStatic<BatchRuntime> mocked = mockStatic(BatchRuntime.class)) {
            mocked.when(BatchRuntime::getJobOperator).thenReturn(jobOperator);
            when(jobOperator.start(eq("main-test-Job2"), any(Properties.class))).thenReturn(2L);
            when(jobOperator.getJobExecution(2L).getBatchStatus()).thenReturn(BatchStatus.FAILED);

            assertThat(new JobExecutor("main-test-Job2", new Properties()).execute(), is(1));
        }
    }

    /**
     * 警告終了する場合のテスト
     */
    @Test
    public void testMainWarning() {
        try (final MockedStatic<BatchRuntime> mocked = mockStatic(BatchRuntime.class)) {
            mocked.when(BatchRuntime::getJobOperator).thenReturn(jobOperator);
            when(jobOperator.start(eq("main-test-Job3"), any(Properties.class))).thenReturn(3L);

            final JobExecution jobExecution = jobOperator.getJobExecution(3L);
            when(jobExecution.getBatchStatus()).thenReturn(BatchStatus.COMPLETED);
            when(jobExecution.getExitStatus()).thenReturn("WARNING");

            assertThat(new JobExecutor("main-test-Job3", new Properties()).execute(), is(2));
        }
    }

    /**
     * FAILEDだが警告終了している場合のテスト<br/>
     * （BatchStatusよりExitStatusのWARNINGが優先されることの確認）
     */
    @Test
    public void testMainFailedButWarning() {
        try (final MockedStatic<BatchRuntime> mocked = mockStatic(BatchRuntime.class)) {
            mocked.when(BatchRuntime::getJobOperator).thenReturn(jobOperator);
            when(jobOperator.start(eq("main-test-Job4"), any(Properties.class))).thenReturn(4L);

            final JobExecution jobExecution = jobOperator.getJobExecution(4L);
            when(jobExecution.getBatchStatus()).thenReturn(BatchStatus.FAILED);
            when(jobExecution.getExitStatus()).thenReturn("WARNING");
            
            assertThat(new JobExecutor("main-test-Job4", new Properties()).execute(), is(2));
        }
    }
}
