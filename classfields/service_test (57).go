package include

import (
	"strings"
	"testing"

	"github.com/YandexClassifieds/shiva/common"
	"github.com/YandexClassifieds/shiva/pkg/include/domain"
	"github.com/YandexClassifieds/shiva/pkg/include/model/data"
	"github.com/YandexClassifieds/shiva/pkg/include/model/link"
	"github.com/YandexClassifieds/shiva/test"
	"github.com/YandexClassifieds/shiva/test/test_db"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
)

const (
	yml = `
PARAM_1: param1
PARAM_2: param2
`
	path = "test/test.yml"

	namePrefix = "my_service_"
)

func TestService_ReadAndSave(t *testing.T) {

	test.RunUp(t)
	s := NewService(test_db.NewDb(t), test.NewLogger(t))
	require.NoError(t, s.ReadAndSave([]byte(yml), 10, path))
	require.NoError(t, s.ReadAndSave([]byte(yml), 10, path))
	require.NoError(t, s.ReadAndSave([]byte(yml), 10, path))

	conf, err := s.GetByPath(path)
	require.NoError(t, err)
	assert.Equal(t, 2, len(conf.Value))
	assert.Equal(t, path, conf.Path)
}

func TestService_GetCurrentItems(t *testing.T) {
	test.RunUp(t)
	db := test_db.NewDb(t)

	s := NewService(db, test.NewLogger(t))
	require.NoError(t, s.dataSt.Save(&data.Data{Path: "test/test.yml"}))
	require.NoError(t, s.dataSt.Save(&data.Data{Path: "test/test2.yml"}))

	paths, err := s.GetCurrentItems()
	require.NoError(t, err)
	assert.Len(t, paths, 2)
	assert.Contains(t, paths, "conf/test/test.yml")
	assert.Contains(t, paths, "conf/test/test2.yml")
}

func TestService_DeleteItemsByPath(t *testing.T) {
	test.RunUp(t)
	db := test_db.NewDb(t)

	s := NewService(db, test.NewLogger(t))
	require.NoError(t, s.dataSt.Save(&data.Data{Path: "stuff/test.yml"}))
	require.NoError(t, s.dataSt.Save(&data.Data{Path: "stuff/test2.yml"}))

	err := s.DeleteItemByPath("conf/stuff/test.yml")
	require.NoError(t, err)

	cntTest := int64(-1)
	cntTest2 := int64(-1)
	db.GormDb.Model(data.Data{}).Where("path = 'stuff/test.yml'").Count(&cntTest)
	db.GormDb.Model(data.Data{}).Where("path = 'stuff/test2.yml'").Count(&cntTest2)
	assert.Equal(t, int64(0), cntTest)
	assert.Equal(t, int64(1), cntTest2)
}

func TestService_PreparePath(t *testing.T) {

	test.RunUp(t)
	cases := map[string]string{
		"/conf/conf.yml":      "conf.yml",
		"conf/conf.yml":       "conf.yml",
		"conf/conf/conf.yml":  "conf/conf.yml",
		"service/common.yml":  "service/common.yml",
		"/service/common.yml": "service/common.yml",
		"config.yml":          "config.yml",
	}
	for path, result := range cases {
		t.Run(path, func(t *testing.T) {
			assert.Equal(t, result, PreparePath(path))
		})
	}
}

func TestUpdate(t *testing.T) {

	type TestCase struct {
		name   string
		update []*domain.Include
		layer  common.Layer
	}

	test.RunUp(t)
	log := test.NewLogger(t)
	s := NewService(test_db.NewDb(t), log)

	cases := []TestCase{
		{
			name:   "update",
			update: emptyConfs("shiva/prod.yml", "shiva/common.yml"),
			layer:  common.Prod,
		},
		{
			name:   "delete_common_prod",
			update: emptyConfs("shiva/prod.yml"),
			layer:  common.Prod,
		},
		{
			name:   "delete_common_test",
			update: emptyConfs("shiva/test.yml"),
			layer:  common.Test,
		},
		{
			name:   "delete_prod",
			update: emptyConfs("shiva/common.yml"),
			layer:  common.Prod,
		},
		{
			name:   "delete_test",
			update: emptyConfs("shiva/common.yml"),
			layer:  common.Test,
		},
		{
			name:   "add_prod",
			update: emptyConfs("shiva/prod.yml", "shiva/prod2.yml", "shiva/common.yml"),
			layer:  common.Prod,
		},
		{
			name:   "add_test",
			update: emptyConfs("shiva/test.yml", "shiva/test2.yml", "shiva/common.yml"),
			layer:  common.Test,
		},
		{
			name:   "delete_and_add",
			update: emptyConfs("shiva/new-prod.yml", "shiva/common.yml"),
			layer:  common.Prod,
		},
	}

	for _, c := range cases {
		t.Run(c.name, func(t *testing.T) {
			name := namePrefix + c.name
			runUpLinks(t, s, name)
			require.NoError(t, s.UpdateLinks(name, c.layer, c.update))
			links, err := s.linkSt.GetByServiceAndLayer(name, c.layer)
			require.NoError(t, err)
			printLinks(t, c.update, links)
			assert.Equal(t, len(c.update), len(links))
			linksMap := map[string]*link.Link{}
			for _, l := range links {
				linksMap[l.Path] = l
			}
			for _, conf := range c.update {
				_, ok := linksMap[conf.Path]
				assert.True(t, ok)
			}
		})
	}
}

func TestUpdateByConfPrefix(t *testing.T) {

	test.RunUp(t)
	s := NewService(test_db.NewDb(t), test.NewLogger(t))
	require.NoError(t, s.UpdateLinks("test", common.Test, emptyConfs("conf/service/prod.yml")))
	require.NoError(t, s.UpdateLinks("test", common.Test, emptyConfs("/conf/service/common.yml")))
	links, err := s.linkSt.GetByServiceAndLayer("test", common.Test)
	require.NoError(t, err)
	for _, l := range links {
		assert.True(t, strings.Index(l.Path, "conf") != 0)
		assert.True(t, strings.Index(l.Path, "/conf") != 0)
	}
}

func printLinks(t *testing.T, update []*domain.Include, result []*link.Link) {

	log := test.NewLogger(t)
	var resultPath []string
	for _, conf := range update {
		resultPath = append(resultPath, conf.Path)
	}
	log.Infof("update: %v", resultPath)
	for _, r := range result {
		resultPath = append(resultPath, r.Path)
	}
	log.Infof("result: %v", resultPath)
}

func runUpLinks(t *testing.T, s *Service, name string) {
	makeLink(t, s, name, "shiva/prod.yml", common.Prod)
	makeLink(t, s, name, "shiva/common.yml", common.Prod)
	makeLink(t, s, name, "shiva/test.yml", common.Test)
	makeLink(t, s, name, "shiva/common.yml", common.Test)
}

func makeLink(t *testing.T, s *Service, name, path string, layer common.Layer) {

	require.NoError(t, s.linkSt.Save(&link.Link{
		ServiceName: name,
		Path:        PreparePath(path),
		Layer:       layer,
	}))
}

func emptyConf(path string) *domain.Include {

	return &domain.Include{
		Path:  path,
		Value: map[string]string{},
	}
}

func emptyConfs(paths ...string) []*domain.Include {

	var result []*domain.Include
	for _, path := range paths {
		result = append(result, emptyConf(path))
	}
	return result
}
