CREATE TABLE indexed_phones (
                                 hash Uint64,
                                 indexed_entity Utf8,
                                 user_key Utf8,
                                 object_id Utf8,
                                 PRIMARY KEY (hash, indexed_entity, user_key, object_id)
) WITH (
      UNIFORM_PARTITIONS = 2
      );