package nablarch.fw.batch.ee.integration.app;

import jakarta.batch.api.AbstractBatchlet;
import jakarta.batch.runtime.context.StepContext;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import nablarch.core.db.connection.AppDbConnection;
import nablarch.core.db.connection.DbConnectionContext;
import nablarch.core.db.statement.SqlPStatement;
import nablarch.fw.batch.ee.progress.ProgressManager;

/**
 * batch_outputテーブルへデータを10レコード登録する{@link jakarta.batch.api.Batchlet}実装。
 *
 */
@Named
@Dependent
public class RegisterBatchOutputTable extends AbstractBatchlet {

    public static boolean processExecuteFlag = false;

    private final ProgressManager progressManager;

    @Inject
    public RegisterBatchOutputTable(ProgressManager progressManager) {
        this.progressManager = progressManager;
    }

    @Inject
    StepContext stepContext;

    @Override
    public String process() throws Exception {
        progressManager.setInputCount(10);
        processExecuteFlag = true;
        final AppDbConnection connection = DbConnectionContext.getConnection();
        final SqlPStatement statement = connection.prepareStatement("insert into batch_output values (?, ?)");

        for (int i = 0; i < 10; i++) {
            int index = i + 1;
            statement.setInt(1, index);
            statement.setString(2, "name_" + index);
            statement.executeUpdate();

            if (index % 5 == 0) {
                progressManager.outputProgressInfo(index);
            }
        }

        return "SUCCESS";
    }
}
