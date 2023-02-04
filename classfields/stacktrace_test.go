package logs

import (
	"fmt"
	"github.com/getsentry/sentry-go"
	"github.com/stretchr/testify/require"
	"io/ioutil"
	"testing"
	"time"
)

type testTransport struct {
	t        *testing.T
	funcName string
}

func (tran *testTransport) Configure(options sentry.ClientOptions) {}

func (tran *testTransport) SendEvent(event *sentry.Event) {
	require.Len(tran.t, event.Exception, 1)
	require.Len(tran.t, event.Exception[0].Stacktrace.Frames, 1)
	frame := event.Exception[0].Stacktrace.Frames[0]
	require.Contains(tran.t, frame.AbsPath, "stacktrace_test.go")
	require.Equal(tran.t, "github.com/YandexClassifieds/vtail/internal/logs", frame.Module)
	require.Equal(tran.t, tran.funcName, frame.Function)
}

func (tran *testTransport) Flush(_ time.Duration) bool {
	return true
}

func TestStackTrace(t *testing.T) {
	transport := &testTransport{t: t, funcName: "TestStackTrace"}
	logger := NewExtended(ioutil.Discard, "", "debug", "", "", "", transport)
	logger.WithError(fmt.Errorf("abc")).Error("def")
}

func TestErrorStackTrace(t *testing.T) {
	transport := &testTransport{t: t, funcName: "TestErrorStackTrace"}
	logger := NewExtended(ioutil.Discard, "", "debug", "", "", "", transport)
	logger.Error("def")
}

func TestErrorfStackTrace(t *testing.T) {
	transport := &testTransport{t: t, funcName: "TestErrorfStackTrace"}
	logger := NewExtended(ioutil.Discard, "", "debug", "", "", "", transport)
	logger.Errorf("def")
}
