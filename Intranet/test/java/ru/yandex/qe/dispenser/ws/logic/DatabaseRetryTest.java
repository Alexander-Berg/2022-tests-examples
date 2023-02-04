package ru.yandex.qe.dispenser.ws.logic;

import java.sql.SQLException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.postgresql.util.PSQLException;
import org.postgresql.util.PSQLState;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import ru.yandex.qe.dispenser.domain.dao.DiJdbcTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class DatabaseRetryTest {

    private static final int DEFAULT_RETURN_VALUE = 42;

    private JdbcTemplate jdbcTemplateOrigin;
    private NamedParameterJdbcTemplate jdbcTemplate;
    private NamedParameterJdbcTemplate readOnlyJdbcTemplate;
    private DiJdbcTemplate diJdbcTemplate;
    public static final DataAccessResourceFailureException RETRIABLE_EXCEPTION = new DataAccessResourceFailureException("EOF", new PSQLException("EOF", PSQLState.CONNECTION_FAILURE));

    @BeforeEach
    public void setup() {
        jdbcTemplateOrigin = Mockito.mock(JdbcTemplate.class);
        jdbcTemplate = Mockito.mock(NamedParameterJdbcTemplate.class);
        readOnlyJdbcTemplate = Mockito.mock(NamedParameterJdbcTemplate.class);
        diJdbcTemplate = new DiJdbcTemplate(jdbcTemplate, readOnlyJdbcTemplate, jdbcTemplateOrigin, 1, 0, 0);
    }

    @Test
    public void wontRetryWhenNotTransientExceptionIsThrown() {
        assertThrows(BadSqlGrammarException.class, () -> {
            setupExceptionSequence(new BadSqlGrammarException("", "", new SQLException()));
            diJdbcTemplate.update("");
        });
    }

    @Test
    public void willRetryAfterConnectionProblems() {
        setupExceptionSequence(RETRIABLE_EXCEPTION);
        assertEquals(DEFAULT_RETURN_VALUE, diJdbcTemplate.update(""));
    }

    @Test
    public void willReturnExpectedValueWhenNoExceptionIsThrown() {
        setupNormalWork();
        assertEquals(DEFAULT_RETURN_VALUE, diJdbcTemplate.update(""));
    }

    @Test
    public void willRetryBatchUpdateWhenTransientExceptionIsThrown() {
        assertThrows(Success.class, () -> {
            setupExceptionSequence(RETRIABLE_EXCEPTION);
            diJdbcTemplate.batchUpdate("", "");
        });
    }

    @Test
    public void willExecuteBatchUpdateWhenNoExceptionIsThrown() {
        assertThrows(Success.class, () -> {
            setupNormalWork();
            diJdbcTemplate.batchUpdate("", "");
        });
    }

    @Test
    public void willRetryExecutionWhenTransientExceptionIsThrown() {
        setupExceptionSequence(RETRIABLE_EXCEPTION);
        assertEquals(DEFAULT_RETURN_VALUE, (int) diJdbcTemplate.execute(Mockito.mock(ConnectionCallback.class)));
    }

    @Test
    public void willExecuteSuccessfullyWhenNoExceptionIsThrown() {
        setupNormalWork();
        assertEquals(DEFAULT_RETURN_VALUE, (int) diJdbcTemplate.execute(Mockito.mock(ConnectionCallback.class)));
    }


    @SuppressWarnings("unchecked")
    @SafeVarargs
    private final void setupExceptionSequence(final Throwable... throwable) {
        Mockito.when(jdbcTemplate.update(Mockito.anyString(), Mockito.anyMap())).thenThrow(throwable).thenReturn(DEFAULT_RETURN_VALUE);
        Mockito.when(jdbcTemplateOrigin.batchUpdate(Mockito.any())).thenThrow(throwable).thenThrow(Success.class);
        Mockito.when(jdbcTemplateOrigin.execute(Mockito.any(ConnectionCallback.class))).thenThrow(throwable).thenReturn(DEFAULT_RETURN_VALUE);

    }

    @SuppressWarnings("unchecked")
    private void setupNormalWork() {
        Mockito.when(jdbcTemplate.update(Mockito.anyString(), Mockito.anyMap())).thenReturn(DEFAULT_RETURN_VALUE);
        Mockito.when(jdbcTemplateOrigin.batchUpdate(Mockito.any())).thenThrow(Success.class);
        Mockito.when(jdbcTemplateOrigin.execute(Mockito.any(ConnectionCallback.class))).thenReturn(DEFAULT_RETURN_VALUE);
    }

    private static class Success extends RuntimeException {
    }

}
