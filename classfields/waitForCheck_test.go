package action

import (
	"testing"
	"time"

	"github.com/YandexClassifieds/cms/cmd/server/agent"
	"github.com/YandexClassifieds/cms/cmd/server/hosts"
	pbAgent "github.com/YandexClassifieds/cms/pb/cms/api/agent"
	pbCheckStatuses "github.com/YandexClassifieds/cms/pb/cms/domains/check_statuses"
	pbChecks "github.com/YandexClassifieds/cms/pb/cms/domains/checks"
	"github.com/YandexClassifieds/cms/test"
	"github.com/nhatthm/grpcmock"
	"github.com/stretchr/testify/require"
)

func TestWaitForCheck_Do(t *testing.T) {
	test.InitTestEnv()

	host, port := test.StartAgentServer(t, "cms.api.agent.AgentService", func(s *grpcmock.Server) {
		s.ExpectUnary("cms.api.agent.AgentService/GetCheckResults").Once().Return(&pbAgent.GetCheckResultsResponse{
			CheckResults: []*pbAgent.CheckResult{
				{
					Check:  pbChecks.Check_CRON,
					Status: pbCheckStatuses.Status_OK,
				},
				{
					Check:  pbChecks.Check_NOMAD_AUTODRAIN,
					Status: pbCheckStatuses.Status_CRIT,
				},
			},
		})
		s.ExpectUnary("cms.api.agent.AgentService/GetCheckResults").Once().Return(&pbAgent.GetCheckResultsResponse{
			CheckResults: []*pbAgent.CheckResult{
				{
					Check:  pbChecks.Check_CRON,
					Status: pbCheckStatuses.Status_OK,
				},
				{
					Check:       pbChecks.Check_NOMAD_AUTODRAIN,
					Status:      pbCheckStatuses.Status_OK,
					Description: "ok",
				},
			},
		})
	})

	agentConf := agent.NewConf()
	agentConf.Port = port
	a := NewWaitForCheckStatus(
		agent.NewFactory(agentConf),
		pbChecks.Check_NOMAD_AUTODRAIN,
		pbCheckStatuses.Status_OK,
		"ok",
	)
	a.tickInterval = 1 * time.Second

	err := a.Do(&hosts.Host{Name: host})
	require.NoError(t, err)
}

func TestWaitForCheck_Do_Timeout(t *testing.T) {
	test.InitTestEnv()

	host, port := test.StartAgentServer(t, "cms.api.agent.AgentService", func(s *grpcmock.Server) {
		s.ExpectUnary("cms.api.agent.AgentService/GetCheckResults").UnlimitedTimes().Return(&pbAgent.GetCheckResultsResponse{
			CheckResults: []*pbAgent.CheckResult{
				{
					Check:  pbChecks.Check_CRON,
					Status: pbCheckStatuses.Status_OK,
				},
				{
					Check:  pbChecks.Check_NOMAD_AUTODRAIN,
					Status: pbCheckStatuses.Status_CRIT,
				},
			},
		})
	})

	agentConf := agent.NewConf()
	agentConf.Port = port
	a := NewWaitForCheckStatus(
		agent.NewFactory(agentConf),
		pbChecks.Check_NOMAD_AUTODRAIN,
		pbCheckStatuses.Status_OK,
		"ok",
	)
	a.tickInterval = 1 * time.Second
	a.timeout = 5 * time.Second

	err := a.Do(&hosts.Host{Name: host})
	require.ErrorIs(t, err, ErrTimeout)
}
