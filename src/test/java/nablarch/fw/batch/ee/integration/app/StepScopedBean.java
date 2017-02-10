package nablarch.fw.batch.ee.integration.app;

import nablarch.fw.batch.ee.cdi.StepScoped;

/**
 * Step間で共有するBean
 */
@StepScoped
public class StepScopedBean {

    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
