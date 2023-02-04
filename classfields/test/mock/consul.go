package mock

import (
	"time"

	"github.com/YandexClassifieds/envoy-api/consul"
)

const (
	oldDefaultTimeout = time.Second * 5
	newDefaultTimeout = time.Second * 10
)

type ConsulMock struct {
	data     map[string]*consul.ServiceInfo
	settings map[string]consul.SettingsInfo
}

func NewConsulMock(data []*consul.ServiceInfo, settings map[string]consul.SettingsInfo) consul.IService {
	result := make(map[string]*consul.ServiceInfo)
	for _, d := range data {
		result[d.Name] = d
	}
	return &ConsulMock{
		data: result,
		settings: settings,
	}
}

func (c *ConsulMock) DefaultSettings() consul.SettingsInfo {
	return consul.SettingsInfo{
		RDS: consul.RDSSettings{
			Route: consul.Route{
				UpstreamTimeout: consul.DurationWrapper{Value: newDefaultTimeout},
			},
		},
	}
}

func (c *ConsulMock) OldSettings(domain string) consul.SettingsInfo {
	if v, ok := c.settings[domain]; ok {
		return v
	}
	return consul.SettingsInfo{
		CDS: consul.CDSSettings{
			ConnectTimeout: consul.DurationWrapper{Value: time.Millisecond * 250},
			HealthChecks: consul.HealthChecks{
				Timeout:  consul.DurationWrapper{Value: time.Second},
				Interval: consul.DurationWrapper{Value: time.Second},
			},
		},
		RDS: consul.RDSSettings{
			Route: consul.Route{
				UpstreamTimeout: consul.DurationWrapper{Value: oldDefaultTimeout},
			},
		},
	}
}

func (c *ConsulMock) Info() map[string]*consul.ServiceInfo {
	return c.data
}
