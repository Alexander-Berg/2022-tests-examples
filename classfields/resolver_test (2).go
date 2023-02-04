package client

import (
	"context"
	"testing"

	"github.com/YandexClassifieds/shiva/arcadia/yp/go/proto/api"
	mock2 "github.com/YandexClassifieds/shiva/test/mock"
	"github.com/golang/protobuf/proto"
	"github.com/stretchr/testify/mock"
	"google.golang.org/grpc"
	"google.golang.org/grpc/resolver"
)

func TestYpResolver_ResolveNow(t *testing.T) {
	mockDiscovery := new(mockDiscoveryServiceClient)
	mockConn := new(mock2.ResolverConn)
	r := &ypResolver{
		dsClient: mockDiscovery,
		cc:       mockConn,
	}
	mockDiscovery.On("GetMasters", mock.Anything, &api.TReqGetMasters{}).Return(&api.TRspGetMasters{
		MasterInfos: []*api.TRspGetMasters_TMasterInfo{
			{GrpcAddress: proto.String("a1:1234")},
			{GrpcAddress: proto.String("a2:1234")},
		},
	}, nil)
	mockConn.On("UpdateState", resolver.State{
		Addresses: []resolver.Address{
			{Addr: "a1:1234"},
			{Addr: "a2:1234"},
		},
	}).Return(nil)
	r.ResolveNow(resolver.ResolveNowOptions{})
	mock.AssertExpectationsForObjects(t, mockDiscovery, mockConn)
}

type mockDiscoveryServiceClient struct {
	mock.Mock
}

func (m *mockDiscoveryServiceClient) GetMasters(ctx context.Context, in *api.TReqGetMasters, opts ...grpc.CallOption) (*api.TRspGetMasters, error) {
	args := m.MethodCalled("GetMasters", ctx, in)
	if arg0 := args.Get(0); arg0 != nil {
		return arg0.(*api.TRspGetMasters), args.Error(1)
	}
	return nil, args.Error(1)
}
