package xds

import (
	"testing"
	"time"

	"github.com/YandexClassifieds/envoy-api/common/logger"
	"github.com/YandexClassifieds/envoy-api/consul"
	"github.com/YandexClassifieds/envoy-api/info"
	spb "github.com/YandexClassifieds/envoy-api/pb/shiva/service_map"
	"github.com/YandexClassifieds/envoy-api/pb/shiva/types/deployment"
	infoPb "github.com/YandexClassifieds/envoy-api/pb/shiva/types/info"
	"github.com/YandexClassifieds/envoy-api/pb/shiva/types/layer"
	"github.com/YandexClassifieds/envoy-api/pb/shiva/types/traffic"
	"github.com/YandexClassifieds/envoy-api/static"
	"github.com/YandexClassifieds/envoy-api/test"
	"github.com/YandexClassifieds/envoy-api/test/mock"
	"github.com/YandexClassifieds/envoy-api/utils"
	clusterV3 "github.com/envoyproxy/go-control-plane/envoy/config/cluster/v3"
	endpointV3 "github.com/envoyproxy/go-control-plane/envoy/config/endpoint/v3"
	listenerV3 "github.com/envoyproxy/go-control-plane/envoy/config/listener/v3"
	routeV3 "github.com/envoyproxy/go-control-plane/envoy/config/route/v3"
	"github.com/envoyproxy/go-control-plane/pkg/cache/types"
	"github.com/envoyproxy/go-control-plane/pkg/cache/v3"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
)

func TestBuildSnapshot(t *testing.T) {
	test.Init(t)
	log := logger.Logger.WithField("_context", "test")

	s := (&snapshotBuilder{
		Datacenter:  "sas",
		Defaults:    defaultSettings,
		ClusterData: prepareTestData(),
		svc:         mock.NewConsulMock(nil, nil),
		log:         log,
	}).Build()

	result := parseSnapshot(t, s)

	t.Run("simple_shiva", func(t *testing.T) {
		require.Contains(t, result.VHosts, "simple-http")
		require.Contains(t, result.Clusters, "simple-http")
		require.Contains(t, result.Endpoints, "simple-http")

		require.NoError(t, result.VHosts["simple-http"].Validate())
		require.NoError(t, result.Clusters["simple-http"].Validate())

		// check endpoints are properly generated
		la := result.Endpoints["simple-http"]
		require.NoError(t, la.Validate())
		require.Len(t, la.Endpoints, 2, "expected 2 localities")

		byZone := make(map[string]*endpointV3.LocalityLbEndpoints)
		for _, v := range la.Endpoints {
			byZone[v.Locality.Zone] = v
		}

		assert.Len(t, byZone["sas"].GetLbEndpoints(), 2)
		assert.Len(t, byZone["vla"].GetLbEndpoints(), 2)
		assert.Equal(t, uint32(1), byZone["vla"].GetPriority(), "non-local dc should have lower priority")

		assert.Equal(t, "::1", byZone["sas"].GetLbEndpoints()[0].GetEndpoint().GetAddress().GetSocketAddress().GetAddress())
		assert.Equal(t, uint32(42), byZone["sas"].GetLbEndpoints()[0].GetEndpoint().GetAddress().GetSocketAddress().GetPortValue())
		assert.Equal(t, "::3", byZone["vla"].GetLbEndpoints()[0].GetEndpoint().GetAddress().GetSocketAddress().GetAddress())
	})

	t.Run("nomad", func(t *testing.T) {
		require.Contains(t, result.VHosts, "nomad-http")
		require.Contains(t, result.Clusters, "nomad-http")
		require.Contains(t, result.Endpoints, "nomad-http")
	})

	t.Run("branch_no_traffic", func(t *testing.T) {
		vh := result.VHosts["branchy-api"]
		require.NotNil(t, vh)

		assert.Equal(t, "branchy-f1-api", vh.Routes[0].GetRoute().GetCluster())
		hdrMatch := vh.Routes[0].GetMatch().GetHeaders()[0]
		assert.Equal(t, "x-branch-name", hdrMatch.GetName())
		assert.Contains(t, hdrMatch.GetSafeRegexMatch().GetRegex(), "f1")

		assert.Equal(t, "x-sticky-uid", vh.Routes[1].GetMatch().GetHeaders()[0].GetName())
		assert.Len(t, vh.Routes[2].GetRoute().GetWeightedClusters().GetClusters(), 1)

		vhBranch := result.VHosts["branchy-f1-api"]
		require.NotNil(t, vhBranch)
		// sticky route + main branch route
		require.Len(t, vhBranch.Routes, 2)
		assert.Equal(t, "branchy-f1-api", vh.Routes[0].GetRoute().GetCluster())
	})

	t.Run("branch_with_traffic", func(t *testing.T) {
		vh := result.VHosts["branch-traf-api"]
		require.NotNil(t, vh)

		wc := vh.Routes[2].GetRoute().GetWeightedClusters().GetClusters()
		require.Len(t, wc, 2)
		assert.Equal(t, uint32(99), wc[0].Weight.Value, "main route weight check")
		assert.Equal(t, "branch-traf-api", wc[0].Name)
		assert.Equal(t, uint32(1), wc[1].Weight.Value, "branch route weight check")
		assert.Equal(t, "branch-traf-f1-api", wc[1].Name)
	})

	t.Run("validate", func(t *testing.T) {
		for k, v := range result.VHosts {
			require.NoError(t, v.Validate(), "route %s validation failed", k)
		}
		for k, v := range result.Clusters {
			require.NoError(t, v.Validate(), "cluster %s validation failed", k)
		}
		for k, v := range result.Endpoints {
			require.NoError(t, v.Validate(), "endpoints for %s validation failed", k)
		}
		require.NoError(t, result.Route.Validate(), "route validation failed")
	})
}

