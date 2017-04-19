package nablarch.fw.batch.ee.integration;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.logging.LogManager;

import javax.batch.runtime.BatchStatus;
import javax.batch.runtime.JobExecution;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Response;

import org.hamcrest.Matchers;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.ArchivePath;
import org.jboss.shrinkwrap.api.Filter;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.slf4j.bridge.SLF4JBridgeHandler;

import nablarch.core.db.statement.SqlResultSet;
import nablarch.core.db.statement.SqlRow;
import nablarch.core.repository.ObjectLoader;
import nablarch.core.repository.SystemRepository;
import nablarch.fw.batch.ee.JobExecutor;
import nablarch.fw.batch.ee.initializer.RepositoryInitializer;
import nablarch.fw.batch.ee.integration.app.FileWriter;
import nablarch.fw.batch.ee.integration.app.RegisterBatchOutputTable;
import nablarch.fw.batch.ee.integration.app.ThrowErrorWriter;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;

import mockit.Deencapsulation;

/**
 * Java Batchの結合テスト。
 *
 * @author Hisaaki Shioiri
 */
@RunWith(Arquillian.class)
public class BatchIntegrationTest {

    @Rule
    public IntegrationTestResource resource = new IntegrationTestResource();

    @Deployment
    public static WebArchive createDeployment() {
        final WebArchive archive = ShrinkWrap.create(WebArchive.class, "batch.war")
                .addPackages(true, new Filter<ArchivePath>() {
                    @Override
                    public boolean include(ArchivePath object) {
                        final String s = object.get();
                        return !s.contains("Test") && !s.contains("test");
                    }
                }, "nablarch");
        return archive;
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
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

    }

    @Before
    public void setUp() throws Exception {
        Deencapsulation.setField(RepositoryInitializer.class, "isInitialized", false);
        RegisterBatchOutputTable.processExecuteFlag = false;
        ThrowErrorWriter.skipIds = new int[0];
        ThrowErrorWriter.errorId = -1;
        ThrowErrorWriter.retryError = Collections.emptySet();
        InMemoryAppender.clear();
    }

    @After
    public void tearDown() throws Exception {
        Deencapsulation.setField(RepositoryInitializer.class, "isInitialized", false);
    }

    /**
     * {@link javax.batch.api.Batchlet}実装クラスで行った登録処理が正常に完了していること。
     * <p/>
     * <ul>
     * <li>バッチ処理は正常に終了していること</li>
     * <li>Batchletで登録した10レコードが参照できること(トランザクションがコミットされていること)</li>
     * </ul>
     */
    @Test
    public void executeBatchlet_Success() throws Exception {

        // -------------------------------------------------- clear output table
        resource.clearBatchOutputTable();

        // -------------------------------------------------- execute batch job
        final JobExecution execution = resource.startJob("batchlet-integration-test");
        assertThat("バッチ処理が正常に終わっていること", execution.getBatchStatus(), is(BatchStatus.COMPLETED));

        // -------------------------------------------------- assert output table
        final SqlResultSet rs = resource.findBatchOutputTable();
        assertThat("10レコード登録されるはず", rs.size(), is(10));
        for (int i = 0; i < 10; i++) {
            int index = i + 1;
            final SqlRow row = rs.get(i);
            assertThat(row.getInteger("id"), is(index));
            assertThat(row.getString("name"), is("name_" + index));
        }

        // -------------------------------------------------- assert batch status
        assertThat("バッチが終了したので非アクティブになっていること",
                resource.findBatchStatus("batchlet-integration-test"), is("0"));
    }

    /**
     * {@link javax.batch.api.Batchlet}実装クラスで例外が発生した場合、DBへの更新がロールバックされること。
     * <p/>
     * <ul>
     * <li>バッチ処理は異常終了(FAILED)していること</li>
     * <li>Batchletの処理はロールバックされるので元の1レコードのみが存在していること</li>
     * </ul>
     */
    @Test
    public void executeBatchlet_Failed() throws Exception {

        // -------------------------------------------------- clear output table
        resource.clearBatchOutputTable();

        // -------------------------------------------------- add initial data
        resource.insertBatchOutputTable(9);

        // -------------------------------------------------- execute batch job
        final JobExecution execution = resource.startJob("batchlet-integration-test");
        assertThat("一意成約違反が発生してバッチJOBは異常終了する", execution.getBatchStatus(), is(BatchStatus.FAILED));

        // -------------------------------------------------- assert execute batchlet?
        assertThat("Batchletは実行されていること", RegisterBatchOutputTable.processExecuteFlag, is(true));

        // -------------------------------------------------- assert output table
        final SqlResultSet rs = resource.findBatchOutputTable();
        assertThat(rs.size(), is(1));
        assertThat("初期データが存在する。", rs.get(0)
                .getInteger("id"), is(9));

        // -------------------------------------------------- assert batch status
        assertThat("異常終了でもステータスは非アクティブに変更されること",
                resource.findBatchStatus("batchlet-integration-test"), is("0"));
    }

