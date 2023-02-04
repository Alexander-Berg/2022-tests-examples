package xds

import (
	"fmt"
	"strings"
	"testing"
	"time"

	"github.com/YandexClassifieds/envoy-api/common/logger"
	"github.com/YandexClassifieds/envoy-api/info"
	"github.com/YandexClassifieds/envoy-api/test"
	"github.com/YandexClassifieds/envoy-api/test/mock"
	routeV3 "github.com/envoyproxy/go-control-plane/envoy/config/route/v3"
	"github.com/envoyproxy/go-control-plane/pkg/cache/v3"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
	"google.golang.org/protobuf/types/known/durationpb"
	"google.golang.org/protobuf/types/known/wrapperspb"
)

const (
	oldDefaultTimeout = time.Second * 5
	port              = ":80"
	httpTimeout       = time.Second * 120
)

func TestDynamicRoutes_SimpleHttp(t *testing.T) {
	data := test.Simple()
	name := data.Shiva[0].Map.Name + "-" + data.Shiva[0].Map.Provides[0].Name
	testDynamicRoutes(t, data, []*routeV3.VirtualHost{
		newHost(name, weightCluster(name)),
	})
}

func TestDynamicRoutes_SimpleGrpc(t *testing.T) {
	data := test.SimpleGrpc()
	name := data.Shiva[0].Map.Name + "-" + data.Shiva[0].Map.Provides[0].Name
	host := newHost(name, weightCluster(name))
	for i := range host.Routes {
		host.Routes[i].GetRoute().Timeout = durationpb.New(time.Hour)
	}
	testDynamicRoutes(t, data, []*routeV3.VirtualHost{
		host,
	})
}

func TestDynamicRoutes_SimpleTcp(t *testing.T) {
	data := test.SimpleTcp()
	name := data.Shiva[0].Map.Name + "-" + data.Shiva[0].Map.Provides[0].Name
	host := newHost(name, weightCluster(name))
	for i := range host.Routes {
		host.Routes[i].GetRoute().Timeout = durationpb.New(time.Second * 10)
	}
	testDynamicRoutes(t, data, []*routeV3.VirtualHost{
		host,
	})
}

func TestDynamicRoutes_Empty(t *testing.T) {
	testDynamicRoutes(t, test.Empty(), []*routeV3.VirtualHost{})
}

func TestDynamicRoutes_EmptyProvides(t *testing.T) {
	testDynamicRoutes(t, test.EmptyProvides(), []*routeV3.VirtualHost{})
}

func TestDynamicRoutes_OnlyBranch(t *testing.T) {
	data := test.OnlyBranch()
	name := data.Shiva[0].Map.Name + "-" + data.Shiva[0].Deployment.Branch + "-" + data.Shiva[0].Map.Provides[0].Name
	testDynamicRoutes(t, data, []*routeV3.VirtualHost{
		newHost(name, weightCluster(name)),
	})
}

func TestDynamicRoutes_Old(t *testing.T) {
	data := test.Old()
	name := data.Consul[0].Name
	testDynamicRoutes(t, data, []*routeV3.VirtualHost{
		oldHost(name),
	})
}

func TestDynamicRoutes_Olds(t *testing.T) {
	data := test.Olds()
	testDynamicRoutes(t, data, []*routeV3.VirtualHost{
		oldHost(data.Consul[0].Name),
		oldHost(data.Consul[1].Name),
		oldHost(data.Consul[2].Name),
	})
}

func TestDynamicRoutes_OldsCommonDomain(t *testing.T) {
	data := test.OldsCommonDomain()
	testDynamicRoutes(t, data, []*routeV3.VirtualHost{
		oldHost(data.Consul[0].Name),
		oldHost(data.Consul[1].Name),
		oldHost(data.Consul[2].Name),
	})
}

func TestDynamicRoutes_Simples(t *testing.T) {
	data := test.Simples()
	name1 := data.Shiva[0].Map.Name + "-" + data.Shiva[0].Map.Provides[0].Name
	name2 := data.Shiva[1].Map.Name + "-" + data.Shiva[1].Map.Provides[0].Name
	name3 := data.Shiva[2].Map.Name + "-" + data.Shiva[2].Map.Provides[0].Name
	testDynamicRoutes(t, data, []*routeV3.VirtualHost{
		newHost(name1, weightCluster(name1)),
		newHost(name2, weightCluster(name2)),
		newHost(name3, weightCluster(name3)),
	})
}

