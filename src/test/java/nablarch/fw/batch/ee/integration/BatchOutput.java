package nablarch.fw.batch.ee.integration;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "BATCH_OUTPUT")
public class BatchOutput {
    public BatchOutput() {
    }

    public BatchOutput(Integer id, String name) {
        this.id = id;
        this.name = name;
    }

    @Id
    @Column(name = "ID", length = 10)
    public Integer id;

    @Column(name = "NAME", length = 100)
    public String name;
}
