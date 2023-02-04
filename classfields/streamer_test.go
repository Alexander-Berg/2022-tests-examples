package handler_test

import (
	"testing"
	"time"

	vlog "github.com/YandexClassifieds/go-common/log"
	"github.com/YandexClassifieds/vtail/api/backend"
	"github.com/YandexClassifieds/vtail/api/core"
	"github.com/YandexClassifieds/vtail/cmd/streamer/handler"
	"github.com/YandexClassifieds/vtail/internal/http"
	"github.com/YandexClassifieds/vtail/internal/task"
	"github.com/YandexClassifieds/vtail/test"
	"github.com/spf13/viper"
	"github.com/stretchr/testify/require"
	"go.uber.org/goleak"
	"google.golang.org/grpc"
	"google.golang.org/protobuf/types/known/timestamppb"
)

func TestNewStreamer(t *testing.T) {
	defer goleak.VerifyNone(t)

	test.InitConfig(t)
	zkPath := test.ZkPrepare(t)
	logger := test.NewTestLogger()
	zkAddresses := viper.GetStringSlice("zk_addresses")

	observer := task.NewObserver(zkAddresses, zkPath, logger)
	defer observer.Close()

	storage, err := task.NewStorage(zkAddresses, zkPath, logger)
	require.NoError(t, err)
	defer storage.Close()

	source := make(chan *core.LogMessage, 1)
	streamer := handler.NewStreamer(source, observer, storage, logger)
	go streamer.HandleFlows()
	defer streamer.Close()

	address, grpcServer, testServer := StartBackendServer(t, logger)
	defer grpcServer.Stop()

	logger.Debug("add new flow")
	flowId, err := storage.AddFlow(&task.Flow{
		Filters:     &core.Parenthesis{},
		Include:     &backend.IncludeFields{Message: true},
		Destination: address,
	})
	require.NoError(t, err)

	logger.Debug("wait for connected client")
	<-testServer.ClientConnected
	logger.Debug("client connected")

	logger.Debug("write message to source")
	source <- getLogMessage(t)

	req := <-testServer.Out
	require.Equal(t, flowId, req.FlowId)

	logger.Debug("shutdown all")
}

func getLogMessage(t *testing.T) *core.LogMessage {
	ts := timestamppb.New(time.Unix(1580458894, 324))

	return &core.LogMessage{
		Timestamp: ts,
		Service:   "srv",
		Version:   "1.2",
		Layer:     "test",
		Level:     "INFO",
		Message:   "random message",
		Rest:      `{"myField": "my field value"}`,
	}
}

func StartBackendServer(t *testing.T, logger vlog.Logger) (string, *grpc.Server, *TestServer) {
	lis := http.Listen("localhost:0", logger)

	grpcServer := grpc.NewServer()
	server := NewTestServer(t)
	backend.RegisterLogsWriterServer(grpcServer, server)

	go func() {
		err := grpcServer.Serve(lis)
		require.NoError(t, err)
	}()

	return lis.Addr().String(), grpcServer, server
}

func TestMessagesWithUnavailableBackend(t *testing.T) {
	// ignore grpc goroutines
	defer goleak.VerifyNone(t,
		goleak.IgnoreTopFunction("google.golang.org/grpc.(*ccBalancerWrapper).watcher"),
		goleak.IgnoreTopFunction("google.golang.org/grpc.(*addrConn).resetTransport"))

	test.InitConfig(t)
	zkPath := test.ZkPrepare(t)
	logger := test.NewTestLogger()
	zkAddresses := viper.GetStringSlice("zk_addresses")

	observer := task.NewObserver(zkAddresses, zkPath, logger)
	defer observer.Close()

	storage, err := task.NewStorage(zkAddresses, zkPath, logger)
	require.NoError(t, err)
	defer storage.Close()

	source := make(chan *core.LogMessage)
	streamer := handler.NewStreamer(source, observer, storage, logger)
	go streamer.HandleFlows()
	defer streamer.Close()

	flowId, err := storage.AddFlow(&task.Flow{
		Filters:     &core.Parenthesis{},
		Include:     &backend.IncludeFields{Message: true},
		Destination: "localhost:0", // unavailable backend
	})
	require.NoError(t, err)

	closer := make(chan struct{})
	defer close(closer)
	go func() {
		message := getLogMessage(t)
		for {
			select {
			case source <- message:
			case <-closer:
				return
			}
		}
	}()

	time.Sleep(100 * time.Millisecond)
	err = storage.RemoveFlow(flowId)
	require.NoError(t, err)
}

func TestMessagesWithUnavailableBackend2(t *testing.T) {
	// ignore grpc goroutines
	defer goleak.VerifyNone(t,
		goleak.IgnoreTopFunction("google.golang.org/grpc.(*ccBalancerWrapper).watcher"),
		goleak.IgnoreTopFunction("google.golang.org/grpc.(*addrConn).resetTransport"))

	test.InitConfig(t)
	zkPath := test.ZkPrepare(t)
	logger := test.NewTestLogger()
	zkAddresses := viper.GetStringSlice("zk_addresses")

	observer := task.NewObserver(zkAddresses, zkPath, logger)
	defer observer.Close()

	storage, err := task.NewStorage(zkAddresses, zkPath, logger)
	require.NoError(t, err)
	defer storage.Close()

	source := make(chan *core.LogMessage)
	streamer := handler.NewStreamer(source, observer, storage, logger)
	go streamer.HandleFlows()
	defer streamer.Close()

	flowId, err := storage.AddFlow(&task.Flow{
		Filters:     &core.Parenthesis{},
		Include:     &backend.IncludeFields{Message: true},
		Destination: "localhost:0", // unavailable backend
	})
	require.NoError(t, err)
	_, err = storage.AddFlow(&task.Flow{
		Filters:     &core.Parenthesis{},
		Include:     &backend.IncludeFields{Message: true},
		Destination: "localhost:0", // unavailable backend
	})
	require.NoError(t, err)

	closer := make(chan struct{})
	defer close(closer)
	go func() {
		message := getLogMessage(t)
		for {
			select {
			case source <- message:
			case <-closer:
				return
			}
		}
	}()

	time.Sleep(100 * time.Millisecond)
	err = storage.RemoveFlow(flowId)
	require.NoError(t, err)
}
