package nablarch.fw.batch.ee.chunk;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.hamcrest.beans.HasPropertyWithValue.hasProperty;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.io.Serializable;
import java.util.Iterator;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import nablarch.common.dao.EntityUtil;
import nablarch.common.dao.UniversalDao;
import nablarch.core.db.connection.ConnectionFactory;
import nablarch.core.db.connection.DbConnectionContext;
import nablarch.core.db.connection.TransactionManagerConnection;
import nablarch.core.repository.SystemRepository;
import nablarch.core.transaction.TransactionContext;
import nablarch.test.support.SystemRepositoryResource;
import nablarch.test.support.db.helper.DatabaseTestRunner;
import nablarch.test.support.db.helper.VariousDbTestHelper;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;

import mockit.Mocked;
import mockit.Verifications;

/**
 * {@link BaseDatabaseItemReader}のテストクラス。
 */
@RunWith(DatabaseTestRunner.class)
public class BaseDatabaseItemReaderTest {

    @Rule
    public SystemRepositoryResource systemRepositoryResource = new SystemRepositoryResource("db-default.xml");

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Before
    public void before() {
        VariousDbTestHelper.createTable(TestEntity.class);
        VariousDbTestHelper.setUpTable(new TestEntity(1, "name1"), new TestEntity(2, "name2"));

        final ConnectionFactory connectionFactory = systemRepositoryResource.getComponentByType(
                ConnectionFactory.class);
        final TransactionManagerConnection connection = connectionFactory.getConnection(
                TransactionContext.DEFAULT_TRANSACTION_CONTEXT_KEY);
        DbConnectionContext.setConnection(connection);
    }

    @After
    public void tearDown() throws Exception {
        if (DbConnectionContext.containConnection(TransactionContext.DEFAULT_TRANSACTION_CONTEXT_KEY)) {
            final TransactionManagerConnection connection = DbConnectionContext.getTransactionManagerConnection();
            connection.terminate();
            DbConnectionContext.removeConnection();
        }
    }

    /**
     * デフォルト接続でのDB操作がリーダーの処理に影響しないこと。
     */
    @Test
    public void readItem() throws Exception {
        BaseDatabaseItemReader sut = new TestDatabaseItemReader();
        final TransactionManagerConnection connection = DbConnectionContext.getTransactionManagerConnection();
        sut.open(null);
        assertThat(DbConnectionContext.getTransactionManagerConnection(), sameInstance(connection));
        connection.commit();
        connection.terminate();

        try {
            assertThat(sut.readItem(), allOf(
                    hasProperty("id", is(1)),
                    hasProperty("name", is("name1"))
            ));

            assertThat(sut.readItem(), allOf(
                    hasProperty("id", is(2)),
                    hasProperty("name", is("name2"))
            ));

            assertThat(sut.readItem(), is(nullValue()));
        } finally {
            sut.close();
        }
    }

    /**
     * open時に例外が発生しても元々のコネクションに戻ること。
     *
     * @throws Exception
     */
    @Test
    public void readItem_failed() throws Exception {

        final BaseDatabaseItemReader sut = new FailedDatabaseItemReader();

        final TransactionManagerConnection originalConnection = DbConnectionContext.getTransactionManagerConnection();

        try {
            sut.open(null);
            fail("例外が発生する");
        } catch (Exception e) {
            assertThat(e.getMessage(), is("doOpenで例外"));
        }

        assertThat(DbConnectionContext.getTransactionManagerConnection(), sameInstance(originalConnection));
    }

    /**
     * close時にコネクションが閉じられること。
     */
    @Test
    public void close(@Mocked final TransactionManagerConnection mockConnection) throws Exception {
        systemRepositoryResource.addComponent("connectionFactory", new ConnectionFactory() {
            @Override
            public TransactionManagerConnection getConnection(final String s) {
                return mockConnection;
            }
        });

        final TestDatabaseItemReader sut = new TestDatabaseItemReader();
        sut.open(null);
        sut.close();

        new Verifications() {{
            mockConnection.terminate();
            times = 1;
        }};
    }

    /**
     * リポジトリに {@link ConnectionFactory}が存在しない場合は、例外が送出されること
     */
    @Test
    public void connectionFactoryNotFound_shouldThrowException() throws Exception {
        SystemRepository.clear();

        final TestDatabaseItemReader sut = new TestDatabaseItemReader();

        expectedException.expect(IllegalStateException.class);
        expectedException.expectMessage(
                "ConnectionFactory was not found. must be set ConnectionFactory(component name=connectionFactory).");
        sut.open(null);
    }

    /**
     * リーダが接続を持っていない場合(接続を開く際に例外が発生した場合を想定)でも、closeは例外を送出しないこと
     */
    @Test
    public void connectionNotFound_callClose() throws Exception {
        final BaseDatabaseItemReader sut = new TestDatabaseItemReader();
        sut.close();
    }

    @Test
    public void デフォルトの接続が設定されていない場合でもリーダでデータベースアクセスが出来ること() throws Exception {
        DbConnectionContext.removeConnection();

        final BaseDatabaseItemReader sut = new TestDatabaseItemReader();
        sut.open(null);
        try {
            assertThat(sut.readItem(), hasProperty("id", is(1)));
        } finally {
            sut.close();
        }

    }

    /**
     * Testテーブルのレコードを読み込むリーダー
     */
    private static class TestDatabaseItemReader extends BaseDatabaseItemReader {

        private Iterator<TestEntity> entityList;

        @Override
        protected void doOpen(final Serializable checkpoint) throws Exception {
            EntityUtil.getTableName(TestEntity.class);
            entityList = UniversalDao.defer()
                                     .findAllBySqlFile(TestEntity.class, "find_all")
                                     .iterator();
        }

        @Override
        public Object readItem() throws Exception {
            if (entityList.hasNext()) {
                return entityList.next();
            }
            return null;
        }
    }

    /**
     * Open時に例外が発生するリーダー
     */
    private static class FailedDatabaseItemReader extends BaseDatabaseItemReader {

        @Override
        protected void doOpen(Serializable checkpoint) throws Exception {
            throw new Exception("doOpenで例外");
        }

        @Override
        public Object readItem() throws Exception {
            return null;
        }
    }

    /**
     * テストテーブルのエンティティ
     */
    @Entity
    @Table(name = "TestEntity")
    public static class TestEntity {

        @Id
        @Column(name = "id", length = 10)
        public Integer id;

        @Column(length = 16, name = "name")
        public String name;

        public TestEntity() {
        }

        public TestEntity(Integer id, String name) {
            this.id = id;
            this.name = name;
        }


        public Integer getId() {
            return id;
        }

        public void setId(Integer id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

}