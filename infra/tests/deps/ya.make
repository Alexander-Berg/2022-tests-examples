PACKAGE()

OWNER(g:awacs)

DEPENDS(jdk)

# https://sandbox.yandex-team.ru/resource/2450512956/view 252-1
# https://sandbox.yandex-team.ru/resource/2942818576/view 280-1
FROM_SANDBOX(
    FILE
    2450512956
    EXECUTABLE RENAME pack/balancer OUT_NOAUTO balancer
)

# https://docs.yandex-team.ru/ya-make/manual/common/data
# https://sandbox.yandex-team.ru/resources?type=ZOOKEEPER_ARCHIVE&limit=100&offset=0
# https://a.yandex-team.ru/arc_vcs/sandbox/projects/common/environments/__init__.py?rev=r9357898#L894
# https://sandbox.yandex-team.ru/resource/84213842/view
FROM_SANDBOX(
    FILE
    84213842
    OUT_NOAUTO zookeeper.tar.gz
)

# https://a.yandex-team.ru/arc_vcs/sandbox/projects/common/environments/__init__.py?rev=r9357964#L916
# https://sandbox.yandex-team.ru/resource/243667290/view
FROM_SANDBOX(
    FILE
    243667290
    EXECUTABLE OUT_NOAUTO mongod
)

# https://sandbox.yandex-team.ru/resource/3150780942/view
FROM_SANDBOX(
    FILE
    3150780942
    EXECUTABLE OUT_NOAUTO stdbuf
)

END()