func TestDynamicRoutes_OldAddress(t *testing.T) {
	data := test.OldAddress()
	oldAddr := data.Shiva[0].Map.Provides[0].OldAddressTest
	name := data.Shiva[0].Map.Name + "-" + data.Shiva[0].Map.Provides[0].Name
	oldName := strings.ReplaceAll(oldAddr, ".", "_")
	testDynamicRoutes(t, data, []*routeV3.VirtualHost{
		newHost(name, weightCluster(name)),
		newHostWithAddr(oldName, oldAddr, weightCluster(oldName)),
	})
}

func TestDynamicRoutes_WithOld(t *testing.T) {
	data := test.WithOld()
	name := data.Shiva[0].Map.Name + "-" + data.Shiva[0].Map.Provides[0].Name
	testDynamicRoutes(t, data, []*routeV3.VirtualHost{
		newHost(name, weightCluster(name)),
		oldHost(data.Consul[0].Name),
	})
}

func TestDynamicRoutes_WithOldAddressAndOld(t *testing.T) {
	data := test.WithOldAddressAndOld()
	name := data.Shiva[0].Map.Name + "-" + data.Shiva[0].Map.Provides[0].Name
	oldAddr := data.Shiva[0].Map.Provides[0].OldAddressTest
	oldName := strings.ReplaceAll(oldAddr, ".", "_")
	testDynamicRoutes(t, data, []*routeV3.VirtualHost{
		newHost(name, weightCluster(name)),
		newHostWithAddr(oldName, oldAddr, weightCluster(oldName)),
	})
}

func TestDynamicRoutes_BranchWithoutTraffic(t *testing.T) {
	data := test.BranchWithoutTraffic()
	main := data.Shiva[0]
	branch := data.Shiva[1]
	mainName := main.Map.Name + "-" + main.Map.Provides[0].Name
	branchName := branch.Map.Name + "-" + branch.Deployment.Branch + "-" + branch.Map.Provides[0].Name
	branchHost := newHost(branchName, weightCluster(branchName))
	branchRoute := makeBranchRoute(
		httpTimeout,
		branchName,
		branch.Deployment.Branch,
		branchName+info.TestBalancer,
	)
	testDynamicRoutes(t, data, []*routeV3.VirtualHost{
		newHost(mainName, weightCluster(mainName), branchRoute),
		branchHost,
	})
}

func TestDynamicRoutes_BranchWithTraffic(t *testing.T) {
	data := test.BranchWithTraffic()
	main := data.Shiva[0]
	branch := data.Shiva[1]
	mainName := main.Map.Name + "-" + main.Map.Provides[0].Name
	branchName := branch.Map.Name + "-" + branch.Deployment.Branch + "-" + branch.Map.Provides[0].Name
	branchHost := newHost(branchName, weightCluster(branchName))
	branchRoute := makeBranchRoute(
		httpTimeout,
		branchName,
		branch.Deployment.Branch,
		branchName+info.TestBalancer,
	)
	testDynamicRoutes(t, data, []*routeV3.VirtualHost{
		newHost(mainName, weightCluster(mainName), branchRoute),
		branchHost,
	})
}

func TestDynamicRoutes_Canary(t *testing.T) {
	data := test.Canary()
	main := data.Shiva[0]
	canary := data.Shiva[1]
	mainName := join(main.Map.Name, main.Map.Provides[0].Name)
	canaryName := join(canary.Map.Name, "canary", canary.Map.Provides[0].Name)
	canaryHost := newHost(canaryName, weightCluster(canaryName))
	canaryRoute := makeBranchRoute(
		httpTimeout,
		canaryName,
		"canary",
		canaryName+info.TestBalancer,
	)
	testDynamicRoutes(t, data, []*routeV3.VirtualHost{
		newHost(mainName, weightCluster(mainName), canaryRoute),
		canaryHost,
	})
}

