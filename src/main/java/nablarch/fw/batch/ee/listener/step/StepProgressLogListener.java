package nablarch.fw.batch.ee.listener.step;

import java.text.MessageFormat;

import nablarch.core.log.Logger;
import nablarch.core.log.LoggerManager;
import nablarch.fw.batch.ee.listener.NablarchListenerContext;

/**
 * ステップの進捗ログを出力するクラス。<br>
 * ステップ開始時と終了時にログを出力する。
 *
 * @author Shohei Ukawa
 */
public class StepProgressLogListener extends AbstractNablarchStepListener {

    /** 進捗ログ出力用ロガー */
    private static final Logger LOGGER = LoggerManager.get("PROGRESS");

    /**
     * ステップ開始のログを出力する。
     */
    @Override
    public void beforeStep(NablarchListenerContext context) {
        LOGGER.logInfo(MessageFormat.format("start step. step name=[{0}]", context.getStepName()));
    }

    /**
     * ステップ終了のログを出力する。
     */
    @Override
    public void afterStep(NablarchListenerContext context) {
        String status = context.isStepProcessSucceeded() ? "SUCCEEDED" : "FAILED";

        LOGGER.logInfo(
                MessageFormat.format("finish step. step name=[{0}], step status=[{1}]", context.getStepName(), status));
    }
}
