package secrets

import (
	"context"
	"sort"

	proto "github.com/YandexClassifieds/shiva/pb/ss/access"
	"github.com/stretchr/testify/mock"
	"google.golang.org/grpc"
)

type MockTokenClient struct {
	mock.Mock
}

func (m *MockTokenClient) NewToken(ctx context.Context, in *proto.NewTokenRequest, opts ...grpc.CallOption) (*proto.NewTokenResponse, error) {
	sort.Sort(secretList(in.Env))
	args := m.Called(ctx, in)
	if r, ok := args.Get(0).(*proto.NewTokenResponse); ok {
		return r, args.Error(1)
	}
	return nil, args.Error(1)
}

func (m *MockTokenClient) RemoveToken(ctx context.Context, in *proto.RemoveTokenRequest, opts ...grpc.CallOption) (*proto.RemoveTokenResponse, error) {
	panic("implement me")
}

func (m *MockTokenClient) CheckToken(ctx context.Context, in *proto.CheckTokenRequest, opts ...grpc.CallOption) (*proto.CheckTokenResponse, error) {
	args := m.Called(ctx, in)
	if r, ok := args.Get(0).(*proto.CheckTokenResponse); ok {
		return r, args.Error(1)
	}
	return nil, args.Error(1)
}

func (m *MockTokenClient) GetDelegationToken(ctx context.Context, in *proto.GetDelegationTokenRequest, opts ...grpc.CallOption) (*proto.GetDelegationTokenResponse, error) {
	args := m.Called(ctx, in)
	if r, ok := args.Get(0).(*proto.GetDelegationTokenResponse); ok {
		return r, args.Error(1)
	}
	return nil, args.Error(1)
}
