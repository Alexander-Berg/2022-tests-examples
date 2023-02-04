package annotation

import (
	"fmt"
	"math/rand"
	"strconv"
	"strings"
	"testing"
	"time"

	btePb "github.com/YandexClassifieds/shiva/pb/shiva/events/batch_task"
	"github.com/YandexClassifieds/shiva/pb/shiva/events/event2"
	"github.com/YandexClassifieds/shiva/pb/shiva/events/grafana"
	smProto "github.com/YandexClassifieds/shiva/pb/shiva/service_map"
	btPb "github.com/YandexClassifieds/shiva/pb/shiva/types/batch_task"
	"github.com/YandexClassifieds/shiva/pb/shiva/types/deployment"
	"github.com/YandexClassifieds/shiva/pb/shiva/types/dtype"
	layerPb "github.com/YandexClassifieds/shiva/pb/shiva/types/layer"
	"github.com/YandexClassifieds/shiva/pb/shiva/types/state"
	"github.com/YandexClassifieds/shiva/pkg/mq"
	"github.com/YandexClassifieds/shiva/pkg/service_map"
	"github.com/YandexClassifieds/shiva/test"
	smock "github.com/YandexClassifieds/shiva/test/mock"
	mqMock "github.com/YandexClassifieds/shiva/test/mock/mq"
	"github.com/YandexClassifieds/shiva/test/mock/service_change"
	"github.com/YandexClassifieds/shiva/test/test_db"
	"github.com/google/uuid"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/mock"
	"github.com/stretchr/testify/require"
	"google.golang.org/protobuf/types/known/timestamppb"
)

var (
	version = "0.0.1"
	name    = "service_name"
	r       *rand.Rand
)

const sMap = `
name: %s
depends_on:
  - service: %s
`

func init() {
	r = rand.New(rand.NewSource(time.Now().UnixNano()))
}

type TestCase struct {
	name string
	// test
	dType            dtype.DeploymentType
	prepare          state.DeploymentState
	test             state.DeploymentState
	branch           string
	dependentService string
	// asserts
	startState state.DeploymentState
	endState   state.DeploymentState
	tags       []string
	msg        string
	state      State
	isShort    bool
}

func TestCanaryPipeline(t *testing.T) {
	test.InitTestEnv()
	db := test_db.NewSeparatedDb(t)

	apiMock := &smock.IAnnotation{}
	sMapSvc := service_map.NewService(db, test.NewLogger(t), nil)
	service := NewService(db, test.NewLogger(t), NewConf(), apiMock, sMapSvc)
	issued := time.Date(2021, 1, 1, 1, 1, 1, 25*int(time.Millisecond), time.UTC)

	msg := fmt.Sprintf("RUN %s %s to canary is in progress", name, version)
	apiMock.On("Start", msg, mock.Anything, issued).Return(int64(1), nil)
	e := makeEvent2(name, version, dtype.DeploymentType_RUN, state.DeploymentState_CANARY_PROGRESS, issued)
	grafanaEvent := &grafana.GrafanaEvent{DeploymentEvent: e, DependentService: ""}
	require.NoError(t, service.handleGrafanaEvent(makeMqMsg(t, grafanaEvent)))

	msg = fmt.Sprintf("RUN %s %s to canary with 1%% traffic", name, version)
	apiMock.On("Stop", msg, mock.Anything, issued.Add(time.Minute), int64(1)).Return(nil)
	e.Deployment.State = state.DeploymentState_CANARY_ONE_PERCENT
	e.EventCreated = timestamppb.New(issued.Add(time.Minute))
	require.NoError(t, service.handleGrafanaEvent(makeMqMsg(t, grafanaEvent)))

	msg = fmt.Sprintf("%s %s - canary with real traffic share", name, version)
	apiMock.On("Point", msg, mock.Anything, issued.Add(2*time.Minute)).Return(int64(2), nil)
	e.Deployment.State = state.DeploymentState_CANARY
	e.EventCreated = timestamppb.New(issued.Add(2 * time.Minute))
	require.NoError(t, service.handleGrafanaEvent(makeMqMsg(t, grafanaEvent)))

	msg = fmt.Sprintf("RUN %s %s is in progress", name, version)
	e.Deployment.State = state.DeploymentState_IN_PROGRESS
	e.EventCreated = timestamppb.New(issued.Add(3 * time.Minute))
	apiMock.On("Start", msg, mock.Anything, issued.Add(3*time.Minute)).Return(int64(3), nil)
	require.NoError(t, service.handleGrafanaEvent(makeMqMsg(t, grafanaEvent)))

	msg = fmt.Sprintf("%s %s - canary stopped", name, version)
	apiMock.On("Point", msg, mock.Anything, issued.Add(4*time.Minute)).Return(int64(4), nil)
	e.Deployment.State = state.DeploymentState_CANARY_STOPPED
	e.EventCreated = timestamppb.New(issued.Add(4 * time.Minute))
	require.NoError(t, service.handleGrafanaEvent(makeMqMsg(t, grafanaEvent)))

	msg = fmt.Sprintf("RUN %s %s", name, version)
	e.Deployment.State = state.DeploymentState_SUCCESS
	e.EventCreated = timestamppb.New(issued.Add(4 * time.Minute))
	apiMock.On("Stop", msg, mock.Anything, issued.Add(4*time.Minute), int64(3)).Return(nil)
	require.NoError(t, service.handleGrafanaEvent(makeMqMsg(t, grafanaEvent)))

	apiMock.AssertExpectations(t)
}

