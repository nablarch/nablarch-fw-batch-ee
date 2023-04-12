package nablarch.fw.batch.ee.integration;

import jakarta.batch.runtime.BatchStatus;
import jakarta.batch.runtime.JobExecution;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Response;
import mockit.Deencapsulation;
import nablarch.core.repository.ObjectLoader;
import nablarch.core.repository.SystemRepository;
import nablarch.core.repository.di.DiContainer;
import nablarch.core.repository.di.config.xml.XmlComponentDefinitionLoader;
import nablarch.core.util.StringUtil;
import nablarch.fw.batch.ee.JobExecutor;
import nablarch.fw.batch.ee.initializer.RepositoryInitializer;
import nablarch.fw.batch.ee.integration.app.FileWriter;
import nablarch.fw.batch.ee.integration.app.RegisterBatchOutputTable;
import nablarch.fw.batch.ee.integration.app.ThrowErrorWriter;
import nablarch.test.support.db.helper.VariousDbTestHelper;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.ArchivePath;
import org.jboss.shrinkwrap.api.Filter;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.slf4j.bridge.SLF4JBridgeHandler;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.LogManager;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;

/**
 * Java Batchの結合テスト。
 * <p>
 * ログ出力のテストがコメントアウトされている理由について。
 *
 * 本テストは Arquillian で実行されるが、以前は GlassFish の組み込み版を利用していた。
 * その場合は、ログ出力結果をインメモリに記録してあとでアサートすることができていた。
 *
 * しかし、 Jakarta EE 10 対応の際に GlassFish の EE 10 対応版が Arquillian で利用できなかったため、
 * 代わりに WildFly の Embedded 版を利用するように変更を行った。
 * この WildFly の Embedded 版は、 Embedded と言っているが実際は別途ダウンロードした WildFly にデプロイ
 * して実行するという方式となっている。
 * このため、デプロイしたコードは WildFly が管理する別のクラスローダーによって読み込まれた
 * クラス上で実行されることになり、インメモリを経由したログのアサートができなくなった。
 *
 * ログをファイルに出力するような方法も検証してみたが上手く動作させることができず、
 * 仕方なくログ出力のアサート部分だけをコメントアウトしてテストを通すこととした。
 *
 * ログのメッセージ内容自体は各ログ出力クラスの単体テストで担保されており、
 * また Java Batch の処理内容自体はテストできているため、
 * ログ出力の確認ができていなくても品質に大きな影響は無いと判断している。
 *
 * 将来、 EE 10 対応版の GlassFish の組み込み版が Arquillian で利用できるようになった際は、
 * コンテナを変更してログのテストも復活させること。
 * </p>
 *
 * @author Hisaaki Shioiri
 */
@RunWith(Arquillian.class)
public class BatchIntegrationTest {

    @Rule
    public IntegrationTestResource resource = new IntegrationTestResource();

    @Deployment
    public static WebArchive createDeployment() {
        final JavaArchive mainJar = buildMainJar();
        return buildWar(mainJar);
    }

    /**
     * src/main 以下のプロダクションコードを jar にアーカイブする。
     * @return プロダクションコードの Java アーカイブ
     */
    private static JavaArchive buildMainJar() {
        return ShrinkWrap.create(JavaArchive.class, "nablarch-fw-batch-ee.jar")
                .addPackages(true, path -> {
                    final Path realPath = Path.of("target/classes", path.get());
                    return Files.exists(realPath);
                }, "nablarch.fw.batch")
                .addAsManifestResource(
                        new File("src/main/resources/META-INF/services/jakarta.enterprise.inject.spi.Extension"),
                        "services/jakarta.enterprise.inject.spi.Extension")
                .addAsManifestResource(
                        new File("src/main/resources/META-INF/beans.xml"),
                        "beans.xml");
    }

    /**
     * src/test 以下のコードを元にして war アーカイブを生成する。
     * @param mainJar src/main をアーカイブした jar
     * @return war アーカイブ
     */
    private static WebArchive buildWar(JavaArchive mainJar) {
        final String dbProfile = System.getProperty("db-profile");
        final String[] profiles = StringUtil.isNullOrEmpty(dbProfile) ? new String[0] : new String[] {dbProfile};

        final WebArchive archive = ShrinkWrap.create(WebArchive.class, "batch.war")
                .addAsLibraries(
                        Maven.configureResolver()
                                .workOffline()
                                .loadPomFromFile("pom.xml", profiles)
                                .importCompileAndRuntimeDependencies()
                                .importTestDependencies()
                                .resolve()
                                .withTransitivity()
                                .asFile()
                )
                .addAsLibraries(mainJar)
                .addPackages(true, new Filter<ArchivePath>() {
                    @Override
                    public boolean include(ArchivePath path) {
                        final Path realPath = Path.of("target/test-classes", path.get());
                        return Files.exists(realPath);
                    }
                }, "nablarch.fw.batch");
        addTestResources(archive);

        return archive;
    }

