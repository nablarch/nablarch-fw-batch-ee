package nablarch.fw.batch.ee.listener.step;

import nablarch.fw.batch.ee.listener.NablarchListenerContext;

/**
 * ステップの開始と終了時に任意の処理を行うためのインタフェース。
 *
 * @author Hisaaki Shioiri
 */
public interface NablarchStepListener {

    /**
     * ステップ開始時の処理を行う。
     *
     * @param context {@link NablarchListenerContext}
     */
    void beforeStep(NablarchListenerContext context);

    /**
     * ステップ終了時の処理を行う。
     *
     * @param context {@link NablarchListenerContext}
     */
    void afterStep(NablarchListenerContext context);

}
