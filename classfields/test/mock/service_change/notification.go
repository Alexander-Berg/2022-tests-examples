package service_change

import (
	"fmt"
	events "github.com/YandexClassifieds/shiva/pb/shiva/events/change_conf"
	"github.com/YandexClassifieds/shiva/pb/shiva/service_map"
	"github.com/stretchr/testify/assert"
	"testing"
)

type NotificationMock struct {
	NewC         map[string]int
	UpdatedC     map[string]int
	DeletedC     map[string]int
	ForceUpdateC map[string]int
	EnvUpdatedC  map[string]int
}

func (m *NotificationMock) Notify(new *service_map.ServiceMap, old *service_map.ServiceMap, changeType events.ChangeType, _ string) error {
	switch changeType {
	case events.ChangeType_NEW:
		return m.New(new)
	case events.ChangeType_UPDATE:
		return m.Updated(new, old)
	case events.ChangeType_DELETE:
		return m.Deleted(old)
	default:
		return fmt.Errorf("change type undefined: " + changeType.String())
	}
}

func NewNotificationMock() *NotificationMock {
	return &NotificationMock{
		NewC:         map[string]int{},
		UpdatedC:     map[string]int{},
		DeletedC:     map[string]int{},
		ForceUpdateC: map[string]int{},
		EnvUpdatedC:  map[string]int{},
	}
}

func (m *NotificationMock) NotifyEnvChange(sMap *service_map.ServiceMap, changeType events.ChangeType, handler, envKey string) error {
	panic("implement me")
}

func (m *NotificationMock) ForceUpdate(sMap *service_map.ServiceMap, envKey string) error {
	m.ForceUpdateC[sMap.Name]++
	return nil
}

func (m *NotificationMock) EnvUpdated(sMap *service_map.ServiceMap, envKey string) error {
	m.EnvUpdatedC[sMap.Name]++
	return nil
}

func (m *NotificationMock) New(sMap *service_map.ServiceMap) error {
	m.NewC[sMap.Name] = m.NewC[sMap.Name] + 1
	return nil
}

func (m *NotificationMock) Updated(new *service_map.ServiceMap, old *service_map.ServiceMap) error {
	m.UpdatedC[new.Name] = m.UpdatedC[new.Name] + 1
	return nil
}

func (m *NotificationMock) Deleted(sMap *service_map.ServiceMap) error {
	m.DeletedC[sMap.Name] = m.DeletedC[sMap.Name] + 1
	return nil
}

func (m *NotificationMock) AssertSMapCalls(t *testing.T, new, upd, del int) {
	assert.Equal(t, new, m.calls(m.NewC))
	assert.Equal(t, upd, m.calls(m.UpdatedC))
	assert.Equal(t, del, m.calls(m.DeletedC))
}

func (m *NotificationMock) AssertEnvCalls(t *testing.T, forced, envUpd int) {
	assert.Equal(t, forced, m.calls(m.ForceUpdateC))
	assert.Equal(t, envUpd, m.calls(m.EnvUpdatedC))
}

func (m *NotificationMock) calls(c map[string]int) int {
	result := 0
	for _, n := range c {
		result += n
	}
	return result
}
