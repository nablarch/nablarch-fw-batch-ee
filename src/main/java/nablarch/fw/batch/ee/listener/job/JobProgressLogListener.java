package nablarch.fw.batch.ee.listener.job;

import java.text.MessageFormat;

import nablarch.fw.batch.ee.listener.NablarchListenerContext;
import nablarch.fw.batch.progress.ProgressLogger;

/**
 * JOBの進捗ログを出力するリスナークラス。<br>
 * JOB開始時と終了時にログを出力し、終了時にはステータスも併せて出力する。
 *
 * @author Shohei Ukawa
 */
public class JobProgressLogListener extends AbstractNablarchJobListener {

    /**
     * JOB開始のログを出力する。
     */
    @Override
    public void beforeJob(NablarchListenerContext context) {
        ProgressLogger.write(MessageFormat.format("start job. job name: [{0}]", context.getJobName()));
    }

    /**
     * JOB終了のログを出力する。
     */
    @Override
    public void afterJob(NablarchListenerContext context) {
        ProgressLogger.write(MessageFormat.format("finish job. job name: [{0}]", context.getJobName()));
    }
}
