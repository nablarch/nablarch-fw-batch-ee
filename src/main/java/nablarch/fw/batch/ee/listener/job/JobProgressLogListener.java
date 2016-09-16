package nablarch.fw.batch.ee.listener.job;

import java.text.MessageFormat;

import javax.batch.runtime.BatchStatus;

import nablarch.core.log.Logger;
import nablarch.core.log.LoggerManager;
import nablarch.fw.batch.ee.listener.NablarchListenerContext;

/**
 * JOBの進捗ログを出力するリスナークラス。<br>
 * JOB開始時と終了時にログを出力し、終了時にはステータスも併せて出力する。
 *
 * @author Shohei Ukawa
 */
public class JobProgressLogListener extends AbstractNablarchJobListener {

    /** 進捗ログ出力用ロガー */
    private static final Logger LOGGER = LoggerManager.get("PROGRESS");

    /**
     * JOB開始のログを出力する。
     */
    @Override
    public void beforeJob(NablarchListenerContext context) {
        LOGGER.logInfo(MessageFormat.format("start job. job name=[{0}]", context.getJobName()));
    }

    /**
     * JOB終了のログを出力する。
     */
    @Override
    public void afterJob(NablarchListenerContext context) {
        BatchStatus status = context.getJobBatchStatus();
        // この時点でSTARTEDなら後に必ずCOMPLETEDとなるため、COMPLETEDと表示する。
        if (status == BatchStatus.STARTED) {
            status = BatchStatus.COMPLETED;
        }
        LOGGER.logInfo(
                MessageFormat.format("finish job. job name=[{0}], batch status=[{1}]", context.getJobName(), status));
    }
}
