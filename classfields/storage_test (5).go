package pipeline

import (
	"testing"

	"github.com/YandexClassifieds/cms/cmd/server/clusters"
	"github.com/YandexClassifieds/cms/cmd/server/hosts"
	pbAction "github.com/YandexClassifieds/cms/pb/cms/domains/actions/action"
	pbActionState "github.com/YandexClassifieds/cms/pb/cms/domains/actions/state"
	pbDC "github.com/YandexClassifieds/cms/pb/cms/domains/datacenters"
	pbHostTypes "github.com/YandexClassifieds/cms/pb/cms/domains/host_types"
	pbPipeline "github.com/YandexClassifieds/cms/pb/cms/domains/pipelines/pipeline"
	pbPipelineState "github.com/YandexClassifieds/cms/pb/cms/domains/pipelines/state"
	"github.com/YandexClassifieds/cms/test"
	"github.com/YandexClassifieds/go-common/log/logrus"
	"github.com/stretchr/testify/require"
	"gorm.io/gorm"
)

func TestStorage_Save(t *testing.T) {
	test.InitTestEnv()
	storage, hostID := prepare(t)

	p := &Pipeline{
		HostID:   hostID,
		Pipeline: pbPipeline.Pipeline_UNISPACE,
		State:    pbPipelineState.State_IN_PROGRESS,
	}
	require.NoError(t, storage.Save(p))

	var pipelines []*Pipeline
	require.NoError(t, storage.base.DB().Find(&pipelines).Error)
	require.Equal(t, 1, len(pipelines))
	require.Equal(t, hostID, pipelines[0].HostID)
	require.Equal(t, pbPipeline.Pipeline_UNISPACE, pipelines[0].Pipeline)
	require.Equal(t, pbPipelineState.State_IN_PROGRESS, pipelines[0].State)
}

func TestStorage_GetByHostID(t *testing.T) {
	test.InitTestEnv()
	storage, hostID := prepare(t)

	p := &Pipeline{
		HostID:   hostID,
		Pipeline: pbPipeline.Pipeline_UNISPACE,
		State:    pbPipelineState.State_IN_PROGRESS,
	}
	require.NoError(t, storage.Save(p))

	p, err := storage.GetByHostID(hostID)
	require.NoError(t, err)
	require.Equal(t, hostID, p.HostID)
	require.Equal(t, pbPipeline.Pipeline_UNISPACE, p.Pipeline)
	require.Equal(t, pbPipelineState.State_IN_PROGRESS, p.State)
}

func TestStorage_RemoveByHostID(t *testing.T) {
	test.InitTestEnv()
	storage, hostID := prepare(t)

	p := &Pipeline{
		HostID:   hostID,
		Pipeline: pbPipeline.Pipeline_UNISPACE,
		State:    pbPipelineState.State_IN_PROGRESS,
	}
	require.NoError(t, storage.Save(p))
	require.NoError(t, storage.Remove(p))

	_, err := storage.GetByHostID(hostID)
	require.Equal(t, gorm.ErrRecordNotFound, err)
}

func TestStorage_UpdateActionByHostID(t *testing.T) {
	test.InitTestEnv()
	storage, hostID := prepare(t)

	p := &Pipeline{
		HostID:   hostID,
		Pipeline: pbPipeline.Pipeline_UNISPACE,
		State:    pbPipelineState.State_IN_PROGRESS,
	}
	require.NoError(t, storage.Save(p))

	p.Action = pbAction.Action_DRAIN
	p.ActionState = pbActionState.State_SUCCESS
	require.NoError(t, storage.UpdateAction(p))

	p, err := storage.GetByHostID(hostID)
	require.NoError(t, err)
	require.Equal(t, hostID, p.HostID)
	require.Equal(t, pbPipeline.Pipeline_UNISPACE, p.Pipeline)
	require.Equal(t, pbPipelineState.State_IN_PROGRESS, p.State)
	require.Equal(t, pbAction.Action_DRAIN, p.Action)
	require.Equal(t, pbActionState.State_SUCCESS, p.ActionState)
}

func TestStorage_UpdateState(t *testing.T) {
	test.InitTestEnv()
	storage, hostID := prepare(t)

	p := &Pipeline{
		HostID:   hostID,
		Pipeline: pbPipeline.Pipeline_UNISPACE,
		State:    pbPipelineState.State_IN_PROGRESS,
	}
	require.NoError(t, storage.Save(p))

	p.State = pbPipelineState.State_SUCCESS
	require.NoError(t, storage.UpdateState(p))

	p, err := storage.GetByHostID(hostID)
	require.NoError(t, err)
	require.Equal(t, hostID, p.HostID)
	require.Equal(t, pbPipeline.Pipeline_UNISPACE, p.Pipeline)
	require.Equal(t, pbPipelineState.State_SUCCESS, p.State)
}

func prepare(t *testing.T) (*Storage, uint) {
	t.Helper()

	db := test.NewSeparatedGorm(t)
	log := logrus.New()

	clusters := clusters.NewStorage(db, log)
	require.NoError(t, clusters.Save(pbHostTypes.HostType_BAREMETAL, pbDC.DC_SAS, true, 1))
	clusterList, err := clusters.ListByHostType(pbHostTypes.HostType_BAREMETAL)
	require.NoError(t, err)

	hosts := hosts.NewStorage(db, log)
	require.NoError(t, hosts.Save("test", clusterList[0].ID))
	host, err := hosts.GetByName("test")
	require.NoError(t, err)

	return NewStorage(db, log), host.ID
}
