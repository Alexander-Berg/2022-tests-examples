package consul

import (
	_ "embed"
	"encoding/json"
	"testing"
	"time"

	"github.com/YandexClassifieds/envoy-api/common/logger"
	"github.com/YandexClassifieds/envoy-api/utils"
	"github.com/spf13/viper"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
)

var (
	//go:embed test_settings.json
	testSettings []byte
)

func TestService_fetchServiceInfo(t *testing.T) {
	testInit()
	s, err := NewService(Config{
		Address:     viper.GetString("consul_address"),
		Datacenters: []string{viper.GetString("dc")},
		CachePath:   "/tmp",
	}, logger.Logger)
	require.NoError(t, err)
	sm, err := s.fetchServiceInfo()
	require.NoError(t, err)
	require.NotEmpty(t, sm)

	if !assert.NotEmpty(t, sm) {
		return
	}
	assert.Len(t, sm["multi"].HttpDomains, 2)
}

func TestBuildSettings(t *testing.T) {
	def := SettingsInfo{
		CDS: CDSSettings{
			ConnectTimeout: DurationWrapper{Value: time.Second * 10},
			RefreshDelay:   DurationWrapper{Value: time.Second * 10},
			LbPolicy:       "test",
			HealthChecks: HealthChecks{
				Timeout:            DurationWrapper{Value: time.Second * 10},
				Interval:           DurationWrapper{Value: time.Second * 10},
				UnhealthyThreshold: 10,
				HealthyThreshold:   10,
			},
		},
		RDS: RDSSettings{
			Route: Route{
				UpstreamTimeout: DurationWrapper{Value: time.Second * 10},
			},
		},
	}

	tags := []string{
		envoyUpstreamRefreshDelay + "=11s",
		envoyUpstreamLbPolicy + "=custom_test",
		envoyUpstreamHealthCheckTimeout + "=11s",
		envoyUpstreamHealthChecksInterval + "=11s",
		envoyUpstreamHealthChecksUnhealthyThreshold + "=11",
		envoyUpstreamHealthChecksHealthyThreshold + "=0",
		envoyUpstreamTimeout + "=11s",
	}
	result := buildSettings(def, utils.ListToSet(tags))

	//assert result
	assert.Equal(t, time.Second*10, result.CDS.ConnectTimeout.Value)
	assert.Equal(t, time.Second*11, result.CDS.RefreshDelay.Value)
	assert.Equal(t, "custom_test", result.CDS.LbPolicy)
	assert.Equal(t, uint32(11), result.CDS.HealthChecks.UnhealthyThreshold)
	assert.Equal(t, uint32(10), result.CDS.HealthChecks.HealthyThreshold)
	assert.Equal(t, time.Second*11, result.CDS.HealthChecks.Timeout.Value)
	assert.Equal(t, time.Second*11, result.CDS.HealthChecks.Interval.Value)
	assert.Equal(t, time.Second*11, result.RDS.Route.UpstreamTimeout.Value)
}

func TestParseSettings(t *testing.T) {
	data := SettingsInfo{}
	require.NoError(t, json.Unmarshal(testSettings, &data))
	assert.Equal(t, time.Millisecond*400, data.CDS.ConnectTimeout.Value)
}

func testInit() {
	viper.SetDefault("consul_address", "localhost:18500")
	viper.SetDefault("consul_fetch_workers", 20)
	viper.SetDefault("dc", "dc1")
	viper.AutomaticEnv()

	utils.EnvoyApiDatacenter = viper.GetString("dc")
}
