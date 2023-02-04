package reparator

import (
	"fmt"
	"testing"

	"github.com/YandexClassifieds/cms/cmd/server/agent"
	"github.com/YandexClassifieds/cms/cmd/server/checks"
	"github.com/YandexClassifieds/cms/cmd/server/clusters"
	"github.com/YandexClassifieds/cms/cmd/server/hosts"
	"github.com/YandexClassifieds/cms/cmd/server/hostsStatus"
	"github.com/YandexClassifieds/cms/cmd/server/reparator/pipeline"
	pbDC "github.com/YandexClassifieds/cms/pb/cms/domains/datacenters"
	pbHostStatuses "github.com/YandexClassifieds/cms/pb/cms/domains/host_statuses"
	pbHostTypes "github.com/YandexClassifieds/cms/pb/cms/domains/host_types"
	pbPipeline "github.com/YandexClassifieds/cms/pb/cms/domains/pipelines/pipeline"
	pbPipelineState "github.com/YandexClassifieds/cms/pb/cms/domains/pipelines/state"
	"github.com/YandexClassifieds/cms/test"
	mPipeline "github.com/YandexClassifieds/cms/test/mocks/mockery/server/reparator/pipeline"
	"github.com/YandexClassifieds/go-common/log"
	"github.com/YandexClassifieds/go-common/log/logrus"
	"github.com/stretchr/testify/mock"
	"github.com/stretchr/testify/require"
	"gorm.io/gorm"
)

const (
	HostReady        string = "host.ready"
	HostFoundProblem string = "host.found.problem"
	HostInRepair     string = "host.in.repair"
)

func TestService_NeedRepair(t *testing.T) {
	tests := map[string]struct {
		Hostname       string
		HostDisable    bool
		ClusterDisable bool
		Verdict        bool
	}{
		"enable-ready": {
			Hostname: HostReady,
			Verdict:  false,
		},
		"enable-found_problem": {
			Hostname: HostFoundProblem,
			Verdict:  true,
		},
		"already-statrt-repair": {
			Hostname: HostInRepair,
			Verdict:  true,
		},
		"disable-host-found_problem": {
			Hostname:    HostFoundProblem,
			HostDisable: true,
			Verdict:     false,
		},
		"disable-cluster-found_problem": {
			Hostname:       HostFoundProblem,
			ClusterDisable: true,
			Verdict:        false,
		},
		"disable-found_problem": {
			Hostname:       HostFoundProblem,
			HostDisable:    true,
			ClusterDisable: true,
			Verdict:        false,
		},
		"disable-cluster-in-repair": {
			Hostname:       HostInRepair,
			ClusterDisable: true,
			Verdict:        false,
		},
		"disable-host-in-repair": {
			Hostname:    HostInRepair,
			HostDisable: true,
			Verdict:     false,
		},
		"disable-in-repair": {
			Hostname:       HostInRepair,
			HostDisable:    true,
			ClusterDisable: true,
			Verdict:        false,
		},
	}

	test.InitTestEnv()
	db := test.NewSeparatedGorm(t)
	log := logrus.New()
	prepare(t, db, log)

	hostStorage := hosts.NewStorage(db, log)
	hostStatusStorage := hostsStatus.NewStorage(db, log)

	svc := &Service{
		log:          log,
		hostStatuses: hostStatusStorage,
	}

	for name, tc := range tests {
		t.Run(name, func(t *testing.T) {
			host, err := hostStorage.GetByName(tc.Hostname)
			require.NoError(t, err)
			host.AutorepairEnable = !tc.HostDisable
			host.Cluster.AutorepairEnable = !tc.ClusterDisable

			require.Equal(t, tc.Verdict, svc.NeedRepair(host))
		})
	}
}

func TestService_StartRepair_Cancel(t *testing.T) {
	test.InitTestEnv()
	db := test.NewSeparatedGorm(t)
	log := logrus.New()
	prepare(t, db, log)

	clusterStorage := clusters.NewStorage(db, log)
	hostStorage := hosts.NewStorage(db, log)
	hostStatusStorage := hostsStatus.NewStorage(db, log)
	pipelineStorage := pipeline.NewStorage(db, log)
	checkStorage := checks.NewStorage(db, log)
	factory := agent.NewFactory(agent.NewConf())
	checkService := checks.NewService(log, hostStatusStorage, checkStorage, factory, []string{})

	p := makeMatchPipelineMock(t)

	host, err := hostStorage.GetByName(HostFoundProblem)
	require.NoError(t, err)

	// emulate concurrent repair
	require.NoError(t, db.Callback().Update().Register("concurrent repair", func(db *gorm.DB) {
		if db.Statement.Table == "hosts_status" && db.Statement.Vars[0] == pbHostStatuses.Status_IN_REPAIR {
			require.NoError(t, db.Callback().Update().Remove("concurrent repair")) // self delete after 1 execution

			host.Cluster.NotReadyLimit = 0
		}
	}))

	svc := New(
		pipelineStorage,
		hostStatusStorage,
		clusterStorage,
		checkService,
		factory,
		log,
	)
	svc.pipelinesList = []pipeline.IPipeline{p}

	svc.StartRepair(host)

	status, err := hostStatusStorage.GetByHostID(host.ID)
	require.NoError(t, err)
	require.Equal(t, pbHostStatuses.Status_FOUND_PROBLEM, status.Status)

	clusterList, err := clusterStorage.ListByHostType(pbHostTypes.HostType_BAREMETAL)
	require.NoError(t, err)
	require.Equal(t, 1, len(clusterList))
	require.Equal(t, false, clusterList[0].AutorepairEnable)
}

