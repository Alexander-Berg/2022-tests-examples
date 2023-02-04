package handler

import (
	"testing"

	"github.com/YandexClassifieds/shiva/cmd/generator/task"
	events "github.com/YandexClassifieds/shiva/pb/shiva/events/change_conf"
	proto "github.com/YandexClassifieds/shiva/pb/shiva/service_map"
	"github.com/YandexClassifieds/shiva/pb/shiva/types/layer"
	"github.com/YandexClassifieds/shiva/pb/ss/secret"
	tokenMock "github.com/YandexClassifieds/shiva/pkg/secrets/tokens/mocks"
	"github.com/YandexClassifieds/shiva/test/mock/mockery/mocks"
	mock2 "github.com/stretchr/testify/mock"
	"github.com/stretchr/testify/require"
	"google.golang.org/grpc/codes"
	"google.golang.org/grpc/status"
)

var testCases = []struct {
	name              string
	rewriteErr        error
	expectedTaskState task.State
	expectErr         bool
}{
	{
		name:              "success",
		expectedTaskState: task.Success,
	},
	{
		name:              "secret not found",
		rewriteErr:        status.Error(codes.NotFound, "not found"),
		expectedTaskState: task.Fail,
	},
	{
		name:              "internal error",
		rewriteErr:        status.Error(codes.Internal, "some err"),
		expectedTaskState: task.New,
		expectErr:         true,
	},
}

func TestSecret_OnNew(t *testing.T) {
	log, taskS := prepare(t)

	sMap := &proto.ServiceMap{
		Name:   "shiva-test",
		Owners: []string{"https://staff.yandex-team.ru/departments/yandex_personal_vertserv_infra/", "niklogvinenko"},
	}

	for _, tc := range testCases {
		t.Run(tc.name, func(t *testing.T) {
			ssMock := &mocks.SecretClient{}
			tokenMock := &tokenMock.TokensService{}
			h := NewSecretHandler(log, taskS, layer.Layer_TEST, ssMock, tokenMock)

			taskModel := &task.Task{
				Service:    "shiva-test",
				ChangeType: events.ChangeType_NEW,
				Handler:    h.Name(),
				State:      task.New,
			}

			ssMock.On("CreateSecret", mock2.Anything, &secret.CreateSecretRequest{
				ServiceName: taskModel.Service,
				Layer:       layer.Layer_TEST,
				Title:       "autogen.TEST.shiva-test",
			}).Return(&secret.CreateSecretResponse{SecretId: "sec-100"}, nil).Once()
			ssMock.On("RewriteOwners", mock2.Anything, &secret.RewriteOwnersRequest{
				ServiceName:  taskModel.Service,
				Layer:        layer.Layer_TEST,
				ActualOwners: []string{"https://staff.yandex-team.ru/departments/yandex_personal_vertserv_infra/", "niklogvinenko", "robot-vertis-shiva"},
			}).Return(nil, tc.rewriteErr).Once()

			if tc.rewriteErr == nil {
				tokenMock.On("Delegate", taskModel.Service, "sec-100").Return(nil).Once()
			}

			err := h.OnNew(taskModel, sMap)

			require.True(t, (err != nil) == tc.expectErr)
			require.Equal(t, tc.expectedTaskState, taskModel.State)
			ssMock.AssertExpectations(t)
			tokenMock.AssertExpectations(t)
		})
	}

}

func TestSecret_OnUpdate(t *testing.T) {
	log, taskS := prepare(t)

	sMap := &proto.ServiceMap{
		Name:   "shiva-test",
		Owners: []string{"https://staff.yandex-team.ru/departments/yandex_personal_vertserv_infra/", "niklogvinenko"},
	}

	for _, tc := range testCases {
		t.Run(tc.name, func(t *testing.T) {
			ssMock := &mocks.SecretClient{}
			h := NewSecretHandler(log, taskS, layer.Layer_TEST, ssMock, nil)

			taskModel := &task.Task{
				Service:    "shiva-test",
				ChangeType: events.ChangeType_UPDATE,
				Handler:    h.Name(),
				State:      task.New,
			}

			ssMock.On("RewriteOwners", mock2.Anything, &secret.RewriteOwnersRequest{
				ServiceName:  taskModel.Service,
				Layer:        layer.Layer_TEST,
				ActualOwners: []string{"https://staff.yandex-team.ru/departments/yandex_personal_vertserv_infra/", "niklogvinenko", "robot-vertis-shiva"},
			}).Return(nil, tc.rewriteErr).Once()

			err := h.OnUpdate(taskModel, sMap, nil)

			require.True(t, (err != nil) == tc.expectErr)
			require.Equal(t, tc.expectedTaskState, taskModel.State)
			ssMock.AssertExpectations(t)
		})
	}
}

func TestSecret_OnDelete(t *testing.T) {
	log, taskS := prepare(t)

	sMap := &proto.ServiceMap{
		Name:   "shiva-test",
		Owners: []string{"https://staff.yandex-team.ru/departments/yandex_personal_vertserv_infra/", "niklogvinenko"},
	}

	for _, tc := range testCases {
		t.Run(tc.name, func(t *testing.T) {
			ssMock := &mocks.SecretClient{}
			h := NewSecretHandler(log, taskS, layer.Layer_TEST, ssMock, nil)

			taskModel := &task.Task{
				Service:    "shiva-test",
				ChangeType: events.ChangeType_DELETE,
				Handler:    h.Name(),
				State:      task.New,
			}

			ssMock.On("RewriteOwners", mock2.Anything, &secret.RewriteOwnersRequest{
				ServiceName:  taskModel.Service,
				Layer:        layer.Layer_TEST,
				ActualOwners: []string{"robot-vertis-shiva"},
			}).Return(nil, tc.rewriteErr).Once()

			if tc.rewriteErr == nil {
				ssMock.On("DeleteSecret", mock2.Anything, &secret.DeleteSecretRequest{
					ServiceName: taskModel.Service,
					Layer:       layer.Layer_TEST,
				}).Return(nil, nil).Once()
			}

			err := h.OnDelete(taskModel, sMap)

			require.True(t, (err != nil) == tc.expectErr)
			require.Equal(t, tc.expectedTaskState, taskModel.State)
			ssMock.AssertExpectations(t)
		})
	}

}
