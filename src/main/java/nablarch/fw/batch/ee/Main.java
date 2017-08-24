package nablarch.fw.batch.ee;

import java.util.Arrays;
import java.util.Properties;

import nablarch.core.util.StringUtil;
import nablarch.fw.launcher.CommandLineParser;
import nablarch.fw.launcher.CommandLineParser.Result;

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
        if (args.length == 0 || StringUtil.isNullOrEmpty(args[0])) {
            throw new IllegalArgumentException("Please specify JOB XML name as the first argument.");
        }
        final String jobId = args[0];
        final Properties properties = toProperties(args);
        final JobExecutor executor = new JobExecutor(jobId, properties);
        System.exit(executor.execute());
    }

    /**
     * コマンドライン引数をバッチ起動時に指定するパラメータに変換する。
     *
     * @param args コマンドライン引数
     * @return バッチ起動時に指定するパラメータ
     */
    private static Properties toProperties(final String[] args) {
        final String[] commandLine = Arrays.copyOfRange(args, 1, args.length);
        final CommandLineParser parser = new CommandLineParser();
        final Result result = parser.parse(commandLine);
        if (!result.getArgs()
                   .isEmpty()) {
            throw new IllegalArgumentException("command line args is unsupported."
                    + " specify only the command line option.(example: --name1 value1 --name2 value2)");
        }

        final Properties properties = new Properties();
        properties.putAll(result.getOpts());
        return properties;
    }
}
