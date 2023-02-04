PY2TEST()

OWNER(g:runtime-infra)

TEST_SRCS(
    test_abc_client.py
    test_abc_client_data_v4.py
    test_bot_client.py
    test_bot_client_data.py
)

PEERDIR(
    infra/rtc/janitor/clients
    contrib/python/pytest
    contrib/python/mock
    infra/rtc/janitor
)

END()
