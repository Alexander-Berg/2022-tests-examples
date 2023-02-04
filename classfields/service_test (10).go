package cluster

import (
	"encoding/json"
	"github.com/YandexClassifieds/envoy-api/common/logger"
	"github.com/YandexClassifieds/envoy-api/info"
	infoPb "github.com/YandexClassifieds/envoy-api/pb/shiva/types/info"
	"github.com/YandexClassifieds/envoy-api/static"
	"github.com/YandexClassifieds/envoy-api/test"
	"github.com/YandexClassifieds/envoy-api/test/mock"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
	"net/http"
	"net/http/httptest"
	"sort"
	"strings"
	"testing"
	"time"
)

func TestDynamicRoutes_Old(t *testing.T) {
	data := test.Old()
	testDynamicRoutes(t, data, []string{
		data.Consul[0].Name + test.LegacyClusterPostfix,
	})
}

func TestDynamicRoutes_Olds(t *testing.T) {
	data := test.Olds()
	testDynamicRoutes(t, data, []string{
		data.Consul[0].Name + test.LegacyClusterPostfix,
		data.Consul[1].Name + test.LegacyClusterPostfix,
		data.Consul[2].Name + test.LegacyClusterPostfix,
	})
}

func TestDynamicRoutes_OldAddress(t *testing.T) {
	data := test.OldAddress()
	oldAddr := data.Shiva[0].Map.Provides[0].OldAddressTest
	odlName := strings.ReplaceAll(oldAddr, ".", "_")
	testDynamicRoutes(t, data, []string{
		join(data.Shiva[0].Map.Name, data.Shiva[0].Map.Provides[0].Name),
		odlName,
	})
}

func TestDynamicRoutes_OldsCommonDomain(t *testing.T) {
	data := test.OldsCommonDomain()
	testDynamicRoutes(t, data, []string{
		data.Consul[0].Name + test.LegacyClusterPostfix,
		data.Consul[1].Name + test.LegacyClusterPostfix,
		data.Consul[2].Name + test.LegacyClusterPostfix,
	})
}

func TestDynamicRoutes_Empty(t *testing.T) {
	data := test.Empty()
	testDynamicRoutes(t, data, []string{})
}

func TestDynamicRoutes_EmptyProvides(t *testing.T) {
	data := test.EmptyProvides()
	testDynamicRoutes(t, data, []string{})
}

func TestDynamicRoutes_Simple(t *testing.T) {
	data := test.Simple()
	testDynamicRoutes(t, data, []string{
		join(data.Shiva[0].Map.Name, data.Shiva[0].Map.Provides[0].Name),
	})
}

func TestDynamicRoutes_Simples(t *testing.T) {
	data := test.Simples()
	testDynamicRoutes(t, data, []string{
		join(data.Shiva[0].Map.Name, data.Shiva[0].Map.Provides[0].Name),
		join(data.Shiva[1].Map.Name, data.Shiva[1].Map.Provides[0].Name),
		join(data.Shiva[2].Map.Name, data.Shiva[2].Map.Provides[0].Name),
	})
}

func TestDynamicRoutes_SimpleGrpc(t *testing.T) {
	data := test.SimpleGrpc()
	testDynamicRoutes(t, data, []string{
		join(data.Shiva[0].Map.Name, data.Shiva[0].Map.Provides[0].Name),
	})
}

func TestDynamicRoutes_SimpleTcp(t *testing.T) {
	data := test.SimpleTcp()
	testDynamicRoutes(t, data, []string{
		join(data.Shiva[0].Map.Name, data.Shiva[0].Map.Provides[0].Name),
	})
}

func TestDynamicRoutes_Branches(t *testing.T) {
	data := test.Branches()
	main := data.Shiva[0]
	b1 := data.Shiva[1]
	b2 := data.Shiva[2]
	b3 := data.Shiva[3]
	testDynamicRoutes(t, data, []string{
		join(main.Map.Name, main.Map.Provides[0].Name),
		join(b1.Map.Name, b1.Deployment.Branch, b1.Map.Provides[0].Name),
		join(b2.Map.Name, b2.Deployment.Branch, b2.Map.Provides[0].Name),
		join(b3.Map.Name, b3.Deployment.Branch, b3.Map.Provides[0].Name),
	})
}

func TestDynamicRoutes_BranchOnePercent(t *testing.T) {
	data := test.BranchOnePercent()
	main := data.Shiva[0]
	branch := data.Shiva[1]
	testDynamicRoutes(t, data, []string{
		join(main.Map.Name, main.Map.Provides[0].Name),
		join(branch.Map.Name, branch.Deployment.Branch, branch.Map.Provides[0].Name),
	})
}

func TestDynamicRoutes_CanaryOnePercent(t *testing.T) {
	data := test.CanaryOnePercent()
	main := data.Shiva[0]
	canary := data.Shiva[1]
	testDynamicRoutes(t, data, []string{
		join(main.Map.Name, main.Map.Provides[0].Name),
		join(canary.Map.Name, "canary", canary.Map.Provides[0].Name),
	})
}

func TestDynamicRoutes_Canary(t *testing.T) {
	data := test.Canary()
	main := data.Shiva[0]
	canary := data.Shiva[1]
	testDynamicRoutes(t, data, []string{
		join(main.Map.Name, main.Map.Provides[0].Name),
		join(canary.Map.Name, "canary", canary.Map.Provides[0].Name),
	})
}

