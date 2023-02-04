package domain

import (
	"encoding/json"
	"errors"
	"github.com/YandexClassifieds/logs/cmd/collector/testutil"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
	"github.com/valyala/fasthttp"
	"github.com/valyala/fastjson"
	"os"
	"strconv"
	"testing"
	"time"
)

func TestFillBulkMessageTime(t *testing.T) {
	testutil.Init(t)
	f := NewMessageFiller()
	jsonMsg := map[string]interface{}{
		"_time": "2018-11-28T13:12:39.776+03:00",
	}
	err := f.FillBulkMessage(jsonMsg, false)
	require.NoError(t, err)
	assert.Equal(t, "776000000", jsonMsg["_time_nano"])
	assert.Equal(t, "+03:00", jsonMsg["_timezone"])
}

func TestBulkTimeError(t *testing.T) {
	testutil.Init(t)
	f := NewMessageFiller()
	jsonMsg := map[string]interface{}{
		"_time": "2018--28T13:12:39.776+03:00",
	}
	err := f.FillBulkMessage(jsonMsg, false)
	require.Error(t, err)
}

func TestFillGeneralFields(t *testing.T) {
	testutil.Init(t)
	f := NewMessageFiller()

	args := &fasthttp.Args{}
	args.Set("Message", `{"_time":"2018-11-28T13:12:39.776+03:00"}`)
	message, err := f.NewMessage(args)
	require.NoError(t, err)
	m := make(map[string]interface{})
	require.NoError(t, json.Unmarshal(message, &m))
	for _, key := range logFellerFields {
		if key == "_time" || key == "_level" {
			continue
		}
		v, present := m[key]
		assert.True(t, present, "key %s not present", key)
		assert.Equal(t, "", v, "value for key %s is not empty")
	}
	assert.Equal(t, "INFO", m["_level"])
}

func TestCanaryField(t *testing.T) {
	testutil.Init(t)
	var m map[string]interface{}
	f := NewMessageFiller()

	args := &fasthttp.Args{}
	args.Set("Message", `{"_time":"2018-11-28T13:12:39.776+03:00"}`)
	args.Set("Canary", `true`)
	message, err := f.NewMessage(args)
	m = make(map[string]interface{})
	require.NoError(t, json.Unmarshal(message, &m))
	require.NoError(t, err)
	assert.Equal(t, true, m["_canary"])

	args = &fasthttp.Args{}
	args.Set("Message", `{"_time":"2018-11-28T13:12:39.776+03:00"}`)
	args.Set("Canary", `false`)
	message, err = f.NewMessage(args)
	require.NoError(t, err)
	m = make(map[string]interface{})
	require.NoError(t, json.Unmarshal(message, &m))
	v, present := m["_canary"]
	assert.True(t, present)
	assert.Equal(t, false, v)

	args = &fasthttp.Args{}
	args.Set("Message", `{"_time":"2018-11-28T13:12:39.776+03:00"}`)
	message, err = f.NewMessage(args)
	m = make(map[string]interface{})
	require.NoError(t, json.Unmarshal(message, &m))
	require.NoError(t, err)
	assert.True(t, present)
	assert.Equal(t, false, v)
}

func TestNewMessage(t *testing.T) {
	testutil.Init(t)
	f := NewMessageFiller()
	f.layer = "local_test"

	messagePayload := `{"_time":"2018-11-28T13:12:39.776+03:00","_level":"WARN","_message":"this is warn message","_context":"ctx","_thread":"thr","_request_id":"req","customField":"abc","objKey":{"objProp":"5"}}`
	args := &fasthttp.Args{}
	args.Set("Message", messagePayload)
	args.Set("Canary", `true`)
	args.Set("ContainerName", `my-cont-name`)
	args.Set("AllocationId", `abcdef123456`)
	args.Set("ImageId", `123456"abcdef`)
	args.Set("ImageName", `YandexVerticals/myimage:123`)
	args.Set("Service", `service-name`)
	args.Set("Version", `123`)
	args.Set("ContainerId", `123-22-33`)
	message, err := f.NewMessage(args)
	require.NoError(t, err)
	m := make(map[string]interface{})
	require.NoError(t, json.Unmarshal(message, &m))

	assert.NotEmpty(t, m["_uuid"])
	delete(m, "_uuid")
	h, err := os.Hostname()
	require.NoError(t, err)
	expectedObj := map[string]interface{}{
		"_time":          "2018-11-28T13:12:39.776+03:00",
		"_container_id":  "123-22-33",
		"_allocation_id": "abcdef123456",
		"_message":       "this is warn message",
		"_canary":        true,
		"_dc":            "-",
		"_layer":         "local_test",
		"_branch":        "",
		"_service":       "service-name",
		"_context":       "ctx",
		"_thread":        "thr",
		"_level":         "WARN",
		"_request_id":    "req",
		"_time_nano":     "776000000",
		"_timestamp":     "2018-11-28T13:12:39",
		"_timezone":      "+03:00",
		"_version":       "123",
		"_host":          h,
		"customField":    "abc",
		"objKey":         map[string]interface{}{"objProp": "5"},
	}
	assert.Equal(t, expectedObj, m)
}