func TestSkipEventValidateFailed(t *testing.T) {
	test.InitTestEnv()
	db := test_db.NewSeparatedDb(t)

	apiMock := &smock.IAnnotation{}
	pMock := mqMock.NewProducerMock()
	sMapSvc := service_map.NewService(db, test.NewLogger(t), nil)
	service := NewService(db, test.NewLogger(t), NewConf(), apiMock, sMapSvc)
	service.producer = pMock
	issued := time.Date(2021, 1, 1, 1, 1, 1, 25*int(time.Millisecond), time.UTC)

	e := makeEvent2(name, version, dtype.DeploymentType_RUN, state.DeploymentState_VALIDATE_FAILED, issued)
	require.NoError(t, service.handleEvent2(e))

	require.Equal(t, 0, len(pMock.Msg))
}

func TestProcess(t *testing.T) {
	test.InitTestEnv()
	db := test_db.NewSeparatedDb(t)

	cs := []TestCase{
		{
			name:       "IN_PROGRESS_with_branch",
			test:       state.DeploymentState_IN_PROGRESS,
			startState: state.DeploymentState_IN_PROGRESS,
			endState:   state.DeploymentState_UNKNOWN,
			branch:     "my_branch",
			tags:       []string{name, "prod", "Prometheus", "branch", "in_progress"},
			msg:        fmt.Sprintf("RUN %s %s (branch my_branch) is in progress", name, version),
			state:      Started,
		},
		{
			name:       "IN_PROGRESS",
			test:       state.DeploymentState_IN_PROGRESS,
			startState: state.DeploymentState_IN_PROGRESS,
			endState:   state.DeploymentState_UNKNOWN,
			tags:       []string{name, "prod", "Prometheus", "in_progress"},
			msg:        fmt.Sprintf("RUN %s %s is in progress", name, version),
			state:      Started,
		},
		{
			name:       "IN_PROGRESS_to_SUCCESS",
			prepare:    state.DeploymentState_IN_PROGRESS,
			test:       state.DeploymentState_SUCCESS,
			startState: state.DeploymentState_IN_PROGRESS,
			endState:   state.DeploymentState_SUCCESS,
			tags:       []string{name, "prod", "Prometheus", "success"},
			msg:        "RUN service_name 0.0.1",
			state:      Finished,
		},
		{
			name:       "SUCCESS",
			test:       state.DeploymentState_SUCCESS,
			startState: state.DeploymentState_IN_PROGRESS,
			endState:   state.DeploymentState_SUCCESS,
			tags:       []string{name, "prod", "Prometheus", "success"},
			msg:        "RUN service_name 0.0.1",
			state:      Finished,
		},
		{
			dType:      dtype.DeploymentType_PROMOTE,
			name:       "SUCCESS_PROMOTE",
			test:       state.DeploymentState_SUCCESS,
			startState: state.DeploymentState_IN_PROGRESS,
			endState:   state.DeploymentState_SUCCESS,
			tags:       []string{name, "prod", "Prometheus", "success"},
			msg:        "PROMOTE service_name 0.0.1",
			state:      Finished,
		},
		{
			dType:      dtype.DeploymentType_RESTART,
			name:       "SUCCESS_RESTART",
			test:       state.DeploymentState_SUCCESS,
			startState: state.DeploymentState_IN_PROGRESS,
			endState:   state.DeploymentState_SUCCESS,
			tags:       []string{name, "prod", "Prometheus", "success"},
			msg:        "RESTART service_name 0.0.1",
			state:      Finished,
		},
		{
			dType:      dtype.DeploymentType_STOP,
			name:       "SUCCESS_STOP",
			test:       state.DeploymentState_SUCCESS,
			startState: state.DeploymentState_IN_PROGRESS,
			endState:   state.DeploymentState_SUCCESS,
			tags:       []string{name, "prod", "Prometheus", "success"},
			msg:        "STOP service_name 0.0.1",
			state:      Finished,
		},
		{
			dType:      dtype.DeploymentType_REVERT,
			name:       "SUCCESS_REVERT",
			test:       state.DeploymentState_SUCCESS,
			startState: state.DeploymentState_IN_PROGRESS,
			endState:   state.DeploymentState_SUCCESS,
			tags:       []string{name, "prod", "Prometheus", "success"},
			msg:        "REVERT service_name 0.0.1",
			state:      Finished,
		},
		{
			name:       "IN_PROGRESS_to_CANCELED",
			prepare:    state.DeploymentState_IN_PROGRESS,
			test:       state.DeploymentState_CANCELED,
			startState: state.DeploymentState_IN_PROGRESS,
			endState:   state.DeploymentState_CANCELED,
			tags:       []string{name, "prod", "Prometheus", "canceled"},
			msg:        "RUN service_name 0.0.1",
			state:      Finished,
		},
		{
			name:       "CANARY_PROGRESS",
			test:       state.DeploymentState_CANARY_PROGRESS,
			startState: state.DeploymentState_CANARY_PROGRESS,
			endState:   state.DeploymentState_UNKNOWN,
			tags:       []string{name, "prod", "Prometheus", "canary", "canary_progress"},
			msg:        fmt.Sprintf("RUN %s %s to canary is in progress", name, version),
			state:      Started,
		},
		{
			name:       "CANARY_PROGRESS_to_CANARY_FAILED",
			prepare:    state.DeploymentState_CANARY_PROGRESS,
			test:       state.DeploymentState_CANARY_FAILED,
			startState: state.DeploymentState_CANARY_PROGRESS,
			endState:   state.DeploymentState_CANARY_FAILED,
			tags:       []string{name, "prod", "Prometheus", "canary_failed", "canary"},
			msg:        "RUN service_name 0.0.1 to canary",
			state:      Finished,
		},
		{
			name:       "CANARY_PROGRESS_to_CANARY_CANCELED",
			prepare:    state.DeploymentState_CANARY_PROGRESS,
			test:       state.DeploymentState_CANARY_CANCELED,
			startState: state.DeploymentState_CANARY_PROGRESS,
			endState:   state.DeploymentState_CANARY_CANCELED,
			tags:       []string{name, "prod", "Prometheus", "canary", "canary_canceled"},
			msg:        fmt.Sprintf("RUN %s %s to canary", name, version),
			state:      Finished,
		},
		{
			name:       "CANARY_PROGRESS_to_CANARY_ONE_PERCENT",
			prepare:    state.DeploymentState_CANARY_PROGRESS,
			test:       state.DeploymentState_CANARY_ONE_PERCENT,
			startState: state.DeploymentState_CANARY_PROGRESS,
			endState:   state.DeploymentState_CANARY_ONE_PERCENT,
			tags:       []string{name, "prod", "Prometheus", "canary", "canary_one_percent"},
			msg:        fmt.Sprintf("RUN %s %s to canary with 1%% traffic", name, version),
			state:      Finished,
		},
		{
			name:       "CANARY_ONE_PERCENT",
			test:       state.DeploymentState_CANARY_ONE_PERCENT,
			startState: state.DeploymentState_CANARY_PROGRESS,
			endState:   state.DeploymentState_CANARY_ONE_PERCENT,
			tags:       []string{name, "prod", "Prometheus", "canary", "canary_one_percent"},
			msg:        fmt.Sprintf("RUN %s %s to canary with 1%% traffic", name, version),
			state:      Finished,
		},
		{
			name:       "CANARY",
			test:       state.DeploymentState_CANARY,
			startState: state.DeploymentState_CANARY,
			endState:   state.DeploymentState_CANARY,
			tags:       []string{name, "prod", "Prometheus", "canary", "traffic_share", "canary_success"},
			msg:        fmt.Sprintf("%s %s - canary with real traffic share", name, version),
			state:      Finished,
			isShort:    true,
		},
		{
			name:       "DeploymentState_CANARY_STOPPED",
			test:       state.DeploymentState_CANARY_STOPPED,
			startState: state.DeploymentState_CANARY_STOPPED,
			endState:   state.DeploymentState_CANARY_STOPPED,
			tags:       []string{name, "prod", "Prometheus", "canary", "canary_stopped"},
			msg:        fmt.Sprintf("%s %s - canary stopped", name, version),
			state:      Finished,
			isShort:    true,
		},
		{
			name:             "Dependent service start",
			test:             state.DeploymentState_IN_PROGRESS,
			startState:       state.DeploymentState_IN_PROGRESS,
			endState:         state.DeploymentState_UNKNOWN,
			branch:           "my_branch",
			tags:             []string{"_" + name, "prod", "Prometheus", "branch", "in_progress", "svc1_depends"},
			msg:              fmt.Sprintf("RUN %s %s (branch my_branch) is in progress", name, version),
			dependentService: "svc1",
			state:            Started,
		},
		{
			name:             "Dependent service point",
			test:             state.DeploymentState_CANARY,
			startState:       state.DeploymentState_CANARY,
			endState:         state.DeploymentState_CANARY,
			tags:             []string{"_" + name, "prod", "Prometheus", "svc1_depends", "canary", "traffic_share", "canary_success"},
			msg:              fmt.Sprintf("%s %s - canary with real traffic share", name, version),
			dependentService: "svc1",
			state:            Finished,
			isShort:          true,
		},
		{
			name:             "Dependent service stop",
			prepare:          state.DeploymentState_IN_PROGRESS,
			test:             state.DeploymentState_SUCCESS,
			startState:       state.DeploymentState_IN_PROGRESS,
			endState:         state.DeploymentState_SUCCESS,
			tags:             []string{"_" + name, "prod", "Prometheus", "success", "svc1_depends"},
			msg:              "RUN service_name 0.0.1",
			dependentService: "svc1",
			state:            Finished,
		},
	}
	issued := time.Date(2021, 1, 1, 1, 1, 1, 25*int(time.Millisecond), time.UTC)
	issuedBefore := issued.Add(-30 * time.Second)
	for _, c := range cs {
		t.Run(c.name, func(t *testing.T) {
			apiMock := annotationMock(t, c, issued, issuedBefore)

			sMapSvc := service_map.NewService(db, test.NewLogger(t), nil)
			service := NewService(db, test.NewLogger(t), NewConf(), apiMock, sMapSvc)
			e := makeEvent2(name, version, dtype.DeploymentType_RUN, state.DeploymentState_UNKNOWN, issued)
			grafanaEvent := &grafana.GrafanaEvent{DeploymentEvent: e, DependentService: c.dependentService}
			if c.dType != dtype.DeploymentType_UNKNOWN {
				e.Deployment.Type = c.dType
			}
			if c.branch != "" {
				e.Deployment.Branch = c.branch
			}
			if c.prepare != state.DeploymentState_UNKNOWN {
				e.Deployment.State = c.startState
				e.EventCreated = timestamppb.New(issuedBefore)
				require.NoError(t, service.handleGrafanaEvent(makeMqMsg(t, grafanaEvent)))
			}
			e.Deployment.State = c.test
			e.UUID = uuid.New().String()
			e.EventCreated = timestamppb.New(issued)

			require.NoError(t, service.handleGrafanaEvent(makeMqMsg(t, grafanaEvent)))

			ds, err := service.storage.getByDeployment(e.Deployment.ID())
			require.NoError(t, err)
			require.Len(t, ds, 1)
			data := ds[0]
			assert.Equal(t, c.startState.String(), data.StartDState.String())
			assert.Equal(t, c.endState.String(), data.EndDState.String())
			assert.Equal(t, c.state.String(), data.State.String())
			assert.NotEmpty(t, data.Text)
			assert.NotEmpty(t, data.Tags)
			assert.Equal(t, c.msg, data.Text)
			tags := strings.Split(data.Tags, ",")
			assert.ElementsMatch(t, tags, c.tags)

			apiMock.AssertExpectations(t)
		})
	}
}

