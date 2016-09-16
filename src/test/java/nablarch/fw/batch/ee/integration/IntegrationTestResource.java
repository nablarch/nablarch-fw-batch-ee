package nablarch.fw.batch.ee.integration;


import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.TimeUnit;

import javax.batch.operations.JobOperator;
import javax.batch.runtime.BatchRuntime;
import javax.batch.runtime.BatchStatus;
import javax.batch.runtime.JobExecution;
import javax.sql.DataSource;

import nablarch.core.db.statement.ResultSetIterator;
import nablarch.core.db.statement.SqlResultSet;
import nablarch.core.repository.di.DiContainer;
import nablarch.core.repository.di.config.xml.XmlComponentDefinitionLoader;

import org.junit.rules.ExternalResource;

/**
 * Java Batchの結合テストをサポートするクラス。
 */
public class IntegrationTestResource extends ExternalResource {

    private Connection connection = null;

    @Override
    protected void before() throws Throwable {

        final XmlComponentDefinitionLoader loader = new XmlComponentDefinitionLoader("integration-test/datasource.xml");
        final DiContainer container = new DiContainer(loader);
        final DataSource dataSource = container.getComponentByName("dataSource");

        connection = dataSource.getConnection();
        createBatchOutputTable();
        createBatchStatus();
    }

    @Override
    protected void after() {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * バッチのアウトプットテーブルを作成する。
     *
     * @throws SQLException
     */
    private void createBatchOutputTable() throws SQLException {
        final PreparedStatement create = connection.prepareStatement("create table IF NOT EXISTS batch_output ("
                + "id number,"
                + "name varchar2(100),"
                + "PRIMARY KEY (id))");
        create.execute();
        create.close();

        final PreparedStatement truncate = connection.prepareStatement("truncate table batch_output");
        truncate.execute();
        truncate.close();
    }

    /**
     * バッチのステータス管理テーブルを作成する。
     */
    private void createBatchStatus() throws Exception {
        final PreparedStatement create = connection.prepareStatement("create table IF NOT EXISTS batch_status ("
                + "job_name VARCHAR2(100),"
                + "active char(1),"
                + "PRIMARY KEY (job_name))");
        create.execute();
        create.close();

        final PreparedStatement truncate = connection.prepareStatement("truncate table batch_status");
        truncate.execute();
        truncate.close();

        final PreparedStatement insert = connection.prepareStatement("insert into batch_status values (?, ?)");
        insert.setString(1, "batchlet-integration-test");
        insert.setString(2, "0");
        insert.addBatch();
        insert.setString(1, "chunk-integration-test");
        insert.setString(2, "0");
        insert.addBatch();
        insert.executeBatch();
        insert.close();
        connection.commit();
    }

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

    public void clearBatchOutputTable() throws Exception {
        final PreparedStatement truncate = connection.prepareStatement("truncate table batch_output");
        truncate.execute();
        truncate.close();
        connection.commit();
    }

    public SqlResultSet findBatchOutputTable() throws Exception {
        final PreparedStatement statement = connection.prepareStatement("select * from batch_output order by id");
        final ResultSet rs = statement.executeQuery();
        final SqlResultSet rows = new SqlResultSet(new ResultSetIterator(rs, null), 0, 0);
        return rows;
    }

    public void insertBatchOutputTable(int id) throws Exception{
        final PreparedStatement statement = connection.prepareStatement("insert into batch_output values (?, ?)");
        statement.setInt(1, id);
        statement.setString(2, "data_" + id);
        statement.execute();
        statement.close();
        connection.commit();
    }

    public void deleteBatchOutputTable(int id) throws Exception {
        final PreparedStatement statement = connection.prepareStatement("delete from batch_output where id = ?");
        statement.setInt(1, id);
        statement.execute();
        statement.close();
        connection.commit();
    }

    public void updateBatchStatus(String jobName, String activeFlag) throws Exception {
        final PreparedStatement statement = connection.prepareStatement(
                "update batch_status set active = ? where job_name = ?");
        statement.setString(1, activeFlag);
        statement.setString(2, jobName);
        statement.execute();
        statement.close();
    }

    public String findBatchStatus(String jobName) throws Exception {
        final PreparedStatement statement = connection.prepareStatement(
                "select active from batch_status where job_name = ?");
        statement.setString(1, jobName);
        final ResultSet rs = statement.executeQuery();
        if (!rs.next()) {
            throw new IllegalArgumentException("batch_status not found. jobname=" + jobName);
        }
        return rs.getString(1);
    }
}
