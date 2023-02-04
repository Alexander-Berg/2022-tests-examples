#!/bin/sh
set -e

clickhouse client -n <<-EOSQL
    CREATE DATABASE IF NOT EXISTS logs;
EOSQL

clickhouse client -n <<-EOSQL
CREATE TABLE IF NOT EXISTS logs.logs
(
    _time DateTime CODEC(DoubleDelta,ZSTD),
    _layer         LowCardinality(String),
    _service       String,
    _version       Nullable(String),
    _branch        Nullable(String),
    _canary        Nullable(UInt8),
    _host          Nullable(String),
    _level         LowCardinality(String),
    _request_id    String,
    _uuid          UUID CODEC(NONE),
    _context       Nullable(String),
    _thread        Nullable(String),
    _message       String CODEC(ZSTD(7)),
    _rest          Nullable(String) CODEC(ZSTD(7)),
    _dc            LowCardinality(Nullable(String)),
    _allocation_id Nullable(String),
    _container_id  Nullable(String),
    _time_nano     UInt64,
    INDEX idx_message _message TYPE ngrambf_v1(3, 512, 2, 0) GRANULARITY 4,
    INDEX idx_reqid _request_id TYPE tokenbf_v1(128, 2, 0) GRANULARITY 128
)
ENGINE = ReplacingMergeTree()
PARTITION BY toStartOfHour(_time)
ORDER BY (_layer, _service, _level, _request_id, _time, _uuid)
SETTINGS index_granularity = 8192;
EOSQL