package oauth_test

import (
	"github.com/YandexClassifieds/go-common/blackbox"
	"github.com/YandexClassifieds/go-common/blackbox/grpc/oauth"
	"github.com/YandexClassifieds/go-common/tvm"
	grpcMiddleware "github.com/grpc-ecosystem/go-grpc-middleware"
	"google.golang.org/grpc"
)

func Example_newMiddleware() {
	// params
	var (
		tvmClient tvm.Client
	)

	bb := blackbox.NewClient(blackbox.Config{SelfTvmId: 42}, tvmClient)
	middleware := oauth.NewMiddleware(bb)
	grpc.NewServer(
		grpc.StreamInterceptor(grpcMiddleware.ChainStreamServer(middleware.StreamExtractor(), middleware.StreamValidator())),
		grpc.UnaryInterceptor(grpcMiddleware.ChainUnaryServer(middleware.UnaryExtractor(), middleware.UnaryValidator())),
	)
}
