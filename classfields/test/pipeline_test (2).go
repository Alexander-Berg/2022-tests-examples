package test

import (
	"bytes"
	"fmt"
	"net"
	"strings"
	"sync"
	"testing"
	"time"

	vlog "github.com/YandexClassifieds/go-common/log"
	vlogrus "github.com/YandexClassifieds/go-common/log/logrus"
	"github.com/YandexClassifieds/goLB"
	"github.com/YandexClassifieds/goLB/producer"
	backendApp "github.com/YandexClassifieds/vtail/cmd/backend/app"
	"github.com/YandexClassifieds/vtail/cmd/cli/commands"
	"github.com/YandexClassifieds/vtail/cmd/cli/commands/format"
	streamerApp "github.com/YandexClassifieds/vtail/cmd/streamer/app"
	"github.com/YandexClassifieds/vtail/cmd/streamer/source/auth"
	"github.com/YandexClassifieds/vtail/internal/logs"
	"github.com/sirupsen/logrus"
	"github.com/spf13/viper"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
	"go.uber.org/goleak"
)

func TestPipeline(t *testing.T) {
	// ignore grpc goroutines
	defer goleak.VerifyNone(t,
		goleak.IgnoreTopFunction("google.golang.org/grpc.(*ccBalancerWrapper).watcher"),
		goleak.IgnoreTopFunction("google.golang.org/grpc.(*addrConn).resetTransport"),
		goleak.IgnoreTopFunction("github.com/desertbit/timer.timerRoutine"),
	)

	InitConfig(t)
	path := ZkPrepare(t)
	viper.Set("zk_path", path)

	log := logrus.New()
	log.Level = logrus.DebugLevel
	logger := vlogrus.Wrap(log.WithField(vlog.ContextF, "test"))

	shutdownChan := make(chan struct{})
	defer func() {
		close(shutdownChan)
		time.Sleep(time.Second / 2)
	}()

	//streamer

	go streamerApp.Main(shutdownChan, logger)
	// wait for init
	time.Sleep(time.Second / 2)

	//backend

	tlsListenString := "localhost:" + findAvailablePort(t)
	viper.Set("backend_grpc_tls_listen", tlsListenString)
	go backendApp.Main(shutdownChan, logger)
	// wait for init
	time.Sleep(time.Second / 2)

	// cli
	out := &bytes.Buffer{}
	fields := []string{"level", "message"}
	tail := &commands.Tail{
		Log:                   log,
		Address:               tlsListenString,
		Query:                 "service=pl-dev",
		Formatter:             format.NewFormatter(format.Json, fields, log),
		Fields:                fields,
		OAuthToken:            viper.GetString("vtail_oauth_token"),
		TlsServerNameOverride: viper.GetString("vtail_tls_server_name_override"),
		Out:                   out,
	}
	go func() {
		err := tail.Exec(shutdownChan)
		if err == commands.SilentExitError {
			return
		}
		require.NoError(t, err)
	}()

	writeLogLines(t, logger)

	success := assert.Eventually(t, func() bool {
		for i := 0; i < 5; i++ {
			if !strings.Contains(out.String(), fmt.Sprintf(`{"level":"INFO","message":"%d"}`, i)) {
				return false
			}
		}
		return true
	}, time.Second*5, time.Second/2, "can't find log entries in output")
	if !success {
		fmt.Printf("out buf: %s\n", out.String())
	}
}

func writeLogLines(t *testing.T, logger vlog.Logger) {
	logger = logs.WithContext(logger, "writer")
	tokenProvider := auth.TokenProvider(logger)

	cfg := &producer.Config{
		Address:        viper.GetString("_deploy_dc") + "." + viper.GetString("lb_address"),
		Topic:          viper.GetString("lb_topic"),
		SourceId:       fmt.Sprintf("%s_%d", "vtail_pipeline_test", 0),
		Codec:          goLB.CodecGzip,
		GzipLevel:      1,
		PartitionGroup: 0,
	}
	simpleProducer := producer.NewBatchProducer(cfg, tokenProvider, logger.WithField(vlog.ContextF, "producer"))
	_, err := simpleProducer.Init()
	require.NoError(t, err)
	defer simpleProducer.Close()

	logger.Debug("start writing messages")
	wg := sync.WaitGroup{}
	wg.Add(5)
	for i := 0; i < 5; i++ {
		simpleProducer.Push(&goLB.Data{
			Body: []byte(fmt.Sprintf(`{"_level": "INFO", "_service": "pl-dev", "_layer": "pipeline", "_message": "%d", "_time": "%s"}"`, i, time.Now().Format(time.RFC3339Nano))),
			OnSuccess: func() {
				logger.Debug("wrote 1 message")
				wg.Done()
			},
			OnFail: func() {
				require.FailNow(t, "fail to push message to LB")
			},
		})
	}
	wg.Wait()
	logger.Debug("done writing messages")
}

func findAvailablePort(t *testing.T) string {
	lis, err := net.Listen("tcp", "localhost:0")
	require.NoError(t, err)
	defer lis.Close()

	return strings.Replace(lis.Addr().String(), "127.0.0.1:", "", 1)
}
