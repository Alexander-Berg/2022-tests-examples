package static

import (
	_ "embed"
	"testing"
	"time"

	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
	"gopkg.in/yaml.v3"
)

func TestStaticFromYml(t *testing.T) {
	var cfg ExtraConfiguration
	err := yaml.Unmarshal(staticData, &cfg)
	require.NoError(t, err)
	require.Len(t, cfg.TcpListeners, 2)

	expectedClusters := []TcpListener{
		{
			Name:         "foo",
			ListenPort:   2242,
			UpstreamHost: "some-host:42",
		},
		{
			Name:         "svc2",
			ListenPort:   2250,
			UpstreamHost: "[::1]:5000",
			Thresholds: Thresholds{
				MaxConnections:     100,
				MaxPendingRequests: 200,
				MaxRequests:        300,
			},
		},
	}
	assert.Equal(t, expectedClusters, cfg.TcpListeners)

	require.Len(t, cfg.HttpServices, 2)
	expectedHttp := []HttpService{
		{
			Domain:          "api-searcher-test-int.slb.vertis.yandex.net",
			UpstreamTimeout: time.Second * 123,
			Upstreams: []string{
				"back-rt-01-sas.test.vertis.yandex.net:34389",
				"back-rt-02-sas.test.vertis.yandex.net:34389",
			},
		},
		{
			Domain:    "localhost",
			Upstreams: []string{"localhost:1700"},
		},
	}
	assert.Equal(t, expectedHttp, cfg.HttpServices)
}

var (
	//go:embed test_config.yaml
	staticData []byte
)
