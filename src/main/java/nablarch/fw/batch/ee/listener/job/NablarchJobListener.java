package nablarch.fw.batch.ee.listener.job;

import nablarch.fw.batch.ee.listener.NablarchListenerContext;

/**
 * JOBの開始と終了時に任意の処理を行うためのインタフェース。
 *
 * @author Hisaaki Shioiri
 */
public interface NablarchJobListener {

    /**
     * JOB開始後の処理を行う。
     *
     * @param context {@link NablarchListenerContext}
     */
    void beforeJob(final NablarchListenerContext context);

    /**
     * JOB終了時の処理を行う。
     *
     * @param context {@link NablarchListenerContext}
     */
    void afterJob(final NablarchListenerContext context);
}
