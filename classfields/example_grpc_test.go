package tvm_test

import (
	"github.com/YandexClassifieds/go-common/tvm"
	"github.com/YandexClassifieds/go-common/tvm/grpc/srvticket"
	"github.com/YandexClassifieds/go-common/tvm/tvmtool"
	grpcMiddleware "github.com/grpc-ecosystem/go-grpc-middleware"
	"google.golang.org/grpc"
)

func Example_tvmGrpcServer() {

	// params
	var (
		url, token    string
		selfTvmId     int
		allowedTvmIds []int
	)

	tvmClient := tvmtool.NewClient(tvmtool.NewConf(url, token))
	middleware := srvticket.NewMiddleware(tvmClient, selfTvmId, allowedTvmIds)
	grpc.NewServer(
		grpc.StreamInterceptor(grpcMiddleware.ChainStreamServer(middleware.StreamExtractor(), middleware.StreamValidator())),
		grpc.UnaryInterceptor(grpcMiddleware.ChainUnaryServer(middleware.UnaryExtractor(), middleware.UnaryValidator())),
	)
}

func Example_tvmGrpcClient() {
	// client ticket auth example
	var (
		srcTvmId, dstTvmId int
		tvmCli             tvm.Client // initialized elsewhere
	)

	// dial with tvm service ticketauth
	cc, err := grpc.Dial("some-tvm-api.vertis.yandex.net:1234",
		srvticket.WithPerRPCCredentials(tvmCli, srcTvmId, dstTvmId),
	)

	_ = cc
	_ = err
}
