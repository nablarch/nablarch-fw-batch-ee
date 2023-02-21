package nablarch.fw.batch.ee.integration;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

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
