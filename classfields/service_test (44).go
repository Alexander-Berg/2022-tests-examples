package api

import (
	"errors"
	"fmt"
	"github.com/YandexClassifieds/shiva/pkg/links"
	"math/rand"
	"testing"
	"time"

	"github.com/YandexClassifieds/shiva/cmd/shiva/scheduler/consul"
	"github.com/YandexClassifieds/shiva/cmd/telegram/api/handler/stream"
	"github.com/YandexClassifieds/shiva/cmd/telegram/bot"
	"github.com/YandexClassifieds/shiva/cmd/telegram/command"
	"github.com/YandexClassifieds/shiva/cmd/telegram/message"
	"github.com/YandexClassifieds/shiva/cmd/telegram/scheduler"
	"github.com/YandexClassifieds/shiva/cmd/telegram/subscribe"
	"github.com/YandexClassifieds/shiva/common/user_error"
	deployMocks "github.com/YandexClassifieds/shiva/pb/shiva/api/deploy/mocks"
	"github.com/YandexClassifieds/shiva/pb/shiva/api/deploy2"
	apiSm "github.com/YandexClassifieds/shiva/pb/shiva/api/service_map"
	"github.com/YandexClassifieds/shiva/pb/shiva/events/event2"
	"github.com/YandexClassifieds/shiva/pb/shiva/service_map"
	"github.com/YandexClassifieds/shiva/pb/shiva/types/deployment"
	dType "github.com/YandexClassifieds/shiva/pb/shiva/types/dtype"
	error2 "github.com/YandexClassifieds/shiva/pb/shiva/types/error"
	"github.com/YandexClassifieds/shiva/pb/shiva/types/info"
	"github.com/YandexClassifieds/shiva/pb/shiva/types/layer"
	"github.com/YandexClassifieds/shiva/pb/shiva/types/revert"
	dState "github.com/YandexClassifieds/shiva/pb/shiva/types/state"
	"github.com/YandexClassifieds/shiva/pkg/consul/kv"
	"github.com/YandexClassifieds/shiva/pkg/staff"
	staffapi "github.com/YandexClassifieds/shiva/pkg/staff/api"
	"github.com/YandexClassifieds/shiva/test"
	smock "github.com/YandexClassifieds/shiva/test/mock"
	"github.com/YandexClassifieds/shiva/test/mock/election"
	"github.com/YandexClassifieds/shiva/test/mock/mockery/mocks"
	"github.com/YandexClassifieds/shiva/test/test_db"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/mock"
	"github.com/stretchr/testify/require"
	"google.golang.org/grpc/codes"
	"google.golang.org/grpc/status"
	"google.golang.org/protobuf/types/known/timestamppb"
)

const (
	tgLogin     = "danevge"
	login       = "danevge"
	cancelLogin = "alexander-s"
	serviceName = "my_service"
	dUUID       = "dUUID"
	dID         = "1"
)

var (
	namespace = test.RandString(5)
	r         = rand.New(rand.NewSource(time.Now().UnixNano()))
	defLoc    = time.FixedZone("default_city", 3*3600) // TZ env may be empty
	start     = timestamppb.New(time.Date(2020, 6, 15, 1, 15, 30, 0, defLoc))
)

func newKV(t *testing.T) *kv.EphemeralKV {
	conf := kv.NewConf(namespace)
	conf.ServiceName = "shiva-ci/" + t.Name()
	conf.AllocationID = test.RandString(5)
	return kv.NewEphemeralKV(test.NewLogger(t), conf, &stream.MessageState{})
}

func TestRestoreStream_Run_InProgress(t *testing.T) {
	test.InitTestEnv()
	dKV := newKV(t)
	s, m, res, _ := newService(t, layer.Layer_TEST, subscribe.All, true)

	chatId := makeChatID()
	msgId := m.AddMessage(chatId, &smock.MsgInfo{Text: "init"})
	msgState := stream.NewMessageState(login, dID, chatId, msgId, dState.DeploymentState_IN_PROGRESS, dState.DeploymentState_IN_PROGRESS)
	require.NoError(t, dKV.Save(msgState.Key(), msgState))
	go s.restoreProcess()

	res <- makeState(1, 1, 0, dState.DeploymentState_IN_PROGRESS, dType.DeploymentType_RUN, layer.Layer_TEST)
	res <- makeState(1, 1, 0, dState.DeploymentState_IN_PROGRESS, dType.DeploymentType_RUN, layer.Layer_TEST)
	res <- makeState(1, 1, 1, dState.DeploymentState_SUCCESS, dType.DeploymentType_RUN, layer.Layer_TEST)
	close(res)
	actual := m.Read(t, 3)
	assert.Equal(t, s.msgS.BacktickHack(`Command: Run
Service: ¬my_service¬
Layer: Test
Version: ¬0.0.1¬
Initiator: [danevge](https://staff.yandex-team.ru/danevge)
Time: ¬2020-06-15T01:15¬

State: *progress*
myt: 1 of 1 (health check: 0)
sas: 1 of 1 (health check: 0)`), actual[0].Message)
	assert.Equal(t, s.msgS.BacktickHack(`Command: Run
Service: ¬my_service¬
Layer: Test
Version: ¬0.0.1¬
Initiator: [danevge](https://staff.yandex-team.ru/danevge)
Time: ¬2020-06-15T01:15¬

State: *success*
myt: 1 of 1 (health check: 1)
sas: 1 of 1 (health check: 1)`), actual[1].Message)
	assert.Equal(t, "Run *success* on #Test `my_service:0.0.1`", actual[2].Message)
	assert.Equal(t, "promote", actual[2].Buttons[0].Message)
	assert.Equal(t, "/promote -id 1", actual[2].Buttons[0].Callback)

	// assert tg
	assert.False(t, actual[0].IsNew)
	assertEdit(t, actual[0], actual[1])
	assertReplay(t, actual[1], actual[2])

}

func TestRestoreStream_Run_Success(t *testing.T) {
	test.RunUp(t)
	dKV := newKV(t)

	s, m, res, _ := newService(t, layer.Layer_TEST, subscribe.All, true)
	chatId := makeChatID()
	msgId := m.AddMessage(chatId, &smock.MsgInfo{Text: "init"})
	msgState := stream.NewMessageState(login, dID, chatId, msgId, dState.DeploymentState_IN_PROGRESS, dState.DeploymentState_IN_PROGRESS)
	require.NoError(t, dKV.Save(msgState.Key(), msgState))
	go s.restoreProcess()

	res <- makeState(1, 1, 1, dState.DeploymentState_SUCCESS, dType.DeploymentType_RUN, layer.Layer_TEST)
	close(res)
	actual := m.Read(t, 2)
	assert.Equal(t, s.msgS.BacktickHack(`Command: Run
Service: ¬my_service¬
Layer: Test
Version: ¬0.0.1¬
Initiator: [danevge](https://staff.yandex-team.ru/danevge)
Time: ¬2020-06-15T01:15¬

State: *success*
myt: 1 of 1 (health check: 1)
sas: 1 of 1 (health check: 1)`), actual[0].Message)
	assert.Equal(t, "Run *success* on #Test `my_service:0.0.1`", actual[1].Message)
	assert.Equal(t, "promote", actual[1].Buttons[0].Message)
	assert.Equal(t, "/promote -id 1", actual[1].Buttons[0].Callback)

	// assert tg
	assert.False(t, actual[0].IsNew)
	assertReplay(t, actual[0], actual[1])

}

func runStates(res chan *deploy2.StateResponse) {
	res <- makeState(1, 1, 0, dState.DeploymentState_PREPARE, dType.DeploymentType_RUN, layer.Layer_TEST)
	res <- makeState(1, 1, 0, dState.DeploymentState_IN_PROGRESS, dType.DeploymentType_RUN, layer.Layer_TEST)
	res <- makeState(1, 1, 0, dState.DeploymentState_IN_PROGRESS, dType.DeploymentType_RUN, layer.Layer_TEST)
	res <- makeState(1, 1, 0, dState.DeploymentState_IN_PROGRESS, dType.DeploymentType_RUN, layer.Layer_TEST)
	res <- makeState(1, 1, 1, dState.DeploymentState_SUCCESS, dType.DeploymentType_RUN, layer.Layer_TEST)
	close(res)
}

func TestRunWithComment(t *testing.T) {
	s, m, res, chatID := newService(t, layer.Layer_TEST, subscribe.All, true)
	runStates(res)
	initMSg := smock.NewInputMessageMock("/run -l test -v -c My comment `*marckdown*` 0.0.1 my_service", tgLogin, chatID)
	m.IN <- initMSg
	actual := m.Read(t, 4)

	// assert msg
	assert.Equal(t, "Run for `my_service:0.0.1` on Test", actual[0].Message)
	assert.Equal(t, s.msgS.BacktickHack(`Command: Run
Service: ¬my_service¬
Layer: Test
Version: ¬0.0.1¬
Initiator: [danevge](https://staff.yandex-team.ru/danevge)
Time: ¬2020-06-15T01:15¬

State: *progress*
myt: 1 of 1 (health check: 0)
sas: 1 of 1 (health check: 0)`), actual[1].Message)
	assert.Equal(t, s.msgS.BacktickHack(`Command: Run
Service: ¬my_service¬
Layer: Test
Version: ¬0.0.1¬
Initiator: [danevge](https://staff.yandex-team.ru/danevge)
Time: ¬2020-06-15T01:15¬

State: *success*
myt: 1 of 1 (health check: 1)
sas: 1 of 1 (health check: 1)`), actual[2].Message)
	assert.Equal(t, "Run *success* on #Test `my_service:0.0.1`", actual[3].Message)
	assert.Equal(t, "promote", actual[3].Buttons[0].Message)
	assert.Equal(t, "/promote -id 1", actual[3].Buttons[0].Callback)

	// assert tg
	assertInit(t, initMSg, actual)
	assertNew(t, actual[0])
	assertEdit(t, actual[0], actual[1])
	assertEdit(t, actual[1], actual[2])
	assertReplay(t, actual[2], actual[3])

	ds, err := s.deploymentKV.All()
	require.NoError(t, err)
	assert.Equal(t, 1, len(ds))

}

func TestRun(t *testing.T) {
	s, m, res, chatID := newService(t, layer.Layer_TEST, subscribe.All, true)
	runStates(res)
	initMSg := smock.NewInputMessageMock("/run -l test -v 0.0.1 my_service", tgLogin, chatID)

	m.IN <- initMSg
	actual := m.Read(t, 4)

	// assert msg
	assert.Equal(t, "Run for `my_service:0.0.1` on Test", actual[0].Message)
	assert.Equal(t, s.msgS.BacktickHack(`Command: Run
Service: ¬my_service¬
Layer: Test
Version: ¬0.0.1¬
Initiator: [danevge](https://staff.yandex-team.ru/danevge)
Time: ¬2020-06-15T01:15¬

State: *progress*
myt: 1 of 1 (health check: 0)
sas: 1 of 1 (health check: 0)`), actual[1].Message)
	assert.Equal(t, s.msgS.BacktickHack(`Command: Run
Service: ¬my_service¬
Layer: Test
Version: ¬0.0.1¬
Initiator: [danevge](https://staff.yandex-team.ru/danevge)
Time: ¬2020-06-15T01:15¬

State: *success*
myt: 1 of 1 (health check: 1)
sas: 1 of 1 (health check: 1)`), actual[2].Message)
	assert.Equal(t, "Run *success* on #Test `my_service:0.0.1`", actual[3].Message)
	assert.Equal(t, "promote", actual[3].Buttons[0].Message)
	assert.Equal(t, "/promote -id 1", actual[3].Buttons[0].Callback)

	// assert tg
	assertInit(t, initMSg, actual)
	assertNew(t, actual[0])
	assertEdit(t, actual[0], actual[1])
	assertEdit(t, actual[1], actual[2])
	assertReplay(t, actual[2], actual[3])

	ds, err := s.deploymentKV.All()
	require.NoError(t, err)
	assert.Equal(t, 1, len(ds))
}

func TestFail(t *testing.T) {
	_, m, res, chatID := newService(t, layer.Layer_TEST, subscribe.All, true)
	state := makeState(1, 1, 1, dState.DeploymentState_FAILED, dType.DeploymentType_RUN, layer.Layer_TEST)
	state.GetService().GetDeployment().Status = &deployment.Status{
		Error: &error2.UserError{
			RuMessage: "Ошибка, вроде, без доки, даже",
		},
	}
	res <- state
	initMSg := smock.NewInputMessageMock("/run -l test -v 0.0.1 my_service", tgLogin, chatID)

	m.IN <- initMSg
	actual := m.Read(t, 1)

	// assert msg
	assert.Equal(t, "Run *failed* on #Test `my_service:0.0.1`\nError: `Ошибка, вроде, без доки, даже`", actual[0].Message)

	// assert tg
	assertInit(t, initMSg, actual)
	assertNew(t, actual[0])
}

