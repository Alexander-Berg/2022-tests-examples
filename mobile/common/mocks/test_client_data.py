SOME_DEVICE = {
    'vendor': 'Fly',
    'models': 'FS454'
}

SOME_UUID = "2516d3cff3ff5de96c2f69d449706123"
SOME_PHONE_ID = "a6804d2d6a56981afa772809285b9989876da7c89872cb5d11a76d8332439460"
SOME_DEVICE_ID = "11111111111111111111111111111111"
SOME_RUSSIAN_IP = "95.108.172.99"

RUSSIAN_OPERATOR = {
    "carrier_name": "Vympelcom",
    "display_name": "Beeline",
    "country_iso": "ru",
    "mcc": 250,
    "mnc": 1,
    "data_roaming": 0,
    "sim_slot_index": 1,
    "is_embedded": 0,
    "icc_id": 89701011687056425190
}

UKRAINIAN_OPERATOR = {
    "carrier_name": "Kyivstar",
    "display_name": "Kyivstar",
    "country_iso": "ua",
    "mcc": 255,
    "mnc": 3,
    "data_roaming": 0,
    "sim_slot_index": 1,
    "is_embedded": 0,
    "icc_id": 89701011687056425190
}

ANDROID_CLIENT_REQUEST_DATA = {
    "os_build": {
        "string_fields": [
            {"key": "BOARD", "value": "goldfish"},
            {"key": "BOOTLOADER", "value": "123"},
            {"key": "MANUFACTURER", "value": SOME_DEVICE['vendor']},
            {"key": "MODEL", "value": SOME_DEVICE['models']},
        ],
        "version": {
            "codename": "The current development codename, or the string 'REL' if this is a release build",
            "incremental": "The internal value used by the underlying source control to represent this build",
            "release": "The user-visible version string",
            "sdk_int": 22,
        }
    },
    "display_metrics": {
        "width_pixels": 720,
        "height_pixels": 1184,
        "density": 2.0,
        "density_dpi": 320,
        "scaled_density": 2.0123,
        "xdpi": 315.31033,
        "ydpi": 318.7451
    },
    "user_settings": {
        "locale": "ru_RU"
    },
    "gl_es_version": 128,
    "gl_extensions": ["extension1", "extension2"],
    "features": ["feature1", "feature2"],
    "shared_libraries": ["library1", "library2"],
    "device_id": SOME_DEVICE_ID,
    "ad_id": "96bd03b6-defc-4203-83d3-dc1c730801f7",
    "android_id": "1af1af1af1af1af1",
    "supported_card_types": {
        "allapps": ["Icon1"],
        "allapps/game": ["Singleapp_card", "Multiapps_card_expandable"],
        "allapps/social": ["Singleapp_card", "Multiapps_card_expandable"],
    },
    "rec_views_config": {
        "feed_view": [
            {
                "card_type": "Scrollable",
                "count": 16
            },
            {
                "card_type": "Single",
                "count": 3
            }
        ],
        "app_rec_view": [
            {
                "card_type": "Multi_apps_Rich",
                "count": 4
            }
        ]
    },
    "clids": {
        "clid1": "9912",
        "clid1010": "1af1af1af1af1af1"
    }
}

MOBILE_NETWORK_CODE = 456
MOBILE_COUNTRY_CODE = 250
MOBILE_NETWORK_CODE_UKRAINE = 1
MOBILE_COUNTRY_CODE_UKRAINE = 255

RUSSIAN_CELL = {
    "country_code": MOBILE_COUNTRY_CODE,
    "operator_id": MOBILE_NETWORK_CODE,
    "cell_id": 789,
    "lac": 1011,
    "signal_strength": -1213,
    "age": 1415,
}

UKRAINIAN_CELL = {
    "country_code": MOBILE_COUNTRY_CODE_UKRAINE,
    "operator_id": MOBILE_NETWORK_CODE_UKRAINE,
    "cell_id": 789,
    "lac": 1011,
    "signal_strength": -1213,
    "age": 1415,
}

LBS_REQUEST_DATA = {
    "location": {
        "latitude": 55.123456,
        "longitude": 37.123456
    },
    "time_zone": {
        "name": "Europe/Moscow",
        "utc_offset": 3600,
    },
    "cells": [
        RUSSIAN_CELL
    ],
    "wifi_networks": [
        {
            "mac": "12-34-56-78-9A-BC",
            "signal_strength": -2,
            "age": 3,
        },
    ]
}
