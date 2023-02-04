package source

import (
	"bytes"
	"fmt"
	"strings"
	"testing"
	"time"

	vlogrus "github.com/YandexClassifieds/go-common/log/logrus"
	"github.com/YandexClassifieds/goLB"
	"github.com/YandexClassifieds/vtail/cmd/streamer/source/auth"
	"github.com/sirupsen/logrus"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
)

func TestGrpcError(t *testing.T) {
	out := &bytes.Buffer{}
	logger := logrus.New()
	logger.Out = out
	logger.Level = logrus.DebugLevel
	l := vlogrus.Wrap(logger.WithField("tst", 1))

	reader := NewLbReader("127.0.0.1", auth.OAuthProvider{OauthToken: "-"}, l, "-", false)
	reader.GetLogs()

	success := assert.Eventually(t, func() bool {
		return strings.Contains(out.String(), "discovery failed:") &&
			strings.Contains(out.String(), `rpc error: code = Unavailable`) &&
			strings.Contains(out.String(), "cause=unavailable")
	}, 5*time.Second, time.Second/10, "can't find log entries in output")
	if !success {
		fmt.Printf("out buf: %s", out.String())
	}
}

func TestCommonError(t *testing.T) {
	out := &bytes.Buffer{}
	logger := logrus.New()
	logger.Out = out
	logger.Level = logrus.DebugLevel
	l := vlogrus.Wrap(logger.WithField("tst", 1))

	LogLbError(l, fmt.Errorf("unknown error"))
	require.Contains(t, out.String(), `level=error msg="unknown error"`)
}

func TestRestartRequestError(t *testing.T) {
	out := &bytes.Buffer{}
	logger := logrus.New()
	logger.Out = out
	logger.Level = logrus.DebugLevel
	l := vlogrus.Wrap(logger.WithField("tst", 1))

	LogLbError(l, &goLB.LogBrokerError{
		Code:        "INITIALIZING",
		Description: "read-session 9853: datacenter classifier initialized, restart session please",
	})
	require.Contains(t, out.String(), `level=warning msg="LB error: code = INITIALIZING; description = datacenter classifier initialized, restart session please" cause=restart_request`)
}

func TestLogLbError_Unknown(t *testing.T) {
	out := &bytes.Buffer{}
	logger := logrus.New()
	logger.Out = out
	logger.Level = logrus.DebugLevel
	l := vlogrus.Wrap(logger.WithField("tst", 1))

	LogLbError(l, &goLB.LogBrokerError{
		Code:        "UNKNOWN_TOPIC",
		Description: "main directory describe error, Status# StatusNotAvailable, reason: Schemeshard not available, Marker# PQ1",
	})
	require.Contains(t, out.String(), `level=warning msg="LB error: code = UNKNOWN_TOPIC`)
}
