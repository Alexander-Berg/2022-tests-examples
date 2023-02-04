package main

import (
	vlog "github.com/YandexClassifieds/go-common/i/log"
	vlogrus "github.com/YandexClassifieds/go-common/log/logrus"
	"github.com/YandexClassifieds/teamcity-metrics/pkg/config"
	"github.com/YandexClassifieds/teamcity-metrics/pkg/teamcity_api"
	"github.com/stretchr/testify/require"
	"testing"
)

func TestService(t *testing.T) {
	var logger vlog.Logger
	logger = vlogrus.New()

	err := config.Init(logger)
	require.NoError(t, err)

	_, _, err = teamcity_api.GetAgentsStatistics()
	require.NoError(t, err)

	_, _, err = teamcity_api.GetBuildStatistics()
	require.NoError(t, err)
}
