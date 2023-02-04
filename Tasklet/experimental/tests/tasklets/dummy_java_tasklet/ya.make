JAVA_PROGRAM(dummy-java-tasklet)

JDK_VERSION(11)

OWNER(g:tasklet)

UBERJAR()

JAVA_SRCS(SRCDIR src/main/java **/*.java)

PEERDIR(
    contrib/java/org/json/json/20210307
    tasklet/sdk/v2/java
)

END()

RECURSE_FOR_TESTS(
    src/test
)