func TestDynamicRoutes_BranchWithTraffic(t *testing.T) {
	data := test.BranchWithTraffic()
	main := data.Shiva[0]
	branch := data.Shiva[1]
	testDynamicRoutes(t, data, []string{
		join(main.Map.Name, main.Map.Provides[0].Name),
		join(branch.Map.Name, branch.Deployment.Branch, branch.Map.Provides[0].Name),
	})
}

func TestDynamicRoutes_BranchesProvides(t *testing.T) {
	data := test.BranchesProvides()
	main := data.Shiva[0]
	b1 := data.Shiva[1]
	b2 := data.Shiva[2]
	b3 := data.Shiva[3]
	testDynamicRoutes(t, data, []string{
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

func TestDynamicRoutes_BranchWithoutTraffic(t *testing.T) {
	data := test.BranchWithoutTraffic()
	main := data.Shiva[0]
	branch := data.Shiva[1]
	testDynamicRoutes(t, data, []string{
		join(main.Map.Name, main.Map.Provides[0].Name),
		join(branch.Map.Name, branch.Deployment.Branch, branch.Map.Provides[0].Name),
	})
}

func TestDynamicRoutes_OnlyBranch(t *testing.T) {
	data := test.OnlyBranch()
	branch := data.Shiva[0]
	testDynamicRoutes(t, data, []string{
		join(branch.Map.Name, branch.Deployment.Branch, branch.Map.Provides[0].Name),
	})
}

func TestDynamicRoutes_WithOld(t *testing.T) {
	data := test.WithOld()
	main := data.Shiva[0]
	old := data.Consul[0]
	testDynamicRoutes(t, data, []string{
		join(main.Map.Name, main.Map.Provides[0].Name),
		old.Name + test.LegacyClusterPostfix,
	})
}

func TestDynamicRoutes_WithOldAddressAndOld(t *testing.T) {
	data := test.WithOldAddressAndOld()
	main := data.Shiva[0]
	oldAddr := data.Shiva[0].Map.Provides[0].OldAddressTest
	odlName := strings.ReplaceAll(oldAddr, ".", "_")
	testDynamicRoutes(t, data, []string{
		join(main.Map.Name, main.Map.Provides[0].Name),
		odlName,
	})
}

func testDynamicRoutes(t *testing.T, data *test.Data, result []string) {
	test.Init(t)
	consulMock := mock.NewConsulMock(data.Consul, nil)
	shivaMock := mock.NewShivaMock(data.Shiva)
	log := logger.Logger.WithField("test", "test")
	conf := info.NewConf()
	s := NewService(info.RunService(conf, shivaMock, consulMock, log), consulMock, static.NewService(log, static.NewConf()))
	clusters, err := s.dynamicClusters()
	require.NoError(t, err)
	assertCluster(t, result, clusters)
}

func assertCluster(t *testing.T, want []string, actualClusters []cluster) {
	require.Len(t, actualClusters, len(want))
	actual := make([]string, 0)
	for _, c := range actualClusters {
		actual = append(actual, c.Name)
	}
	sort.Strings(want)
	sort.Strings(actual)
	assert.Equal(t, want, actual)
}

func join(s ...string) string {
	return strings.Join(s, "-")
}

func TestConsulServices(t *testing.T) {
	test.Init(t)
	consulSvc := test.ConsulSvc(t)
	log := logger.Logger.WithField("test", "test")
	conf := info.NewConf()
	s := NewService(info.RunService(conf, ShivaMock{}, consulSvc, log), consulSvc, static.NewService(log, static.NewConf()))

	var response clusterDiscoveryServiceResponse

	time.Sleep(3 * time.Second)

	req, err := http.NewRequest("POST", "/v2/discovery:clusters", nil)
	require.NoError(t, err)

	rec := httptest.NewRecorder()
	handler := http.HandlerFunc(s.Handle)

	handler.ServeHTTP(rec, req)

	if status := rec.Code; status != http.StatusOK {
		t.Errorf("handler returned wrong status code: got %v want %v",
			status, http.StatusOK)
	}

	dec := json.NewDecoder(rec.Body)
	if !assert.NoError(t, dec.Decode(&response)) {
		return
	}

	haveSlb := false
	haveSlbFullThresholds := false
	haveSlbMaxConnections := false

	for _, resource := range response.Resources {
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
			if resource.CircuitBreakers.Thresholds[0].MaxConnections != 2 {
				t.Errorf("Error in threshold %v on %v", "MaxConnections", resource.Name)
			}
			if resource.CircuitBreakers.Thresholds[0].MaxPendingRequests != 3 {
				t.Errorf("Error in threshold %v on %v", "MaxPendingRequests", resource.Name)
			}
			if resource.CircuitBreakers.Thresholds[0].MaxRequests != 4 {
				t.Errorf("Error in threshold %v on %v", "MaxRequests", resource.Name)
			}
			if resource.CircuitBreakers.Thresholds[0].MaxRetries != 5 {
				t.Errorf("Error in threshold %v on %v", "MaxRetries", resource.Name)
			}
		case "slb-max-connections":
			haveSlbMaxConnections = true
			if resource.CircuitBreakers.Thresholds[0].MaxRequests != 0 {
				t.Errorf("Found threshold %v on %v", "MaxRequests", resource.Name)
			}
			if resource.CircuitBreakers.Thresholds[0].MaxConnections != 1 {
				t.Errorf("Error in threshold %v on %v", "MaxRetries", resource.Name)
			}
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
