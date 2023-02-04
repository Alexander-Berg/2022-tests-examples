package file

import (
	"testing"

	"github.com/YandexClassifieds/shiva/pkg/arc"
	"github.com/YandexClassifieds/shiva/pkg/arc/client"
	"github.com/YandexClassifieds/shiva/test"
	"github.com/stretchr/testify/require"
)

func TestListFilesWithSubFolders(t *testing.T) {
	test.InitTestEnv()

	service := prepareService(t)
	fileInfo, err := service.ListFiles(9202026, "conf/shiva")

	require.NoError(t, err)
	require.Len(t, fileInfo, 24)
}

func TestListFiles(t *testing.T) {
	test.InitTestEnv()

	service := prepareService(t)
	fileInfo, err := service.ListFiles(9202026, "conf/shiva/test-conf")
	require.NoError(t, err)
	require.Len(t, fileInfo, 3)

	expectedNames := []string{"common.yml", "prod.yml", "test.yml"}
	for i, f := range fileInfo {
		require.Equal(t, expectedNames[i], f.Name)
		require.Equal(t, uint64(9202026), f.RevisionLastChanged)
	}
}

func TestActualRevision(t *testing.T) {
	test.InitTestEnv()

	service := prepareService(t)
	revision, err := service.ActualRevision()
	require.NoError(t, err)

	require.True(t, revision > 0)
}

func TestReadFile(t *testing.T) {
	test.InitTestEnv()

	service := prepareService(t)
	data, err := service.ReadFile(9202026, "conf/shiva/test-conf/test.yml")
	require.NoError(t, err)

	require.NotEmpty(t, data)
}

func prepareService(t *testing.T) IService {
	conf := client.NewConf()
	log := test.NewLogger(t)
	cli := client.NewFileClient(conf, log)

	return NewService(cli, arc.NewConf(), log)
}
