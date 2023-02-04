package clickHouse

import (
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
	"net/url"
	"testing"
)

func TestBuildDSN(t *testing.T) {
	dsn, err := url.Parse(BuildDSN(ConnectParams{
		Hosts:    []string{"somehost:123"},
		Database: "somedb",
		Username: "user",
		Password: "pass",
		Secure:   true,
	}))
	require.NoError(t, err)
	assert.Equal(t, "somehost:123", dsn.Host)
	assert.Equal(t, "somedb", dsn.Query().Get("database"))
	assert.Equal(t, "user", dsn.Query().Get("username"))
	assert.Equal(t, "pass", dsn.Query().Get("password"))
	assert.Equal(t, "true", dsn.Query().Get("secure"))
}
