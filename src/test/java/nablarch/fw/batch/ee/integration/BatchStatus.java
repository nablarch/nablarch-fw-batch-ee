package nablarch.fw.batch.ee.integration;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "BATCH_STATUS")
public class BatchStatus {
    public BatchStatus() {
    }

    public BatchStatus(String jobName, String active) {
        this.jobName = jobName;
        this.active = active;
    }

    @Id
    @Column(name = "JOB_NAME", length = 100)
    public String jobName;
    @Column(name = "ACTIVE", length = 1)
    public String active;
}
