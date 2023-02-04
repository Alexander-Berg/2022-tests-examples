import os

QUERY_IDS_PARAMS_LIST = (
    [  # https://a.yandex-team.ru/arc/trunk/arcadia/ads/bsyeti/eagle/collect/collect.cpp?rev=4661631#L144
        "bigb-uid",
        "crypta-id",
        "gaid",
        "google-ad-id",
        "idfa",
        "puid",
        "uuid",
        "mac",
        "device-id",
        "duid",
    ]
)
QUERY_IDS_PARAMS_PRIORITY = {e: i for i, e in enumerate(QUERY_IDS_PARAMS_LIST)}
QUERY_IDS_PARAMS = set(QUERY_IDS_PARAMS_LIST)
SPECIAL_QUERY_PARAMS = set(["client", "keyword-set"])
ALLOWED_QUERY_KEYS = QUERY_IDS_PARAMS | SPECIAL_QUERY_PARAMS

EAGLE_TO_YT_PREFIXES = {
    "bigb-uid": ["y"],
    "crypta-id": ["y"],
    "gaid": ["gaid/"],
    "google-ad-id": ["gaid/"],
    "idfa": ["idfa/"],
    "puid": ["p"],
    "uuid": ["uuid/"],
    "mac": ["mac/"],
    "device-id": ["idfa/", "gaid/"],
    "duid": ["duid/"],
}

TARGET_CLUSTER_LOCATION = "sas"

IS2_URL = "http://%s.id.crypta.yandex.net" % TARGET_CLUSTER_LOCATION
IS2_SERVICE_NAME = "is2"
IS2_TVM_ID = 2000525
IS2_TVM_DICT = {IS2_SERVICE_NAME: IS2_TVM_ID}

BIGB_TVM_ID = int(os.environ.get("BIGB_TVM_ID", "0").strip())
BIGB_TVM_SECRET = os.environ.get("BIGB_TVM_SECRET", "").strip()

YT_USER = os.environ.get("YT_USER", "").strip()
YT_TOKEN = os.environ.get("YT_TOKEN", "").strip()
YT_CLUSTER = "seneca-%s" % TARGET_CLUSTER_LOCATION

YT_PROFILE_TABLE = "//home/bigb/production/Profiles"
YT_REMOTE_TEST_TABLE = "//home/bigb/production/RemoteTestProfiles"
YT_USER_SHOW_TABLE = "//home/bigb/production/UserShows"
YT_VULTURE_CRYPTA_TABLE = "//home/bigb/production/VultureCrypta"
YT_COOKIES_TABLE = "//home/bigb/production/Cookies"
YT_SEARCH_PERS_TABLE = "//home/searchpers/production/States"
YT_OFFSETS_TABLE = "//home/bigb/production/Offsets"
YT_CRYPTA_REPLICAS_TABLE = "//home/bigb/crypta_replicas/replica"

YT_INPUT_QUEUE_PATH = "//tmp/input_queue"
YT_URL_TO_OFFER_EXPORT_PATH = "//tmp/UrlToOfferExport"

YT_QUEUES_TESTDATA_FOLDER = "//home/bigb/bt-logs/yt-data"
LB_BTLOGS_TESTDATA_FOLDER = "//home/bigb/bt-logs/lb-data"

TESTDATA_DIR = "testdata_folder"

PROFILES_SCHEMA_PATH = TESTDATA_DIR + "/profiles_schema.pkl"
USER_SHOWS_SCHEMA_PATH = TESTDATA_DIR + "/user_shows_schema.pkl"
VULTURE_CRYPTA_SCHEMA_PATH = TESTDATA_DIR + "/vulture_crypta_schema.pkl"
COOKIES_SCHEMA_PATH = TESTDATA_DIR + "/cookies_schema.pkl"
SEARCH_PERS_SCHEMA_PATH = TESTDATA_DIR + "/search_pers_schema.pkl"

PROFILES_TABLE_PATH = TESTDATA_DIR + "/profiles_table.pkl"
USER_SHOWS_TABLE_PATH = TESTDATA_DIR + "/user_shows.pkl"
VULTURE_CRYPTA_TABLE_PATH = TESTDATA_DIR + "/vulture_crypta_table.pkl"
COOKIES_TABLE_PATH = TESTDATA_DIR + "/cookies_table.pkl"
SEARCH_PERS_TABLE_PATH = TESTDATA_DIR + "/search_pers_table.pkl"

TIMESTAMP_PATH = TESTDATA_DIR + "/timestamp"
IS2_REPSONSES_PATH = TESTDATA_DIR + "/is2_resps.pkl"

