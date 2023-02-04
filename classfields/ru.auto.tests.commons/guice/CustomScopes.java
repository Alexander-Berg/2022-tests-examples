package ru.auto.tests.commons.guice;

import com.google.inject.Scope;

/**
 * @author Artem Eroshenko <erosenkoam@me.com>
 */
public class CustomScopes {

    public static final Scope THREAD = new ThreadLocalScope();

    private CustomScopes() {
    }

}