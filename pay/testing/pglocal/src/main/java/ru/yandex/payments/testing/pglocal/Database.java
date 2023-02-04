package ru.yandex.payments.testing.pglocal;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;

import lombok.Getter;
import lombok.val;

@Getter
public class Database {
    private static final int QUERY_TIMEOUT_SEC = 5;

    private final String name;
    private final int port;
    private final String user;
    private final String connString;

    public Database(String name, int port, String user) {
        this.name = name;
        this.port = port;
        this.user = user;
        this.connString = String.format("jdbc:postgresql://localhost:%d/%s?user=%s",
                port, name, user);
    }

    private Connection connect() throws SQLException {
        return DriverManager.getConnection(connString);
    }

    public void execute(String query) {
        execute(query, QUERY_TIMEOUT_SEC);
    }

    public void execute(String query, int timeoutSec) {
        try (val connection = connect();
             val statement = connection.createStatement()) {
            statement.setQueryTimeout(timeoutSec);
            statement.execute(query);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @FunctionalInterface
    public interface Fetcher<T> {
        T fetch(ResultSet resultSet) throws SQLException;
    }

    public <T> T fetch(String query, Fetcher<T> fetcher) {
        return fetch(query, QUERY_TIMEOUT_SEC, fetcher);
    }

    public <T> T fetch(String query, int timeoutSec, Fetcher<T> fetcher) {
        try (val connection = connect();
             val statement = connection.createStatement()) {
            statement.setQueryTimeout(timeoutSec);
            val resultSet = statement.executeQuery(query);
            return fetcher.fetch(resultSet);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
