package domain_test

import (
	"github.com/YandexClassifieds/logs/cmd/golf/domain"
	"github.com/stretchr/testify/assert"
	"testing"
)

func TestGetFieldValue(t *testing.T) {
	json := `{
		"_time": "2018-10-25T16:35:00.000+05:00",
		"_uuid": "100001f3594c14d37bd9ae84bfc7025e",
		"customField": "1",
		"_layer": "ci",
		"_version": "tc37",
		"_host": "docker-02-sas.test.vertis.yandex.net",
		"_service": "AutoRu"
	}`
	fields, err := domain.JsonToFields([]byte(json))
	assert.NoError(t, err)

	value, err := domain.GetFieldValue(fields, "_service")
	assert.NoError(t, err)
	assert.Equal(t, "AutoRu", value)

	value, err = domain.GetFieldValue(fields, "_allocation_id")
	assert.Error(t, err)
	assert.Equal(t, err, domain.ErrKeyNotFound)

	value, err = domain.GetFieldValue(fields, "_rest")
	assert.Error(t, err)
	assert.Equal(t, domain.ErrNotString, err)
}
