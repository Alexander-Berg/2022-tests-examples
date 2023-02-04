package action

import (
	"testing"
	"time"

	"github.com/YandexClassifieds/cms/cmd/server/agent"
	"github.com/YandexClassifieds/cms/cmd/server/hosts"
	pbAgent "github.com/YandexClassifieds/cms/pb/cms/api/agent"
	pbAction "github.com/YandexClassifieds/cms/pb/cms/domains/actions/action"
	pbActionState "github.com/YandexClassifieds/cms/pb/cms/domains/actions/state"
	"github.com/YandexClassifieds/cms/test"
	"github.com/nhatthm/grpcmock"
	"github.com/stretchr/testify/require"
)

func TestAgentAction_Do_Ok(t *testing.T) {
	test.InitTestEnv()

	host, port := test.StartAgentServer(t, "cms.api.agent.AgentService", func(s *grpcmock.Server) {
		s.ExpectUnary("cms.api.agent.AgentService/DoAction").Return(&pbAgent.DoActionResponse{})
		s.ExpectUnary("cms.api.agent.AgentService/GetActionStatus").Once().Return(&pbAgent.GetActionStatusResponse{
			Action: pbAction.Action_DRAIN,
			State:  pbActionState.State_IN_PROGRESS,
		})
		s.ExpectUnary("cms.api.agent.AgentService/GetActionStatus").Once().Return(&pbAgent.GetActionStatusResponse{
			Action: pbAction.Action_DRAIN,
			State:  pbActionState.State_SUCCESS,
		})
	})

	agentConf := agent.NewConf()
	agentConf.Port = port
	a := NewAgentAction(pbAction.Action_DRAIN, agent.NewFactory(agentConf))
	a.tickInterval = 1 * time.Second

	err := a.Do(&hosts.Host{Name: host})
	require.NoError(t, err)
}

func TestAgentAction_Do_Error(t *testing.T) {
	test.InitTestEnv()

	host, port := test.StartAgentServer(t, "cms.api.agent.AgentService", func(s *grpcmock.Server) {
		s.ExpectUnary("cms.api.agent.AgentService/DoAction").Return(&pbAgent.DoActionResponse{})
		s.ExpectUnary("cms.api.agent.AgentService/GetActionStatus").Once().Return(&pbAgent.GetActionStatusResponse{
			Action: pbAction.Action_DRAIN,
			State:  pbActionState.State_IN_PROGRESS,
		})
		s.ExpectUnary("cms.api.agent.AgentService/GetActionStatus").Once().Return(&pbAgent.GetActionStatusResponse{
			Action: pbAction.Action_DRAIN,
			State:  pbActionState.State_FAILED,
		})
	})

	agentConf := agent.NewConf()
	agentConf.Port = port
	a := NewAgentAction(pbAction.Action_DRAIN, agent.NewFactory(agentConf))
	a.tickInterval = 1 * time.Second

	err := a.Do(&hosts.Host{Name: host})
	require.ErrorIs(t, err, ErrActionFailed)
}

func TestAgentAction_Do_Timeout(t *testing.T) {
	test.InitTestEnv()

	host, port := test.StartAgentServer(t, "cms.api.agent.AgentService", func(s *grpcmock.Server) {
		s.ExpectUnary("cms.api.agent.AgentService/DoAction").Return(&pbAgent.DoActionResponse{})
		s.ExpectUnary("cms.api.agent.AgentService/GetActionStatus").UnlimitedTimes().Return(&pbAgent.GetActionStatusResponse{
			Action: pbAction.Action_DRAIN,
			State:  pbActionState.State_IN_PROGRESS,
		})
	})

	agentConf := agent.NewConf()
	agentConf.Port = port
	a := NewAgentAction(pbAction.Action_DRAIN, agent.NewFactory(agentConf))
	a.tickInterval = 1 * time.Second
	a.timeout = 5 * time.Second

	err := a.Do(&hosts.Host{Name: host})
	require.ErrorIs(t, err, ErrTimeout)
}
