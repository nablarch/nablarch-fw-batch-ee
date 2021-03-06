package nablarch.fw.batch.ee.integration.app;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "batch_output")
public class BatchOutputEntity {

    private Integer id;

    private String name;

    public BatchOutputEntity() {
    }

    public BatchOutputEntity(Integer id, String name) {
        this.id = id;
        this.name = name;
    }

    @Id
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
