package nablarch.fw.batch.ee.initializer;

import nablarch.core.repository.SystemRepository;

import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;

/**
 * {@link RepositoryInitializer}のテスト。
 *
 * @author Naoki Yamamoto
 */
public class RepositoryInitializerTest {

    @Before
    public void setUp() throws Exception {
        SystemRepository.clear();
        RepositoryInitializer.isInitialized = false;
    }

    /**
     * {@link RepositoryInitializer#initialize(String)}のテスト。
     * 指定した設定ファイルが読み込まれることを確認するケース。
     *
     * @throws Exception
     */
    @Test
    public void testInitializeForReadFile() throws Exception {

        // batch-boot.xmlを読み込むケース。
        RepositoryInitializer.initialize("batch-boot.xml");

        // batch-boot.xmlに定義されているコンポーネントがリポジトリに登録されていること
        assertThat(SystemRepository.get("mockComponent"), is(notNullValue()));
        assertThat(SystemRepository.get("customComponent"), is(nullValue()));


        // custom-batch-boot.xmlを読み込むケース
        SystemRepository.clear();
        RepositoryInitializer.isInitialized = false;
        RepositoryInitializer.initialize("custom-batch-boot.xml");

        // custom-batch-boot.xmlに定義されているコンポーネントがリポジトリに登録されていること
        assertThat(SystemRepository.get("customComponent"), is(notNullValue()));
        assertThat(SystemRepository.get("mockComponent"), is(nullValue()));
    }

    /**
     * {@link RepositoryInitializer#initialize(String)}のテスト。
     * initializeメソッドが複数回実行されるケース。
     *
     * @throws Exception
     */
    @Test
    public void testInitializeForInitialized() throws Exception {

        // 設定ファイルが読み込まれていないこと。
        assertThat(RepositoryInitializer.isInitialized, is(false));

        // 1回目
        RepositoryInitializer.initialize("batch-boot.xml");
        MockComponent mockComponent = SystemRepository.get("mockComponent");

        // 設定ファイルが読み込み済みであること。
        assertThat(mockComponent, is(notNullValue()));
        assertThat(mockComponent.getProperty(), is("test"));
        assertThat(RepositoryInitializer.isInitialized, is(true));

        // 2回目
        mockComponent.setProperty("loaded");
        RepositoryInitializer.initialize("batch-boot.xml");
        mockComponent = SystemRepository.get("mockComponent");

        // 設定ファイルが再度読み込まれていないこと。
        assertThat(mockComponent, is(notNullValue()));
        assertThat(mockComponent.getProperty(), is("loaded"));
    }
}