package main

import (
	"context"
	"fmt"
	"net"
	"net/http"
	"os"
	"os/signal"
	"syscall"

	"github.com/YandexClassifieds/shiva/cmd/testapp/handler"
	"github.com/YandexClassifieds/shiva/common/config"
	hpb "github.com/YandexClassifieds/shiva/pb/shiva/api/hook"
	"github.com/YandexClassifieds/shiva/test"
	"google.golang.org/grpc"
	"google.golang.org/grpc/examples/features/proto/echo"
	"google.golang.org/grpc/health"
	"google.golang.org/grpc/health/grpc_health_v1"
)

var (
	log = test.NewLoggerWithoutTest()
)

type HookLogger struct {
}

func (h *HookLogger) Handle(_ context.Context, r *hpb.HookRequest) (*hpb.HookResponse, error) {
	log.Infof("Handle: %s", r.Deployment.Id)
	return &hpb.HookResponse{}, nil
}

func init() {
	config.Init("testapp_hook")
}

func main() {

	shutdown := make(chan bool)
	go shutdownHook(shutdown)
	go GRPCInit()
	go monitoring()
	<-shutdown
}

func monitoring() {
	http.HandleFunc("/ping", func(w http.ResponseWriter, r *http.Request) {
		w.WriteHeader(200)
	})
	http.HandleFunc("/metrics", func(w http.ResponseWriter, r *http.Request) {
		w.WriteHeader(200)
	})

	go httpListener(config.Int("OPS_PORT"))
}

func GRPCInit() {
	lis, err := net.Listen("tcp", ":"+config.Str("API_PORT"))
	if err != nil {
		panic(err)
	}

	grpcServer := grpc.NewServer()
	hcServer := health.NewServer()
	hcServer.SetServingStatus(config.Str("SERVICE_NAME"), grpc_health_v1.HealthCheckResponse_SERVING)
	grpc_health_v1.RegisterHealthServer(grpcServer, hcServer)
	hpb.RegisterHookHandlerServer(grpcServer, &HookLogger{})
	echo.RegisterEchoServer(grpcServer, &handler.GRPCHandler{})
	err = grpcServer.Serve(lis)
	if err != nil {
		panic(err)
	}
}

func httpListener(port int) {
	err := http.ListenAndServe(fmt.Sprintf(":%d", port), nil)
	if err != nil {
		panic(err)
	}
}

func shutdownHook(shutdown chan bool) {
	signals := make(chan os.Signal, 1)
	signal.Notify(signals, syscall.SIGINT, syscall.SIGTERM)
	<-signals
	close(shutdown)
}
