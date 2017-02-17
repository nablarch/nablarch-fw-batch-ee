package nablarch.fw.batch.ee.listener.step;

import java.text.MessageFormat;

import nablarch.fw.batch.ee.listener.NablarchListenerContext;
import nablarch.fw.batch.progress.ProgressLogger;

/**
 * ステップの進捗ログを出力するクラス。<br>
 * ステップ開始時と終了時にログを出力する。
 *
 * @author Shohei Ukawa
 */
public class StepProgressLogListener extends AbstractNablarchStepListener {

    /**
     * ステップ開始のログを出力する。
     */
    @Override
    public void beforeStep(NablarchListenerContext context) {
        ProgressLogger.write(MessageFormat.format("start step. job name: [{0}] step name: [{1}]",
                context.getJobName(), context.getStepName()));
    }

    /**
     * ステップ終了のログを出力する。
     */
    @Override
    public void afterStep(NablarchListenerContext context) {
        ProgressLogger.write(MessageFormat.format(
                "finish step. job name: [{0}] step name: [{1}] step status: [{2}]",
                context.getJobName(),
                context.getStepName(),
                context.getStepExitStatus()));
    }
}
