package producer

import (
	"github.com/YandexClassifieds/goLB/internal/logger"
	"github.com/stretchr/testify/require"
	"testing"
)

func TestWriteSession_DiscoverCluster(t *testing.T) {
	cfg := &Config{
		Address:        "logbroker.yandex.net",
		Topic:          "/vertis-vertis-backend-log",
		SourceId:       "wtf",
		PartitionGroup: 42,
	}
	s := newWriteSession(cfg, nil, logger.New())
	_, err := s.discoverCluster()
	require.NoError(t, err)
}
