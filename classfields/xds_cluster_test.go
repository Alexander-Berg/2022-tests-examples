package xds

import (
	"sort"
	"strings"
	"testing"

	"github.com/YandexClassifieds/envoy-api/common/logger"
	"github.com/YandexClassifieds/envoy-api/info"
	infoPb "github.com/YandexClassifieds/envoy-api/pb/shiva/types/info"
	"github.com/YandexClassifieds/envoy-api/test"
	"github.com/YandexClassifieds/envoy-api/test/mock"
	clusterV3 "github.com/envoyproxy/go-control-plane/envoy/config/cluster/v3"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
)

func TestCluster_Old(t *testing.T) {
	data := test.Old()
	testClusterData(t, data, []string{
		data.Consul[0].Name + test.LegacyClusterPostfix,
	})
}

func TestCluster_Olds(t *testing.T) {
	data := test.Olds()
	testClusterData(t, data, []string{
		data.Consul[0].Name + test.LegacyClusterPostfix,
		data.Consul[1].Name + test.LegacyClusterPostfix,
		data.Consul[2].Name + test.LegacyClusterPostfix,
	})
}

func TestCluster_OldAddress(t *testing.T) {
	data := test.OldAddress()
	oldAddr := data.Shiva[0].Map.Provides[0].OldAddressTest
	odlName := strings.ReplaceAll(oldAddr, ".", "_")
	testClusterData(t, data, []string{
		join(data.Shiva[0].Map.Name, data.Shiva[0].Map.Provides[0].Name),
		odlName,
	})
}

func TestCluster_OldsCommonDomain(t *testing.T) {
	data := test.OldsCommonDomain()
	testClusterData(t, data, []string{
		data.Consul[0].Name + test.LegacyClusterPostfix,
		data.Consul[1].Name + test.LegacyClusterPostfix,
		data.Consul[2].Name + test.LegacyClusterPostfix,
	})
}

func TestCluster_Empty(t *testing.T) {
	data := test.Empty()
	testClusterData(t, data, []string{})
}

func TestCluster_EmptyProvides(t *testing.T) {
	data := test.EmptyProvides()
	testClusterData(t, data, []string{})
}

func TestCluster_Simple(t *testing.T) {
	data := test.Simple()
	testClusterData(t, data, []string{
		join(data.Shiva[0].Map.Name, data.Shiva[0].Map.Provides[0].Name),
	})
}

func TestCluster_Simples(t *testing.T) {
	data := test.Simples()
	testClusterData(t, data, []string{
		join(data.Shiva[0].Map.Name, data.Shiva[0].Map.Provides[0].Name),
		join(data.Shiva[1].Map.Name, data.Shiva[1].Map.Provides[0].Name),
		join(data.Shiva[2].Map.Name, data.Shiva[2].Map.Provides[0].Name),
	})
}

func TestCluster_SimpleGrpc(t *testing.T) {
	data := test.SimpleGrpc()
	testClusterData(t, data, []string{
		join(data.Shiva[0].Map.Name, data.Shiva[0].Map.Provides[0].Name),
	})
}

func TestCluster_SimpleTcp(t *testing.T) {
	data := test.SimpleTcp()
	testClusterData(t, data, []string{
		join(data.Shiva[0].Map.Name, data.Shiva[0].Map.Provides[0].Name),
	})
}

func TestCluster_Branches(t *testing.T) {
	data := test.Branches()
	main := data.Shiva[0]
	b1 := data.Shiva[1]
	b2 := data.Shiva[2]
	b3 := data.Shiva[3]
	testClusterData(t, data, []string{
		join(main.Map.Name, main.Map.Provides[0].Name),
		join(b1.Map.Name, b1.Deployment.Branch, b1.Map.Provides[0].Name),
		join(b2.Map.Name, b2.Deployment.Branch, b2.Map.Provides[0].Name),
		join(b3.Map.Name, b3.Deployment.Branch, b3.Map.Provides[0].Name),
	})
}

