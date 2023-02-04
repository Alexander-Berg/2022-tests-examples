PY3TEST()

OWNER(g:maps-nmaps)

SIZE(MEDIUM)

PEERDIR(
    maps/wikimap/stat/libs/nile_ut
    maps/wikimap/stat/tasks_payment/dictionaries/puid_map/lib
    statbox/nile
)

TEST_SRCS(
    outsource_company.py
    puid_map.py
)

END()
