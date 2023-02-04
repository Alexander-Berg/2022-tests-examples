SET(jupiter-version 5.8.2)
INCLUDE(${ARCADIA_ROOT}/contrib/java/org/junit/junit-bom/${jupiter-version}/ya.dependency_management.inc)
INCLUDE(${KOTLIN_BOM_FILE})

EXCLUDE(
    contrib/java/junit/junit
    contrib/java/javax/ws/rs/jsr311-api
    contrib/java/log4j/log4j
    contrib/java/org/ow2/asm/asm-all
)