REQUESTS_DIR = TESTDATA_DIR + "/access_log_requests"
COMMON_REQUESTS = TESTDATA_DIR + "/common_requests"

SANDBOX_OUTPUT_RESOURCE_TYPE = "BIGB_EAGLE_TESTDATA"
SANDBOX_BUZZARD_OUTPUT_RESOURCE_TYPE = "BIGB_BUZZARD_TESTDATA"

MULTIHASH_MODULO = 13841
MULTIHASH_SALT = 666

BUZZARD_QYT_TESTDATA = TESTDATA_DIR + "/qyt_testdata"
BUZZARD_LB_CONFIG = {
    "resharded_lb_log": ("resharded-bt-log", TESTDATA_DIR + "/resharded_bt_log.messages"),
    "resharded_realtime_lb_log": (
        "resharded-realtime-bt-log",
        TESTDATA_DIR + "/resharded_realtime_bt_log.messages",
    ),
}
BUZZARD_LB2YT_DATA = TESTDATA_DIR + "/lb2yt_data"
BUZZARD_YT_DATA_DIR = TESTDATA_DIR + "/yt_data"
BUZZARD_YT_REQUESTS_DIR = TESTDATA_DIR + "/yt_requests"

ALL_YT_TABLES_CONFIG = {
    "Profiles": (YT_PROFILE_TABLE, PROFILES_SCHEMA_PATH, PROFILES_TABLE_PATH),
    "RemoteTestProfiles": (YT_REMOTE_TEST_TABLE, "", ""),
    "UserShows": (YT_USER_SHOW_TABLE, USER_SHOWS_SCHEMA_PATH, USER_SHOWS_TABLE_PATH),
    "VultureCrypta": (
        YT_VULTURE_CRYPTA_TABLE,
        VULTURE_CRYPTA_SCHEMA_PATH,
        VULTURE_CRYPTA_TABLE_PATH,
    ),
    "Cookies": (YT_COOKIES_TABLE, COOKIES_SCHEMA_PATH, COOKIES_TABLE_PATH),
    "SearchPers": (YT_SEARCH_PERS_TABLE, SEARCH_PERS_SCHEMA_PATH, SEARCH_PERS_TABLE_PATH),
    "Offsets": (YT_OFFSETS_TABLE, "", ""),
    "CryptaReplicas": (YT_CRYPTA_REPLICAS_TABLE, "", ""),
}

DEBUG_CLIENTS = set(
    [
        "debug",
    ]
)

YABS_MAIN_CLIENTS = DEBUG_CLIENTS | set(["ks_1", "yabs", "search", "ssp", "yabs-market"])

ADS_SERVER_CLIENTS = YABS_MAIN_CLIENTS | set(
    [
        "server_count",
        "distribution_main_page",
        "adfox",
    ]
)

PROFILES_SCHEMA = [
    {
        "name": "Hash",
        "expression": "farm_hash(UniqID) % 768",
        "type": "uint64",
        "sort_order": "ascending",
    },
    {"name": "UniqID", "type": "string", "sort_order": "ascending"},
    {"name": "CodecID", "type": "uint64"},
    {"name": "Main", "type": "string"},
    {"name": "MainPatch", "type": "string"},
    {"name": "UserItems", "type": "string"},
    {"name": "UserItemsPatch", "type": "string"},
    {"name": "Counters", "type": "string"},
    {"name": "CountersPatch", "type": "string"},
    {"name": "Applications", "type": "string"},
    {"name": "ApplicationsPatch", "type": "string"},
    {"name": "Banners", "type": "string"},
    {"name": "BannersPatch", "type": "string"},
    {"name": "Dmps", "type": "string"},
    {"name": "DmpsPatch", "type": "string"},
    {"name": "Queries", "type": "string"},
    {"name": "QueriesPatch", "type": "string"},
    {"name": "Aura", "type": "string"},
    {"name": "AuraPatch", "type": "string"},
    {"name": "DjProfiles", "type": "string"},
    {"name": "DjProfilesPatch", "type": "string"},
]

SEARCH_PERS_SCHEMA = [
    {
        "name": "Hash",
        "expression": "bigb_hash(Id) % 256",
        "type": "uint64",
        "sort_order": "ascending",
    },
    {"name": "Id", "type": "string", "sort_order": "ascending"},
    {"name": "Codec", "type": "uint64"},
    {"name": "State", "type": "string"},
    {"name": "StatePatch", "type": "string"},
    {"name": "CodecString", "type": "string"},
]
