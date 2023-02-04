EXECTEST()

OWNER(g:yatool)

RUN(check_json --recurse --weak-match *.json)

DATA(arcadia/fuzzing)

TEST_CWD(fuzzing)

DEPENDS(tools/check_json/bin)

END()
