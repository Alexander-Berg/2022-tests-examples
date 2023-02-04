package hook

import (
	"context"
	"math/rand"
	"net"
	"strconv"
	"sync"
	"testing"
	"time"

	"github.com/YandexClassifieds/shiva/cmd/hook/hook/model"
	"github.com/YandexClassifieds/shiva/cmd/hook/setting"
	"github.com/YandexClassifieds/shiva/common/logger"
	hpb "github.com/YandexClassifieds/shiva/pb/shiva/api/hook"
	"github.com/YandexClassifieds/shiva/pb/shiva/types/deployment"
	"github.com/YandexClassifieds/shiva/pb/shiva/types/dtype"
	"github.com/YandexClassifieds/shiva/pb/shiva/types/layer"
	"github.com/YandexClassifieds/shiva/pb/shiva/types/state"
	"github.com/YandexClassifieds/shiva/pkg/mq"
	"github.com/YandexClassifieds/shiva/test"
	"github.com/YandexClassifieds/shiva/test/test_db"
	"github.com/golang/protobuf/proto"
	"github.com/golang/protobuf/ptypes"
	"github.com/google/uuid"
	"github.com/stretchr/testify/assert"
	"google.golang.org/grpc"
	"google.golang.org/grpc/codes"
	"google.golang.org/grpc/status"
)

const (
	serverAddress = "localhost:8090"
	firstFail     = "first_fail"
	allFail       = "all_fail"
)

var (
	r *rand.Rand
)

func init() {
	r = rand.New(rand.NewSource(time.Now().UnixNano()))
}

func TestHandle(t *testing.T) {

	test.RunUp(t)
	defer test.Down(t)
	s := newService(t)
	handler := NewMockHandler(t)
	go handler.Run(t)
	st, err := s.settingS.NewSetting(&setting.Setting{
		Address: serverAddress,
	})
	test.Check(t, err)

	type TestCase struct {
		name    string
		state   model.State
		prepare func(t *testing.T, d *deployment.Deployment)
		assert  func(t *testing.T, h *model.Hook, handler *MockHandler)
	}

	cases := []TestCase{
		{
			name:  "golden_case",
			state: model.InFlight,
			assert: func(t *testing.T, h *model.Hook, handler *MockHandler) {
				assertProcessed(t, h, handler.successC)
				assertEmpty(t, h, handler.successC)
				assertEmpty(t, h, handler.failC)
			},
		},
		{
			name:  "handle_new_state",
			state: model.New,
			assert: func(t *testing.T, h *model.Hook, handler *MockHandler) {
				assertProcessed(t, h, handler.successC)
				assertEmpty(t, h, handler.successC)
				assertEmpty(t, h, handler.failC)
			},
		},
		{
			name:  "skip_success_state",
			state: model.Success,
			assert: func(t *testing.T, h *model.Hook, handler *MockHandler) {
				assertEmpty(t, h, handler.failC)
				assertEmpty(t, h, handler.successC)
			},
		},
		{
			name:  "skip_fail_state",
			state: model.Fail,
			assert: func(t *testing.T, h *model.Hook, handler *MockHandler) {
				assertEmpty(t, h, handler.failC)
				assertEmpty(t, h, handler.successC)
			},
		},
		{
			name:  "retry_first",
			state: model.InFlight,
			prepare: func(t *testing.T, d *deployment.Deployment) {
				d.Comment = firstFail
			},
			assert: func(t *testing.T, h *model.Hook, handler *MockHandler) {
				assertProcessed(t, h, handler.failC)
				assertProcessed(t, h, handler.successC)
				assertEmpty(t, h, handler.successC)
				assertEmpty(t, h, handler.failC)
			},
		},
		{
			name:  "retry_all",
			state: model.InFlight,
			prepare: func(t *testing.T, d *deployment.Deployment) {
				d.Comment = allFail
			},
			assert: func(t *testing.T, h *model.Hook, handler *MockHandler) {
				for i := 0; i < s.conf.RetryCount; i++ {
					assertProcessed(t, h, handler.failC)
				}
				assertEmpty(t, h, handler.failC)
			},
		},
	}

	for _, c := range cases {
		t.Run(c.name, func(t *testing.T) {
			// make new hook
			d := makeDeployment()
			if c.prepare != nil {
				c.prepare(t, d)
			}
			h, err := s.NewHook(uuid.New().String(), st, d)
			test.Check(t, err)
			h.State = c.state
			test.Check(t, s.Save(h))

			// run test
			b, err := proto.Marshal(&Hook{
				Id: h.ID,
			})
			test.Check(t, err)
			msg := mq.NewMessage(t.Name(), b, nil)
			test.Check(t, s.handle(msg))

			// assert
			c.assert(t, h, handler)
		})
	}
	handler.Stop(t)
}