func TestSnapshot_NomadGrpc(t *testing.T) {
	test.Init(t)
	log := logger.Logger.WithField("_context", "test")

	data := map[string]*consul.ServiceInfo{
		"nomad-rpc": {
			Name:        "nomad-rpc",
			HttpDomains: utils.StringSetFromSlice([]string{"nomad-rpc"}),
		},
	}
	infoSvc := info.NewService(info.NewConf(), nil, nil, log)
	settings := map[string]consul.SettingsInfo{
		"nomad-rpc": {
			CDS: consul.CDSSettings{
				IsGRPC: true,
			},
		},
	}

	s := (&snapshotBuilder{
		Datacenter:  "sas",
		Defaults:    defaultSettings,
		ClusterData: infoSvc.NewInfo(nil, data).Clusters,
		svc:         mock.NewConsulMock(nil, settings),
		log:         log,
	}).Build()
	result := parseSnapshot(t, s)

	require.Contains(t, result.Clusters, "nomad-rpc")
	opts := result.Clusters["nomad-rpc"].TypedExtensionProtocolOptions["envoy.extensions.upstreams.http.v3.HttpProtocolOptions"]
	require.NotNil(t, opts, "http protocol options for grpc check failed")
}

func TestSnapshot_StaticHttp(t *testing.T) {
	test.Init(t)
	log := logger.Logger.WithField("_context", "test")

	testData := []static.HttpService{
		{
			Domain:          "svc1",
			UpstreamTimeout: time.Second * 20,
			Upstreams:       []string{"yandex.ru:1", "ya.ru:2"},
		},
		{
			Domain:    "svc2",
			Upstreams: []string{"localhost:1700"},
		},
	}
	s := (&snapshotBuilder{
		Datacenter: "sas",
		Defaults:   defaultSettings,
		StaticData: &static.ExtraConfiguration{HttpServices: testData},
		svc:        mock.NewConsulMock(nil, nil),
		log:        log,
	}).Build()
	result := parseSnapshot(t, s)

	require.Len(t, result.VHosts, 3) // 2 clusters + default route
	require.Len(t, result.Clusters, 2)
	assert.Contains(t, result.Clusters, "svc1-static")
	assert.Contains(t, result.Clusters, "svc2-static")
	require.Len(t, result.Endpoints["svc1-static"].GetEndpoints(), 1)
	require.Len(t, result.Endpoints["svc1-static"].GetEndpoints()[0].GetLbEndpoints(), 2)
	assert.Equal(t, time.Second*20, result.VHosts["svc1-static"].GetRoutes()[0].GetRoute().GetTimeout().AsDuration())
	assert.Equal(t, time.Second*15, result.VHosts["svc2-static"].GetRoutes()[0].GetRoute().GetTimeout().AsDuration())
	assert.Len(t, result.Clusters["svc1-static"].HealthChecks, 1)
}

