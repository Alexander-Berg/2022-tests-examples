package source_test

import (
	"github.com/YandexClassifieds/vtail/api/core"
	"github.com/YandexClassifieds/vtail/cmd/streamer/source"
	"github.com/stretchr/testify/require"
	"google.golang.org/protobuf/types/known/timestamppb"

	"testing"
	"time"
)

var (
	marshalled = []byte(`{
		"_time": "2018-11-28T13:12:39.776+03:00",
		"_container_id": "123-22-33",
		"_container_name": "my-cont-name",
		"_image_name": "YandexVerticals/myimage:123",
		"_image_id": "abcdef",
		"_allocation_id": "abcdef123456",
		"_message":"this is warn message",
		"_canary": true,
		"_dc": "-",
		"_layer": "local_test",
		"_branch": "br",
		"_service":"service-name",
		"_context":"ctx",
		"_thread": "thr",
		"_level": "WARN",
		"_request_id": "req",
		"_time_nano": "776000000",
		"_timestamp": "2018-11-28T13:12:39",
		"_timezone": "+03:00",
		"_version": "123",
		"_host": "localhost",
		"_uuid": "abcde-12345-fghij",
		"customField": "abc",
		"objKey": {"objProp":"5"}
	}`)
)

func TestUnmarshal(t *testing.T) {
	m, err := source.DecodeMessage(marshalled)
	require.NoError(t, err)
	err = m.Validate()
	require.NoError(t, err)

	parsed, err := time.Parse(source.DateFormat, "2018-11-28T13:12:39.776+03:00")
	require.NoError(t, err)
	require.Equal(t, &core.LogMessage{
		Timestamp:    timestamppb.New(parsed),
		Service:      "service-name",
		Version:      "123",
		Layer:        "local_test",
		Level:        "WARN",
		Message:      "this is warn message",
		Dc:           "-",
		Canary:       true,
		Branch:       "br",
		Host:         "localhost",
		AllocationId: "abcdef123456",
		Context:      "ctx",
		Thread:       "thr",
		RequestId:    "req",
		Rest:         `{"customField":"abc","objKey":{"objProp":"5"}}`,
	}, &m.LogMessage)
}