func annotationMock(t *testing.T, c TestCase, issued time.Time, issuedBefore time.Time) *smock.IAnnotation {
	apiMock := &smock.IAnnotation{}
	tagsMatcher := mock.MatchedBy(func(a []string) bool {
		return assert.ElementsMatch(t, c.tags, a)
	})
	if c.isShort {
		apiMock.On("Point", c.msg, tagsMatcher, issued).Return(int64(1), nil).Once()
	} else {
		if c.prepare != state.DeploymentState_UNKNOWN {
			apiMock.On("Start", mock.Anything, mock.Anything, issuedBefore).Return(int64(1), nil).Once()
		} else {
			apiMock.On("Start", c.msg, tagsMatcher, issued).Return(int64(1), nil).Once()
		}
		if c.state == Finished {
			apiMock.On("Stop", c.msg, tagsMatcher, issued, int64(1)).Return(nil).Once()
		}
	}
	return apiMock
}

func TestService_HandleEvent_WaitApprove(t *testing.T) {
	test.InitTestEnv()
	db := test_db.NewSeparatedDb(t)

	grafanaConf := NewConf()
	grafanaConf.DeployTopic = t.Name()
	apiMock := &smock.IAnnotation{}

	sMapSvc := service_map.NewService(db, test.NewLogger(t), nil)
	service := NewService(db, test.NewLogger(t), NewConf(), apiMock, sMapSvc)

	issued := time.Now()
	e := makeEvent2(t.Name(), version, dtype.DeploymentType_PROMOTE, state.DeploymentState_WAIT_APPROVE, issued)
	require.NoError(t, service.handleEvent2(e))
	ans, err := service.storage.getByDeployment(e.Deployment.ID())
	require.NoError(t, err)
	assert.Empty(t, ans)
}

