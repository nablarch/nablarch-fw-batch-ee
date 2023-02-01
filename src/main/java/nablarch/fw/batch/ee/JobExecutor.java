package nablarch.fw.batch.ee;

import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import jakarta.batch.operations.JobOperator;
import jakarta.batch.runtime.BatchRuntime;
import jakarta.batch.runtime.BatchStatus;
import jakarta.batch.runtime.JobExecution;

/**
 * JOB の実行をするクラス
 * <p/>
 * JOBを実行し、終了するまで待機して以下の戻り値を返す。
 * <ul>
 * <li>正常終了：0 - 終了ステータスが "WARNING" 以外の場合で、バッチステータスが {@link BatchStatus#COMPLETED}の場合</li>
 * <li>異常終了：1 - 終了ステータスが "WARNING" 以外の場合で、バッチステータスが {@link BatchStatus#COMPLETED} 以外の場合</li>
 * <li>警告終了：2 - 終了ステータスが "WARNING" の場合</li>
 * </ul>
 * なお、JOBの終了待ちの間に中断された場合は、異常終了のコードを返す。
 * <p/>
 * バリデーションエラーなど警告すべき事項が発生している場合に、警告終了させることができる。
 * 警告終了の方法はchunkまたはbatchlet内で、{@link jakarta.batch.runtime.context.JobContext#setExitStatus(String)}を
 * 呼び出し "WARNING" を終了ステータスとして設定する。警告終了時は、バッチステータスは任意の値を許可するため、
 * chunkまたはbatchlet内で、 例外を送出しバッチステータスが {@link BatchStatus#FAILED} となる場合であっても、
 * 終了ステータスに “WARNING” を設定していれば、警告終了する。
 *
 * @author T.Shimoda
 */
public class JobExecutor {

    /** JOB XMLファイル名（.xmlを除いた名前） */
    private final String jobXmlName;

    /** バッチ起動時に指定する引数 */
    private final Properties properties;

    /**
     * JOBの実行情報。
     */
    private JobExecution jobExecution;

    /**
     * コンストラクタ
     * @param jobXmlName JOB XMLファイル名
     * @param properties properties
     */
    public JobExecutor(final String jobXmlName, final Properties properties) {
        this.jobXmlName = jobXmlName;
        this.properties = properties;
    }

    /**
     * JOB XMLファイル名を返す。
     * @return JOB XMLファイル名
     */
    public String getJobXmlName() {
        return jobXmlName;
    }

    /**
     * JOBの実行情報を返す。
     * 開始時刻や終了時刻などJOBの詳細を取得したい場合にこのAPIから取得する。
     * @return JOBの実行情報、開始前はnullを返す。
     */
    public JobExecution getJobExecution() {
        return jobExecution;
    }

    /**
     * JOBを実行する。JOBが終了または中断されるまで待機する。
     * @return 終了コード
     */
    public int execute() {
        return execute(100);
    }

    /**
     * JOBを実行する。JOBが終了または中断されるまで待機する。
     * 指定したミリ秒間隔で終了しているかどうかのチェックを行う。
     * @param mills 終了をチェックするミリ秒の間隔
     * @return 終了コード
     * @throws IllegalArgumentException ミリ秒が0以下の場合
     */
    public int execute(final long mills) {
        if (mills < 1) {
            throw new IllegalArgumentException("mills must be greater than 0.");
        }
        start();
        return waitForEnd(mills);
    }

    /**
     * JOBを開始する。非同期で開始するため、終了を待つには、{@link JobExecutor#waitForEnd}を呼び出す。
     * @throws IllegalStateException JOBが既に開始されている場合
     */
    private void start() {
        if (jobExecution != null) {
            throw new IllegalStateException(String.format("Job is already started. JobXmlName=[%s]", jobXmlName));
        }
        final JobOperator jobOperator = BatchRuntime.getJobOperator();
        final long executionId = jobOperator.start(jobXmlName, properties);
        jobExecution = jobOperator.getJobExecution(executionId);
    }

    /**
     * 開始したJOBの終了を待つ。
     * 指定したミリ秒間隔で終了しているかどうかのチェックを行う。
     * @param mills チェックを行うミリ秒
     * @return 終了コード
     */
    private int waitForEnd(final long mills) {
        final Set<BatchStatus> endStatuses = new HashSet<BatchStatus>();
        endStatuses.add(BatchStatus.ABANDONED);
        endStatuses.add(BatchStatus.COMPLETED);
        endStatuses.add(BatchStatus.FAILED);
        endStatuses.add(BatchStatus.STOPPED);
        while (true) {
            try {
                BatchStatus batchStatus = jobExecution.getBatchStatus();
                if (endStatuses.contains(batchStatus)) {
                    break;
                }
                Thread.sleep(mills);
            } catch (InterruptedException ignored) {
                return 1;//中断された場合は例外はスローせず、異常終了とする。
            }
        }
        return getExitCode();
    }

    /**
     * 終了コードを導出する
     *
     * @return 終了コード
     */
    private int getExitCode() {
        // 終了ステータスが WARNING の場合は、警告終了
        final String status = jobExecution.getExitStatus();
        if ("WARNING".equals(status)) {
            return 2;
        }
        // バッチステータスがCOMPLETED以外の場合は、異常終了
        if (jobExecution.getBatchStatus() != BatchStatus.COMPLETED) {
            return 1;
        }
        // 上記いずれにも当てはまらない場合は、正常終了
        return 0;
    }
}
