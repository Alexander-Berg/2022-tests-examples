package hbf

import (
	"encoding/json"
	"net/http"
	"testing"
	"time"

	"github.com/YandexClassifieds/drills-helper/log"
	"github.com/YandexClassifieds/drills-helper/test"
	"github.com/jarcoal/httpmock"
	"github.com/stretchr/testify/assert"
)

var (
	preset = Preset{
		ID:        130,
		Projects:  []string{"test1", "test2"},
		Dcs:       []string{"sas"},
		Exclude:   []string{"exclude1", "exclude2"},
		CloseFrom: []string{"close-from1", "close-from2"},
	}
	trainingID = TrainingID{ID: 1}
)

func TestClient_Start(t *testing.T) {
	test.InitTestEnv()

	client := &Client{
		cli:     &http.Client{},
		baseURL: "http://127.0.0.1",
		log:     log.InitLogging(),
	}

	httpmock.Activate()
	t.Cleanup(httpmock.DeactivateAndReset)

	httpmock.RegisterResponder(http.MethodGet, "http://127.0.0.1/api/v1/trainings/presets/data?id=137",
		httpmock.NewJsonResponderOrPanic(200, preset))
	httpmock.RegisterResponder(http.MethodPost, "http://127.0.0.1/api/v1/trainings/add",
		func(r *http.Request) (*http.Response, error) {
			var training Training
			err := json.NewDecoder(r.Body).Decode(&training)
			assert.NoError(t, err)

			assert.Equal(t, preset.Projects, training.Projects)
			assert.Equal(t, preset.Dcs, training.Dcs)
			assert.Equal(t, preset.CloseFrom, training.CloseFrom)
			assert.Equal(t, preset.Exclude, training.Exclude)
			assert.Equal(t, int64((2 * time.Minute).Seconds()), training.Duration)
			assert.Equal(t, "[drills-helper] Close DC for drills", training.Comment)
			assert.InDelta(t, time.Now().Add(BeginDelay).UnixNano(), training.Begins, float64((1 * time.Minute).Nanoseconds()))
			assert.Equal(t, "VERTISADMIN-111", training.Ticket)

			return httpmock.NewStringResponse(200, ""), nil
		})

	err := client.Start("sas", 2*time.Minute, "VERTISADMIN-111")
	assert.NoError(t, err)
}

func TestClient_Stop(t *testing.T) {
	test.InitTestEnv()

	client := &Client{
		cli:     &http.Client{},
		baseURL: "http://127.0.0.1",
		log:     log.InitLogging(),
	}

	httpmock.Activate()
	t.Cleanup(httpmock.DeactivateAndReset)

	httpmock.RegisterResponder(http.MethodGet, "http://127.0.0.1/api/v1/trainings/presets/data?id=137",
		httpmock.NewJsonResponderOrPanic(200, preset))
	httpmock.RegisterResponder(http.MethodGet, "http://127.0.0.1/api/v1/trainings?limit=1&archived=false&dcs=sas&projects=test1&projects=test2",
		httpmock.NewJsonResponderOrPanic(200, []TrainingID{trainingID}))
	httpmock.RegisterResponder(http.MethodPost, "http://127.0.0.1/api/v1/trainings/cancel",
		func(r *http.Request) (*http.Response, error) {
			var tID TrainingID
			err := json.NewDecoder(r.Body).Decode(&tID)
			assert.NoError(t, err)

			assert.Equal(t, trainingID.ID, tID.ID)
			return httpmock.NewStringResponse(200, ""), nil
		})

	_, err := client.Stop("sas")
	assert.NoError(t, err)
}
