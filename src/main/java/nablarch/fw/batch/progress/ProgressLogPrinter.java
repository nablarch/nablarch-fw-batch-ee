package nablarch.fw.batch.progress;

import java.text.SimpleDateFormat;

/**
 * 進捗状況をログに出力するクラス。
 *
 * @author siosio
 */
public class ProgressLogPrinter implements ProgressPrinter {
    
    @Override
    public void print(final ProcessName processName, final Progress progress) {
        final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd hh:mm:ss.SSS");

        ProgressLogger.write(String.format("%s tps: [%.2f] estimated end time: [%s] remaining count: [%d]",
                processName.formatProcessName(),
                progress.getTps(),
                dateFormat.format(progress.getEstimatedEndTime()),
                progress.getRemainingCount()));
    }
}
