package main

import (
	"testing"
)

var origAlert = `{
  "id": "solomon-uptime",
  "projectId": "solomon",
  "name": "Solomon: Uptime",
  "version": 49,
  "createdBy": "gordiychuk",
  "updatedBy": "uranix",
  "groupByLabels": [
    "host",
    "cluster",
    "service"
  ],
  "notificationChannels": [
    "jamel-q",
    "solomon-notifications",
    "solomon-ops-q",
    "solomon-ops-telegram"
  ],
  "channels": [
    {
      "id": "jamel-q",
      "config": {
        "notifyAboutStatuses": [
          "ERROR",
          "ALARM",
          "NO_DATA"
        ],
        "repeatDelaySecs": 0
      }
    },
    {
      "id": "solomon-notifications",
      "config": {
        "notifyAboutStatuses": [
          "ERROR",
          "ALARM",
          "NO_DATA"
        ],
        "repeatDelaySecs": 0
      }
    },
    {
      "id": "solomon-ops-q",
      "config": {
        "notifyAboutStatuses": [
          "ERROR",
          "ALARM",
          "NO_DATA"
        ],
        "repeatDelaySecs": 0
      }
    },
    {
      "id": "solomon-ops-telegram",
      "config": {
        "notifyAboutStatuses": [
          "ERROR",
          "ALARM",
          "NO_DATA"
        ],
        "repeatDelaySecs": 0
      }
    }
  ],
  "type": {
    "expression": {
      "program": "let source = sum({ \n  cluster='production|stockpile_sas|stockpile_vla|storage_sas|storage_vla', \n  host!='cluster|Sas|Man|Vla|Myt',\n  sensor='jvm.runtime.uptime|uptimeMillis'\n}) by host;\n\nlet uptime = last(source);\n\n\nlet uptimeHours = uptime / 1000 / 60 / 60;\nlet prettyHours = to_fixed(uptimeHours, 0);\n\nalarm_if(uptime != uptime);\nalarm_if(uptime < 10*60*1000);",
      "checkExpression": ""
    }
  },
  "annotations": {
    "uptime": "{{expression.prettyHours}}h",
    "host": "solomon.prod.{{labels.host}}",
    "staffOnly": "https://solomon.yandex-team.ru/staffOnly/{{labels.host}}:4510/bla-bla-bla"
  },
  "windowSecs": 300,
  "delaySecs": 60,
  "description": "Смотреть здесь: https://wiki.yandex-team.ru/solomon/dev/runbooks/#solomon-uptime",
  "resolvedEmptyPolicy": "RESOLVED_EMPTY_DEFAULT",
  "noPointsPolicy": "NO_POINTS_DEFAULT"
}`

func TestAlertConversionSolomonCloud(t *testing.T) {
	alert, _ := deserialize([]byte(origAlert))
	patched, _ := patchAlert(ProdMainSolomonCloud, alert)
	//printJSON(serialize(patched))
	description := "Synched from https://solomon.yandex-team.ru/admin/projects/solomon/alerts/solomon-uptime\n\n"
	description += "Смотреть здесь: https://wiki.yandex-team.ru/solomon_cloud/dev/runbooks/#solomon-uptime"
	notifyAboutStatuses := getByPath(patched, ".channels[0].config.notifyAboutStatuses")
	assertEquals("ERROR", arrayGet(notifyAboutStatuses, 0).(string), t)
	assertEquals("ALARM", arrayGet(notifyAboutStatuses, 1).(string), t)
	annotationHost := getByPath(patched, ".annotations.host").(string)
	assertEquals("solomon_cloud.prod.{{labels.host}}", annotationHost, t)
	annotationStaffOnly := getByPath(patched, ".annotations.staffOnly").(string)
	assertEquals("https://solomon.cloud.yandex-team.ru/staffOnly/{{labels.host}}:4510/bla-bla-bla", annotationStaffOnly, t)
}

func TestAlertConversionPre(t *testing.T) {
	alert, _ := deserialize([]byte(origAlert))
	patched, _ := patchAlert(PreMainSolomon, alert)
	//printJSON(serialize(patched))
	description := "Synched from https://solomon.yandex-team.ru/admin/projects/solomon/alerts/solomon-uptime\n\n"
	description += "Смотреть здесь: https://wiki.yandex-team.ru/solomon/dev/runbooks/#solomon-uptime"
	assertEquals(description, patched["description"].(string), t)

	_, exists := patched["channels"]
	if exists {
		t.Error("Expected channels to be removed")
	}

	_, exists = patched["notificationChannels"]
	if exists {
		t.Error("Expected notificationChannels to be removed")
	}
}
