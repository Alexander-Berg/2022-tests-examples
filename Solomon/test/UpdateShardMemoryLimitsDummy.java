package ru.yandex.stockpile.server.shard.test;

import org.springframework.stereotype.Component;

import ru.yandex.stockpile.server.shard.UpdateShardMemoryLimits;

/**
 * @author Sergey Polovko
 */
@Component
public class UpdateShardMemoryLimitsDummy implements UpdateShardMemoryLimits {
    @Override
    public void updateShardMemoryLimits() {
    }
}
