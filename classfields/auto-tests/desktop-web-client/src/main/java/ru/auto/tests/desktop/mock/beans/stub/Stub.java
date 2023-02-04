package ru.auto.tests.desktop.mock.beans.stub;

import com.google.gson.GsonBuilder;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.List;

@Getter
@Accessors(chain = true)
public class Stub {

    List<Predicate> predicates;
    List<Response> responses;

    public static Stub stub() {
        return new Stub();
    }

    public Stub setPredicate(Predicate predicate) {
        predicates = new ArrayList<>();
        predicates.add(predicate);
        return this;
    }

    public Stub setResponse(Response response) {
        responses = new ArrayList<>();
        responses.add(response);
        return this;
    }

    @Override
    public String toString() {
        return new GsonBuilder().create().toJson(this);
    }

}
