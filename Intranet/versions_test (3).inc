SET(junit.version 5.8.2)
SET(junit-platform.version 1.8.2)

INCLUDE(${ARCADIA_ROOT}/contrib/java/org/junit/junit-bom/${junit.version}/ya.dependency_management.inc)

EXCLUDE(
    contrib/java/junit/junit
    contrib/java/org/junit/vintage/junit-vintage-engine
)

DEPENDENCY_MANAGEMENT(
    contrib/java/org/junit/platform/junit-platform-commons/${junit-platform.version}
    contrib/java/org/junit/platform/junit-platform-engine/${junit-platform.version}
    contrib/java/org/junit/platform/junit-platform-launcher/${junit-platform.version}
)
