package domain

import (
	"github.com/YandexClassifieds/logs/cmd/golf/test"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
	"testing"
)

const (
	fullJsonLog string = `
	{
	"_message": "message data",
	"_context" : "MyClass",
	"_time" : "2018-10-25T16:35:00.123+05:00",
	"_time_nano": "123456789",
	"_level" : "ERROR",
    "_layer" : "ci",
	"_version": "tc37",
	"_host": "docker-02-sas.test.vertis.yandex.net",
	"_thread" : "[005]",
	"_request_id" : "444535f9378a3dfa1b8604bc9e05a303",
	"_service" : "AutoService",
	"_image" : "image:1.3",
	"_image_id": "2345nf8234hfjid34t",
	"_container_id" : "sdfgsdvsd",
	"_container_name" : "super_service",
	"_image_name" : "name???",
	"_host" : "delete this row",
	"custom_string" : "value",
	"custom_int_to_float" : 5,
	"custom_float" : 34.53,
	"@custom_text_int": "1",
	"custom_nul" : null,		
	"custom_bool" : true,
	"custom_str_bool" : "false",
	"custom_bullshit " : {
		"k1": "text",
		"k2": 435,
		"k3": false
	},
	"custom_array" :[ "Ford", "BMW", "Fiat" ],
	"_container_id": "container_id_value",
	"_container_name":  "container_name_value",
	"_image_id": "image_id_value",
	"_image_name": "image_name_value",
    "random_fields": "random_value",
    "_random_fields": "_random_value", 
    "_service": "service_value",
 	"service": "service_value",
	"_uuid": "000001f3594c14d37bd9ae84bfc7025e",
	"_timestamp": "2018-10-25T16:34:00",
	"_timezone": "+05:00"
	}
	`

	notValidTimestamp string = `
	{
	"_message": "message value",
	"_time": "2018-10-25T16:35:00.000+05:00",
	"_version": "tc37",
	"_host": "docker-02-sas.test.vertis.yandex.net",
	"_service": "service_value",
	"_uuid": "000001f3594c14d37bd9ae84bfc7025e",
	"_timestamp": "abc",
	 "_layer" : "ci"
	}
	`

	MissingDefaultField string = `
	{
	"_message": "message value",
	"_time": "2018-10-25T16:35:00.000+05:00",
	"_service": "service_value",
	"_uuid": "000001f3594c14d37bd9ae84bfc7025e",
	"_timestamp": "2018-10-25T16:34:00",
	"_layer" : "ci"
	}
	`

	booleanField string = `
	{
	"_message": "message value",
	"_time": "2018-10-25T16:35:00.000+05:00",
	"_canary": true,
	"_service": "service_value",
	"_uuid": "000001f3594c14d37bd9ae84bfc7025e",
	"_timestamp": "2018-10-25T16:34:00",
	"_layer" : "ci"
	}
	`

	jsonNumber string = `
	{
	"_message": "message value",
	"_time": "2018-10-25T16:35:00.000+05:00",
	"_version": "tc37",
	"_host": "docker-02-sas.test.vertis.yandex.net",
	"level_value" : 20000,
	"_service": "service_value",
	"_uuid": "000001f3594c14d37bd9ae84bfc7025e",
 	"_layer" : "ci"
	}
	`

	notJson = `abc`
)

var (
	innerFields = []string{"_message", "_context", "_time", "_level", "_thread", "_request_id"}

	requiredFields = []string{"_container_id", "_service", "_uuid", "_version", "_host"}

	customFields = []string{"service", "custom_string", "custom_int_to_float", "custom_float", "custom_bool", "custom_str_bool", "@custom_text_int"}

	missFields = []string{"custom_bullshit", "custom_array", "custom_nul", "_image", "message", "random_fields", "_random_fields", "_timestamp", "_timezone"}
)

func TestParser(t *testing.T) {

	test.InitEnv()
	fields, err := JsonToFields([]byte(fullJsonLog))
	require.NoError(t, err)

	m := make(map[string]interface{})
	for _, f := range fields {
		m[f.Key.Key] = f.JsonValue
	}
	assert.Equal(t, 123456789, m["_time_nano"])

	assertFields(t, innerFields, fields, true)
	assertFields(t, requiredFields, fields, true)
	assertFields(t, missFields, fields, false)
	assertCustomFields(t, fields, customFields...)
}

func TestNotJson(t *testing.T) {

	test.InitEnv()
	_, err := JsonToFields([]byte(notJson))
	require.Error(t, err)
	_, ok := err.(ErrUnmarshal)
	require.Truef(t, ok, "returned error isn't ErrUnmarshal")
}

func TestBooleanField(t *testing.T) {

	test.InitEnv()
	fields, err := JsonToFields([]byte(booleanField))
	require.NoError(t, err)

	field := getField(fields, "_canary")
	require.NotNil(t, field)
	require.Equal(t, true, field.JsonValue)
}

func TestNotValidField(t *testing.T) {

	test.InitEnv()
	fields, err := JsonToFields([]byte(notValidTimestamp))
	require.NoError(t, err)

	field := getField(fields, "_timestamp")
	require.Nil(t, field)
}

func TestLevelValue(t *testing.T) {

	test.InitEnv()
	fields, err := JsonToFields([]byte(jsonNumber))
	require.NoError(t, err)

	assertCustomFields(t, fields, "level_value")
}

func assertCustomFields(t *testing.T, fields []Field, keys ...string) {

	test.InitEnv()
	assertFields(t, keys, fields, false)
	field := getField(fields, CustomFieldsK.Key)
	value := field.JsonValue.(CustomFieldsMap)
	for _, k := range keys {
		require.Contains(t, value, k)
	}
}

func getField(fields []Field, key string) *Field {
	for _, f := range fields {
		if f.Key.Key == key {
			return &f
		}
	}
	return nil
}

func assertFields(t *testing.T, expected []string, actual []Field, exist bool) {
	asserts := map[string]bool{}
	for _, key := range expected {
		asserts[key] = false
	}
	for _, f := range actual {
		if _, ok := asserts[f.Key.Key]; ok {
			asserts[f.Key.Key] = true
		}
	}
	for k, a := range asserts {
		require.Equal(t, exist, a, "key: %s", k)
	}
}
