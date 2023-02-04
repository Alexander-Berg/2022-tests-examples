package test

import (
	"bytes"
	"encoding/json"
	"net"
	"os"
	"syscall"
	"testing"
	"time"

	"github.com/docker/docker/api/types/plugins/logdriver"
	"github.com/docker/docker/daemon/logger"
	"github.com/docker/go-plugins-helpers/sdk"
	"github.com/sirupsen/logrus"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"

	"github.com/YandexClassifieds/logs/cmd/docker-driver/docker"
	wmetrics "github.com/YandexClassifieds/logs/pkg/agent/writer/metrics"
)

// todo: refactor to PipelineSuite
func TestDockerInfo(t *testing.T) {
	log := logrus.WithField("dev", "true")
	h := sdk.NewHandler(`{"Implements": ["LoggingDriver"]}`)
	ms := newStorageRecorder()
	docker.Handlers(&h, docker.NewPlugin(ms, log))
	listen, err := net.Listen("tcp", "127.0.0.1:0")
	require.NoError(t, err)
	go func() {
		_ = h.Serve(listen)
	}()

	entry := logdriver.LogEntry{
		Source:   "",
		TimeNano: 0,
		Line:     []byte("my log line"),
		Partial:  false,
	}

	pipeFile := "/tmp/echo3.sock"
	_ = os.Remove(pipeFile)
	err = syscall.Mkfifo(pipeFile, 0666)
	require.NoError(t, err)
	f, err := os.OpenFile(pipeFile, os.O_RDWR|os.O_CREATE|os.O_APPEND, 0777)
	require.NoError(t, err)
	require.NotNil(t, f)
	encoder := logdriver.NewLogEntryEncoder(f)

	dockerReq := &docker.StartLoggingRequest{
		File: pipeFile,
		Info: logger.Info{
			Config: map[string]string{
			},
			ContainerID:         "",
			ContainerName:       "test-container",
			ContainerEntrypoint: "",
			ContainerArgs:       nil,
			ContainerImageID:    "",
			ContainerImageName:  "",
			ContainerCreated:    time.Time{},
			ContainerEnv: []string{
				"NOMAD_JOB_NAME=test-job-name",
				"NOMAD_ALLOC_ID=alloc-id",
				"NOMAD_META_APP_VERSION=0.1-dev",
			},
			ContainerLabels: nil,
			LogPath:         "",
			DaemonName:      "",
		},
	}
	marshal, err := json.Marshal(dockerReq)
	require.NoError(t, err)
	reader := bytes.NewReader(marshal)
	responseString, resp := DoHttpRequest(t, "GET", "http://"+listen.Addr().String()+"/LogDriver.StartLogging", reader)

	require.Equal(t, "{\"Err\":\"\"}\n", responseString)
	require.NotNil(t, resp)

	err = encoder.Encode(&entry)
	require.NoError(t, err)

	select {
	case answer := <-ms.answerC:
		require.Len(t, answer.Data, 1)
		assert.Equal(t, "alloc-id", answer.GetContext().GetAllocationId())
		assert.Equal(t, "test-job-name", answer.GetContext().GetService())
		assert.Equal(t, "0.1-dev", answer.GetContext().GetVersion())
		assert.Equal(t, "my log line", string(answer.Data[0].RawJson))
	case <-time.After(time.Second * 2):
		t.Errorf("didn't receive message")
	}

	err = listen.Close()
	require.NoError(t, err)
}

func TestBadPayload(t *testing.T) {
	log := logrus.WithField("dev", "true")
	h := sdk.NewHandler(`{"Implements": ["LoggingDriver"]}`)
	ms := new(storageRecorder)
	docker.Handlers(&h, docker.NewPlugin(ms, log))
	listen, err := net.Listen("tcp", "127.0.0.1:0")
	require.NoError(t, err)
	go func() {
		_ = h.Serve(listen)
	}()

	reader := bytes.NewReader([]byte("abcd"))
	responseString, resp := DoHttpRequest(t, "GET", "http://"+listen.Addr().String()+"/LogDriver.StartLogging", reader)

	require.Equal(t, "invalid character 'a' looking for beginning of value\n", responseString)
	require.NotNil(t, resp)

	err = listen.Close()
	require.NoError(t, err)
}

func init() {
	wmetrics.Init("test")
}
