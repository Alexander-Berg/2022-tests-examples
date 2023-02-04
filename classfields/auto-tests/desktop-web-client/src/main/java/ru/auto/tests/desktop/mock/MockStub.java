package ru.auto.tests.desktop.mock;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import io.restassured.http.Method;
import ru.auto.tests.commons.mountebank.http.predicates.PredicateType;
import ru.auto.tests.desktop.mock.beans.stub.Is;
import ru.auto.tests.desktop.mock.beans.stub.Parameters;
import ru.auto.tests.desktop.mock.beans.stub.Query;
import ru.auto.tests.desktop.mock.beans.stub.Stub;

import java.util.Optional;

import static io.restassured.http.ContentType.JSON;
import static java.lang.String.format;
import static javax.ws.rs.HttpMethod.GET;
import static org.apache.http.HttpStatus.SC_OK;
import static ru.auto.tests.commons.util.Utils.getResourceAsString;
import static ru.auto.tests.desktop.mock.PredicateTypeDependentMethods.getParameters;
import static ru.auto.tests.desktop.mock.PredicateTypeDependentMethods.getPredicate;
import static ru.auto.tests.desktop.mock.beans.stub.Headers.headers;
import static ru.auto.tests.desktop.mock.beans.stub.Is.is;
import static ru.auto.tests.desktop.mock.beans.stub.Parameters.parameters;
import static ru.auto.tests.desktop.mock.beans.stub.Response.response;
import static ru.auto.tests.desktop.utils.Utils.getJsonByPath;
import static ru.auto.tests.desktop.utils.Utils.getJsonObject;

public class MockStub {

    private Stub stub;
    private String path;
    private String method;
    private JsonObject query;
    private JsonObject requestBody;
    private JsonObject responseBody;
    private String contentType;
    private int statusCode;
    private PredicateType predicateType;
    private boolean isStubEdited;

    private MockStub() {
    }

    public static MockStub stub() {
        return new MockStub();
    }

    public static MockStub stub(String stubPath) {
        try {
            String stub = getResourceAsString(format("mocks/%s.json", stubPath));

            return stub().setStub(new Gson()
                    .fromJson(stub, Stub.class));
        } catch (NullPointerException e) {
            String exceptionMessage = format("Can't read mock file '%s'", stubPath);
            throw new RuntimeException(exceptionMessage, e.getCause());
        }
    }

    private MockStub setStub(Stub stub) {
        this.stub = stub;
        return this;
    }

    public MockStub withResponseBody(String responseBodyPath) {
        this.responseBody = getJsonByPath(format("mocks/%s.json", responseBodyPath));
        return this;
    }

    public MockStub withResponseBody(JsonObject jsonObject) {
        this.responseBody = jsonObject;
        return this;
    }

    public MockStub withPredicateType(PredicateType predicateType) {
        this.predicateType = predicateType;
        isStubEdited = true;
        return this;
    }

    public MockStub withPath(String path) {
        this.path = path;
        return this;
    }

    public MockStub withMethod(Method method) {
        this.method = method.toString();
        return this;
    }

    public MockStub withRequestQuery(Query query) {
        this.query = getJsonObject(query);
        return this;
    }

    public MockStub withRequestBody(String requestBodyPath) {
        this.requestBody = getJsonByPath(format("mocks/%s.json", requestBodyPath));
        return this;
    }

    public MockStub withRequestBody(JsonObject requestBody) {
        this.requestBody = requestBody;
        return this;
    }

    public MockStub withContentType(String contentType) {
        this.contentType = contentType;
        return this;
    }

    public MockStub withStatusCode(int statusCode) {
        this.statusCode = statusCode;
        return this;
    }

    public MockStub withGetDeepEquals(String path) {
        withPredicateType(PredicateType.DEEP_EQUALS);
        withMethod(Method.GET);
        withPath(path);
        return this;
    }

    public MockStub withPostDeepEquals(String path) {
        withPredicateType(PredicateType.DEEP_EQUALS);
        withMethod(Method.POST);
        withPath(path);
        return this;
    }

    public MockStub withPutDeepEquals(String path) {
        withPredicateType(PredicateType.DEEP_EQUALS);
        withMethod(Method.PUT);
        withPath(path);
        return this;
    }

    public MockStub withDeleteDeepEquals(String path) {
        withPredicateType(PredicateType.DEEP_EQUALS);
        withMethod(Method.DELETE);
        withPath(path);
        return this;
    }

    public MockStub withPostMatches(String path) {
        withPredicateType(PredicateType.MATCHES);
        withMethod(Method.POST);
        withPath(path);
        return this;
    }

    public MockStub withStatusSuccessResponse() {
        JsonObject body = new JsonObject();
        body.addProperty("status", "SUCCESS");

        contentType = JSON.toString();
        statusCode = SC_OK;
        responseBody = body;

        return this;
    }

    private void build() {
        if (stub == null) {
            saveNewStub();
        } else if (isStubEdited) {
            editCurrentStub();
        }
    }

    private void saveNewStub() {
        Parameters predicateParameters = parameters();
        predicateParameters.setMethod(Optional.ofNullable(method).orElse(GET));
        predicateParameters.setPath(path);
        predicateParameters.setQuery(query);
        predicateParameters.setBody(requestBody);

        Is is = is();
        is.setHeaders(headers().setContentType(Optional.ofNullable(contentType).orElse(JSON.toString())));
        is.setBody(responseBody == null ? new JsonObject() : responseBody);
        is.setStatusCode(statusCode == 0 ? SC_OK : statusCode);

        stub = Stub.stub()
                .setPredicate(getPredicate(predicateType, predicateParameters))
                .setResponse(response().setIs(is));
    }

    private void editCurrentStub() {
        Parameters predicateParameters = getParameters(predicateType, stub.getPredicates().get(0));

        Optional.ofNullable(method).ifPresent(predicateParameters::setMethod);
        Optional.ofNullable(path).ifPresent(predicateParameters::setPath);
        Optional.ofNullable(query).ifPresent(predicateParameters::setQuery);
        Optional.ofNullable(requestBody).ifPresent(predicateParameters::setBody);

        stub.getPredicates().set(0, getPredicate(predicateType, predicateParameters));

        Optional.ofNullable(responseBody).ifPresent(stub.getResponses().get(0).getIs()::setBody);

        if (statusCode != 0) {
            stub.getResponses().get(0).getIs().setStatusCode(statusCode);
        }

    }

    public Stub getStub() {
        build();
        return stub;
    }

    public static MockStub sessionAuthUserStub() {
        return stub("desktop/SessionAuthUser");
    }

    @Override
    public String toString() {
        return stub.toString();
    }

}