func TestDynamicRoutes_CanaryOnePercent(t *testing.T) {
	data := test.CanaryOnePercent()
	main := data.Shiva[0]
	canary := data.Shiva[1]
	mainName := join(main.Map.Name, main.Map.Provides[0].Name)
	canaryName := join(canary.Map.Name, "canary", canary.Map.Provides[0].Name)
	canaryHost := newHost(canaryName, weightCluster(canaryName))
	canaryRoute := makeBranchRoute(
		httpTimeout,
		canaryName,
		"canary",
		canaryName+info.TestBalancer,
	)
	testDynamicRoutes(t, data, []*routeV3.VirtualHost{
		newHost(mainName, weightCluster(mainName, canaryName), canaryRoute),
		canaryHost,
	})
}

func TestDynamicRoutes_BranchOnePercent(t *testing.T) {
	data := test.BranchOnePercent()
	main := data.Shiva[0]
	branch := data.Shiva[1]
	mainName := main.Map.Name + "-" + main.Map.Provides[0].Name
	branchName := branch.Map.Name + "-" + branch.Deployment.Branch + "-" + branch.Map.Provides[0].Name
	branchHost := newHost(branchName, weightCluster(branchName))
	branchRoute := makeBranchRoute(
		httpTimeout,
		branchName,
		branch.Deployment.Branch,
		branchName+info.TestBalancer,
	)
	testDynamicRoutes(t, data, []*routeV3.VirtualHost{
		newHost(mainName, weightCluster(mainName, branchName), branchRoute),
		branchHost,
	})
}

func TestDynamicRoutes_Branches(t *testing.T) {
	data := test.Branches()
	main := data.Shiva[0]
	bNoTraffic := data.Shiva[1]
	bOnePercent := data.Shiva[2]
	bFairShare := data.Shiva[3]
	mainName := main.Map.Name + "-" + main.Map.Provides[0].Name

	bNoTrafficName := join(bNoTraffic.Map.Name, bNoTraffic.Deployment.Branch, bNoTraffic.Map.Provides[0].Name)
	bOnePercentName := join(bOnePercent.Map.Name, bOnePercent.Deployment.Branch, bOnePercent.Map.Provides[0].Name)
	bFairShareName := join(bFairShare.Map.Name, bFairShare.Deployment.Branch, bFairShare.Map.Provides[0].Name)
	branchRoutes := []*routeV3.Route{
		makeBranchRoute(httpTimeout, bNoTrafficName, bNoTraffic.Deployment.Branch, bNoTrafficName+info.TestBalancer),
		makeBranchRoute(httpTimeout, bOnePercentName, bOnePercent.Deployment.Branch, bOnePercentName+info.TestBalancer),
		makeBranchRoute(httpTimeout, bFairShareName, bFairShare.Deployment.Branch, bFairShareName+info.TestBalancer),
	}
	testDynamicRoutes(t, data, []*routeV3.VirtualHost{
		newHost(mainName, weightCluster(mainName, bOnePercentName), branchRoutes...),
		newHost(bNoTrafficName, weightCluster(bNoTrafficName)),
		newHost(bOnePercentName, weightCluster(bOnePercentName)),
		newHost(bFairShareName, weightCluster(bFairShareName)),
	})
}