func TestFail_InvalidLayer(t *testing.T) {
	test.InitTestEnv()
	db := test_db.NewDb(t)
	log := test.NewLogger(t)
	service := command.NewService(subscribe.NewService(db, nil, log), nil)
	_, _, err := service.Run(makeChatID(), tgLogin, "/run -l -test shiva", dUUID)
	require.Error(t, err)
	assert.Contains(t, err.Error(), "invalid layer")
}

func TestFailFirstRunByTerminate(t *testing.T) {

	s, m, res, chatID := newService(t, layer.Layer_TEST, subscribe.All, true)
	res <- makeState(1, 1, 0, dState.DeploymentState_IN_PROGRESS, dType.DeploymentType_RUN, layer.Layer_TEST)
	res <- makeState(1, 1, 0, dState.DeploymentState_IN_PROGRESS, dType.DeploymentType_RUN, layer.Layer_TEST)
	failState := addRevertType(makeState(1, 1, 0, dState.DeploymentState_FAILED, dType.DeploymentType_RUN, layer.Layer_TEST), revert.RevertType_Terminate)
	failState.GetService().GetDeployment().GetStatus().Error = &error2.UserError{
		EnMessage: "fail without revert",
	}
	res <- failState

	initMSg := smock.NewInputMessageMock("/run -l test -v 0.0.1 my_service", tgLogin, chatID)

	m.IN <- initMSg
	actual := m.Read(t, 4)

	// assert msg
	assert.Equal(t, "Run for `my_service:0.0.1` on Test", actual[0].Message)
	assert.Equal(t, s.msgS.BacktickHack(`Command: Run
Service: ¬my_service¬
Layer: Test
Version: ¬0.0.1¬
Initiator: [danevge](https://staff.yandex-team.ru/danevge)
Time: ¬2020-06-15T01:15¬

State: *progress*
myt: 1 of 1 (health check: 0)
sas: 1 of 1 (health check: 0)`), actual[1].Message)
	assert.Equal(t, s.msgS.BacktickHack(`Command: Run
Service: ¬my_service¬
Layer: Test
Version: ¬0.0.1¬
Initiator: [danevge](https://staff.yandex-team.ru/danevge)
Time: ¬2020-06-15T01:15¬

State: *failed*
Error: ¬fail without revert¬
myt: 1 of 1 (health check: 0)
sas: 1 of 1 (health check: 0)`), actual[2].Message)
	assert.Equal(t, s.msgS.BacktickHack(fmt.Sprintf(`Run *failed* on #Test ¬my_service:0.0.1¬
Error: ¬fail without revert¬
docker container exited with non-zero code
Logs for your service:
- [111](%s)
- [222](%s)`,
		unescapedLogUrl("service=my_service layer=test allocation_id=111"),
		unescapedLogUrl("service=my_service layer=test allocation_id=222"))),
		actual[3].Message)

	// assert tg
	assertInit(t, initMSg, actual)
	assertNew(t, actual[0])

}

func TestFailFirstRunByUnhealthy(t *testing.T) {

	s, m, res, chatID := newService(t, layer.Layer_TEST, subscribe.All, true)
	res <- makeState(1, 1, 0, dState.DeploymentState_IN_PROGRESS, dType.DeploymentType_RUN, layer.Layer_TEST)
	res <- makeState(1, 1, 0, dState.DeploymentState_IN_PROGRESS, dType.DeploymentType_RUN, layer.Layer_TEST)
	failState := unhealthy(makeState(1, 1, 0, dState.DeploymentState_FAILED, dType.DeploymentType_RUN, layer.Layer_TEST),
		[]*deployment.ProvideStatus{
			{
				Provide: &service_map.ServiceProvides{Name: "monitoring"},
				Status:  true,
			},
			{
				Provide: &service_map.ServiceProvides{Name: "first-provider"},
				Status:  false,
			},
		},
	)
	failState.GetService().GetDeployment().GetStatus().Error = &error2.UserError{
		EnMessage: "fail without revert",
	}
	res <- failState

	initMSg := smock.NewInputMessageMock("/run -l test -v 0.0.1 my_service", tgLogin, chatID)

	m.IN <- initMSg
	actual := m.Read(t, 4)

	// assert msg
	assert.Equal(t, "Run for `my_service:0.0.1` on Test", actual[0].Message)
	assert.Equal(t, s.msgS.BacktickHack(`Command: Run
Service: ¬my_service¬
Layer: Test
Version: ¬0.0.1¬
Initiator: [danevge](https://staff.yandex-team.ru/danevge)
Time: ¬2020-06-15T01:15¬

State: *progress*
myt: 1 of 1 (health check: 0)
sas: 1 of 1 (health check: 0)`), actual[1].Message)
	assert.Equal(t, s.msgS.BacktickHack(`Command: Run
Service: ¬my_service¬
Layer: Test
Version: ¬0.0.1¬
Initiator: [danevge](https://staff.yandex-team.ru/danevge)
Time: ¬2020-06-15T01:15¬

State: *failed*
Error: ¬fail without revert¬
myt: 1 of 1 (health check: 0)
sas: 1 of 1 (health check: 0)`), actual[2].Message)
	failMsg := s.msgS.BacktickHack(fmt.Sprintf(`Run *failed* on #Test ¬my_service:0.0.1¬
Error: ¬fail without revert¬
some allocs were unhealthy, following checks failed:
- monitoring (port 81), [docs](%s) 
Logs for your service:
- [111](%s)
- [222](%s)`,
		message.DocsMonitoring,
		unescapedLogUrl("service=my_service layer=test allocation_id=111"),
		unescapedLogUrl("service=my_service layer=test allocation_id=222")))
	assert.Equal(t, failMsg, actual[3].Message)

	// assert tg
	assertInit(t, initMSg, actual)
	assertNew(t, actual[0])

}

func TestUpdateEventToCanaryOnePercent(t *testing.T) {
	s, m, res, _ := newService(t, layer.Layer_PROD, subscribe.All, true)
	res <- makeState(1, 1, 0, dState.DeploymentState_PREPARE, dType.DeploymentType_UPDATE, layer.Layer_PROD)
	res <- makeState(1, 1, 0, dState.DeploymentState_CANARY_PROGRESS, dType.DeploymentType_UPDATE, layer.Layer_PROD)
	res <- makeState(1, 1, 0, dState.DeploymentState_CANARY_PROGRESS, dType.DeploymentType_UPDATE, layer.Layer_PROD)
	res <- makeState(1, 1, 0, dState.DeploymentState_CANARY_PROGRESS, dType.DeploymentType_UPDATE, layer.Layer_PROD)
	res <- makeState(1, 1, 1, dState.DeploymentState_CANARY_ONE_PERCENT, dType.DeploymentType_UPDATE, layer.Layer_PROD)
	close(res)

	event := makeEvent(dType.DeploymentType_UPDATE, dState.DeploymentState_IN_PROGRESS)
	event.Deployment.Layer = layer.Layer_PROD
	require.NoError(t, s.handleDeploymentEvent(event))
	actual := m.Read(t, 4)

	//assert msg
	assert.Equal(t, "Update for `my_service:0.0.1` on Prod", actual[0].Message)
	assert.Equal(t, s.msgS.BacktickHack(`Command: Update
Service: ¬my_service¬
Layer: Prod
Version: ¬0.0.1¬
Initiator: [danevge](https://staff.yandex-team.ru/danevge)
Time: ¬2020-06-15T01:15¬

State: *canary in progress*
myt: 1 of 1 (health check: 0)
sas: 1 of 1 (health check: 0)`), actual[1].Message)
	assert.Equal(t, s.msgS.BacktickHack(`Command: Update
Service: ¬my_service¬
Layer: Prod
Version: ¬0.0.1¬
Initiator: [danevge](https://staff.yandex-team.ru/danevge)
Time: ¬2020-06-15T01:15¬

State: *canary with 1%*
myt: 1 of 1 (health check: 1)
sas: 1 of 1 (health check: 1)`), actual[2].Message)
	assert.Equal(t, "Update *canary with 1%* on #Prod `my_service:0.0.1`", actual[3].Message)
	assert.Equal(t, "promote", actual[3].Buttons[0].Message)
	assert.Equal(t, "/promote -id 1", actual[3].Buttons[0].Callback)
	assert.Equal(t, "cancel", actual[3].Buttons[1].Message)
	assert.Equal(t, "/cancel -id 1", actual[3].Buttons[1].Callback)

	// assert tg
	assertNew(t, actual[0])
	assertEdit(t, actual[0], actual[1])
	assertEdit(t, actual[1], actual[2])
	assertReplay(t, actual[2], actual[3])

}

func TestWithStateGroup(t *testing.T) {

	msgS := message.NewService(test.NewLogger(t), &NdaMock{}, message.NewMarkdown())

	testCases := []struct {
		name       string
		dState     dState.DeploymentState
		sg         subscribe.StateGroup
		revertType revert.RevertType

		isEmpty     bool
		expectedMsg string
	}{
		{
			name:    "Sub all. Skip end state",
			dState:  dState.DeploymentState_FAILED,
			sg:      subscribe.All,
			isEmpty: true,
		},
		{
			name:    "Sub all. Skip by state",
			dState:  dState.DeploymentState_CANCELED,
			sg:      subscribe.All,
			isEmpty: true,
		},
		{
			name:    "Sub fail. Skip end state",
			dState:  dState.DeploymentState_SUCCESS,
			sg:      subscribe.Fail,
			isEmpty: true,
		},
		{
			name:   "Sub end. Success state",
			dState: dState.DeploymentState_SUCCESS,
			sg:     subscribe.End,
			expectedMsg: msgS.BacktickHack(`Command: Run
Service: ¬my_service¬
Layer: Test
Version: ¬0.0.1¬
Initiator: [danevge](https://staff.yandex-team.ru/danevge)
Time: ¬2020-06-15T01:15¬

State: ¬success¬`),
		},

		{
			name:   "Sub end. Fail state without revert",
			dState: dState.DeploymentState_FAILED,
			sg:     subscribe.End,
			expectedMsg: msgS.BacktickHack(fmt.Sprintf(`Command: Run
Service: ¬my_service¬
Layer: Test
Version: ¬0.0.1¬
Initiator: [danevge](https://staff.yandex-team.ru/danevge)
Time: ¬2020-06-15T01:15¬

State: ¬failed¬
*Reason:* undefined fail
Logs for your service:
- [111](%s)
- [222](%s)`,
				unescapedLogUrl("service=my_service layer=test allocation_id=111"),
				unescapedLogUrl("service=my_service layer=test allocation_id=222"))),
		},

		{
			name:       "Sub Fail. Fail state with revert",
			dState:     dState.DeploymentState_FAILED,
			sg:         subscribe.Fail,
			revertType: revert.RevertType_OOM,
			expectedMsg: msgS.BacktickHack(fmt.Sprintf(`Command: Run
Service: ¬my_service¬
Layer: Test
Version: ¬0.0.1¬
Initiator: [danevge](https://staff.yandex-team.ru/danevge)
Time: ¬2020-06-15T01:15¬

State: ¬reverted¬
*Reason:* docker container exited because of OOM Killer
Logs for your service:
- [111](%s)
- [222](%s)`,
				unescapedLogUrl("service=my_service layer=test allocation_id=111"),
				unescapedLogUrl("service=my_service layer=test allocation_id=222"))),
		},
		{
			name:       "Sub End. Fail state with revert",
			dState:     dState.DeploymentState_FAILED,
			sg:         subscribe.End,
			revertType: revert.RevertType_OOM,
			expectedMsg: msgS.BacktickHack(fmt.Sprintf(`Command: Run
Service: ¬my_service¬
Layer: Test
Version: ¬0.0.1¬
Initiator: [danevge](https://staff.yandex-team.ru/danevge)
Time: ¬2020-06-15T01:15¬

State: ¬reverted¬
*Reason:* docker container exited because of OOM Killer
Logs for your service:
- [111](%s)
- [222](%s)`,
				unescapedLogUrl("service=my_service layer=test allocation_id=111"),
				unescapedLogUrl("service=my_service layer=test allocation_id=222"))),
		},
	}

	for _, tc := range testCases {
		t.Run(tc.name, func(t *testing.T) {

			s, m, _, _ := newService(t, layer.Layer_TEST, tc.sg, true)

			d := &deployment.Deployment{
				Id:          "1",
				ServiceName: "my_service",
				Version:     "0.0.1",
				User:        login,
				Type:        dType.DeploymentType_RUN,
				Layer:       layer.Layer_TEST,
				State:       tc.dState,
				RevertType:  tc.revertType,
				Status: &deployment.Status{
					RevertType:      tc.revertType,
					FailAllocations: []string{"111", "222"},
				},
				Start: start,
			}

			event := &event2.Event{Deployment: d}

			require.NoError(t, s.handleDeploymentEvent(event))

			if !tc.isEmpty {
				actual := m.Read(t, 1)

				require.Equal(t, tc.expectedMsg, actual[0].Message)
			} else {
				m.Read(t, 0)
			}
		})
	}

}

