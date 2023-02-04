CREATE TABLE entity_index (
  hash Uint64,
  entity_type Utf8,
  entity_id Utf8,
  external_id Utf8,
  updated_at Timestamp,
  PRIMARY KEY (hash, entity_type, entity_id, external_id)
) WITH (
  UNIFORM_PARTITIONS = 2
);
