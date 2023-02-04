JTEST()

JDK_VERSION(11)

OWNER(violin)

NO_LINT()

PEERDIR(
    infra/qloud/kikimr-logs-gateway/lib

    contrib/java/junit/junit/4.12
    contrib/java/ru/yandex/qe/commons-spring/6.3039
)

EXCLUDE(
    contrib/java/org/hamcrest/hamcrest-core/1.1
)

JAVA_SRCS(SRCDIR java **/*)

SIZE(SMALL)

END()
