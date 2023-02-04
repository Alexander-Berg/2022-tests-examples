package generator

import (
	"github.com/YandexClassifieds/go-common/log/logrus"
	"github.com/YandexClassifieds/nginx-configs-generator/config"
	"github.com/YandexClassifieds/nginx-configs-generator/shivaApi"
	"github.com/stretchr/testify/assert"
	"testing"
)

var (
	branches = map[string][]shivaApi.BranchInfo{
		"good-service1": {
			{
				Name:   "branch1",
				Suffix: 99,
			},
			{
				Name:   "branch2",
				Suffix: 98,
			},
		},
		"good-service2": {
			{
				Name:   "branch11",
				Suffix: 99,
			},
			{
				Name: "branch12",
			},
		},
	}

	cfg = map[string]config.IncludeParams{
		"good-service1": {
			Prefix: "~.*",
			Suffix: ":.*:.*:.*",
			Path:   "/somewhere/good-svc.inc",
		},
		"noShiva-service": {
			Prefix: "~.*",
			Suffix: ":.*:.*:.*",
			Path:   "/somewhere/noShiva-svc.inc",
		},
	}

	includeData0 = IncludeData{
		Path:    "/somewhere/good-svc.inc",
		Content: "~.*99:.*:.*:.*	branch1;\n~.*98:.*:.*:.*	branch2;",
	}

	includeData1 = IncludeData{
		Path:    "/somewhere/noShiva-svc.inc",
		Content: "",
	}
)

func TestService_generateIncludes(t *testing.T) {
	log := logrus.New("debug")
	svc := NewService(cfg, log)

	incData := svc.generateIncludes(branches)

	assert.Equal(t, 2, len(incData))
	assert.Equal(t, includeData0, incData[0])
	assert.Equal(t, includeData1, incData[1])
}
