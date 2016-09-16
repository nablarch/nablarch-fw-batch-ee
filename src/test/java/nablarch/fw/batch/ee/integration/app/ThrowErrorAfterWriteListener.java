package nablarch.fw.batch.ee.integration.app;

import java.util.List;

import nablarch.fw.batch.ee.listener.NablarchListenerContext;
import nablarch.fw.batch.ee.listener.chunk.AbstractNablarchItemWriteListener;

public class ThrowErrorAfterWriteListener extends AbstractNablarchItemWriteListener {

    @Override
    public void beforeWrite(NablarchListenerContext context, List<Object> items) {
        System.out.println("ThrowErrorAfterWriteListener#beforeWrite");
        super.beforeWrite(context, items);
    }

    @Override
    public void afterWrite(NablarchListenerContext context, List<Object> items) {
        System.out.println("ThrowErrorAfterWriteListener#afterWrite");
        throw new IllegalArgumentException("error:" + context.getStepName());
    }

    @Override
    public void onWriteError(NablarchListenerContext context, List<Object> items, Exception ex) {
        System.out.println("ThrowErrorAfterWriteListener#onWriteError");
        super.onWriteError(context, items, ex);
    }
}
