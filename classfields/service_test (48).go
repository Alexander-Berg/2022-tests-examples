package commit

import (
	"testing"

	"github.com/YandexClassifieds/shiva/pkg/arc"
	"github.com/YandexClassifieds/shiva/pkg/arc/client"
	"github.com/YandexClassifieds/shiva/test"
	"github.com/stretchr/testify/require"
)

func TestService_GetCommitOid(t *testing.T) {
	test.InitTestEnv()
	log := test.NewLogger(t)
	commitCli := client.NewCommitClient(client.NewConf(), log)

	service := NewService(commitCli, arc.NewConf(), log)
	oid, err := service.GetCommitOid("users/robot-stark/classifieds/services/2498871/merge_pin")
	require.NoError(t, err)
	require.NotEmpty(t, oid)
}
