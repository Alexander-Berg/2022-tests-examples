package scheduler

import (
	"errors"
	"fmt"
	"testing"

	"github.com/YandexClassifieds/shiva/common/user_error"
	"github.com/YandexClassifieds/shiva/pb/shiva/api/deploy2"
	deploymentPb "github.com/YandexClassifieds/shiva/pb/shiva/types/deployment"
	infoPb "github.com/YandexClassifieds/shiva/pb/shiva/types/info"
	statePb "github.com/YandexClassifieds/shiva/pb/shiva/types/state"
	"github.com/YandexClassifieds/shiva/test"
	"github.com/YandexClassifieds/shiva/test/mock/mockery/mocks"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/mock"
	"github.com/stretchr/testify/require"
	"google.golang.org/grpc/codes"
	"google.golang.org/grpc/status"
)

func TestClient_State(t *testing.T) {
	test.RunUp(t)
	mc := new(mocks.DeployServiceClient)
	cli := NewClient(mc, test.NewLogger(t))

	t.Run("base_success", func(t *testing.T) {
		mockStates(mc, []statePb.DeploymentState{
			statePb.DeploymentState_PREPARE,
			statePb.DeploymentState_IN_PROGRESS,
			statePb.DeploymentState_SUCCESS,
		})

		stateC := cli.State("42")
		assertLastState(t, stateC, statePb.DeploymentState_SUCCESS)
	})
	t.Run("canary_one_percent", func(t *testing.T) {
		mockStates(mc, []statePb.DeploymentState{
			statePb.DeploymentState_PREPARE,
			statePb.DeploymentState_CANARY_PROGRESS,
			statePb.DeploymentState_CANARY_ONE_PERCENT,
		})

		stateC := cli.State("42")
		assertLastState(t, stateC, statePb.DeploymentState_CANARY_ONE_PERCENT)
	})
	t.Run("canary_full", func(t *testing.T) {
		mockStates(mc, []statePb.DeploymentState{
			statePb.DeploymentState_CANARY,
		})

		stateC := cli.State("42")
		assertLastState(t, stateC, statePb.DeploymentState_CANARY)
	})
}

func TestClient_Status(t *testing.T) {
	test.RunUp(t)
	mc := new(mocks.DeployServiceClient)
	cli := NewClient(mc, test.NewLogger(t))

	t.Run("fail with detail", func(t *testing.T) {
		userError := user_error.NewUserError(fmt.Errorf("some error"), "Ошибка")
		st := status.New(codes.InvalidArgument, userError.Error())
		st, err := st.WithDetails(userError.ToProto())
		require.NoError(t, err)

		mc.On("Status", mock.Anything, mock.Anything).Return(nil, st.Err()).Once()

		_, err = cli.Status("service")
		userErrors := new(user_error.UserErrors)
		if !errors.As(err, &userErrors) {
			assert.FailNow(t, "not user_errors")
		}
		assert.Equal(t, userErrors.Get()[0], userError)
	})

	t.Run("fail with details", func(t *testing.T) {
		userError1 := user_error.NewUserError(fmt.Errorf("some error 1"), "Ошибка 1")
		userError2 := user_error.NewUserError(fmt.Errorf("some error 2"), "Ошибка 2")
		st := status.New(codes.InvalidArgument, userError1.Error())
		st, err := st.WithDetails(userError1.ToProto(), userError2.ToProto())
		require.NoError(t, err)

		mc.On("Status", mock.Anything, mock.Anything).Return(nil, st.Err()).Once()

		_, err = cli.Status("service")
		userErrors := new(user_error.UserErrors)
		if !errors.As(err, &userErrors) {
			assert.FailNow(t, "not user_errors")
		}
		assert.Equal(t, userErrors.Get()[0], userError1)
		assert.Equal(t, userErrors.Get()[1], userError2)
	})

	t.Run("fail without details", func(t *testing.T) {
		st := status.Error(codes.InvalidArgument, "some error")

		mc.On("Status", mock.Anything, mock.Anything).Return(nil, st).Once()

		_, err := cli.Status("service")
		assert.Equal(t, err, st)
	})
}

func mockStates(mc *mocks.DeployServiceClient, states []statePb.DeploymentState) {
	for _, s := range states {
		resp := &deploy2.StateResponse{
			Service: &infoPb.DeploymentInfo{
				Deployment: &deploymentPb.Deployment{State: s},
			},
		}
		mc.On("State", mock.Anything, &deploy2.StateRequest{Id: "42"}).Return(resp, nil).Once()
	}
}

func assertLastState(t *testing.T, stateC <-chan *deploy2.StateResponse, expected statePb.DeploymentState) {
	t.Helper()
	var lastState statePb.DeploymentState
	for s := range stateC {
		lastState = s.GetService().GetDeployment().GetState()
	}
	assert.Equal(t, expected, lastState)
}
