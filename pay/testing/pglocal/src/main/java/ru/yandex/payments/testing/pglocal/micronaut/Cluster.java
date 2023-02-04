package ru.yandex.payments.testing.pglocal.micronaut;

import java.util.List;

import ru.yandex.payments.testing.pglocal.Server;

record Cluster(Node master,
               List<Node> slaves) {

    static record Node(Server server,
                       String name) {
    }
}
