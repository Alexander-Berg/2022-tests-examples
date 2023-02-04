package runserver

import (
	"context"
	"net/http"
	"os"
	"testing"
	"time"

	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"

	"a.yandex-team.ru/billing/library/go/billingo/pkg/interactions"
	"a.yandex-team.ru/library/go/core/metrics/solomon"
)

func TestRunServer(t *testing.T) {
	registry := solomon.NewRegistry(nil)

	client, err := interactions.NewClient(interactions.Config{
		BaseURL: os.Getenv("PROCESSOR_BASE_URL"),
		Name:    "processor",
	}, nil, registry)
	require.NoError(t, err)

	ctx, cancel := context.WithTimeout(context.Background(), time.Second)
	defer cancel()

	body := map[string]any{
		"namespace": "dummy",
		"endpoint":  "calc",
		"event": map[string]any{
			"client_id":   1,
			"contract_id": 121,
			"amount":      123.4,
			"product":     "B.U. Alexandrov",
			"external_id": "1",
		},
	}

	response := client.MakeRequestRaw(ctx, interactions.Request{
		APIMethod: "/v1/process",
		Method:    http.MethodPost,
		Body:      body,
		Name:      "v1_process",
	})

	assert.Equal(t, 200, response.Code, string(response.Response))
	require.NoError(t, response.Error)
}
