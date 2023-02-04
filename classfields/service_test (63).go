package mdb

import (
	"testing"

	"github.com/YandexClassifieds/shiva/common/config"
	"github.com/YandexClassifieds/shiva/pb/shiva/service_map"
	"github.com/YandexClassifieds/shiva/test"
	"github.com/stretchr/testify/require"
)

func TestGetClusterMySQL(t *testing.T) {
	test.InitTestEnv()

	log := test.NewLogger(t)
	mdbService := NewService(NewConf(), log)
	cluster, err := mdbService.GetCluster(service_map.ServiceType_mdb_mysql, config.Str("TEST_MDB_MYSQL_CLUSTER"))
	require.NoError(t, err)
	require.NotEmpty(t, cluster.Name)
	require.Equal(t, Prestable, cluster.Environment)
}

func TestGetClusterPostgreSQL(t *testing.T) {
	test.InitTestEnv()

	log := test.NewLogger(t)
	mdbService := NewService(NewConf(), log)
	cluster, err := mdbService.GetCluster(service_map.ServiceType_mdb_postgresql, config.Str("TEST_MDB_POSTGRESQL_CLUSTER"))
	require.NoError(t, err)
	require.NotEmpty(t, cluster.Name)
	require.Equal(t, Prestable, cluster.Environment)
}

func TestGetClusterKafka(t *testing.T) {
	test.InitTestEnv()

	log := test.NewLogger(t)
	mdbService := NewService(NewConf(), log)
	cluster, err := mdbService.GetCluster(service_map.ServiceType_kafka, config.Str("TEST_KAFKA_CLUSTER"))
	require.NoError(t, err)
	require.NotEmpty(t, cluster.Name)
	require.Equal(t, Prestable, cluster.Environment)
}