func assertProcessed(t *testing.T, h *model.Hook, r chan *hpb.HookRequest) {
	select {
	case result := <-r:
		assert.Equal(t, h.UUID, result.UUID)
	case <-time.NewTimer(3 * time.Second).C:
		assert.Fail(t, "timeout")
	}
}

func assertEmpty(t *testing.T, _ *model.Hook, r chan *hpb.HookRequest) {

	select {
	case <-r:
		assert.Fail(t, "Message sent")
	case <-time.NewTimer(500 * time.Millisecond).C:
	}
}

func TestDeDuplicate(t *testing.T) {

	test.RunUp(t)
	defer test.Down(t)

	s := newService(t)

	st, err := s.settingS.NewSetting(&setting.Setting{})
	test.Check(t, err)
	d := makeDeployment()
	parent := uuid.New().String()

	hook, err := s.NewHook(parent, st, d)
	test.Check(t, err)
	assert.Equal(t, model.New, hook.State)

	duplicate, err := s.NewHook(parent, st, d)
	test.Check(t, err)
	assert.Equal(t, model.New, duplicate.State)
	assert.Equal(t, hook.UUID, duplicate.UUID)
	assert.Equal(t, hook.ID, duplicate.ID)

	hook.State = model.InFlight
	test.Check(t, s.Save(hook))

	_, err = s.NewHook(parent, st, d)
	assert.Equal(t, ErrHookProcessed, err)
}

func newService(t *testing.T) *Service {
	db := test_db.NewDb(t)
	log := test.NewLogger(t)
	settingS := setting.NewService(db, log)
	return NewService(NewConf(), db, log, settingS)
}

func makeDeployment() *deployment.Deployment {
	return &deployment.Deployment{
		Id:          strconv.FormatInt(r.Int63(), 10),
		ServiceName: "service1",
		Version:     "0.0.1",
		User:        "login",
		Branch:      "",
		Type:        dtype.DeploymentType_RUN,
		Layer:       layer.Layer_PROD,
		State:       state.DeploymentState_SUCCESS,
		Start:       ptypes.TimestampNow(),
		End:         ptypes.TimestampNow(),
	}
}

type MockHandler struct {
	sync.Mutex
	successC chan *hpb.HookRequest
	failC    chan *hpb.HookRequest
	grpc     *grpc.Server
	log      logger.Logger
	fails    map[string]int
}

func NewMockHandler(t *testing.T) *MockHandler {

	server := grpc.NewServer()
	handler := &MockHandler{
		successC: make(chan *hpb.HookRequest, 10),
		failC:    make(chan *hpb.HookRequest, 10),
		grpc:     server,
		fails:    map[string]int{},
	}
	hpb.RegisterHookHandlerServer(server, handler)
	return handler
}

func (m *MockHandler) Run(t *testing.T) {
	listener, err := net.Listen("tcp", serverAddress)
	test.Check(t, err)
	err = m.grpc.Serve(listener)
	if err != nil {
		m.log.WithError(err).Warn("Run", "Server stop")
	}
}

func (m *MockHandler) Stop(t *testing.T) {
	m.grpc.GracefulStop()
}

func (m *MockHandler) Handle(_ context.Context, r *hpb.HookRequest) (*hpb.HookResponse, error) {

	m.Lock()
	defer m.Unlock()

	switch r.Deployment.Comment {
	case firstFail:
		_, ok := m.fails[r.UUID]
		if !ok {
			m.fails[r.UUID] = 1
			m.failC <- r
			return nil, status.Errorf(codes.Internal, "mock error")
		}
		m.successC <- r
		return &hpb.HookResponse{}, nil
	case allFail:
		m.failC <- r
		return nil, status.Errorf(codes.Internal, "mock error")
	default:
		m.successC <- r
		return &hpb.HookResponse{}, nil
	}
}
