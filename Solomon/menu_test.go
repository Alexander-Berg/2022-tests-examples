package main

import (
	"strings"
	"testing"
)

var origMenu = `
{
  "items": [
    {
      "title": "Shard {{shardId}}",
      "children": [
        {
          "title": "Admin",
          "url": "https://solomon.yandex-team.ru/admin/projects/{{projectId}}/shards/{{shardId}}",
          "selectors": "cluster=production|gateway&service=*&projectId=*&shardId=*"
        },
        {
          "title": "Admin on PRE",
          "url": "https://solomon-pre.yandex-team.ru/admin/projects/{{projectId}}/shards/{{shardId}}",
          "selectors": "cluster=prestable&service=*&projectId=*&shardId=*"
        }
      ],
      "selectors": "cluster=*&service=*&shardId=!total"
    },
    {
      "title": "On Host {{host}}",
      "children": [
        {
          "title": "Staff only",
          "url": "https://solomon.yandex-team.ru/staffOnly?a={{host}}",
          "selectors": "cluster=production&host=*"
        },
        {
          "title": "Staff only",
          "url": "https://solomon-pre.yandex-team.ru/staffOnly?a={{host}}",
          "selectors": "cluster=prestable&host=*"
        }
      ],
      "selectors": "host=*"
    }
  ],
  "version": 318
}
`

func TestControlPlaneLinksArePatched(t *testing.T) {
	menu, _ := deserialize([]byte(origMenu))
	patched := patchMenu(ProdMainSolomonCloud, menu)
	//printJSON(serialize(patched))

	for _, itemRaw := range patched["items"].([]interface{}) {
		item := itemRaw.(map[string]interface{})
		for _, childRaw := range item["children"].([]interface{}) {
			child := childRaw.(map[string]interface{})
			url := child["url"].(string)
			if !strings.HasPrefix(url, ProdMainSolomonCloud.Admin) &&
				!strings.HasPrefix(url, ProdMainSolomonCloud.AdminPre) {
				body, _ := serialize(child)
				t.Error("Element was not patched: " + string(body))
			}
		}
	}
}
