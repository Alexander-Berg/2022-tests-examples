package serviceDiscovery

import (
	"github.com/YandexClassifieds/go-common/log/logrus"
	"github.com/YandexClassifieds/h2p/test"
	"github.com/spf13/viper"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
	"net"
	"testing"
)

func TestServiceDiscovery_GetMDBInfo(t *testing.T) {
	tests := map[string]struct {
		Host         string
		ExpectedInfo *MDBInfo
	}{
		"mysql-ro": {
			Host: "database.mdb-ro-cluster.query.consul",
			ExpectedInfo: &MDBInfo{
				Cluster: "cluster",
				Db:      "database",
				Mode:    "ro",
			},
		},
		"mysql-rw": {
			Host: "database.mdb-rw-cluster.query.consul",
			ExpectedInfo: &MDBInfo{
				Cluster: "cluster",
				Db:      "database",
				Mode:    "rw",
			},
		},
		"postgresql-ro": {
			Host: "database.pg-ro-cluster.query.consul",
			ExpectedInfo: &MDBInfo{
				Cluster: "cluster",
				Db:      "database",
				Mode:    "ro",
			},
		},
		"postgresql-rw": {
			Host: "database.pg-rw-cluster.query.consul",
			ExpectedInfo: &MDBInfo{
				Cluster: "cluster",
				Db:      "database",
				Mode:    "rw",
			},
		},
	}

	for name, tc := range tests {
		t.Run(name, func(t *testing.T) {
			sd := ServiceDiscovery{}
			info := sd.GetMDBInfo(tc.Host)

			assert.Equal(t, tc.ExpectedInfo, info)
		})
	}
}

func TestServiceDiscovery_consulResolvers(t *testing.T) {
	test.InitConfig(t)

	sd := New(viper.GetString("consul_addr"), nil, logrus.New("info"))
	resolvers := sd.consulResolvers()

	require.Equal(t, 1, len(resolvers))
	dcResolvers, ok := resolvers["dc1"]
	require.True(t, ok)
	require.Equal(t, 3, len(dcResolvers))

	for _, resolver := range dcResolvers {
		_, port, err := net.SplitHostPort(resolver)
		require.NoError(t, err)
		require.Equal(t, "8600", port)
	}
}