func TestCluster_BranchOnePercent(t *testing.T) {
	data := test.BranchOnePercent()
	main := data.Shiva[0]
	branch := data.Shiva[1]
	testClusterData(t, data, []string{
		join(main.Map.Name, main.Map.Provides[0].Name),
		join(branch.Map.Name, branch.Deployment.Branch, branch.Map.Provides[0].Name),
	})
}

func TestCluster_CanaryOnePercent(t *testing.T) {
	data := test.CanaryOnePercent()
	main := data.Shiva[0]
	canary := data.Shiva[1]
	testClusterData(t, data, []string{
		join(main.Map.Name, main.Map.Provides[0].Name),
		join(canary.Map.Name, "canary", canary.Map.Provides[0].Name),
	})
}

func TestCluster_Canary(t *testing.T) {
	data := test.Canary()
	main := data.Shiva[0]
	canary := data.Shiva[1]
	testClusterData(t, data, []string{
		join(main.Map.Name, main.Map.Provides[0].Name),
		join(canary.Map.Name, "canary", canary.Map.Provides[0].Name),
	})
}

func TestCluster_BranchWithTraffic(t *testing.T) {
	data := test.BranchWithTraffic()
	main := data.Shiva[0]
	branch := data.Shiva[1]
	testClusterData(t, data, []string{
		join(main.Map.Name, main.Map.Provides[0].Name),
		join(branch.Map.Name, branch.Deployment.Branch, branch.Map.Provides[0].Name),
	})
}

func TestCluster_BranchesProvides(t *testing.T) {
	data := test.BranchesProvides()
	main := data.Shiva[0]
	b1 := data.Shiva[1]
	b2 := data.Shiva[2]
	b3 := data.Shiva[3]
	testClusterData(t, data, []string{
		join(main.Map.Name, main.Map.Provides[0].Name),
		join(main.Map.Name, main.Map.Provides[1].Name),
		join(b1.Map.Name, b1.Deployment.Branch, b1.Map.Provides[0].Name),
		join(b1.Map.Name, b1.Deployment.Branch, b1.Map.Provides[1].Name),
		join(b2.Map.Name, b2.Deployment.Branch, b2.Map.Provides[0].Name),
		join(b2.Map.Name, b2.Deployment.Branch, b2.Map.Provides[1].Name),
		join(b3.Map.Name, b3.Deployment.Branch, b3.Map.Provides[0].Name),
		join(b3.Map.Name, b3.Deployment.Branch, b3.Map.Provides[1].Name),
	})
}

func TestCluster_BranchWithoutTraffic(t *testing.T) {
	data := test.BranchWithoutTraffic()
	main := data.Shiva[0]
	branch := data.Shiva[1]
	testClusterData(t, data, []string{
		join(main.Map.Name, main.Map.Provides[0].Name),
		join(branch.Map.Name, branch.Deployment.Branch, branch.Map.Provides[0].Name),
	})
}

func TestCluster_OnlyBranch(t *testing.T) {
	data := test.OnlyBranch()
	branch := data.Shiva[0]
	testClusterData(t, data, []string{
		join(branch.Map.Name, branch.Deployment.Branch, branch.Map.Provides[0].Name),
	})
}

func TestCluster_WithOld(t *testing.T) {
	data := test.WithOld()
	main := data.Shiva[0]
	old := data.Consul[0]
	testClusterData(t, data, []string{
		join(main.Map.Name, main.Map.Provides[0].Name),
		old.Name + test.LegacyClusterPostfix,
	})
}

func TestCluster_WithOldAddressAndOld(t *testing.T) {
	data := test.WithOldAddressAndOld()
	main := data.Shiva[0]
	oldAddr := data.Shiva[0].Map.Provides[0].OldAddressTest
	odlName := strings.ReplaceAll(oldAddr, ".", "_")
	testClusterData(t, data, []string{
		join(main.Map.Name, main.Map.Provides[0].Name),
		odlName,
	})
}