func TestService_StartRepair_Run(t *testing.T) {
	test.InitTestEnv()
	db := test.NewSeparatedGorm(t)
	log := logrus.New()
	prepare(t, db, log)

	clusterStorage := clusters.NewStorage(db, log)
	hostStorage := hosts.NewStorage(db, log)
	hostStatusStorage := hostsStatus.NewStorage(db, log)
	pipelineStorage := pipeline.NewStorage(db, log)
	checkStorage := checks.NewStorage(db, log)
	factory := agent.NewFactory(agent.NewConf())
	checkService := checks.NewService(log, hostStatusStorage, checkStorage, factory, []string{})

	p := makeMatchPipelineMock(t)

	svc := New(
		pipelineStorage,
		hostStatusStorage,
		clusterStorage,
		checkService,
		factory,
		log,
	)
	svc.pipelinesList = []pipeline.IPipeline{p}

	host, err := hostStorage.GetByName(HostFoundProblem)
	require.NoError(t, err)

	svc.StartRepair(host)

	status, err := hostStatusStorage.GetByHostID(host.ID)
	require.NoError(t, err)
	require.Equal(t, pbHostStatuses.Status_READY, status.Status)

	p.AssertCalled(t, "Run", host)
}

func TestService_repair_New(t *testing.T) {
	test.InitTestEnv()
	db := test.NewSeparatedGorm(t)
	log := logrus.New()
	prepare(t, db, log)

	clusterStorage := clusters.NewStorage(db, log)
	hostStorage := hosts.NewStorage(db, log)
	hostStatusStorage := hostsStatus.NewStorage(db, log)
	pipelineStorage := pipeline.NewStorage(db, log)
	checkStorage := checks.NewStorage(db, log)
	factory := agent.NewFactory(agent.NewConf())
	checkService := checks.NewService(log, hostStatusStorage, checkStorage, factory, []string{})

	svc := New(
		pipelineStorage,
		hostStatusStorage,
		clusterStorage,
		checkService,
		factory,
		log,
	)

	p1, p2 := makeNonMatchPipelineMock(t), makeMatchPipelineMock(t)
	svc.pipelinesList = []pipeline.IPipeline{p1, p2}

	host, err := hostStorage.GetByName(HostFoundProblem)
	require.NoError(t, err)

	svc.repair(host)

	status, err := hostStatusStorage.GetByHostID(host.ID)
	require.NoError(t, err)
	require.Equal(t, pbHostStatuses.Status_READY, status.Status)

	p1.AssertNotCalled(t, "Run", host)
	p2.AssertCalled(t, "Run", host)
}

func TestService_repair_Continue(t *testing.T) {
	test.InitTestEnv()
	db := test.NewSeparatedGorm(t)
	log := logrus.New()
	prepare(t, db, log)

	clusterStorage := clusters.NewStorage(db, log)
	hostStorage := hosts.NewStorage(db, log)
	hostStatusStorage := hostsStatus.NewStorage(db, log)
	pipelineStorage := pipeline.NewStorage(db, log)
	checkStorage := checks.NewStorage(db, log)
	factory := agent.NewFactory(agent.NewConf())
	checkService := checks.NewService(log, hostStatusStorage, checkStorage, factory, []string{})

	svc := New(
		pipelineStorage,
		hostStatusStorage,
		clusterStorage,
		checkService,
		factory,
		log,
	)

	p1, p2 := makeNonMatchPipelineMock(t), makeMatchPipelineMock(t)
	svc.pipelinesList = []pipeline.IPipeline{p1, p2}

	host, err := hostStorage.GetByName(HostInRepair)
	require.NoError(t, err)
	require.NoError(t, pipelineStorage.Save(&pipeline.Pipeline{
		HostID:   host.ID,
		Pipeline: p1.Type(),
		State:    pbPipelineState.State_IN_PROGRESS,
	}))

	svc.repair(host)

	status, err := hostStatusStorage.GetByHostID(host.ID)
	require.NoError(t, err)
	require.Equal(t, pbHostStatuses.Status_READY, status.Status)

	p1.AssertCalled(t, "Run", host)
	p2.AssertNotCalled(t, "Run", host)
}

