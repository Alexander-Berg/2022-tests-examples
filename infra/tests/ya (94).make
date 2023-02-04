EXECTEST()

OWNER(
    g:kernel
)

FORK_TESTS()

DEPENDS(
	infra/kernel/tools/coroner
	devtools/dummy_arcadia/recipe/path_preload
	library/go/test/mutable_testdata
)

DATA(arcadia/infra/kernel/tools/coroner/tests)

USE_RECIPE(devtools/dummy_arcadia/recipe/path_preload/path_preload /bin/zcat)

USE_RECIPE(
    library/go/test/mutable_testdata/mutable_testdata
    --testdata-dir
    infra/kernel/tools/coroner/tests
)

RUN(
    NAME dmesg1.log
    STDOUT ${TEST_OUT_ROOT}/stdout.txt
    CANONIZE_LOCALLY ${TEST_OUT_ROOT}/stdout.txt
    coroner data/dmesg1.log --json
)


RUN(
    NAME dmesg1.log.gz
    STDOUT ${TEST_OUT_ROOT}/stdout.txt
    CANONIZE_LOCALLY ${TEST_OUT_ROOT}/stdout.txt
    coroner data/dmesg1.log.gz --json
)

RUN(
    NAME dmesg1.json
    STDOUT ${TEST_OUT_ROOT}/stdout.txt
    CANONIZE_LOCALLY ${TEST_OUT_ROOT}/stdout.txt
    coroner -e data/dmesg1.json --json
)

RUN(
    NAME dmesg2.log
    STDOUT ${TEST_OUT_ROOT}/stdout.txt
    CANONIZE_LOCALLY ${TEST_OUT_ROOT}/stdout.txt
    coroner data/dmesg2.log --json
)

RUN(
    NAME dmesg2.json

    STDOUT ${TEST_OUT_ROOT}/stdout.txt
    CANONIZE_LOCALLY ${TEST_OUT_ROOT}/stdout.txt
    coroner -e data/dmesg2.json --json
)

RUN(
    NAME dmesg3.log

    STDOUT ${TEST_OUT_ROOT}/stdout.txt
    CANONIZE_LOCALLY ${TEST_OUT_ROOT}/stdout.txt
    coroner data/dmesg3.log --json
)

END()

