package nablarch.fw.batch.ee.listener;

import nablarch.core.log.Logger;
import nablarch.core.log.LoggerManager;
import nablarch.core.repository.SystemRepository;

import javax.batch.runtime.context.JobContext;
import javax.batch.runtime.context.StepContext;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * 各レベルのリスナー実行クラスにて共通で必要となる処理をまとめたクラス。
 * <p/>
 * 本クラスの使用手順は以下の通り。
 * <ol>
 * <li>{@link NablarchListenerExecutor}のインスタンス変数をリスナー実行クラスに定義する</li>
 * <li>リスナー実行クラスの事前処理内で{@link NablarchListenerExecutor}のインスタンスを作成する</li>
 * <li>リスナー実行クラスで状況に応じて{@link #executeBefore(Runner)}、{@link #executeAfter(Runner)}、
 * {@link #executeOnError(Runner)}を呼び出す
 * <p/>
 * なお、各メソッドの引数で渡す{@link Runner}は、個別に実装クラスを作成し、
 * {@link Runner#run(Object, NablarchListenerContext)}にリスナーの事前処理等を記述する。
 * <p/>
 * 例:
 * <pre>
 * {@code
 * executor.executeBefore(new Runner<NablarchJobListener>() {
 *     public void run(NablarchJobListener listener, NablarchListenerContext context) {
 *         listener.beforeJob(jobContext);
 *     }
 * );
 * }
 * </pre>
 * </li>
 * </ol>
 * @param <T> リスナークラス
 * @author Naoki Yamamoto
 */
public class NablarchListenerExecutor<T> {

    /** ロガー */
    private static final Logger LOGGER = LoggerManager.get(NablarchListenerExecutor.class);

    /** {@link #executeBefore(Runner)} (T)}で実行された{@link T}のスタック */
    private final LinkedList<T> executedListenerStack = new LinkedList<T>();

    /** {@link SystemRepository}に登録されているデフォルトのリスナーリスト名 */
    private final String listenerListName;

    /** ジョブ名 */
    private final JobContext jobContext;

    /** ステップ名 */
    private final StepContext stepContext;

    /**
     * コンストラクタ
     *
     * @param listenerListName {@link SystemRepository}に登録されているデフォルトのリスナーリスト名
     * @param jobContext ジョブコンテキスト
     */
    public NablarchListenerExecutor(String listenerListName, JobContext jobContext) {
        this(listenerListName, jobContext, null);
    }

    /**
     * コンストラクタ
     *
     * @param listenerListName {@link SystemRepository}に登録されているデフォルトのリスナーリスト名
     * @param jobContext ジョブコンテキスト
     * @param stepContext ステップコンテキスト
     */
    public NablarchListenerExecutor(String listenerListName, JobContext jobContext, StepContext stepContext) {
        this.listenerListName = listenerListName;
        this.jobContext = jobContext;
        this.stepContext = stepContext;
    }

    /**
     * {@link SystemRepository}より実行対象となるリスナーのリストを取得し、リスナーの事前処理を順次実行する。
     *
     * @param runner 事前処理を行う{@link Runner}
     */
    public final void executeBefore(Runner<T> runner) {
        final NablarchListenerContext context = new NablarchListenerContext(jobContext, stepContext);
        final List<T> listeners = lookupNablarchListenerList();
        for (T listener : listeners) {
            executedListenerStack.push(listener);
            try {
                runner.run(listener, context);
            } catch (RuntimeException ex) {
                context.setProcessSucceeded(false);
                throw ex;
            } catch (Error er) {
                context.setProcessSucceeded(false);
                throw er;
            }
        }
    }

    /**
     * {@link #executeBefore(Runner)}にて実行された(例外が発生したものを含む)リスナーの事後処理を、
     * {@link #executeBefore(Runner)}で実行された順番とは逆順で実行する。
     * <p/>
     * リスナーの事後処理の実行時に例外が発生した場合、ワーニングログを出力して後続のリスナーの事後処理を実行後、
     * 発生した例外を呼び出し元に送出する。
     * 複数の例外が発生した場合は、最初に発生した例外を呼び出し元に送出する。
     *
     * @param runner 事後処理を行う{@link Runner}
     * @throws Exception {@link Runner#run(Object, NablarchListenerContext)}実行時に最初に送出された例外
     */
    public final void executeAfter(Runner<T> runner) throws Exception {
        final NablarchListenerContext context = new NablarchListenerContext(jobContext, stepContext);
        final ThrowableList throwableList = new ThrowableList();
        for (T listener : executedListenerStack) {
            try {
                runner.run(listener, context);
            } catch (Throwable t) {
                context.setProcessSucceeded(false);
                throwableList.add(t);
                outputLog(t);
            }
        }
        throwableList.throwFirstThrowable();
    }

    /**
     * {@link #executeBefore(Runner)}にて実行された(例外が発生したものを含む)リスナーのエラー発生時処理を、
     * {@link #executeBefore(Runner)}で実行された順番とは逆順で実行する。
     * <p/>
     * リスナーのエラー発生時処理の実行時に例外が発生した場合、
     * ワーニングログを出力して後続のリスナーのエラー発生時処理を実行後、発生した例外を呼び出し元に送出する。
     * 複数の例外が発生した場合は、最初に発生した例外を呼び出し元に送出する。
     *
     * @param runner エラー発生時処理を行う{@link Runner}
     * @throws Exception {@link Runner#run(Object, NablarchListenerContext)}実行時に最初に送出された例外
     */
    public final void executeOnError(Runner<T> runner) throws Exception {
        final NablarchListenerContext context = new NablarchListenerContext(jobContext, stepContext);
        final ThrowableList throwableList = new ThrowableList();
        for (T listener : executedListenerStack) {
            try {
                runner.run(listener, context);
            } catch (Throwable t) {
                context.setProcessSucceeded(false);
                throwableList.add(t);
                outputLog(t);
            }
        }
        throwableList.throwFirstThrowable();
    }

    /**
     * 実行すべきリスナーのリストを{@link SystemRepository}から取得する。
     * <p/>
     * {@link SystemRepository}に登録されているリスナーを以下の順で探索し、合致したリスナーのリストを返す。
     * <ol>
     *     <li>ジョブのステップ毎のリスナー</li>
     *     <li>ジョブ毎のリスナー</li>
     *     <li>デフォルト設定のリスナー</li>
     * </ol>
     * デフォルト設定が存在しない場合には、空のリストを返す。
     *
     * @return リスナーのリスト
     */
    private List<T> lookupNablarchListenerList() {
        if (stepContext != null) {
            final List<T> customStepListeners = SystemRepository.get(
                    jobContext.getJobName() + '.' + stepContext.getStepName() + '.' + listenerListName);
            if (customStepListeners != null) {
                return customStepListeners;
            }
        }
        final List<T> customListeners = SystemRepository.get(
                jobContext.getJobName() + '.' + listenerListName);
        if (customListeners != null) {
            return customListeners;
        }
        final List<T> defaultListeners = SystemRepository.get(listenerListName);
        if (defaultListeners != null) {
            return defaultListeners;
        }
        return Collections.emptyList();
    }

    /**
     * ワーニングログを出力する。
     * <p/>
     * ステップ名が設定されていればステップ名もログに出力する。
     *
     * @param t 例外
     */
    private void outputLog(Throwable t) {
        if (stepContext != null) {
            LOGGER.logWarn(MessageFormat.format(
                    "failed to execute listener. job=[{0}], step=[{1}]", jobContext.getJobName(), stepContext.getStepName()), t);
        } else {
            LOGGER.logWarn(MessageFormat.format(
                    "failed to execute listener. job=[{0}]", jobContext.getJobName()), t);
        }
    }

    /**
     * リスナーを実行するランナー
     *
     * @param <L> リスナー
     * @author Naoki Yamamoto
     */
    public interface Runner<L> {
        /**
         * リスナーの処理を実行する。
         *
         * @param listener リスナー
         * @param context リスナー実行時のコンテキスト情報
         */
        void run(L listener, NablarchListenerContext context);
    }

    /**
     * {@link Throwable}を保持するリスト
     */
    private static class ThrowableList {

        /** {@link Throwable}を格納するリスト */
        private final List<Throwable> throwables = new ArrayList<Throwable>();

        /**
         * リストに{@link Throwable}を格納する
         *
         * @param throwable リストに追加する{@link Throwable}
         */
        public void add(Throwable throwable) {
            throwables.add(throwable);
        }

        /**
         * リストに格納されている先頭の{@link Throwable}を呼び出し元に送出する。
         * <p/>
         * 送出の際には、{@link Throwable}の型を{@link Exception}もしくは{@link Error}に変換する。
         * <p/>
         * リストに{@link Throwable}が格納されていない場合は何もしない。
         *
         * @throws Exception リストの先頭に格納された{@link Exception}
         */
        public void throwFirstThrowable() throws Exception {
            if (!throwables.isEmpty()) {
                Throwable throwable = throwables.get(0);
                if (throwable instanceof Exception) {
                    throw (Exception) throwable;
                } else {
                    throw (Error) throwable;
                }
            }
        }
    }
}
