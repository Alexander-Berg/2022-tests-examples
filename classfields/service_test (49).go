package diff

import (
	"testing"

	"github.com/YandexClassifieds/shiva/pkg/arc"
	"github.com/YandexClassifieds/shiva/pkg/arc/client"
	"github.com/YandexClassifieds/shiva/pkg/arc/file"
	"github.com/YandexClassifieds/shiva/test"
	"github.com/stretchr/testify/require"
)

func TestService_ChangeList(t *testing.T) {
	test.InitTestEnv()
	log := test.NewLogger(t)
	fileCli := client.NewFileClient(client.NewConf(), log)
	diffCli := client.NewDiffClient(client.NewConf(), log)

	fileService := file.NewService(fileCli, arc.NewConf(), log)
	service := NewService(diffCli, fileService, arc.NewConf(), log)
	files, err := service.ChangeList(2498871)
	require.NoError(t, err)
	require.Len(t, files, 2)
}