func TestSnapshot_StaticTcp(t *testing.T) {
	test.Init(t)
	log := logger.Logger.WithField("_context", "test")

	s := (&snapshotBuilder{
		Datacenter: "sas",
		Defaults:   defaultSettings,
		StaticData: &static.ExtraConfiguration{
			TcpListeners: []static.TcpListener{
				{
					Name:         "h2p-ssh",
					ListenPort:   1234,
					UpstreamHost: "host.query.consul:2222",
				},
				{
					Name:         "svc2",
					ListenPort:   2233,
					UpstreamHost: "[::1]:11",
					Thresholds: static.Thresholds{
						MaxConnections:     1,
						MaxPendingRequests: 2,
						MaxRequests:        3,
					},
				},
			},
		},
		svc: mock.NewConsulMock(nil, nil),
		log: log,
	}).Build()
	result := parseSnapshot(t, s)

	require.Len(t, result.Listeners, 2)
	require.Len(t, result.Clusters, 2)

	assert.Equal(t, uint32(1234), result.Listeners["h2p-ssh"].GetAddress().GetSocketAddress().GetPortValue())

	assert.Equal(t, uint32(2233), result.Listeners["svc2"].GetAddress().GetSocketAddress().GetPortValue())
	assert.Equal(t, uint32(11), result.Clusters["svc2"].GetLoadAssignment().GetEndpoints()[0].GetLbEndpoints()[0].GetEndpoint().GetAddress().GetSocketAddress().GetPortValue())
	assert.Equal(t, uint32(1), result.Clusters["svc2"].GetCircuitBreakers().GetThresholds()[0].GetMaxConnections().GetValue())
	assert.Equal(t, uint32(2), result.Clusters["svc2"].GetCircuitBreakers().GetThresholds()[0].GetMaxPendingRequests().GetValue())
	assert.Equal(t, uint32(3), result.Clusters["svc2"].GetCircuitBreakers().GetThresholds()[0].GetMaxRequests().GetValue())
}

func parseSnapshot(t *testing.T, s cache.Snapshot) snapshotData {
	route := s.Resources[types.Route].Items["local_route"].(*routeV3.RouteConfiguration)

	vhosts := make(map[string]*routeV3.VirtualHost)
	for _, v := range route.VirtualHosts {
		if _, exists := vhosts[v.Name]; exists {
			t.Fatalf("duplicate vhost '%s'", v.Name)
		}
		vhosts[v.Name] = v
	}

	clusters := make(map[string]*clusterV3.Cluster)
	for k, v := range s.Resources[types.Cluster].Items {
		clusters[k] = v.(*clusterV3.Cluster)
	}

	endpoints := make(map[string]*endpointV3.ClusterLoadAssignment)
	for k, v := range s.Resources[types.Endpoint].Items {
		endpoints[k] = v.(*endpointV3.ClusterLoadAssignment)
	}

	listeners := make(map[string]*listenerV3.Listener)
	for k, v := range s.Resources[types.Listener].Items {
		listeners[k] = v.(*listenerV3.Listener)
	}

	return snapshotData{
		VHosts:    vhosts,
		Clusters:  clusters,
		Endpoints: endpoints,
		Listeners: listeners,
		Route:     route,
	}
}

type snapshotData struct {
	Listeners map[string]*listenerV3.Listener
	Clusters  map[string]*clusterV3.Cluster
	VHosts    map[string]*routeV3.VirtualHost
	Endpoints map[string]*endpointV3.ClusterLoadAssignment
	Route     *routeV3.RouteConfiguration
}

