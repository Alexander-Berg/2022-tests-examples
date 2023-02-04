package ru.yandex.solomon.alert.dao.memory;

import javax.annotation.ParametersAreNonnullByDefault;

import com.fasterxml.jackson.databind.ObjectMapper;

import ru.yandex.solomon.alert.dao.codec.AlertCodec;
import ru.yandex.solomon.alert.dao.codec.AlertRecord;
import ru.yandex.solomon.alert.domain.Alert;

/**
 * @author Ivan Tsybulin
 */
@ParametersAreNonnullByDefault
public class InMemoryAlertsDao extends InMemoryEntitiesDao<Alert, AlertRecord> {
    public InMemoryAlertsDao() {
        super(new AlertCodec(new ObjectMapper()));
    }
}
