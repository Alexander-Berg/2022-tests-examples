package system

import (
	"testing"

	dModel "github.com/YandexClassifieds/shiva/cmd/shiva/deployment/model"
	"github.com/YandexClassifieds/shiva/common"
	"github.com/YandexClassifieds/shiva/pkg/manifest/model"
	"github.com/YandexClassifieds/shiva/test"
	"github.com/YandexClassifieds/shiva/test/test_db"
	"github.com/stretchr/testify/require"
)

var (
	sName = "some-svc"
)

func TestGetByLayer(t *testing.T) {
	test.InitTestEnv()
	s := NewService(test_db.NewSeparatedDb(t), test.NewLogger(t))

	m := makeSimpleManifest(0)
	envs := s.GetByLayer(m, common.Test)

	require.Equal(t, getBaseEnvs(), envs)
}

func TestGetByLayerWithGeobase(t *testing.T) {
	test.InitTestEnv()
	s := NewService(test_db.NewSeparatedDb(t), test.NewLogger(t))

	m := makeSimpleManifest(4)
	envs := s.GetByLayer(m, common.Test)

	expectedEnvs := getBaseEnvs()
	expectedEnvs["_DEPLOY_GEOBASE_PATH"] = "/var/cache/geobase/geodata4.bin"
	expectedEnvs["_DEPLOY_GEOBASE_TZ_PATH"] = "/usr/share/geobase/zones_bin"

	require.Equal(t, expectedEnvs, envs)
}

func TestGetByDeployment(t *testing.T) {
	test.InitTestEnv()

	db := test_db.NewSeparatedDb(t)
	db.GormDb.DisableForeignKeyConstraintWhenMigrating = true
	s := NewService(db, test.NewLogger(t))

	d := &dModel.Deployment{
		Name:    sName,
		State:   dModel.Success,
		Layer:   common.Test,
		Version: "v10",
		Branch:  "some-br",
	}

	require.NoError(t, s.dStorage.Save(d))

	envs, err := s.GetByDeploymentId(makeSimpleManifest(0), d.ID)
	require.NoError(t, err)

	expectedEnvs := getBaseEnvs()

	expectedEnvs["_DEPLOY_APP_VERSION"] = "v10"
	expectedEnvs["_DEPLOY_BRANCH"] = "some-br"
	expectedEnvs["_DEPLOY_HPROF_DIRECTORY"] = "/alloc/logs/some-svc__v10__some-br__false"

	require.Equal(t, expectedEnvs, envs)
}

func makeSimpleManifest(geobaseVersion int) *model.Manifest {
	return &model.Manifest{
		Name:           sName,
		GeobaseVersion: geobaseVersion,
	}

}

func getBaseEnvs() map[string]string {
	env := make(map[string]string, 20)
	env["_DEPLOY_METRICS_PORT"] = "81"
	env["_DEPLOY_SERVICE_NAME"] = sName
	env["_DEPLOY_APP_VERSION"] = ""
	env["_DEPLOY_BRANCH"] = ""
	env["_DEPLOY_CANARY"] = "false"
	env["_DEPLOY_LAYER"] = "test"
	env["_DEPLOY_DC"] = ""
	env["_DEPLOY_HOSTNAME"] = ""
	env["_DEPLOY_ALLOC_ID"] = ""
	env["_DEPLOY_TRACING_ADDR"] = ""
	env["_DEPLOY_TRACING_COMPACT_PORT"] = "6831"
	env["_DEPLOY_TRACING_BINARY_PORT"] = "6832"
	env["_DEPLOY_TRACING_COMPACT_ENDPOINT"] = ""
	env["_DEPLOY_TRACING_BINARY_ENDPOINT"] = ""
	env["_DEPLOY_HPROF_DIRECTORY"] = ""
	env["_DEPLOY_ZOOKEEPER_CONN_STRING"] = "zk-01-vla.test.vertis.yandex.net:2181,zk-01-sas.test.vertis.yandex.net:2181,zk-01-man.test.vertis.yandex.net:2181"
	env["_DEPLOY_PROXY_URL"] = "proxy-ext.test.vertis.yandex.net:3128"

	return env
}
