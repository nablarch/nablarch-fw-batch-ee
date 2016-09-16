package nablarch.fw.batch.ee.initializer;

import mockit.Mocked;
import mockit.Verifications;
import nablarch.core.log.app.FailureLogUtil;
import nablarch.core.log.app.LogInitializationHelper;

import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

/**
 * {@link LogInitializer}のテスト。
 *
 * @author Naoki Yamamoto
 */
public class LogInitializerTest {

    @Before
    public void setUp() throws Exception {
        LogInitializer.isInitialized = false;
    }

    /**
     * {@link LogInitializer#initialize()}のテスト。
     * initializeメソッドが1度だけ実行されるケース。
     *
     * @throws Exception
     */
    @Test
    public void testInitialize(@Mocked FailureLogUtil failureLogUtil,
                               @Mocked LogInitializationHelper logInitializationHelper) throws Exception {
        // 初期化済みフラグがfalseであること
        assertThat(LogInitializer.isInitialized, is(false));

        LogInitializer.initialize();

        // ログ初期化処理が実行されていること
        new Verifications() {{
            FailureLogUtil.initialize(); times = 1;
            LogInitializationHelper.initialize(); times = 1;
        }};

        // 初期化済みフラグがtrueであること
        assertThat(LogInitializer.isInitialized, is(true));
    }

    /**
     * {@link LogInitializer#initialize()}のテスト。
     * 複数ジョブの起動によりinitializeメソッドが複数回実行されるケース。
     *
     * @throws Exception
     */
    @Test
    public void testBeforeJobForInitialized(
            @Mocked FailureLogUtil failureLogUtil,
            @Mocked LogInitializationHelper logInitializationHelper) throws Exception {

        // 初期化済みフラグがfalseであること
        assertThat(LogInitializer.isInitialized, is(false));

        // 1回目
        LogInitializer.initialize();

        // 2回目
        LogInitializer.initialize();

        // 初期化済みフラグがtrueであること
        assertThat(LogInitializer.isInitialized, is(true));

        // ログ初期化処理が一度だけ実行されていること
        new Verifications() {{
            FailureLogUtil.initialize(); times = 1;
            LogInitializationHelper.initialize(); times = 1;
        }};
    }
}