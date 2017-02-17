package nablarch.fw.batch.ee.progress;

import nablarch.fw.batch.progress.ProcessName;

/**
 * バッチ処理を識別する名前を持つクラス。
 *
 * @author siosio
 */
public class JBatchProcessName implements ProcessName {

    /** ジョブ名 */
    private final String jobName;

    /** ステップ名 */
    private final String stepName;

    /**
     * プロセス名を構築する。
     *
     * @param jobName ジョブ名
     * @param stepName ステップ名
     */
    public JBatchProcessName(final String jobName, final String stepName) {
        this.jobName = jobName;
        this.stepName = stepName;
    }

    @Override
    public String formatProcessName() {
        return "job name: [" + jobName + "] step name: [" + stepName + ']';
    }
}