func TestDynamicRoutes_BranchesProvides(t *testing.T) {
	data := test.BranchesProvides()
	main := data.Shiva[0]
	bNoTraffic := data.Shiva[1]
	bOnePercent := data.Shiva[2]
	bFairShare := data.Shiva[3]
	mainName1 := main.Map.Name + "-" + main.Map.Provides[0].Name
	mainName2 := main.Map.Name + "-" + main.Map.Provides[1].Name
	bNoTrafficName1 := join(bNoTraffic.Map.Name, bNoTraffic.Deployment.Branch, bNoTraffic.Map.Provides[0].Name)
	bOnePercentName1 := join(bOnePercent.Map.Name, bOnePercent.Deployment.Branch, bOnePercent.Map.Provides[0].Name)
	bFairShareName1 := join(bFairShare.Map.Name, bFairShare.Deployment.Branch, bFairShare.Map.Provides[0].Name)
	bNoTrafficName2 := join(bNoTraffic.Map.Name, bNoTraffic.Deployment.Branch, bNoTraffic.Map.Provides[1].Name)
	bOnePercentName2 := join(bOnePercent.Map.Name, bOnePercent.Deployment.Branch, bOnePercent.Map.Provides[1].Name)
	bFairShareName2 := join(bFairShare.Map.Name, bFairShare.Deployment.Branch, bFairShare.Map.Provides[1].Name)
	branchRoutes1 := []*routeV3.Route{
		makeBranchRoute(httpTimeout, bNoTrafficName1, bNoTraffic.Deployment.Branch, bNoTrafficName1+info.TestBalancer),
		makeBranchRoute(httpTimeout, bOnePercentName1, bOnePercent.Deployment.Branch, bOnePercentName1+info.TestBalancer),
		makeBranchRoute(httpTimeout, bFairShareName1, bFairShare.Deployment.Branch, bFairShareName1+info.TestBalancer),
	}
	branchRoutes2 := []*routeV3.Route{
		makeBranchRoute(httpTimeout, bNoTrafficName2, bNoTraffic.Deployment.Branch, bNoTrafficName2+info.TestBalancer),
		makeBranchRoute(httpTimeout, bOnePercentName2, bOnePercent.Deployment.Branch, bOnePercentName2+info.TestBalancer),
		makeBranchRoute(httpTimeout, bFairShareName2, bFairShare.Deployment.Branch, bFairShareName2+info.TestBalancer),
	}
	testDynamicRoutes(t, data, []*routeV3.VirtualHost{
		newHost(mainName1, weightCluster(mainName1, bOnePercentName1), branchRoutes1...),
		newHost(mainName2, weightCluster(mainName2, bOnePercentName2), branchRoutes2...),
		newHost(bNoTrafficName1, weightCluster(bNoTrafficName1)),
		newHost(bOnePercentName1, weightCluster(bOnePercentName1)),
		newHost(bFairShareName1, weightCluster(bFairShareName1)),
		newHost(bNoTrafficName2, weightCluster(bNoTrafficName2)),
		newHost(bOnePercentName2, weightCluster(bOnePercentName2)),
		newHost(bFairShareName2, weightCluster(bFairShareName2)),
	})
}

func join(s ...string) string {
	return strings.Join(s, "-")
}

func newHost(cluster string, weight []*routeV3.WeightedCluster_ClusterWeight, branchRoutes ...*routeV3.Route) *routeV3.VirtualHost {
	return newHostWithAddr(cluster, cluster+info.TestBalancer, weight, branchRoutes...)
}

// routes order is important
func newHostWithAddr(cluster, domain string, weight []*routeV3.WeightedCluster_ClusterWeight, branchRoutes ...*routeV3.Route) *routeV3.VirtualHost {
	rs := make([]*routeV3.Route, 0)
	rs = append(rs, branchRoutes...)
	rs = append(rs, makeStickyUidRote(httpTimeout, cluster, domain))
	rs = append(rs, makeWeightedRoute(weight, httpTimeout, domain))
	return &routeV3.VirtualHost{
		Name:    cluster,
		Domains: []string{domain, domain + port},
		Routes:  rs,
	}
}

func weightCluster(name string, branches ...string) []*routeV3.WeightedCluster_ClusterWeight {
	wc := []*routeV3.WeightedCluster_ClusterWeight{{
		Name:   name,
		Weight: wrapperspb.UInt32(100),
	}}

	for _, b := range branches {
		wc[0].Weight.Value--
		wc = append(wc, &routeV3.WeightedCluster_ClusterWeight{
			Name:   b,
			Weight: wrapperspb.UInt32(1),
		})
	}
	return wc
}

func oldHost(cluster string) *routeV3.VirtualHost {
	return &routeV3.VirtualHost{
		Name:    cluster + test.LegacyClusterPostfix,
		Domains: []string{cluster + info.TestOldBalancer, cluster + info.TestOldBalancer + port},
		Routes: []*routeV3.Route{{
			Name:      cluster+test.LegacyClusterPostfix,
			Match:     &routeV3.RouteMatch{PathSpecifier: matchPrefix("/")},
			Action:    routeCluster(cluster+test.LegacyClusterPostfix, oldDefaultTimeout),
			Decorator: &routeV3.Decorator{Operation: cluster + info.TestOldBalancer},
		}},
	}
}

