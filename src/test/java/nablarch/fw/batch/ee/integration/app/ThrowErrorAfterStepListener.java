package nablarch.fw.batch.ee.integration.app;

import nablarch.fw.batch.ee.listener.NablarchListenerContext;
import nablarch.fw.batch.ee.listener.step.AbstractNablarchStepListener;

public class ThrowErrorAfterStepListener extends AbstractNablarchStepListener {
    @Override
    public void afterStep(NablarchListenerContext context) {
        throw new IllegalArgumentException("error:" + context.getStepName());
    }
}
