package api

import (
	"testing"

	subscribe2 "github.com/YandexClassifieds/shiva/cmd/telegram/subscribe"
	"github.com/YandexClassifieds/shiva/pb/shiva/api/deploy2"
	"github.com/YandexClassifieds/shiva/pb/shiva/service_map"
	"github.com/YandexClassifieds/shiva/pb/shiva/types/batch"
	"github.com/YandexClassifieds/shiva/pb/shiva/types/deployment"
	dType "github.com/YandexClassifieds/shiva/pb/shiva/types/dtype"
	"github.com/YandexClassifieds/shiva/pb/shiva/types/info"
	"github.com/YandexClassifieds/shiva/pb/shiva/types/layer"
	statePb "github.com/YandexClassifieds/shiva/pb/shiva/types/state"
	"github.com/YandexClassifieds/shiva/test"
	"github.com/YandexClassifieds/shiva/test/mock"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
)

func TestBatchRun(t *testing.T) {
	test.InitTestEnv()
	s, m, res, chatID := newService(t, layer.Layer_TEST, subscribe2.All, true)
	inputMsg := mock.NewInputMessageMock("/run -l test -v 0.0.1 my_service", tgLogin, chatID)
	// additional test: skip process messages
	res <- makeBatchState(statePb.DeploymentState_PREPARE, dType.DeploymentType_RUN, batch.State_Unknown, "")
	res <- makeBatchState(statePb.DeploymentState_IN_PROGRESS, dType.DeploymentType_RUN, batch.State_Unknown, "")
	res <- makeBatchState(statePb.DeploymentState_SUCCESS, dType.DeploymentType_RUN, batch.State_Active, "")
	close(res)
	m.IN <- inputMsg
	actual := m.Read(t, 1)
	msg := actual[0]
	assert.Equal(t, s.msgS.BacktickHack(`Command: Run
Service: ¬my_batch_service¬
Layer: Test
Version: ¬0.1.0¬
Initiator: [danevge](https://staff.yandex-team.ru/danevge)
Time: ¬2020-06-15T01:15¬
State: ¬success¬

Batch state: ¬Active¬
Periodic: ¬1 * * * *¬
Next run: ¬2020-06-15T01:15¬
`), msg.Message)

	// assert tg
	assertInit(t, inputMsg, actual)
	assertNew(t, actual[0])

	ds, err := s.deploymentKV.All()
	require.NoError(t, err)
	assert.Equal(t, 1, len(ds))

	assert.Len(t, actual[0].Buttons, 1)
	assert.Equal(t, "promote", actual[0].Buttons[0].Message)
	assert.Equal(t, "/promote -id 1", actual[0].Buttons[0].Callback)

}

func TestBatchRunBranch(t *testing.T) {
	test.InitTestEnv()
	s, m, res, chatID := newService(t, layer.Layer_TEST, subscribe2.All, true)
	inputMsg := mock.NewInputMessageMock("/run -l test -v 0.0.1 -b br1 my_service", tgLogin, chatID)
	// additional test: skip process messages
	res <- makeBatchState(statePb.DeploymentState_PREPARE, dType.DeploymentType_RUN, batch.State_Unknown, "br1")
	res <- makeBatchState(statePb.DeploymentState_IN_PROGRESS, dType.DeploymentType_RUN, batch.State_Unknown, "br1")
	res <- makeBatchState(statePb.DeploymentState_SUCCESS, dType.DeploymentType_RUN, batch.State_Active, "br1")
	close(res)
	m.IN <- inputMsg
	actual := m.Read(t, 1)
	msg := actual[0]
	assert.Equal(t, s.msgS.BacktickHack(`Command: Run
Service: ¬my_batch_service¬
Layer: Test
Version: ¬0.1.0¬
Branch: ¬br1¬
Initiator: [danevge](https://staff.yandex-team.ru/danevge)
Time: ¬2020-06-15T01:15¬
State: ¬success¬

Batch state: ¬Active¬
Periodic: ¬1 * * * *¬
Next run: ¬2020-06-15T01:15¬
`), msg.Message)

	// assert tg
	assertInit(t, inputMsg, actual)
	assertNew(t, actual[0])

	ds, err := s.deploymentKV.All()
	require.NoError(t, err)
	assert.Equal(t, 1, len(ds))

	assert.Len(t, actual[0].Buttons, 1)
	assert.Equal(t, "promote", actual[0].Buttons[0].Message)
	assert.Equal(t, "/promote -id 1", actual[0].Buttons[0].Callback)
}

func TestBatchRevert(t *testing.T) {
	test.InitTestEnv()
	s, m, res, chatID := newService(t, layer.Layer_TEST, subscribe2.All, true)
	inputMsg := mock.NewInputMessageMock("/run -l test -v 0.0.1 my_service", tgLogin, chatID)
	res <- makeBatchState(statePb.DeploymentState_SUCCESS, dType.DeploymentType_REVERT, batch.State_Active, "")
	close(res)
	m.IN <- inputMsg
	actual := m.Read(t, 1)
	msg := actual[0]
	assert.Equal(t, s.msgS.BacktickHack(`Command: Revert
Service: ¬my_batch_service¬
Layer: Test
Version: ¬0.1.0¬
Initiator: [danevge](https://staff.yandex-team.ru/danevge)
Time: ¬2020-06-15T01:15¬
State: ¬success¬

Batch state: ¬Active¬
Periodic: ¬1 * * * *¬
Next run: ¬2020-06-15T01:15¬
`), msg.Message)

	// assert tg
	assertInit(t, inputMsg, actual)
	assertNew(t, actual[0])
	assert.Empty(t, actual[0].Buttons)

}

