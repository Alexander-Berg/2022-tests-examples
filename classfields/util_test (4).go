package proxy

import (
	"context"
	"github.com/YandexClassifieds/sender-proxy/pkg/broker"
	"github.com/stretchr/testify/mock"
)

type mockStaff struct {
	mock.Mock
}

func (m *mockStaff) ExistsByNumber(_ context.Context, number string) bool {
	return m.Called(number).Bool(0)
}

func (m *mockStaff) ExistsByEmail(_ context.Context, addr string) bool {
	return m.Called(addr).Bool(0)
}

type mockBroker struct {
	mock.Mock
	Data []broker.Event
}

func (m *mockBroker) Add(evt broker.Event) {
	m.Called(evt)
}
