from infra.cauth.server.master.importers.servers.sources import HdAbcGroups, GroupResult


SERVICE_TO_FQDNS = {
    1: ['iva1.search.yandex.net', 'sas1.search.yandex.net'],
    2: ['iva3.search.yandex.net'],
}
PRODUCT_1 = 'prod1'
PRODUCT_2 = 'prod2'

SERVICE_TO_SLUG = {
    1: 'market',
    2: 'ysearch',
}

RESPONSIBLES = ['resp1', 'resp2']

IDS = ["zomb-browser", "zomb-browser2"]


def test_bot_import(mock_hd_https):
    mock_hd_https(HD_ANSWER, ABC_ANSWER, CONS_ANSWER)
    HdAbcGroups.SERVICES_PER_REQUEST = 1
    result = HdAbcGroups(None).fetch()
    expected_result = [
        GroupResult(
            name='.'.join((HdAbcGroups.PREFIX, SERVICE_TO_SLUG[1])),
            responsibles={RESPONSIBLES[0]},
            source=HdAbcGroups.PREFIX,
            hosts=[
                GroupResult.Host(hostname=SERVICE_TO_FQDNS[1][0], is_baremetal=False)
            ],
        ),
        GroupResult(
            name='.'.join((HdAbcGroups.PREFIX, SERVICE_TO_SLUG[2])),
            responsibles={PRODUCT_2},
            source=HdAbcGroups.PREFIX,
            hosts=[
                GroupResult.Host(hostname=SERVICE_TO_FQDNS[2][0], is_baremetal=False)
            ],
        ),
    ]
    assert all(it in expected_result for it in result)


HD_ANSWER = """[
    {{
      \"fqdn\" : \"{first_fqdn}\",
      \"login\":  \"{first_login}\"
    }},
    {{
      \"fqdn\" : \"{second_fqdn}\",
      \"login\":  \"{second_login}\"
    }}]""".format(first_fqdn=SERVICE_TO_FQDNS[1][0], first_login=IDS[0],
                  second_fqdn=SERVICE_TO_FQDNS[2][0], second_login=IDS[1])

ABC_ANSWER = [
    {
        "person": {
            "login": RESPONSIBLES[0],
        },
        "service": {
            "id": 1,
            "slug": SERVICE_TO_SLUG[1],
        },
        "role": {
            "id": HdAbcGroups.HARDWARE_MANAGER_ID,
        },
        "resource": {
            "external_id": IDS[0]
        }
    },
    {
        "person": {
            "login": PRODUCT_2,
        },
        "service": {
            "id": 2,
            "slug": SERVICE_TO_SLUG[2],
        },
        "role": {
            "id": HdAbcGroups.HARDWARE_MANAGER_ID,
        },
        "resource": {
            "external_id": IDS[1]
        }
    }
]

CONS_ANSWER = [
    {
        'resource': {'external_id': IDS[0]},
        'service': {'id': 1}
    }, {
        'resource': {'external_id': IDS[1]},
        'service': {'id': 2}
    }
]
