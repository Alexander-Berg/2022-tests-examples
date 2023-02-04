PY2_PROGRAM()

PEERDIR(
    infra/clusterstate/libraries
)

PY_MAIN(infra.clusterstate.test_libraries.main)

PY_SRCS(
    main.py
)

OWNER(okats)

END()