func TestSkipUpdateEvent(t *testing.T) {
	s, m, res, _ := newService(t, layer.Layer_TEST, subscribe.All, false)
	res <- makeState(1, 1, 0, dState.DeploymentState_PREPARE, dType.DeploymentType_UPDATE, layer.Layer_TEST)
	res <- makeState(1, 1, 0, dState.DeploymentState_IN_PROGRESS, dType.DeploymentType_UPDATE, layer.Layer_TEST)
	res <- makeState(1, 1, 0, dState.DeploymentState_IN_PROGRESS, dType.DeploymentType_UPDATE, layer.Layer_TEST)
	res <- makeState(1, 1, 0, dState.DeploymentState_IN_PROGRESS, dType.DeploymentType_UPDATE, layer.Layer_TEST)
	res <- makeState(1, 1, 1, dState.DeploymentState_SUCCESS, dType.DeploymentType_UPDATE, layer.Layer_TEST)
	close(res)

	require.NoError(t, s.handleDeploymentEvent(makeEvent(dType.DeploymentType_UPDATE, dState.DeploymentState_IN_PROGRESS)))

	m.Read(t, 0)
}

func TestRunEventToCanaryOnePercent(t *testing.T) {
	s, m, res, _ := newService(t, layer.Layer_PROD, subscribe.All, true)
	res <- makeState(1, 1, 0, dState.DeploymentState_PREPARE, dType.DeploymentType_RUN, layer.Layer_PROD)
	res <- makeState(1, 1, 0, dState.DeploymentState_CANARY_PROGRESS, dType.DeploymentType_RUN, layer.Layer_PROD)
	res <- makeState(1, 1, 0, dState.DeploymentState_CANARY_PROGRESS, dType.DeploymentType_RUN, layer.Layer_PROD)
	res <- makeState(1, 1, 0, dState.DeploymentState_CANARY_PROGRESS, dType.DeploymentType_RUN, layer.Layer_PROD)
	res <- makeState(1, 1, 1, dState.DeploymentState_CANARY_ONE_PERCENT, dType.DeploymentType_RUN, layer.Layer_PROD)
	close(res)

	event := makeEvent(dType.DeploymentType_RUN, dState.DeploymentState_IN_PROGRESS)
	event.Deployment.Layer = layer.Layer_PROD
	require.NoError(t, s.handleDeploymentEvent(event))
	actual := m.Read(t, 4)

	//assert msg
	assert.Equal(t, "Run for `my_service:0.0.1` on Prod", actual[0].Message)
	assert.Equal(t, s.msgS.BacktickHack(`Command: Run
Service: ¬my_service¬
Layer: Prod
Version: ¬0.0.1¬
Initiator: [danevge](https://staff.yandex-team.ru/danevge)
Time: ¬2020-06-15T01:15¬

State: *canary in progress*
myt: 1 of 1 (health check: 0)
sas: 1 of 1 (health check: 0)`), actual[1].Message)
	assert.Equal(t, s.msgS.BacktickHack(`Command: Run
Service: ¬my_service¬
Layer: Prod
Version: ¬0.0.1¬
Initiator: [danevge](https://staff.yandex-team.ru/danevge)
Time: ¬2020-06-15T01:15¬

State: *canary with 1%*
myt: 1 of 1 (health check: 1)
sas: 1 of 1 (health check: 1)`), actual[2].Message)
	assert.Equal(t, "Run *canary with 1%* on #Prod `my_service:0.0.1`", actual[3].Message)
	assert.Equal(t, "promote", actual[3].Buttons[0].Message)
	assert.Equal(t, "/promote -id 1", actual[3].Buttons[0].Callback)
	assert.Equal(t, "cancel", actual[3].Buttons[1].Message)
	assert.Equal(t, "/cancel -id 1", actual[3].Buttons[1].Callback)

	// assert tg
	assertNew(t, actual[0])
	assertEdit(t, actual[0], actual[1])
	assertEdit(t, actual[1], actual[2])
	assertReplay(t, actual[2], actual[3])

}

func TestRunToCanaryOnePercent(t *testing.T) {

	s, m, res, chatID := newService(t, layer.Layer_PROD, subscribe.All, true)
	initMSg := smock.NewInputMessageMock("/run -l prod -v 0.0.1 my_service", tgLogin, chatID)
	m.IN <- initMSg
	res <- makeState(1, 1, 0, dState.DeploymentState_PREPARE, dType.DeploymentType_RUN, layer.Layer_PROD)
	res <- makeState(1, 1, 0, dState.DeploymentState_CANARY_PROGRESS, dType.DeploymentType_RUN, layer.Layer_PROD)
	res <- makeState(1, 1, 0, dState.DeploymentState_CANARY_PROGRESS, dType.DeploymentType_RUN, layer.Layer_PROD)
	res <- makeState(1, 1, 0, dState.DeploymentState_CANARY_PROGRESS, dType.DeploymentType_RUN, layer.Layer_PROD)
	res <- makeState(1, 1, 1, dState.DeploymentState_CANARY_ONE_PERCENT, dType.DeploymentType_RUN, layer.Layer_PROD)
	close(res)

	actual := m.Read(t, 4)

	//assert msg
	assert.Equal(t, "Run for `my_service:0.0.1` on Prod", actual[0].Message)
	assert.Equal(t, s.msgS.BacktickHack(`Command: Run
Service: ¬my_service¬
Layer: Prod
Version: ¬0.0.1¬
Initiator: [danevge](https://staff.yandex-team.ru/danevge)
Time: ¬2020-06-15T01:15¬

State: *canary in progress*
myt: 1 of 1 (health check: 0)
sas: 1 of 1 (health check: 0)`), actual[1].Message)
	assert.Equal(t, s.msgS.BacktickHack(`Command: Run
Service: ¬my_service¬
Layer: Prod
Version: ¬0.0.1¬
Initiator: [danevge](https://staff.yandex-team.ru/danevge)
Time: ¬2020-06-15T01:15¬

State: *canary with 1%*
myt: 1 of 1 (health check: 1)
sas: 1 of 1 (health check: 1)`), actual[2].Message)
	assert.Equal(t, "Run *canary with 1%* on #Prod `my_service:0.0.1`", actual[3].Message)
	assert.Equal(t, "promote", actual[3].Buttons[0].Message)
	assert.Equal(t, "/promote -id 1", actual[3].Buttons[0].Callback)
	assert.Equal(t, "cancel", actual[3].Buttons[1].Message)
	assert.Equal(t, "/cancel -id 1", actual[3].Buttons[1].Callback)

	// assert tg
	assertInit(t, initMSg, actual)
	assertNew(t, actual[0])
	assertEdit(t, actual[0], actual[1])
	assertEdit(t, actual[1], actual[2])
	assertReplay(t, actual[2], actual[3])

}

func TestPromoteSox(t *testing.T) {
	s, m, res, chatID := newService(t, layer.Layer_PROD, subscribe.All, true)
	inputMsg := smock.NewInputMessageMock("/promote -id 1", tgLogin, chatID)
	m.IN <- inputMsg
	res <- makeState(0, 0, 0, dState.DeploymentState_WAIT_APPROVE, dType.DeploymentType_PROMOTE, layer.Layer_PROD)
	close(res)

	actual := m.Read(t, 1)[0]
	assert.Equal(t, s.msgS.BacktickHack(`Command: Promote
Service: ¬my_service¬
Layer: Prod
Version: ¬0.0.1¬
Initiator: [danevge](https://staff.yandex-team.ru/danevge)
Time: ¬2020-06-15T01:15¬

State: *wait approve*`), actual.Message)
	require.Equal(t, 2, len(actual.Buttons))
	assert.Equal(t, "approve", actual.Buttons[0].Message)
	assert.Equal(t, "cancel", actual.Buttons[1].Message)

}

func TestPromote(t *testing.T) {

	s, m, res, chatID := newService(t, layer.Layer_PROD, subscribe.All, true)
	initMSg := smock.NewInputMessageMock("/promote -id 1", tgLogin, chatID)
	m.IN <- initMSg
	res <- makeState(1, 1, 0, dState.DeploymentState_PREPARE, dType.DeploymentType_PROMOTE, layer.Layer_PROD)
	res <- makeState(1, 1, 0, dState.DeploymentState_IN_PROGRESS, dType.DeploymentType_PROMOTE, layer.Layer_PROD)
	res <- makeState(1, 1, 0, dState.DeploymentState_IN_PROGRESS, dType.DeploymentType_PROMOTE, layer.Layer_PROD)
	res <- makeState(1, 1, 0, dState.DeploymentState_IN_PROGRESS, dType.DeploymentType_PROMOTE, layer.Layer_PROD)
	res <- makeState(1, 1, 1, dState.DeploymentState_SUCCESS, dType.DeploymentType_PROMOTE, layer.Layer_PROD)
	close(res)

	actual := m.Read(t, 4)

	assert.Equal(t, "Promote for `my_service:0.0.1` on Prod", actual[0].Message)
	assert.Equal(t, s.msgS.BacktickHack(`Command: Promote
Service: ¬my_service¬
Layer: Prod
Version: ¬0.0.1¬
Initiator: [danevge](https://staff.yandex-team.ru/danevge)
Time: ¬2020-06-15T01:15¬

State: *progress*
myt: 1 of 1 (health check: 0)
sas: 1 of 1 (health check: 0)`), actual[1].Message)
	assert.Equal(t, s.msgS.BacktickHack(`Command: Promote
Service: ¬my_service¬
Layer: Prod
Version: ¬0.0.1¬
Initiator: [danevge](https://staff.yandex-team.ru/danevge)
Time: ¬2020-06-15T01:15¬

State: *success*
myt: 1 of 1 (health check: 1)
sas: 1 of 1 (health check: 1)`), actual[2].Message)
	assert.Equal(t, "Promote *success* on #Prod `my_service:0.0.1`", actual[3].Message)
	assert.Equal(t, 0, len(actual[3].Buttons))

	// assert tg
	assertInit(t, initMSg, actual)
	assertNew(t, actual[0])
	assertEdit(t, actual[0], actual[1])
	assertEdit(t, actual[1], actual[2])
	assertReplay(t, actual[2], actual[3])
}

func TestButtonHideOnCanaryEvent(t *testing.T) {
	s, m, _, chatID := newService(t, layer.Layer_PROD, subscribe.All, true)

	msgId := m.AddMessage(
		chatID,
		&smock.MsgInfo{
			Text: "Run *canary with 1%* on #Prod `my_service:0.0.1`",
			Buttons: []*bot.Button{
				{Message: "promote", Callback: "/promote -id 1"},
				{Message: "cancel", Callback: "/cancel -id 1"},
			},
		})
	msgState := stream.NewMessageState(login, dID, chatID, msgId, dState.DeploymentState_CANARY_ONE_PERCENT, dState.DeploymentState_CANARY_PROGRESS)
	require.NoError(t, newKV(t).Save(msgState.Key(), msgState))

	event := makeEvent(dType.DeploymentType_RUN, dState.DeploymentState_IN_PROGRESS)
	event.Deployment.State = dState.DeploymentState_CANARY
	event.Deployment.Layer = layer.Layer_PROD

	require.NoError(t, s.handleDeploymentEvent(event))

	msgHistory := m.History(chatID)

	assert.Equal(t, 1, len(msgHistory))
	assert.Equal(t, "Run *canary with 1%* on #Prod `my_service:0.0.1`", msgHistory[0].Text)
	assert.Equal(t, 0, len(msgHistory[0].Buttons))
}

func TestButtonHideOnCanaryStoppedEvent(t *testing.T) {
	s, m, _, chatID := newService(t, layer.Layer_PROD, subscribe.All, true)

	msgId := m.AddMessage(chatID, &smock.MsgInfo{Text: "Run *canary with 1%* on #Prod `my_service:0.0.1`"})
	msgState := stream.NewMessageState(login, dID, chatID, msgId, dState.DeploymentState_CANARY, dState.DeploymentState_CANARY_PROGRESS)
	require.NoError(t, newKV(t).Save(msgState.Key(), msgState))

	msgId = m.AddMessage(
		chatID,
		&smock.MsgInfo{
			Text: "Run *canary with real traffic* on #Prod `my_service:0.0.1`",
			Buttons: []*bot.Button{
				{Message: "promote", Callback: "/promote -id 1"},
				{Message: "cancel", Callback: "/cancel -id 1"},
			},
		})
	msgState = stream.NewMessageState(login, dID, chatID, msgId, dState.DeploymentState_CANARY, dState.DeploymentState_CANARY)
	require.NoError(t, newKV(t).Save(msgState.Key(), msgState))

	event := makeEvent(dType.DeploymentType_RUN, dState.DeploymentState_IN_PROGRESS)
	event.Deployment.State = dState.DeploymentState_CANARY_STOPPED
	event.Deployment.Layer = layer.Layer_PROD

	require.NoError(t, s.handleDeploymentEvent(event))

	msgHistory := m.History(chatID)

	assert.Equal(t, 2, len(msgHistory))
	assert.Equal(t, "Run *canary with 1%* on #Prod `my_service:0.0.1`", msgHistory[0].Text)
	assert.Equal(t, 0, len(msgHistory[0].Buttons))

	assert.Equal(t, "Run *canary with real traffic* on #Prod `my_service:0.0.1`", msgHistory[1].Text)
	assert.Equal(t, 0, len(msgHistory[1].Buttons))
}

