package nablarch.fw.batch.ee;

import static org.hamcrest.CoreMatchers.*;

import java.util.Properties;

import javax.batch.operations.JobOperator;
import javax.batch.runtime.BatchRuntime;
import javax.batch.runtime.BatchStatus;
import javax.batch.runtime.JobExecution;

import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.ExpectedSystemExit;
import org.junit.rules.ExpectedException;

import mockit.Expectations;
import mockit.Mocked;

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

    @Mocked
    BatchRuntime runtime;

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
        new Expectations() {{
            final JobOperator jobOperator = BatchRuntime.getJobOperator();
            jobOperator.start("main-test-Job1", null);
            result = 1L;
            final JobExecution jobExecution = jobOperator.getJobExecution(1L);
            jobExecution.getBatchStatus();
            result = BatchStatus.COMPLETED;
        }};
        exit.expectSystemExitWithStatus(0);
        Main.main("main-test-Job1");
    }

    /**
     * 異常終了する場合のテスト
     */
    @Test
    public void testMainError() {
        new Expectations() {{
            final JobOperator jobOperator = BatchRuntime.getJobOperator();
            jobOperator.start("main-test-Job2", new Properties());
            result = 2L;
            final JobExecution jobExecution = jobOperator.getJobExecution(2L);
            jobExecution.getBatchStatus();
            result = BatchStatus.FAILED;
        }};
        exit.expectSystemExitWithStatus(1);
        Main.main("main-test-Job2");
    }

    /**
     * 警告終了する場合のテスト
     */
    @Test
    public void testMainWarning() {
        new Expectations() {{
            final JobOperator jobOperator = BatchRuntime.getJobOperator();
            jobOperator.start("main-test-Job3", new Properties());
            result = 3L;
            final JobExecution jobExecution = jobOperator.getJobExecution(3L);
            jobExecution.getBatchStatus();
            result = BatchStatus.COMPLETED;
            jobExecution.getExitStatus();
            result = "WARNING";
        }};
        exit.expectSystemExitWithStatus(2);
        Main.main("main-test-Job3");
    }

    /**
     * FAILEDだが警告終了している場合のテスト<br/>
     * （BatchStatusよりExitStatusのWARNINGが優先されることの確認）
     */
    @Test
    public void testMainFailedButWarning() {
        new Expectations() {{
            final JobOperator jobOperator = BatchRuntime.getJobOperator();
            final long executionId = jobOperator.start("main-test-Job4", new Properties());
            result = 4L;
            final JobExecution jobExecution = jobOperator.getJobExecution(4L);
            jobExecution.getBatchStatus();
            result = BatchStatus.FAILED;
            jobExecution.getExitStatus();
            result = "WARNING";
        }};
        exit.expectSystemExitWithStatus(2);
        Main.main("main-test-Job4");
    }
}
