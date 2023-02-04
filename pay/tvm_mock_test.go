package server

import (
	"context"
	"errors"

	"a.yandex-team.ru/library/go/yandex/tvm"
)

type MockedTVMClient struct {
	CheckServiceTicketFunc func(ctx context.Context, ticket string) (*tvm.CheckedServiceTicket, error)
}

func NewMockedTVMClient() *MockedTVMClient {
	return &MockedTVMClient{}
}

var _ tvm.Client = &MockedTVMClient{}

func (t *MockedTVMClient) GetServiceTicketForAlias(context.Context, string) (string, error) {
	panic("implement me")
}

func (t *MockedTVMClient) GetServiceTicketForID(context.Context, tvm.ClientID) (string, error) {
	panic("implement me")
}

func (t *MockedTVMClient) CheckServiceTicket(ctx context.Context, ticket string) (*tvm.CheckedServiceTicket, error) {
	if t.CheckServiceTicketFunc == nil {
		return nil, errors.New("tvm mock: override CheckServiceTicket")
	}

	return t.CheckServiceTicketFunc(ctx, ticket)
}

func (t *MockedTVMClient) CheckUserTicket(context.Context, string, ...tvm.CheckUserTicketOption) (*tvm.CheckedUserTicket, error) {
	panic("implement me")
}

func (t *MockedTVMClient) GetStatus(context.Context) (tvm.ClientStatusInfo, error) {
	panic("implement me")
}

func (t *MockedTVMClient) GetRoles(ctx context.Context) (*tvm.Roles, error) {
	return nil, errors.New("not implemented")
}
