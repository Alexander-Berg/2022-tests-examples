package checks

import (
	"sort"
	"testing"

	"github.com/YandexClassifieds/cms/cmd/server/agent"
	"github.com/YandexClassifieds/cms/cmd/server/clusters"
	"github.com/YandexClassifieds/cms/cmd/server/hosts"
	"github.com/YandexClassifieds/cms/cmd/server/hostsStatus"
	pbAgent "github.com/YandexClassifieds/cms/pb/cms/api/agent"
	pbCheckStatuses "github.com/YandexClassifieds/cms/pb/cms/domains/check_statuses"
	pbChecks "github.com/YandexClassifieds/cms/pb/cms/domains/checks"
	pbDC "github.com/YandexClassifieds/cms/pb/cms/domains/datacenters"
	pbHostStatuses "github.com/YandexClassifieds/cms/pb/cms/domains/host_statuses"
	pbHostTypes "github.com/YandexClassifieds/cms/pb/cms/domains/host_types"
	"github.com/YandexClassifieds/cms/test"
	"github.com/YandexClassifieds/go-common/log"
	"github.com/YandexClassifieds/go-common/log/logrus"
	"github.com/nhatthm/grpcmock"
	"github.com/stretchr/testify/require"
	"gorm.io/gorm"
)

var (
	checkResultsExample = []*pbAgent.CheckResult{
		{
			Check:       pbChecks.Check_CONSUL,
			Status:      pbCheckStatuses.Status_CRIT,
			Description: "Test description1",
		},
		{
			Check:       pbChecks.Check_ANSIBLE_PULL,
			Status:      pbCheckStatuses.Status_CRIT,
			Description: "Test description2",
		},
	}
	hostname = "test"
)

func TestService_Update(t *testing.T) {
	test.InitTestEnv()
	db := test.NewSeparatedGorm(t)
	log := logrus.New()

	clusterStorage := clusters.NewStorage(db, log)
	hostStorage := hosts.NewStorage(db, log)

	addr, port := test.StartAgentServer(t, "cms.api.agent.AgentService", func(s *grpcmock.Server) {
		s.ExpectUnary("cms.api.agent.AgentService/GetCheckResults").Return(&pbAgent.GetCheckResultsResponse{
			CheckResults: checkResultsExample,
		})
	})

	svc := prepare(t, db, log, port)

	clusterMap, err := clusterStorage.GetMapByHostType(pbHostTypes.HostType_BAREMETAL)
	require.NoError(t, err)
	require.NoError(t, hostStorage.Save(addr, clusterMap[pbDC.DC_SAS]))
	host, err := hostStorage.GetByName(addr)
	require.NoError(t, err)

	svc.Update(host)
	checks := getChecks(t, svc)
	require.Equal(t, len(checkResultsExample), len(checks))

	require.Equal(t, len(checkResultsExample), len(checks))

	sort.Slice(checks, func(i, j int) bool {
		return checks[i].Type < checks[j].Type
	})
	sort.Slice(checkResultsExample, func(i, j int) bool {
		return checkResultsExample[i].Check < checkResultsExample[j].Check
	})

	for i := 0; i < len(checks); i++ {
		require.Equal(t, checkResultsExample[i].Check, checks[i].Type)
		require.Equal(t, checkResultsExample[i].Status, checks[i].Status)
		require.Equal(t, checkResultsExample[i].Description, checks[i].Description)
		require.Equal(t, host.ID, checks[i].HostID)
	}
}

