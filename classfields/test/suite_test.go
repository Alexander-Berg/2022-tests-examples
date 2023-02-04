package test

import (
	"bytes"
	"context"
	"encoding/json"
	"github.com/YandexClassifieds/logs/api/collector"
	"google.golang.org/grpc"
	"net"
	"os"
	"strings"
	"syscall"
	"testing"
	"time"

	"github.com/docker/docker/api/types/plugins/logdriver"
	"github.com/docker/docker/daemon/logger"
	"github.com/docker/go-plugins-helpers/sdk"
	"github.com/sirupsen/logrus"
	"github.com/stretchr/testify/suite"

	"github.com/YandexClassifieds/logs/cmd/docker-driver/config"
	"github.com/YandexClassifieds/logs/cmd/docker-driver/docker"
)

func TestPipelineSuite(t *testing.T) {
	suite.Run(t, new(PipelineSuite))
}

type PipelineSuite struct {
	suite.Suite
	lis     net.Listener
	encoder logdriver.LogEntryEncoder
	rec     *storageRecorder
}

func (s *PipelineSuite) SetupTest() {
	var err error
	config.MessagesPerWindow = 100
	config.BytesPerWindow = 40 * 1024

	log := logrus.WithField("dev", "true")
	h := sdk.NewHandler(`{"Implements": ["LoggingDriver"]}`)
	s.rec = newStorageRecorder()
	docker.Handlers(&h, docker.NewPlugin(s.rec, log))
	s.lis, err = net.Listen("tcp", "127.0.0.1:0")
	s.NoError(err)
	go func() {
		_ = h.Serve(s.lis)
	}()

	pipeFile := "/tmp/echo3.sock"
	_ = os.Remove(pipeFile)
	err = syscall.Mkfifo(pipeFile, 0666)
	s.NoError(err)
	f, err := os.OpenFile(pipeFile, os.O_RDWR|os.O_CREATE|os.O_APPEND, 0777)
	s.NoError(err)
	s.NotNil(f)
	s.encoder = logdriver.NewLogEntryEncoder(f)

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
	s.NoError(err)
	reader := bytes.NewReader(marshal)
	responseString, resp := DoHttpRequest(s.T(), "GET", "http://"+s.lis.Addr().String()+"/LogDriver.StartLogging", reader)

	s.Equal("{\"Err\":\"\"}\n", responseString)
	s.NotNil(resp)
}

func (s *PipelineSuite) TearDownTest() {
	err := s.lis.Close()
	s.NoError(err)
}

func (s *PipelineSuite) TestLongLine() {
	entries := []logdriver.LogEntry{
		{
			Line:    []byte(strings.Repeat("a", config.MaxMessageSize)),
			Partial: true,
		},
		{
			Line:    []byte(strings.Repeat("a", 16*1024)),
			Partial: true,
		},
		{
			Line:    []byte("a"), // this entry should be skipped
			Partial: true,
		},
		{
			Line:    []byte(strings.Repeat("a", 16*1024)),
			Partial: true,
		},
		{
			Line:    []byte(strings.Repeat("a", 1024)),
			Partial: true,
		},
	}

	for _, entry := range entries {
		err := s.encoder.Encode(&entry)
		s.NoError(err)
	}

	select {
	case answer := <-s.rec.answerC:
		if !s.Len(answer.Data, 1) {
			return
		}
		s.Equal(strings.Repeat("a", 17*1024), string(answer.Data[0].RawJson))
		s.Equal("test-job-name", answer.GetContext().GetService())
		s.Equal("0.1-dev", answer.GetContext().GetVersion())
	case <-time.After(time.Second * 2):
		s.FailNow("didn't receive message")
	}

	entries = []logdriver.LogEntry{
		{
			Line:    []byte(strings.Repeat("b", 16*1024)),
			Partial: true,
		},
		{
			Line:    []byte(strings.Repeat("b", 1024)),
			Partial: true,
		},
	}

	for _, entry := range entries {
		err := s.encoder.Encode(&entry)
		s.NoError(err)
	}

	select {
	case answer := <-s.rec.answerC:
		if !s.Len(answer.Data, 1) {
			return
		}
		s.Equal(strings.Repeat("b", 17*1024), string(answer.Data[0].RawJson))
		s.Equal("test-job-name", answer.GetContext().GetService())
		s.Equal("0.1-dev", answer.GetContext().GetVersion())
	case <-time.After(time.Second * 2):
		s.FailNow("didn't receive message")
	}
}

type storageRecorder struct {
	answerC chan *collector.BatchRequest
}

func newStorageRecorder() *storageRecorder {
	return &storageRecorder{
		answerC: make(chan *collector.BatchRequest),
	}
}

func (m *storageRecorder) BatchWrite(ctx context.Context, in *collector.BatchRequest, opts ...grpc.CallOption) (*collector.BatchResponse, error) {
	m.answerC <- in
	return &collector.BatchResponse{}, nil
}
