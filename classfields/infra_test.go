package service

import (
	"fmt"
	"net/http"
	"net/http/httptest"
	"testing"

	"github.com/YandexClassifieds/go-common/log/logrus"
	"github.com/YandexClassifieds/grafana-infra-sync/infra"
	"github.com/YandexClassifieds/grafana-infra-sync/test"
	"github.com/grafana-tools/sdk"
	"github.com/magiconair/properties/assert"
	"github.com/spf13/viper"
)

func infraServerMock() *httptest.Server {
	event := `
[{
    "id": 532282,
    "title": "Experimental version on CAs",
    "description": "experimental version",
    "service_id": 9999,
    "environment_id": 86,
    "type": "maintenance",
    "severity": "major",
    "start_time": 1584798689,
    "finish_time": 1584813060,
    "created_by": "akozhikhov",
    "created_at": 1584798769,
    "updated_by": "akozhikhov",
    "updated_at": 1584798769,
    "man": true,
    "myt": false,
    "sas": false,
    "vla": false,
    "tickets": "AS-1",
    "environment_name": "Socrates",
    "service_name": "YT"
  },
{
    "id": 532283,
    "title": "test title",
    "description": "desc",
    "service_id": 9998,
    "environment_id": 86,
    "type": "maintenance",
    "severity": "major",
    "start_time": 1584798689,
    "finish_time": 0,
    "created_by": "akozhikhov",
    "created_at": 1584798769,
    "updated_by": "akozhikhov",
    "updated_at": 1584798769,
    "man": false,
    "myt": true,
    "sas": false,
    "vla": false,
    "tickets": "",
    "environment_name": "Socrates",
    "service_name": "YT"
  }
]
`

	ts := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		if r.Method == "GET" {
			fmt.Fprintln(w, event)
		}

	}))

	return ts
}

func removeAnnotations(client *sdk.Client) {
	ga, err := client.GetAnnotations(sdk.WithTag("from_infra"))
	if err != nil {
		panic(err)
	}
	for _, g := range ga {
		_, err := client.DeleteAnnotation(g.ID)
		if err != nil {
			panic(err)
		}
	}

}

func TestUpdate(t *testing.T) {
	test.InitTestConfig()

	ts := infraServerMock()
	defer ts.Close()

	grafanaClient := sdk.NewClient(viper.GetString("GRAFANA_URL"), viper.GetString("GRAFANA_TOKEN"), sdk.DefaultHTTPClient)
	infraClient := infra.NewClient(ts.URL, "test")

	updater := &InfraUpdater{
		log:           logrus.New(),
		grafanaClient: grafanaClient,
		infraClient:   infraClient,
		serviceIds:    []int{65},
	}
	defer removeAnnotations(updater.grafanaClient)

	updater.Update()
	ga82, err := updater.grafanaClient.GetAnnotations(sdk.WithTag("532282"))
	if err != nil {
		panic(err)
	}

	assert.Equal(t, len(ga82), 1)

	for _, ga := range ga82 {
		fmt.Println(ga.Tags)
		assert.Equal(t, ga.Tags, []string{"YT", "maintenance", "Socrates", "from_infra", "532282", "major", "AS-1", "man"})
		assert.Equal(t, ga.Text, "Experimental version on CAs\nexperimental version")
		assert.Equal(t, ga.TimeEnd, int64(1584813060000))
		assert.Equal(t, ga.Time, int64(1584798689000))
	}

	updater.Update()
	ga83, err := updater.grafanaClient.GetAnnotations(sdk.WithTag("532283"))
	if err != nil {
		panic(err)
	}

	assert.Equal(t, len(ga83), 1)
	for _, ga := range ga83 {
		assert.Equal(t, ga.Tags, []string{"YT", "maintenance", "Socrates", "from_infra", "532283", "major", "myt", "inProgress"})
		assert.Equal(t, ga.Text, "test title\ndesc")
		assert.Equal(t, ga.Time, int64(1584798689000))
	}

}
