package nablarch.fw.batch.ee.integration.app;

import nablarch.core.log.Logger;
import nablarch.core.log.LoggerManager;
import nablarch.fw.batch.ee.listener.NablarchListenerContext;
import nablarch.fw.batch.ee.listener.step.AbstractNablarchStepListener;

import java.text.MessageFormat;

/**
 * ログ出力用StepListener
 */
public class LoggingStepLevelListener extends AbstractNablarchStepListener {

    /** ロガー */
    private static final Logger LOGGER = LoggerManager.get("listener");

    @Override
    public void beforeStep(NablarchListenerContext context) {
        super.beforeStep(context);
        LOGGER.logInfo(
                MessageFormat.format("{0} is executed on {1}.", LoggingStepLevelListener.class.getSimpleName(), context.getStepName())
        );
    }
}
