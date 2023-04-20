package nablarch.fw.batch.ee.initializer;

import nablarch.core.log.app.FailureLogUtil;
import nablarch.core.log.app.LogInitializationHelper;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mockStatic;

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
    public void testInitialize() throws Exception {
        try (
            final MockedStatic<FailureLogUtil> failureLogUtil = mockStatic(FailureLogUtil.class);
            final MockedStatic<LogInitializationHelper> logInitializationHelper = mockStatic(LogInitializationHelper.class);
        ) {
            // 初期化済みフラグがfalseであること
            assertThat(LogInitializer.isInitialized, is(false));

            LogInitializer.initialize();

            // ログ初期化処理が実行されていること
            failureLogUtil.verify(FailureLogUtil::initialize);
            logInitializationHelper.verify(LogInitializationHelper::initialize);

            // 初期化済みフラグがtrueであること
            assertThat(LogInitializer.isInitialized, is(true));
        }
    }

    /**
     * {@link LogInitializer#initialize()}のテスト。
     * 複数ジョブの起動によりinitializeメソッドが複数回実行されるケース。
     *
     * @throws Exception
     */
    @Test
    public void testBeforeJobForInitialized() throws Exception {
        try (
            final MockedStatic<FailureLogUtil> failureLogUtil = mockStatic(FailureLogUtil.class);
            final MockedStatic<LogInitializationHelper> logInitializationHelper = mockStatic(LogInitializationHelper.class);
        ) {
            // 初期化済みフラグがfalseであること
            assertThat(LogInitializer.isInitialized, is(false));

            // 1回目
            LogInitializer.initialize();

            // 2回目
            LogInitializer.initialize();

            // 初期化済みフラグがtrueであること
            assertThat(LogInitializer.isInitialized, is(true));

            // ログ初期化処理が一度だけ実行されていること
            failureLogUtil.verify(FailureLogUtil::initialize);
            logInitializationHelper.verify(LogInitializationHelper::initialize);
        }
    }
}