func prepareTestData() info.Clusters {
	log := logger.Logger.WithField("_context", "test")
	infoSvc := info.NewService(info.NewConf(), nil, nil, log)
	deploys := []*infoPb.DeploymentInfo{
		{
			Deployment: &deployment.Deployment{Layer: layer.Layer_TEST},
			Map: &spb.ServiceMap{
				Name: "simple",
				Provides: []*spb.ServiceProvides{
					{Name: "http", Protocol: spb.ServiceProvides_http, Port: 8000},
					{Name: "grpc", Protocol: spb.ServiceProvides_grpc, Port: 42},
				},
			},
		},
		{
			Deployment: &deployment.Deployment{Layer: layer.Layer_TEST},
			Map: &spb.ServiceMap{
				Name: "branchy",
				Provides: []*spb.ServiceProvides{
					{Name: "api", Protocol: spb.ServiceProvides_http, Port: 8000},
				},
			},
		},
		{
			Deployment: &deployment.Deployment{Layer: layer.Layer_TEST, Branch: "f1"},
			Map: &spb.ServiceMap{
				Name: "branchy",
				Provides: []*spb.ServiceProvides{
					{Name: "api", Protocol: spb.ServiceProvides_http, Port: 8000},
				},
			},
		},
		{
			Deployment: &deployment.Deployment{Layer: layer.Layer_TEST},
			Map: &spb.ServiceMap{
				Name: "branch-traf",
				Provides: []*spb.ServiceProvides{
					{Name: "api", Protocol: spb.ServiceProvides_http, Port: 8000},
				},
			},
		},
		{
			Deployment: &deployment.Deployment{Layer: layer.Layer_TEST, Branch: "f1"},
			Map: &spb.ServiceMap{
				Name: "branch-traf",
				Provides: []*spb.ServiceProvides{
					{Name: "api", Protocol: spb.ServiceProvides_http, Port: 8000},
				},
			},
			BalancerInfo: &infoPb.BalancerInfo{BranchUIDSuffix: 42, Traffic: traffic.Traffic_ONE_PERCENT},
		},
	}
	consulData := map[string]*consul.ServiceInfo{
		"simple-http": {
			Name: "simple-http",
			Nodes: []consul.ServiceNode{
				{DataCenter: "sas", ServiceIP: "::1", ServicePort: 42},
				{DataCenter: "sas", ServiceIP: "::2", ServicePort: 42},
				{DataCenter: "vla", ServiceIP: "::3", ServicePort: 43},
				{DataCenter: "vla", ServiceIP: "::4", ServicePort: 43},
			},
		},
		"simple-grpc": {},
		"nomad-http": {
			Name:        "n",
			HttpDomains: utils.StringSetFromSlice([]string{"nomad-http.vrts-slb.test.vertis.yandex.net"}),
			Nodes: []consul.ServiceNode{
				{DataCenter: "sas", ServiceIP: "f2::1", ServicePort: 80},
				{DataCenter: "vla", ServiceIP: "f2::2", ServicePort: 80},
			},
			Tags: utils.StringSetFromSlice([]string{"noshiva"}),
		},
		"branchy-api":        {},
		"branchy-f1-api":     {},
		"branch-traf-api":    {},
		"branch-traf-f1-api": {},
	}

	data := infoSvc.NewInfo(deploys, consulData)
	return data.Clusters
}

var (
	defaultSettings = consul.SettingsInfo{
		CDS: consul.CDSSettings{
			ConnectTimeout:                consul.DurationWrapper{Value: time.Millisecond * 400},
			RefreshDelay:                  consul.DurationWrapper{Value: time.Second},
			LbPolicy:                      "ROUND_ROBIN",
			DrainConnectionsOnHostRemoval: consul.BoolWrapper{Value: true},
			HealthChecks: consul.HealthChecks{
				Timeout:            consul.DurationWrapper{Value: time.Second},
				Interval:           consul.DurationWrapper{Value: time.Second * 2},
				UnhealthyThreshold: 1,
				HealthyThreshold:   3,
			},
		},
		RDS: consul.RDSSettings{
			Route: consul.Route{
				UpstreamTimeout: consul.DurationWrapper{Value: time.Second * 10},
			},
		},
		IsOldDeploy: false,
	}
)
