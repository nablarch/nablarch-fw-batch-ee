package nablarch.fw.batch.ee;

import nablarch.core.util.StringUtil;

/**
 * バッチアプリケーションのメインクラス
 * <p/>
 * 実行引数として、対象JOBのXMLファイル名(.xmlを除いたファイル名)を指定する。
 * 実処理は、{@link JobExecutor}に移譲しているため、詳細はそちらを参照。
 *
 * @author T.Shimoda
 */
public final class Main {

    /**
     * プライベートコンストラクタ
     */
    private Main() {
    }

    /**
     * メインメソッド。<br>
     * 指定されたJOBのXMLファイル名を実行する。
     *
     * @param args 第一引数にJOBのXMLファイル名を指定すること。
     * @throws IllegalArgumentException mainメソッドの引数の指定が正しくない場合
     */
    public static void main(final String... args) {
        if (args.length != 1 || StringUtil.isNullOrEmpty(args[0])) {
            throw new IllegalArgumentException("Please specify JOB XML name as the first argument.");
        }
        final JobExecutor executor = new JobExecutor(args[0]);
        System.exit(executor.execute());
    }
}
