package main

import (
	"fmt"
	"math/rand"
	"net"
	"net/http"
	"strconv"
	"time"

	"github.com/YandexClassifieds/go-common/log/logrus"
	"github.com/YandexClassifieds/shiva/common/logger"

	"github.com/YandexClassifieds/shiva/cmd/testapp/handler"
	"github.com/YandexClassifieds/shiva/common/config"
	"google.golang.org/grpc"
	"google.golang.org/grpc/examples/features/proto/echo"
	"google.golang.org/grpc/health"
	"google.golang.org/grpc/health/grpc_health_v1"
)

var (
	log = logger.NewLogger(logrus.NewLogger(
		logrus.WithModuleIgnore(logger.SkipModules...),
		logrus.WithMetrics(),
	), nil)
	envVarsList = []string{"_DEPLOY_DC", "_DEPLOY_LAYER", "_DEPLOY_HOSTNAME", "_DEPLOY_ALLOC_ID",
		"_DEPLOY_SERVICE_NAME", "_DEPLOY_APP_VERSION", "_DEPLOY_BRANCH", "_DEPLOY_CANARY"}
	// TODO delete global variables
)

func init() {
	config.Init("testapp")
}

func main() {
	if config.SafeBool("OOM") {
		makeOOM()
	}

	if config.SafeBool("CI_MODE") {
		ciHelper := NewCIHelper(log)
		if ciHelper.OOMEnable() {
			makeOOM()
		}
		ciHelper.rescheduleOnDemand()
	}

	go monitoring()
	healthCheck()

	if config.SafeBool("FAIL") {
		go func() {
			time.Sleep(config.Duration("FAIL_TIME"))
			panic("panic")
		}()
	}
	if config.SafeBool("IS_BATCH") {
		batchTime := config.Duration("BATCH_TIME")
		log.Infof("batching for %s", batchTime)
		<-time.After(batchTime)
		log.Info("stopping batch")
		return
	}
	ticker()
}

func ticker() {
	ticker := time.NewTicker(1 * time.Second)
	for {
		t := <-ticker.C
		log.Infof("tick: %v", t)
	}
}

func makeOOM() {
	log.Infof("make OOM")
	forOOM := ""
	for {
		forOOM = forOOM + strconv.Itoa(rand.Int()) + forOOM
	}
}

func healthCheck() {
	apiType := config.Str("API")
	switch apiType {
	case "http":
		go httpHealthCheck()
	case "grpc":
		go grpcHealthCheck()
	case "tcp":
		go tcpHealthCheck()
	case "no":
		return
	default:
		panic("unknown api type")
	}
}

func monitoring() {
	http.HandleFunc("/ping", func(w http.ResponseWriter, r *http.Request) {
		w.WriteHeader(200)
	})

	go httpListener(config.Int("OPS_PORT"))
}

func tcpHealthCheck() {
	l, err := net.Listen("tcp", ":"+config.Str("API_PORT"))
	if err != nil {
		panic(err)
	}

	for {
		conn, err := l.Accept()
		if err != nil {
			log.Fatal(err)
		}
		go conn.Close()
	}
}

func grpcHealthCheck() {
	lis, err := net.Listen("tcp", ":"+config.Str("API_PORT"))
	if err != nil {
		panic(err)
	}

	grpcServer := grpc.NewServer()
	hcServer := health.NewServer()
	hcServer.SetServingStatus(config.Str("SERVICE_NAME"), grpc_health_v1.HealthCheckResponse_SERVING)
	grpc_health_v1.RegisterHealthServer(grpcServer, hcServer)
	echo.RegisterEchoServer(grpcServer, &handler.GRPCHandler{})
	err = grpcServer.Serve(lis)
	if err != nil {
		panic(err)
	}
}

func httpHealthCheck() {
	http.HandleFunc("/keep5", func(w http.ResponseWriter, r *http.Request) {
		time.Sleep(5 * time.Second)
		w.WriteHeader(200)
	})

	http.HandleFunc("/keep10", func(w http.ResponseWriter, r *http.Request) {
		time.Sleep(10 * time.Second)
		w.WriteHeader(200)
	})

	go httpListener(config.Int("API_PORT"))

	if config.IsSet("API_PORT2") {
		http.HandleFunc("/vars", getVars)

		go httpListener(config.Int("API_PORT2"))
	}
}

func httpListener(port int) {
	err := http.ListenAndServe(fmt.Sprintf(":%d", port), nil)
	if err != nil {
		panic(err)
	}
}

func getVars(w http.ResponseWriter, _ *http.Request) {
	for _, v := range envVarsList {
		_, _ = fmt.Fprintf(w, "%s: %s\n", v, config.Str(v))
	}
}
