package ru.yandex.solomon.alert.dao.memory;

import javax.annotation.ParametersAreNonnullByDefault;

import com.fasterxml.jackson.databind.ObjectMapper;

import ru.yandex.solomon.alert.dao.codec.MuteCodec;
import ru.yandex.solomon.alert.dao.codec.MuteRecord;
import ru.yandex.solomon.alert.mute.domain.Mute;

/**
 * @author Ivan Tsybulin
 */
@ParametersAreNonnullByDefault
public class InMemoryMutesDao extends InMemoryEntitiesDao<Mute, MuteRecord> {
    public InMemoryMutesDao() {
        super(new MuteCodec(new ObjectMapper()));
    }
}
