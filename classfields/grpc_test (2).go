package handler

import (
	"encoding/json"
	"testing"

	vlogrus "github.com/YandexClassifieds/go-common/log/logrus"
	"github.com/YandexClassifieds/logs/api/collector"
	"github.com/YandexClassifieds/logs/cmd/collector/testutil"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
)

func TestGrpcHandler_ExtractMessage(t *testing.T) {
	testutil.Init(t)
	h := &GrpcHandler{
		log: vlogrus.New(),
	}

	t.Run("basic", func(t *testing.T) {
		lc := &collector.LogContext{
			Service:      "svc1",
			AllocationId: "alloc",
			Version:      "ver",
			Branch:       "branch",
			Canary:       true,
			ContainerId:  "cont",
			Hostname:     "host",
			Dc:           "vla",
			Layer:        collector.Layer_Prod,
		}
		row := &collector.LogRow{
			UniqueId: "test-id",
			RawJson:  []byte(`{"_time":"2018-11-28T13:12:39.776+03:00", "_context":"ctx1","_message":"test"}`),
		}
		resultBytes, err := h.extractMessage(lc, row)
		require.NoError(t, err)
		result := make(map[string]interface{})
		err = json.Unmarshal(resultBytes, &result)
		require.NoError(t, err)

		expectedResult := map[string]interface{}{
			"_time":          "2018-11-28T13:12:39.776+03:00",
			"_time_nano":     "776000000",           // logfeller + legacy stuff
			"_timestamp":     "2018-11-28T13:12:39", // logfeller
			"_timezone":      "+03:00",              // logfeller
			"_context":       "ctx1",
			"_message":       "test",
			"_uuid":          "test-id",
			"_level":         "INFO",
			"_request_id":    "",
			"_thread":        "",
			"_service":       "svc1",
			"_allocation_id": "alloc",
			"_version":       "ver",
			"_branch":        "branch",
			"_canary":        true,
			"_container_id":  "cont",
			"_host":          "host",
			"_dc":            "vla",
			"_layer":         "prod",
		}
		assert.Equal(t, expectedResult, result)
	})
	t.Run("wrap_broken", func(t *testing.T) {
		lc := &collector.LogContext{
			Service: "svc1",
		}
		row := &collector.LogRow{
			UniqueId: "test-id",
			RawJson:  []byte(`{}`),
		}
		resultBytes, err := h.extractMessage(lc, row)
		require.NoError(t, err)
		result := make(map[string]interface{})
		err = json.Unmarshal(resultBytes, &result)
		require.NoError(t, err)

		assert.Equal(t, "validation_failed", result["_context"])
	})
	
	t.Run("invalid utf8", func(t *testing.T) {
		lc := &collector.LogContext{
			Service: "svc1",
		}
		row := &collector.LogRow{
			UniqueId: "test-id",
			RawJson:  []byte("{\"_time\":\"2018-11-28T13:12:39.776+03:00\", \"_context\":\"ctx1\",\"_message\":\"te\xffst\"\xff}"),
		}
		resultBytes, err := h.extractMessage(lc, row)
		require.NoError(t, err)
		result := make(map[string]interface{})
		err = json.Unmarshal(resultBytes, &result)
		require.NoError(t, err)

		require.Equal(t, "test", result["_message"])
	})
}
