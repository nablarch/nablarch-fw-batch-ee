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
        expectedException.expectMessage(is("Please specify JOB XML name as the first argument."));
        Main.main("arg1","arg2");
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
        new Expectations(){{
            final JobOperator jobOperator = BatchRuntime.getJobOperator();
            jobOperator.start("main-test-Job1", new Properties());
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
        new Expectations(){{
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
        new Expectations(){{
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
     * 警告ステータスだが異常終了している場合のテスト<br/>
     * （ExitStatusよりBatchStatusが優先されることの確認）
     */
    @Test
    public void testMainWarningButNotCompletedError() {
        new Expectations(){{
            final JobOperator jobOperator = BatchRuntime.getJobOperator();
            final long executionId = jobOperator.start("main-test-Job4", new Properties());
            result = 4L;
            final JobExecution jobExecution = jobOperator.getJobExecution(4L);
            jobExecution.getBatchStatus();
            result = BatchStatus.STOPPED;
            jobExecution.getExitStatus();
            result = "WARNING";
        }};
        exit.expectSystemExitWithStatus(1);
        Main.main("main-test-Job4");
    }

    /**
     * ジョブの終了待ちの間に中断される場合のテスト
     */
    @Test
    public void testMainInterrupted() {
        new Expectations(){{
            final JobOperator jobOperator = BatchRuntime.getJobOperator();
            jobOperator.start("main-test-Job5", new Properties());
            result = 5L;
            final JobExecution jobExecution = jobOperator.getJobExecution(6L);
            jobExecution.getBatchStatus();
            result = new InterruptedException("Interrupted.");
        }};
        exit.expectSystemExitWithStatus(1);
        Main.main("main-test-Job5");
    }

    /**
     * ジョブがFAILEDで終わる場合
     */
    @Test
    public void testMainFailedEnd() {
        new Expectations(){{
            final JobOperator jobOperator = BatchRuntime.getJobOperator();
            jobOperator.start("main-test-Job6-1", new Properties());
            result = 6L;
            final JobExecution jobExecution = jobOperator.getJobExecution(6L);
            jobExecution.getBatchStatus();
            result = BatchStatus.FAILED;
        }};
        exit.expectSystemExitWithStatus(1);
        Main.main("main-test-Job6-1");
    }

    /**
     * ジョブがSTOPPEDで終わる場合
     */
    @Test
    public void testMainStoppedEnd() {
        new Expectations(){{
            final JobOperator jobOperator = BatchRuntime.getJobOperator();
            jobOperator.start("main-test-Job6-2", new Properties());
            result = 6L;
            final JobExecution jobExecution = jobOperator.getJobExecution(6L);
            jobExecution.getBatchStatus();
            result = BatchStatus.STOPPED;
        }};
        exit.expectSystemExitWithStatus(1);
        Main.main("main-test-Job6-2");
    }

    /**
     * ジョブがABANDONEDで終わる場合
     */
    @Test
    public void testMainAbandonedEnd() {
        new Expectations(){{
            final JobOperator jobOperator = BatchRuntime.getJobOperator();
            jobOperator.start("main-test-Job6-3", new Properties());
            result = 6L;
            final JobExecution jobExecution = jobOperator.getJobExecution(6L);
            jobExecution.getBatchStatus();
            result = BatchStatus.ABANDONED;
        }};
        exit.expectSystemExitWithStatus(1);
        Main.main("main-test-Job6-3");
    }

    /**
     * ジョブがCOMPLETEDで終わる場合
     */
    @Test
    public void testMainCompletedEnd() {
        new Expectations(){{
            final JobOperator jobOperator = BatchRuntime.getJobOperator();
            jobOperator.start("main-test-Job6-4", new Properties());
            result = 6L;
            final JobExecution jobExecution = jobOperator.getJobExecution(6L);
            jobExecution.getBatchStatus();
            result = BatchStatus.COMPLETED;
        }};
        exit.expectSystemExitWithStatus(0);
        Main.main("main-test-Job6-4");
    }

    /**
     * ジョブの終了以外のステータスを返す場合
     */
    @Test
    public void testMainCoverAllBatchStatusAndStopped() {
        new Expectations(){{
            final JobOperator jobOperator = BatchRuntime.getJobOperator();
            jobOperator.start("main-test-Job6-5", new Properties());
            result = 6L;
            final JobExecution jobExecution = jobOperator.getJobExecution(6L);
            jobExecution.getBatchStatus();
            returns(BatchStatus.STARTING, BatchStatus.STARTED, BatchStatus.STOPPING, BatchStatus.STOPPED);
        }};
        exit.expectSystemExitWithStatus(1);
        Main.main("main-test-Job6-5");
    }

}
