package nablarch.fw.batch.ee.listener.job;

import nablarch.fw.batch.ee.listener.NablarchListenerContext;
import nablarch.fw.handler.AlreadyProcessRunningException;
import nablarch.fw.handler.DuplicateProcessChecker;

import javax.batch.operations.BatchRuntimeException;

/**
 * 同一ジョブが同時に複数実行されないことを保証するための{@link NablarchJobListener}実装クラス。
 *
 * @author Hisaaki Shioiri
 */
public class DuplicateJobRunningCheckListener extends AbstractNablarchJobListener {

    /** 複数起動時の場合に設定する終了ステータス */
    private static final String EXIT_STATUS = "JobAlreadyRunning";

    /** 多重起動チェックを行うクラス。 */
    private DuplicateProcessChecker duplicateProcessChecker;

    /**
     * 多重起動チェックするクラスを設定する。
     *
     * @param duplicateProcessChecker 多重起動をチェックするクラス
     */
    public void setDuplicateProcessChecker(DuplicateProcessChecker duplicateProcessChecker) {
        this.duplicateProcessChecker = duplicateProcessChecker;
    }

    /**
     * プロセス(JOB)の多重起動防止チェックを行う。
     * <p/>
     * 多重起動ではない場合、現在のジョブをアクティブ状態に変更する。
     *
     * @see DuplicateProcessChecker#checkAndActive(String)
     */
    @Override
    public void beforeJob(NablarchListenerContext context) {
        try {
            duplicateProcessChecker.checkAndActive(context.getJobName());
        } catch (AlreadyProcessRunningException e) {
            context.setExitStatus(EXIT_STATUS);
            throw new BatchRuntimeException(e);
        }
    }

    /**
     * プロセス(JOB)の非活性化を行う。
     *
     * @see DuplicateProcessChecker#inactive(String)
     */
    @Override
    public void afterJob(NablarchListenerContext context) {
        if (EXIT_STATUS.equals(context.getExitStatus())) {
            // ２重起動だった場合には、プロセスの非アクティブ化処理は行わない。
            return;
        }
        duplicateProcessChecker.inactive(context.getJobName());
    }
}

