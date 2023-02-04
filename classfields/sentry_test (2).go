package handler

import (
	"context"
	"fmt"
	"testing"

	"github.com/YandexClassifieds/shiva/cmd/generator/task"
	events "github.com/YandexClassifieds/shiva/pb/shiva/events/change_conf"
	privateApi "github.com/YandexClassifieds/shiva/pb/shiva/private_api"
	"github.com/YandexClassifieds/shiva/pb/shiva/private_api/mocks"
	proto "github.com/YandexClassifieds/shiva/pb/shiva/service_map"
	"github.com/YandexClassifieds/shiva/pb/shiva/types/env"
	"github.com/YandexClassifieds/shiva/pb/shiva/types/layer"
	"github.com/YandexClassifieds/shiva/pkg/sentry"
	"github.com/YandexClassifieds/shiva/test"
	"github.com/YandexClassifieds/shiva/test/test_db"
	"github.com/google/uuid"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/mock"
	"github.com/stretchr/testify/require"
	"google.golang.org/grpc"
)

func TestSentry_OnNew(t *testing.T) {

	test.RunUp(t)
	log := test.NewLogger(t)
	db := test_db.NewDb(t)
	taskS := task.NewService(log, db)
	sentryM := NewSentryMock()
	privateApiMock := &mocks.PrivateServiceClient{}
	h := NewSentryHandler(log, taskS, sentryM, privateApiMock)
	UUID := t.Name() + uuid.New().String()
	taskModel := &task.Task{
		UUID:       UUID,
		Service:    "shiva-test",
		ChangeType: events.ChangeType_NEW,
		Handler:    h.Name(),
		State:      task.New,
	}
	sMap := &proto.ServiceMap{
		Name:   "shiva-test",
		Owners: []string{"https://staff.yandex-team.ru/departments/yandex_personal_vertserv_infra/"},
		Type:   proto.ServiceType_service,
	}
	sentryM.On("CreateProject", "shiva-test").Return(&sentry.Project{
		Name:    "shiva-test",
		TestDSN: "https://123@sentry.vertis.yandex.net/13",
		ProdDSN: "https://456@sentry.vertis.yandex.net/13",
	}, nil)
	privateApiMock.On("SetEnvs",
		mock.Anything,
		mock.MatchedBy(func(r *privateApi.SetEnvsRequest) bool {
			if assert.Equal(t, "shiva-test", r.Service) == false {
				return false
			}

			switch r.Layer {
			case layer.Layer_TEST:
				res := assert.Len(t, r.Envs, 1)
				res = res && assert.Equal(t, "_DEPLOY_G_SENTRY_DSN", r.Envs[0].Key)
				res = res && assert.Equal(t, "https://123@sentry.vertis.yandex.net/13", r.Envs[0].Value)
				return res
			case layer.Layer_PROD:
				res := assert.Len(t, r.Envs, 1)
				res = res && assert.Equal(t, "_DEPLOY_G_SENTRY_DSN", r.Envs[0].Key)
				res = res && assert.Equal(t, "https://456@sentry.vertis.yandex.net/13", r.Envs[0].Value)
				return res
			default:
				return false
			}
		}),
	).Return(&privateApi.SetEnvsResponse{}, nil)

	require.NoError(t, h.OnNew(taskModel, sMap))

	sentryM.AssertCalled(t, "CreateProject", "shiva-test")
	result, err := taskS.Get(h.Name(), UUID)
	require.NoError(t, err)
	assert.Equal(t, task.Success, result.State)
}

func TestSentry_ObsoleteAPI_OnNew(t *testing.T) {
	test.RunUp(t)
	log := test.NewLogger(t)
	db := test_db.NewDb(t)
	taskS := task.NewService(log, db)
	sentryM := NewSentryMock()
	privateApiMock := newShivaClientMock()
	h := NewSentryHandler(log, taskS, sentryM, privateApiMock)
	UUID := t.Name() + uuid.New().String()
	taskModel := &task.Task{
		UUID:       UUID,
		Service:    "shiva-test",
		ChangeType: events.ChangeType_NEW,
		Handler:    h.Name(),
		State:      task.New,
	}
	sMap := &proto.ServiceMap{
		Name:   "shiva-test",
		Owners: []string{"https://staff.yandex-team.ru/departments/yandex_personal_vertserv_infra/"},
		Type:   proto.ServiceType_service,
	}
	sentryM.On("CreateProject", "shiva-test").Return(&sentry.Project{
		Name:    "shiva-test",
		TestDSN: "https://123@sentry.vertis.yandex.net/13",
		ProdDSN: "https://456@sentry.vertis.yandex.net/13",
	}, nil)

	require.NoError(t, h.OnNew(taskModel, sMap))

	sentryM.AssertCalled(t, "CreateProject", "shiva-test")
	result, err := taskS.Get(h.Name(), UUID)
	require.NoError(t, err)
	assert.Equal(t, task.Success, result.State)
	for _, e := range privateApiMock.envs {
		l := e.GetObsoleteLayer()
		k := e.GetKey()
		switch {
		case l == layer.Layer_PROD && k == "_DEPLOY_G_SENTRY_DSN":
			assert.Equal(t, "https://456@sentry.vertis.yandex.net/13", e.GetValue())
		case l == layer.Layer_TEST && k == "_DEPLOY_G_SENTRY_DSN":
			assert.Equal(t, "https://123@sentry.vertis.yandex.net/13", e.GetValue())
		default:
			assert.FailNow(t, fmt.Sprintf("undefined env (layer:%s;key%s)", l.String(), k))
		}
	}
}

type SentryMock struct {
	mock.Mock
}

func NewSentryMock() *SentryMock {
	return &SentryMock{}
}

func (m *SentryMock) CreateProject(name string) (*sentry.Project, error) {
	args := m.Called(name)
	return args.Get(0).(*sentry.Project), args.Error(1)
}

type ShivaClientMock struct {
	envs []*env.Env
}

func newShivaClientMock() *ShivaClientMock {
	return &ShivaClientMock{}
}

// Deprecated
func (s *ShivaClientMock) SetEnvs(_ context.Context, r *privateApi.SetEnvsRequest, _ ...grpc.CallOption) (*privateApi.SetEnvsResponse, error) {
	s.envs = append(s.envs, r.GetObsoleteEnv()...)
	return &privateApi.SetEnvsResponse{}, nil
}
