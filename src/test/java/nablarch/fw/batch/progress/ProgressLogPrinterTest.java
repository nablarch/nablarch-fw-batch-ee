package nablarch.fw.batch.progress;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.Matchers.contains;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import nablarch.fw.batch.ee.integration.InMemoryAppender;
import nablarch.fw.batch.ee.progress.JBatchProcessName;

import org.junit.Before;
import org.junit.Test;

/**
 * {@link ProgressLogPrinter}のテスト
 */
public class ProgressLogPrinterTest {

    private final ProgressLogPrinter sut = new ProgressLogPrinter();

    @Before
    public void setUp() throws Exception {
        InMemoryAppender.clear();
    }

    @Test
    public void 進捗状況がログに出力できること() throws Exception {
        final Date estimatedEndTime = new Date();
        final Progress progress = new Progress(2.345, 6.789, estimatedEndTime, 100);
        sut.print(new JBatchProcessName("test-job", "test-step"), progress);

        final List<String> messages = InMemoryAppender.getLogMessages("PROGRESS");
        if (messages.isEmpty()) {
            fail("ログが出力されているはず");
        }
        final String message = messages.get(0);
        assertThat(message,
                containsString("job name: [test-job] step name: [test-step] total tps: [2.35] current tps: [6.79] estimated end time: ["
                        + new SimpleDateFormat("yyyy/MM/dd hh:mm:ss.SSS").format(estimatedEndTime) + ']'
                        + " remaining count: [100]"));
    }

    @Test
    public void 終了予測時間が不明な場合はunknownで出力されること() throws Exception {
        final Progress progress = new Progress(0, 0, null, 100);
        sut.print(new JBatchProcessName("test-job", "test-step"), progress);

        final List<String> messages = InMemoryAppender.getLogMessages("PROGRESS");
        assertThat(messages, contains(
                containsString("job name: [test-job] step name: [test-step] total tps: [0.00] current tps: [0.00]"
                        + " estimated end time: [unknown] remaining count: [100]"))
        );
    }
}