package agent

import (
	"context"
	"testing"

	pbAgent "github.com/YandexClassifieds/cms/pb/cms/api/agent"
	pbAction "github.com/YandexClassifieds/cms/pb/cms/domains/actions/action"
	pbActionState "github.com/YandexClassifieds/cms/pb/cms/domains/actions/state"
	pbCheckStatuses "github.com/YandexClassifieds/cms/pb/cms/domains/check_statuses"
	pbChecks "github.com/YandexClassifieds/cms/pb/cms/domains/checks"
	"github.com/YandexClassifieds/cms/test"
	"github.com/YandexClassifieds/go-common/tvm/tvmauth"
	"github.com/nhatthm/grpcmock"
	"github.com/stretchr/testify/require"
	"google.golang.org/grpc/metadata"
)

func TestService_GetCheckResults(t *testing.T) {
	test.InitTestEnv()

	conf := NewConf()

	testResults := []*pbAgent.CheckResult{
		{
			Check:       pbChecks.Check_CRON,
			Status:      pbCheckStatuses.Status_WARN,
			Description: "test",
		},
	}

	host, port := test.StartAgentServer(t, "cms.api.agent.AgentService", func(s *grpcmock.Server) {
		s.ExpectUnary("cms.api.agent.AgentService/GetCheckResults").Run(checkAuth(
			t,
			conf.SelfTvmID,
			conf.DstTvmID,
			&pbAgent.GetCheckResultsResponse{
				CheckResults: testResults,
			},
		))
	})

	conf.Port = port
	factory := NewFactory(conf)

	svc, err := factory.New(host)
	require.NoError(t, err)
	results, err := svc.GetCheckResults()
	require.NoError(t, err)
	require.Equal(t, 1, len(results))
	require.Equal(t, testResults[0].Check, results[0].Check)
	require.Equal(t, testResults[0].Status, results[0].Status)
	require.Equal(t, testResults[0].Description, results[0].Description)
}

func TestService_DoAction(t *testing.T) {
	test.InitTestEnv()

	conf := NewConf()

	host, port := test.StartAgentServer(t, "cms.api.agent.AgentService", func(s *grpcmock.Server) {
		s.ExpectUnary("cms.api.agent.AgentService/DoAction").Run(checkAuth(
			t,
			conf.SelfTvmID,
			conf.DstTvmID,
			&pbAgent.DoActionResponse{},
		))
	})

	conf.Port = port
	factory := NewFactory(conf)

	svc, err := factory.New(host)
	require.NoError(t, err)
	err = svc.DoAction(pbAction.Action_DRAIN)
	require.NoError(t, err)
}

func TestService_GetActionStatus(t *testing.T) {
	test.InitTestEnv()

	conf := NewConf()

	host, port := test.StartAgentServer(t, "cms.api.agent.AgentService", func(s *grpcmock.Server) {
		s.ExpectUnary("cms.api.agent.AgentService/GetActionStatus").Run(checkAuth(
			t,
			conf.SelfTvmID,
			conf.DstTvmID,
			&pbAgent.GetActionStatusResponse{
				Action: pbAction.Action_DRAIN,
				State:  pbActionState.State_SUCCESS,
			},
		))
	})

	conf.Port = port
	factory := NewFactory(conf)

	svc, err := factory.New(host)
	require.NoError(t, err)

	state, err := svc.GetActionStatus()
	require.NoError(t, err)
	require.Equal(t, pbActionState.State_SUCCESS, state)
}

func checkAuth(t *testing.T, srcID int, dstID int, result interface{}) func(ctx context.Context, in interface{}) (interface{}, error) {
	t.Helper()

	return func(ctx context.Context, in interface{}) (interface{}, error) {
		md, ok := metadata.FromIncomingContext(ctx)
		require.True(t, ok)
		require.Len(t, md.Get("x-ya-service-ticket"), 1)
		ticket := md.Get("x-ya-service-ticket")[0]

		tvmClient := tvmauth.NewClient()
		data, err := tvmClient.CheckServiceTicket(ticket)
		require.NoError(t, err)
		require.Equal(t, srcID, data.SrcID)
		require.Equal(t, dstID, data.DstID)

		return result, nil
	}
}
