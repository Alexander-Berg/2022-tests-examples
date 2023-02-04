package session

import (
	"github.com/YandexClassifieds/goLB"
	"github.com/YandexClassifieds/goLB/internal/logger"
	"github.com/stretchr/testify/require"
	"testing"
)

func TestLBSession_DiscoverEndpoint(t *testing.T) {
	s := &LBSession{
		settings: goLB.Settings{
			Address: "sas.logbroker.yandex.net",
		},
		log: logger.New(),
	}
	err := s.discoverEndpoint()
	require.NoError(t, err)
}
