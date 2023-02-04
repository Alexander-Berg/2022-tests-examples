package model

import (
	"encoding/json"
	"testing"

	"github.com/YandexClassifieds/logs/api/collector"
	"github.com/francoispqt/gojay"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
)

func TestNewLogMessage(t *testing.T) {
	t.Run("basic", func(t *testing.T) {
		row := &collector.LogRow{
			UniqueId: "test-id",
			RawJson:  []byte(`{"_time":"2020-10-01T13:46:00.1234+03:00"}`),
		}
		info := &collector.LogContext{Service: "tst"}
		msg, err := NewLogMessage(row, info)
		require.NoError(t, err)
		assert.Equal(t, info, msg.context)
	})
	t.Run("malformed_json1", func(t *testing.T) {
		row := &collector.LogRow{
			RawJson: []byte(`{"_time":"2018-11-28T13:12:39.776+03:00","_level":"WARN","_message"}`),
		}
		info := &collector.LogContext{}
		_, err := NewLogMessage(row, info)
		require.Error(t, err)
		assert.Contains(t, err.Error(), "cannot parse JSON")
	})
	t.Run("malformed_json2", func(t *testing.T) {
		row := &collector.LogRow{
			RawJson: []byte(`{"_time":"2018-11-28T13:12:39.776+03:00","res":{"m":"5":{"wrong":"object"}}}`),
		}
		info := &collector.LogContext{}
		_, err := NewLogMessage(row, info)
		require.Error(t, err)
		assert.Contains(t, err.Error(), "cannot parse JSON")
	})
	t.Run("not_object", func(t *testing.T) {
		row := &collector.LogRow{
			RawJson: []byte(`["123"]`),
		}
		info := &collector.LogContext{}
		_, err := NewLogMessage(row, info)
		require.Error(t, err)
		assert.Contains(t, err.Error(), "Cannot unmarshal JSON to type")
	})
	t.Run("garbage_trailers", func(t *testing.T) {
		row := &collector.LogRow{
			RawJson: []byte(` { "_time":"2018-11-28T13:12:39.776+03:00"		}` + "\n"),
		}
		info := &collector.LogContext{}
		msg, err := NewLogMessage(row, info)
		require.NoError(t, err)
		assert.NoError(t, msg.Validate())
	})
	t.Run("not_json", func(t *testing.T) {
		row := &collector.LogRow{
			RawJson: []byte(`not json stuff`),
		}
		info := &collector.LogContext{}
		_, err := NewLogMessage(row, info)
		require.Error(t, err)
		assert.Contains(t, err.Error(), "cannot parse JSON")
	})
	t.Run("wrong_time_format", func(t *testing.T) {
		_, err := createTestMessage(`{"_time":"2018-11-28T13:12"}`)
		require.Error(t, err)
		assert.Contains(t, err.Error(), "parsing time")
	})
	t.Run("wrong_type", func(t *testing.T) {
		_, err := createTestMessage(`{"_time":"2018-11-28T13:12:39.776+03:00","_message":123}`)
		require.Error(t, err)
		assert.Contains(t, err.Error(), "field '_message' parse error")
	})
	t.Run("reserved_field", func(t *testing.T) {
		_, err := createTestMessage(`{"_time":"2018-11-28T13:12:39.776+03:00","_reserved":123}`)
		require.Error(t, err)
		assert.Contains(t, err.Error(), "reserved field")
	})
	t.Run("embedded_json_check", func(t *testing.T) {
		row := &collector.LogRow{
			RawJson: []byte(`{"fld":"double_slash\\"}`),
		}

		d := make(map[string]interface{})
		require.NoError(t, json.Unmarshal(row.RawJson, &d))

		_, err := NewLogMessage(row, &collector.LogContext{})
		require.NoError(t, err)
	})
}

func TestLogMessage_Validate(t *testing.T) {
	t.Run("base", func(t *testing.T) {
		msg, err := createTestMessage(`{"_time":"2018-11-28T13:12:39.776+03:00","_level":"WARN","_message":"test"}`)
		require.NoError(t, err, "msg parse error")
		require.NoError(t, msg.Validate(), "msg validate error")
	})
	t.Run("wrong_level_field", func(t *testing.T) {
		msg, err := createTestMessage(`{"_time":"2018-11-28T13:12:39.776+03:00","_level":"wtf"}`)
		require.NoError(t, err)
		err = msg.Validate()
		require.Error(t, err)
		assert.Contains(t, err.Error(), "field '_level' validate error")
	})
}

func TestLogMessage_MarshalJSONObject(t *testing.T) {
	row := &collector.LogRow{
		UniqueId: "test-id",
		RawJson:  []byte(`{"_time":"2020-12-01T16:45:31.4321+03:00","_level":"INFO","_context":"ctx1","_thread":"thr","_request_id":"req","_message":"test msg"}`),
	}
	info := &collector.LogContext{
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
	msg, err := NewLogMessage(row, info)
	require.NoError(t, err, "parse error")

	rb, err := gojay.MarshalJSONObject(msg)
	require.NoError(t, err, "marshal error")

	result := make(map[string]interface{})
	require.NoError(t, json.Unmarshal(rb, &result))

	expect := map[string]interface{}{
		"_time":          "2020-12-01T16:45:31.4321+03:00",
		"_time_nano":     "432100000",           // logfeller + legacy stuff
		"_timestamp":     "2020-12-01T16:45:31", // logfeller
		"_timezone":      "+03:00",              // logfeller
		"_context":       "ctx1",
		"_message":       "test msg",
		"_uuid":          "test-id",
		"_level":         "INFO",
		"_request_id":    "req",
		"_thread":        "thr",
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
	assert.Equal(t, expect, result)
}

func createTestMessage(s string) (*LogMessage, error) {
	row := &collector.LogRow{
		UniqueId: "test-id",
		RawJson:  []byte(s),
	}
	info := &collector.LogContext{
		Service: "test-svc",
	}
	return NewLogMessage(row, info)
}
