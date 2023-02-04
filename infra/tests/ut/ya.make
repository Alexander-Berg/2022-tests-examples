PY3_LIBRARY()

OWNER(g:rtc-sysdev)

# tests splited into 8 parts in bb
# https://bb.yandex-team.ru/projects/PORTO/repos/porto/browse/test/test-parts.py

TEST_SRCS(
    conftest.py
    porto_tests.py
)

END()