func TestButtonHideOnPromoteSuccessEvent(t *testing.T) {
	s, m, _, chatID := newService(t, layer.Layer_PROD, subscribe.All, true)
	parentDid := "2"
	targetMsgText := "Run *success* on #Test `my_service:0.0.1`"
	msgId := m.AddMessage(
		chatID,
		&smock.MsgInfo{
			Text: targetMsgText,
			Buttons: []*bot.Button{
				{Message: "promote", Callback: "/promote -id 1"},
			},
		})
	msgState := stream.NewMessageState(login, parentDid, chatID, msgId, dState.DeploymentState_SUCCESS, dState.DeploymentState_IN_PROGRESS)
	require.NoError(t, newKV(t).Save(msgState.Key(), msgState))
	nextMessageText := "Promote stuff"
	msgId = m.AddMessage(
		chatID,
		&smock.MsgInfo{
			Text: nextMessageText,
		})
	msgState = stream.NewMessageState(login, dID, chatID, msgId, dState.DeploymentState_SUCCESS, dState.DeploymentState_IN_PROGRESS)
	require.NoError(t, newKV(t).Save(msgState.Key(), msgState))
	event := makeEvent(dType.DeploymentType_PROMOTE, dState.DeploymentState_IN_PROGRESS)
	event.Deployment.State = dState.DeploymentState_SUCCESS
	event.ParentDeployment = &deployment.Deployment{
		Id:          parentDid,
		ServiceName: "my_service",
		Version:     "0.0.1",
		User:        login,
		Type:        dType.DeploymentType_RUN,
		Layer:       layer.Layer_TEST,
		State:       dState.DeploymentState_SUCCESS,
		Status:      &deployment.Status{},
	}

	require.NoError(t, s.handleDeploymentEvent(event))

	msgHistory := m.History(chatID)
	assert.Equal(t, 2, len(msgHistory))
	assert.Equal(t, targetMsgText, msgHistory[0].Text)
	assert.Equal(t, 0, len(msgHistory[0].Buttons))
	assert.Equal(t, nextMessageText, msgHistory[1].Text)
	assert.Equal(t, 0, len(msgHistory[1].Buttons))
}

func TestButtonHideOnPromoteSox(t *testing.T) {
	s, m, _, chatID := newService(t, layer.Layer_PROD, subscribe.All, true)
	parentDid := "2"
	targetMsgText := "Run *success* on #Test `my_service:0.0.1`"
	msgId := m.AddMessage(
		chatID,
		&smock.MsgInfo{
			Text: targetMsgText,
			Buttons: []*bot.Button{
				{Message: "promote", Callback: "/promote -id 1"},
			},
		})
	msgState := stream.NewMessageState(login, parentDid, chatID, msgId, dState.DeploymentState_SUCCESS, dState.DeploymentState_IN_PROGRESS)
	require.NoError(t, newKV(t).Save(msgState.Key(), msgState))
	nextMessageText := "Promote stuff"
	msgId = m.AddMessage(
		chatID,
		&smock.MsgInfo{
			Text: nextMessageText,
		})
	msgState = stream.NewMessageState(login, dID, chatID, msgId, dState.DeploymentState_SUCCESS, dState.DeploymentState_IN_PROGRESS)
	require.NoError(t, newKV(t).Save(msgState.Key(), msgState))
	event := makeEvent(dType.DeploymentType_PROMOTE, dState.DeploymentState_IN_PROGRESS)
	event.Deployment.State = dState.DeploymentState_WAIT_APPROVE
	event.ServiceMap = &service_map.ServiceMap{
		Sox: true,
	}
	event.ParentDeployment = &deployment.Deployment{
		Id:          parentDid,
		ServiceName: "my_service",
		Version:     "0.0.1",
		User:        login,
		Type:        dType.DeploymentType_RUN,
		Layer:       layer.Layer_TEST,
		State:       dState.DeploymentState_SUCCESS,
		Status:      &deployment.Status{},
	}

	require.NoError(t, s.handleDeploymentEvent(event))

	msgHistory := m.History(chatID)
	assert.Equal(t, 2, len(msgHistory))
	assert.Equal(t, targetMsgText, msgHistory[0].Text)
	assert.Equal(t, 0, len(msgHistory[0].Buttons))
	assert.Equal(t, nextMessageText, msgHistory[1].Text)
	assert.Equal(t, 0, len(msgHistory[1].Buttons))
}

func TestButtonHideOnApproveSox(t *testing.T) {
	testCases := []struct {
		Name       string
		IsApproved bool
	}{
		{Name: "Approved", IsApproved: true},
		{Name: "Canceled"},
	}
	for _, tc := range testCases {
		t.Run(tc.Name, func(t *testing.T) {
			s, m, _, chatID := newService(t, layer.Layer_PROD, subscribe.All, true)
			parentDid := "2"
			targetMsgText := s.msgS.BacktickHack(`Command: Promote
Service: ¬my_service¬
Layer: Prod
Version: ¬0.0.1¬
Initiator: [danevge](https://staff.yandex-team.ru/danevge)
Time: ¬2022-02-17T12:00¬

State: *wait approve*`)
			msgId := m.AddMessage(
				chatID,
				&smock.MsgInfo{
					Text: targetMsgText,
					Buttons: []*bot.Button{
						{Message: "approve", Callback: "/promote -id 1"},
						{Message: "cancel", Callback: "/cancel -id 1"},
					},
				})
			msgState := stream.NewMessageState(login, dID, chatID, msgId, dState.DeploymentState_WAIT_APPROVE, dState.DeploymentState_WAIT_APPROVE)
			require.NoError(t, newKV(t).Save(msgState.Key(), msgState))
			nextMessageText := "Promote stuff"
			msgId = m.AddMessage(
				chatID,
				&smock.MsgInfo{
					Text: nextMessageText,
				})
			msgState = stream.NewMessageState(login, dID, chatID, msgId, dState.DeploymentState_SUCCESS, dState.DeploymentState_IN_PROGRESS)
			require.NoError(t, newKV(t).Save(msgState.Key(), msgState))
			promoteState := dState.DeploymentState_SUCCESS
			if !tc.IsApproved {
				promoteState = dState.DeploymentState_CANCELED
			}

			event := makeEvent(dType.DeploymentType_PROMOTE, promoteState)
			event.ServiceMap = &service_map.ServiceMap{
				Sox: true,
			}
			event.Deployment.Layer = layer.Layer_PROD

			event.Deployment.Start = &timestamppb.Timestamp{Seconds: 1645088400}
			event.ParentDeployment = &deployment.Deployment{
				Id:          parentDid,
				ServiceName: "my_service",
				Version:     "0.0.1",
				User:        login,
				Type:        dType.DeploymentType_RUN,
				Layer:       layer.Layer_TEST,
				State:       dState.DeploymentState_SUCCESS,
				Status:      &deployment.Status{},
			}

			require.NoError(t, s.handleDeploymentEvent(event))

			msgHistory := m.History(chatID)
			assert.Equal(t, 2, len(msgHistory))
			assert.Equal(t, targetMsgText, msgHistory[0].Text)
			assert.Equal(t, 0, len(msgHistory[0].Buttons))
			assert.Equal(t, nextMessageText, msgHistory[1].Text)
			assert.Equal(t, 0, len(msgHistory[1].Buttons))
		})
	}

}

func TestPromoteToCanaryOnePercent(t *testing.T) {
	s, m, res, chatID := newService(t, layer.Layer_PROD, subscribe.All, true)
	initMSg := smock.NewInputMessageMock("/promote -id 1", tgLogin, chatID)
	m.IN <- initMSg
	res <- makeState(1, 1, 0, dState.DeploymentState_PREPARE, dType.DeploymentType_PROMOTE, layer.Layer_PROD)
	res <- makeState(1, 1, 0, dState.DeploymentState_CANARY_PROGRESS, dType.DeploymentType_PROMOTE, layer.Layer_PROD)
	res <- makeState(1, 1, 0, dState.DeploymentState_CANARY_PROGRESS, dType.DeploymentType_PROMOTE, layer.Layer_PROD)
	res <- makeState(1, 1, 0, dState.DeploymentState_CANARY_PROGRESS, dType.DeploymentType_PROMOTE, layer.Layer_PROD)
	res <- makeState(1, 1, 1, dState.DeploymentState_CANARY_ONE_PERCENT, dType.DeploymentType_PROMOTE, layer.Layer_PROD)
	close(res)

	actual := m.Read(t, 4)

	assert.Equal(t, "Promote for `my_service:0.0.1` on Prod", actual[0].Message)
	assert.Equal(t, s.msgS.BacktickHack(`Command: Promote
Service: ¬my_service¬
Layer: Prod
Version: ¬0.0.1¬
Initiator: [danevge](https://staff.yandex-team.ru/danevge)
Time: ¬2020-06-15T01:15¬

State: *canary in progress*
myt: 1 of 1 (health check: 0)
sas: 1 of 1 (health check: 0)`), actual[1].Message)
	assert.Equal(t, s.msgS.BacktickHack(`Command: Promote
Service: ¬my_service¬
Layer: Prod
Version: ¬0.0.1¬
Initiator: [danevge](https://staff.yandex-team.ru/danevge)
Time: ¬2020-06-15T01:15¬

State: *canary with 1%*
myt: 1 of 1 (health check: 1)
sas: 1 of 1 (health check: 1)`), actual[2].Message)
	assert.Equal(t, "Promote *canary with 1%* on #Prod `my_service:0.0.1`", actual[3].Message)
	assert.Equal(t, "promote", actual[3].Buttons[0].Message)
	assert.Equal(t, "/promote -id 1", actual[3].Buttons[0].Callback)
	assert.Equal(t, "cancel", actual[3].Buttons[1].Message)
	assert.Equal(t, "/cancel -id 1", actual[3].Buttons[1].Callback)

	// assert tg
	assertInit(t, initMSg, actual)
	assertNew(t, actual[0])
	assertEdit(t, actual[0], actual[1])
	assertEdit(t, actual[1], actual[2])
	assertReplay(t, actual[2], actual[3])

}

func TestStop(t *testing.T) {
	_, m, res, chatID := newService(t, layer.Layer_TEST, subscribe.All, true)
	res <- makeState(1, 1, 0, dState.DeploymentState_PREPARE, dType.DeploymentType_STOP, layer.Layer_TEST)
	res <- makeState(1, 1, 0, dState.DeploymentState_IN_PROGRESS, dType.DeploymentType_STOP, layer.Layer_TEST)
	res <- makeState(1, 1, 1, dState.DeploymentState_SUCCESS, dType.DeploymentType_STOP, layer.Layer_TEST)
	close(res)
	initMSg := smock.NewInputMessageMock("/stop -l test -v 0.0.1 my_service", tgLogin, chatID)
	m.IN <- initMSg
	actual := m.Read(t, 1)

	assert.Equal(t, "Stop *success* on #Test `my_service:0.0.1`", actual[0].Message)

	// assert tg
	assertInit(t, initMSg, actual)
	assertNew(t, actual[0])

}

func TestRestart(t *testing.T) {
	s, m, res, chatID := newService(t, layer.Layer_TEST, subscribe.All, true)
	res <- makeState(1, 1, 0, dState.DeploymentState_PREPARE, dType.DeploymentType_RESTART, layer.Layer_TEST)
	res <- makeState(1, 1, 0, dState.DeploymentState_IN_PROGRESS, dType.DeploymentType_RESTART, layer.Layer_TEST)
	res <- makeState(1, 1, 0, dState.DeploymentState_IN_PROGRESS, dType.DeploymentType_RESTART, layer.Layer_TEST)
	res <- makeState(1, 1, 0, dState.DeploymentState_IN_PROGRESS, dType.DeploymentType_RESTART, layer.Layer_TEST)
	res <- makeState(1, 1, 1, dState.DeploymentState_SUCCESS, dType.DeploymentType_RESTART, layer.Layer_TEST)
	close(res)
	initMSg := smock.NewInputMessageMock("/restart -l test my_service", tgLogin, chatID)
	m.IN <- initMSg
	actual := m.Read(t, 4)

	// assert msg
	assert.Equal(t, "Restart for `my_service:0.0.1` on Test", actual[0].Message)
	assert.Equal(t, s.msgS.BacktickHack(`Command: Restart
Service: ¬my_service¬
Layer: Test
Version: ¬0.0.1¬
Initiator: [danevge](https://staff.yandex-team.ru/danevge)
Time: ¬2020-06-15T01:15¬

State: *progress*
myt: 1 of 1 (health check: 0)
sas: 1 of 1 (health check: 0)`), actual[1].Message)
	assert.Equal(t, s.msgS.BacktickHack(`Command: Restart
Service: ¬my_service¬
Layer: Test
Version: ¬0.0.1¬
Initiator: [danevge](https://staff.yandex-team.ru/danevge)
Time: ¬2020-06-15T01:15¬

State: *success*
myt: 1 of 1 (health check: 1)
sas: 1 of 1 (health check: 1)`), actual[2].Message)
	assert.Equal(t, "Restart *success* on #Test `my_service:0.0.1`", actual[3].Message)

	// assert tg
	assertInit(t, initMSg, actual)
	assertNew(t, actual[0])
	assertEdit(t, actual[0], actual[1])
	assertEdit(t, actual[1], actual[2])
	assertReplay(t, actual[2], actual[3])

}

