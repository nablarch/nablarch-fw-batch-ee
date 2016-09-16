package nablarch.fw.batch.ee.integration.app;

import nablarch.fw.batch.ee.listener.NablarchListenerContext;
import nablarch.fw.batch.ee.listener.step.AbstractNablarchStepListener;

public class ThrowErrorBeforeStepListener extends AbstractNablarchStepListener {
    @Override
    public void beforeStep(NablarchListenerContext context) {
        throw new IllegalArgumentException("error:" + context.getStepName());
    }
}