    /**
     * {@link javax.batch.api.Batchlet}実装クラスで{@link Error}系の例外が発生した場合、DBへの更新がロールバックされること。
     * <p/>
     * <ul>
     * <li>バッチ処理は、異常終了していること</li>
     * <li>Batchletの処理はロールバックされるのでテーブルはからのまま</li>
     * </ul>
     */
    @Test
    public void executeBatch_throwError() throws Exception {
        // -------------------------------------------------- clear output table
        resource.clearBatchOutputTable();

        // -------------------------------------------------- assert execute batchlet?
        final JobExecution execution = resource.startJob("batchlet-error-integration-test");
        assertThat("異常終了していること", execution.getBatchStatus(), is(BatchStatus.FAILED));

        // -------------------------------------------------- assert output table
        assertThat("レコードは登録されていないこと", resource.findBatchOutputTable()
                .isEmpty(), is(true));

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
        resource.clearBatchOutputTable();

        // -------------------------------------------------- execute batch job
        final JobExecution execution = resource.startJob("chunk-integration-test");
        assertThat("バッチ処理が正常に終わっていること", execution.getBatchStatus(), is(BatchStatus.COMPLETED));

        // -------------------------------------------------- assert batch output table
        final SqlResultSet rs = resource.findBatchOutputTable();
        assertThat("25レコード登録されていること", rs.size(), is(25));

        for (int i = 0; i < 25; i++) {
            int index = i + 1;
            final SqlRow row = rs.get(i);
            assertThat(row.getInteger("id"), is(index));
            assertThat(row.getString("name"), is("name_" + index));
        }

        List<String> messages = InMemoryAppender.getLogMessages("PROGRESS");
        assertThat(messages, contains(
                startsWith("INFO progress start job. job name: [chunk-integration-test]"),
                startsWith("INFO progress start step. job name: [chunk-integration-test] step name: [myStep]"),
                startsWith("INFO progress job name: [chunk-integration-test] step name: [myStep] input count: [25]"),
                startsWith("INFO progress chunk progress. write count=[10]"),
                allOf(
                        startsWith("INFO progress job name: [chunk-integration-test] step name: [myStep] tps:"),
                        containsString("estimated end time:"),
                        containsString("remaining count: [15]")
                ),
                startsWith("INFO progress chunk progress. write count=[20]"),
                allOf(
                        startsWith("INFO progress job name: [chunk-integration-test] step name: [myStep] tps:"),
                        containsString("estimated end time:"),
                        containsString("remaining count: [5]")
                ),
                startsWith("INFO progress chunk progress. write count=[25]"),
                allOf(
                        startsWith("INFO progress job name: [chunk-integration-test] step name: [myStep] tps:"),
                        containsString("estimated end time:"),
                        containsString("remaining count: [0]")
                ),
                startsWith("INFO progress finish step. job name: [chunk-integration-test] step name: [myStep] step status: [null]"),
                startsWith("INFO progress finish job. job name: [chunk-integration-test]")
        ));
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
        resource.clearBatchOutputTable();
        resource.insertBatchOutputTable(18);

        // -------------------------------------------------- execute batch job
        final JobExecution execution = resource.startJob("chunk-integration-test");
        assertThat("バッチ処理が異常終了していること", execution.getBatchStatus(), is(BatchStatus.FAILED));

        // -------------------------------------------------- assert output table
        final SqlResultSet rs = resource.findBatchOutputTable();
        assertThat("障害発生原因のレコード + 登録した10レコードが登録されているはず", rs.size(), is(11));

        List<String> messages = InMemoryAppender.getLogMessages("PROGRESS");
        assertThat(messages, contains(
                startsWith("INFO progress start job. job name: [chunk-integration-test]"),
                startsWith("INFO progress start step. job name: [chunk-integration-test] step name: [myStep]"),
                startsWith("INFO progress job name: [chunk-integration-test] step name: [myStep] input count: [25]"),
                startsWith("INFO progress chunk progress. write count=[10]"),
                allOf(
                        startsWith("INFO progress job name: [chunk-integration-test] step name: [myStep] tps:"),
                        containsString("estimated end time:"),
                        containsString("remaining count: [15]")
                ),
                startsWith("INFO progress finish step. job name: [chunk-integration-test] step name: [myStep] step status: [null]"),
                startsWith("INFO progress finish job. job name: [chunk-integration-test]")
        ));

        // -------------------------------------------------- clear abnormal data
        resource.deleteBatchOutputTable(18);
        InMemoryAppender.clear();

        // -------------------------------------------------- restart batch job
        final JobExecution restartExecution = resource.restartJob(execution.getExecutionId());
        assertThat("再開後はJOBが正常に終了すること", restartExecution.getBatchStatus(), is(BatchStatus.COMPLETED));

        // -------------------------------------------------- assert output table
        final SqlResultSet restartResult = resource.findBatchOutputTable();
        assertThat("処理対象の25レコード全てが登録されていること", restartResult.size(), is(25));
        for (int i = 0; i < 25; i++) {
            int index = i + 1;
            final SqlRow row = restartResult.get(i);
            assertThat(row.getInteger("id"), is(index));
            assertThat(row.getString("name"), is("name_" + index));
        }

        messages = InMemoryAppender.getLogMessages("PROGRESS");
        assertThat(messages, contains(
                startsWith("INFO progress start job. job name: [chunk-integration-test]"),
                startsWith("INFO progress start step. job name: [chunk-integration-test] step name: [myStep]"),
                startsWith("INFO progress job name: [chunk-integration-test] step name: [myStep] input count: [15]"),
                startsWith("INFO progress chunk progress. write count=[10]"),
                allOf(
                        startsWith("INFO progress job name: [chunk-integration-test] step name: [myStep] tps:"),
                        containsString("estimated end time:"),
                        containsString("remaining count: [5]")
                ),
                startsWith("INFO progress chunk progress. write count=[15]"),
                allOf(
                        startsWith("INFO progress job name: [chunk-integration-test] step name: [myStep] tps:"),
                        containsString("estimated end time:"),
                        containsString("remaining count: [0]")
                ),
                startsWith("INFO progress finish step. job name: [chunk-integration-test] step name: [myStep] step status: [null]"),
                startsWith("INFO progress finish job. job name: [chunk-integration-test]")
        ));


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
        resource.clearBatchOutputTable();

        // -------------------------------------------------- set error id
        ThrowErrorWriter.errorId = 25;

        // -------------------------------------------------- execute batch job
        final JobExecution execution = resource.startJob("chunk-error-integration-test");
        assertThat("バッチ処理が異常終了すること", execution.getBatchStatus(), is(BatchStatus.FAILED));

        // -------------------------------------------------- assert output table
        final SqlResultSet rs = resource.findBatchOutputTable();
        assertThat("2つのChunkでスキップ対象例外が発生するので、最後のChunkのデータだけ登録される", rs.size(), is(20));
        assertThat("最初のレコードのID:1", rs.get(0)
                .getInteger("id"), is(1));
        assertThat("最後のレコードのID:20", rs.get(19)
                .getInteger("id"), is(20));
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
        resource.clearBatchOutputTable();

        // -------------------------------------------------- set error id
        ThrowErrorWriter.skipIds = new int[] {10, 19};

        // -------------------------------------------------- execute batch job
        final JobExecution execution = resource.startJob("chunk-error-integration-test");
        assertThat("バッチ処理は正常に終了すること", execution.getBatchStatus(), is(BatchStatus.COMPLETED));

        // -------------------------------------------------- assert output table
        final SqlResultSet rs = resource.findBatchOutputTable();
        assertThat("2つのChunkでスキップ対象例外が発生するので、最後のChunkのデータだけ登録される", rs.size(), is(29));
        assertThat("最初のレコードのIDは21", rs.get(0)
                .getInteger("id"), is(21));
        assertThat("最後のレコードのIDは49", rs.get(28)
                .getInteger("id"), is(49));
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
        resource.clearBatchOutputTable();

        // -------------------------------------------------- set error id
        ThrowErrorWriter.skipIds = new int[] {10, 19, 39};

        // -------------------------------------------------- execute batch job
        final JobExecution execution = resource.startJob("chunk-error-integration-test");
        assertThat("スキップのリミット突破でJOBは異常終了する ", execution.getBatchStatus(), is(BatchStatus.FAILED));

        // -------------------------------------------------- assert output table
        final SqlResultSet rs = resource.findBatchOutputTable();
        assertThat("正常に処理できたChunkのデータのみ存在する", rs.size(), is(10));
        assertThat("最初のデータはID:21", rs.get(0)
                .getInteger("id"), is(21));
        assertThat("最後のデータはID:30", rs.get(9)
                .getInteger("id"), is(30));
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
        resource.clearBatchOutputTable();

        // -------------------------------------------------- set error id
        ThrowErrorWriter.retryError = new HashSet<Integer>(Arrays.asList(25));

        // -------------------------------------------------- execute batch job
        final JobExecution execution = resource.startJob("chunk-error-integration-test");
        assertThat("正常に終了すること", execution.getBatchStatus(), is(BatchStatus.COMPLETED));

        // -------------------------------------------------- assert output table
        final SqlResultSet rs = resource.findBatchOutputTable();
        assertThat("リトライが実行されすべて正常に登録される", rs.size(), is(49));
        for (int i = 0; i < 49; i++) {
            int index = i + 1;
            final SqlRow row = rs.get(i);
            assertThat(row.getInteger("id"), is(index));
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
        resource.clearBatchOutputTable();

        // -------------------------------------------------- set error id
        ThrowErrorWriter.retryError = new HashSet<Integer>(Arrays.asList(25, 41));

        // -------------------------------------------------- execute batch job
        final JobExecution execution = resource.startJob("chunk-error-integration-test");
        assertThat("リトライリミット突破でJOBは異常終了する", execution.getBatchStatus(), is(BatchStatus.FAILED));

        // -------------------------------------------------- assert output table
        final SqlResultSet rs = resource.findBatchOutputTable();
        assertThat("リトライ上限突破で、最後にリトライ対象エラーがでたChunkの前までが登録される",
                rs.size(), is(40));
        for (int i = 0; i < 40; i++) {
            int index = i + 1;
            final SqlRow row = rs.get(i);
            assertThat(row.getInteger("id"), is(index));
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
        resource.clearBatchOutputTable();

        // -------------------------------------------------- execute batch job
        final JobExecution execution = resource.startJob("multi-step-integration-test");
        assertThat("正常に終了すること", execution.getBatchStatus(), is(BatchStatus.COMPLETED));

        // -------------------------------------------------- assert output table
        final SqlResultSet rs = resource.findBatchOutputTable();
        assertThat("Batchletでデータが登録できていること", rs.size(), is(10));
        for (int i = 0; i < 10; i++) {
            int index = i + 1;
            final SqlRow row = rs.get(i);
            assertThat(row.getInteger("id"), is(index));
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

        List<String> messages = InMemoryAppender.getLogMessages("PROGRESS");
        assertThat(messages, contains(
                startsWith("INFO progress start job. job name: [multi-step-integration-test]"),
                startsWith("INFO progress start step. job name: [multi-step-integration-test] step name: [batchlet]"),
                startsWith("INFO progress job name: [multi-step-integration-test] step name: [batchlet] input count: [10]"),
                allOf(
                        startsWith("INFO progress job name: [multi-step-integration-test] step name: [batchlet] tps:"),
                        containsString("estimated end time:"),
                        containsString("remaining count: [5]")
                ),
                allOf(
                        startsWith("INFO progress job name: [multi-step-integration-test] step name: [batchlet] tps:"),
                        containsString("estimated end time:"),
                        containsString("remaining count: [0]")
                ),
                startsWith("INFO progress finish step. job name: [multi-step-integration-test] step name: [batchlet] step status: [null]"),
                startsWith("INFO progress start step. job name: [multi-step-integration-test] step name: [chunk]"),
                startsWith("INFO progress job name: [multi-step-integration-test] step name: [chunk] input count: [10]"),
                startsWith("INFO progress chunk progress. write count=[10]"),
                allOf(
                        startsWith("INFO progress job name: [multi-step-integration-test] step name: [chunk] tps:"),
                        containsString("estimated end time:"),
                        containsString("remaining count: [0]")
                ),
                startsWith("INFO progress finish step. job name: [multi-step-integration-test] step name: [chunk] step status: [null]"),
                startsWith("INFO progress finish job. job name: [multi-step-integration-test]")

        ));
    }

    /**
     * マルチステップ構成のJOBで後続Stepが失敗した場合のテスト。
     */
    @Test
    public void executeMultiStep_Failed() throws Exception {
        final File outputFile = new File(folder.getRoot(), "outputFile.txt");
        FileWriter.outputPath = null;

        // -------------------------------------------------- setup output table
        resource.clearBatchOutputTable();

        // -------------------------------------------------- execute batch job
        final JobExecution execution = resource.startJob("multi-step-integration-test");
        assertThat("2番目のステップが失敗するのでJOBは異常終了する",
                execution.getBatchStatus(), is(BatchStatus.FAILED));

        // -------------------------------------------------- assert output table
        final SqlResultSet rs = resource.findBatchOutputTable();
        assertThat("Batchletでデータが登録できていること", rs.size(), is(10));
        for (int i = 0; i < 10; i++) {
            int index = i + 1;
            final SqlRow row = rs.get(i);
            assertThat(row.getInteger("id"), is(index));
        }

        // -------------------------------------------------- assert output table
        assertThat("2番めのステップで出力するファイルは存在していない", outputFile.exists(), is(false));

        List<String> messages = InMemoryAppender.getLogMessages("PROGRESS");
        assertThat(messages, contains(
                startsWith("INFO progress start job. job name: [multi-step-integration-test]"),
                startsWith("INFO progress start step. job name: [multi-step-integration-test] step name: [batchlet]"),
                startsWith("INFO progress job name: [multi-step-integration-test] step name: [batchlet] input count: [10]"),
                allOf(
                        startsWith("INFO progress job name: [multi-step-integration-test] step name: [batchlet] tps:"),
                        containsString("estimated end time:"),
                        containsString("remaining count: [5]")
                ),
                allOf(
                        startsWith("INFO progress job name: [multi-step-integration-test] step name: [batchlet] tps:"),
                        containsString("estimated end time:"),
                        containsString("remaining count: [0]")
                ),
                startsWith("INFO progress finish step. job name: [multi-step-integration-test] step name: [batchlet] step status: [null]"),
                startsWith("INFO progress start step. job name: [multi-step-integration-test] step name: [chunk]"),
                startsWith("INFO progress job name: [multi-step-integration-test] step name: [chunk] input count: [10]"),
                startsWith("INFO progress finish step. job name: [multi-step-integration-test] step name: [chunk] step status: [null]"),
                startsWith("INFO progress finish job. job name: [multi-step-integration-test]")
        ));

        // -------------------------------------------------- restart job
        InMemoryAppender.clear();
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

        messages = InMemoryAppender.getLogMessages("PROGRESS");
        assertThat(messages, contains(
                startsWith("INFO progress start job. job name: [multi-step-integration-test]"),
                startsWith("INFO progress start step. job name: [multi-step-integration-test] step name: [chunk]"),
                startsWith("INFO progress job name: [multi-step-integration-test] step name: [chunk] input count: [10]"),
                startsWith("INFO progress chunk progress. write count=[10]"),
                allOf(
                        startsWith("INFO progress job name: [multi-step-integration-test] step name: [chunk] tps:"),
                        containsString("estimated end time:"),
                        containsString("remaining count: [0]")
                ),
                startsWith("INFO progress finish step. job name: [multi-step-integration-test] step name: [chunk] step status: [null]"),
                startsWith("INFO progress finish job. job name: [multi-step-integration-test]")
        ));
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
        resource.clearBatchOutputTable();

        // -------------------------------------------------- update job active
        resource.updateBatchStatus("batchlet-integration-test", "1");

        // -------------------------------------------------- execute batch job
        final JobExecution execution = resource.startJob("batchlet-integration-test");
        assertThat("多重起動なのでJOBは異常終了する", execution.getBatchStatus(), is(BatchStatus.FAILED));
        assertThat("多重起動エラーが設定されていること", execution.getExitStatus(), is("JobAlreadyRunning"));

        // -------------------------------------------------- assert execute batchlet?
        assertThat("Batchletは実行されていないこと", RegisterBatchOutputTable.processExecuteFlag, is(false));

        // -------------------------------------------------- assert output table
        final SqlResultSet rs = resource.findBatchOutputTable();
        assertThat("Batchletは実行されないので何も登録されない", rs.isEmpty(), is(true));

        // -------------------------------------------------- assert batch status
        assertThat("バッチの状態は活性のままであること(0にはならないこと)",
                resource.findBatchStatus("batchlet-integration-test"), is("1"));

        // -------------------------------------------------- change active status
        resource.updateBatchStatus("batchlet-integration-test", "0");

        // -------------------------------------------------- restart job
        final JobExecution restartExecution = resource.restartJob(execution.getExecutionId());
        assertThat("リスタートには成功する。", restartExecution.getBatchStatus(), is(BatchStatus.COMPLETED));
        assertThat("バッチレットでデータが登録できているはず",
                resource.findBatchOutputTable()
                        .size(), is(10));
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
        resource.clearBatchOutputTable();

        // -------------------------------------------------- execute batch job
        final JobExecution execution = resource.startJob("item-write-listener-error-on-before-write-test");
        assertThat("異常終了していること", execution.getBatchStatus(), is(BatchStatus.FAILED));

        // -------------------------------------------------- assert output table
        final SqlResultSet rs = resource.findBatchOutputTable();
        assertThat("レコードは登録されていないこと", resource.findBatchOutputTable()
                .isEmpty(), is(true));
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
        resource.clearBatchOutputTable();

        // -------------------------------------------------- execute batch job
        final JobExecution execution = resource.startJob("item-write-listener-error-on-after-write-test");
        assertThat("異常終了していること", execution.getBatchStatus(), is(BatchStatus.FAILED));

        // -------------------------------------------------- assert output table
        final SqlResultSet rs = resource.findBatchOutputTable();
        assertThat("レコードは登録されていないこと", resource.findBatchOutputTable()
                .isEmpty(), is(true));
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
        resource.clearBatchOutputTable();

        // -------------------------------------------------- assert execute batchlet?
        final JobExecution execution = resource.startJob("step-listener-error-on-before-step-test");
        assertThat("異常終了していること", execution.getBatchStatus(), is(BatchStatus.FAILED));

        // -------------------------------------------------- assert output table
        assertThat("レコードは登録されていないこと", resource.findBatchOutputTable()
                .isEmpty(), is(true));
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
        resource.clearBatchOutputTable();

        // -------------------------------------------------- assert execute batchlet?
        final JobExecution execution = resource.startJob("step-listener-error-on-after-step-test");
        assertThat("異常終了していること", execution.getBatchStatus(), is(BatchStatus.FAILED));

        // -------------------------------------------------- assert output table
        assertThat("レコードは登録されていないこと", resource.findBatchOutputTable()
                .isEmpty(), is(true));
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
        resource.clearBatchOutputTable();

        // -------------------------------------------------- execute batch job
        final JobExecution prevExecution = resource.startJob("item-write-listener-error-on-after-write-test");
        assertThat("異常終了していること", prevExecution.getBatchStatus(), is(BatchStatus.FAILED));

        // -------------------------------------------------- assert output table
        assertThat("レコードは登録されていないこと", resource.findBatchOutputTable().isEmpty(), is(true));

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
        final SqlResultSet rs = resource.findBatchOutputTable();
        assertThat("25レコード登録されていること", rs.size(), is(15));

        for (int i = 0; i < 15; i++) {
            int index = i + 1;
            final SqlRow row = rs.get(i);
            assertThat(row.getInteger("id"), is(index));
            assertThat(row.getString("name"), is("name_" + index));
        }
    }

    /**
     * 正常終了するバッチジョブの進捗ログが出力されること。
     */
    @Test
    public void testProgressLog_Success() throws Exception {

        // -------------------------------------------------- clear output table
        resource.clearBatchOutputTable();

        // -------------------------------------------------- execute batch job
        resource.startJob("batchlet-integration-test");

        // -------------------------------------------------- assert log
        assertThat(InMemoryAppender.getLogMessages("PROGRESS"), contains(
                startsWith("INFO progress start job. job name: [batchlet-integration-test]"),
                startsWith("INFO progress start step. job name: [batchlet-integration-test] step name: [step1]"),
                startsWith("INFO progress job name: [batchlet-integration-test] step name: [step1] input count: [10]"),
                allOf(
                        startsWith("INFO progress job name: [batchlet-integration-test] step name: [step1] tps:"),
                        containsString("estimated end time:"),
                        containsString("remaining count: [5]")
                ),
                allOf(
                        startsWith("INFO progress job name: [batchlet-integration-test] step name: [step1] tps:"),
                        containsString("estimated end time:"),
                        containsString("remaining count: [0]")
                ),
                startsWith("INFO progress finish step. job name: [batchlet-integration-test] step name: [step1] step status: [null]"),
                startsWith("INFO progress finish job. job name: [batchlet-integration-test]")));
    }

    /**
     * 異常終了するバッチジョブの進捗ログが出力されること。
     */
    @Test
    public void testProgressLog_Failed() throws Exception {

        // -------------------------------------------------- clear output table
        resource.clearBatchOutputTable();

        // -------------------------------------------------- add initial data
        resource.insertBatchOutputTable(9);

        // -------------------------------------------------- execute batch job
        resource.startJob("batchlet-integration-test");

        // -------------------------------------------------- assert log
        assertThat(InMemoryAppender.getLogMessages("PROGRESS"), contains(
                startsWith("INFO progress start job. job name: [batchlet-integration-test]"),
                startsWith("INFO progress start step. job name: [batchlet-integration-test] step name: [step1]"),
                startsWith("INFO progress job name: [batchlet-integration-test] step name: [step1] input count: [10]"),
                allOf(
                        startsWith("INFO progress job name: [batchlet-integration-test] step name: [step1] tps:"),
                        containsString("estimated end time:"),
                        containsString("remaining count: [5]")
                ),
                startsWith("INFO progress finish step. job name: [batchlet-integration-test] step name: [step1] step status: [null]"),
                startsWith("INFO progress finish job. job name: [batchlet-integration-test]")));
    }

    /**
     * 正常終了するマルチステップの進捗ログが出力されること。
     */
    @Test
    public void testProgressLogMultiStep_Success() throws Exception {
        final File outputFile = folder.newFile("outputFile.txt");
        FileWriter.outputPath = outputFile;

        // -------------------------------------------------- setup output table
        resource.clearBatchOutputTable();

        // -------------------------------------------------- execute batch job
        resource.startJob("multi-step-integration-with-job-listener-test");

        // -------------------------------------------------- assert log
        assertThat(InMemoryAppender.getLogMessages("PROGRESS"), contains(
                startsWith("INFO progress start job. job name: [multi-step-integration-with-job-listener-test]"),
                startsWith("INFO progress start step. job name: [multi-step-integration-with-job-listener-test] step name: [batchlet]"),
                startsWith("INFO progress job name: [multi-step-integration-with-job-listener-test] step name: [batchlet] input count: [10]"),
                allOf(
                        startsWith("INFO progress job name: [multi-step-integration-with-job-listener-test] step name: [batchlet] tps:"),
                        containsString("estimated end time:"),
                        containsString("remaining count: [5]")
                ),
                allOf(
                        startsWith("INFO progress job name: [multi-step-integration-with-job-listener-test] step name: [batchlet] tps:"),
                        containsString("estimated end time:"),
                        containsString("remaining count: [0]")
                ),
                startsWith("INFO progress finish step. job name: [multi-step-integration-with-job-listener-test] step name: [batchlet] step status: [null]"),
                startsWith("INFO progress start step. job name: [multi-step-integration-with-job-listener-test] step name: [chunk]"),
                startsWith("INFO progress job name: [multi-step-integration-with-job-listener-test] step name: [chunk] input count: [10]"),
                startsWith("INFO progress chunk progress. write count=[10]"),
                allOf(
                        startsWith("INFO progress job name: [multi-step-integration-with-job-listener-test] step name: [chunk] tps:"),
                        containsString("estimated end time:"),
                        containsString("remaining count: [0]")
                ),
                startsWith("INFO progress finish step. job name: [multi-step-integration-with-job-listener-test] step name: [chunk] step status: [null]"),
                startsWith("INFO progress finish job. job name: [multi-step-integration-with-job-listener-test]")));
    }

    /**
     * 一度異常終了し、再起動時に正常終了するマルチステップの進捗ログが出力されること。
     */
    @Test
    public void testProgressLogMultiStep_Failed() throws Exception {
        final File outputFile = new File(folder.getRoot(), "outputFile.txt");
        FileWriter.outputPath = null;

        // -------------------------------------------------- setup output table
        resource.clearBatchOutputTable();

        // -------------------------------------------------- execute batch job
        final JobExecution execution = resource.startJob("multi-step-integration-with-job-listener-test");

        // -------------------------------------------------- assert log (1)
        assertThat(InMemoryAppender.getLogMessages("PROGRESS"), contains(
                startsWith("INFO progress start job. job name: [multi-step-integration-with-job-listener-test]"),
                startsWith("INFO progress start step. job name: [multi-step-integration-with-job-listener-test] step name: [batchlet]"),
                startsWith("INFO progress job name: [multi-step-integration-with-job-listener-test] step name: [batchlet] input count: [10]"),
                allOf(
                        startsWith("INFO progress job name: [multi-step-integration-with-job-listener-test] step name: [batchlet] tps:"),
                        containsString("estimated end time:"),
                        containsString("remaining count: [5]")
                ),
                allOf(
                        startsWith("INFO progress job name: [multi-step-integration-with-job-listener-test] step name: [batchlet] tps:"),
                        containsString("estimated end time:"),
                        containsString("remaining count: [0]")
                ),
                startsWith("INFO progress finish step. job name: [multi-step-integration-with-job-listener-test] step name: [batchlet] step status: [null]"),
                startsWith("INFO progress start step. job name: [multi-step-integration-with-job-listener-test] step name: [chunk]"),
                startsWith("INFO progress job name: [multi-step-integration-with-job-listener-test] step name: [chunk] input count: [10]"),
                startsWith("INFO progress finish step. job name: [multi-step-integration-with-job-listener-test] step name: [chunk] step status: [null]"),
                startsWith("INFO progress finish job. job name: [multi-step-integration-with-job-listener-test]")
        ));
        InMemoryAppender.clear();

        // -------------------------------------------------- restart batch job
        FileWriter.outputPath = outputFile;
        resource.restartJob(execution.getExecutionId());

        // -------------------------------------------------- assert log (2)
        assertThat(InMemoryAppender.getLogMessages("PROGRESS"), contains(
                startsWith("INFO progress start job. job name: [multi-step-integration-with-job-listener-test]"),
                startsWith("INFO progress start step. job name: [multi-step-integration-with-job-listener-test] step name: [chunk]"),
                startsWith("INFO progress job name: [multi-step-integration-with-job-listener-test] step name: [chunk] input count: [10]"),
                startsWith("INFO progress chunk progress. write count=[10]"),
                allOf(
                        startsWith("INFO progress job name: [multi-step-integration-with-job-listener-test] step name: [chunk] tps:"),
                        containsString("estimated end time:"),
                        containsString("remaining count: [0]")
                ),
                startsWith("INFO progress finish step. job name: [multi-step-integration-with-job-listener-test] step name: [chunk] step status: [null]"),
                startsWith("INFO progress finish job. job name: [multi-step-integration-with-job-listener-test]")));
    }

    /**
     * chunk実行時に進捗ログが出力されること。
     */
    @Test
    public void testProgressLogChunk() throws Exception {

        // -------------------------------------------------- clear output table
        resource.clearBatchOutputTable();

        // -------------------------------------------------- execute batch job
        resource.startJob("chunk-integration-test");

        // -------------------------------------------------- assert log
        assertThat(InMemoryAppender.getLogMessages("PROGRESS"), contains(
                startsWith("INFO progress start job. job name: [chunk-integration-test]"),
                startsWith("INFO progress start step. job name: [chunk-integration-test] step name: [myStep]"),
                startsWith("INFO progress job name: [chunk-integration-test] step name: [myStep] input count: [25]"),
                startsWith("INFO progress chunk progress. write count=[10]"),
                allOf(
                        startsWith("INFO progress job name: [chunk-integration-test] step name: [myStep] tps:"),
                        containsString("estimated end time:"),
                        containsString("remaining count: [15]")
                ),
                startsWith("INFO progress chunk progress. write count=[20]"),
                allOf(
                        startsWith("INFO progress job name: [chunk-integration-test] step name: [myStep] tps:"),
                        containsString("estimated end time:"),
                        containsString("remaining count: [5]")
                ),
                startsWith("INFO progress chunk progress. write count=[25]"),
                allOf(
                        startsWith("INFO progress job name: [chunk-integration-test] step name: [myStep] tps:"),
                        containsString("estimated end time:"),
                        containsString("remaining count: [0]")
                ),
                startsWith("INFO progress finish step. job name: [chunk-integration-test] step name: [myStep] step status: [null]"),
                startsWith("INFO progress finish job. job name: [chunk-integration-test]")
        ));
    }

    /**
     * batchlet実行時に運用通知ログが出力されること。
     */
    @Test
    public void testOperationLogBatchlet() throws Exception {

        // -------------------------------------------------- clear output table
        resource.clearBatchOutputTable();

        // -------------------------------------------------- execute batch job
        JobExecution execution = resource.startJob("operator-batchlet-test");

        // -------------------------------------------------- assert log
        assertThat("運用者向けのメッセージのみ出力されていること",
                InMemoryAppender.getLogMessages("OPERATION"), contains(allOf(
                        startsWith("ERROR operator ファイルの読み込みに失敗しました。ファイルが存在するか確認してください。"),
                        not(containsString("FileNotFoundException")))));

        assertThat("運用者向けのメッセージと合わせてスタックトレースも出力されていること",
                InMemoryAppender.getLogMessages("ALL"), Matchers.<String>hasItem(allOf(
                        startsWith("ERROR operator ファイルの読み込みに失敗しました。ファイルが存在するか確認してください。"),
                        containsString("FileNotFoundException"))));

        // -------------------------------------------------- assert batch status
        assertThat(execution.getBatchStatus(), is(BatchStatus.FAILED));
    }

    /**
     * chunk実行時に運用通知ログが出力されること。
     */
    @Test
    public void testOperationLogChunk() throws Exception {

        // -------------------------------------------------- clear output table
        resource.clearBatchOutputTable();

        // -------------------------------------------------- execute batch job
        JobExecution execution = resource.startJob("operator-chunk-test");

        // -------------------------------------------------- assert log
        assertThat("運用者向けのメッセージのみ出力されていること",
                InMemoryAppender.getLogMessages("OPERATION"), contains(allOf(
                        startsWith("ERROR operator ファイルの書き込みに失敗しました。他のプロセスによってファイルがロックされていないか確認してください。"),
                        not(containsString("IllegalStateException")))));

        assertThat("運用者向けのメッセージと合わせてスタックトレースも出力されていること",
                InMemoryAppender.getLogMessages("ALL"), Matchers.<String>hasItem(allOf(
                        startsWith("ERROR operator ファイルの書き込みに失敗しました。他のプロセスによってファイルがロックされていないか確認してください。"),
                        containsString("IllegalStateException"))));

        // -------------------------------------------------- assert batch status
        assertThat(execution.getBatchStatus(), is(BatchStatus.FAILED));
    }

    /**
     * 予期せぬ例外が発生した場合は運用者向けにログが出力されないこと
     */
    @Test
    public void testSystemError() throws Exception {

        // -------------------------------------------------- clear output table
        resource.clearBatchOutputTable();

        // -------------------------------------------------- execute batch job
        JobExecution execution = resource.startJob("batchlet-error-integration-test");

        // -------------------------------------------------- assert log
        assertThat("運用者向けのメッセージは出力されないこと",
                InMemoryAppender.getLogMessages("OPERATION"), is(nullValue()));

        assertThat("スタックトレースが出力されていること",
                InMemoryAppender.getLogMessages("ALL"),
                Matchers.<String>hasItem(containsString("StackOverflowError")));

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
     * アプリ側で{@link javax.batch.runtime.context.StepContext#setTransientUserData(Object)}を使用された場合に例外を送出すること
     *
     * @throws Exception
     */
    @Test
    public void testStepScoped_useTransientUserData() throws Exception {
        // -------------------------------------------------- execute batch job
        final JobExecution execution = resource.startJob("step-scoped-error-integration-test");
        assertThat(execution.getBatchStatus(), is(BatchStatus.FAILED));

        // -------------------------------------------------- assert log
        assertThat(InMemoryAppender.getLogMessages("ALL"), Matchers.<String>hasItem(allOf(
                containsString("javax.batch.operations.BatchRuntimeException"),
                containsString("TransientUserData of StepContext must be StepScopedHolder type."))));

    }

    @Test
    @RunAsClient
    public void RESTアクセスではStepContextが存在しないためStepScopedなBeanは利用できないこと(
            @ArquillianResource URI deploymentUri) throws Exception {

        final Response response = ClientBuilder.newClient()
                                               .target(deploymentUri)
                                               .path("/api/hello")
                                               .request()
                                               .get();

        assertThat(response.getStatus(), is(500));
        assertThat(response.readEntity(String.class),
                containsString("No active contexts for scope type nablarch.fw.batch.ee.cdi.StepScoped"));
    }

    /**
     * MainでBuchletを実行時に、バッチステータスがCOMPLETEDの場合に、戻り値0を返すこと。
     */
    @Test
    public void testJobExecutorSuccessBatchlet() throws Exception {
        final JobExecutor executor = new JobExecutor("success-batchlet-test");
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
        final JobExecutor executor = new JobExecutor("success-chunk-test");
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
        final JobExecutor executor = new JobExecutor("warning-batchlet-test");
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
        final JobExecutor executor = new JobExecutor("warning-batchlet-throw-error-test");
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
        final JobExecutor executor = new JobExecutor("warning-chunk-test");
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
        final JobExecutor executor = new JobExecutor("warning-chunk-throw-error-test");
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
        final JobExecutor executor = new JobExecutor("failure-batchlet-throw-error-test");
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
        final JobExecutor executor = new JobExecutor("failure-chunk-throw-error-test");
        int exitCode = executor.execute();
        assertThat(exitCode, is(1));
        assertThat(executor.getJobExecution().getExitStatus(), not("WARNING"));
        assertThat(executor.getJobExecution().getBatchStatus(), is(BatchStatus.FAILED));
    }

    /**
     * ChunkステップをもつバッチJOB、ステップ単位で指定したリスナーを実行し正常に処理が完了すること。
     * <p/>
     * <ul>
     * <li>バッチJOBは正常終了していること</li>
     * <li>アウトプットテーブルにレコードが登録されていること</li>
     * </ul>
     */
    @Test
    public void executeChunk_SuccessWithStepLevelListener() throws Exception {
        // -------------------------------------------------- clear output table
        resource.clearBatchOutputTable();

        // -------------------------------------------------- execute batch job
        final JobExecution execution = resource.startJob("specified-steplevel-integration-test");
        assertThat("バッチ処理が正常に終わっていること", execution.getBatchStatus(), is(BatchStatus.COMPLETED));

        // -------------------------------------------------- assert batch output table
        final SqlResultSet rs = resource.findBatchOutputTable();
        assertThat("25レコード登録されていること", rs.size(), is(25));

        for (int i = 0; i < 25; i++) {
            int index = i + 1;
            final SqlRow row = rs.get(i);
            assertThat(row.getInteger("id"), is(index));
            assertThat(row.getString("name"), is("name_" + index));
        }

        List<String> messages = InMemoryAppender.getLogMessages("PROGRESS");
        assertThat(messages, contains(
                startsWith("INFO progress start job. job name: [specified-steplevel-integration-test]"),
                startsWith("INFO progress start step. job name: [specified-steplevel-integration-test] step name: [myStep]"),
                startsWith("INFO progress job name: [specified-steplevel-integration-test] step name: [myStep] input count: [25]"),
                startsWith("INFO progress chunk progress. write count=[10]"),
                allOf(
                        startsWith("INFO progress job name: [specified-steplevel-integration-test] step name: [myStep] tps:"),
                        containsString("estimated end time:"),
                        containsString("remaining count: [15]")
                ),
                startsWith("INFO progress chunk progress. write count=[20]"),
                allOf(
                        startsWith("INFO progress job name: [specified-steplevel-integration-test] step name: [myStep] tps:"),
                        containsString("estimated end time:"),
                        containsString("remaining count: [5]")
                ),
                startsWith("INFO progress chunk progress. write count=[25]"),
                allOf(
                        startsWith("INFO progress job name: [specified-steplevel-integration-test] step name: [myStep] tps:"),
                        containsString("estimated end time:"),
                        containsString("remaining count: [0]")
                ),
                startsWith("INFO progress finish step. job name: [specified-steplevel-integration-test] step name: [myStep] step status: [null]"),
                startsWith("INFO progress finish job. job name: [specified-steplevel-integration-test]")
        ));
        assertThat(messages, not(contains(
                startsWith("INFO progress start step. job name: [specified-steplevel-integration-test] step name: [myStepWithoutProgressLog]"),
                startsWith("INFO progress finish step. job name: [specified-steplevel-integration-test] step name: [myStepWithoutProgressLog] step status: [null]")
        )));
    }
}

