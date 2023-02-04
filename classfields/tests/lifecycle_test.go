package tests

import (
	"github.com/YandexClassifieds/goLB/consumer"
	"github.com/YandexClassifieds/goLB/internal/logger"
	"github.com/YandexClassifieds/goLB/internal/testauth"
	"github.com/YandexClassifieds/goLB/producer"
	"os"
	"testing"
	"time"

	_ "net/http/pprof"
)

var (
	clientId = "vertis/ci/ci"
	sourceId = "vertis/ci/ci"
	topic    = "vertis/ci/golb"
)

func TestWriteReadLifecycle(t *testing.T) {
	runWithSession(t, batchProducer(), simpleConsumer())
}

func simpleConsumer() consumer.Consumer {
	address := "sas.logbroker.yandex.net"
	if isCI() {
		address = "myt.logbroker.yandex.net"
	}

	cfg := consumer.Config{
		Address:              address,
		ClientId:             clientId,
		Topic:                topic,
		IdleTimeoutSec:       3,
		CommitIntervalMs:     0,
		MaxReadMessagesCount: 2000,
		MaxReadSize:          524288,
		MaxReadParts:         0,
		MaxTimeLagMs:         0,
		ReadTimestampMs:      0,
	}
	tp := &testauth.OAuthProvider{OAuthToken: os.Getenv("OAUTH_TOKEN")}
	return consumer.NewSimpleConsumer(cfg, tp, logger.New())
}

func batchProducer() producer.Producer {
	address := "sas.logbroker.yandex.net"
	cluster := "sas"
	if isCI() {
		address = "myt.logbroker.yandex.net"
		cluster = "myt"
	}

	cfg := &producer.Config{
		Address:             address,
		UseClusterDiscovery: true,
		PreferredCluster:    cluster,

		Topic:      topic,
		SourceId:   sourceId,
		BatchSize:  10240,
		BatchCount: 100,
		BatchTime:  200 * time.Millisecond,
	}
	auth := &testauth.OAuthProvider{OAuthToken: os.Getenv("OAUTH_TOKEN")}
	return producer.NewBatchProducer(cfg, auth, logger.New())
}

func isCI() bool {
	return os.Getenv("CI") == "true"
}
