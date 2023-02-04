package mock

import (
	"context"
	"github.com/stretchr/testify/mock"
	"google.golang.org/grpc"
)

type ServerStream struct {
	mock.Mock
	grpc.ServerStream
	ctx context.Context
}

func NewServerStream() *ServerStream {
	return &ServerStream{
		ctx: context.Background(),
	}
}

func (m *ServerStream) Context() context.Context {
	return m.ctx
}

func (m *ServerStream) SendMsg(msg interface{}) error {
	return m.Called(msg).Error(0)
}

func (m *ServerStream) RecvMsg(msg interface{}) error {
	return m.Called(msg).Error(0)
}
