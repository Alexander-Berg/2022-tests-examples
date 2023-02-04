package manifest

import (
	"testing"

	"github.com/YandexClassifieds/shiva/common"
	"github.com/YandexClassifieds/shiva/pkg/include/model/link"
	"github.com/YandexClassifieds/shiva/test"
	"github.com/YandexClassifieds/shiva/test/test_db"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
)

func TestService_ReadAndSave(t *testing.T) {

	test.InitTestEnv()
	db := test_db.NewSeparatedDb(t)

	service := newService(t, db)

	require.NoError(t, service.includeS.ReadAndSave([]byte(commonYml), 10, "service/common.yml"))
	require.NoError(t, service.ReadAndSave([]byte(manifest), 10, ""))

	prodM, err := service.GetByName(common.Prod, name)
	require.NoError(t, err)
	assert.Equal(t, name, prodM.Name)

	testM, err := service.GetByName(common.Test, name)
	require.NoError(t, err)
	assert.Equal(t, name, testM.Name)
	assert.Equal(t, 2, testM.DC["sas"])
	assert.Equal(t, 3, testM.DC["yd_vla"])

	usage, err := service.includeS.FindUsage("service/common.yml")
	require.NoError(t, err)
	assert.Len(t, usage, 2)
}

func TestService_ReadAndSave_WithConf(t *testing.T) {
	test.InitTestEnv()
	db := test_db.NewSeparatedDb(t)

	s := newService(t, db)

	require.NoError(t, s.includeS.ReadAndSave([]byte(testYml), 10, "shiva/test.yml"))
	require.NoError(t, s.includeS.ReadAndSave([]byte(prodYml), 10, "shiva/prod.yml"))
	require.NoError(t, s.includeS.ReadAndSave([]byte(commonYml), 10, "shiva/common.yml"))
	require.NoError(t, s.ReadAndSave([]byte(manifestWithConf), 10, ""))

	m, err := s.GetByName(common.Prod, "yandex_vertis_example_service")
	require.NoError(t, err)
	assert.Len(t, m.Config.Files, 2)

	conf, err := s.includeS.GetByPath("shiva/prod.yml")
	require.NoError(t, err)
	assert.Equal(t, "prod param", conf.Value["PROD_PARAM"])

	conf, err = s.includeS.GetByPath("shiva/common.yml")
	require.NoError(t, err)
	assert.Equal(t, "common param", conf.Value["COMMON_PARAM"])
}

func TestService_GetCurrentItems(t *testing.T) {
	test.InitTestEnv()
	db := test_db.NewSeparatedDb(t)

	s := newService(t, db)

	require.NoError(t, s.storage.Save(&Data{Name: "test", Path: "deploy/test.yml"}))
	require.NoError(t, s.storage.Save(&Data{Name: "test2", Path: "deploy/test2.yml"}))

	paths, err := s.GetCurrentItems()
	require.NoError(t, err)
	assert.Len(t, paths, 2)
	assert.Contains(t, paths, "deploy/test.yml")
	assert.Contains(t, paths, "deploy/test2.yml")
}

func TestService_DeleteItemsByPath(t *testing.T) {
	test.InitTestEnv()
	db := test_db.NewSeparatedDb(t)

	s := newService(t, db)

	require.NoError(t, s.storage.Save(&Data{Name: "test", Path: "deploy/test.yml"}))
	require.NoError(t, s.storage.Save(&Data{Name: "test2", Path: "deploy/test2.yml"}))
	require.NoError(t, db.GormDb.Save(&link.Link{Layer: common.Test, ServiceName: "test", Path: "tst"}).Error)
	require.NoError(t, db.GormDb.Save(&link.Link{Layer: common.Prod, ServiceName: "test", Path: "tst"}).Error)
	require.NoError(t, db.GormDb.Save(&link.Link{Layer: common.Prod, ServiceName: "test2", Path: "tst2"}).Error)

	err := s.DeleteItemByPath("deploy/test.yml")
	require.NoError(t, err)

	cntTest := int64(-1)
	cntLinkTest := int64(-1)
	cntTest2 := int64(-1)
	cntLinkTest2 := int64(-1)
	db.GormDb.Model(Data{}).Where("path = 'deploy/test.yml'").Count(&cntTest)
	db.GormDb.Model(Data{}).Where("path = 'deploy/test2.yml'").Count(&cntTest2)
	db.GormDb.Model(link.Link{}).Where("service_name = 'test'").Count(&cntLinkTest)
	db.GormDb.Model(link.Link{}).Where("service_name = 'test2'").Count(&cntLinkTest2)
	assert.Equal(t, int64(0), cntTest)
	assert.Equal(t, int64(0), cntLinkTest, "links should be deleted")
	assert.Equal(t, int64(1), cntTest2)
	assert.Equal(t, int64(1), cntLinkTest2)
}

func TestResolveIncludes(t *testing.T) {

	test.InitTestEnv()
	db := test_db.NewSeparatedDb(t)

	service := newService(t, db)

	require.NoError(t, service.includeS.ReadAndSave([]byte(testYml), 10, "shiva/test.yml"))
	require.NoError(t, service.includeS.ReadAndSave([]byte(prodYml), 10, "shiva/prod.yml"))
	require.NoError(t, service.includeS.ReadAndSave([]byte(commonYml), 10, "shiva/common.yml"))
	require.NoError(t, service.ReadAndSave([]byte(manifestWithConf), 10, "yandex_vertis_example_service.yml"))

	m, err := service.GetByName(common.Prod, "yandex_vertis_example_service")
	require.NoError(t, err)

	envs, err := m.Config.GetEnvs()
	require.NoError(t, err)

	assert.Len(t, envs, 4)
	assert.Equal(t, "${sec-42:ver-42123:kkk}", envs["SECRET_VER"])
	assert.Equal(t, "${port:shiva-tg:admin}", envs["TEMPLATE"])
	assert.Equal(t, "common param", envs["COMMON_PARAM"])
	assert.Equal(t, "prod param", envs["PROD_PARAM"])

	m, err = service.GetByName(common.Test, "yandex_vertis_example_service")
	require.NoError(t, err)

	envs, err = m.Config.GetEnvs()
	require.NoError(t, err)

	assert.Len(t, envs, 2)
	assert.Equal(t, "common param", envs["COMMON_PARAM"])
	assert.Equal(t, "test param", envs["TEST_PARAM"])
}

const (
	name     = "new_service"
	manifest = `
name: new_service
general:
  datacenters:
    sas:
      count: 2
    yd_vla:
      count: 3
test:
  conf:
    - service/common.yml
prod:
  conf:
    - service/common.yml
`
	manifestWithConf = `
name: yandex_vertis_example_service
general:
  datacenters:
    sas:
      count: 2
test:
  conf:
    - shiva/common.yml
    - shiva/test.yml

prod:
  conf:
    - shiva/prod.yml
    - shiva/common.yml
`

	commonYml = `
COMMON_PARAM: common param
`
	testYml = `
TEST_PARAM: test param
`
	prodYml = `
PROD_PARAM: prod param
SECRET_VER: "${sec-42:ver-42123:kkk}"
TEMPLATE: "${port:shiva-tg:admin}"
`
)
