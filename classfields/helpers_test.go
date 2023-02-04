package endpoint

import (
	"github.com/YandexClassifieds/envoy-api/common/logger"
	"github.com/YandexClassifieds/envoy-api/info"
	infoPb "github.com/YandexClassifieds/envoy-api/pb/shiva/types/info"
	"github.com/YandexClassifieds/envoy-api/static"
	"github.com/YandexClassifieds/envoy-api/test"
	"github.com/YandexClassifieds/envoy-api/utils"
	"github.com/stretchr/testify/assert"
	"net"
	"sort"
	"testing"
)

func TestGetClusters(t *testing.T) {
	test.Init(t)
	t.Run("basic", testBasic)
	t.Run("multi-domain", testMulti)
	t.Run("cut-suffix", testCutSuffix)
	t.Run("fqdn", testFqdn)
}

func testBasic(t *testing.T) {
	log := logger.Logger.WithField("test", "test")
	consulSvc := test.ConsulSvc(t)
	conf := info.NewConf()
	s := NewService(log, info.RunService(conf, ShivaMock{}, consulSvc, log), static.NewService(log, static.NewConf()))
	la, err := s.getEndpoints([]string{"websvc"}, "", "")
	if !assert.NoError(t, err) {
		return
	}
	sortAssignment(la)
	if !assert.Len(t, la, 1) {
		return
	}
	assert.Equal(t, "websvc", la[0].ClusterName)
	if !assert.Len(t, la[0].Endpoints, 1) {
		return
	}
	expectedEndpoints := []localityLbEndpoint{
		{
			LbEndpoints: []lbEndpoint{
				{Endpoint: endpoint{Address: endpointAddress{SocketAddress: socketAddress{Address: "::1", Port: 80}}}},
				{Endpoint: endpoint{Address: endpointAddress{SocketAddress: socketAddress{Address: "::2", Port: 80}}}},
			},
			Priority: 0,
		},
	}
	assert.Equal(t, expectedEndpoints, la[0].Endpoints)
}

func testMulti(t *testing.T) {
	consulSvc := test.ConsulSvc(t)
	log := logger.Logger.WithField("test", "test")
	conf := info.NewConf()
	s := NewService(log, info.RunService(conf, ShivaMock{}, consulSvc, log), static.NewService(log, static.NewConf()))
	la1, err := s.getEndpoints([]string{"multi1"}, "", "")
	if !assert.NoError(t, err) {
		return
	}
	la2, err := s.getEndpoints([]string{"multi2"}, "", "")
	if !assert.NoError(t, err) {
		return
	}

	sortAssignment(la1)
	sortAssignment(la2)
	if !assert.Len(t, la1, 1) || !assert.Len(t, la2, 1) {
		return
	}
	assert.Equal(t, la1[0].Endpoints, la2[0].Endpoints)
	assert.Len(t, la1[0].Endpoints[0].LbEndpoints, 1)
}

func testCutSuffix(t *testing.T) {
	consulSvc := test.ConsulSvc(t)
	log := logger.Logger.WithField("test", "test")
	conf := info.NewConf()
	s := NewService(log, info.RunService(conf, ShivaMock{}, consulSvc, log), static.NewService(log, static.NewConf()))
	la, err := s.getEndpoints([]string{"cut"}, "", "")
	if !assert.NoError(t, err) {
		return
	}
	assert.Len(t, la, 1)
}

func testFqdn(t *testing.T) {
	consulSvc := test.ConsulSvc(t)
	log := logger.Logger.WithField("test", "test")
	conf := info.NewConf()
	s := NewService(log, info.RunService(conf, ShivaMock{}, consulSvc, log), static.NewService(log, static.NewConf()))
	la, err := s.getEndpoints([]string{"fqdn_vertis_yandex_net"}, "", "")
	if !assert.NoError(t, err) {
		return
	}
	assert.Len(t, la, 1)
}

func TestGetPriority(t *testing.T) {
	utils.EnvoyApiDatacenter = "sas"
	assert.Equal(t, 0, getPriority("sas", DefaultVertisNodeId))
	assert.Equal(t, 1, getPriority("myt", DefaultVertisNodeId))
	// GetPriority doesn't check whether datacenter name is valid
	assert.Equal(t, 1, getPriority("mytascasc", DefaultVertisNodeId))
	assert.Equal(t, 0, getPriority("myt", "myt"))
	assert.Equal(t, 1, getPriority("myt", "sas"))
	assert.Equal(t, 0, getPriority("vla", "vla"))
	assert.Equal(t, 1, getPriority("vla", "sas"))
	assert.Equal(t, 1, getPriority("myt", "vla"))
}

func TestGetIpv6LbEndpoints(t *testing.T) {
	ips1 := []net.IP{
		net.ParseIP("2001:0db8:0001:0000:0000:0ab9:C0A8:0102"),
		net.ParseIP("19.117.63.126"),
		net.ParseIP("684D:1111:222:3333:4444:5555:6:77"),
	}
	expectedLbEndpoints1 := []lbEndpoint{
		{
			Endpoint: endpoint{
				Address: endpointAddress{
					SocketAddress: socketAddress{
						Address: net.ParseIP("2001:0db8:0001:0000:0000:0ab9:C0A8:0102").String(),
						Port:    2345,
					},
				},
			},
		},
		{
			Endpoint: endpoint{
				Address: endpointAddress{
					SocketAddress: socketAddress{
						Address: net.ParseIP("684D:1111:222:3333:4444:5555:6:77").String(),
						Port:    2345,
					},
				},
			},
		},
	}
	checkGetIpv6LbEndpoints(t, ips1, 2345, expectedLbEndpoints1, nil)
}

func checkGetIpv6LbEndpoints(t *testing.T, ips []net.IP, port int64, expectedLbEndpoints []lbEndpoint, expectedErr error) {
	lbEndpoints, err := getIpv6LbEndpoints(ips, port)
	assert.Equal(t, expectedErr, err)
	assert.Equal(t, expectedLbEndpoints, lbEndpoints)
}

func checkGetPriority(t *testing.T, datacenter string, nodeId string, expectedPriority int) {
	priority := getPriority(datacenter, nodeId)

	if priority != expectedPriority {
		t.Errorf("Failed to get priority for datacenter '%s': got %d, expected %d", datacenter, priority, expectedPriority)
	}
}

func sortAssignment(c []clusterLoadAssignment) {
	for i, cluster := range c {
		sort.Slice(cluster.Endpoints, func(i, j int) bool {
			return cluster.Endpoints[i].Priority < cluster.Endpoints[j].Priority
		})
		for j, endpoints := range cluster.Endpoints {
			sort.Slice(endpoints.LbEndpoints, func(i, j int) bool {
				return endpoints.LbEndpoints[i].Endpoint.Address.SocketAddress.Address < endpoints.LbEndpoints[j].Endpoint.Address.SocketAddress.Address
			})
			cluster.Endpoints[j] = endpoints
		}
		c[i] = cluster
	}
}

type ShivaMock struct {
}

func (s ShivaMock) StatusInfo() []*infoPb.DeploymentInfo {
	return []*infoPb.DeploymentInfo{}
}
