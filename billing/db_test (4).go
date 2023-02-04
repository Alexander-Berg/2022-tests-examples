package db

import (
	"os"
	"testing"

	"github.com/stretchr/testify/require"
)

func TestParseHostsEmpty(t *testing.T) {
	hosts, err := parseHosts("")
	require.NoError(t, err)
	require.Nil(t, hosts)
}

func TestParseHostsFail(t *testing.T) {
	_, err := parseHosts(`{"xx": 23}`)
	require.Error(t, err)

	_, err = parseHosts(`["db-host=5588"]`)
	require.Error(t, err)
	require.Equal(t, ErrWrongHostFormat, err)
}

func TestParseHostsOne(t *testing.T) {
	hosts, err := parseHosts(`["db-host:5588"]`)
	require.NoError(t, err)
	require.Equal(t, 1, len(hosts))
	require.Equal(t, "db-host", hosts[0].Host)
	require.Equal(t, 5588, hosts[0].Port)
}

func TestParseHostsMany(t *testing.T) {
	hosts, err := parseHosts(`["db-host:5588", "one-more-host:9988"]`)
	require.NoError(t, err)
	require.Equal(t, 2, len(hosts))
	require.Equal(t, "db-host", hosts[0].Host)
	require.Equal(t, 5588, hosts[0].Port)
	require.Equal(t, "one-more-host", hosts[1].Host)
	require.Equal(t, 9988, hosts[1].Port)
}

// для покрытия
func TestNewStorage(t *testing.T) {
	require.NoError(t, os.Setenv("PAYOUT_DB_HOSTS", `["db-host:5588"]`))
	require.NoError(t, os.Setenv("PAYOUT_DB_PASSWORD", "xxx-yyy"))

	config := StorageConfig{}
	s := NewStorage(config)

	require.NotNil(t, s)
}
