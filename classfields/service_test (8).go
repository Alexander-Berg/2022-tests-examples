package watcher

import (
	"sort"
	"testing"
	"time"

	"github.com/YandexClassifieds/cms/cmd/server/agent"
	checks "github.com/YandexClassifieds/cms/cmd/server/checks"
	"github.com/YandexClassifieds/cms/cmd/server/clusters"
	"github.com/YandexClassifieds/cms/cmd/server/hosts"
	"github.com/YandexClassifieds/cms/cmd/server/hostsStatus"
	reparator "github.com/YandexClassifieds/cms/cmd/server/reparator"
	"github.com/YandexClassifieds/cms/cmd/server/reparator/pipeline"
	pbAgent "github.com/YandexClassifieds/cms/pb/cms/api/agent"
	pbAction "github.com/YandexClassifieds/cms/pb/cms/domains/actions/action"
	pbActionState "github.com/YandexClassifieds/cms/pb/cms/domains/actions/state"
	pbCheckStatuses "github.com/YandexClassifieds/cms/pb/cms/domains/check_statuses"
	pbChecks "github.com/YandexClassifieds/cms/pb/cms/domains/checks"
	pbDC "github.com/YandexClassifieds/cms/pb/cms/domains/datacenters"
	pbHostStatuses "github.com/YandexClassifieds/cms/pb/cms/domains/host_statuses"
	pbHostTypes "github.com/YandexClassifieds/cms/pb/cms/domains/host_types"
	pbPipelineState "github.com/YandexClassifieds/cms/pb/cms/domains/pipelines/state"
	"github.com/YandexClassifieds/cms/test"
	"github.com/YandexClassifieds/go-common/log"
	"github.com/YandexClassifieds/go-common/log/logrus"
	"github.com/nhatthm/grpcmock"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
	"gorm.io/gorm"
)

var (
	exampleCheckResults = []*pbAgent.CheckResult{
		{
			Check:       pbChecks.Check_CRON,
			Status:      pbCheckStatuses.Status_OK,
			Description: "test",
		},
		{
			Check:  pbChecks.Check_UNISPACE,
			Status: pbCheckStatuses.Status_WARN,
		},
	}
)

// it uses lock with static name '::', so do not start it frequently
func TestService_Run(t *testing.T) {
	test.InitTestEnv()
	db := test.NewSeparatedGorm(t)
	log := logrus.New()

	addr, port := test.StartAgentServer(t, "cms.api.agent.AgentService", func(s *grpcmock.Server) {
		s.ExpectUnary("cms.api.agent.AgentService/GetCheckResults").Once().Return(&pbAgent.GetCheckResultsResponse{
			CheckResults: exampleCheckResults,
		})
		s.ExpectUnary("cms.api.agent.AgentService/DoAction").Return(&pbAgent.DoActionResponse{})
		s.ExpectUnary("cms.api.agent.AgentService/GetActionStatus").Return(&pbAgent.GetActionStatusResponse{
			Action: pbAction.Action_DRAIN,
			State:  pbActionState.State_SUCCESS,
		})
		s.ExpectUnary("cms.api.agent.AgentService/DoAction").Return(&pbAgent.DoActionResponse{})
		s.ExpectUnary("cms.api.agent.AgentService/GetActionStatus").Return(&pbAgent.GetActionStatusResponse{
			Action: pbAction.Action_UNDRAIN,
			State:  pbActionState.State_SUCCESS,
		})
	})
	prepareDB(t, db, log, addr)

	clusterStorage := clusters.NewStorage(db, log)
	hostStorage := hosts.NewStorage(db, log)
	hostStatusStorage := hostsStatus.NewStorage(db, log)
	checkStorage := checks.NewStorage(db, log)
	pipelineStorage := pipeline.NewStorage(db, log)
	agentConf := agent.NewConf()
	agentConf.Port = port
	factory := agent.NewFactory(agentConf)
	checkService := checks.NewService(log, hostStatusStorage, checkStorage, factory, []string{"UNISPACE"})
	reparatorService := reparator.New(pipelineStorage, hostStatusStorage, clusterStorage, checkService, factory, log)

	svc := newService(hostStorage, checkService, reparatorService, log)

	host, err := hostStorage.GetByName(addr)
	require.NoError(t, err)

	go svc.run()
	require.Eventually(t, func() bool {
		t.Log("check")
		status, err := hostStatusStorage.GetByHostID(host.ID)
		hostStatusPassed := assert.NoError(t, err) && assert.Equal(t, pbHostStatuses.Status_READY, status.Status)

		checkResults, err := checkStorage.GetByHostID(host.ID)
		checksPassed := assert.NoError(t, err) && assertEqualChecks(t, exampleCheckResults, checkResults)

		var p pipeline.Pipeline
		pipelinePassed := assert.NoError(t, db.Model(&pipeline.Pipeline{}).Unscoped().Where("host_id = ?", host.ID).Take(&p).Error) &&
			assert.Equal(t, pbPipelineState.State_SUCCESS, p.State)

		return hostStatusPassed && checksPassed && pipelinePassed
	}, 5*time.Minute, 1*time.Second)
}

func prepareDB(t *testing.T, db *gorm.DB, log log.Logger, addr string) {
	t.Helper()

	clusterStorage := clusters.NewStorage(db, log)
	require.NoError(t, clusterStorage.Save(pbHostTypes.HostType_BAREMETAL, pbDC.DC_SAS, true, 1))
	clusterMap, err := clusterStorage.GetMapByHostType(pbHostTypes.HostType_BAREMETAL)
	require.NoError(t, err)

	hostStorage := hosts.NewStorage(db, log)
	require.NoError(t, hostStorage.Save(addr, clusterMap[pbDC.DC_SAS]))
	hostID, err := hostStorage.GetIDByName(addr)
	require.NoError(t, err)

	hostStatusStorage := hostsStatus.NewStorage(db, log)
	require.NoError(t, hostStatusStorage.Save(&hostsStatus.HostsStatus{
		HostID: hostID,
		Status: pbHostStatuses.Status_READY,
	}))
}

func assertEqualChecks(t *testing.T, exampleChecks []*pbAgent.CheckResult, checks []*checks.Check) bool {
	t.Helper()

	if len(exampleCheckResults) != len(checks) {
		return false
	}

	sort.Slice(exampleChecks, func(i, j int) bool {
		return exampleChecks[i].Check < exampleChecks[j].Check
	})
	sort.Slice(checks, func(i, j int) bool {
		return checks[i].Type < checks[j].Type
	})

	result := true
	for i := 0; i < len(exampleChecks); i++ {
		result = result && (exampleChecks[i].Check == checks[i].Type)
		result = result && (exampleChecks[i].Status == checks[i].Status)
		result = result && (exampleChecks[i].Description == checks[i].Description)
	}

	return result
}