    /**
     * src/test/resources 配下の全てのファイルを WebArchive の WEB-INF の下に配置する。
     * @param archive WebArchive
     */
    private static void addTestResources(WebArchive archive) {
        Path srcTestResources = Path.of("src/test/resources");
        Path classes = Path.of("classes");

        try {
            Files.walkFileTree(srcTestResources, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    final Path relativePath = srcTestResources.relativize(file);
                    final Path targetPath = classes.resolve(relativePath);
                    archive.addAsWebInfResource(file.toFile(), targetPath.toString().replace('\\', '/'));
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static boolean initialized;

    /**
     * クラス全体での初期化処理を行う。
     * <p>
     * BeforeClassを使用せずに自前のフラグで制御している理由について。<br>
     * この Arquillian のテストは、 Wildfly の embedded を使用している。<br>
     * この場合、 BeforeClass でコールバックされたときと BeforeEach でコールバックされたときで、
     * クラスローダーが異なるという現象が発生する（BeforeClass はもとの JUnit を起動したときの
     * クラスローダーだが、 BeforeEach のときは Wildfly が管理するクラスローダーになっている）。<br>
     * このため、 static フィールドで状態管理をしている VariousDbTestHelper などのクラスが
     * 正常に動作しなくなる問題が発生する。<br>
     * この問題を回避するため、本テストでは BeforeEach の先頭で1回だけ初期化処理を行うような
     * 形でクラス全体での初期化処理を行うように実装している。
     * </p>
     * @throws Exception 例外が発生した場合
     */
    public static void setUpClass() throws Exception {
        if (initialized) {
            return;
        }
        SLF4JBridgeHandler.removeHandlersForRootLogger();
        SLF4JBridgeHandler.install();

        // jBatch実装側のログをより詳細まで出るように変更。
        // デフォルトレベルだと、情報少なくてよくわからないので。
        LogManager.getLogManager()
                .readConfiguration(new ByteArrayInputStream(
                        ("handlers=org.slf4j.bridge.SLF4JBridgeHandler\n"
                                + ".level=INFO\n"
                                + "com.ibm.level=FINE\n"
                                + "org.slf4j.bridge.SLF4JBridgeHandler.level=FINEST\n"
                                + "org.slf4j.bridge.SLF4JBridgeHandler.formatter=java.util.logging.SimpleFormatter\n").getBytes()));

        DiContainer container = new DiContainer(new XmlComponentDefinitionLoader("db-default.xml"));
        VariousDbTestHelper.initialize(container);
        VariousDbTestHelper.createTable(BatchOutput.class);
        VariousDbTestHelper.createTable(nablarch.fw.batch.ee.integration.BatchStatus.class);

        initialized = true;
    }

    @Before
    public void setUp() throws Exception {
        setUpClass();

        Deencapsulation.setField(RepositoryInitializer.class, "isInitialized", false);
        RegisterBatchOutputTable.processExecuteFlag = false;
        ThrowErrorWriter.skipIds = new int[0];
        ThrowErrorWriter.errorId = -1;
        ThrowErrorWriter.retryError = Collections.emptySet();
//        InMemoryAppender.clear();

        setUpTestData();
    }

    private static void setUpTestData() {
        clearBatchOutputTable();
        VariousDbTestHelper.delete(nablarch.fw.batch.ee.integration.BatchStatus.class);
        VariousDbTestHelper.setUpTable(
                new nablarch.fw.batch.ee.integration.BatchStatus("batchlet-integration-test", "0")
                ,new nablarch.fw.batch.ee.integration.BatchStatus("chunk-integration-test", "0")
                ,new nablarch.fw.batch.ee.integration.BatchStatus("operator-batchlet-test", "0")
                ,new nablarch.fw.batch.ee.integration.BatchStatus("operator-chunk-test", "0")
                ,new nablarch.fw.batch.ee.integration.BatchStatus("step-scoped-integration-test", "0")
                ,new nablarch.fw.batch.ee.integration.BatchStatus("specified-steplevel-listener-test", "0")
                ,new nablarch.fw.batch.ee.integration.BatchStatus("specified-joblevel-listener-test", "0")
        );
    }

    @After
    public void tearDown() throws Exception {
        Deencapsulation.setField(RepositoryInitializer.class, "isInitialized", false);
    }

    /**
     * {@link jakarta.batch.api.Batchlet}実装クラスで行った登録処理が正常に完了していること。
     * <p/>
     * <ul>
     * <li>バッチ処理は正常に終了していること</li>
     * <li>Batchletで登録した10レコードが参照できること(トランザクションがコミットされていること)</li>
     * </ul>
     */
    @Test
    public void executeBatchlet_Success() throws Exception {

        // -------------------------------------------------- clear output table
        clearBatchOutputTable();

        // -------------------------------------------------- execute batch job
        final JobExecution execution = resource.startJob("batchlet-integration-test");
        assertThat("バッチ処理が正常に終わっていること", execution.getBatchStatus(), is(BatchStatus.COMPLETED));

        // -------------------------------------------------- assert output table
        List<BatchOutput> batchOutputs = findBatchOutputTable();
        assertThat("10レコード登録されるはず", batchOutputs.size(), is(10));
        for (int i = 0; i < 10; i++) {
            int index = i + 1;
            BatchOutput output = batchOutputs.get(i);
            assertThat(output.id, is(index));
            assertThat(output.name, is("name_" + index));
        }

        // -------------------------------------------------- assert batch status
        assertThat("バッチが終了したので非アクティブになっていること",
                findBatchStatus("batchlet-integration-test").active, is("0"));
    }

    /**
     * {@link jakarta.batch.api.Batchlet}実装クラスで例外が発生した場合、DBへの更新がロールバックされること。
     * <p/>
     * <ul>
     * <li>バッチ処理は異常終了(FAILED)していること</li>
     * <li>Batchletの処理はロールバックされるので元の1レコードのみが存在していること</li>
     * </ul>
     */
    @Test
    public void executeBatchlet_Failed() throws Exception {

        // -------------------------------------------------- clear output table
        clearBatchOutputTable();

        // -------------------------------------------------- add initial data
        insertBatchOutputTable(9);

        // -------------------------------------------------- execute batch job
        final JobExecution execution = resource.startJob("batchlet-integration-test");
        assertThat("一意成約違反が発生してバッチJOBは異常終了する", execution.getBatchStatus(), is(BatchStatus.FAILED));

        // -------------------------------------------------- assert execute batchlet?
        assertThat("Batchletは実行されていること", RegisterBatchOutputTable.processExecuteFlag, is(true));

        // -------------------------------------------------- assert output table
        List<BatchOutput> batchOutputs = findBatchOutputTable();
        assertThat(batchOutputs.size(), is(1));
        assertThat("初期データが存在する。", batchOutputs.get(0).id, is(9));

        // -------------------------------------------------- assert batch status
        assertThat("異常終了でもステータスは非アクティブに変更されること",
                findBatchStatus("batchlet-integration-test").active, is("0"));
    }

    /**
     * {@link jakarta.batch.api.Batchlet}実装クラスで{@link Error}系の例外が発生した場合、DBへの更新がロールバックされること。
     * <p/>
     * <ul>
     * <li>バッチ処理は、異常終了していること</li>
     * <li>Batchletの処理はロールバックされるのでテーブルはからのまま</li>
     * </ul>
     */
    @Test
    public void executeBatch_throwError() throws Exception {
        // -------------------------------------------------- clear output table
        clearBatchOutputTable();

        // -------------------------------------------------- assert execute batchlet?
        final JobExecution execution = resource.startJob("batchlet-error-integration-test");
        assertThat("異常終了していること", execution.getBatchStatus(), is(BatchStatus.FAILED));

        // -------------------------------------------------- assert output table
        assertThat("レコードは登録されていないこと", findBatchOutputTable().isEmpty(), is(true));

    }

    /**
     * ChunkステップをもつバッチJOBを実行し正常に処理が完了すること。
     * <p/>
     * <ul>
     * <li>バッチJOBは正常終了していること</li>
     * <li>アウトプットテーブルにレコードが登録されていること</li>
     * </ul>
     */
    @Test
    public void executeChunk_Success() throws Exception {
        // -------------------------------------------------- clear output table
        clearBatchOutputTable();

        // -------------------------------------------------- execute batch job
        final JobExecution execution = resource.startJob("chunk-integration-test");
        assertThat("バッチ処理が正常に終わっていること", execution.getBatchStatus(), is(BatchStatus.COMPLETED));

        // -------------------------------------------------- assert batch output table
        List<BatchOutput> batchOutputs = findBatchOutputTable();
        assertThat("25レコード登録されていること", batchOutputs.size(), is(25));

        for (int i = 0; i < 25; i++) {
            int index = i + 1;
            BatchOutput output = batchOutputs.get(i);
            assertThat(output.id, is(index));
            assertThat(output.name, is("name_" + index));
        }

        // ★レコードが登録されていることは確認している
//        List<String> messages = InMemoryAppender.getLogMessages("PROGRESS");
//        assertThat(messages, contains(
//                startsWith("INFO progress start job. job name: [chunk-integration-test]"),
//                startsWith("INFO progress start step. job name: [chunk-integration-test] step name: [myStep]"),
//                startsWith("INFO progress job name: [chunk-integration-test] step name: [myStep] input count: [25]"),
//                startsWith("INFO progress chunk progress. write count=[10]"),
//                allOf(
//                        startsWith("INFO progress job name: [chunk-integration-test] step name: [myStep] total tps:"),
//                        containsString("current tps:"),
//                        containsString("estimated end time:"),
//                        containsString("remaining count: [15]")
//                ),
//                startsWith("INFO progress chunk progress. write count=[20]"),
//                allOf(
//                        startsWith("INFO progress job name: [chunk-integration-test] step name: [myStep] total tps:"),
//                        containsString("current tps:"),
//                        containsString("estimated end time:"),
//                        containsString("remaining count: [5]")
//                ),
//                startsWith("INFO progress chunk progress. write count=[25]"),
//                allOf(
//                        startsWith("INFO progress job name: [chunk-integration-test] step name: [myStep] total tps:"),
//                        containsString("current tps:"),
//                        containsString("estimated end time:"),
//                        containsString("remaining count: [0]")
//                ),
//                startsWith("INFO progress finish step. job name: [chunk-integration-test] step name: [myStep] step status: [null]"),
//                startsWith("INFO progress finish job. job name: [chunk-integration-test]")
//        ));
    }

    /**
     * Chunkステップの処理中に例外が発生した場合、前回のChunkのデータはコミットされJOBが異常終了すること。
     * <p/>
     * <ul>
     * <li>バッチJOBは異常終了すること。</li>
     * <li>エラーが発生したChunkより前に処理されたChunkはコミットされていること</li>
     * <li>エラーが発生したChunkのデータはロールバックされていること</li>
     * <li>障害原因レコードを除去後にリスタートするとJOBは正常に終了すること</li>
     * </ul>
     */
    @Test
    public void executeChunk_Failed() throws Exception {
        // -------------------------------------------------- setup output table
        clearBatchOutputTable();
        insertBatchOutputTable(18);

        // -------------------------------------------------- execute batch job
        final JobExecution execution = resource.startJob("chunk-integration-test");
        assertThat("バッチ処理が異常終了していること", execution.getBatchStatus(), is(BatchStatus.FAILED));

        // -------------------------------------------------- assert output table
        List<BatchOutput> batchOutputs = findBatchOutputTable();
        assertThat("障害発生原因のレコード + 登録した10レコードが登録されているはず", batchOutputs.size(), is(11));

//        List<String> messages = InMemoryAppender.getLogMessages("PROGRESS");
//        assertThat(messages, contains(
//                startsWith("INFO progress start job. job name: [chunk-integration-test]"),
//                startsWith("INFO progress start step. job name: [chunk-integration-test] step name: [myStep]"),
//                startsWith("INFO progress job name: [chunk-integration-test] step name: [myStep] input count: [25]"),
//                startsWith("INFO progress chunk progress. write count=[10]"),
//                allOf(
//                        startsWith("INFO progress job name: [chunk-integration-test] step name: [myStep] total tps:"),
//                        containsString("current tps:"),
//                        containsString("estimated end time:"),
//                        containsString("remaining count: [15]")
//                ),
//                startsWith("INFO progress finish step. job name: [chunk-integration-test] step name: [myStep] step status: [null]"),
//                startsWith("INFO progress finish job. job name: [chunk-integration-test]")
//        ));

        // -------------------------------------------------- clear abnormal data
        deleteBatchOutputTable(18);
//        InMemoryAppender.clear();

        // -------------------------------------------------- restart batch job
        final JobExecution restartExecution = resource.restartJob(execution.getExecutionId());
        assertThat("再開後はJOBが正常に終了すること", restartExecution.getBatchStatus(), is(BatchStatus.COMPLETED));

        // -------------------------------------------------- assert output table
        List<BatchOutput> restartedBatchOutputs = findBatchOutputTable();
        assertThat("処理対象の25レコード全てが登録されていること", restartedBatchOutputs.size(), is(25));
        for (int i = 0; i < 25; i++) {
            int index = i + 1;
            BatchOutput output = restartedBatchOutputs.get(i);
            assertThat(output.id, is(index));
            assertThat(output.name, is("name_" + index));
        }

//        messages = InMemoryAppender.getLogMessages("PROGRESS");
//        assertThat(messages, contains(
//                startsWith("INFO progress start job. job name: [chunk-integration-test]"),
//                startsWith("INFO progress start step. job name: [chunk-integration-test] step name: [myStep]"),
//                startsWith("INFO progress job name: [chunk-integration-test] step name: [myStep] input count: [15]"),
//                startsWith("INFO progress chunk progress. write count=[10]"),
//                allOf(
//                        startsWith("INFO progress job name: [chunk-integration-test] step name: [myStep] total tps:"),
//                        containsString("current tps:"),
//                        containsString("estimated end time:"),
//                        containsString("remaining count: [5]")
//                ),
//                startsWith("INFO progress chunk progress. write count=[15]"),
//                allOf(
//                        startsWith("INFO progress job name: [chunk-integration-test] step name: [myStep] total tps:"),
//                        containsString("current tps:"),
//                        containsString("estimated end time:"),
//                        containsString("remaining count: [0]")
//                ),
//                startsWith("INFO progress finish step. job name: [chunk-integration-test] step name: [myStep] step status: [null]"),
//                startsWith("INFO progress finish job. job name: [chunk-integration-test]")
//        ));


    }

    /**
     * Chunkステップ処理中に{@link Error}が発生した場合、そのChunk以前のデータはコミットされること。
     * <p/>
     * <ul>
     * <li>バッチJOBは異常終了する</li>
     * <li>エラーが発生したChunkのデータはロールバックされる</li>
     * </ul>
     */
    @Test
    public void executeChunk_ThrowError() throws Exception {
        // -------------------------------------------------- setup output table
        clearBatchOutputTable();

        // -------------------------------------------------- set error id
        ThrowErrorWriter.errorId = 25;

        // -------------------------------------------------- execute batch job
        final JobExecution execution = resource.startJob("chunk-error-integration-test");
        assertThat("バッチ処理が異常終了すること", execution.getBatchStatus(), is(BatchStatus.FAILED));

        // -------------------------------------------------- assert output table
        List<BatchOutput> batchOutputs = findBatchOutputTable();
        assertThat("2つのChunkでスキップ対象例外が発生するので、最後のChunkのデータだけ登録される", batchOutputs.size(), is(20));
        assertThat("最初のレコードのID:1", batchOutputs.get(0).id, is(1));
        assertThat("最後のレコードのID:20", batchOutputs.get(19).id, is(20));
    }

    /**
     * Chunkステップの処理中にスキップ対象の例外が発生した場合、そのレコードが含まれるChunkの処理はスキップされること。
     * <p/>
     * <ul>
     * <li>JOBは正常に終了すること。</li>
     * <li>スキップされたレコードを持つChunkは登録されていないこと</li>
     * </ul>
     */
    @Test
    public void executeChunk_Skip() throws Exception {
        // -------------------------------------------------- setup output table
        clearBatchOutputTable();

        // -------------------------------------------------- set error id
        ThrowErrorWriter.skipIds = new int[] {10, 19};

        // -------------------------------------------------- execute batch job
        final JobExecution execution = resource.startJob("chunk-error-integration-test");
        assertThat("バッチ処理は正常に終了すること", execution.getBatchStatus(), is(BatchStatus.COMPLETED));

        // -------------------------------------------------- assert output table
        List<BatchOutput> batchOutputs = findBatchOutputTable();
        assertThat("2つのChunkでスキップ対象例外が発生するので、最後のChunkのデータだけ登録される", batchOutputs.size(), is(29));
        assertThat("最初のレコードのIDは21", batchOutputs.get(0).id, is(21));
        assertThat("最後のレコードのIDは49", batchOutputs.get(28).id, is(49));
    }

    /**
     * Chunkのステップ処理中にスキップ対象の例外が発生した場合でリミットを超えた場合はバッチJOBが異常終了すること
     * <p/>
     * <ul>
     * <li>バッチJOBが異常終了すること</li>
     * <li>スキップされたChunkのデータは破棄されていること</li>
     * <li>スキップされていないChunkのデータはコミットされていること</li>
     * </ul>
     */
    @Test
    public void executeChunk_SkipLimit() throws Exception {
        // -------------------------------------------------- setup output table
        clearBatchOutputTable();

        // -------------------------------------------------- set error id
        ThrowErrorWriter.skipIds = new int[] {10, 19, 39};

        // -------------------------------------------------- execute batch job
        final JobExecution execution = resource.startJob("chunk-error-integration-test");
        assertThat("スキップのリミット突破でJOBは異常終了する ", execution.getBatchStatus(), is(BatchStatus.FAILED));

        // -------------------------------------------------- assert output table
        List<BatchOutput> batchOutputs = findBatchOutputTable();
        assertThat("正常に処理できたChunkのデータのみ存在する", batchOutputs.size(), is(10));
        assertThat("最初のデータはID:21", batchOutputs.get(0).id, is(21));
        assertThat("最後のデータはID:30", batchOutputs.get(9).id, is(30));
    }

    /**
     * Chunkの処理中にリトライ対象の例外が発生した場合、リトライが実行され処理が正常に完了すること。
     * <p/>
     * <ul>
     * <li>バッチJOBが正常に終了すること。</li>
     * <li>リトライが成功し全てのデータの登録が成功すること</li>
     * </ul>
     */
    @Test
    public void executeChunk_Retry() throws Exception {
        // -------------------------------------------------- setup output table
        clearBatchOutputTable();

        // -------------------------------------------------- set error id
        ThrowErrorWriter.retryError = new HashSet<Integer>(Arrays.asList(25));

        // -------------------------------------------------- execute batch job
        final JobExecution execution = resource.startJob("chunk-error-integration-test");
        assertThat("正常に終了すること", execution.getBatchStatus(), is(BatchStatus.COMPLETED));

        // -------------------------------------------------- assert output table
        List<BatchOutput> batchOutputs = findBatchOutputTable();
        assertThat("リトライが実行されすべて正常に登録される", batchOutputs.size(), is(49));
        for (int i = 0; i < 49; i++) {
            int index = i + 1;
            BatchOutput output = batchOutputs.get(i);
            assertThat(output.id, is(index));
        }
    }

    /**
     * Chunkの処理中にリトライ対象の例外が発生した場合でリミットを突破する場合JOBが異常終了すること。
     * <p/>
     * <ul>
     * <li>バッチJOBは異常終了すること</li>
     * <li>リミットを突破したChunkより前のChunkのデータはコミットされていること</li>
     * </ul>
     */
    @Test
    public void executeChunk_RetryLimit() throws Exception {
        // -------------------------------------------------- setup output table
        clearBatchOutputTable();

        // -------------------------------------------------- set error id
        ThrowErrorWriter.retryError = new HashSet<Integer>(Arrays.asList(25, 41));

        // -------------------------------------------------- execute batch job
        final JobExecution execution = resource.startJob("chunk-error-integration-test");
        assertThat("リトライリミット突破でJOBは異常終了する", execution.getBatchStatus(), is(BatchStatus.FAILED));

        // -------------------------------------------------- assert output table
        List<BatchOutput> batchOutputs = findBatchOutputTable();
        assertThat("リトライ上限突破で、最後にリトライ対象エラーがでたChunkの前までが登録される",
                batchOutputs.size(), is(40));
        for (int i = 0; i < 40; i++) {
            int index = i + 1;
            BatchOutput output = batchOutputs.get(i);
            assertThat(output.id, is(index));
        }
    }

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    /**
     * 複数のステップ(Batchlet->Chunk)からなるJOBを実行した場合でも正常に処理できること。
     * <p/>
     * <ul>
     * <li>バッチJOBが正常に終了すること</li>
     * <li>バッチレットで処理したデータがコミットされていること</li>
     * <li>バッチレットで処理したデータを入力としてChunk処理ができること</li>
     * </ul>
     */
    @Test
    public void executeMultiStep() throws Exception {
        final File outputFile = folder.newFile("outputFile.txt");
        FileWriter.outputPath = outputFile;

        // -------------------------------------------------- setup output table
        clearBatchOutputTable();

        // -------------------------------------------------- execute batch job
        final JobExecution execution = resource.startJob("multi-step-integration-test");
        assertThat("正常に終了すること", execution.getBatchStatus(), is(BatchStatus.COMPLETED));

        // -------------------------------------------------- assert output table
        List<BatchOutput> batchOutputs = findBatchOutputTable();
        assertThat("Batchletでデータが登録できていること", batchOutputs.size(), is(10));
        for (int i = 0; i < 10; i++) {
            int index = i + 1;
            BatchOutput output = batchOutputs.get(i);
            assertThat(output.id, is(index));
        }

        // -------------------------------------------------- assert output file
        final BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(outputFile),
                Charset.forName("utf-8")));
        try {
            for (int i = 0; i < 10; i++) {
                int index = i + 1;
                assertThat("ファイルに出力されていること", reader.readLine(), is(String.valueOf(index)));
            }
            assertThat("ファイルの終端に達していること", reader.readLine(), is(nullValue()));
        } finally {
            reader.close();
        }

//        List<String> messages = InMemoryAppender.getLogMessages("PROGRESS");
//        assertThat(messages, contains(
//                startsWith("INFO progress start job. job name: [multi-step-integration-test]"),
//                startsWith("INFO progress start step. job name: [multi-step-integration-test] step name: [batchlet]"),
//                startsWith("INFO progress job name: [multi-step-integration-test] step name: [batchlet] input count: [10]"),
//                allOf(
//                        startsWith("INFO progress job name: [multi-step-integration-test] step name: [batchlet] total tps:"),
//                        containsString("current tps:"),
//                        containsString("estimated end time:"),
//                        containsString("remaining count: [5]")
//                ),
//                allOf(
//                        startsWith("INFO progress job name: [multi-step-integration-test] step name: [batchlet] total tps:"),
//                        containsString("current tps:"),
//                        containsString("estimated end time:"),
//                        containsString("remaining count: [0]")
//                ),
//                startsWith("INFO progress finish step. job name: [multi-step-integration-test] step name: [batchlet] step status: [null]"),
//                startsWith("INFO progress start step. job name: [multi-step-integration-test] step name: [chunk]"),
//                startsWith("INFO progress job name: [multi-step-integration-test] step name: [chunk] input count: [10]"),
//                startsWith("INFO progress chunk progress. write count=[10]"),
//                allOf(
//                        startsWith("INFO progress job name: [multi-step-integration-test] step name: [chunk] total tps:"),
//                        containsString("current tps:"),
//                        containsString("estimated end time:"),
//                        containsString("remaining count: [0]")
//                ),
//                startsWith("INFO progress finish step. job name: [multi-step-integration-test] step name: [chunk] step status: [null]"),
//                startsWith("INFO progress finish job. job name: [multi-step-integration-test]")
//
//        ));
    }

    /**
     * マルチステップ構成のJOBで後続Stepが失敗した場合のテスト。
     */
    @Test
    public void executeMultiStep_Failed() throws Exception {
        final File outputFile = new File(folder.getRoot(), "outputFile.txt");
        FileWriter.outputPath = null;

        // -------------------------------------------------- setup output table
        clearBatchOutputTable();

        // -------------------------------------------------- execute batch job
        final JobExecution execution = resource.startJob("multi-step-integration-test");
        assertThat("2番目のステップが失敗するのでJOBは異常終了する",
                execution.getBatchStatus(), is(BatchStatus.FAILED));

        // -------------------------------------------------- assert output table
        List<BatchOutput> batchOutputs = findBatchOutputTable();
        assertThat("Batchletでデータが登録できていること", batchOutputs.size(), is(10));
        for (int i = 0; i < 10; i++) {
            int index = i + 1;
            BatchOutput output = batchOutputs.get(i);
            assertThat(output.id, is(index));
        }

        // -------------------------------------------------- assert output table
        assertThat("2番めのステップで出力するファイルは存在していない", outputFile.exists(), is(false));

//        List<String> messages = InMemoryAppender.getLogMessages("PROGRESS");
//        assertThat(messages, contains(
//                startsWith("INFO progress start job. job name: [multi-step-integration-test]"),
//                startsWith("INFO progress start step. job name: [multi-step-integration-test] step name: [batchlet]"),
//                startsWith("INFO progress job name: [multi-step-integration-test] step name: [batchlet] input count: [10]"),
//                allOf(
//                        startsWith("INFO progress job name: [multi-step-integration-test] step name: [batchlet] total tps:"),
//                        containsString("current tps:"),
//                        containsString("estimated end time:"),
//                        containsString("remaining count: [5]")
//                ),
//                allOf(
//                        startsWith("INFO progress job name: [multi-step-integration-test] step name: [batchlet] total tps:"),
//                        containsString("current tps:"),
//                        containsString("estimated end time:"),
//                        containsString("remaining count: [0]")
//                ),
//                startsWith("INFO progress finish step. job name: [multi-step-integration-test] step name: [batchlet] step status: [null]"),
//                startsWith("INFO progress start step. job name: [multi-step-integration-test] step name: [chunk]"),
//                startsWith("INFO progress job name: [multi-step-integration-test] step name: [chunk] input count: [10]"),
//                startsWith("INFO progress finish step. job name: [multi-step-integration-test] step name: [chunk] step status: [null]"),
//                startsWith("INFO progress finish job. job name: [multi-step-integration-test]")
//        ));

        // -------------------------------------------------- restart job
//        InMemoryAppender.clear();
        FileWriter.outputPath = outputFile;
        final JobExecution restartExecution = resource.restartJob(execution.getExecutionId());
        assertThat("障害原因を取り除いたのでJOBが正常終了する", restartExecution.getBatchStatus(), is(BatchStatus.COMPLETED));

        // -------------------------------------------------- assert output file
        final BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(outputFile),
                Charset.forName("utf-8")));
        try {
            for (int i = 0; i < 10; i++) {
                int index = i + 1;
                assertThat("ファイルに出力されていること", reader.readLine(), is(String.valueOf(index)));
            }
            assertThat("ファイルの終端に達していること", reader.readLine(), is(nullValue()));
        } finally {
            reader.close();
        }

//        messages = InMemoryAppender.getLogMessages("PROGRESS");
//        assertThat(messages, contains(
//                startsWith("INFO progress start job. job name: [multi-step-integration-test]"),
//                startsWith("INFO progress start step. job name: [multi-step-integration-test] step name: [chunk]"),
//                startsWith("INFO progress job name: [multi-step-integration-test] step name: [chunk] input count: [10]"),
//                startsWith("INFO progress chunk progress. write count=[10]"),
//                allOf(
//                        startsWith("INFO progress job name: [multi-step-integration-test] step name: [chunk] total tps:"),
//                        containsString("current tps:"),
//                        containsString("estimated end time:"),
//                        containsString("remaining count: [0]")
//                ),
//                startsWith("INFO progress finish step. job name: [multi-step-integration-test] step name: [chunk] step status: [null]"),
//                startsWith("INFO progress finish job. job name: [multi-step-integration-test]")
//        ));
    }

    /**
     * JOBを多重実行した場合、異常終了すること。
     * <p/>
     * <ul>
     * <li>バッチ処理は異常終了(FAILED)すること</li>
     * <li>バッチステータステーブルの状態は変更されないこと</li>
     * </ul>
     */
    @Test
    public void executeJob_DuplicatedExecuteJob() throws Exception {
        // -------------------------------------------------- clear output table
        clearBatchOutputTable();

        // -------------------------------------------------- update job active
        updateBatchStatus("batchlet-integration-test", "1");

        // -------------------------------------------------- execute batch job
        final JobExecution execution = resource.startJob("batchlet-integration-test");
        assertThat("多重起動なのでJOBは異常終了する", execution.getBatchStatus(), is(BatchStatus.FAILED));
        assertThat("多重起動エラーが設定されていること", execution.getExitStatus(), is("JobAlreadyRunning"));

        // -------------------------------------------------- assert execute batchlet?
        assertThat("Batchletは実行されていないこと", RegisterBatchOutputTable.processExecuteFlag, is(false));

        // -------------------------------------------------- assert output table
        List<BatchOutput> batchOutputs = findBatchOutputTable();
        assertThat("Batchletは実行されないので何も登録されない", batchOutputs.isEmpty(), is(true));

        // -------------------------------------------------- assert batch status
        assertThat("バッチの状態は活性のままであること(0にはならないこと)",
                findBatchStatus("batchlet-integration-test").active, is("1"));

        // -------------------------------------------------- change active status
        updateBatchStatus("batchlet-integration-test", "0");

        // -------------------------------------------------- restart job
        final JobExecution restartExecution = resource.restartJob(execution.getExecutionId());
        assertThat("リスタートには成功する。", restartExecution.getBatchStatus(), is(BatchStatus.COMPLETED));
        assertThat("バッチレットでデータが登録できているはず",
                findBatchOutputTable().size(), is(10));
    }

    /**
     * トランザクション制御の後続リスナーの事前処理で例外が発生した場合、トランザクションはロールバックされること。
     * <p/>
     * <ul>
     * <li>バッチジョブは異常終了すること</li>
     * <li>ロールバックされるのでテーブルはからのまま</li>
     * </ul>
     */
    @Test
    public void executeChunk_ItemWriteListenerErrorOnBeforeWrite() throws Exception {
        // -------------------------------------------------- clear output table
        clearBatchOutputTable();

        // -------------------------------------------------- execute batch job
        final JobExecution execution = resource.startJob("item-write-listener-error-on-before-write-test");
        assertThat("異常終了していること", execution.getBatchStatus(), is(BatchStatus.FAILED));

        // -------------------------------------------------- assert output table
        assertThat("レコードは登録されていないこと", findBatchOutputTable().isEmpty(), is(true));
    }

    /**
     * トランザクション制御の後続リスナーの事後処理で例外が発生した場合、トランザクションはロールバックされること。
     * <p/>
     * <ul>
     * <li>バッチジョブは異常終了すること</li>
     * <li>ロールバックされるのでテーブルはからのまま</li>
     * </ul>
     */
    @Test
    public void executeChunk_ItemWriteListenerErrorOnAfterWrite() throws Exception {
        // -------------------------------------------------- clear output table
        clearBatchOutputTable();

        // -------------------------------------------------- execute batch job
        final JobExecution execution = resource.startJob("item-write-listener-error-on-after-write-test");
        assertThat("異常終了していること", execution.getBatchStatus(), is(BatchStatus.FAILED));

        // -------------------------------------------------- assert output table
        assertThat("レコードは登録されていないこと", findBatchOutputTable().isEmpty(), is(true));
    }

    /**
     * トランザクション制御の後続リスナーの事前処理で例外が発生した場合、トランザクションはロールバックされること。
     * <p/>
     * <ul>
     * <li>バッチジョブは異常終了すること</li>
     * <li>ロールバックされるのでテーブルはからのまま</li>
     * </ul>
     */
    @Test
    public void executeBatch_StepListenerErrorOnBeforeStep() throws Exception {
        // -------------------------------------------------- clear output table
        clearBatchOutputTable();

        // -------------------------------------------------- assert execute batchlet?
        final JobExecution execution = resource.startJob("step-listener-error-on-before-step-test");
        assertThat("異常終了していること", execution.getBatchStatus(), is(BatchStatus.FAILED));

        // -------------------------------------------------- assert output table
        assertThat("レコードは登録されていないこと", findBatchOutputTable().isEmpty(), is(true));
    }

    /**
     * トランザクション制御の後続リスナーの事後処理で例外が発生した場合、トランザクションはロールバックされること。
     * <p/>
     * <ul>
     * <li>バッチジョブは異常終了すること</li>
     * <li>ロールバックされるのでテーブルはからのまま</li>
     * </ul>
     */
    @Test
    public void executeBatch_StepListenerErrorOnAfterStep() throws Exception {
        // -------------------------------------------------- clear output table
        clearBatchOutputTable();

        // -------------------------------------------------- assert execute batchlet?
        final JobExecution execution = resource.startJob("step-listener-error-on-after-step-test");
        assertThat("異常終了していること", execution.getBatchStatus(), is(BatchStatus.FAILED));

        // -------------------------------------------------- assert output table
        assertThat("レコードは登録されていないこと", findBatchOutputTable().isEmpty(), is(true));
    }

    /**
     * リスタートした場合に、ロールバックされたポイントから再開されること。
     * <p/>
     * <ul>
     * <li>最初はバッチジョブは異常終了すること</li>
     * <li>ロールバックされるのでテーブルはからのまま</li>
     * <li>リスタート後は登録されること。</li>
     * </ul>
     */
    @Test
    public void executeChunk_ItemWriteListenerErrorOnAfterWrite_Restart() throws Exception {

        // -------------------------------------------------- clear output table
        clearBatchOutputTable();

        // -------------------------------------------------- execute batch job
        final JobExecution prevExecution = resource.startJob("item-write-listener-error-on-after-write-test");
        assertThat("異常終了していること", prevExecution.getBatchStatus(), is(BatchStatus.FAILED));

        // -------------------------------------------------- assert output table
        assertThat("レコードは登録されていないこと", findBatchOutputTable().isEmpty(), is(true));

        // 正常なリストに入れ替えます。
        final List<?> listeners = SystemRepository.get("itemWriteListeners");
        SystemRepository.load(new ObjectLoader() {
            @Override
            public Map<String, Object> load() {
                return new HashMap<String, Object>() {{
                    put("item-write-listener-error-on-after-write-test.itemWriteListeners", listeners);
                }};
            }
        });

        // -------------------------------------------------- execute batch job
        final JobExecution execution = resource.restartJob("item-write-listener-error-on-after-write-test",
                prevExecution.getExecutionId());
        assertThat("バッチ処理が正常に終わっていること", execution.getBatchStatus(), is(BatchStatus.COMPLETED));

        // -------------------------------------------------- assert batch output table
        List<BatchOutput> batchOutputs = findBatchOutputTable();
        assertThat("15レコード登録されていること", batchOutputs.size(), is(15));

        for (int i = 0; i < 15; i++) {
            int index = i + 1;
            BatchOutput output = batchOutputs.get(i);
            assertThat(output.id, is(index));
            assertThat(output.name, is("name_" + index));
        }
    }

    /**
     * 正常終了するバッチジョブの進捗ログが出力されること。
     */
    @Test
    public void testProgressLog_Success() throws Exception {

        // -------------------------------------------------- clear output table
        clearBatchOutputTable();

        // -------------------------------------------------- execute batch job
        resource.startJob("batchlet-integration-test");

        // -------------------------------------------------- assert log
//        assertThat(InMemoryAppender.getLogMessages("PROGRESS"), contains(
//                startsWith("INFO progress start job. job name: [batchlet-integration-test]"),
//                startsWith("INFO progress start step. job name: [batchlet-integration-test] step name: [step1]"),
//                startsWith("INFO progress job name: [batchlet-integration-test] step name: [step1] input count: [10]"),
//                allOf(
//                        startsWith("INFO progress job name: [batchlet-integration-test] step name: [step1] total tps:"),
//                        containsString("current tps:"),
//                        containsString("estimated end time:"),
//                        containsString("remaining count: [5]")
//                ),
//                allOf(
//                        startsWith("INFO progress job name: [batchlet-integration-test] step name: [step1] total tps:"),
//                        containsString("current tps:"),
//                        containsString("estimated end time:"),
//                        containsString("remaining count: [0]")
//                ),
//                startsWith("INFO progress finish step. job name: [batchlet-integration-test] step name: [step1] step status: [null]"),
//                startsWith("INFO progress finish job. job name: [batchlet-integration-test]")));
    }

    /**
     * 異常終了するバッチジョブの進捗ログが出力されること。
     */
    @Test
    public void testProgressLog_Failed() throws Exception {

        // -------------------------------------------------- clear output table
        clearBatchOutputTable();

        // -------------------------------------------------- add initial data
        insertBatchOutputTable(9);

        // -------------------------------------------------- execute batch job
        resource.startJob("batchlet-integration-test");

        // -------------------------------------------------- assert log
//        assertThat(InMemoryAppender.getLogMessages("PROGRESS"), contains(
//                startsWith("INFO progress start job. job name: [batchlet-integration-test]"),
//                startsWith("INFO progress start step. job name: [batchlet-integration-test] step name: [step1]"),
//                startsWith("INFO progress job name: [batchlet-integration-test] step name: [step1] input count: [10]"),
//                allOf(
//                        startsWith("INFO progress job name: [batchlet-integration-test] step name: [step1] total tps:"),
//                        containsString("current tps:"),
//                        containsString("estimated end time:"),
//                        containsString("remaining count: [5]")
//                ),
//                startsWith("INFO progress finish step. job name: [batchlet-integration-test] step name: [step1] step status: [null]"),
//                startsWith("INFO progress finish job. job name: [batchlet-integration-test]")));
    }

    /**
     * 正常終了するマルチステップの進捗ログが出力されること。
     */
    @Test
    public void testProgressLogMultiStep_Success() throws Exception {
        final File outputFile = folder.newFile("outputFile.txt");
        FileWriter.outputPath = outputFile;

        // -------------------------------------------------- setup output table
        clearBatchOutputTable();

        // -------------------------------------------------- execute batch job
        resource.startJob("multi-step-integration-with-job-listener-test");

        // -------------------------------------------------- assert log
//        assertThat(InMemoryAppender.getLogMessages("PROGRESS"), contains(
//                startsWith("INFO progress start job. job name: [multi-step-integration-with-job-listener-test]"),
//                startsWith("INFO progress start step. job name: [multi-step-integration-with-job-listener-test] step name: [batchlet]"),
//                startsWith("INFO progress job name: [multi-step-integration-with-job-listener-test] step name: [batchlet] input count: [10]"),
//                allOf(
//                        startsWith("INFO progress job name: [multi-step-integration-with-job-listener-test] step name: [batchlet] total tps:"),
//                        containsString("current tps:"),
//                        containsString("estimated end time:"),
//                        containsString("remaining count: [5]")
//                ),
//                allOf(
//                        startsWith("INFO progress job name: [multi-step-integration-with-job-listener-test] step name: [batchlet] total tps:"),
//                        containsString("current tps:"),
//                        containsString("estimated end time:"),
//                        containsString("remaining count: [0]")
//                ),
//                startsWith("INFO progress finish step. job name: [multi-step-integration-with-job-listener-test] step name: [batchlet] step status: [null]"),
//                startsWith("INFO progress start step. job name: [multi-step-integration-with-job-listener-test] step name: [chunk]"),
//                startsWith("INFO progress job name: [multi-step-integration-with-job-listener-test] step name: [chunk] input count: [10]"),
//                startsWith("INFO progress chunk progress. write count=[10]"),
//                allOf(
//                        startsWith("INFO progress job name: [multi-step-integration-with-job-listener-test] step name: [chunk] total tps:"),
//                        containsString("current tps:"),
//                        containsString("estimated end time:"),
//                        containsString("remaining count: [0]")
//                ),
//                startsWith("INFO progress finish step. job name: [multi-step-integration-with-job-listener-test] step name: [chunk] step status: [null]"),
//                startsWith("INFO progress finish job. job name: [multi-step-integration-with-job-listener-test]")));
    }

    /**
     * 一度異常終了し、再起動時に正常終了するマルチステップの進捗ログが出力されること。
     */
    @Test
    public void testProgressLogMultiStep_Failed() throws Exception {
        final File outputFile = new File(folder.getRoot(), "outputFile.txt");
        FileWriter.outputPath = null;

        // -------------------------------------------------- setup output table
        clearBatchOutputTable();

        // -------------------------------------------------- execute batch job
        final JobExecution execution = resource.startJob("multi-step-integration-with-job-listener-test");

        // -------------------------------------------------- assert log (1)
//        assertThat(InMemoryAppender.getLogMessages("PROGRESS"), contains(
//                startsWith("INFO progress start job. job name: [multi-step-integration-with-job-listener-test]"),
//                startsWith("INFO progress start step. job name: [multi-step-integration-with-job-listener-test] step name: [batchlet]"),
//                startsWith("INFO progress job name: [multi-step-integration-with-job-listener-test] step name: [batchlet] input count: [10]"),
//                allOf(
//                        startsWith("INFO progress job name: [multi-step-integration-with-job-listener-test] step name: [batchlet] total tps:"),
//                        containsString("current tps:"),
//                        containsString("estimated end time:"),
//                        containsString("remaining count: [5]")
//                ),
//                allOf(
//                        startsWith("INFO progress job name: [multi-step-integration-with-job-listener-test] step name: [batchlet] total tps:"),
//                        containsString("current tps:"),
//                        containsString("estimated end time:"),
//                        containsString("remaining count: [0]")
//                ),
//                startsWith("INFO progress finish step. job name: [multi-step-integration-with-job-listener-test] step name: [batchlet] step status: [null]"),
//                startsWith("INFO progress start step. job name: [multi-step-integration-with-job-listener-test] step name: [chunk]"),
//                startsWith("INFO progress job name: [multi-step-integration-with-job-listener-test] step name: [chunk] input count: [10]"),
//                startsWith("INFO progress finish step. job name: [multi-step-integration-with-job-listener-test] step name: [chunk] step status: [null]"),
//                startsWith("INFO progress finish job. job name: [multi-step-integration-with-job-listener-test]")
//        ));
//        InMemoryAppender.clear();

        // -------------------------------------------------- restart batch job
        FileWriter.outputPath = outputFile;
        resource.restartJob(execution.getExecutionId());

        // -------------------------------------------------- assert log (2)
//        assertThat(InMemoryAppender.getLogMessages("PROGRESS"), contains(
//                startsWith("INFO progress start job. job name: [multi-step-integration-with-job-listener-test]"),
//                startsWith("INFO progress start step. job name: [multi-step-integration-with-job-listener-test] step name: [chunk]"),
//                startsWith("INFO progress job name: [multi-step-integration-with-job-listener-test] step name: [chunk] input count: [10]"),
//                startsWith("INFO progress chunk progress. write count=[10]"),
//                allOf(
//                        startsWith("INFO progress job name: [multi-step-integration-with-job-listener-test] step name: [chunk] total tps:"),
//                        containsString("current tps:"),
//                        containsString("estimated end time:"),
//                        containsString("remaining count: [0]")
//                ),
//                startsWith("INFO progress finish step. job name: [multi-step-integration-with-job-listener-test] step name: [chunk] step status: [null]"),
//                startsWith("INFO progress finish job. job name: [multi-step-integration-with-job-listener-test]")));
    }

    /**
     * chunk実行時に進捗ログが出力されること。
     */
    @Test
    public void testProgressLogChunk() throws Exception {

        // -------------------------------------------------- clear output table
        clearBatchOutputTable();

        // -------------------------------------------------- execute batch job
        resource.startJob("chunk-integration-test");

        // -------------------------------------------------- assert log
//        assertThat(InMemoryAppender.getLogMessages("PROGRESS"), contains(
//                startsWith("INFO progress start job. job name: [chunk-integration-test]"),
//                startsWith("INFO progress start step. job name: [chunk-integration-test] step name: [myStep]"),
//                startsWith("INFO progress job name: [chunk-integration-test] step name: [myStep] input count: [25]"),
//                startsWith("INFO progress chunk progress. write count=[10]"),
//                allOf(
//                        startsWith("INFO progress job name: [chunk-integration-test] step name: [myStep] total tps:"),
//                        containsString("current tps:"),
//                        containsString("estimated end time:"),
//                        containsString("remaining count: [15]")
//                ),
//                startsWith("INFO progress chunk progress. write count=[20]"),
//                allOf(
//                        startsWith("INFO progress job name: [chunk-integration-test] step name: [myStep] total tps:"),
//                        containsString("current tps:"),
//                        containsString("estimated end time:"),
//                        containsString("remaining count: [5]")
//                ),
//                startsWith("INFO progress chunk progress. write count=[25]"),
//                allOf(
//                        startsWith("INFO progress job name: [chunk-integration-test] step name: [myStep] total tps:"),
//                        containsString("current tps:"),
//                        containsString("estimated end time:"),
//                        containsString("remaining count: [0]")
//                ),
//                startsWith("INFO progress finish step. job name: [chunk-integration-test] step name: [myStep] step status: [null]"),
//                startsWith("INFO progress finish job. job name: [chunk-integration-test]")
//        ));
    }

    /**
     * batchlet実行時に運用通知ログが出力されること。
     */
    @Test
    public void testOperationLogBatchlet() throws Exception {

        // -------------------------------------------------- clear output table
        clearBatchOutputTable();

        // -------------------------------------------------- execute batch job
        JobExecution execution = resource.startJob("operator-batchlet-test");

        // -------------------------------------------------- assert log
//        assertThat("運用者向けのメッセージのみ出力されていること",
//                InMemoryAppender.getLogMessages("OPERATION"), contains(allOf(
//                        startsWith("ERROR operator ファイルの読み込みに失敗しました。ファイルが存在するか確認してください。"),
//                        not(containsString("FileNotFoundException")))));
//
//        assertThat("運用者向けのメッセージと合わせてスタックトレースも出力されていること",
//                InMemoryAppender.getLogMessages("ALL"), Matchers.<String>hasItem(allOf(
//                        startsWith("ERROR operator ファイルの読み込みに失敗しました。ファイルが存在するか確認してください。"),
//                        containsString("FileNotFoundException"))));

        // -------------------------------------------------- assert batch status
        assertThat(execution.getBatchStatus(), is(BatchStatus.FAILED));
    }

    /**
     * chunk実行時に運用通知ログが出力されること。
     */
    @Test
    public void testOperationLogChunk() throws Exception {

        // -------------------------------------------------- clear output table
        clearBatchOutputTable();

        // -------------------------------------------------- execute batch job
        JobExecution execution = resource.startJob("operator-chunk-test");

        // -------------------------------------------------- assert log
//        assertThat("運用者向けのメッセージのみ出力されていること",
//                InMemoryAppender.getLogMessages("OPERATION"), contains(allOf(
//                        startsWith("ERROR operator ファイルの書き込みに失敗しました。他のプロセスによってファイルがロックされていないか確認してください。"),
//                        not(containsString("IllegalStateException")))));
//
//        assertThat("運用者向けのメッセージと合わせてスタックトレースも出力されていること",
//                InMemoryAppender.getLogMessages("ALL"), Matchers.<String>hasItem(allOf(
//                        startsWith("ERROR operator ファイルの書き込みに失敗しました。他のプロセスによってファイルがロックされていないか確認してください。"),
//                        containsString("IllegalStateException"))));

        // -------------------------------------------------- assert batch status
        assertThat(execution.getBatchStatus(), is(BatchStatus.FAILED));
    }

    /**
     * 予期せぬ例外が発生した場合は運用者向けにログが出力されないこと
     */
    @Test
    public void testSystemError() throws Exception {

        // -------------------------------------------------- clear output table
        clearBatchOutputTable();

        // -------------------------------------------------- execute batch job
        JobExecution execution = resource.startJob("batchlet-error-integration-test");

        // -------------------------------------------------- assert log
//        assertThat("運用者向けのメッセージは出力されないこと",
//                InMemoryAppender.getLogMessages("OPERATION"), is(nullValue()));
//
//        assertThat("スタックトレースが出力されていること",
//                InMemoryAppender.getLogMessages("ALL"),
//                Matchers.<String>hasItem(containsString("StackOverflowError")));

        // -------------------------------------------------- assert batch status
        assertThat(execution.getBatchStatus(), is(BatchStatus.FAILED));
    }

    /**
     * ステップ単位で値を持ち回ることができること
     *
     * @throws Exception
     */
    @Test
    public void testStepScoped() throws Exception {
        // -------------------------------------------------- execute batch job
        final JobExecution execution = resource.startJob("step-scoped-integration-test");
        assertThat(execution.getBatchStatus(), is(BatchStatus.COMPLETED));
    }

    /**
     * アプリ側で{@link jakarta.batch.runtime.context.StepContext#setTransientUserData(Object)}を使用された場合に例外を送出すること
     *
     * @throws Exception
     */
    @Test
    public void testStepScoped_useTransientUserData() throws Exception {
        // -------------------------------------------------- execute batch job
        final JobExecution execution = resource.startJob("step-scoped-error-integration-test");
        assertThat(execution.getBatchStatus(), is(BatchStatus.FAILED));

        // -------------------------------------------------- assert log
//        assertThat(InMemoryAppender.getLogMessages("ALL"), Matchers.<String>hasItem(allOf(
//                containsString("jakarta.batch.operations.BatchRuntimeException"),
//                containsString("TransientUserData of StepContext must be StepScopedHolder type."))));

    }

    /**
     * Ignore している理由。
     * <p>
     * クラスの Javadoc にも記載した通り、本テストは Jakarta EE 10 対応の際に
     * アプリケーションサーバーを GlassFish から WildFly に変更している。
     * その際、なぜか Jakarta RESTful Web Services (以下、Jakarta REST)の
     * デプロイが上手くいかなくなり、本テストが正常に動作しなくなった
     * (レスポンスが 404 Not Found になる)。
     *
     * WildFly の Embedded だと Jakarta REST が動作しないということはなく、
     * 別途簡易的に作成したテストコードでは Jakarta REST のデプロイができる
     * ことが確認できている。
     * しかし、なぜ本テスト内で 404 になってしまうのかは、調査しても原因が
     * 特定できなかった。
     *
     * 本テストは、テスト内容的に通さないと品質の担保ができなくなるような性質の
     * モノではないため、一旦本テストは除外することにした。
     *
     * 将来、 EE 10 対応した GlassFish の組み込み版が Arquillian で利用できるように
     * なった際は、 Ignore を外してテストが通せるか検証すること。
     * </p>
     * @param deploymentUri デプロイされたアプリケーションの URI
     * @throws Exception 例外が発生した場合
     */
    @Ignore
    @Test
    @RunAsClient
    public void RESTアクセスではStepContextが存在しないためStepScopedなBeanは利用できないこと(
            @ArquillianResource URI deploymentUri) throws Exception {
        System.out.println("deploymentUri=" + deploymentUri);
        final WebTarget path = ClientBuilder.newClient()
                .target(deploymentUri)
                .path("/api/hello");
        System.out.println("path.getUri()=" + path.getUri());

        final Response response = ClientBuilder.newClient()
                                               .target(deploymentUri)
                                               .path("/api/hello")
                                               .request()
                                               .get();

        System.out.println(response.readEntity(String.class));

        assertThat(response.getStatus(), is(500));
        assertThat(response.readEntity(String.class),
                containsString("No active contexts for scope type nablarch.fw.batch.ee.cdi.StepScoped"));
    }

    /**
     * MainでBuchletを実行時に、バッチステータスがCOMPLETEDの場合に、戻り値0を返すこと。
     */
    @Test
    public void testJobExecutorSuccessBatchlet() throws Exception {
        final JobExecutor executor = new JobExecutor("success-batchlet-test", new Properties());
        int exitCode = executor.execute();
        assertThat(exitCode, is(0));
        assertThat(executor.getJobExecution().getExitStatus(), not("WARNING"));
        assertThat(executor.getJobExecution().getBatchStatus(), is(BatchStatus.COMPLETED));
    }

    /**
     * MainでBuchletを実行時に、バッチステータスがCOMPLETEDの場合に、戻り値0を返すこと。
     */
    @Test
    public void testJobExecutorSuccessChunk() throws Exception {
        final JobExecutor executor = new JobExecutor("success-chunk-test", new Properties());
        int exitCode = executor.execute();
        assertThat(exitCode, is(0));
        assertThat(executor.getJobExecution().getExitStatus(), not("WARNING"));
        assertThat(executor.getJobExecution().getBatchStatus(), is(BatchStatus.COMPLETED));
    }

    /**
     * MainでBuchletを実行時に、終了ステータスがWARNINGの場合に、戻り値2を返すこと。
     */
    @Test
    public void testJobExecutorWarningBatchlet() throws Exception {
        final JobExecutor executor = new JobExecutor("warning-batchlet-test", new Properties());
        int exitCode = executor.execute();
        assertThat(exitCode, is(2));
        assertThat(executor.getJobExecution().getExitStatus(), is("WARNING"));
        assertThat(executor.getJobExecution().getBatchStatus(), is(BatchStatus.COMPLETED));
    }

    /**
     * MainでBuchletを実行時に、例外をスローしても終了ステータスがWARNINGの場合、戻り値2を返すこと。
     */
    @Test
    public void testJobExecutorWarningBatchletThrowError() throws Exception {
        final JobExecutor executor = new JobExecutor("warning-batchlet-throw-error-test", new Properties());
        int exitCode = executor.execute();
        assertThat(exitCode, is(2));
        assertThat(executor.getJobExecution().getExitStatus(), is("WARNING"));
        assertThat(executor.getJobExecution().getBatchStatus(), is(BatchStatus.FAILED));
    }

    /**
     * MainでChunkを実行時に、終了ステータスがWARNINGの場合に、戻り値2を返すこと。
     */
    @Test
    public void testJobExecutorWarningChunk() throws Exception {
        final JobExecutor executor = new JobExecutor("warning-chunk-test", new Properties());
        int exitCode = executor.execute();
        assertThat(exitCode, is(2));
        assertThat(executor.getJobExecution().getExitStatus(), is("WARNING"));
        assertThat(executor.getJobExecution().getBatchStatus(), is(BatchStatus.COMPLETED));
    }


    /**
     * MainでChunkを実行時に、例外をスローしても終了ステータスがWARNINGの場合、戻り値2を返すこと。
     */
    @Test
    public void testJobExecutorWarningChunkThrowError() throws Exception {
        final JobExecutor executor = new JobExecutor("warning-chunk-throw-error-test", new Properties());
        int exitCode = executor.execute();
        assertThat(exitCode, is(2));
        assertThat(executor.getJobExecution().getExitStatus(), is("WARNING"));
        assertThat(executor.getJobExecution().getBatchStatus(), is(BatchStatus.FAILED));
    }

    /**
     * MainでBuchletを実行時に、例外がスローされバッチステータスがFAILEDの場合、戻り値1を返すこと。
     */
    @Test
    public void testJobExecutorFailureBatchletThrowError() throws Exception {
        final JobExecutor executor = new JobExecutor("failure-batchlet-throw-error-test", new Properties());
        int exitCode = executor.execute();
        assertThat(exitCode, is(1));
        assertThat(executor.getJobExecution().getExitStatus(), not("WARNING"));
        assertThat(executor.getJobExecution().getBatchStatus(), is(BatchStatus.FAILED));
    }

    /**
     * MainでChunkを実行時に、例外がスローされバッチステータスがFAILEDの場合、戻り値1を返すこと。
     */
    @Test
    public void testJobExecutorFailureChunkThrowError() throws Exception {
        final JobExecutor executor = new JobExecutor("failure-chunk-throw-error-test", new Properties());
        int exitCode = executor.execute();
        assertThat(exitCode, is(1));
        assertThat(executor.getJobExecution().getExitStatus(), not("WARNING"));
        assertThat(executor.getJobExecution().getBatchStatus(), is(BatchStatus.FAILED));
    }

    /**
     * ステップ単位でリスナーを設定できること。
     */
    @Test
    public void executeStepLevelListener() throws Exception {
        final JobExecution execution = resource.startJob("specified-steplevel-listener-test");

//        assertThat("LoggingStepLevelListenerの実行結果がログ出力されていること",
//                InMemoryAppender.getLogMessages("ALL"), Matchers.<String>hasItem(allOf(
//                        containsString("LoggingStepLevelListener is executed on step1"))));
    }

    /**
     * ジョブ単位でリスナーを設定できること。
     */
    @Test
    public void executeJobLevelListener() throws Exception {
        final JobExecution execution = resource.startJob("specified-joblevel-listener-test");

//        assertThat("LoggingStepLevelListenerの実行結果がログ出力されていること",
//                InMemoryAppender.getLogMessages("ALL"), Matchers.<String>hasItem(allOf(
//                        containsString("LoggingStepLevelListener is executed on step2"))));
    }

    private static void clearBatchOutputTable() {
        VariousDbTestHelper.delete(BatchOutput.class);
    }

    private static List<BatchOutput> findBatchOutputTable() {
        return VariousDbTestHelper.findAll(BatchOutput.class, "id");
    }

    private static void insertBatchOutputTable(int id) {
        VariousDbTestHelper.insert(new BatchOutput(id, "data_" + id));
    }

    private static void deleteBatchOutputTable(int id) {
        VariousDbTestHelper.delete(
                VariousDbTestHelper.findById(BatchOutput.class, id)
        );
    }

    private void updateBatchStatus(String jobName, String activeFlag){
        VariousDbTestHelper.update(new nablarch.fw.batch.ee.integration.BatchStatus(jobName, activeFlag));
    }

    private static nablarch.fw.batch.ee.integration.BatchStatus findBatchStatus(String jobName) {
        return VariousDbTestHelper.findById(
                nablarch.fw.batch.ee.integration.BatchStatus.class, jobName);
    }
}

