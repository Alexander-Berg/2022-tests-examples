PHRASEID_PATH = "//tmp/md5path"
TURBOURL_PATH = "//tmp/TurboUrlDict"
OFFER_LISTINGS_PATH = "//tmp/OfferListingDict"
COMMUNICATIONS_CHANNEL_PATH = "//tmp/CommunicationsChannel"

TABLES = {
    PHRASEID_PATH: {
        "dynamic": True,
        "schema": [
            {"name": "DataMD5", "type": "uint64", "sort_order": "ascending"},
            {"name": "PhraseID", "type": "uint64"},
        ],
    },
    TURBOURL_PATH: {
        "dynamic": True,
        "schema": [
            {"name": "Hash", "type": "uint64", "sort_order": "ascending"},
            {"name": "OriginalUrl", "type": "string", "sort_order": "ascending"},
            {"name": "TurboUrl", "type": "string"},
        ],
    },
    OFFER_LISTINGS_PATH: {
        "dynamic": True,
        "schema": [
            {"name": "Hash", "type": "uint64", "sort_order": "ascending"},
            {"name": "ID", "type": "string", "sort_order": "ascending"},
            {"name": "TurboShopID", "type": "string", "sort_order": "ascending"},
            {"name": "CanonizedUrl", "type": "string"},
        ],
    },
    COMMUNICATIONS_CHANNEL_PATH: {
        "dynamic": True,
        "schema": [
            {"name": "Channel", "type": "uint64", "sort_order": "ascending"},
            {"name": "MessageId", "type": "uint64", "sort_order": "ascending"},
            {"name": "Codec", "type": "string"},
            {"name": "EventId", "type": "uint64"},
            {"name": "Uid", "type": "uint64"},
            {"name": "Source", "type": "string"},
            {"name": "Created", "type": "uint64"},
            {"name": "Expired", "type": "uint64"},
            {"name": "Data", "type": "string"},
            {"name": "TargetEntityId", "type": "uint64"},
            {"name": "NextShowTime", "type": "uint64"},
            {"name": "Statuses", "type": "uint64"},
            {"name": "Slots", "type": "uint64"},
        ],
    },
}
