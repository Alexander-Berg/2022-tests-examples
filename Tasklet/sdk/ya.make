JUNIT5()

JDK_VERSION(11)

SIZE(SMALL)

JAVA_SRCS(SRCDIR java **/*)
JAVA_SRCS(SRCDIR resources **/*)

PEERDIR(
    tasklet/sdk/v2/java
    tasklet/sdk/v2/java/src/test/proto
)

END()

RECURSE(
    proto
)
