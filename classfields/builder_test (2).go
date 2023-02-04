package updater

import (
	"testing"

	route "github.com/envoyproxy/go-control-plane/envoy/config/route/v3"
	rbac "github.com/envoyproxy/go-control-plane/envoy/extensions/filters/http/rbac/v3"
	"github.com/envoyproxy/go-control-plane/pkg/cache/types"
	"github.com/golang/protobuf/ptypes"
	"github.com/sirupsen/logrus"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/mock"
	"github.com/stretchr/testify/require"

	"github.com/YandexClassifieds/mesh-control/pb/shiva/api/deploy"
	sm "github.com/YandexClassifieds/mesh-control/pb/shiva/service_map"
)

func TestXdsBuilder_Build(t *testing.T) {
	svc := &sm.ServiceMap{
		Name: "test-service",
		Provides: []*sm.ServiceProvides{
			{Name: "api", Protocol: sm.ServiceProvides_http, Port: 80},
			{Name: "other", Protocol: sm.ServiceProvides_grpc, Port: 42},
		},
		DependsOn: []*sm.ServiceDependency{
			{ServiceName: "other-service", InterfaceName: "s2"},
		},
	}
	otherSvc := &sm.ServiceMap{
		Name: "other-svc",
		Provides: []*sm.ServiceProvides{
			{Name: "s2", Protocol: sm.ServiceProvides_http, Port: 1234},
		},
	}
	otherSvcStatus := []*deploy.StatusResponse_Info{
		{
			Service: "other-svc",
		},
		{
			Service:    "other-svc",
			BranchInfo: &deploy.BranchInfo{Name: "br"},
		},
	}

	ms := new(mockStateService)
	certData := &certInfo{CaChain: []string{}}
	svcList := map[string]dcEntryList{}
	b := newXdsBuilder(svc, "", ms, certData, "", svcList, logrus.New())

	ms.On("ChildServiceNames", "test-service", "api").Return([]string{"client-service"})
	ms.On("ChildServiceNames", "test-service", "other").Return(nil)
	ms.On("StatusForService", "other-service").Return(otherSvcStatus)
	ms.On("GetServiceMap", "other-service").Return(otherSvc)

	snapshot, err := b.Build()
	require.NoError(t, err)

	routes := snapshot.Resources[types.Route].Items
	clusters := snapshot.Resources[types.Cluster].Items
	assert.Len(t, snapshot.Resources[types.Listener].Items, 2)
	assert.Len(t, routes, 2)
	if assert.Contains(t, routes, "ingress") {
		r := routes["ingress"].(*route.RouteConfiguration)
		vhosts := make(map[string]*route.VirtualHost, 0)
		for _, vh := range r.VirtualHosts {
			vhosts[vh.GetName()] = vh
		}
		require.Contains(t, vhosts, "ingress-api")
		assert.NotEmpty(t, vhosts["ingress-api"].TypedPerFilterConfig[wkRbac])
		rbacData := &rbac.RBACPerRoute{}
		err := ptypes.UnmarshalAny(vhosts["ingress-api"].TypedPerFilterConfig[wkRbac], rbacData)
		require.NoError(t, err)
	}
	assert.Contains(t, routes, "egress")
	assert.Contains(t, clusters, "egress-other-service-s2")
	assert.Contains(t, clusters, "egress-other-service-s2:br")
	assert.Contains(t, clusters, "ingress-api")
	assert.Contains(t, clusters, "ingress-other")
}

type mockStateService struct {
	mock.Mock
}

func (m *mockStateService) GetServiceMap(service string) *sm.ServiceMap {
	args := m.Called(service)
	if retVal, ok := args.Get(0).(*sm.ServiceMap); ok {
		return retVal
	}
	return nil
}

func (m *mockStateService) StatusForService(service string) []*deploy.StatusResponse_Info {
	args := m.Called(service)
	if retVal, ok := args.Get(0).([]*deploy.StatusResponse_Info); ok {
		return retVal
	}
	return nil
}

func (m *mockStateService) ChildServiceNames(service, interfaceName string) []string {
	args := m.Called(service, interfaceName)
	if retVal, ok := args.Get(0).([]string); ok {
		return retVal
	}
	return nil
}
