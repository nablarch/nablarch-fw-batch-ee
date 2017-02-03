package nablarch.fw.batch.ee.integration;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import ch.qos.logback.core.encoder.Encoder;
import nablarch.core.util.FileUtil;

/**
 * ログメッセージをメモリ上に保持するアペンダ
 */
public class InMemoryAppender extends AppenderBase<ILoggingEvent> {

    protected Encoder<ILoggingEvent> encoder;

    /** ログメッセージを保持するマップ */
    private static final Map<String, List<String>> LOG_MAP = new HashMap<String, List<String>>();

    @Override
    protected void append(ILoggingEvent eventObject) {
        List<String> messages = LOG_MAP.get(getName());
        if (messages == null) {
            messages = new ArrayList<String>();
            LOG_MAP.put(getName(), messages);
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            encoder.init(out);
            encoder.doEncode(eventObject);
        } catch (IOException ignored) {
        } finally {
            FileUtil.closeQuietly(out);
            try {
                encoder.close();
            } catch (IOException ignored) {
            }
        }

        messages.add(out.toString());
        System.out.print(out);
    }

    public static void clear() {
        LOG_MAP.clear();
    }

    public static List<String> getLogMessages(final String name) {
        return LOG_MAP.get(name);
    }

    public void setEncoder(Encoder encoder) {
        this.encoder = encoder;
    }
}
