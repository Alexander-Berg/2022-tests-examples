package grpc

import (
	"context"
	"testing"

	"github.com/YandexClassifieds/cms/cmd/agent/actions"
	pbAgent "github.com/YandexClassifieds/cms/pb/cms/api/agent"
	pbAction "github.com/YandexClassifieds/cms/pb/cms/domains/actions/action"
	pbActionState "github.com/YandexClassifieds/cms/pb/cms/domains/actions/state"
	pbCheckStatuses "github.com/YandexClassifieds/cms/pb/cms/domains/check_statuses"
	pbChecks "github.com/YandexClassifieds/cms/pb/cms/domains/checks"
	"github.com/YandexClassifieds/cms/test"
	mChecks "github.com/YandexClassifieds/cms/test/mocks/mockery/agent/checks"
	"github.com/YandexClassifieds/go-common/log/logrus"
	"github.com/stretchr/testify/require"
	"google.golang.org/grpc/codes"
	"google.golang.org/grpc/status"
)

func TestHandler_GetCheckResults(t *testing.T) {
	test.InitTestEnv()
	log := logrus.New()

	checkResults := []*pbAgent.CheckResult{
		{
			Check:  pbChecks.Check_CRON,
			Status: pbCheckStatuses.Status_OK,
		}, {
			Check:       pbChecks.Check_ANSIBLE_PULL,
			Status:      pbCheckStatuses.Status_CRIT,
			Description: "test",
		},
	}

	checksMock := &mChecks.IService{}
	checksMock.On("GetResults").Return(checkResults)

	h := NewHandler(checksMock, nil, log)
	result, err := h.GetCheckResults(context.Background(), &pbAgent.GetCheckResultsRequest{})
	require.NoError(t, err)
	require.EqualValues(t, checkResults, result.GetCheckResults())
}

func TestHandler_DoAction(t *testing.T) {
	test.InitTestEnv()
	log := logrus.New()

	a := actions.New(log)
	h := NewHandler(nil, a, log)

	_, err := h.DoAction(context.Background(), &pbAgent.DoActionRequest{
		Action: pbAction.Action_UNDRAIN,
	})
	require.NoError(t, err)
}

func TestHandler_DoActionInProgress(t *testing.T) {
	test.InitTestEnv()
	log := logrus.New()

	a := actions.New(log)
	h := NewHandler(nil, a, log)

	_, err := h.DoAction(context.Background(), &pbAgent.DoActionRequest{
		Action: pbAction.Action_UNDRAIN,
	})
	require.NoError(t, err)
	_, err = h.DoAction(context.Background(), &pbAgent.DoActionRequest{
		Action: pbAction.Action_DRAIN,
	})
	require.Equal(t, status.Error(codes.AlreadyExists, "another action in progress"), err)
}

func TestHandler_GetActionStatus(t *testing.T) {
	test.InitTestEnv()
	log := logrus.New()

	a := actions.New(log)
	h := NewHandler(nil, a, log)

	_, err := h.DoAction(context.Background(), &pbAgent.DoActionRequest{
		Action: pbAction.Action_UNDRAIN,
	})
	require.NoError(t, err)
	result, err := h.GetActionStatus(context.Background(), &pbAgent.GetActionStatusRequest{})
	require.NoError(t, err)
	require.Equal(t, pbAction.Action_UNDRAIN, result.GetAction())
	require.Equal(t, pbActionState.State_ACCEPTED, result.GetState())
}