func TestNotJson(t *testing.T) {
	testutil.Init(t)
	f := NewMessageFiller()

	args := &fasthttp.Args{}
	args.Set("Message", `my not json output`)
	//branch test filling
	message, err := f.NewMessage(args)
	require.Error(t, err)
	require.Nil(t, message)
}

func TestBadJsonObject(t *testing.T) {
	testutil.Init(t)
	f := NewMessageFiller()

	args := &fasthttp.Args{}
	args.Set("Message", `{"_time":"2018-11-28T13:12:39.776+03:00","_level":"WARN","_message"}`)
	message, err := f.NewMessage(args)
	require.Error(t, err)
	assert.Nil(t, message)
}

func TestBadJsonObject3(t *testing.T) {
	testutil.Init(t)
	f := NewMessageFiller()

	args := &fasthttp.Args{}
	args.Set("Message", `{"_time":"2018-11-28T13:12:39.776+03:00","res":{"m":"5":{"wrong":"object"}}}`)
	message, err := f.NewMessage(args)
	require.Error(t, err)
	assert.Nil(t, message)
}

func TestJsonArray(t *testing.T) {
	testutil.Init(t)
	f := NewMessageFiller()

	args := &fasthttp.Args{}
	args.Set("Message", `["123"]`)
	message, err := f.NewMessage(args)
	require.Error(t, err)
	require.True(t, errors.Is(err, InvalidJSON))
	require.Nil(t, message)
}

func TestNotIdealJson(t *testing.T) {
	testutil.Init(t)
	f := NewMessageFiller()

	payload := `"_time":"2018-11-28T13:12:39.776+03:00"`
	args := &fasthttp.Args{}
	args.Set("Message", " { "+payload+"\t}\n")
	message, err := f.NewMessage(args)
	assert.NoError(t, err)
	assert.NotNil(t, message)
	require.Contains(t, string(message), payload)
	err = fastjson.ValidateBytes(message)
	assert.NoError(t, err)
}

func TestBadTimeField(t *testing.T) {
	testutil.Init(t)
	f := NewMessageFiller()

	args := &fasthttp.Args{}
	args.Set("Message", `{"_time":"2018-11-28T13:12"}`)
	message, err := f.NewMessage(args)
	require.Error(t, err)
	require.True(t, errors.Is(err, InvalidValue))
	require.EqualError(t, err, "field '_time': invalid value")
	require.Nil(t, message)
}

func TestMissingTimeField(t *testing.T) {
	testutil.Init(t)
	f := NewMessageFiller()

	args := &fasthttp.Args{}
	args.Set("Message", `{"_message":"123"}`)
	message, err := f.NewMessage(args)
	require.Error(t, err)
	require.True(t, errors.Is(err, InvalidValue))
	assert.Contains(t, err.Error(), "_time field not present")
	assert.Nil(t, message)
}

func TestEmptyMessage(t *testing.T) {
	testutil.Init(t)
	f := NewMessageFiller()

	args := &fasthttp.Args{}
	args.Set("Message", `{}`)
	message, err := f.NewMessage(args)
	require.Error(t, err)
	require.True(t, errors.Is(err, InvalidValue))
	assert.Contains(t, err.Error(), "_time field not present")
	assert.Nil(t, message)
}

func TestWrongType(t *testing.T) {
	testutil.Init(t)
	f := NewMessageFiller()

	args := &fasthttp.Args{}
	args.Set("Message", `{"_time":"2018-11-28T13:12:39.776+03:00","_message":123}`)
	message, err := f.NewMessage(args)
	require.Error(t, err)
	require.True(t, errors.Is(err, InvalidValue))
	require.EqualError(t, err, "field '_message': invalid value")
	require.Nil(t, message)
}

