package consumer

import (
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
	"testing"
)

func TestReadSession_DiscoverCluster(t *testing.T) {
	s := newReadSession(Config{Address: "logbroker.yandex.net", Topic: "vertis/ci/golb"}, nil, nil)
	c, err := s.discoverCluster()
	require.NoError(t, err)
	assert.NotEqual(t, "", c)
	t.Logf("read cluster: %s", c)
}
