import json
from mock import Mock
import pytest

from staff.oebs.controllers.datasources import HeadcountPositionsDatasource
from staff.oebs.models import HeadcountPosition
from staff.oebs.constants import REPLACEMENT_TYPE

data = """
{
    "detOrgLimits": [{
            "global_org_id": 1163,
            "totalHC": 5,
            "relevanceDate": "2018-12-13",
            "states": [{
                    "state1": "OCCUPIED",
                    "state2": "REPLACEMENT",
                    "headCount": 4,
                    "crossHeadCount": 0,
                    "positions": [{
                            "code": 3219,
                            "name": "Дизайнер продукта",
                            "productID": 100001216,
                            "geo": "RUSc",
                            "rwsBonusID": 1,
                            "rwsRewardID": 1,
                            "rwsReviewID": 1,
                            "loginCurr": "23di",
                            "loginPrev": "tigr",
                            "isCrossHc": false,
                            "hc": 1
                        }, {
                            "code": 4960,
                            "name": "Дизайнер",
                            "productID": 100000316,
                            "geo": "RUSc",
                            "rwsBonusID": 2,
                            "rwsRewardID": 2,
                            "rwsReviewID": 2,
                            "loginCurr": "stereolya",
                            "loginPrev": "chizh",
                            "isCrossHc": false,
                            "hc": 1
                        }, {
                            "code": 9206,
                            "name": "Cтарший дизайнер продукта",
                            "productID": 100001216,
                            "geo": "RUSc",
                            "rwsBonusID": 3,
                            "rwsRewardID": 3,
                            "rwsReviewID": 3,
                            "loginCurr": "dmithree",
                            "loginPrev": "vinastya",
                            "isCrossHc": false,
                            "hc": 1
                        }, {
                            "code": 23914,
                            "name": "Дизайнер",
                            "productID": 100001216,
                            "geo": "RUSc",
                            "rwsBonusID": 4,
                            "rwsRewardID": 4,
                            "rwsReviewID": 4,
                            "loginCurr": "kuchuganovaa",
                            "loginPrev": "juananeva",
                            "isCrossHc": false,
                            "hc": 1
                        }
                    ]
                }, {
                    "state1": "VACANCY_OPEN",
                    "state2": "REPLACEMENT",
                    "headCount": 1,
                    "crossHeadCount": 0,
                    "positions": [{
                            "code": 23100,
                            "name": "Дизайнер продукта",
                            "productID": 100001216,
                            "geo": "RUSc",
                            "rwsBonusID": 8,
                            "rwsRewardID": 8,
                            "rwsReviewID": 8,
                            "loginCurr": "thejuly",
                            "loginPrev": "kovbasa",
                            "loginPrev2": null,
                            "isCrossHc": false,
                            "hc": 1
                        }
                    ]
                }
            ]
        }
    ]
}
"""

data_update = (
    '{'
    '"detOrgLimits": [{'
    '    "global_org_id": 1163,'
    '    "totalHC": 5,'
    '    "relevanceDate": "2018-12-13",'
    '    "states": [{'
    '        "state1": "OCCUPIED",'
    '        "state2": "REPLACEMENT",'
    '        "headCount": 4,'
    '        "crossHeadCount": 0,'
    '        "positions": [{'
    '            "code": 3219,'
    '            "name": "Дизайнер продукта",'
    '            "productID": 100001216,'
    '            "geo": "RUSc",'
    '            "rwsBonusID": 9,'
    '            "rwsRewardID": 9,'
    '            "rwsReviewID": 9,'
    '            "loginCurr": "23di",'
    '            "loginPrev": "tigr",'
    '            "loginPrev2": null,'
    '            "isCrossHc": false,'
    '            "hc": 1'
    '        }]'
    '    }]'
    '}]'
    '}'
)


def test_headcounts_datasource():
    datasource = HeadcountPositionsDatasource(HeadcountPosition.oebs_type, HeadcountPosition.method, Mock())
    datasource._data = json.loads(data)
    result = list(datasource)
    assert 5 == len(result)


@pytest.mark.django_db
def test_headcounts_updater(build_updater):
    datasource = HeadcountPositionsDatasource(HeadcountPosition.oebs_type, HeadcountPosition.method, Mock())
    datasource._data = json.loads(data)

    updater = build_updater(model=HeadcountPosition, datasource=datasource)
    results = updater.run_sync()
    assert 5 == results['created']
    assert 5 == results['all']

    headcount_positions = list(HeadcountPosition.objects.all())
    assert 5 == len(headcount_positions)

    datasource = HeadcountPositionsDatasource(HeadcountPosition.oebs_type, HeadcountPosition.method, Mock())
    datasource._data = json.loads(data_update)

    updater = build_updater(model=HeadcountPosition, datasource=datasource)
    updater.run_sync()
    headcount_positions = list(HeadcountPosition.objects.all())
    assert 1 == len(headcount_positions)


position_variants = [
    ({'isTotalHc': False, 'prevIndx': None, 'nextIndx': 1}, REPLACEMENT_TYPE.HAS_REPLACEMENT),
    ({'isTotalHc': True, 'prevIndx': 1, 'nextIndx': None}, REPLACEMENT_TYPE.HC_IS_BUSY),
    ({'isTotalHc': True, 'prevIndx': None, 'nextIndx': None}, REPLACEMENT_TYPE.WO_REPLACEMENT),
    ({'isTotalHc': False, 'prevIndx': 1, 'nextIndx': 2}, REPLACEMENT_TYPE.HAS_REPLACEMENT_AND_HC_IS_BUSY),
]


@pytest.mark.parametrize('position,result', position_variants)
def test_extract_replacement_type(position, result):
    assert HeadcountPositionsDatasource._extract_replacement_type(position) == result
