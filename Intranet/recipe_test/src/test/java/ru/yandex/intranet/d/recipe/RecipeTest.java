package ru.yandex.intranet.d.recipe;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.Executor;

import com.google.protobuf.Empty;
import io.grpc.CallCredentials;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Metadata;
import io.grpc.health.v1.HealthCheckRequest;
import io.grpc.health.v1.HealthCheckResponse;
import io.grpc.health.v1.HealthGrpc;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.yandex.intranet.d.backend.service.recipe_proto.CreateServiceRequest;
import ru.yandex.intranet.d.backend.service.recipe_proto.CreateUserRequest;
import ru.yandex.intranet.d.backend.service.recipe_proto.RecipeServiceGrpc;
import ru.yandex.intranet.d.backend.service.recipe_proto.ServiceParent;
import ru.yandex.intranet.d.backend.service.recipe_proto.ServiceRoles;

/**
 * Recipe integration test.
 *
 * @author Dmitriy Timashov <dm-tim@yandex-team.ru>
 */
public class RecipeTest {

    private static final Logger LOG = LoggerFactory.getLogger(RecipeTest.class);

    @Test
    public void testRecipe() {
        String recipeHttpPort = System.getenv("RECIPE_HTTP_PORT");
        String recipeGrpcPort = System.getenv("RECIPE_GRPC_PORT");
        Assertions.assertNotNull(recipeHttpPort);
        Assertions.assertNotNull(recipeGrpcPort);
        LOG.info("RECIPE_HTTP_PORT={}, RECIPE_GRPC_PORT={}", recipeHttpPort, recipeGrpcPort);
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + recipeHttpPort + "/local/readiness"))
                .build();
        String httpResponse = client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::body)
                .join();
        Assertions.assertEquals("OK", httpResponse);
        ManagedChannel channel = ManagedChannelBuilder
                .forAddress("localhost", Integer.parseInt(recipeGrpcPort))
                .usePlaintext()
                .build();
        try {
            HealthCheckResponse.ServingStatus status = HealthGrpc.newBlockingStub(channel)
                    .withCallCredentials(new CallCredentials() {
                @Override
                public void applyRequestMetadata(RequestInfo requestInfo, Executor appExecutor,
                                                 MetadataApplier applier) {
                    Metadata.Key<String> authHeaderKey = Metadata.Key.of("X-Ya-Uid",
                            Metadata.ASCII_STRING_MARSHALLER);
                    Metadata authHeaders = new Metadata();
                    authHeaders.put(authHeaderKey, "2120000000000001");
                    applier.apply(authHeaders);
                }

                @Override
                public void thisUsesUnstableApi() {
                }
            }).check(HealthCheckRequest.newBuilder().build()).getStatus();
            Assertions.assertEquals(HealthCheckResponse.ServingStatus.SERVING, status);
        } finally {
            channel.shutdown();
        }
    }

    @Test
    public void testCreateServiceHttp() {
        String recipeHttpPort = System.getenv("RECIPE_HTTP_PORT");
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + recipeHttpPort + "/recipe/service"))
                .POST(HttpRequest.BodyPublishers.ofString("{\"id\": 42, \"nameEn\": \"Test-1\", " +
                        "\"nameRu\": \"Тест-1\", \"slug\": \"test_1\"}"))
                .header("Content-type", "application/json")
                .header("X-Ya-Uid", "2120000000000001")
                .build();
        Integer httpResponse = client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::statusCode)
                .join();
        Assertions.assertEquals(204, httpResponse);
    }

    @Test
    public void testCreateChildServiceHttp() {
        String recipeHttpPort = System.getenv("RECIPE_HTTP_PORT");
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + recipeHttpPort + "/recipe/service"))
                .POST(HttpRequest.BodyPublishers.ofString("{\"id\": 69, \"nameEn\": \"Test-2\", " +
                        "\"nameRu\": \"Тест-2\", \"slug\": \"test_2\", \"parentId\": 1}"))
                .header("Content-type", "application/json")
                .header("X-Ya-Uid", "2120000000000001")
                .build();
        Integer httpResponse = client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::statusCode)
                .join();
        Assertions.assertEquals(204, httpResponse);
    }

    @Test
    public void testCreateUserHttp() {
        String recipeHttpPort = System.getenv("RECIPE_HTTP_PORT");
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + recipeHttpPort + "/recipe/user"))
                .POST(HttpRequest.BodyPublishers.ofString("{\"firstNameEn\":\"Test\",\"firstNameRu\":\"Тест\"," +
                        "\"lastNameEn\":\"Test\",\"lastNameRu\":\"Тест\",\"login\":\"testlogin\"," +
                        "\"uid\":2120000000000002,\"workEmail\":\"test@yandex-team.ru\",\"gender\":\"M\"," +
                        "\"langUi\":\"en\",\"timeZone\":\"Europe/Moscow\",\"rolesByService\":{\"1\":[754,1113]}," +
                        "\"admin\":true}"))
                .header("Content-type", "application/json")
                .header("X-Ya-Uid", "2120000000000001")
                .build();
        Integer httpResponse = client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::statusCode)
                .join();
        Assertions.assertEquals(204, httpResponse);
    }

    @Test
    public void testCreateDefaultFoldersHttp() {
        String recipeHttpPort = System.getenv("RECIPE_HTTP_PORT");
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + recipeHttpPort + "/recipe/_createDefaultFolders"))
                .POST(HttpRequest.BodyPublishers.noBody())
                .header("Content-type", "application/json")
                .header("X-Ya-Uid", "2120000000000001")
                .build();
        Integer httpResponse = client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::statusCode)
                .join();
        Assertions.assertEquals(204, httpResponse);
    }

    @Test
    public void testGetAccountsHttp() {
        String recipeHttpPort = System.getenv("RECIPE_HTTP_PORT");
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + recipeHttpPort + "/recipe/accounts"))
                .GET()
                .header("Content-type", "application/json")
                .header("X-Ya-Uid", "2120000000000001")
                .build();
        Integer httpResponse = client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::statusCode)
                .join();
        Assertions.assertEquals(200, httpResponse);
    }

    @Test
    public void testGetAccountsQuotasHttp() {
        String recipeHttpPort = System.getenv("RECIPE_HTTP_PORT");
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + recipeHttpPort + "/recipe/accountsQuotas"))
                .GET()
                .header("Content-type", "application/json")
                .header("X-Ya-Uid", "2120000000000001")
                .build();
        Integer httpResponse = client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::statusCode)
                .join();
        Assertions.assertEquals(200, httpResponse);
    }

    @Test
    public void testCreateServiceGrpc() {
        String recipeGrpcPort = System.getenv("RECIPE_GRPC_PORT");
        ManagedChannel channel = ManagedChannelBuilder
                .forAddress("localhost", Integer.parseInt(recipeGrpcPort))
                .usePlaintext()
                .build();
        try {
            RecipeServiceGrpc.newBlockingStub(channel)
                    .withCallCredentials(new CallCredentials() {
                        @Override
                        public void applyRequestMetadata(RequestInfo requestInfo, Executor appExecutor,
                                                         MetadataApplier applier) {
                            Metadata.Key<String> authHeaderKey = Metadata.Key.of("X-Ya-Uid",
                                    Metadata.ASCII_STRING_MARSHALLER);
                            Metadata authHeaders = new Metadata();
                            authHeaders.put(authHeaderKey, "2120000000000001");
                            applier.apply(authHeaders);
                        }

                        @Override
                        public void thisUsesUnstableApi() {
                        }
                    }).createService(CreateServiceRequest.newBuilder()
                        .setId(43)
                        .setNameEn("Test-3")
                        .setNameRu("Тест-3")
                        .setSlug("test_3")
                        .build());
        } finally {
            channel.shutdown();
        }
    }

    @Test
    public void testCreateChildServiceGrpc() {
        String recipeGrpcPort = System.getenv("RECIPE_GRPC_PORT");
        ManagedChannel channel = ManagedChannelBuilder
                .forAddress("localhost", Integer.parseInt(recipeGrpcPort))
                .usePlaintext()
                .build();
        try {
            RecipeServiceGrpc.newBlockingStub(channel)
                    .withCallCredentials(new CallCredentials() {
                        @Override
                        public void applyRequestMetadata(RequestInfo requestInfo, Executor appExecutor,
                                                         MetadataApplier applier) {
                            Metadata.Key<String> authHeaderKey = Metadata.Key.of("X-Ya-Uid",
                                    Metadata.ASCII_STRING_MARSHALLER);
                            Metadata authHeaders = new Metadata();
                            authHeaders.put(authHeaderKey, "2120000000000001");
                            applier.apply(authHeaders);
                        }

                        @Override
                        public void thisUsesUnstableApi() {
                        }
                    }).createService(CreateServiceRequest.newBuilder()
                        .setId(70)
                        .setNameEn("Test-4")
                        .setNameRu("Тест-4")
                        .setSlug("test_4")
                        .setParent(ServiceParent.newBuilder().setParentId(1).build())
                        .build());
        } finally {
            channel.shutdown();
        }
    }

    @Test
    public void testCreateUserGrpc() {
        String recipeGrpcPort = System.getenv("RECIPE_GRPC_PORT");
        ManagedChannel channel = ManagedChannelBuilder
                .forAddress("localhost", Integer.parseInt(recipeGrpcPort))
                .usePlaintext()
                .build();
        try {
            RecipeServiceGrpc.newBlockingStub(channel)
                    .withCallCredentials(new CallCredentials() {
                        @Override
                        public void applyRequestMetadata(RequestInfo requestInfo, Executor appExecutor,
                                                         MetadataApplier applier) {
                            Metadata.Key<String> authHeaderKey = Metadata.Key.of("X-Ya-Uid",
                                    Metadata.ASCII_STRING_MARSHALLER);
                            Metadata authHeaders = new Metadata();
                            authHeaders.put(authHeaderKey, "2120000000000001");
                            applier.apply(authHeaders);
                        }

                        @Override
                        public void thisUsesUnstableApi() {
                        }
                    }).createUser(CreateUserRequest.newBuilder()
                        .setFirstNameEn("TestName")
                        .setFirstNameRu("ТестИмя")
                        .setLastNameEn("TestName")
                        .setLastNameRu("ТестИмя")
                        .setLogin("testlogin-1")
                        .setUid(2120000000000003L)
                        .setWorkEmail("test-1@yandex-team.ru")
                        .setGender("M")
                        .setLangUi("en")
                        .setTimeZone("Europe/Moscow")
                        .addServiceRoles(ServiceRoles.newBuilder()
                                .setServiceId(1L)
                                .addRoleIds(754L)
                                .addRoleIds(1113L)
                                .build())
                        .setAdmin(true)
                        .build());
        } finally {
            channel.shutdown();
        }
    }

    @Test
    public void testCreateDefaultFoldersGrpc() {
        String recipeGrpcPort = System.getenv("RECIPE_GRPC_PORT");
        ManagedChannel channel = ManagedChannelBuilder
                .forAddress("localhost", Integer.parseInt(recipeGrpcPort))
                .usePlaintext()
                .build();
        try {
            RecipeServiceGrpc.newBlockingStub(channel)
                    .withCallCredentials(new CallCredentials() {
                        @Override
                        public void applyRequestMetadata(RequestInfo requestInfo, Executor appExecutor,
                                                         MetadataApplier applier) {
                            Metadata.Key<String> authHeaderKey = Metadata.Key.of("X-Ya-Uid",
                                    Metadata.ASCII_STRING_MARSHALLER);
                            Metadata authHeaders = new Metadata();
                            authHeaders.put(authHeaderKey, "2120000000000001");
                            applier.apply(authHeaders);
                        }

                        @Override
                        public void thisUsesUnstableApi() {
                        }
                    }).createDefaultFolders(Empty.newBuilder().build());
        } finally {
            channel.shutdown();
        }
    }

    @Test
    public void testGetAccountsGrpc() {
        String recipeGrpcPort = System.getenv("RECIPE_GRPC_PORT");
        ManagedChannel channel = ManagedChannelBuilder
                .forAddress("localhost", Integer.parseInt(recipeGrpcPort))
                .usePlaintext()
                .build();
        try {
            RecipeServiceGrpc.newBlockingStub(channel)
                    .withCallCredentials(new CallCredentials() {
                        @Override
                        public void applyRequestMetadata(RequestInfo requestInfo, Executor appExecutor,
                                                         MetadataApplier applier) {
                            Metadata.Key<String> authHeaderKey = Metadata.Key.of("X-Ya-Uid",
                                    Metadata.ASCII_STRING_MARSHALLER);
                            Metadata authHeaders = new Metadata();
                            authHeaders.put(authHeaderKey, "2120000000000001");
                            applier.apply(authHeaders);
                        }

                        @Override
                        public void thisUsesUnstableApi() {
                        }
                    }).getAccounts(Empty.newBuilder().build());
        } finally {
            channel.shutdown();
        }
    }

    @Test
    public void testGetAccountsQuotasGrpc() {
        String recipeGrpcPort = System.getenv("RECIPE_GRPC_PORT");
        ManagedChannel channel = ManagedChannelBuilder
                .forAddress("localhost", Integer.parseInt(recipeGrpcPort))
                .usePlaintext()
                .build();
        try {
            RecipeServiceGrpc.newBlockingStub(channel)
                    .withCallCredentials(new CallCredentials() {
                        @Override
                        public void applyRequestMetadata(RequestInfo requestInfo, Executor appExecutor,
                                                         MetadataApplier applier) {
                            Metadata.Key<String> authHeaderKey = Metadata.Key.of("X-Ya-Uid",
                                    Metadata.ASCII_STRING_MARSHALLER);
                            Metadata authHeaders = new Metadata();
                            authHeaders.put(authHeaderKey, "2120000000000001");
                            applier.apply(authHeaders);
                        }

                        @Override
                        public void thisUsesUnstableApi() {
                        }
                    }).getAccountsQuotas(Empty.newBuilder().build());
        } finally {
            channel.shutdown();
        }
    }

}
