package nablarch.fw.batch.ee.listener.chunk;

import java.text.MessageFormat;
import java.util.List;

import nablarch.core.log.Logger;
import nablarch.core.log.LoggerManager;
import nablarch.fw.batch.ee.listener.NablarchListenerContext;

/**
 * chunkの進捗ログを出力するリスナークラス。
 *
 * @author Shohei Ukawa
 */
public class ChunkProgressLogListener extends AbstractNablarchItemWriteListener {

    /** 進捗ログ出力用ロガー */
    private static final Logger LOGGER = LoggerManager.get("PROGRESS");

    /**
     * chunkの進捗ログを出力する。
     */
    @Override
    public void afterWrite(
            final NablarchListenerContext context,
            final List<Object> items) {
        LOGGER.logInfo(MessageFormat.format("chunk progress. write count=[{0}]", context.getReadCount()));
    }
}