func TestRevert(t *testing.T) {

	s, m, res, chatID := newService(t, layer.Layer_TEST, subscribe.All, true)
	res <- makeState(1, 1, 0, dState.DeploymentState_PREPARE, dType.DeploymentType_REVERT, layer.Layer_TEST)
	res <- makeState(1, 1, 0, dState.DeploymentState_IN_PROGRESS, dType.DeploymentType_REVERT, layer.Layer_TEST)
	res <- makeState(1, 1, 0, dState.DeploymentState_IN_PROGRESS, dType.DeploymentType_REVERT, layer.Layer_TEST)
	res <- makeState(1, 1, 0, dState.DeploymentState_IN_PROGRESS, dType.DeploymentType_REVERT, layer.Layer_TEST)
	res <- makeState(1, 1, 1, dState.DeploymentState_SUCCESS, dType.DeploymentType_REVERT, layer.Layer_TEST)
	close(res)
	initMSg := smock.NewInputMessageMock("/revert -l test my_service", tgLogin, chatID)
	m.IN <- initMSg
	actual := m.Read(t, 4)

	// assert msg
	assert.Equal(t, "Revert for `my_service:0.0.1` on Test", actual[0].Message)
	assert.Equal(t, s.msgS.BacktickHack(`Command: Revert
Service: ¬my_service¬
Layer: Test
Version: ¬0.0.1¬
Initiator: [danevge](https://staff.yandex-team.ru/danevge)
Time: ¬2020-06-15T01:15¬

State: *progress*
myt: 1 of 1 (health check: 0)
sas: 1 of 1 (health check: 0)`), actual[1].Message)
	assert.Equal(t, s.msgS.BacktickHack(`Command: Revert
Service: ¬my_service¬
Layer: Test
Version: ¬0.0.1¬
Initiator: [danevge](https://staff.yandex-team.ru/danevge)
Time: ¬2020-06-15T01:15¬

State: *success*
myt: 1 of 1 (health check: 1)
sas: 1 of 1 (health check: 1)`), actual[2].Message)
	assert.Equal(t, "Revert *success* on #Test `my_service:0.0.1`", actual[3].Message)

	// assert tg
	assertInit(t, initMSg, actual)
	assertNew(t, actual[0])
	assertEdit(t, actual[0], actual[1])
	assertEdit(t, actual[1], actual[2])
	assertReplay(t, actual[2], actual[3])

}

func TestCancelButton(t *testing.T) {

	_, m, res, chatID := newService(t, layer.Layer_TEST, subscribe.All, true)
	res <- makeState(0, 1, 0, dState.DeploymentState_PREPARE, dType.DeploymentType_CANCEL, layer.Layer_TEST)
	res <- makeState(0, 1, 0, dState.DeploymentState_CANCEL, dType.DeploymentType_CANCEL, layer.Layer_TEST)
	res <- makeState(1, 1, 0, dState.DeploymentState_CANCEL, dType.DeploymentType_CANCEL, layer.Layer_TEST)
	res <- makeState(1, 1, 0, dState.DeploymentState_CANCEL, dType.DeploymentType_CANCEL, layer.Layer_TEST)
	res <- makeState(1, 1, 1, dState.DeploymentState_CANCELED, dType.DeploymentType_CANCEL, layer.Layer_TEST)
	close(res)
	initMSg := smock.NewCallbackInputMessageMock("/cancel -id 1", tgLogin, chatID)
	m.IN <- initMSg
	m.Read(t, 0)

}

func TestCancelRunCommand(t *testing.T) {

	s, m, res, chatID := newService(t, layer.Layer_TEST, subscribe.All, true)
	res <- makeState(1, 0, 0, dState.DeploymentState_IN_PROGRESS, dType.DeploymentType_RUN, layer.Layer_TEST)
	res <- makeState(1, 1, 0, dState.DeploymentState_IN_PROGRESS, dType.DeploymentType_RUN, layer.Layer_TEST)
	res <- makeState(1, 0, 0, dState.DeploymentState_CANCEL, dType.DeploymentType_RUN, layer.Layer_TEST)
	res <- makeState(1, 0, 0, dState.DeploymentState_CANCEL, dType.DeploymentType_RUN, layer.Layer_TEST)
	res <- makeState(1, 1, 0, dState.DeploymentState_CANCEL, dType.DeploymentType_RUN, layer.Layer_TEST)
	res <- makeState(1, 1, 0, dState.DeploymentState_CANCEL, dType.DeploymentType_RUN, layer.Layer_TEST)
	res <- makeState(1, 1, 1, dState.DeploymentState_CANCELED, dType.DeploymentType_RUN, layer.Layer_TEST)
	close(res)
	initMSg := smock.NewInputMessageMock("/run -l test -v 0.0.1 my_service", tgLogin, chatID)
	m.IN <- initMSg
	actual := m.Read(t, 8)
	assert.Equal(t, "Run for `my_service:0.0.1` on Test", actual[0].Message)
	assert.Equal(t, s.msgS.BacktickHack(s.msgS.BacktickHack(`Command: Run
Service: ¬my_service¬
Layer: Test
Version: ¬0.0.1¬
Initiator: [danevge](https://staff.yandex-team.ru/danevge)
Time: ¬2020-06-15T01:15¬

State: *progress*
myt: 1 of 1 (health check: 0)
sas: 1 of 1 (health check: 0)`)), actual[1].Message)
	assert.Equal(t, "[alexander-s](https://staff.yandex-team.ru/alexander-s) cancel deploy `my_service:0.0.1`", actual[2].Message)
	assert.Equal(t, "[alexander-s](https://staff.yandex-team.ru/alexander-s) cancel deploy `my_service:0.0.1`", actual[3].Message)
	assert.Equal(t, s.msgS.BacktickHack(`Command: Run
Service: ¬my_service¬
Layer: Test
Version: ¬0.0.2¬
Initiator: [danevge](https://staff.yandex-team.ru/danevge)
Time: ¬2020-06-15T01:15¬

State: *cancel*
myt: 0 of 1 (health check: 0)
sas: 0 of 1 (health check: 0)`), actual[4].Message)
	assert.Equal(t, s.msgS.BacktickHack(`Command: Run
Service: ¬my_service¬
Layer: Test
Version: ¬0.0.2¬
Initiator: [danevge](https://staff.yandex-team.ru/danevge)
Time: ¬2020-06-15T01:15¬

State: *cancel*
myt: 1 of 1 (health check: 0)
sas: 1 of 1 (health check: 0)`), actual[5].Message)
	assert.Equal(t, s.msgS.BacktickHack(`Command: Run
Service: ¬my_service¬
Layer: Test
Version: ¬0.0.2¬
Initiator: [danevge](https://staff.yandex-team.ru/danevge)
Time: ¬2020-06-15T01:15¬

State: *canceled*
myt: 1 of 1 (health check: 1)
sas: 1 of 1 (health check: 1)`), actual[6].Message)
	assert.Equal(t, "Run *canceled* on #Test `my_service:0.0.2`", actual[7].Message)

	// assert tg
	assertInit(t, initMSg, actual)
	assertNew(t, actual[0])
	assertEdit(t, actual[0], actual[1])
	assertEdit(t, actual[1], actual[2])
	assertReplay(t, actual[2], actual[3])
	assertEdit(t, actual[3], actual[4])
	assertEdit(t, actual[4], actual[5])
	assertEdit(t, actual[5], actual[6])
	assertReplay(t, actual[6], actual[7])

}

func TestCancelEvent(t *testing.T) {

	s, m, res, _ := newService(t, layer.Layer_TEST, subscribe.All, true)
	res <- makeState(1, 1, 0, dState.DeploymentState_PREPARE, dType.DeploymentType_CANCEL, layer.Layer_TEST)
	res <- makeState(1, 1, 0, dState.DeploymentState_CANCEL, dType.DeploymentType_CANCEL, layer.Layer_TEST)
	res <- makeState(1, 1, 0, dState.DeploymentState_CANCEL, dType.DeploymentType_CANCEL, layer.Layer_TEST)
	res <- makeState(1, 1, 0, dState.DeploymentState_CANCEL, dType.DeploymentType_CANCEL, layer.Layer_TEST)
	res <- makeState(1, 1, 1, dState.DeploymentState_CANCELED, dType.DeploymentType_CANCEL, layer.Layer_TEST)
	close(res)
	require.NoError(t, s.handleDeploymentEvent(makeEvent(dType.DeploymentType_CANCEL, dState.DeploymentState_IN_PROGRESS)))
	m.Read(t, 0)

}

func TestStopEvent(t *testing.T) {

	s, m, res, _ := newService(t, layer.Layer_TEST, subscribe.All, true)
	res <- makeState(1, 1, 0, dState.DeploymentState_PREPARE, dType.DeploymentType_STOP, layer.Layer_TEST)
	res <- makeState(1, 1, 1, dState.DeploymentState_SUCCESS, dType.DeploymentType_STOP, layer.Layer_TEST)
	close(res)
	require.NoError(t, s.handleDeploymentEvent(makeEvent(dType.DeploymentType_STOP, dState.DeploymentState_IN_PROGRESS)))
	actual := m.Read(t, 1)

	// assert msg
	assert.Equal(t, "Stop *success* on #Test `my_service:0.0.1`", actual[0].Message)

	// assert tg
	assertInit(t, nil, actual)
	assertNew(t, actual[0])

}

func TestRunRevert(t *testing.T) {

	s, m, res, chatID := newService(t, layer.Layer_TEST, subscribe.All, true)
	res <- makeState(1, 1, 0, dState.DeploymentState_PREPARE, dType.DeploymentType_RUN, layer.Layer_TEST)
	res <- makeState(1, 1, 0, dState.DeploymentState_IN_PROGRESS, dType.DeploymentType_RUN, layer.Layer_TEST)
	res <- makeState(1, 1, 0, dState.DeploymentState_IN_PROGRESS, dType.DeploymentType_RUN, layer.Layer_TEST)
	res <- makeState(1, 1, 0, dState.DeploymentState_IN_PROGRESS, dType.DeploymentType_RUN, layer.Layer_TEST)
	res <- addRevertType(makeState(1, 0, 0, dState.DeploymentState_REVERT, dType.DeploymentType_RUN, layer.Layer_TEST), revert.RevertType_Terminate)
	res <- addRevertType(makeState(1, 1, 0, dState.DeploymentState_REVERT, dType.DeploymentType_RUN, layer.Layer_TEST), revert.RevertType_Terminate)
	res <- addRevertType(makeState(1, 1, 0, dState.DeploymentState_REVERT, dType.DeploymentType_RUN, layer.Layer_TEST), revert.RevertType_Terminate)
	res <- addRevertType(makeState(1, 1, 0, dState.DeploymentState_REVERT, dType.DeploymentType_RUN, layer.Layer_TEST), revert.RevertType_Terminate)
	res <- addRevertType(makeState(1, 1, 1, dState.DeploymentState_REVERTED, dType.DeploymentType_RUN, layer.Layer_TEST), revert.RevertType_Terminate)
	close(res)
	initMSg := smock.NewInputMessageMock("/run -l test -v 0.0.1 my_service", tgLogin, chatID)
	m.IN <- initMSg
	actual := m.Read(t, 7)

	// assert msg
	assert.Equal(t, "Run for `my_service:0.0.1` on Test", actual[0].Message)
	assert.Equal(t, s.msgS.BacktickHack(`Command: Run
Service: ¬my_service¬
Layer: Test
Version: ¬0.0.1¬
Initiator: [danevge](https://staff.yandex-team.ru/danevge)
Time: ¬2020-06-15T01:15¬

State: *progress*
myt: 1 of 1 (health check: 0)
sas: 1 of 1 (health check: 0)`), actual[1].Message)
	failMsg := s.msgS.BacktickHack(fmt.Sprintf(`Fail deploy ¬my_service:0.0.1¬.
*Reason:* docker container exited with non-zero code
Logs for your service:
- [111](%s)
- [222](%s)`,
		unescapedLogUrl("service=my_service layer=test allocation_id=111"),
		unescapedLogUrl("service=my_service layer=test allocation_id=222")))
	assert.Equal(t, failMsg, actual[2].Message)
	assert.Equal(t, failMsg, actual[3].Message)
	assert.Equal(t, s.msgS.BacktickHack(`Command: Run
Service: ¬my_service¬
Layer: Test
Version: ¬0.0.2¬
Initiator: [danevge](https://staff.yandex-team.ru/danevge)
Time: ¬2020-06-15T01:15¬

State: *revert*
myt: 1 of 1 (health check: 0)
sas: 1 of 1 (health check: 0)`), actual[4].Message)
	assert.Equal(t, s.msgS.BacktickHack(`Command: Run
Service: ¬my_service¬
Layer: Test
Version: ¬0.0.2¬
Initiator: [danevge](https://staff.yandex-team.ru/danevge)
Time: ¬2020-06-15T01:15¬

State: *reverted*
myt: 1 of 1 (health check: 1)
sas: 1 of 1 (health check: 1)`), actual[5].Message)
	assert.Equal(t, "Run *reverted* on #Test `my_service:0.0.2`", actual[6].Message)

	// assert tg
	assertInit(t, initMSg, actual)
	assertNew(t, actual[0])
	assertEdit(t, actual[0], actual[1])
	assertEdit(t, actual[1], actual[2])
	assertReplay(t, actual[2], actual[3])
	assertEdit(t, actual[3], actual[4])
	assertReplay(t, actual[5], actual[6])

}

