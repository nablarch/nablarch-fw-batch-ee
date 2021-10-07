package nablarch.fw.batch.ee.integration;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import ch.qos.logback.core.encoder.Encoder;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

        byte[] encodedMessage = encoder.encode(eventObject);
        String message = new String(encodedMessage, Charset.defaultCharset());

        messages.add(message);
        System.out.print(message);
    }

    public static void clear() {
        LOG_MAP.clear();
    }

    public static List<String> getLogMessages(final String name) {
        return LOG_MAP.get(name);
    }

    public void setEncoder(Encoder<ILoggingEvent> encoder) {
        this.encoder = encoder;
    }
}