func TestService_HandleEvent_Duplicate(t *testing.T) {
	test.InitTestEnv()
	db := test_db.NewSeparatedDb(t)

	apiMock := &smock.IAnnotation{}
	apiMock.On("Start", mock.Anything, mock.Anything, mock.Anything).Return(int64(1), nil).Once()
	apiMock.On("Stop", mock.Anything, mock.Anything, mock.Anything, int64(1)).Return(nil).Once()

	sMapSvc := service_map.NewService(db, test.NewLogger(t), nil)
	service := NewService(db, test.NewLogger(t), NewConf(), apiMock, sMapSvc)

	issued := time.Now()
	e := makeEvent2(name, version, dtype.DeploymentType_RUN, state.DeploymentState_IN_PROGRESS, issued)
	grafanaEvent := &grafana.GrafanaEvent{DeploymentEvent: e}

	require.NoError(t, service.handleGrafanaEvent(makeMqMsg(t, grafanaEvent)))
	assertState(t, service, e.Deployment.ID(), Started)
	require.NoError(t, service.handleGrafanaEvent(makeMqMsg(t, grafanaEvent)))
	assertState(t, service, e.Deployment.ID(), Started)

	e.Deployment.State = state.DeploymentState_SUCCESS
	e.UUID = uuid.New().String()
	require.NoError(t, service.handleGrafanaEvent(makeMqMsg(t, grafanaEvent)))
	assertState(t, service, e.Deployment.ID(), Finished)

	e.Deployment.State = state.DeploymentState_SUCCESS
	e.UUID = uuid.New().String()
	require.NoError(t, service.handleGrafanaEvent(makeMqMsg(t, grafanaEvent)))
	assertState(t, service, e.Deployment.ID(), Finished)

	e.Deployment.State = state.DeploymentState_IN_PROGRESS
	e.UUID = uuid.New().String()
	require.NoError(t, service.handleGrafanaEvent(makeMqMsg(t, grafanaEvent)))
	assertState(t, service, e.Deployment.ID(), Finished)

	apiMock.AssertExpectations(t)
}

