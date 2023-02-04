package ru.auto.tests.commons.extension.context;

import io.qameta.atlas.core.api.Context;
import io.qameta.atlas.core.util.MethodInfo;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import static com.google.common.collect.Iterables.getLast;

@Accessors(chain = true)
public class StepContext implements Context<List<ElementInfo>> {

    @Getter
    private String id = "";

    @Getter@Setter
    private String url;

    @Getter@Setter
    private String action;

    @Getter@Setter
    private Object[] args;

    @Getter@Setter
    private String description;

    @Getter
    private List<ElementInfo> elementsChain = new LinkedList<>();

    @Getter
    private List<ElementInfo> currentElement = new LinkedList<>();

    @Override
    public List<ElementInfo> getValue() {
        return currentElement.subList(tail(), currentElement.size());
    }

    public StepContext() {
        id = UUID.randomUUID().toString();
    }

    public void addElement(ElementInfo newElement) {
//        System.out.println(" add fb -> " + newElement.getFindBy());
        if (elementsChain.isEmpty()) {
            elementsChain.add(newElement);
            return;
        }
        if ( ! getLast(elementsChain).getFindBy()
                .equals(newElement.getFindBy())) {
            elementsChain.add(newElement);
        }
    }

    public StepContext currentContext() {
        if (elementsChain.size() > 0) {
            currentElement = elementsChain;
            elementsChain = new ArrayList<>();
        }
        return this;
    }

    public void print() {
        currentElement.forEach(e -> System.out.println(" -> " + e.getFindBy()));
    }

    public String locator() {
        int tail = tail();
        StringBuilder locators = new StringBuilder();
        for (ElementInfo elementInfo : currentElement.subList(tail, currentElement.size())) {
            if (elementInfo.getFindBy().startsWith(".")) {
                locators.append(elementInfo.getFindBy().substring(1));
            } else if (elementInfo.getFindBy().startsWith("[")) {
                locators.insert(0, "(").append(")").append(elementInfo.getFindBy());
            } else {
                locators.append(elementInfo.getFindBy());
            }
        }
        return locators.toString();
    }

    public String name() {
        int tail = tail();
        StringBuilder names = new StringBuilder();
        for (ElementInfo name : currentElement.subList(tail, currentElement.size())) {
            names.append(String.format(" > %s", name.getName()));
        }
        if (names.length()==0) return "";
        return names.toString().substring(3);
    }

    private int tail() {
        int tail = 0;
        int size = currentElement.size();
        for (int i = 0; i < size; i++) {
            if (currentElement.get(i).getFindBy().startsWith("//")) {
                tail = i;
            }
        }
        return tail;
    }

    public StepContext copy() {
        StepContext copy = new StepContext();
        copy.setUrl(this.url);
        copy.setAction(this.action);
        copy.setArgs(this.args);
        copy.getElementsChain().addAll(this.elementsChain);
        copy.getCurrentElement().addAll(this.currentElement);
        return copy;
    }
}
