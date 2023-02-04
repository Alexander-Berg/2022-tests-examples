package juggler

import (
	"bytes"
	"encoding/json"
	"fmt"
	"net/http"
	"testing"
	"time"

	"github.com/YandexClassifieds/drills-helper/common"
	"github.com/YandexClassifieds/drills-helper/test"
	"github.com/spf13/viper"
	"github.com/stretchr/testify/require"
)

type DTItem struct {
	Filters     []Filter `json:"filters"`
	StartTime   float64  `json:"start_time"`
	EndTime     float64  `json:"end_time"`
	Description string   `json:"description"`
	DowntimeID  string   `json:"downtime_id"`
}

type DTResponse struct {
	Items []DTItem `json:"items"`
}

func TestClient_SetDowntime(t *testing.T) {
	test.InitTestEnv()

	filters := []Filter{
		{
			Namespace: "vertis-test",
			Host:      "CGROUP%vertis_for_test@dc=sas",
			Tags:      []string{},
		},
		{
			Namespace: "vertis-test",
			Host:      "CGROUP%vertis_for_test2@dc=sas",
			Tags:      []string{},
		},
		{
			Namespace: "vertis-test",
			Tags:      []string{"vertis_sre_test", "vertis_sre_dc_sas"},
		},
		{
			Namespace: "vertis-test",
			Service:   "iptables_reject",
			Tags:      []string{},
		},
		{
			Namespace: "vertis-test",
			Host:      "sas",
			Tags:      []string{"vertis_sre_test", "slb"},
		},
		{
			Namespace: "vertis-test",
			Host:      "CGROUP%vertis_for_test@dc=man",
			Service:   "consul",
			Tags:      []string{},
		},
		{
			Namespace: "vertis-test",
			Host:      "CGROUP%vertis_for_test2@dc=man",
			Service:   "consul",
			Tags:      []string{},
		},
	}

	t.Cleanup(func() {
		dtResponse, err := getDTs(filters)
		require.NoError(t, err)

		var dtIds []string
		for _, dt := range dtResponse.Items {
			dtIds = append(dtIds, dt.DowntimeID)
		}

		reqBody, err := json.Marshal(struct {
			Downtimes []string `json:"downtime_ids"`
		}{
			Downtimes: dtIds,
		})

		_, err = common.HTTPOAuth(
			&http.Client{},
			http.MethodPost,
			fmt.Sprintf("%s/v2/downtimes/remove_downtimes", viper.GetString("JUGGLER_URL")),
			bytes.NewBuffer(reqBody),
			viper.GetString("DH_OAUTH_TOKEN"),
		)
		require.NoError(t, err)
	})

	cli := New()
	err := cli.SetDowntime(common.DCSas, common.LayerTest, 2*time.Minute)
	require.NoError(t, err)

	// Check created downtime
	dtResponse, err := getDTs(filters)
	require.NoError(t, err)

	require.Len(t, dtResponse.Items, 1)
	require.Equal(t, filters, dtResponse.Items[0].Filters)
	require.Equal(t, "Downtime for drills", dtResponse.Items[0].Description)
	require.InDelta(t, time.Now().Unix(), dtResponse.Items[0].StartTime, 10)
	require.InDelta(t, time.Now().Add(2*time.Minute).Unix(), dtResponse.Items[0].EndTime, 10)
}

func getDTs(filters []Filter) (DTResponse, error) {
	reqBody, err := json.Marshal(struct {
		Filters []Filter `json:"filters"`
	}{
		Filters: filters,
	})
	if err != nil {
		return DTResponse{}, err
	}

	respBody, err := common.HTTPOAuth(
		&http.Client{},
		http.MethodPost,
		fmt.Sprintf("%s/v2/downtimes/get_downtimes", viper.GetString("JUGGLER_URL")),
		bytes.NewBuffer(reqBody),
		viper.GetString("DH_OAUTH_TOKEN"),
	)
	if err != nil {
		return DTResponse{}, err
	}

	var dtResponse DTResponse
	err = json.Unmarshal(respBody, &dtResponse)
	if err != nil {
		return DTResponse{}, err
	}

	return dtResponse, nil
}