func TestService_HandleEvent2(t *testing.T) {
	test.InitTestEnv()
	db := test_db.NewSeparatedDb(t)

	testCases := []struct {
		Name  string
		dType dtype.DeploymentType
	}{
		{
			Name:  "Run",
			dType: dtype.DeploymentType_RUN,
		},
		{
			Name:  "Update",
			dType: dtype.DeploymentType_UPDATE,
		},
	}

	for _, tc := range testCases {
		t.Run(tc.Name, func(t *testing.T) {
			pMock := mqMock.NewProducerMock()
			sMapSvc := service_map.NewService(db, test.NewLogger(t), service_change.NewNotificationMock())
			service := NewService(db, test.NewLogger(t), NewConf(), nil, sMapSvc)
			service.producer = pMock

			issued := time.Now()
			e := makeEvent2(name, version, tc.dType, state.DeploymentState_IN_PROGRESS, issued)

			makeMap(t, sMapSvc, "s1", name)
			makeMap(t, sMapSvc, "s2", name)
			makeMap(t, sMapSvc, "s3", "some-srv")

			expectedEvents := []*grafana.GrafanaEvent{
				{DeploymentEvent: e, DependentService: ""},
				{DeploymentEvent: e, DependentService: "s1"},
				{DeploymentEvent: e, DependentService: "s2"},
			}

			var expectedMsg []*mq.Message
			for _, event := range expectedEvents {
				m, err := mq.NewProtoMessage("", event, nil)
				require.NoError(t, err)
				expectedMsg = append(expectedMsg, m)
			}

			require.NoError(t, service.handleEvent2(e))
			require.Equal(t, len(expectedMsg), len(pMock.Msg))

			for range expectedMsg {
				require.Contains(t, expectedMsg, pMock.Get(t))
			}
		})
	}
}

