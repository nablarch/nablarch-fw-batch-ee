package nablarch.fw.batch.ee.listener.chunk;

import java.text.MessageFormat;
import java.util.List;

import nablarch.fw.batch.ee.listener.NablarchListenerContext;
import nablarch.fw.batch.progress.ProgressLogger;

/**
 * chunkの進捗ログを出力するリスナークラス。
 *
 * @deprecated chunkの進捗ログを出力するリスナは、
 * {@link nablarch.fw.batch.ee.progress.ProgressLogListener}に置き換わりました。
 * @author Shohei Ukawa
 */
@Deprecated
public class ChunkProgressLogListener extends AbstractNablarchItemWriteListener {

    /**
     * chunkの進捗ログを出力する。
     */
    @Override
    public void afterWrite(
            final NablarchListenerContext context,
            final List<Object> items) {
        ProgressLogger.write(MessageFormat.format("chunk progress. write count=[{0}]", context.getReadCount()));
    }
}
