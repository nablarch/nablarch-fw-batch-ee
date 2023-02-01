package nablarch.fw.batch.ee.integration.app;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Serializable;
import java.nio.charset.Charset;
import java.util.List;

import jakarta.batch.api.chunk.AbstractItemWriter;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Named;

/**
 * ファイルに出力する。
 */
@Dependent
@Named
public class FileWriter extends AbstractItemWriter {

    public static File outputPath;

    private BufferedWriter writer = null;

    @Override
    public void open(Serializable checkpoint) throws Exception {
        writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputPath), Charset.forName("utf-8")));
    }

    @Override
    public void writeItems(List<Object> list) throws Exception {
        for (Object o : list) {
            BatchOutputEntity entity = (BatchOutputEntity) o;
            writer.write(String.valueOf(entity.getId()));
            writer.write("\n");
        }
    }

    @Override
    public void close() throws Exception {
        if (writer != null) {
            writer.close();
        }
    }
}
