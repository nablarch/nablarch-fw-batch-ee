package nablarch.fw.batch.progress;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import nablarch.fw.batch.ee.integration.InMemoryAppender;

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
        final Progress progress = new Progress(2.345, estimatedEndTime, 100);
        sut.print(progress);

        final List<String> messages = InMemoryAppender.getLogMessages("ALL");
        if (messages.isEmpty()) {
            fail("ログが出力されているはず");
        }
        final String message = messages.get(0);
        assertThat(message,
                containsString("tps: [2.35], estimated end time: ["
                        + new SimpleDateFormat("yyyy/MM/dd hh:mm:ss.SSS").format(estimatedEndTime) + "]," 
                        + " remaining count: [100]"));
    }
}