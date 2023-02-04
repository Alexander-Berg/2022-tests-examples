PY23_LIBRARY()

OWNER(g:walle)

PY_SRCS(
    NAMESPACE sepelib.flask
    testcase.py
)

PEERDIR(
    contrib/python/Flask
    contrib/python/Werkzeug
    infra/walle/server/contrib/sepelib/mongo-test
)

END()
