package nablarch.fw.batch.ee.integration.app;

import java.util.List;
import javax.batch.api.chunk.AbstractItemWriter;
import javax.enterprise.context.Dependent;
import javax.inject.Named;

import nablarch.core.log.basic.LogLevel;
import nablarch.core.log.operation.OperationLogger;

@Dependent
@Named
public class OperationWriter extends AbstractItemWriter {

    @Override
    public void writeItems(List<Object> list) throws Exception {
        try {
            throw new IllegalStateException("could not write to output file.");
        } catch (Exception e) {
            OperationLogger.write(LogLevel.ERROR, "ファイルの書き込みに失敗しました。他のプロセスによってファイルがロックされていないか確認してください。", e);
            throw e;
        }
    }
}
