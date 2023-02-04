package st

import (
	"encoding/json"
	"io/ioutil"
	"net/http"
	"testing"

	"github.com/YandexClassifieds/go-common/conf/viper"
	"github.com/stretchr/testify/require"
)

var (
	testSummary     = "aptly tests"
	testDescription = "Этот тикет создан в рамках прогона тестов **aptly**. Не обращай внимания"
)

func TestClient_CreateTicket(t *testing.T) {
	c := viper.NewTestConf()
	cli := New(c)

	issue, err := cli.CreateTicket(testSummary, testDescription)
	require.NoError(t, err)

	req, err := http.NewRequest(http.MethodGet, c.Str("ST_URL")+"/v2/issues/"+issue, nil)
	require.NoError(t, err)
	req.Header.Add("Authorization", "OAuth "+c.Str("ST_TOKEN"))

	var http http.Client
	resp, err := http.Do(req)
	require.NoError(t, err)

	body, err := ioutil.ReadAll(resp.Body)
	require.NoError(t, err)
	require.NoError(t, resp.Body.Close())

	var info struct {
		Summary     string `json:"summary"`
		Description string `json:"description"`
		Status      struct {
			Key string `json:"key"`
		} `json:"status"`
	}
	require.NoError(t, json.Unmarshal(body, &info))

	require.Equal(t, testSummary, info.Summary)
	require.Equal(t, testDescription, info.Description)
	require.Equal(t, "open", info.Status.Key)
}