func TestService_repair_NotMatch(t *testing.T) {
	test.InitTestEnv()
	db := test.NewSeparatedGorm(t)
	log := logrus.New()
	prepare(t, db, log)

	clusterStorage := clusters.NewStorage(db, log)
	hostStorage := hosts.NewStorage(db, log)
	hostStatusStorage := hostsStatus.NewStorage(db, log)
	pipelineStorage := pipeline.NewStorage(db, log)
	checkStorage := checks.NewStorage(db, log)
	factory := agent.NewFactory(agent.NewConf())
	checkService := checks.NewService(log, hostStatusStorage, checkStorage, factory, []string{})

	svc := New(
		pipelineStorage,
		hostStatusStorage,
		clusterStorage,
		checkService,
		factory,
		log,
	)

	p1, p2 := makeNonMatchPipelineMock(t), makeNonMatchPipelineMock(t)
	svc.pipelinesList = []pipeline.IPipeline{p1, p2}

	host, err := hostStorage.GetByName(HostFoundProblem)
	require.NoError(t, err)

	svc.repair(host)

	status, err := hostStatusStorage.GetByHostID(host.ID)
	require.NoError(t, err)
	require.Equal(t, pbHostStatuses.Status_NEED_MANUAL, status.Status)

	p1.AssertNotCalled(t, "Run", host)
	p2.AssertNotCalled(t, "Run", host)
}

func TestService_repair_Fail(t *testing.T) {
	test.InitTestEnv()
	db := test.NewSeparatedGorm(t)
	log := logrus.New()
	prepare(t, db, log)

	clusterStorage := clusters.NewStorage(db, log)
	hostStorage := hosts.NewStorage(db, log)
	hostStatusStorage := hostsStatus.NewStorage(db, log)
	pipelineStorage := pipeline.NewStorage(db, log)
	checkStorage := checks.NewStorage(db, log)
	factory := agent.NewFactory(agent.NewConf())
	checkService := checks.NewService(log, hostStatusStorage, checkStorage, factory, []string{})

	svc := New(
		pipelineStorage,
		hostStatusStorage,
		clusterStorage,
		checkService,
		factory,
		log,
	)

	p1 := makeFailPipelineMock(t)
	svc.pipelinesList = []pipeline.IPipeline{p1}

	host, err := hostStorage.GetByName(HostFoundProblem)
	require.NoError(t, err)

	svc.repair(host)

	status, err := hostStatusStorage.GetByHostID(host.ID)
	require.NoError(t, err)
	require.Equal(t, pbHostStatuses.Status_NEED_MANUAL, status.Status)

	p1.AssertCalled(t, "Run", host)
}

func prepare(t *testing.T, db *gorm.DB, log log.Logger) {
	t.Helper()

	// clusters
	clusterStorage := clusters.NewStorage(db, log)
	require.NoError(t, clusterStorage.Save(pbHostTypes.HostType_BAREMETAL, pbDC.DC_SAS, true, 3))
	clusterMap, err := clusterStorage.GetMapByHostType(pbHostTypes.HostType_BAREMETAL)
	require.NoError(t, err)

	// hosts
	hostStorage := hosts.NewStorage(db, log)
	require.NoError(t, hostStorage.Save(HostReady, clusterMap[pbDC.DC_SAS]))
	readyID, err := hostStorage.GetIDByName(HostReady)
	require.NoError(t, err)

	require.NoError(t, hostStorage.Save(HostFoundProblem, clusterMap[pbDC.DC_SAS]))
	problemID, err := hostStorage.GetIDByName(HostFoundProblem)
	require.NoError(t, err)

	require.NoError(t, hostStorage.Save(HostInRepair, clusterMap[pbDC.DC_SAS]))
	repairID, err := hostStorage.GetIDByName(HostInRepair)
	require.NoError(t, err)

	// statuses
	hostStatusStorage := hostsStatus.NewStorage(db, log)
	require.NoError(t, hostStatusStorage.Save(&hostsStatus.HostsStatus{
		HostID: readyID,
		Status: pbHostStatuses.Status_READY,
	}))
	require.NoError(t, hostStatusStorage.Save(&hostsStatus.HostsStatus{
		HostID: problemID,
		Status: pbHostStatuses.Status_FOUND_PROBLEM,
	}))
	require.NoError(t, hostStatusStorage.Save(&hostsStatus.HostsStatus{
		HostID: repairID,
		Status: pbHostStatuses.Status_IN_REPAIR,
	}))
}

func makeMatchPipelineMock(t *testing.T) *mPipeline.IPipeline {
	t.Helper()

	p := &mPipeline.IPipeline{}
	p.On("Match", mock.Anything).Return(true)
	p.On("Type").Return(pbPipeline.Pipeline_UNISPACE)
	p.On("Run", mock.Anything).Return(nil)

	return p
}

func makeNonMatchPipelineMock(t *testing.T) *mPipeline.IPipeline {
	t.Helper()

	p := &mPipeline.IPipeline{}
	p.On("Match", mock.Anything).Return(false)
	p.On("Type").Return(pbPipeline.Pipeline_UNKNOWN)
	p.On("Run", mock.Anything).Return(nil)

	return p
}

func makeFailPipelineMock(t *testing.T) *mPipeline.IPipeline {
	t.Helper()

	p := &mPipeline.IPipeline{}
	p.On("Match", mock.Anything).Return(true)
	p.On("Type").Return(pbPipeline.Pipeline_UNISPACE)
	p.On("Run", mock.Anything).Return(fmt.Errorf("test"))

	return p
}
