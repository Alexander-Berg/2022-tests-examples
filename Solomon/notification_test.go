package main

import "testing"

var jugglerChannel = `{
  "id": "juggler",
  "projectId": "solomon",
  "name": "juggler",
  "version": 14,
  "notifyAboutStatuses": [
    "OK",
    "ERROR",
    "ALARM",
    "WARN"
  ],
  "repeatNotifyDelayMillis": 0,
  "createdBy": "gordiychuk",
  "updatedBy": "gordiychuk",
  "method": {
    "juggler": {
      "host": "{{annotations.host}}{{^annotations.host}}solomon{{/annotations.host}}",
      "service": "{{annotations.service}}{{^annotations.service}}{{alert.id}}{{/annotations.service}}",
      "description": "",
      "tags": [
        "{{^alert.parent.id}}{{alert.id}}{{/alert.parent.id}}",
        "{{annotations.tags}}",
        "{{alert.parent.id}}",
        "solomon"
      ]
    }
  }
}`

func TestJugglerChannelHost(t *testing.T) {
	body, _ := deserialize([]byte(jugglerChannel))
	patched := patchNotificationChannel(ProdMainSolomonCloud, body)

	assertEquals("{{annotations.host}}{{^annotations.host}}solomon_cloud{{/annotations.host}}", getByPath(patched, ".method.juggler.host").(string), t)
}
