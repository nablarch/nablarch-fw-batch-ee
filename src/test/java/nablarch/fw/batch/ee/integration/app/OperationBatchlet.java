package nablarch.fw.batch.ee.integration.app;

import java.io.FileNotFoundException;
import jakarta.batch.api.AbstractBatchlet;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Named;

import nablarch.core.log.basic.LogLevel;
import nablarch.core.log.operation.OperationLogger;

@Named
@Dependent
public class OperationBatchlet extends AbstractBatchlet {

    @Override
    public String process() throws Exception {
        try {
            throw new FileNotFoundException("input file is not found.");
        } catch (Exception e) {
            OperationLogger.write(LogLevel.ERROR, "ファイルの読み込みに失敗しました。ファイルが存在するか確認してください。", e);
            throw e;
        }
    }
}
