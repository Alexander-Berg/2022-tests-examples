package testtools

import (
	"fmt"
	"math"

	"a.yandex-team.ru/infra/maxwell/go/pkg/walle"
)

type TestWalle struct {
	// mock.Mock
	WalleStatuses []*walle.HostStatus
}

func CreateEmptyTestWalle() walle.IClient {
	return &TestWalle{WalleStatuses: make([]*walle.HostStatus, 0)}
}

func CreateTestWalle(statuses []*walle.HostStatus) walle.IClient {
	return &TestWalle{
		// Mock:          mock.Mock{},
		WalleStatuses: statuses,
	}
}

func (w *TestWalle) GetHost(hostname string) (*walle.HostStatus, error) {
	for _, h := range w.WalleStatuses {
		if h.Name == hostname {
			return h, nil
		}
	}
	return nil, fmt.Errorf("host not found")
}

func (w *TestWalle) GetHosts(req *walle.GetHostsRequest) (*walle.HostsStatus, error) {
	capacity := int(math.Min(float64(req.Params.Limit), float64(len(w.WalleStatuses))))
	hostsCollection := make([]walle.HostStatus, capacity)
	for i := 0; i < capacity; i++ {
		hostsCollection[i] = *w.WalleStatuses[i]
	}
	return &walle.HostsStatus{Result: hostsCollection, Total: capacity}, nil
}

func (w *TestWalle) ProfileHost(req *walle.ProfileHostRequest) ([]byte, error) {
	return make([]byte, 0), nil
}

func (w *TestWalle) RedeployHost(req *walle.RedeployHostRequest) ([]byte, error) {
	return make([]byte, 0), nil
}

func (w *TestWalle) RebootHost(req *walle.RebootHostRequest) ([]byte, error) {
	return make([]byte, 0), nil
}

func (w *TestWalle) ValidateRestrictions(hostname string, checkRestrictions []string) (bool, error) {
	return true, nil
}

func (w *TestWalle) GetHealthCheck(fqdn string, checkName string) (*walle.HealthCheck, error) {
	return nil, fmt.Errorf("not implemented")
}

func (w *TestWalle) GetHealthChecks(fqdn string, checks []string) (*walle.HealthChecksResp, error) {
	return nil, fmt.Errorf("not implemented")
}

func (w *TestWalle) GetProject(req *walle.GetProjectRequest) (*walle.ProjectResp, error) {
	return &walle.ProjectResp{
		HealingAutomation: &walle.ProjectAutomation{Enabled: true},
		DNSAutomation:     &walle.ProjectAutomation{Enabled: true},
	}, nil
}

func (w *TestWalle) IsProjectOwner(project string, owner string) (bool, error) {
	return false, fmt.Errorf("not implemented")
}
