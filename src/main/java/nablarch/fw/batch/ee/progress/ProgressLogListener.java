package nablarch.fw.batch.ee.progress;

import java.util.List;
import jakarta.batch.api.chunk.listener.AbstractItemWriteListener;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.inject.Named;

/**
 * Chunk実行時の進捗ログを出力する{@link jakarta.batch.api.chunk.listener.ItemWriteListener}実装クラス
 *
 * @author Naoki Yamamoto
 */
@Named
@Dependent
public class ProgressLogListener extends AbstractItemWriteListener {

    /** 進捗管理Bean */
    private final ProgressManager progressManager;

    /**
     * {@link ProgressManager}をインジェクトするコンストラクタ。
     *
     * @param progressManager 進捗管理Bean
     */
    @Inject
    public ProgressLogListener(final ProgressManager progressManager) {
        this.progressManager = progressManager;
    }

    @Override
    public void afterWrite(final List<Object> items) throws Exception {
        progressManager.outputProgressInfo();
    }
}