func TestService_HandleBatchEvent_StartStop(t *testing.T) {
	test.InitTestEnv()
	db := test_db.NewSeparatedDb(t)

	apiMock := new(smock.IAnnotation)
	sMapSvc := service_map.NewService(db, test.NewLogger(t), nil)
	svc := NewService(db, test.NewLogger(t), NewConf(), apiMock, sMapSvc)
	msg, err := mq.NewProtoMessage("", &btePb.BatchTaskEvent{
		Deployment: &deployment.Deployment{
			Layer:       layerPb.Layer_PROD,
			ServiceName: "test-svc",
			Version:     "v1",
		},
		Task: &btPb.BatchTask{
			Id:        42,
			StartDate: timestamppb.New(time.Now()),
			State:     btPb.State_Process,
		},
	}, nil)
	require.NoError(t, err)

	msg2, err := mq.NewProtoMessage("", &btePb.BatchTaskEvent{
		Deployment: &deployment.Deployment{
			Layer:       layerPb.Layer_PROD,
			ServiceName: "test-svc",
			Version:     "v1",
		},
		Task: &btPb.BatchTask{
			Id:        42,
			StartDate: timestamppb.New(time.Now()),
			EndDate:   timestamppb.New(time.Now().Add(time.Hour)),
			State:     btPb.State_Success,
		},
	}, nil)
	require.NoError(t, err)

	apiMock.On("Start", "test-svc v1 is running", mock.Anything, mock.Anything).Return(int64(1042), nil).Once()
	apiMock.On("Stop", "test-svc v1 is running", mock.Anything, mock.Anything, int64(1042)).Return(nil).Once()
	defer apiMock.AssertExpectations(t)

	require.NoError(t, svc.handleBatchTaskEvent(msg))
	require.NoError(t, svc.handleBatchTaskEvent(msg2))
}

