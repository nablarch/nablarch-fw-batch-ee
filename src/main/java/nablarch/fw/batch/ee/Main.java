package nablarch.fw.batch.ee;

import java.util.HashSet;
import java.util.Set;

import javax.batch.operations.JobOperator;
import javax.batch.runtime.BatchRuntime;
import javax.batch.runtime.BatchStatus;
import javax.batch.runtime.JobExecution;

import nablarch.core.util.StringUtil;

/**
 * バッチアプリケーションのメインクラス
 * <p/>
 * 実行引数として、対象JOBのXMLファイル名(.xmlを除いたファイル名)を指定する。
 * <p/>
 * 終了コードは以下の通り。<br>
 * <ul>
 * <li>正常終了：0 - バッチステータスが {@link BatchStatus#COMPLETED} で、終了ステータスが "WARNING" 以外の場合</li>
 * <li>異常終了：1 - バッチステータスが {@link BatchStatus#COMPLETED} 以外の場合</li>
 * <li>警告終了：2 - バッチステータスが {@link BatchStatus#COMPLETED} で、終了ステータスが "WARNING" の場合</li>
 * </ul>
 * なお、JOBの終了待ちの間に中断された場合は、異常終了のコードを返す。
 * <p/>
 * バリデーションエラーなど警告すべき事項が発生している場合に、警告終了させることができる。
 * 警告終了の方法はchunkまたはbatchlet内で、{@link javax.batch.runtime.context.JobContext#setExitStatus(String)}を
 * 呼び出し "WARNING" を終了ステータスとして設定する。
 * <p/>
 *
 * @author T.Shimoda
 */
public final class Main {

    /**
     * プライベートコンストラクタ
     */
    private Main() {
    }

    /**
     * メインメソッド。<br>
     * 指定されたJOBのXMLファイル名を実行する。
     *
     * @param args 第一引数にJOBのXMLファイル名を指定すること。
     * @throws IllegalArgumentException mainメソッドの引数の指定が正しくない場合
     */
    public static void main(final String... args) {
        if (args.length != 1 || StringUtil.isNullOrEmpty(args[0])) {
            throw new IllegalArgumentException("Please specify JOB XML name as the first argument.");
        }
        final int exitCode = executeJob(args[0]);
        System.exit(exitCode);
    }

    /**
     * ジョブを実行する
     *
     * @param jobXMLName JOBのXMLファイル名
     * @return 終了コード
     */
    private static int executeJob(final String jobXMLName) {
        // Jobの実行
        final JobOperator jobOperator = BatchRuntime.getJobOperator();
        final long executionId = jobOperator.start(jobXMLName, null);
        final JobExecution jobExecution = jobOperator.getJobExecution(executionId);
        // Jobの終了を待つ
        try {
            waitForFinishingJob(jobExecution);
        } catch (InterruptedException ignored) {
            return 1;//中断された場合は例外はスローせず、異常終了とする。
        }
        return getExitCode(jobExecution);
    }

    /**
     * ジョブの終了を待つ
     *
     * @param jobExecution ジョブの実行情報
     * @throws InterruptedException 中断された場合
     */
    private static void waitForFinishingJob(final JobExecution jobExecution) throws InterruptedException {
        final Set<BatchStatus> endStatuses = new HashSet<BatchStatus>();
        endStatuses.add(BatchStatus.ABANDONED);
        endStatuses.add(BatchStatus.COMPLETED);
        endStatuses.add(BatchStatus.FAILED);
        endStatuses.add(BatchStatus.STOPPED);
        while (true) {
            Thread.sleep(100);
            if (endStatuses.contains(jobExecution.getBatchStatus())) {
                break;
            }
        }
    }

    /**
     * 終了コードを導出する
     *
     * @param jobExecution ジョブの実行情報
     * @return 終了コード
     */
    private static int getExitCode(final JobExecution jobExecution) {
        // バッチステータスがCOMPLETED以外の場合は、異常終了
        if (jobExecution.getBatchStatus() != BatchStatus.COMPLETED) {
            return 1;
        }
        // 終了ステータスが WARNING の場合は、警告終了
        final String status = jobExecution.getExitStatus();
        if ("WARNING".equals(status)) {
            return 2;
        }
        // 上記いずれにも当てはまらない場合は、正常終了
        return 0;
    }
}
