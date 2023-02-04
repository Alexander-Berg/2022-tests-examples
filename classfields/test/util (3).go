package test

import (
	"database/sql"
	"fmt"
	"math/rand"
	"testing"
	"time"

	vlog "github.com/YandexClassifieds/go-common/log"
	vlogrus "github.com/YandexClassifieds/go-common/log/logrus"
	"github.com/YandexClassifieds/vtail/config"
	"github.com/YandexClassifieds/vtail/internal/logs"
	"github.com/go-zookeeper/zk"
	"github.com/sirupsen/logrus"
	"github.com/spf13/viper"
	"github.com/stretchr/testify/require"
)

func InitConfig(t *testing.T) {
	t.Helper()
	viper.AutomaticEnv()
	viper.AddConfigPath(".")
	viper.AddConfigPath("..")
	viper.AddConfigPath("../..")
	viper.AddConfigPath("../../..")
	viper.AddConfigPath("../../../..")
	viper.AddConfigPath("../../../../..")

	err := config.LoadConfig("test")
	if err != nil {
		t.Fatal(err)
	}
}

func NewTestLogger() vlog.Logger {
	log := logrus.New()
	log.Level = logrus.DebugLevel
	return vlogrus.Wrap(log.WithField(vlog.ContextF, "test"))
}

func ZkPrepare(t *testing.T) string {
	t.Helper()
	logger := NewTestLogger()
	zkLogger := &logs.ZkLogger{
		Logger: logs.WithContext(logger, "zk-prepare"),
	}
	conn, _, err := zk.Connect(viper.GetStringSlice("zk_addresses"), time.Second, zk.WithLogger(zkLogger))
	require.NoError(t, err)
	defer conn.Close()

	path := GenerateUniqueZkPath()
	zkCleanNode(t, conn, path)

	return path
}

func zkCleanNode(t *testing.T, conn *zk.Conn, path string) {
	childs, _, err := conn.Children(path)
	if err == zk.ErrNoNode {
		return
	}
	require.NoError(t, err)
	for _, child := range childs {
		var childPath string
		if path == "/" {
			childPath = path + child
		} else {
			childPath = path + "/" + child
		}
		if childPath == "/zookeeper" {
			// skip system node
			continue
		}

		zkCleanNode(t, conn, childPath)
	}
	err = conn.Delete(path, -1)
	require.NoError(t, err)
}

func GenerateUniqueZkPath() string {
	return fmt.Sprintf("%s/%d", viper.GetString("zk_path"), 1000000000+rand.Int31n(1147483647))
}

func InitDb(t *testing.T) *sql.DB {
	db, err := sql.Open("clickhouse", fmt.Sprintf("tcp://%s/?database=%s&debug=false", viper.GetStringSlice("CH_ADDRESSES")[0], viper.GetString("CH_DATABASE")))
	require.NoError(t, err)
	require.NoError(t, db.Ping(), "ping failed")
	_, err = db.Exec("TRUNCATE TABLE logs", "truncate table failed")
	require.NoError(t, err)
	return db
}