func TestService_HandleBatchEvent_EndEvent(t *testing.T) {
	test.InitTestEnv()
	db := test_db.NewSeparatedDb(t)

	apiMock := new(smock.IAnnotation)
	sMapSvc := service_map.NewService(db, test.NewLogger(t), nil)
	svc := NewService(db, test.NewLogger(t), NewConf(), apiMock, sMapSvc)
	msg2, err := mq.NewProtoMessage("", &btePb.BatchTaskEvent{
		Deployment: &deployment.Deployment{
			Layer:       layerPb.Layer_PROD,
			ServiceName: "test-svc",
			Version:     "v1",
		},
		Task: &btPb.BatchTask{
			Id:        44,
			StartDate: timestamppb.New(time.Now()),
			EndDate:   timestamppb.New(time.Now().Add(time.Hour)),
			State:     btPb.State_Success,
		},
	}, nil)
	require.NoError(t, err)

	apiMock.On("Start", "test-svc v1 is running", mock.Anything, mock.Anything).Return(int64(1042), nil).Once()
	apiMock.On("Stop", "test-svc v1 is running", mock.Anything, mock.Anything, int64(1042)).Return(nil).Once()
	defer apiMock.AssertExpectations(t)

	require.NoError(t, svc.handleBatchTaskEvent(msg2))
}

func assertState(t *testing.T, service *Service, id int64, state State) {
	res, err := service.storage.getByDeployment(id)

	require.NoError(t, err)
	require.Len(t, res, 1)
	require.Equal(t, state, res[0].State)
}

func makeEvent2(name, version string, dType dtype.DeploymentType, state state.DeploymentState, issued time.Time) *event2.Event {
	e := &event2.Event{
		UUID:         uuid.New().String(),
		EventCreated: timestamppb.New(issued),
		Deployment: &deployment.Deployment{
			Id:          strconv.FormatInt(r.Int63(), 10),
			ServiceName: name,
			Version:     version,
			Start:       timestamppb.New(time.Now()),
			End:         timestamppb.New(time.Now().Add(time.Hour)),
			State:       state,
			Type:        dType,
			Layer:       layerPb.Layer_PROD,
		},
	}
	return e
}

func makeMqMsg(t *testing.T, grafanaEvent *grafana.GrafanaEvent) *mq.Message {
	result, err := mq.NewProtoMessage("", grafanaEvent, nil)
	require.NoError(t, err)
	return result
}

func makeMap(t *testing.T, service *service_map.Service, name, dependsName string) {
	path := smProto.ToFullPath(name)

	require.NoError(t, service.ReadAndSave([]byte(fmt.Sprintf(sMap, name, dependsName)), test.AtomicNextUint(), path))

	_, _, err := service.GetByFullPath(path)
	require.NoError(t, err)
}
