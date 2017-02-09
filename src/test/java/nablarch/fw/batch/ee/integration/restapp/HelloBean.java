package nablarch.fw.batch.ee.integration.restapp;

import nablarch.fw.batch.ee.cdi.StepScoped;

@StepScoped
public class HelloBean {

    public String get() {
        return "hello!!!";
    }
}
