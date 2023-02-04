package handler_test

import (
	"github.com/YandexClassifieds/vtail/api/backend"
	"github.com/stretchr/testify/require"
	"testing"
)

type TestServer struct {
	t               *testing.T
	Out             chan *backend.PostLogsRequest
	ClientConnected chan struct{}
	backend.UnimplementedLogsWriterServer
}

func NewTestServer(t *testing.T) *TestServer {
	return &TestServer{
		t:               t,
		Out:             make(chan *backend.PostLogsRequest),
		ClientConnected: make(chan struct{}),
	}
}

func (t *TestServer) PostLogs(stream backend.LogsWriter_PostLogsServer) error {
	t.ClientConnected <- struct{}{}
	recv, err := stream.Recv()
	require.NoError(t.t, err)

	t.Out <- recv

	return err
}