func TestRunRevertUnhealthy(t *testing.T) {

	s, m, res, chatID := newService(t, layer.Layer_TEST, subscribe.All, true)
	ps := []*deployment.ProvideStatus{
		{
			Provide: &service_map.ServiceProvides{Name: "monitoring"},
			Status:  true,
		},
		{
			Provide: &service_map.ServiceProvides{Name: "first-provider"},
			Status:  false,
		},
	}
	res <- makeState(1, 1, 0, dState.DeploymentState_PREPARE, dType.DeploymentType_RUN, layer.Layer_TEST)
	res <- makeState(1, 1, 0, dState.DeploymentState_IN_PROGRESS, dType.DeploymentType_RUN, layer.Layer_TEST)
	res <- makeState(1, 1, 0, dState.DeploymentState_IN_PROGRESS, dType.DeploymentType_RUN, layer.Layer_TEST)
	res <- makeState(1, 1, 0, dState.DeploymentState_IN_PROGRESS, dType.DeploymentType_RUN, layer.Layer_TEST)
	res <- unhealthy(makeState(1, 0, 0, dState.DeploymentState_REVERT, dType.DeploymentType_RUN, layer.Layer_TEST), ps)
	res <- unhealthy(makeState(1, 1, 0, dState.DeploymentState_REVERT, dType.DeploymentType_RUN, layer.Layer_TEST), ps)
	res <- unhealthy(makeState(1, 1, 0, dState.DeploymentState_REVERT, dType.DeploymentType_RUN, layer.Layer_TEST), ps)
	res <- unhealthy(makeState(1, 1, 0, dState.DeploymentState_REVERT, dType.DeploymentType_RUN, layer.Layer_TEST), ps)
	res <- unhealthy(makeState(1, 1, 1, dState.DeploymentState_REVERTED, dType.DeploymentType_RUN, layer.Layer_TEST), ps)
	close(res)
	initMSg := smock.NewInputMessageMock("/run -l test -v 0.0.1 my_service", tgLogin, chatID)
	m.IN <- initMSg
	actual := m.Read(t, 7)

	// assert msg
	assert.Equal(t, "Run for `my_service:0.0.1` on Test", actual[0].Message)
	assert.Equal(t, s.msgS.BacktickHack(`Command: Run
Service: ¬my_service¬
Layer: Test
Version: ¬0.0.1¬
Initiator: [danevge](https://staff.yandex-team.ru/danevge)
Time: ¬2020-06-15T01:15¬

State: *progress*
myt: 1 of 1 (health check: 0)
sas: 1 of 1 (health check: 0)`), actual[1].Message)
	failMsg := s.msgS.BacktickHack(fmt.Sprintf(`Fail deploy ¬my_service:0.0.1¬.
*Reason:* some allocs were unhealthy, following checks failed:
- monitoring (port 81), [docs](%s) 
Logs for your service:
- [111](%s)
- [222](%s)`,
		message.DocsMonitoring,
		unescapedLogUrl("service=my_service layer=test allocation_id=111"),
		unescapedLogUrl("service=my_service layer=test allocation_id=222")))
	assert.Equal(t, failMsg, actual[2].Message)
	assert.Equal(t, failMsg, actual[3].Message)
	assert.Equal(t, s.msgS.BacktickHack(`Command: Run
Service: ¬my_service¬
Layer: Test
Version: ¬0.0.2¬
Initiator: [danevge](https://staff.yandex-team.ru/danevge)
Time: ¬2020-06-15T01:15¬

State: *revert*
myt: 1 of 1 (health check: 0)
sas: 1 of 1 (health check: 0)`), actual[4].Message)
	assert.Equal(t, s.msgS.BacktickHack(`Command: Run
Service: ¬my_service¬
Layer: Test
Version: ¬0.0.2¬
Initiator: [danevge](https://staff.yandex-team.ru/danevge)
Time: ¬2020-06-15T01:15¬

State: *reverted*
myt: 1 of 1 (health check: 1)
sas: 1 of 1 (health check: 1)`), actual[5].Message)
	assert.Equal(t, "Run *reverted* on #Test `my_service:0.0.2`", actual[6].Message)

	// assert tg
	assertInit(t, initMSg, actual)
	assertNew(t, actual[0])
	assertEdit(t, actual[0], actual[1])
	assertEdit(t, actual[1], actual[2])
	assertReplay(t, actual[2], actual[3])
	assertEdit(t, actual[3], actual[4])
	assertReplay(t, actual[5], actual[6])

}

func TestRunEvent(t *testing.T) {

	s, m, res, _ := newService(t, layer.Layer_TEST, subscribe.All, true)
	res <- makeState(1, 1, 0, dState.DeploymentState_PREPARE, dType.DeploymentType_RUN, layer.Layer_TEST)
	res <- makeState(1, 1, 0, dState.DeploymentState_IN_PROGRESS, dType.DeploymentType_RUN, layer.Layer_TEST)
	res <- makeState(1, 1, 0, dState.DeploymentState_IN_PROGRESS, dType.DeploymentType_RUN, layer.Layer_TEST)
	res <- makeState(1, 1, 0, dState.DeploymentState_IN_PROGRESS, dType.DeploymentType_RUN, layer.Layer_TEST)
	res <- makeState(1, 1, 1, dState.DeploymentState_SUCCESS, dType.DeploymentType_RUN, layer.Layer_TEST)
	close(res)

	require.NoError(t, s.handleDeploymentEvent(makeEvent(dType.DeploymentType_RUN, dState.DeploymentState_IN_PROGRESS)))
	actual := m.Read(t, 4)

	// assert msg
	assert.Equal(t, "Run for `my_service:0.0.1` on Test", actual[0].Message)
	assert.Equal(t, s.msgS.BacktickHack(`Command: Run
Service: ¬my_service¬
Layer: Test
Version: ¬0.0.1¬
Initiator: [danevge](https://staff.yandex-team.ru/danevge)
Time: ¬2020-06-15T01:15¬

State: *progress*
myt: 1 of 1 (health check: 0)
sas: 1 of 1 (health check: 0)`), actual[1].Message)
	assert.Equal(t, s.msgS.BacktickHack(`Command: Run
Service: ¬my_service¬
Layer: Test
Version: ¬0.0.1¬
Initiator: [danevge](https://staff.yandex-team.ru/danevge)
Time: ¬2020-06-15T01:15¬

State: *success*
myt: 1 of 1 (health check: 1)
sas: 1 of 1 (health check: 1)`), actual[2].Message)
	assert.Equal(t, "Run *success* on #Test `my_service:0.0.1`", actual[3].Message)

	// assert tg
	assertInit(t, nil, actual)
	assertNew(t, actual[0])
	assertEdit(t, actual[0], actual[1])
	assertEdit(t, actual[1], actual[2])
	assertReplay(t, actual[2], actual[3])

}

func TestUpdateEvent(t *testing.T) {
	s, m, res, _ := newService(t, layer.Layer_TEST, subscribe.All, true)
	res <- makeState(1, 1, 0, dState.DeploymentState_PREPARE, dType.DeploymentType_UPDATE, layer.Layer_TEST)
	res <- makeState(1, 1, 0, dState.DeploymentState_IN_PROGRESS, dType.DeploymentType_UPDATE, layer.Layer_TEST)
	res <- makeState(1, 1, 0, dState.DeploymentState_IN_PROGRESS, dType.DeploymentType_UPDATE, layer.Layer_TEST)
	res <- makeState(1, 1, 0, dState.DeploymentState_IN_PROGRESS, dType.DeploymentType_UPDATE, layer.Layer_TEST)
	res <- makeState(1, 1, 1, dState.DeploymentState_SUCCESS, dType.DeploymentType_UPDATE, layer.Layer_TEST)
	close(res)

	require.NoError(t, s.handleDeploymentEvent(makeEvent(dType.DeploymentType_UPDATE, dState.DeploymentState_IN_PROGRESS)))
	actual := m.Read(t, 4)

	// assert msg
	assert.Equal(t, "Update for `my_service:0.0.1` on Test", actual[0].Message)
	assert.Equal(t, s.msgS.BacktickHack(`Command: Update
Service: ¬my_service¬
Layer: Test
Version: ¬0.0.1¬
Initiator: [danevge](https://staff.yandex-team.ru/danevge)
Time: ¬2020-06-15T01:15¬

State: *progress*
myt: 1 of 1 (health check: 0)
sas: 1 of 1 (health check: 0)`), actual[1].Message)
	assert.Equal(t, s.msgS.BacktickHack(`Command: Update
Service: ¬my_service¬
Layer: Test
Version: ¬0.0.1¬
Initiator: [danevge](https://staff.yandex-team.ru/danevge)
Time: ¬2020-06-15T01:15¬

State: *success*
myt: 1 of 1 (health check: 1)
sas: 1 of 1 (health check: 1)`), actual[2].Message)
	assert.Equal(t, "Update *success* on #Test `my_service:0.0.1`", actual[3].Message)

	// assert tg
	assertInit(t, nil, actual)
	assertNew(t, actual[0])
	assertEdit(t, actual[0], actual[1])
	assertEdit(t, actual[1], actual[2])
	assertReplay(t, actual[2], actual[3])

}

func TestCanaryRunSuccessEvent(t *testing.T) {

	s, m, res, _ := newService(t, layer.Layer_PROD, subscribe.All, true)
	res <- makeState(1, 1, 1, dState.DeploymentState_CANARY, dType.DeploymentType_RUN, layer.Layer_PROD)
	close(res)
	event := makeEvent(dType.DeploymentType_RUN, dState.DeploymentState_IN_PROGRESS)
	event.Deployment.Layer = layer.Layer_PROD
	require.NoError(t, s.handleDeploymentEvent(event))
	actual := m.Read(t, 1)

	// assert msg
	assert.Equal(t, "Run *canary with real traffic* on #Prod `my_service:0.0.1`", actual[0].Message)

	// assert tg
	assertNew(t, actual[0])
	assert.Equal(t, "promote", actual[0].Buttons[0].Message)
	assert.Equal(t, "/promote -id 1", actual[0].Buttons[0].Callback)
	assert.Equal(t, "cancel", actual[0].Buttons[1].Message)
	assert.Equal(t, "/cancel -id 1", actual[0].Buttons[1].Callback)

}

func TestCanaryUpdateSuccessEvent(t *testing.T) {

	s, m, res, _ := newService(t, layer.Layer_PROD, subscribe.All, true)
	res <- makeState(1, 1, 1, dState.DeploymentState_CANARY, dType.DeploymentType_UPDATE, layer.Layer_PROD)
	close(res)
	event := makeEvent(dType.DeploymentType_UPDATE, dState.DeploymentState_IN_PROGRESS)
	event.Deployment.Layer = layer.Layer_PROD
	require.NoError(t, s.handleDeploymentEvent(event))
	actual := m.Read(t, 1)

	// assert msg
	assert.Equal(t, "Update *canary with real traffic* on #Prod `my_service:0.0.1`", actual[0].Message)

	// assert tg
	assertNew(t, actual[0])
	assert.Equal(t, "promote", actual[0].Buttons[0].Message)
	assert.Equal(t, "promote", actual[0].Buttons[0].Message)
	assert.Equal(t, "/promote -id 1", actual[0].Buttons[0].Callback)
	assert.Equal(t, "cancel", actual[0].Buttons[1].Message)
	assert.Equal(t, "/cancel -id 1", actual[0].Buttons[1].Callback)

}

