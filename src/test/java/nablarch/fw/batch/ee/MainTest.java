package nablarch.fw.batch.ee;

import jakarta.batch.operations.JobOperator;
import jakarta.batch.runtime.BatchRuntime;
import jakarta.batch.runtime.BatchStatus;
import jakarta.batch.runtime.JobExecution;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.ExpectedSystemExit;
import org.junit.rules.ExpectedException;
import org.mockito.MockedStatic;

import java.util.Properties;

import static org.hamcrest.CoreMatchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

/**
 * バッチアプリケーションのメインクラスのテスト。
 *
 * @author T.Shimoda
 */
public class MainTest {

    @Rule
    public final ExpectedSystemExit exit = ExpectedSystemExit.none();

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

            exit.expectSystemExitWithStatus(0);
            Main.main("main-test-Job1");
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

            exit.expectSystemExitWithStatus(1);
            Main.main("main-test-Job2");
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

            exit.expectSystemExitWithStatus(2);
            Main.main("main-test-Job3");
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
            
            exit.expectSystemExitWithStatus(2);
            Main.main("main-test-Job4");
        }
    }
}
