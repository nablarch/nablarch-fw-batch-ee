package nablarch.fw.batch.progress;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 進捗状況をログに出力するクラス。
 *
 * @author siosio
 */
public class ProgressLogPrinter implements ProgressPrinter {
    
    @Override
    public void print(final ProcessName processName, final Progress progress) {
        final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd hh:mm:ss.SSS");

        final Date estimatedEndTime = progress.getEstimatedEndTime();
        ProgressLogger.write(String.format("%s total tps: [%.2f] current tps: [%.2f] estimated end time: [%s] remaining count: [%d]",
                processName.formatProcessName(),
                progress.getTps(),
                progress.getCurrentTps(),
                estimatedEndTime == null ? "unknown" : dateFormat.format(estimatedEndTime),
                progress.getRemainingCount()));
    }
}
