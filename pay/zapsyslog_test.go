package zapsyslog

import (
	"bufio"
	"bytes"
	"encoding/json"
	"fmt"
	"io"
	"log"
	"log/syslog"
	"net"
	"os"
	"strings"
	"sync"
	"testing"
	"time"

	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
	"go.uber.org/zap"
	"go.uber.org/zap/zapcore"
)

// modified version of https://golang.org/src/log/syslog/syslog_test.go?m=text

func runStreamSyslog(l net.Listener, done chan<- string, wg *sync.WaitGroup) {
	for {
		var c net.Conn
		var err error
		if c, err = l.Accept(); err != nil {
			return
		}
		wg.Add(1)
		go func(c net.Conn) {
			defer wg.Done()
			err := c.SetReadDeadline(time.Now().Add(5 * time.Second))
			if err != nil {
				panic(err)
			}
			b := bufio.NewReader(c)
			for {
				s, err := b.ReadString('\n')
				if err != nil {
					break
				}
				done <- s
			}
			_ = c.Close()
		}(c)
	}
}

func startServer(done chan<- string) (addr string, sock io.Closer, wg *sync.WaitGroup) {
	const (
		la = "127.0.0.1:0"
		n  = "tcp"
	)

	wg = new(sync.WaitGroup)

	l, err := net.Listen(n, la)
	if err != nil {
		log.Fatalf("startServer failed: %v", err)
	}

	addr = l.Addr().String()
	sock = l
	wg.Add(1)
	go func() {
		defer wg.Done()
		runStreamSyslog(l, done, wg)
	}()

	return
}

var severityToLevel = map[zapcore.Level]syslog.Priority{
	zapcore.DebugLevel:  syslog.LOG_DEBUG,
	zapcore.InfoLevel:   syslog.LOG_INFO,
	zapcore.WarnLevel:   syslog.LOG_WARNING,
	zapcore.ErrorLevel:  syslog.LOG_ERR,
	zapcore.DPanicLevel: syslog.LOG_CRIT,
	zapcore.PanicLevel:  syslog.LOG_CRIT,
	zapcore.FatalLevel:  syslog.LOG_CRIT,
}

func check(t *testing.T, expectedFacility syslog.Priority, expectedMsg, gotLogLine string) {
	tmpl := "<%d>%s %s duckgo-test[%d]: %s\n"
	hostname, err := os.Hostname()
	require.NoError(t, err)

	var (
		parsedPriority                syslog.Priority
		parsedHostname, timestamp, js string
		pid                           int
	)

	n, err := fmt.Sscanf(gotLogLine, tmpl, &parsedPriority, &timestamp, &parsedHostname, &pid, &js)
	require.Truef(t, err == nil && n == 5, "Got %q, does not match template %q", gotLogLine, tmpl)
	require.Equal(t, hostname, parsedHostname)

	var logLine struct {
		Level string `json:"level"`
		Msg   string `json:"msg"`
	}
	err = json.Unmarshal([]byte(js), &logLine)
	require.NoError(t, err)
	assert.Equal(t, expectedMsg, logLine.Msg)

	var parsedLevel zapcore.Level
	err = parsedLevel.UnmarshalText([]byte(logLine.Level))
	require.NoError(t, err)

	expectedSeverity, ok := severityToLevel[parsedLevel]
	require.True(t, ok)

	expectedPriority := expectedSeverity | expectedFacility
	require.Equal(t, expectedPriority, parsedPriority)
}

func newTestLogger(writer *syslog.Writer) *zap.Logger {
	encoder := zapcore.NewJSONEncoder(zap.NewProductionEncoderConfig())
	core := New(zapcore.DebugLevel, encoder, writer)
	return zap.New(core)
}

func TestSyslog(t *testing.T) {
	writer, err := syslog.New(syslog.LOG_ERR|syslog.LOG_LOCAL0, "duckgo-test")
	if err != nil {
		t.Skip("skip test: unix syslog is not available")
	}
	require.NoError(t, err)
	defer func() { _ = writer.Close() }()

	logger := newTestLogger(writer)
	logger.Error("message with error log level", zap.String("a", "b"))
	err = logger.Sync()
	require.NoError(t, err)
}

func TestLogLevels(t *testing.T) {
	done := make(chan string)
	addr, sock, srvWG := startServer(done)
	defer srvWG.Wait()
	defer func() { _ = sock.Close() }()

	facility := syslog.LOG_LOCAL0
	writer, err := syslog.Dial("tcp", addr, facility, "duckgo-test")
	require.NoError(t, err)
	defer func() { _ = writer.Close() }()

	logger := newTestLogger(writer)

	type LogFunc func(msg string, fields ...zapcore.Field)
	for _, logFunc := range []LogFunc{
		logger.Debug,
		logger.Info,
		logger.Warn,
		logger.Error,
	} {
		t.Run("", func(t *testing.T) {
			logFunc("test")
			check(t, facility, "test", <-done)
		})
	}
}

func TestWriterError(t *testing.T) {
	done := make(chan string)
	addr, sock, srvWG := startServer(done)

	writer, err := syslog.Dial("tcp", addr, syslog.LOG_LOCAL0, "duckgo-test")
	require.NoError(t, err)

	_ = writer.Close()
	_ = sock.Close()
	srvWG.Wait()

	encoder := zapcore.NewJSONEncoder(zap.NewProductionEncoderConfig())
	core := New(zapcore.DebugLevel, encoder, writer)
	errOutput := bytes.NewBuffer(nil)
	logger := zap.New(core, zap.ErrorOutput(zapcore.AddSync(errOutput)))
	logger.Info("test")

	assert.NotEmpty(t, errOutput)
}

func TestWithField(t *testing.T) {
	done := make(chan string)
	addr, sock, srvWG := startServer(done)
	defer srvWG.Wait()
	defer func() { _ = sock.Close() }()

	facility := syslog.LOG_LOCAL0
	writer, err := syslog.Dial("tcp", addr, facility, "duckgo-test")
	require.NoError(t, err)
	defer func() { _ = writer.Close() }()

	encoder := zapcore.NewJSONEncoder(zap.NewProductionEncoderConfig())
	core := New(zapcore.DebugLevel, encoder, writer)
	logger := zap.New(core)
	logger = logger.With(zap.String("k", "some_secret"))
	logger.Info("test")
	gotMsg := <-done
	require.True(t, strings.Contains(gotMsg, "some_secret"), gotMsg)
}
