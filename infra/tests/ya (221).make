PY2TEST()

OWNER(g:infra)

TEST_SRCS(
    test_script.py
    test_job.py
    hosts_data_source.py
)

DATA(
    arcadia/infra/rtc/rebootctl/lib/tests/all-tasks.yaml
)

PEERDIR(
    infra/rtc/rebootctl/lib
    infra/rtc/rebootctl/proto
)

END()