func TestBatchStop(t *testing.T) {
	test.RunUp(t)
	s, m, res, chatID := newService(t, layer.Layer_TEST, subscribe2.All, true)
	inputMsg := mock.NewInputMessageMock("/run -l test -v 0.0.1 my_service", tgLogin, chatID)
	res <- makeBatchState(statePb.DeploymentState_SUCCESS, dType.DeploymentType_STOP, batch.State_Inactive, "")
	close(res)
	m.IN <- inputMsg
	actual := m.Read(t, 1)
	msg := actual[0]
	assert.Equal(t, s.msgS.BacktickHack(`Command: Stop
Service: ¬my_batch_service¬
Layer: Test
Version: ¬0.1.0¬
Initiator: [danevge](https://staff.yandex-team.ru/danevge)
Time: ¬2020-06-15T01:15¬
State: ¬success¬

Batch state: ¬Inactive¬
`), msg.Message)

	// assert tg
	assertInit(t, inputMsg, actual)
	assertNew(t, actual[0])
	assert.Empty(t, actual[0].Buttons)

}

func TestBatchForceRun(t *testing.T) {
	test.RunUp(t)
	s, m, res, chatID := newService(t, layer.Layer_TEST, subscribe2.All, true)
	inputMsg := mock.NewInputMessageMock("/run -l test -force -v 0.0.1 my_service", tgLogin, chatID)
	state := makeBatchState(statePb.DeploymentState_SUCCESS, dType.DeploymentType_RUN, batch.State_Active, "")
	state.GetService().GetDeployment().Force = true
	res <- state
	close(res)
	m.IN <- inputMsg
	actual := m.Read(t, 1)
	msg := actual[0]
	assert.Equal(t, s.msgS.BacktickHack(`Command: Run
Service: ¬my_batch_service¬
Layer: Test
Version: ¬0.1.0¬
Initiator: [danevge](https://staff.yandex-team.ru/danevge)
Time: ¬2020-06-15T01:15¬
State: ¬success¬`), msg.Message)

	// assert tg
	assertInit(t, inputMsg, actual)
	assertNew(t, actual[0])
	assert.Empty(t, actual[0].Buttons)

}

func TestBatchRestart(t *testing.T) {
	test.RunUp(t)
	s, m, res, chatID := newService(t, layer.Layer_TEST, subscribe2.All, true)
	inputMsg := mock.NewInputMessageMock("/run -l test -v 0.0.1 my_service", tgLogin, chatID)
	res <- makeBatchState(statePb.DeploymentState_SUCCESS, dType.DeploymentType_RESTART, batch.State_Active, "")
	close(res)
	m.IN <- inputMsg
	actual := m.Read(t, 1)
	msg := actual[0]
	assert.Equal(t, s.msgS.BacktickHack(`Command: Restart
Service: ¬my_batch_service¬
Layer: Test
Version: ¬0.1.0¬
Initiator: [danevge](https://staff.yandex-team.ru/danevge)
Time: ¬2020-06-15T01:15¬
State: ¬success¬`), msg.Message)

	// assert tg
	assertInit(t, inputMsg, actual)
	assertNew(t, actual[0])
	assert.Empty(t, actual[0].Buttons)

}

func TestBatchPromote(t *testing.T) {
	test.RunUp(t)
	s, m, res, chatID := newService(t, layer.Layer_TEST, subscribe2.All, true)
	inputMsg := mock.NewInputMessageMock("/run -l test -v 0.0.1 my_service", tgLogin, chatID)
	state := makeBatchState(statePb.DeploymentState_SUCCESS, dType.DeploymentType_PROMOTE, batch.State_Active, "")
	state.GetService().GetDeployment().Layer = layer.Layer_PROD
	res <- state
	close(res)
	m.IN <- inputMsg
	actual := m.Read(t, 1)
	msg := actual[0]
	assert.Equal(t, s.msgS.BacktickHack(`Command: Promote
Service: ¬my_batch_service¬
Layer: Prod
Version: ¬0.1.0¬
Initiator: [danevge](https://staff.yandex-team.ru/danevge)
Time: ¬2020-06-15T01:15¬
State: ¬success¬

Batch state: ¬Active¬
Periodic: ¬1 * * * *¬
Next run: ¬2020-06-15T01:15¬
`), msg.Message)

	// assert tg
	assertInit(t, inputMsg, actual)
	assertNew(t, actual[0])
	assert.Empty(t, actual[0].Buttons)

}

func makeBatchState(st statePb.DeploymentState, dt dType.DeploymentType, bState batch.State, branch string) *deploy2.StateResponse {
	return &deploy2.StateResponse{
		Service: &info.DeploymentInfo{
			Map: &service_map.ServiceMap{
				Name: "my_batch_service",
				Type: service_map.ServiceType_batch,
			},
			Deployment: &deployment.Deployment{
				Id:          "1",
				State:       st,
				ServiceName: "my_batch_service",
				Branch:      branch,
				Version:     "0.1.0",
				User:        login,
				Type:        dt,
				Layer:       layer.Layer_TEST,
				Start:       start,
				Status:      &deployment.Status{},
			}},
		Batch: &batch.Batch{
			State:    bState,
			Periodic: "1 * * * *",
			NextRun:  start,
		},
	}
}
