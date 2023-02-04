from copy import deepcopy

from paysys.sre.tools.monitorings.lib.util import solomon

alert1 = {
    "id": "test-alert-id",
    "projectId": "test_project",
    "name": "test-alert-id",
    "version": 27,
    "createdBy": "test_user",
    "updatedBy": "test_user",
    "channels": [
        {
            "id": "test-alert-id",
            "config": {
                "notifyAboutStatuses": [
                    "ALARM",
                    "WARN"
                ],
                "repeatDelaySecs": 3600
            }
        }
    ],
    "annotations": {},
    "windowSecs": 120,
    "delaySecs": 0,
    "description": "YT used space monitoring",
    "resolvedEmptyPolicy": "RESOLVED_EMPTY_DEFAULT",
    "noPointsPolicy": "NO_POINTS_DEFAULT",
    "type": {
        "threshold": {
            "selectors": "{project='yt', cluster='hahn', account='logfeller', medium='default', "
                         "service='accounts', sensor='disk_space_in_gb', host='*'}",
            "timeAggregation": "AT_LEAST_ONE",
            "predicate": "GTE",
            "threshold": 80000000,
            "predicateRules": [
                {
                    "thresholdType": "AT_LEAST_ONE",
                    "comparison": "GTE",
                    "threshold": 80000000,
                    "targetStatus": "ALARM"
                },
                {
                    "thresholdType": "AT_LEAST_ONE",
                    "comparison": "GTE",
                    "threshold": 60000000,
                    "targetStatus": "WARN"
                }
            ]
        }
    }
}


def test_alert_compare_fields_ignored(monkeypatch):
    solomon_alert_service = solomon.SolomonAlertingService("xxx")

    alert2 = deepcopy(alert1)
    alert2["version"] = 28
    alert2['type']['threshold']['predicateRules'][0]['thresholdType'] = "AT_ALL_TIMES"

    expected_diff = {"type.threshold.predicateRules.0.thresholdType": ("AT_LEAST_ONE", "AT_ALL_TIMES")}
    diff = {}
    solomon_alert_service._compare_obj(alert1, alert2, diff)
    # field 'type.threshold.predicateRules.thresholdType' is not ignored
    assert expected_diff == diff

    expected_diff = {}
    diff = {}
    # ignore field 'type.threshold.predicateRules.thresholdType'
    monkeypatch.setattr(solomon, "IGNORE_ON_COMPARE_FIELDS",
                        solomon.IGNORE_ON_COMPARE_FIELDS.union({"type.threshold.predicateRules.thresholdType"}))
    solomon_alert_service._compare_obj(alert1, alert2, diff)
    assert expected_diff == diff


def test_alert_compare_list_fields_length(monkeypatch):
    solomon_alert_service = solomon.SolomonAlertingService("xxx")

    alert2 = deepcopy(alert1)
    alert2['type']['threshold']['predicateRules'].append(
        {"thresholdType": "AT_LEAST_ONE", "comparison": "LTE", "threshold": 50000000, "targetStatus": "OK"}
    )

    expected_diff = {"type.threshold.predicateRules": (
        [{"thresholdType": "AT_LEAST_ONE", "comparison": "GTE", "threshold": 80000000, "targetStatus": "ALARM"},
         {"thresholdType": "AT_LEAST_ONE", "comparison": "GTE", "threshold": 60000000, "targetStatus": "WARN"}],
        [{"thresholdType": "AT_LEAST_ONE", "comparison": "GTE", "threshold": 80000000, "targetStatus": "ALARM"},
         {"thresholdType": "AT_LEAST_ONE", "comparison": "GTE", "threshold": 60000000, "targetStatus": "WARN"},
         {"thresholdType": "AT_LEAST_ONE", "comparison": "LTE", "threshold": 50000000, "targetStatus": "OK"}]
    )}
    diff = {}
    solomon_alert_service._compare_obj(alert1, alert2, diff)

    assert expected_diff == diff