func TestRestartEvent(t *testing.T) {
	s, m, res, _ := newService(t, layer.Layer_TEST, subscribe.All, true)
	res <- makeState(1, 1, 0, dState.DeploymentState_PREPARE, dType.DeploymentType_RESTART, layer.Layer_TEST)
	res <- makeState(1, 1, 0, dState.DeploymentState_IN_PROGRESS, dType.DeploymentType_RESTART, layer.Layer_TEST)
	res <- makeState(1, 1, 0, dState.DeploymentState_IN_PROGRESS, dType.DeploymentType_RESTART, layer.Layer_TEST)
	res <- makeState(1, 1, 0, dState.DeploymentState_IN_PROGRESS, dType.DeploymentType_RESTART, layer.Layer_TEST)
	res <- makeState(1, 1, 1, dState.DeploymentState_SUCCESS, dType.DeploymentType_RESTART, layer.Layer_TEST)
	close(res)
	require.NoError(t, s.handleDeploymentEvent(makeEvent(dType.DeploymentType_RUN, dState.DeploymentState_IN_PROGRESS)))
	actual := m.Read(t, 4)

	// assert msg
	assert.Equal(t, "Restart for `my_service:0.0.1` on Test", actual[0].Message)
	assert.Equal(t, s.msgS.BacktickHack(`Command: Restart
Service: ¬my_service¬
Layer: Test
Version: ¬0.0.1¬
Initiator: [danevge](https://staff.yandex-team.ru/danevge)
Time: ¬2020-06-15T01:15¬

State: *progress*
myt: 1 of 1 (health check: 0)
sas: 1 of 1 (health check: 0)`), actual[1].Message)
	assert.Equal(t, s.msgS.BacktickHack(`Command: Restart
Service: ¬my_service¬
Layer: Test
Version: ¬0.0.1¬
Initiator: [danevge](https://staff.yandex-team.ru/danevge)
Time: ¬2020-06-15T01:15¬

State: *success*
myt: 1 of 1 (health check: 1)
sas: 1 of 1 (health check: 1)`), actual[2].Message)
	assert.Equal(t, "Restart *success* on #Test `my_service:0.0.1`", actual[3].Message)

	// assert tg
	assertInit(t, nil, actual)
	assertNew(t, actual[0])
	assertEdit(t, actual[0], actual[1])
	assertEdit(t, actual[1], actual[2])
	assertReplay(t, actual[2], actual[3])

}

func TestSubscriptionsCommand_Batch(t *testing.T) {
	_, tg, _, chatId := newService(t, layer.Layer_TEST, subscribe.All, false)
	name := "test_batch_"
	newSubscribes := []*subscribe.Subscribe{
		subscribe.NewSubscribe(name+"a", chatId, layer.Layer_PROD, false, subscribe.Fail, false),
		subscribe.NewSubscribe(name+"c", chatId, layer.Layer_PROD, false, subscribe.All, false),
		subscribe.NewSubscribe(name+"b", chatId, layer.Layer_PROD, false, subscribe.End, false),
	}
	smMock := &smock.ServiceMapsClient{}
	service := subscribe.NewService(test_db.NewDb(t), smMock, test.NewLogger(t))
	for _, s := range newSubscribes {
		smMock.On("Get", mock.Anything, mock.Anything).Return(
			&apiSm.ServiceData{Service: &service_map.ServiceMap{Type: service_map.ServiceType_batch}}, nil)
		require.NoError(t, service.Subscribe(s.Name, s.ChatID, layer.FromCommonLayer(s.Layer), s.Branch, s.StateGroup, false))
	}

	msg := smock.NewInputMessageMock("/subs", tgLogin, chatId)
	tg.IN <- msg
	response := tg.Read(t, 1)
	expected := `Subscriptions:
	- my_service (layer: Test, state: All)
	- test_batch_a (layer: Prod, state: Fail)
	- test_batch_b (layer: Prod, state: End)
	- test_batch_c (layer: Prod, state: All)`
	assert.Equal(t, expected, response[0].Message)
}

func TestShivaCallFail(t *testing.T) {
	db := test_db.NewDb(t)
	logger := test.NewLogger(t)
	tgMock := smock.NewTelegramMock(t)

	shivaCli := &mocks.DeployServiceClient{}
	shivaCli.On("Run", mock.Anything, mock.Anything).
		Return(nil, status.Error(codes.Unavailable, "no healthy upstream"))

	shivaApi := scheduler.NewClient(shivaCli, logger)
	staffApi := staffapi.NewApi(staffapi.NewConf(), logger)
	staffService := staff.NewService(db, staffApi, logger)
	RunService(
		NewConf(),
		election.NewElectionStub(),
		logger,
		staffService,
		command.NewService(nil, shivaApi),
		nil,
		shivaApi,
		tgMock,
		message.NewService(test.NewLogger(t), &NdaMock{}, message.NewMarkdown()),
		nil,
		newKV(t),
		newKV(t),
	)

	tgMock.IN <- smock.NewInputMessageMock("/run -l test -v 0.0.1 my_service", tgLogin, makeChatID())
	msgs := tgMock.Read(t, 1)
	require.Equal(t, "Message: `Command fail\nError: rpc error: code = Unavailable desc = no healthy upstream`", msgs[0].Message)
}

func TestShivaCallFailWithDetails(t *testing.T) {
	db := test_db.NewDb(t)
	logger := test.NewLogger(t)
	tgMock := smock.NewTelegramMock(t)

	userError1 := user_error.NewUserError(fmt.Errorf("some error 1"), "Ошибка 1")
	userError2 := user_error.NewUserError(fmt.Errorf("some error 2"), "Ошибка 2")
	st := status.New(codes.InvalidArgument, userError1.Error())
	st, err := st.WithDetails(userError1.ToProto(), userError2.ToProto())
	require.NoError(t, err)

	shivaCli := &mocks.DeployServiceClient{}
	shivaCli.On("Run", mock.Anything, mock.Anything).
		Return(nil, st.Err())

	shivaApi := scheduler.NewClient(shivaCli, logger)
	staffApi := staffapi.NewApi(staffapi.NewConf(), logger)
	staffService := staff.NewService(db, staffApi, logger)
	RunService(
		NewConf(),
		election.NewElectionStub(),
		logger,
		staffService,
		command.NewService(nil, shivaApi),
		nil,
		shivaApi,
		tgMock,
		message.NewService(test.NewLogger(t), &NdaMock{}, message.NewMarkdown()),
		nil,
		newKV(t),
		newKV(t),
	)

	tgMock.IN <- smock.NewInputMessageMock("/run -l test -v 0.0.1 my_service", tgLogin, makeChatID())
	msgs := tgMock.Read(t, 1)
	require.Equal(t, "\nError: `Ошибка 1`\nError: `Ошибка 2`", msgs[0].Message)
}

func TestMigrateMessage(t *testing.T) {
	s, m, _, fromChatID := newService(t, layer.Layer_TEST, subscribe.All, true)
	toChatID := makeChatID()

	initMSg := smock.NewMigrateInputMessageMock(tgLogin, fromChatID, toChatID)
	m.IN <- initMSg

	m.Read(t, 0)
	assert.Equal(t, 0, m.SendCount)

	subs, err := s.subscribeS.Subscriptions(fromChatID)
	require.NoError(t, err)
	assert.Len(t, subs, 0)

	subs, err = s.subscribeS.Subscriptions(toChatID)
	require.NoError(t, err)
	assert.Len(t, subs, 1)

}

func TestRunWithRetryEndMessage(t *testing.T) {
	s, m, res, chatID := newService(t, layer.Layer_TEST, subscribe.All, true)

	initMSg := smock.NewInputMessageMock("/run -l test -v 0.0.1 my_service", tgLogin, chatID)
	m.IN <- initMSg

	res <- makeState(1, 1, 0, dState.DeploymentState_IN_PROGRESS, dType.DeploymentType_RUN, layer.Layer_TEST)
	assert.Equal(t, "Run for `my_service:0.0.1` on Test", m.Read(t, 1)[0].Message)
	assert.Equal(t, 1, m.SendCount)

	// should skip not last state message if error
	m.Error = errors.New("some error")
	res <- makeState(1, 1, 0, dState.DeploymentState_IN_PROGRESS, dType.DeploymentType_RUN, layer.Layer_TEST)
	m.Read(t, 0)
	assert.Equal(t, 2, m.SendCount)

	// should not skip last state message if error
	m.Error = errors.New("some error")
	res <- makeState(1, 1, 1, dState.DeploymentState_SUCCESS, dType.DeploymentType_RUN, layer.Layer_TEST)
	msgs := m.Read(t, 2)
	assert.Equal(t, s.msgS.BacktickHack(`Command: Run
Service: ¬my_service¬
Layer: Test
Version: ¬0.0.1¬
Initiator: [danevge](https://staff.yandex-team.ru/danevge)
Time: ¬2020-06-15T01:15¬

State: *success*
myt: 1 of 1 (health check: 1)
sas: 1 of 1 (health check: 1)`), msgs[0].Message)
	assert.Equal(t, "Run *success* on #Test `my_service:0.0.1`", msgs[1].Message)
	assert.Equal(t, 5, m.SendCount)

	close(res)
}

func TestStreamFail(t *testing.T) {
	t.Skip("Should fixed after https://st.yandex-team.ru/VOID-653")

	db := test_db.NewDb(t)
	logger := test.NewLogger(t)
	tgMock := smock.NewTelegramMock(t)

	responseStream := &deployMocks.DeployService_RunClient{}
	responseStream.On("Recv").
		Return(nil, status.Error(codes.Unavailable, "no healthy upstream"))
	shivaCli := &mocks.DeployServiceClient{}
	shivaCli.On("Run", mock.Anything, mock.Anything).Return(responseStream, nil)

	shivaApi := scheduler.NewClient(shivaCli, logger)
	staffApi := staffapi.NewApi(staffapi.NewConf(), logger)
	staffService := staff.NewService(db, staffApi, logger)
	RunService(
		NewConf(),
		election.NewElectionStub(),
		logger,
		staffService,
		command.NewService(nil, shivaApi),
		nil,
		shivaApi,
		tgMock,
		message.NewService(test.NewLogger(t), &NdaMock{}, message.NewMarkdown()),
		nil,
		newKV(t),
		newKV(t))

	tgMock.IN <- smock.NewInputMessageMock("/run -l test -v 0.0.1 my_service", tgLogin, makeChatID())
	msgs := tgMock.Read(t, 1)
	require.Equal(t, "Message: `Command fail\nError: rpc error: code = Unavailable desc = no healthy upstream`", msgs[0].Message)
}

func TestApproveListCommand(t *testing.T) {
	test.InitTestEnv()
	msgS := message.NewService(test.NewLogger(t), &NdaMock{}, message.NewMarkdown())
	contexts := []*deployment.Deployment{
		{
			ServiceName: "my-service",
			Type:        dType.DeploymentType_RUN,
			Layer:       layer.Layer_PROD,
			Version:     "v0.4.2",
			User:        "my-user",
			Issues:      []string{"VOID-42", "VOID-43"},
		},
		{
			ServiceName: "my-service2",
			Type:        dType.DeploymentType_PROMOTE,
			Layer:       layer.Layer_PROD,
			Version:     "v0.4.3",
			User:        "my-user2",
			Issues:      []string{"VOID-44"}},
	}

	type TestCase struct {
		name               string
		deployments        []*deployment.Deployment
		expectedMsg        string
		expectedButtonMsgs []string
	}

	tcs := []TestCase{
		{
			name:               "EmptyList",
			deployments:        nil,
			expectedMsg:        "Empty",
			expectedButtonMsgs: nil,
		},
		{
			name:        "OneService",
			deployments: contexts[0:1],
			expectedMsg: msgS.BacktickHack(`Command: Run
Service: ¬my-service¬
Layer: Prod
Version: ¬v0.4.2¬
Initiator: [my-user](https://staff.yandex-team.ru/my-user)
Issues: [VOID-42](https://st.yandex-team.ru/VOID-42), [VOID-43](https://st.yandex-team.ru/VOID-43)`),
			expectedButtonMsgs: []string{"Approve"},
		},
		{
			name:        "TwoServices",
			deployments: contexts,
			expectedMsg: msgS.BacktickHack(`Command: Run
Service: ¬my-service¬
Layer: Prod
Version: ¬v0.4.2¬
Initiator: [my-user](https://staff.yandex-team.ru/my-user)
Issues: [VOID-42](https://st.yandex-team.ru/VOID-42), [VOID-43](https://st.yandex-team.ru/VOID-43)

Command: Promote
Service: ¬my-service2¬
Layer: Prod
Version: ¬v0.4.3¬
Initiator: [my-user2](https://staff.yandex-team.ru/my-user2)
Issues: [VOID-44](https://st.yandex-team.ru/VOID-44)`) + "\n\n",
			expectedButtonMsgs: []string{"Approve my-service:v0.4.2", "Approve my-service2:v0.4.3"},
		},
	}

	for _, c := range tcs {
		t.Run(c.name, func(t *testing.T) {
			db := test_db.NewDb(t)
			logger := test.NewLogger(t)
			tgMock := smock.NewTelegramMock(t)

			response := &deploy2.ApproveListResponse{Deployment: c.deployments}
			shivaCli := &mocks.DeployServiceClient{}
			shivaCli.On("ApproveList", mock.Anything, mock.Anything).
				Return(response, nil)
			shivaApi := scheduler.NewClient(shivaCli, logger)
			staffApi := staffapi.NewApi(staffapi.NewConf(), logger)
			staffService := staff.NewService(db, staffApi, logger)
			RunService(
				NewConf(),
				election.NewElectionStub(),
				logger,
				staffService,
				command.NewService(nil, shivaApi),
				nil,
				shivaApi,
				tgMock,
				msgS,
				nil,
				newKV(t),
				newKV(t),
			)

			tgMock.IN <- smock.NewInputMessageMock("/approve_list", tgLogin, makeChatID())
			tgMock.Error = errors.New("some error")
			outMsgs := tgMock.Read(t, 1)

			assert.Equal(t, c.expectedMsg, outMsgs[0].Message)
			for i, button := range outMsgs[0].Buttons {
				assert.Equal(t, c.expectedButtonMsgs[i], button.Message)
			}
		})
	}
}