func TestSystemField(t *testing.T) {
	testutil.Init(t)
	f := NewMessageFiller()

	args := &fasthttp.Args{}
	args.Set("Message", `{"_time":"2018-11-28T13:12:39.776+03:00","_system_field":123}`)
	message, err := f.NewMessage(args)
	require.Error(t, err)
	require.True(t, errors.Is(err, InvalidKey))
	require.EqualError(t, err, "key '_system_field': invalid key")
	require.Nil(t, message)
}

func TestTimeNanoField_Present(t *testing.T) {
	testutil.Init(t)
	f := NewMessageFiller()

	args := &fasthttp.Args{}
	args.Set("Message", `{"_time":"2018-11-28T13:12:39.776+03:00","_time_nano":"1234"}`)
	message, err := f.NewMessage(args)
	require.NoError(t, err)
	m := make(map[string]interface{})
	require.NoError(t, json.Unmarshal(message, &m))
	assert.Equal(t, "1234", m["_time_nano"])
}

func TestFillTimeNanoField(t *testing.T) {
	testutil.Init(t)
	f := NewMessageFiller()

	args := &fasthttp.Args{}
	args.Set("Message", `{"_time":"2018-11-28T13:12:39.776+03:00"}`)
	message, err := f.NewMessage(args)
	require.NoError(t, err)
	require.NotNil(t, message)
	m := make(map[string]interface{})
	require.NoError(t, json.Unmarshal(message, &m))
	assert.Equal(t, "776000000", m["_time_nano"])
}

func TestFillLevelField(t *testing.T) {
	testutil.Init(t)
	f := NewMessageFiller()

	args := &fasthttp.Args{}
	args.Set("Message", `{"_time":"2018-11-28T13:12:39.776+03:00"}`)
	message, err := f.NewMessage(args)
	require.NoError(t, err)
	require.NotNil(t, message)
	m := make(map[string]interface{})
	require.NoError(t, json.Unmarshal(message, &m))
	assert.Equal(t, "INFO", m["_level"])
}

func TestLevelField(t *testing.T) {
	testutil.Init(t)
	f := NewMessageFiller()

	args := &fasthttp.Args{}
	args.Set("Message", `{"_time":"2018-11-28T13:12:39.776+03:00","_level":"WARN"}`)
	message, err := f.NewMessage(args)
	require.NoError(t, err)
	require.NotNil(t, message)
	m := make(map[string]interface{})
	require.NoError(t, json.Unmarshal(message, &m))
	assert.Equal(t, "WARN", m["_level"])
}

func TestWrongLevelFieldType(t *testing.T) {
	testutil.Init(t)
	f := NewMessageFiller()

	args := &fasthttp.Args{}
	args.Set("Message", `{"_time":"2018-11-28T13:12:39.776+03:00","_level":["abc"]}`)
	message, err := f.NewMessage(args)
	require.Error(t, err)
	require.True(t, errors.Is(err, InvalidValue))
	require.EqualError(t, err, "field '_level': invalid value")
	require.Nil(t, message)
}

func TestWrongLevelField(t *testing.T) {
	testutil.Init(t)
	f := NewMessageFiller()

	args := &fasthttp.Args{}
	args.Set("Message", `{"_time":"2018-11-28T13:12:39.776+03:00","_level":"random string"}`)
	message, err := f.NewMessage(args)
	require.Error(t, err)
	require.True(t, errors.Is(err, InvalidValue))
	require.EqualError(t, err, "field '_level': invalid value")
	require.Nil(t, message)
}

func TestWrapBrokenMessage(t *testing.T) {
	testutil.Init(t)

	str := "not json data"
	msg, err := WrapInvalidMessage([]byte(str), InvalidJSON.Error())
	require.NoError(t, err)

	m := make(map[string]interface{})
	require.NoError(t, json.Unmarshal(msg, &m))

	assert.Equal(t, ErrLvl, m["_level"])
	assert.Equal(t, InvalidJSON.Error(), m["validation_error"])
	assert.Equal(t, ValidationFailedContext, m["_context"])
	assert.Equal(t, str, m["_message"])
	_, err = time.Parse(time.RFC3339Nano, m["_time"].(string))
	require.NoError(t, err)
	_, err = strconv.Atoi(m["_time_nano"].(string))
	require.NoError(t, err)
}
