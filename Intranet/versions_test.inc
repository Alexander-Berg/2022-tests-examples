SET(junit.version 5.8.2)
SET(junit-platform.version 1.8.2)
SET(mockserver.version 5.11.2)
SET(jsr305.version 3.0.2)
SET(json-schema-validator.version 2.2.14)
SET(swagger.version 2.1.11)
# Temporary workaround for missing xmlunit-placeholders-2.8.4
SET(xmlunit.version 2.8.3)
INCLUDE(${ARCADIA_ROOT}/contrib/java/org/junit/junit-bom/${junit.version}/ya.dependency_management.inc)

EXCLUDE(
    contrib/java/junit/junit
    contrib/java/org/junit/vintage/junit-vintage-engine
    contrib/java/javax/activation/activation
    contrib/java/io/swagger/swagger-core
    contrib/java/com/github/fge/btf
    contrib/java/com/github/fge/jackson-coreutils
    contrib/java/com/github/fge/msg-simple

    library/java/tvmauth
)

DEPENDENCY_MANAGEMENT(
    contrib/java/org/junit/platform/junit-platform-commons/${junit-platform.version}
    contrib/java/org/junit/platform/junit-platform-engine/${junit-platform.version}
    contrib/java/org/junit/platform/junit-platform-launcher/${junit-platform.version}
    contrib/java/org/mock-server/mockserver-netty/${mockserver.version}
    contrib/java/org/mock-server/mockserver-client-java/${mockserver.version}
    contrib/java/org/mock-server/mockserver-core/${mockserver.version}
    contrib/java/org/mock-server/mockserver-junit-jupiter/${mockserver.version}
    contrib/java/com/google/code/findbugs/jsr305/${jsr305.version}
    contrib/java/com/github/java-json-tools/json-schema-validator/${json-schema-validator.version}
    contrib/java/io/swagger/core/v3/swagger-annotations/${swagger.version}
    contrib/java/io/swagger/core/v3/swagger-models/${swagger.version}
    contrib/java/io/swagger/core/v3/swagger-core/${swagger.version}
    # Temporary workaround for missing xmlunit-placeholders-2.8.4
    contrib/java/org/xmlunit/xmlunit-core/${xmlunit.version}
    contrib/java/org/xmlunit/xmlunit-placeholders/${xmlunit.version}
)

