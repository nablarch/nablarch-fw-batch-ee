package nablarch.fw.batch.ee.integration.app;

import javax.batch.api.AbstractBatchlet;
import javax.batch.runtime.context.StepContext;
import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.inject.Named;

import nablarch.core.db.connection.AppDbConnection;
import nablarch.core.db.connection.DbConnectionContext;
import nablarch.core.db.statement.SqlPStatement;

/**
 * batch_outputテーブルへデータを10レコード登録後に{@link StackOverflowError}を送出する{@link javax.batch.api.Batchlet}実装クラス。
 *
 */
@Named
@Dependent
public class ThrowErrorBatchlet extends AbstractBatchlet {

    @Inject
    StepContext stepContext;

    @Override
    public String process() throws Exception {
        final AppDbConnection connection = DbConnectionContext.getConnection();
        final SqlPStatement statement = connection.prepareStatement("insert into batch_output values (?, ?)");

        for (int i = 0; i < 10; i++) {
            int index = i + 1;
            statement.setInt(1, index);
            statement.setString(2, "name_" + index);
            statement.executeUpdate();
        }

        stepContext.setExitStatus("FAILED");
        throw new StackOverflowError();
    }
}
