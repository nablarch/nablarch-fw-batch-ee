package nablarch.fw.batch.ee.integration;


import nablarch.core.repository.di.DiContainer;
import nablarch.core.repository.di.config.xml.XmlComponentDefinitionLoader;
import org.junit.rules.ExternalResource;

import jakarta.batch.operations.JobOperator;
import jakarta.batch.runtime.BatchRuntime;
import jakarta.batch.runtime.BatchStatus;
import jakarta.batch.runtime.JobExecution;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.concurrent.TimeUnit;

/**
 * Java Batchの結合テストをサポートするクラス。
 */
public class IntegrationTestResource extends ExternalResource {
    /**
     * 指定されたJOBを実行する。
     * @param jobName JOB名
     * @return {@link JobExecution}
     */
    public JobExecution startJob(String jobName) throws Exception {
        final JobOperator operator = BatchRuntime.getJobOperator();
        final long executionId = operator.start(jobName, null);
        final JobExecution execution = operator.getJobExecution(executionId);

        while (true) {
            TimeUnit.MILLISECONDS.sleep(500);
            if (execution.getEndTime() != null || execution.getBatchStatus() == BatchStatus.FAILED) {
                break;
            }
        }
        return execution;
    }

    /**
     * 指定されたJOBを再実行する。
     * @param jobName JOB名
     * @param prevExecutionId 前回の実行ID
     * @return {@link JobExecution}
     */
    public JobExecution restartJob(String jobName, long prevExecutionId) throws Exception {
        final JobOperator operator = BatchRuntime.getJobOperator();
        final long executionId = operator.restart(prevExecutionId, null);
        final JobExecution execution = operator.getJobExecution(executionId);

        while (true) {
            TimeUnit.MILLISECONDS.sleep(500);
            if (execution.getEndTime() != null || execution.getBatchStatus() == BatchStatus.FAILED) {
                break;
            }
        }
        return execution;
    }

    /**
     * 指定されたJOBを再実行する。
     */
    public JobExecution restartJob(long executionId) throws Exception {
        final JobOperator operator = BatchRuntime.getJobOperator();
        final long restartExecutionId = operator.restart(executionId, null);
        final JobExecution execution = operator.getJobExecution(restartExecutionId);
        while (true) {
            TimeUnit.MILLISECONDS.sleep(500);
            if (execution.getEndTime() != null || execution.getBatchStatus() == BatchStatus.FAILED) {
                break;
            }
        }
        return execution;
    }
}
