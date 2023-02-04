PY23_LIBRARY()

OWNER(g:walle)

PY_SRCS(
    NAMESPACE sepelib.mongo
    conftest.py
    mock.py
)

PEERDIR(
    contrib/python/mongoengine
    infra/walle/server/contrib/sepelib/mongo
)

END()
