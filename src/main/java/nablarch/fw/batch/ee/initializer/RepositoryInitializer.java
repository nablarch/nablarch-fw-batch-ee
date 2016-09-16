package nablarch.fw.batch.ee.initializer;

import nablarch.core.repository.SystemRepository;
import nablarch.core.repository.di.DiContainer;
import nablarch.core.repository.di.config.xml.XmlComponentDefinitionLoader;

/**
 * コンポーネントの初期化を行うクラス。
 *
 * @author Naoki Yamamoto
 */
public class RepositoryInitializer {

    /** 初期化済みフラグ */
    static boolean isInitialized = false;

    /** 本クラスはインスタンスを作成しない。 */
    private RepositoryInitializer() {
    }

    /**
     * コンポーネントの初期化を行う。<br />
     * 別のジョブの実行等により既にコンポーネントの初期化が行われている場合は何もしない。
     *
     * @param diConfigFilePath コンポーネント設定ファイルパス
     */
    public static synchronized void initialize(String diConfigFilePath) {
        if (!isInitialized) {
            XmlComponentDefinitionLoader loader = new XmlComponentDefinitionLoader(diConfigFilePath);
            SystemRepository.load(new DiContainer(loader));
            isInitialized = true;
        }
    }
}
