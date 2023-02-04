PY3TEST()

OWNER(g:maps-nmaps)

TEST_SRCS(
    edit_tests.py
    tests.py
    util.py
)

PEERDIR(
    contrib/python/PyYAML
    library/python/resource
    maps/wikimap/stat/tasks_payment/dictionaries/tariff/data
    maps/wikimap/stat/tasks_payment/dictionaries/tariff/schema
)

DEPENDS(
    maps/wikimap/mapspro/cfg/editor
)

END()
