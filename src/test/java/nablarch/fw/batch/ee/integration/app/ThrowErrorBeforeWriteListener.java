package nablarch.fw.batch.ee.integration.app;

import nablarch.fw.batch.ee.listener.NablarchListenerContext;
import nablarch.fw.batch.ee.listener.chunk.AbstractNablarchItemWriteListener;

import java.util.List;

public class ThrowErrorBeforeWriteListener extends AbstractNablarchItemWriteListener {

    @Override
    public void beforeWrite(NablarchListenerContext context, List<Object> items) {
        throw new IllegalArgumentException("error:" + context.getStepName());
    }
}
