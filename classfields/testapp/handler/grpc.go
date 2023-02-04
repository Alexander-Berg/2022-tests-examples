package handler

import (
	"context"
	"fmt"
	"github.com/spf13/viper"
	"google.golang.org/grpc/examples/features/proto/echo"
)

type GRPCHandler struct {
	echo.UnsafeEchoServer
}

func (h *GRPCHandler) UnaryEcho(ctx context.Context, req *echo.EchoRequest) (*echo.EchoResponse, error) {
	return &echo.EchoResponse{
		Message: h.prepareResponse(req.Message),
	}, nil
}

func (h GRPCHandler) ServerStreamingEcho(req *echo.EchoRequest, stream echo.Echo_ServerStreamingEchoServer) error {
	msg := h.prepareResponse(req.Message)
	for i := 0; i < 10; i++ {
		err := stream.Send(&echo.EchoResponse{Message: msg})
		if err != nil {
			panic(err)
		}
	}
	return nil
}

func (*GRPCHandler) ClientStreamingEcho(echo.Echo_ClientStreamingEchoServer) error {
	panic("implement me")
}

func (*GRPCHandler) BidirectionalStreamingEcho(echo.Echo_BidirectionalStreamingEchoServer) error {
	panic("implement me")
}

func (*GRPCHandler) prepareResponse(msg string) string {
	return fmt.Sprintf("key='%s' value='%s'", msg, viper.GetString(msg))
}
