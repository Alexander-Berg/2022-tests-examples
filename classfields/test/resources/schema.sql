CREATE TABLE scores
(
    puid Uint64,
    score Uint8,
    update_ts Timestamp,
    PRIMARY KEY(puid)
);

CREATE TABLE summary
(
    puid Uint64,
    passport_registration_datetime Timestamp,
    passport_karma Uint8,
    passport_karma_allow_until Timestamp,
    passport_karma_status Uint8,
    passport_phone_is_confirmed Bool,
    passport_phone_is_bound Bool,
    passport_data_update_ts Timestamp,
    yt_personal_phone_ids Utf8,
    yt_reviews_user Bool,
    yt_reviews_activity Bool,
    yt_disk_activity Bool,
    yt_music_activity Bool,
    yt_kinopoisk_user Bool,
    yt_kinopoisk_activity Bool,
    yt_taxi_user_blocked Bool,
    yt_taxi_activity Bool,
    yt_taxi_user Bool,
    yt_lavka_user_blocked Bool,
    yt_lavka_activity Bool,
    yt_lavka_user Bool,
    yt_eda_user_blocked Bool,
    yt_eda_activity Bool,
    yt_eda_user Bool,
    yt_data_update_ts Timestamp,
    plus_native_auto_subscription_activity Bool,
    plus_data_update_ts Timestamp,
    PRIMARY KEY(puid)
);