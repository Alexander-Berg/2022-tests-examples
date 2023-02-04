package ru.yandex.solomon.alert;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.grpc.Status;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpHeaders;
import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClient;
import org.asynchttpclient.RequestBuilder;

import ru.yandex.discovery.DiscoveryServices;
import ru.yandex.misc.concurrent.CompletableFutures;
import ru.yandex.misc.io.http.HttpStatus;
import ru.yandex.solomon.alert.client.AlertingClient;
import ru.yandex.solomon.alert.client.AlertingClients;
import ru.yandex.solomon.alert.protobuf.TAlert;
import ru.yandex.solomon.alert.protobuf.TCreateAlertRequest;
import ru.yandex.solomon.alert.protobuf.TCreateAlertResponse;
import ru.yandex.solomon.alert.protobuf.TCreateNotificationRequest;
import ru.yandex.solomon.alert.protobuf.TDeleteAlertRequest;
import ru.yandex.solomon.alert.protobuf.TDeleteAlertResponse;
import ru.yandex.solomon.alert.protobuf.TDeleteNotificationRequest;
import ru.yandex.solomon.alert.protobuf.TDeleteNotificationResponse;
import ru.yandex.solomon.alert.protobuf.TEvaluationStatus;
import ru.yandex.solomon.alert.protobuf.TListAlert;
import ru.yandex.solomon.alert.protobuf.TListAlertRequest;
import ru.yandex.solomon.alert.protobuf.TListNotificationsRequest;
import ru.yandex.solomon.alert.protobuf.TReadAlertRequest;
import ru.yandex.solomon.alert.protobuf.notification.TEmailType;
import ru.yandex.solomon.alert.protobuf.notification.TJugglerType;
import ru.yandex.solomon.alert.protobuf.notification.TNotification;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.toList;

/**
 * @author Vladimir Gordiychuk
 */
public class SyncPreProd implements AutoCloseable {
    private static final String token = "OAuth token";

    private static final String FAKE_EMAIL_CHANNEL = "solomon-alerts";
    private static final String FAKE_JUGLER_CHANNEL = "fake-juggler";

    private AlertingClient prod;
    private AlertingClient pre;
    private AsyncHttpClient httpClient;

    public SyncPreProd() {
        this.prod = AlertingClients.create(DiscoveryServices.resolve("conductor_group://solomon_prod_alerting:8799"));
        this.pre = AlertingClients.create(DiscoveryServices.resolve("conductor_group://solomon_pre_alerting:8799"));
        this.httpClient = new DefaultAsyncHttpClient();
    }

    public static void main(String[] args) throws Exception {
        try (SyncPreProd sync = new SyncPreProd()) {
            sync.sync("solomon", false).join();
            System.exit(0);
        }
    }

    public CompletableFuture<Void> sync(String projectId, boolean patch) {
        System.out.println(projectId +" starting sync");
        return syncChannels(projectId)
                .thenCompose(ignore -> syncAlerts(projectId, patch));
    }

    public CompletableFuture<Void> syncAll(boolean patch) {
        return projects("https://solomon.yandex-team.ru")
                .thenCompose(list -> {
                    CompletableFuture<Void> future = null;
                    for (String projectId : list) {
                        if (future == null) {
                            future = sync(projectId, patch);
                        } else {
                            future = future.thenCompose(ignore -> sync(projectId, patch));
                        }
                    }

                    return future;
                });
    }

