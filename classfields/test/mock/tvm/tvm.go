package tvm

import (
	"context"

	"github.com/YandexClassifieds/go-common/tvm"
	"github.com/stretchr/testify/mock"
)

type TvmClient struct {
	mock.Mock
}

func (m *TvmClient) ServiceTicket(src, dst int) (string, error) {
	args := m.Called(src, dst)
	return args.String(0), args.Error(1)
}

func (m *TvmClient) CheckServiceTicket(ticket string) (*tvm.ServiceTicket, error) {
	args := m.Called(ticket)
	var sti *tvm.ServiceTicket
	if v, ok := args.Get(0).(*tvm.ServiceTicket); ok {
		sti = v
	}
	return sti, args.Error(1)
}

func (m *TvmClient) Ping(ctx context.Context) error {
	panic("implement me")
}