func TestService_updateHostStatus_New(t *testing.T) {
	test.InitTestEnv()
	db := test.NewSeparatedGorm(t)
	log := logrus.New()

	svc := prepare(t, db, log, 0)

	hostStorage := hosts.NewStorage(db, log)
	host, err := hostStorage.GetByName(hostname)
	require.NoError(t, err)

	// add new (FOUND_PROBLEMS for new host - OK)
	require.NoError(t, svc.updateHostStatus(host, pbHostStatuses.Status_FOUND_PROBLEM))

	var status *hostsStatus.HostsStatus
	require.NoError(t, db.Where("host_id = ?", host.ID).Take(&status).Error)
	require.Equal(t, pbHostStatuses.Status_NEW, status.Status)

	// all checks OK
	require.NoError(t, svc.updateHostStatus(host, pbHostStatuses.Status_READY))
	require.NoError(t, db.Where("host_id = ?", host.ID).Take(&status).Error)
	require.Equal(t, pbHostStatuses.Status_READY, status.Status)

	// found problems
	require.NoError(t, svc.updateHostStatus(host, pbHostStatuses.Status_FOUND_PROBLEM))
	require.NoError(t, db.Where("host_id = ?", host.ID).Take(&status).Error)
	require.Equal(t, pbHostStatuses.Status_FOUND_PROBLEM, status.Status)
}

func TestService_updateHostStatus_Existing(t *testing.T) {
	test.InitTestEnv()
	db := test.NewSeparatedGorm(t)
	log := logrus.New()

	svc := prepare(t, db, log, 0)

	hostStorage := hosts.NewStorage(db, log)
	host, err := hostStorage.GetByName(hostname)
	require.NoError(t, err)

	// save ready host
	require.NoError(t, db.Create(&hostsStatus.HostsStatus{
		HostID: host.ID,
		Status: pbHostStatuses.Status_READY,
	}).Error)

	require.NoError(t, svc.updateHostStatus(host, pbHostStatuses.Status_FOUND_PROBLEM))

	var status *hostsStatus.HostsStatus
	require.NoError(t, db.Where("host_id = ?", host.ID).Take(&status).Error)
	require.Equal(t, pbHostStatuses.Status_FOUND_PROBLEM, status.Status)
}

func TestService_getAllowedChecks(t *testing.T) {
	checks := getAllowedChecks([]string{"UNISPACE", "IDENTICAL_JOBS", "_NOT_EXISTS_"})
	require.Equal(t, map[pbChecks.Check]struct{}{
		pbChecks.Check_UNISPACE:       {},
		pbChecks.Check_IDENTICAL_JOBS: {},
	}, checks)
}

func getChecks(t *testing.T, svc *Service) []*Check {
	t.Helper()

	var result []*Check
	err := svc.storage.base.GetAll(&result, "")
	require.NoError(t, err)

	return result
}

func prepareOld(t *testing.T, db *gorm.DB, log log.Logger) *hosts.Storage {
	t.Helper()

	clusterStorage := clusters.NewStorage(db, log)
	require.NoError(t, clusterStorage.Save(pbHostTypes.HostType_BAREMETAL, pbDC.DC_SAS, true, 1))
	clusters, err := clusterStorage.GetMapByHostType(pbHostTypes.HostType_BAREMETAL)
	require.NoError(t, err)
	hostsStorage := hosts.NewStorage(db, log)
	require.NoError(t, hostsStorage.Save("test1", clusters[pbDC.DC_SAS]))
	require.NoError(t, hostsStorage.Save("test2", clusters[pbDC.DC_SAS]))
	return hostsStorage
}

func prepare(t *testing.T, db *gorm.DB, log log.Logger, port int) *Service {
	t.Helper()

	clusterStorage := clusters.NewStorage(db, log)
	hostStorage := hosts.NewStorage(db, log)
	hostStatusStorage := hostsStatus.NewStorage(db, log)
	checkStorage := NewStorage(db, log)

	require.NoError(t, clusterStorage.Save(pbHostTypes.HostType_BAREMETAL, pbDC.DC_SAS, true, 1))
	clusterMap, err := clusterStorage.GetMapByHostType(pbHostTypes.HostType_BAREMETAL)
	require.NoError(t, err)
	require.NoError(t, hostStorage.Save(hostname, clusterMap[pbDC.DC_SAS]))

	conf := agent.NewConf()
	conf.Port = port
	factory := agent.NewFactory(conf)

	return NewService(log, hostStatusStorage, checkStorage, factory, []string{})
}