func testDynamicRoutes(t *testing.T, data *test.Data, expected []*routeV3.VirtualHost) {
	t.Helper()
	test.Init(t)
	shivaMock := mock.NewShivaMock(data.Shiva)
	consulMock := mock.NewConsulMock(data.Consul, nil)
	log := logger.Logger.WithField("test", "test")
	conf := info.NewConf()
	infoS := info.RunService(conf, shivaMock, consulMock, log)

	s := (&snapshotBuilder{
		Datacenter:        "sas",
		Defaults:          defaultSettings,
		ClusterData:       infoS.Info().Clusters,
		svc:               mock.NewConsulMock(nil, nil),
		log:               log,
	}).Build()

	result := parseSnapshot(t, s)
	assertHosts(t, expected, result.VHosts)
}

func assertHosts(t *testing.T, want []*routeV3.VirtualHost, actual map[string]*routeV3.VirtualHost) {
	t.Helper()
	delete(actual, "non-existent-route") // always present, do not test for it
	require.Equal(t, len(want), len(actual), "virtual hosts length mismatch")
	for i, wantHost := range want {
		actualHost, ok := actual[wantHost.Name]
		if !ok {
			t.Fatalf("host with name '%s' not found", wantHost.Name)
		}
		//require.Equal(t, wantHost.Routes, actualHost.Routes)
		assert.Equal(t, wantHost.String(), actualHost.String(), "vhost %d mismatch", i)
	}
}

func TestGetConsulRoutes(t *testing.T) {
	test.Init(t)
	consulSvc := test.ConsulSvc(t)
	shivaMock := mock.NewShivaMock(nil)
	log := logger.Logger.WithField("test", "test")
	conf := info.NewConf()
	infoS := info.RunService(conf, shivaMock, consulSvc, log)

	s := buildDynamicSnapshot(infoS.Info().Clusters)
	result := parseSnapshot(t, s)

	require.Contains(t, result.VHosts, "grpcsvc")
	if vhost, ok := result.VHosts["grpcsvc"]; ok {
		assert.Equal(t, "grpcsvc", vhost.Routes[0].GetRoute().GetCluster())
	} else {
		t.Fatalf("vhost '%s' not found", "grpcsvc")
	}
}

func buildDynamicSnapshot(clusters info.Clusters) cache.Snapshot {
	log := logger.Logger.WithField("_context", "test")

	return (&snapshotBuilder{
		Datacenter:        "sas",
		Defaults:          defaultSettings,
		ClusterData:       clusters,
		svc:               mock.NewConsulMock(nil, nil),
		log:               log,
	}).Build()
}

func makeBranchRoute(timeout time.Duration, cluster, branch, domain string) *routeV3.Route {
	branchNameRegex := fmt.Sprintf("(?i)%s", branch)
	return &routeV3.Route{
		Match: &routeV3.RouteMatch{
			PathSpecifier: matchPrefix("/"),
			Headers: []*routeV3.HeaderMatcher{
				matchHeaderRegex("x-branch-name", branchNameRegex),
			},
		},
		Action:    routeCluster(cluster, timeout),
		Decorator: &routeV3.Decorator{Operation: domain},
	}
}

func makeStickyUidRote(timeout time.Duration, cluster, domain string) *routeV3.Route {
	return &routeV3.Route{
		Match: &routeV3.RouteMatch{
			PathSpecifier: matchPrefix("/"),
			Headers: []*routeV3.HeaderMatcher{
				matchHeaderRegex("x-sticky-uid", "1|[yY]es|[tT]rue"),
			},
		},
		Action:    routeCluster(cluster, timeout),
		Decorator: &routeV3.Decorator{Operation: domain},
	}
}

func makeWeightedRoute(clustersWeight []*routeV3.WeightedCluster_ClusterWeight, timeout time.Duration, domain string) *routeV3.Route {
	return &routeV3.Route{
		Match: &routeV3.RouteMatch{
			PathSpecifier: matchPrefix("/"),
		},
		Action:    routeWeightedCluster(clustersWeight, timeout),
		Decorator: &routeV3.Decorator{Operation: domain},
	}
}
