package config

import (
	"github.com/YandexClassifieds/go-common/log/logrus"
	"github.com/stretchr/testify/assert"
	"testing"
)

var (
	configExample = Config{
		Includes: []Include{
			{
				Service: "good-service",
				Value:   "~.*<UIDSUFFIX>:.*:.*:.*",
				Path:    "/somewhere/good-svc.inc",
			},
			{
				Service: "broken-value-service",
				Value:   "~.*:.*:.*:.*",
				Path:    "/somewhere/broken-value-svc.inc",
			},
		},
	}

	ConfigExampleParams = map[string]IncludeParams{
		"good-service": {
			Prefix: "~.*",
			Suffix: ":.*:.*:.*",
			Path: "/somewhere/good-svc.inc",
		},
	}
)

func TestService_getConfig(t *testing.T) {
	log := logrus.New("debug")
	svc := NewService("example.yml", log)

	config := svc.getConfig()
	assert.Equal(t, configExample, config)
}

func TestService_GetParams(t *testing.T) {
	log := logrus.New("debug")
	svc := NewService("example.yml", log)

	paramsMap := svc.GetParams()
	assert.Equal(t, 1, len(paramsMap))
	assert.Equal(t, ConfigExampleParams, paramsMap)
}
