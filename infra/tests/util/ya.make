PY3_LIBRARY()

OWNER(g:runtime-infra)

PY_SRCS(
    mock_database.py
    mock_mds.py
    mock_registry.py
    mock_config.py
    mock_blackbox.py
    test_data.py
)

PEERDIR(
    contrib/python/requests
    contrib/python/psycopg2
)

END()
