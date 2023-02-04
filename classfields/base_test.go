package pipeline

import (
	"sync"
	"testing"
	"time"

	"github.com/YandexClassifieds/cms/cmd/server/hosts"
	"github.com/YandexClassifieds/cms/cmd/server/reparator/action"
	pbAction "github.com/YandexClassifieds/cms/pb/cms/domains/actions/action"
	pbActionState "github.com/YandexClassifieds/cms/pb/cms/domains/actions/state"
	pbPipeline "github.com/YandexClassifieds/cms/pb/cms/domains/pipelines/pipeline"
	pbPipelineState "github.com/YandexClassifieds/cms/pb/cms/domains/pipelines/state"
	"github.com/YandexClassifieds/cms/test"
	mAction "github.com/YandexClassifieds/cms/test/mocks/mockery/server/reparator/action"
	"github.com/stretchr/testify/mock"
	"github.com/stretchr/testify/require"
	"gorm.io/gorm"
)

func TestBasePipeline_Run(t *testing.T) {
	test.InitTestEnv()
	storage, hostID := prepare(t)

	p := &Pipeline{
		HostID:   hostID,
		Pipeline: pbPipeline.Pipeline_UNISPACE,
		State:    pbPipelineState.State_IN_PROGRESS,
	}
	require.NoError(t, storage.Save(p))

	p.Action = pbAction.Action_UNDRAIN
	p.ActionState = pbActionState.State_IN_PROGRESS
	require.NoError(t, storage.UpdateAction(p))

	a1 := makeActionMock(t, pbAction.Action_DRAIN)
	a2 := makeActionMock(t, pbAction.Action_UNDRAIN)

	base := basePipeline{
		pipelineType: pbPipeline.Pipeline_UNISPACE,
		storage:      storage,
		actions:      []action.IAction{a1, a2},
	}

	wg := &sync.WaitGroup{}
	wg.Add(1)

	go func() {
		require.NoError(t, base.Run(&hosts.Host{
			Model: gorm.Model{ID: hostID},
		}))
		wg.Done()
	}()

	require.Eventually(t, func() bool {
		p, err := storage.GetByHostID(hostID)
		require.NoError(t, err)

		if p.Action == pbAction.Action_DRAIN {
			t.Fatal("this action shouldn't be called")
		}

		return p.Action == pbAction.Action_UNDRAIN && p.ActionState == pbActionState.State_SUCCESS
	}, 5*time.Second, 100*time.Millisecond)

	a2.AssertCalled(t, "Do", mock.MatchedBy(func(host *hosts.Host) bool {
		return host.ID == hostID
	}))

	wg.Wait()
}

func makeActionMock(t *testing.T, Type pbAction.Action) *mAction.IAction {
	t.Helper()

	actionMock := &mAction.IAction{}
	actionMock.On("Type").Return(Type)
	actionMock.On("Do", mock.AnythingOfType("*hosts.Host")).Run(func(_ mock.Arguments) {
		time.Sleep(2 * time.Second)
	}).Return(nil)

	return actionMock
}