func TestSubscriptionsCommand(t *testing.T) {
	_, tg, _, chatId := newService(t, layer.Layer_TEST, subscribe.All, false)
	name := "test_svc_"
	newSubscribes := []*subscribe.Subscribe{
		subscribe.NewSubscribe(name+"d", chatId, layer.Layer_TEST, false, subscribe.All, true),
		subscribe.NewSubscribe(name+"c", chatId, layer.Layer_PROD, true, subscribe.All, false),
		subscribe.NewSubscribe(name+"c", chatId, layer.Layer_TEST, true, subscribe.All, true),
		subscribe.NewSubscribe(name+"b", chatId, layer.Layer_TEST, false, subscribe.All, false),
		subscribe.NewSubscribe(name+"b", chatId, layer.Layer_PROD, false, subscribe.All, false),
		subscribe.NewSubscribe(name+"a", chatId, layer.Layer_TEST, false, subscribe.All, false),
		subscribe.NewSubscribe(name+"a", chatId, layer.Layer_PROD, false, subscribe.All, false),
	}
	smMock := &smock.ServiceMapsClient{}
	service := subscribe.NewService(test_db.NewDb(t), smMock, test.NewLogger(t))
	for _, s := range newSubscribes {
		smMock.On("Get", mock.Anything, mock.Anything).Return(
			&apiSm.ServiceData{Service: &service_map.ServiceMap{Type: service_map.ServiceType_service}}, nil)
		require.NoError(t, service.Subscribe(
			s.Name, s.ChatID,
			layer.FromCommonLayer(s.Layer),
			s.Branch,
			s.StateGroup,
			s.DeploymentTypes.Exist(dType.DeploymentType_UPDATE)))
	}

	msg := smock.NewInputMessageMock("/subs", tgLogin, chatId)
	tg.IN <- msg
	tg.Error = errors.New("some error")

	outMsgs := tg.Read(t, 1)
	expected := `Subscriptions:
	- my_service (layer: Test, state: All)
	- test_svc_a (layer: Prod, state: All)
	- test_svc_a (layer: Test, state: All)
	- test_svc_b (layer: Prod, state: All)
	- test_svc_b (layer: Test, state: All)
	- test_svc_c (layer: Prod, state: All) with branch.
	- test_svc_c (layer: Test, state: All) with branch. with Update type
	- test_svc_d (layer: Test, state: All) with Update type`
	assert.Equal(t, expected, outMsgs[0].Message)

}

func TestSubscribeCommandSuccess(t *testing.T) {
	db := test_db.NewDb(t)
	logger := test.NewLogger(t)

	smMock := &smock.ServiceMapsClient{}

	smMock.On("Get", mock.Anything, mock.Anything).Return(
		&apiSm.ServiceData{Service: &service_map.ServiceMap{Type: service_map.ServiceType_service}}, nil)

	service := command.NewService(subscribe.NewService(db, smMock, logger), nil)
	commandName, data, err := service.Run(makeChatID(), tgLogin, "/sub -l test shiva", dUUID)

	require.NoError(t, err)
	require.Equal(t, "sub", commandName)
	require.Equal(t, "success", data.(string))
}

func TestSubscribeCommandFail(t *testing.T) {
	db := test_db.NewDb(t)
	logger := test.NewLogger(t)

	smMock := &smock.ServiceMapsClient{}
	smMock.On("Get", mock.Anything, mock.Anything).Return(nil, status.Error(codes.NotFound, ""))

	service := command.NewService(subscribe.NewService(db, smMock, logger), nil)
	commandName, data, err := service.Run(makeChatID(), tgLogin, "/sub -l test noService", dUUID)

	require.Error(t, err)
	userError := &user_error.UserError{}
	require.True(t, errors.As(err, &userError))
	require.Equal(t, "Сервис noService не найден", userError.RusMessage)
	assert.Equal(t, "", commandName)
	assert.Equal(t, nil, data)
}

func TestSystemStatusCommand(t *testing.T) {
	_, tg, _, chatID := newService(t, layer.Layer_TEST, subscribe.All, true)
	initMSg := smock.NewInputMessageMock("/system_status", tgLogin, chatID)

	tg.IN <- initMSg
	tg.Error = errors.New("some error")

	outMsgs := tg.Read(t, 1)
	require.Equal(t, "Все датацентры доступны", outMsgs[0].Message)
}

func TestServiceStatusCommand(t *testing.T) {
	s, tg, _, chatID := newService(t, layer.Layer_TEST, subscribe.All, true)
	initMSg := smock.NewInputMessageMock("/status my_service", tgLogin, chatID)

	tg.IN <- initMSg
	tg.Error = errors.New("some error")

	outMsgs := tg.Read(t, 1)
	assert.Equal(t, s.msgS.BacktickHack(`Status for service *my_service*
*test*
-  v. ¬0.0.1¬ by [robot-vertis-shiva](https://staff.yandex-team.ru/robot-vertis-shiva) ([map](https://www.example.com/shiva.yml) [manifest](https://www.example.com/shiva.yml))

`), outMsgs[0].Message)
}

func TestHelpCommand(t *testing.T) {
	s, tg, _, chatID := newService(t, layer.Layer_TEST, subscribe.All, true)
	initMSg := smock.NewInputMessageMock("/help", tgLogin, chatID)

	tg.IN <- initMSg
	tg.Error = errors.New("some error")

	actualMsgs := tg.Read(t, 1)
	assert.Equal(t, s.msgS.BacktickHack(`/run -l test -v 0.1.7 service_name
/restart -l test service_name
/revert -l test service_name
/stop -l test service_name
/status service_name
/sub -l test service_name
/unsub -l test service_name
/subs
/system_status

More: `+links.TelegramBot+`
Full docs: `+links.BaseDomain), actualMsgs[0].Message)
}

func TestCommandUnknown(t *testing.T) {
	s, tg, _, chatID := newService(t, layer.Layer_TEST, subscribe.All, true)

	initMSg := smock.NewInputMessageMock("/hello", tgLogin, chatID)
	tg.IN <- initMSg
	tg.Error = errors.New("some error")

	outMsgs := tg.Read(t, 1)
	assert.Equal(t, s.msgS.BacktickHack(`
Error: ¬Команда '/hello' не найдена¬
Docs:
 • `+links.TelegramBot), outMsgs[0].Message)
}

func newService(t *testing.T, layer layer.Layer, sg subscribe.StateGroup, update bool) (*Service, *smock.TelegramMock, chan *deploy2.StateResponse, int64) {
	var err error
	test.RunUp(t)
	chatId := makeChatID()
	result := make(chan *deploy2.StateResponse, 25)
	log := test.NewLogger(t)
	db := test_db.NewDb(t)
	staffApi := staffapi.NewApi(staffapi.NewConf(), log)
	staffService := staff.NewService(db, staffApi, log)
	test.Wait(t, func() error {
		_, err = staffService.GetByTelegram(tgLogin)
		return err
	})
	tgMock := smock.NewTelegramMock(t)
	apiMock := smock.NewShivaApiMock(result)
	smMock := &smock.ServiceMapsClient{}
	smMock.On("Get", mock.Anything, mock.Anything).Return(
		&apiSm.ServiceData{Service: &service_map.ServiceMap{Type: service_map.ServiceType_service}}, nil)
	subSrv := subscribe.NewService(db, smMock, log)
	require.NoError(t, subSrv.Subscribe(serviceName, chatId, layer, false, sg, update))
	service := RunService(
		NewConf(),
		election.NewElectionStub(),
		log,
		staffService,
		command.NewService(subSrv, apiMock),
		subSrv,
		apiMock,
		tgMock,
		message.NewService(test.NewLogger(t), &NdaMock{}, message.NewMarkdown()),
		nil,
		newKV(t),
		newKV(t),
	)

	return service, tgMock, result, chatId
}

func makeState(total, placed, success int64, st dState.DeploymentState, dt dType.DeploymentType, l layer.Layer) *deploy2.StateResponse {

	return &deploy2.StateResponse{
		Service: &info.DeploymentInfo{
			Deployment: &deployment.Deployment{
				State:       st,
				Id:          "1",
				ServiceName: "my_service",
				Version:     "0.0.1",
				User:        login,
				Type:        dt,
				Layer:       l,
				Start:       start,
				Status: &deployment.Status{
					Numbers: []*deployment.Number{
						{
							Dc:            "sas",
							Total:         total,
							Placed:        placed,
							SuccessPlaced: success,
						},
						{
							Dc:            "myt",
							Total:         total,
							Placed:        placed,
							SuccessPlaced: success,
						},
					},
					Provides: []*deployment.ProvideStatus{
						{
							Provide: consul.MonitoringProvide,
							Status:  true,
						},
					},
				},
			}},
		RevertVersion: "0.0.2",
		CancelUser:    cancelLogin,
	}
}

func unhealthy(r *deploy2.StateResponse, ps []*deployment.ProvideStatus) *deploy2.StateResponse {
	d := r.GetService().GetDeployment()
	d.Status.RevertType = revert.RevertType_Unhealthy
	d.Status.Provides = ps
	d.Status.FailAllocations = []string{"111", "222"}
	d.Status.Provides = []*deployment.ProvideStatus{
		{
			Provide: consul.MonitoringProvide,
			Status:  false,
		},
	}
	return r
}

func addRevertType(r *deploy2.StateResponse, rt revert.RevertType) *deploy2.StateResponse {
	d := r.GetService().GetDeployment()
	d.Status.RevertType = rt
	d.Status.FailAllocations = []string{"111", "222"}
	return r
}

func makeEvent(t dType.DeploymentType, state dState.DeploymentState) *event2.Event {
	return &event2.Event{
		Deployment: &deployment.Deployment{
			Id:          "1",
			ServiceName: "my_service",
			Version:     "0.0.1",
			User:        login,
			Type:        t,
			Layer:       layer.Layer_TEST,
			State:       state,
			Status:      &deployment.Status{},
			RevertType:  revert.RevertType_None,
		},
	}
}

func assertReplay(t *testing.T, m1 *bot.OutputMessage, m2 *bot.OutputMessage) {
	assert.Equal(t, m1, m2.Parent)
	assert.True(t, m2.IsNew)
	assert.NotEqual(t, m1.ID(), m2.ID())
}

func assertEdit(t *testing.T, m1 *bot.OutputMessage, m2 *bot.OutputMessage) {
	assert.Equal(t, m1, m2.Parent)
	assert.False(t, m2.IsNew)
	assert.Equal(t, m1.ID(), m2.ID())
}

func assertNew(t *testing.T, m *bot.OutputMessage) {

	assert.True(t, m.ID() != 0)
	assert.True(t, m.IsNew)
}

func assertInit(t *testing.T, init bot.InputMessage, ms []*bot.OutputMessage) {
	for _, m := range ms {
		assert.True(t, init == m.Init)
	}
}

func makeChatID() int64 {
	return r.Int63()
}

func unescapedLogUrl(expr string) string {
	return "https://grafana.vertis.yandex-team.ru/explore?orgId=1&left=[\"1592172630000\",\"1592174730000\",\"vertis-logs\",{\"expr\":\"" +
		expr +
		"\",\"fields\":[\"thread\",\"context\",\"message\",\"rest\"],\"limit\":500},{\"ui\":[true,true,true,\"none\"]}]"
}

type NdaMock struct {
}

func (n *NdaMock) NdaUrl(url string) (string, error) {
	return url, nil
}