func testClusterData(t *testing.T, data *test.Data, expected []string) {
	test.Init(t)
	consulMock := mock.NewConsulMock(data.Consul, nil)
	shivaMock := mock.NewShivaMock(data.Shiva)
	log := logger.Logger.WithField("test", "test")
	conf := info.NewConf()
	infoSvc := info.RunService(conf, shivaMock, consulMock, log)
	s := (&snapshotBuilder{
		Datacenter:        "sas",
		Defaults:          defaultSettings,
		ClusterData:       infoSvc.Info().Clusters,
		svc:               mock.NewConsulMock(nil, nil),
		log:               log,
	}).Build()
	result := parseSnapshot(t, s)
	assertCluster(t, expected, result.Clusters)
}

func assertCluster(t *testing.T, want []string, actualClusters map[string]*clusterV3.Cluster) {
	require.Len(t, actualClusters, len(want))
	actual := make([]string, 0)
	for _, c := range actualClusters {
		actual = append(actual, c.Name)
	}
	sort.Strings(want)
	sort.Strings(actual)
	assert.Equal(t, want, actual)
}

func TestCluster_Consul(t *testing.T) {
	test.Init(t)
	consulSvc := test.ConsulSvc(t)
	log := logger.Logger.WithField("test", "test")
	conf := info.NewConf()
	infoSvc := info.RunService(conf, ShivaMock{}, consulSvc, log)
	s := (&snapshotBuilder{
		Datacenter:        "sas",
		Defaults:          defaultSettings,
		ClusterData:       infoSvc.Info().Clusters,
		svc:               consulSvc,
		log:               log,
	}).Build()
	result := parseSnapshot(t, s)

	haveSlb := false
	haveSlbFullThresholds := false
	haveSlbMaxConnections := false

	for _, resource := range result.Clusters {
		switch resource.Name {
		case "simple-service":
			t.Errorf("Found cluster without balacer tag: %v", resource.Name)
		case "slb":
			haveSlb = true
			require.NotNil(t, resource.CircuitBreakers, "Default circuitBreakers settings should contain in kv consul")
			require.NotNil(t, resource.CircuitBreakers.Thresholds[0].MaxConnections)
			require.NotNil(t, resource.CircuitBreakers.Thresholds[0].MaxPendingRequests)
			require.NotNil(t, resource.CircuitBreakers.Thresholds[0].MaxRequests)
			require.NotNil(t, resource.CircuitBreakers.Thresholds[0].MaxRetries)
		case "slb-full-thresholds":
			haveSlbFullThresholds = true
			assert.Equal(t, uint32(2), resource.CircuitBreakers.Thresholds[0].MaxConnections.Value)
			assert.Equal(t, uint32(3), resource.CircuitBreakers.Thresholds[0].MaxPendingRequests.Value)
			assert.Equal(t, uint32(4), resource.CircuitBreakers.Thresholds[0].MaxRequests.Value)
			assert.Equal(t, uint32(5), resource.CircuitBreakers.Thresholds[0].MaxRetries.Value)
		case "slb-max-connections":
			haveSlbMaxConnections = true
			assert.Equal(t, uint32(0), resource.CircuitBreakers.Thresholds[0].MaxRequests.Value)
			assert.Equal(t, uint32(1), resource.CircuitBreakers.Thresholds[0].MaxConnections.Value)
		}

	}

	if haveSlb == false {
		t.Errorf("Cluster slb not found ")
	}
	if haveSlbFullThresholds == false {
		t.Errorf("Cluster slb-full-thresholds not found")
	}
	if haveSlbMaxConnections == false {
		t.Errorf("Cluster slb-max-connections not found")
	}
}

type ShivaMock struct {
}

func (s ShivaMock) StatusInfo() []*infoPb.DeploymentInfo {
	return []*infoPb.DeploymentInfo{}
}
