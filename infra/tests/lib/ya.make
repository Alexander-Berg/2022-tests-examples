PY3_LIBRARY()

STYLE_PYTHON()

OWNER(g:walle)

PY_SRCS(
    conftest.py
    dns.py
    util.py
    api_util.py
    expert_util.py
    idm_util.py
    rules_util.py
    maintenance_plot_util.py
    scenario_util.py
)

RESOURCE_FILES(
    ../../walle/templates/test.html
    mocks/bot-oebs-projects.json
    mocks/bot-preorders.json
    mocks/certificator-certificate-response.txt
    mocks/certificator-response.json
    mocks/net-layout.xml
    mocks/netmon-alive.json
    mocks/netmon-seen-hosts.json
    mock_health_data.json
)

PEERDIR(
    contrib/python/deepdiff
    contrib/python/mock
    contrib/python/six
    infra/walle/server/contrib/sepelib/flask-test
    infra/walle/server/contrib/sepelib/mongo-test
    infra/walle/server/walle
    library/python/resource
)

END()
