package app

import (
	"fmt"
	"testing"
	"time"

	"github.com/YandexClassifieds/shiva/cmd/github-updater/app/storage"
	"github.com/YandexClassifieds/shiva/common"
	cSync "github.com/YandexClassifieds/shiva/pkg/consul/sync"
	"github.com/YandexClassifieds/shiva/pkg/include"
	"github.com/YandexClassifieds/shiva/pkg/kv_storage"
	"github.com/YandexClassifieds/shiva/pkg/manifest"
	"github.com/YandexClassifieds/shiva/pkg/manifest/parser"
	"github.com/YandexClassifieds/shiva/pkg/service_map"
	"github.com/YandexClassifieds/shiva/test"
	"github.com/YandexClassifieds/shiva/test/mock"
	"github.com/YandexClassifieds/shiva/test/mock/election"
	"github.com/YandexClassifieds/shiva/test/mock/service_change"
	"github.com/YandexClassifieds/shiva/test/test_db"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
)

const mapYaml = `
name: %s
description: Deployment system
is_external: false
owner: https://staff.yandex-team.ru/departments/yandex_personal_vertserv_infra
design_doc:
src:  https://github.com/YandexClassifieds/shiva
provides:
  - name: deploy
    protocol: grpc
    port: 80
    description: Основное апи для управления деплоем
    api_doc: https://wiki.yandex-team.ru/vertis-admin/shiva/
`

const mapMySQLYaml = `
name: shiva
description: Deployment system database
owner: https://staff.yandex-team.ru/departments/yandex_personal_vertserv_infra
type: mysql
provides:
  - name: shiva
    protocol: tcp
    port: 3306
    description: Основная база данных
    api_doc: https://wiki.yandex-team.ru/vertis-admin/shiva/
`
const deployYaml = `
name: %s
image: shiva-test

general:
  datacenters:
    sas:
      count: 4
    myt:
      count: 5
  resources:
    cpu: 100
    memory: 256
  env:
    - FAIL: false
`

const confYml = `
PARAM_1: param 1
PARAM_2: param 2
PARAM_3: param 3
`

func TestUpdater_refreshData(t *testing.T) {
	test.InitTestEnv()

	db := test_db.NewSeparatedDb(t)
	arcService := &mock.ArcFileService{}

	log := test.NewLogger(t)
	confService := include.NewService(db, log)
	mapService := service_map.NewService(db, log, service_change.NewNotificationMock())
	manifestService := manifest.NewService(db, log, parser.NewService(log, nil), confService)
	kvStore := kv_storage.NewKV(db, log)
	kvMock := mock.NewKVMock()
	require.NoError(t, kvStore.Save(revisionKey, "8"))
	syncS := cSync.NewService(log, kvMock, cSync.NewConf(t.Name()))

	fs := newFileSystem(t, confService, mapService, manifestService)
	prepareFileSystem(fs, arcService)

	u := NewUpdater(mapService, manifestService, confService, arcService, kvStore, time.Second, log, &election.Election{}, syncS)
	u.knownRevision = knownRevision
	require.NoError(t, u.fetchAndSave())
	require.Equal(t, uint64(10), u.knownRevision)
	val, err := kvStore.Get(revisionKey)
	require.NoError(t, err)
	require.Equal(t, "10", val)

	expected := map[string]uint64{
		"maps/shiva.yml":       10,
		"maps/mysql/shiva.yml": 9,
		"maps/updated.yml":     9,
	}
	assertCurrentItems(t, mapService, expected)

	expected = map[string]uint64{
		"deploy/shiva-test.yml":     10,
		"deploy/shiva-test-sox.yml": 9,
	}
	assertCurrentItems(t, manifestService, expected)

	expected = map[string]uint64{
		"conf/shiva/conf.yml":         9,
		"conf/shiva/conf1.yml":        8,
		"conf/shiva/conf-updated.yml": 10,
	}
	assertCurrentItems(t, confService, expected)

	assertMap(t, mapService, "shiva", "maps/shiva.yml")
	assertMap(t, mapService, "shiva", "maps/mysql/shiva.yml")
	assertMap(t, mapService, "updated-map", "maps/updated.yml")

	assertManifest(t, manifestService, "shiva-test")
	assertManifest(t, manifestService, "shiva-test-sox")

	assertConf(t, confService, "shiva/conf.yml")
	assertConf(t, confService, "shiva/conf1.yml")
	assertConf(t, confService, "shiva/conf-updated.yml")
}

func assertMap(t *testing.T, s *service_map.Service, name string, path string) {
	sm, _, err := s.GetByFullPath(path)
	require.NoError(t, err)
	assert.Equal(t, name, sm.Name)
	assert.Equal(t, path, sm.Path)
}

func assertManifest(t *testing.T, s *manifest.Service, name string) {
	m, err := s.GetByName(common.Test, name)
	require.NoError(t, err)
	assert.Equal(t, name, m.Name)
}

func assertConf(t *testing.T, s *include.Service, path string) {
	c, err := s.GetByPath(path)
	require.NoError(t, err)
	assert.Equal(t, 3, len(c.Value))
}

func assertCurrentItems(t *testing.T, store storage.ItemStore, expectedItems map[string]uint64) {
	items, err := store.GetCurrentItems()
	require.NoError(t, err)

	require.Equal(t, expectedItems, items)
}

//	maps
//		shiva.yml ------------------ new
//		old-map.yml ---------------- deleted
//      updated.yml ---------------- updated
//		mysql
//			shiva.yml -------------- new
//	deploy
//		shiva-test.yml ------------- new
//		shiva-test-sox.yml --------- updated
//      strange_file.php ----------- skip it
//	conf
//		shiva
//			conf.yml --------------- new
//			conf1.yml -------------- not changed
//   		conf-updated.yml ------- updated

func prepareFileSystem(fs *fileSystem, arcService *mock.ArcFileService) {
	fs.addFile("maps/shiva.yml", 10, New, fmt.Sprintf(mapYaml, "shiva"))
	fs.addFile("maps/old-map.yml", 8, Deleted, fmt.Sprintf(mapYaml, "old-map"))
	fs.addFile("maps/updated.yml", 9, Updated, fmt.Sprintf(mapYaml, "updated-map"))
	fs.addFile("maps/mysql/shiva.yml", 9, New, mapMySQLYaml)

	fs.addFile("deploy/shiva-test.yml", 10, New, fmt.Sprintf(deployYaml, "shiva-test"))
	fs.addFile("deploy/shiva-test-sox.yml", 9, Updated, fmt.Sprintf(deployYaml, "shiva-test-sox"))

	fs.addFile("deploy/strange_file.php", 9, New, "bad data")

	fs.addFile("conf/shiva/conf.yml", 9, New, confYml)
	fs.addFile("conf/shiva/conf1.yml", 8, NotChanged, confYml)
	fs.addFile("conf/shiva/conf-updated.yml", 10, Updated, confYml)

	fs.prepareMock(arcService)
	arcService.On("ActualRevision").Return(actualRevision, nil)
}