    private CompletableFuture<List<String>> projects(String host) {
        return httpClient.executeRequest(new RequestBuilder()
                .setMethod("GET")
                .setUrl(host + "/api/v2/projects")
                .setHeader(HttpHeaders.AUTHORIZATION, token)
                .setHeader(HttpHeaders.ACCEPT, "application/json")
                .build())
                .toCompletableFuture()
                .thenApply(response -> {
                    if (!HttpStatus.is2xx(response.getStatusCode())) {
                        throw new IllegalStateException(response.toString());
                    }

                    ObjectMapper mapper = new ObjectMapper();
                    try {
                        List<String> result = new ArrayList<>();
                        JsonNode root = mapper.readTree(response.getResponseBody());
                        for (JsonNode item : root) {
                            result.add(item.get("id").asText());
                        }
                        return result;
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
    }

    private CompletableFuture<Void> syncChannels(String projectId) {
        System.out.println(projectId + " starting sync channels");
        return deleteChannelsOnPre(projectId)
                .thenCompose(ignore -> copyChannelsFromProd(projectId, ""))
                .thenCompose(ignore -> createFakeChannels(projectId));
    }

    private CompletableFuture<Void> syncAlerts(String projectId, boolean patch) {
        System.out.println(projectId + " starting sync alerts");
        return deleteAlertsOnPre(projectId)
                .thenCompose(ignore -> copyAlertsFromProd(projectId, "", patch));
    }

    private CompletableFuture<Void> deleteChannelsOnPre(String projectId) {
        return pre.listNotification(TListNotificationsRequest.newBuilder()
                .setProjectId(projectId)
                .setPageSize(1000)
                .build())
                .thenCompose(response -> {
                    final String nextToken = response.getNextPageToken();
                    System.out.println(projectId + " will delete " + response.getNotificationCount() + " channels");
                    return response.getNotificationList()
                            .stream()
                            .map(TNotification::getId)
                            .map(id -> deleteChannelOnPre(projectId, id))
                            .collect(Collectors.collectingAndThen(toList(), CompletableFutures::allOfUnit))
                            .thenCompose(unit -> {
                                if (StringUtils.isEmpty(nextToken)) {
                                    System.out.println(projectId + " all channels deleted");
                                    return completedFuture(null);
                                }

                                return deleteChannelsOnPre(projectId);
                            });
                });
    }

    private CompletableFuture<TDeleteNotificationResponse> deleteChannelOnPre(String projectId, String id) {
        System.out.println(projectId + " delete channel " + id);
        return pre.deleteNotification(TDeleteNotificationRequest.newBuilder()
                .setProjectId(projectId)
                .setNotificationId(id)
                .build());
    }

    private CompletableFuture<Void> copyChannelsFromProd(String projectId, String pageToken) {
        return prod.listNotification(TListNotificationsRequest.newBuilder()
                .setProjectId(projectId)
                .setPageToken(pageToken)
                .setPageSize(1000)
                .build())
                .thenCompose(response -> {
                    String nextPageToken = response.getNextPageToken();
                    return response.getNotificationList()
                            .stream()
                            .map(this::createChannelOnPre)
                            .collect(collectingAndThen(toList(), CompletableFutures::allOfUnit))
                            .thenCompose(unit -> {
                                if (StringUtils.isEmpty(nextPageToken)) {
                                    System.out.println("All channels copied from prod");
                                    return completedFuture(null);
                                }

                                return copyChannelsFromProd(projectId, nextPageToken);
                            });
                });
    }

    private CompletableFuture<Void> createFakeChannels(String projectId) {
        return createChannelOnPre(TNotification.newBuilder()
                .setProjectId(projectId)
                .setId(FAKE_EMAIL_CHANNEL)
                .setName(FAKE_EMAIL_CHANNEL)
                .addNotifyAboutStatuses(TEvaluationStatus.ECode.ALARM)
                .setEmail(TEmailType.newBuilder()
                        .addRecipients("solomon-alerts@yandex-team.ru")
                        .build())
                .build())
                .thenCompose(ignore -> createChannelOnPre(TNotification.newBuilder()
                        .setProjectId(projectId)
                        .setId(FAKE_JUGLER_CHANNEL)
                        .setName(FAKE_JUGLER_CHANNEL)
                        .addNotifyAboutStatuses(TEvaluationStatus.ECode.ALARM)
                        .addNotifyAboutStatuses(TEvaluationStatus.ECode.OK)
                        .setJuggler(TJugglerType.newBuilder()
                                .setHost("fake-{{{alert.id}}}")
                                .setService("fake-{{{alert.parent.id}}}")
                                .addTags("from-solomon-alerting-pre")
                                .build())
                        .build()));
    }

    private CompletableFuture<Void> createChannelOnPre(TNotification notification) {
        System.out.println(notification.getProjectId() + " create channel " + notification.getId());
        return pre.createNotification(TCreateNotificationRequest.newBuilder()
                .setNotification(notification)
                .build())
                .exceptionally(e -> {
                    switch (Status.fromThrowable(e).getCode()) {
                        case ALREADY_EXISTS:
                            return null;
                        default:
                            throw new RuntimeException(e);
                    }
                })
                .thenAccept(response -> {});
    }

    private CompletableFuture<Void> deleteAlertsOnPre(String projectId) {
        return pre.listAlerts(TListAlertRequest.newBuilder()
                .setProjectId(projectId)
                .setPageSize(1000)
                .build())
                .thenCompose(response -> {
                    final String nextToken = response.getNextPageToken();
                    System.out.println(projectId + " will delete " + response.getAlertsCount() + " channels");
                    return response.getAlertsList()
                            .stream()
                            .map(TListAlert::getId)
                            .map(id -> deleteAlertOnPre(projectId, id))
                            .collect(Collectors.collectingAndThen(toList(), CompletableFutures::allOfUnit))
                            .thenCompose(unit -> {
                                if (StringUtils.isEmpty(nextToken)) {
                                    System.out.println(projectId + " all alerts deleted");
                                    return completedFuture(null);
                                }

                                return deleteAlertsOnPre(projectId);
                            });
                });
    }

    private CompletableFuture<TDeleteAlertResponse> deleteAlertOnPre(String projectId, String id) {
        System.out.println(projectId + " delete alert " + id);
        return pre.deleteAlert(TDeleteAlertRequest.newBuilder()
                .setProjectId(projectId)
                .setAlertId(id)
                .build());
    }

    private CompletableFuture<Void> copyAlertsFromProd(String projectId, String pageToken, boolean patch) {
        return prod.listAlerts(TListAlertRequest.newBuilder()
                .setProjectId(projectId)
                .setPageToken(pageToken)
                .setPageSize(1000)
                .build())
                .thenCompose(response -> {
                    String nextPageToken = response.getNextPageToken();
                    return response.getAlertsList()
                            .stream()
                            .map(TListAlert::getId)
                            .map(id -> prod.readAlert(TReadAlertRequest.newBuilder()
                                    .setAlertId(id)
                                    .setProjectId(projectId)
                                    .build())
                                    .thenCompose(read -> createAlertOnPre(read.getAlert(), patch)))
                            .collect(collectingAndThen(toList(), CompletableFutures::allOfUnit))
                            .thenCompose(unit -> {
                                if (StringUtils.isEmpty(nextPageToken)) {
                                    System.out.println(projectId + " all alerts copied from prod");
                                    return completedFuture(null);
                                }

                                return copyAlertsFromProd(projectId, pageToken, patch);
                            });
                });
    }

    private CompletionStage<TCreateAlertResponse> createAlertOnPre(TAlert alert, boolean patch) {
        if (patch) {
            alert = patchAlert(alert);
        }

        System.out.println(alert.getProjectId() + " create alert " + alert.getId());
        return pre.createAlert(TCreateAlertRequest.newBuilder()
                .setAlert(alert)
                .build());
    }

    private TAlert patchAlert(TAlert alert) {
        return alert.toBuilder()
                .clearNotificationChannelIds()
                .addNotificationChannelIds(FAKE_EMAIL_CHANNEL)
                .addNotificationChannelIds(FAKE_JUGLER_CHANNEL)
                .build();
    }

    @Override
    public void close() throws Exception {
        prod.close();
        pre.close();
        httpClient.close();
    }
}
