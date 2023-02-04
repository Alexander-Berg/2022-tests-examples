CREATE TABLE photo_cv_hashes (
     hash Uint64,
     photo_cv_hash Utf8,
     user_key Utf8,
     object_id Utf8,
     created_at Timestamp,
     PRIMARY KEY (hash, photo_cv_hash, user_key, object_id)
) WITH (
      UNIFORM_PARTITIONS = 2
);