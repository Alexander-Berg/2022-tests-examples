package ru.yandex.auto.core.generation.scope;

/**
 * User: yan1984
 * Date: 10.05.2011 19:58:20
 */
public class SampleInterfaceImpl implements SampleInterface {
    private String s;

    public SampleInterfaceImpl() {
        System.out.println("hello");
    }

    public void setS(String s) {
        this.s = s;
    }

    @Override
    public String getString() {
        return s;
    }

    @Override
    public void close() {
        
    }
}
