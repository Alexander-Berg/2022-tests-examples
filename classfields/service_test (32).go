package bulk_deployment

import (
	"context"
	"errors"
	"fmt"
	"sync"
	"testing"
	"time"

	"github.com/YandexClassifieds/shiva/cmd/shiva/approve"
	"github.com/YandexClassifieds/shiva/cmd/shiva/deployment"
	dcontext "github.com/YandexClassifieds/shiva/cmd/shiva/deployment/context"
	"github.com/YandexClassifieds/shiva/cmd/shiva/deployment/data"
	"github.com/YandexClassifieds/shiva/cmd/shiva/deployment/issue_link"
	"github.com/YandexClassifieds/shiva/cmd/shiva/deployment/model"
	"github.com/YandexClassifieds/shiva/cmd/shiva/deployment/notification"
	"github.com/YandexClassifieds/shiva/cmd/shiva/deployment/status"
	"github.com/YandexClassifieds/shiva/cmd/shiva/drills"
	"github.com/YandexClassifieds/shiva/cmd/shiva/env/external/reader"
	env_resolver "github.com/YandexClassifieds/shiva/cmd/shiva/env/manifest"
	"github.com/YandexClassifieds/shiva/cmd/shiva/env/override"
	"github.com/YandexClassifieds/shiva/cmd/shiva/scaler"
	"github.com/YandexClassifieds/shiva/common"
	"github.com/YandexClassifieds/shiva/common/config"
	"github.com/YandexClassifieds/shiva/common/storage"
	"github.com/YandexClassifieds/shiva/pb/shiva/types/bulk"
	cSync "github.com/YandexClassifieds/shiva/pkg/consul/sync"
	"github.com/YandexClassifieds/shiva/pkg/feature_flags"
	"github.com/YandexClassifieds/shiva/pkg/i/locker"
	"github.com/YandexClassifieds/shiva/pkg/include"
	"github.com/YandexClassifieds/shiva/pkg/manifest"
	"github.com/YandexClassifieds/shiva/pkg/manifest/parser"
	"github.com/YandexClassifieds/shiva/pkg/mq"
	"github.com/YandexClassifieds/shiva/pkg/registry"
	"github.com/YandexClassifieds/shiva/pkg/secrets"
	"github.com/YandexClassifieds/shiva/pkg/secrets/secret"
	"github.com/YandexClassifieds/shiva/pkg/service_map"
	"github.com/YandexClassifieds/shiva/pkg/staff"
	staffapi "github.com/YandexClassifieds/shiva/pkg/staff/api"
	"github.com/YandexClassifieds/shiva/pkg/template"
	"github.com/YandexClassifieds/shiva/pkg/tracker"
	stAPI "github.com/YandexClassifieds/shiva/pkg/tracker/api"
	"github.com/YandexClassifieds/shiva/test"
	"github.com/YandexClassifieds/shiva/test/mock"
	dmock "github.com/YandexClassifieds/shiva/test/mock/deployment"
	"github.com/YandexClassifieds/shiva/test/mock/mockery/mocks"
	mqMock "github.com/YandexClassifieds/shiva/test/mock/mq"
	"github.com/YandexClassifieds/shiva/test/mock/service_change"
	staff_fixture "github.com/YandexClassifieds/shiva/test/mock/staff"
	"github.com/YandexClassifieds/shiva/test/test_db"
	"github.com/golang/protobuf/proto"
	"github.com/google/uuid"
	"github.com/stretchr/testify/assert"
	mock2 "github.com/stretchr/testify/mock"
	"github.com/stretchr/testify/require"
)

var (
	manifestYml = `
name: %s
general:
  datacenters: {sas: {count: 1}, myt: {count:1}}
  memory: 255
`
	sMapYml = `
name: %s
description: Deployment system
is_external: false
owner: https://staff.yandex-team.ru/departments/yandex_personal_vertserv_infra
design_doc:
src:  https://github.com/YandexClassifieds/shiva
sox: false
`
	sMapPath  = "maps/%s.yml"
	soxMapYml = `
name: %s
description: Deployment system
is_external: false
owner: https://staff.yandex-team.ru/departments/yandex_personal_vertserv_infra
design_doc:
src:  https://github.com/YandexClassifieds/shiva
sox: false
`

	newSMapYml = `
name: %s
description: new Deployment system
is_external: false
owner: https://staff.yandex-team.ru/departments/yandex_personal_vertserv_infra
design_doc:
src:  https://github.com/YandexClassifieds/shiva-test
sox: false
`

	newManifestYml = `
name: %s
general:
  datacenters: {sas: {count: 1}, myt: {count:2}}
  memory: 200
`
)

const (
	robotLogin = "robot-vertis-shiva"
	version    = "v1"
)

func newConf() Conf {
	conf := NewConf()
	conf.ServiceLimit = 2
	conf.LockTimeout = 5
	conf.RetryTime = 250 * time.Millisecond
	conf.Admins = map[string]struct{}{staff_fixture.Owner: {}}
	return conf
}

func TestService_BulkRestart_Resize(t *testing.T) {
	test.InitTestEnv()
	db := test_db.NewSeparatedDb(t)

	statusStorage := status.NewStorage(db, test.NewLogger(t))
	schedulerMock := mock.NewMockScheduler()
	pMock := mqMock.NewProducerMock()
	s := newService(t, db, schedulerMock, dmock.NewProducerMock(), newConf(), pMock)
	srv := ServiceBranch{Service: "svcName"}
	runService(t, db, s, schedulerMock, statusStorage, srv.Service, srv.Branch, common.Test, sMapYml)

	br, err := s.BulkDeployment(common.Test, common.Update, t.Name(), config.AdminSource, NewServiceBranchSet(srv))
	require.NoError(t, err)
	assertBulkRestart(t, br, bulk.State_Started, robotLogin)
	schedulerMock.Success(t, common.Update, srv.Service, srv.Branch, version)
	br = WaitEnd(t, s, br)
	assertBulkRestart(t, br, bulk.State_Success, robotLogin)
	assertEvent(t, pMock, br)
	assert.Empty(t, pMock.Msg)
	assert.Equal(t, common.Update.String(), br.DType.String())
}

func TestServiceBulkRestartSuccessByStoppedBranch(t *testing.T) {
	test.InitTestEnv()
	db := test_db.NewSeparatedDb(t)

	statusStorage := status.NewStorage(db, test.NewLogger(t))
	schedulerMock := mock.NewMockScheduler()
	pMock := mqMock.NewProducerMock()
	s := newService(t, db, schedulerMock, dmock.NewProducerMock(), newConf(), pMock)
	srv1 := ServiceBranch{Service: "svcName", Branch: "bulkDeployment"}
	srv2 := ServiceBranch{Service: "svcName"}
	runService(t, db, s, schedulerMock, statusStorage, srv1.Service, srv1.Branch, common.Test, sMapYml)
	runService(t, db, s, schedulerMock, statusStorage, srv2.Service, srv2.Branch, common.Test, sMapYml)

	//Stop branch
	state, dCtx, err := s.DeploySvc.Stop(context.Background(), model.StopParams{
		UUID:   uuid.New(),
		Layer:  common.Test,
		Name:   srv1.Service,
		Branch: srv1.Branch,
		Login:  staff_fixture.Owner,
	})
	require.NoError(t, err)
	schedulerMock.SuccessStop(t, common.Stop, srv1.Service, srv1.Branch)
	for range state {
	}
	assert.Equal(t, dCtx.Deployment.State, model.Success)
	require.NoError(t, statusStorage.Save(contextToStatus(dCtx)))

	br, err := s.BulkDeployment(common.Test, common.Run, t.Name(), config.AdminSource, NewServiceBranchSet(srv1, srv2))
	require.NoError(t, err)
	assertBulkRestart(t, br, bulk.State_Started, robotLogin)
	schedulerMock.Success(t, common.Run, srv2.Service, srv2.Branch, version)
	br = WaitEnd(t, s, br)
	assertBulkRestart(t, br, bulk.State_Success, robotLogin)
	assertEvent(t, pMock, br)
	assert.Empty(t, pMock.Msg)
	assert.Equal(t, common.Run.String(), br.DType.String())
}

func TestService_BulkRestartFail(t *testing.T) {
	test.InitTestEnv()
	db := test_db.NewSeparatedDb(t)

	statusStorage := status.NewStorage(db, test.NewLogger(t))
	schedulerMock := mock.NewMockScheduler()
	pMock := mqMock.NewProducerMock()
	s := newService(t, db, schedulerMock, dmock.NewProducerMock(), newConf(), pMock)
	srv := ServiceBranch{Service: "svcName"}
	runService(t, db, s, schedulerMock, statusStorage, srv.Service, srv.Branch, common.Test, sMapYml)

	br, err := s.BulkDeployment(common.Test, common.Run, t.Name(), config.AdminSource, NewServiceBranchSet(srv))
	require.NoError(t, err)
	assertBulkRestart(t, br, bulk.State_Started, robotLogin)
	schedulerMock.Fail(t, common.Run, srv.Service, srv.Branch, version)
	br = WaitEnd(t, s, br)
	assertBulkRestart(t, br, bulk.State_Failed, robotLogin)
	assertEvent(t, pMock, br)
	assert.Empty(t, pMock.Msg)
	assert.Equal(t, common.Run.String(), br.DType.String())
}

func TestService_ParallelBulkRestart(t *testing.T) {
	test.InitTestEnv()
	db := test_db.NewSeparatedDb(t)

	statusStorage := status.NewStorage(db, test.NewLogger(t))
	schedulerMock := mock.NewMockScheduler()
	pMock := mqMock.NewProducerMock()
	s := newService(t, db, schedulerMock, dmock.NewProducerMock(), newConf(), pMock)

	srvBrs1 := NewServiceBranchSet(
		ServiceBranch{Service: "srvA"},
		ServiceBranch{Service: "srvB", Branch: "branchB"},
		ServiceBranch{Service: "srvC"})
	srvBrs2 := NewServiceBranchSet(
		ServiceBranch{Service: "srvD"},
		ServiceBranch{Service: "srvE", Branch: "branchE"})
	adminSrv := "srvF"
	for srv := range srvBrs1 {
		runService(t, db, s, schedulerMock, statusStorage, srv.Service, srv.Branch, common.Test, sMapYml)
	}
	for srv := range srvBrs2 {
		runService(t, db, s, schedulerMock, statusStorage, srv.Service, srv.Branch, common.Test, sMapYml)
	}
	runService(t, db, s, schedulerMock, statusStorage, adminSrv, "", common.Test, sMapYml)

	br1, err := s.BulkDeployment(common.Test, common.Run, t.Name(), config.AdminSource, srvBrs1)
	require.NoError(t, err)
	brA, err := s.AdminBulkDeployment(common.Test, staff_fixture.Owner, t.Name(), "VOID-1", []string{adminSrv}, false, false)
	require.NoError(t, err)
	br2, err := s.BulkDeployment(common.Test, common.Run, t.Name(), config.AdminSource, srvBrs2)
	require.NoError(t, err)
	assertBulkRestart(t, br1, bulk.State_Started, robotLogin)
	assertBulkRestart(t, brA, bulk.State_Started, staff_fixture.Owner)
	assertBulkRestart(t, br2, bulk.State_Started, robotLogin)

	time.Sleep(3 * time.Second) // fix flaky test or deploy will try to mark as successful before it started
	for srv := range srvBrs1 {
		schedulerMock.Success(t, common.Run, srv.Service, srv.Branch, version)
	}
	br1 = WaitEnd(t, s, br1)
	schedulerMock.Success(t, common.Run, adminSrv, "", version)
	brA = WaitEnd(t, s, brA)
	for srv := range srvBrs2 {
		schedulerMock.Success(t, common.Run, srv.Service, srv.Branch, version)
	}
	br2 = WaitEnd(t, s, br2)
	assertBulkRestart(t, br1, bulk.State_Success, robotLogin)
	assertBulkRestart(t, brA, bulk.State_Success, staff_fixture.Owner)
	assertBulkRestart(t, br2, bulk.State_Success, robotLogin)
	assertEvent(t, pMock, br1)
	assertEvent(t, pMock, brA)
	assertEvent(t, pMock, br2)
	assert.Empty(t, pMock.Msg)
}

func TestGetServiceAndBranches(t *testing.T) {
	test.InitTestEnv()
	db := test_db.NewSeparatedDb(t)

	statusStorage := status.NewStorage(db, test.NewLogger(t))
	schedulerMock := mock.NewMockScheduler()
	pMock := mqMock.NewProducerMock()
	s := newService(t, db, schedulerMock, dmock.NewProducerMock(), newConf(), pMock)

	srvBrs1 := []*ServiceBranch{
		{Service: "srvA"},
		{Service: "srvA", Branch: "branchA"},
		{Service: "srvC"}}

	for _, srv := range srvBrs1 {
		runService(t, db, s, schedulerMock, statusStorage, srv.Service, srv.Branch, common.Test, sMapYml)
	}

	expectedBr := NewServiceBranchSet(ServiceBranch{Service: "srvA"}, ServiceBranch{Service: "srvA", Branch: "branchA"})

	br, err := s.GetServiceAndBranches("srvA", common.Test)
	require.NoError(t, err)

	assert.Equal(t, expectedBr, br)
}

func TestService_ParallelRestart(t *testing.T) {
	test.InitTestEnv()
	db := test_db.NewSeparatedDb(t)

	statusStorage := status.NewStorage(db, test.NewLogger(t))
	schedulerMock := mock.NewMockScheduler()
	pMock := mqMock.NewProducerMock()
	s := newService(t, db, schedulerMock, dmock.NewProducerMock(), newConf(), pMock)

	srv1 := "srv1"
	srv2 := "srv2"
	runService(t, db, s, schedulerMock, statusStorage, srv1, "", common.Test, sMapYml)
	runService(t, db, s, schedulerMock, statusStorage, srv2, "", common.Test, sMapYml)

	br, err := s.BulkDeployment(
		common.Test, common.Run, t.Name(), config.AdminSource, NewServiceBranchSet(ServiceBranch{Service: srv1}),
	)
	require.NoError(t, err)
	brA, err := s.AdminBulkDeployment(common.Test, staff_fixture.Owner, t.Name(), "VOID-1", []string{srv2}, false, false)
	require.NoError(t, err)
	assertBulkRestart(t, br, bulk.State_Started, robotLogin)
	assertBulkRestart(t, brA, bulk.State_Started, staff_fixture.Owner)
	statuses, err := s.storage.GetByState(Started)
	require.NoError(t, err)
	require.Len(t, statuses, 2)

	s.DeploySvc.(*deployment.Service).GlobalRestore()
	schedulerMock.Success(t, common.Run, srv1, "", version)
	schedulerMock.Success(t, common.Run, srv2, "", version)
	br = WaitEnd(t, s, br)
	brA = WaitEnd(t, s, brA)
	assertBulkRestart(t, br, bulk.State_Success, robotLogin)
	assertBulkRestart(t, brA, bulk.State_Success, staff_fixture.Owner)
}

func assertEvent(t *testing.T, pMock mqMock.ProducerMock, br *BulkDeployment) {

	protoBr := extract(t, pMock.Get(t))
	assert.Equal(t, br.State.String(), protoBr.State.String())
	assert.Equal(t, br.ID, protoBr.ID)
}

func WaitEnd(t *testing.T, s *Service, br *BulkDeployment) *BulkDeployment {

	result := br
	test.Wait(t, func() error {
		updatedBr, err := s.storage.Get(br.ID)
		if err != nil {
			return err
		}
		if !updatedBr.State.IsFinished() {
			return fmt.Errorf("is not end state")
		}
		result = updatedBr
		return nil
	})
	return result
}

func assertBulkRestart(t *testing.T, br *BulkDeployment, state bulk.State, login string) {
	assert.Equal(t, login, br.Login)
	assert.Equal(t, common.Test, br.Layer)
	assert.Equal(t, t.Name(), br.Comment)
	assert.Equal(t, state.String(), br.State.String())
}

// TODO delete time.sleep
func TestService_AdminBulkRestart(t *testing.T) {
	test.InitTestEnv()
	db := test_db.NewSeparatedDb(t)

	statusStorage := status.NewStorage(db, test.NewLogger(t))
	schedulerMock := mock.NewMockScheduler()
	pMock := mqMock.NewProducerMock()
	s := newService(t, db, schedulerMock, dmock.NewProducerMock(), newConf(), pMock)

	for i := 1; i <= 5; i++ {
		runService(t, db, s, schedulerMock, statusStorage, fmt.Sprintf("test%d", i), "", common.Test, sMapYml)
	}

	//run parallel deployment
	c, runCtx, err := s.DeploySvc.Run(context.Background(), model.RunParams{
		Layer:   common.Test,
		Name:    "test2",
		Version: "v2",
		Login:   staff_fixture.Owner,
		Comment: "some comment",
	})
	require.NoError(t, err)

	brA, err := s.AdminBulkDeployment(common.Test, staff_fixture.Owner, "", "VOID-1", []string{"test1", "test2", "test4", "test5"}, false, false)
	require.NoError(t, err)
	br, statuses, err := s.BulkDeploymentState(brA.ID)
	require.NoError(t, err)
	assert.Equal(t, staff_fixture.Owner, br.Login)
	assert.Equal(t, common.Test, br.Layer)
	assert.Equal(t, "", br.Comment)
	for _, st := range statuses {
		assert.Equal(t, NotStarted, st.State)
	}
	runCtx.Deployment.State = model.Success
	require.NoError(t, statusStorage.Save(contextToStatus(runCtx)))
	time.Sleep(time.Second)
	schedulerMock.Success(t, common.Run, "test2", "", "v2")
	for range c {
	}

	schedulerMock.Success(t, common.Run, "test2", "", "v2")
	time.Sleep(time.Second)
	require.NoError(t, s.CancelBulkDeployment(staff_fixture.Owner, brA.ID))
	schedulerMock.Success(t, common.Run, "test1", "", version)
	schedulerMock.RevertSuccess(t, common.Run, "test4", "", version)

	WaitEnd(t, s, br)
	dStorage := model.NewStorage(db, test.NewLogger(t))

	br, statuses, err = s.BulkDeploymentState(brA.ID)
	require.NoError(t, err)
	assert.Len(t, statuses, 4)
	for _, state := range statuses {
		switch state.Name {
		case "test1":
			assert.Equal(t, state.State.String(), Success.String())
			assert.NotZero(t, state.DeploymentId)
			d, err := dStorage.Get(state.DeploymentId)
			require.NoError(t, err)
			assert.Equal(t, "", d.Comment)
		case "test2":
			assert.Equal(t, state.State.String(), Success.String())
			assert.NotZero(t, state.DeploymentId)
		case "test4":
			assert.Equal(t, state.State.String(), Failed.String())
			assert.Equal(t, state.FailReason, "Service reverted, description: , revert type: Undefined")
			assert.NotZero(t, state.DeploymentId)
		case "test5":
			assert.Equal(t, state.State.String(), Canceled.String())
			assert.Zero(t, state.DeploymentId)
		}
	}
	assert.Equal(t, bulk.State_Canceled.String(), br.State.String())
	assert.Equal(t, bulk.State_Canceled.String(), extract(t, pMock.Get(t)).State.String())
	assert.Empty(t, pMock.Msg)
}

func TestService_AdminBulkRestartAll(t *testing.T) {
	test.InitTestEnv()
	db := test_db.NewSeparatedDb(t)

	statusStorage := status.NewStorage(db, test.NewLogger(t))
	schedulerMock := mock.NewMockScheduler()
	pMock := mqMock.NewProducerMock()
	s := newService(t, db, schedulerMock, dmock.NewProducerMock(), newConf(), pMock)

	for i := 1; i <= 2; i++ {
		runService(t, db, s, schedulerMock, statusStorage, fmt.Sprintf("test%d", i), "", common.Test, sMapYml)
	}

	brA, err := s.AdminBulkDeployment(common.Test, staff_fixture.Owner, "", "VOID-1", []string{}, false, true)
	require.NoError(t, err)

	br, statuses, err := s.BulkDeploymentState(brA.ID)
	require.NoError(t, err)

	for i := 1; i <= 2; i++ {
		schedulerMock.Success(t, common.Run, fmt.Sprintf("test%d", i), "", version)
	}

	WaitEnd(t, s, br)
	dStorage := model.NewStorage(db, test.NewLogger(t))

	br, statuses, err = s.BulkDeploymentState(brA.ID)
	require.NoError(t, err)

	for _, state := range statuses {
		assert.Equal(t, state.State.String(), Success.String())
		assert.NotZero(t, state.DeploymentId)
		d, err := dStorage.Get(state.DeploymentId)
		require.NoError(t, err)
		assert.Equal(t, "", d.Comment)
	}
}

func TestService_AdminBulkFailRestartAll(t *testing.T) {
	test.InitTestEnv()
	db := test_db.NewSeparatedDb(t)

	statusStorage := status.NewStorage(db, test.NewLogger(t))
	schedulerMock := mock.NewMockScheduler()
	pMock := mqMock.NewProducerMock()
	s := newService(t, db, schedulerMock, dmock.NewProducerMock(), newConf(), pMock)

	for i := 1; i <= 2; i++ {
		runService(t, db, s, schedulerMock, statusStorage, fmt.Sprintf("test%d", i), "", common.Test, sMapYml)
	}

	_, err := s.AdminBulkDeployment(common.Test, staff_fixture.Owner, "", "VOID-1", []string{}, false, false)
	assert.Equal(t, ErrServicesListIsEmpty, err)
}

func TestService_AdminBulkDeployment_WithIssue(t *testing.T) {
	test.InitTestEnv()
	db := test_db.NewSeparatedDb(t)

	statusStorage := status.NewStorage(db, test.NewLogger(t))
	schedulerMock := mock.NewMockScheduler()
	s := newService(t, db, schedulerMock, dmock.NewProducerMock(), newConf(), mqMock.NewProducerMock())
	services := []string{"test1", "test2"}
	for _, service := range services {
		runService(t, db, s, schedulerMock, statusStorage, service, "", common.Test, sMapYml)
	}

	newSMapAndManifest(t, db, services...)

	brA, err := s.AdminBulkDeployment(common.Test, staff_fixture.Owner, "", "VOID-1", services, false, false)
	require.NoError(t, err)
	br, _, err := s.BulkDeploymentState(brA.ID)
	require.NoError(t, err)
	schedulerMock.Success(t, common.Run, "test1", "", version)
	schedulerMock.Success(t, common.Run, "test2", "", version)
	WaitEnd(t, s, br)

	for _, service := range services {
		d := getDeployments(t, db, service)
		assert.Len(t, d, 2)
		assert.Len(t, d[0].Issues, 1)
		assert.Len(t, d[1].Issues, 0)
		assert.Equal(t, "VOID-1", d[0].Issues[0].Key)
	}
}

func TestService_AdminBulkDeployment_WithoutIssue(t *testing.T) {
	test.InitTestEnv()
	db := test_db.NewSeparatedDb(t)

	statusStorage := status.NewStorage(db, test.NewLogger(t))
	schedulerMock := mock.NewMockScheduler()
	pMock := mqMock.NewProducerMock()
	s := newService(t, db, schedulerMock, dmock.NewProducerMock(), newConf(), pMock)

	for i := 1; i <= 2; i++ {
		runService(t, db, s, schedulerMock, statusStorage, fmt.Sprintf("test%d", i), "", common.Test, sMapYml)
	}

	_, err := s.AdminBulkDeployment(common.Test, staff_fixture.Owner, "", "", []string{"test1"}, false, false)
	assert.Equal(t, ErrInvalidIssue, err)
}

func extract(t *testing.T, msg *mq.Message) *bulk.BulkRun {
	br := &bulk.BulkRun{}
	b := msg.Payload
	err := proto.Unmarshal(b, br)
	require.NoError(t, err)
	return br
}

// TODO delete time.sleep
func TestService_AdminBulkRestartSox(t *testing.T) {
	test.InitTestEnv()
	db := test_db.NewSeparatedDb(t)

	statusStorage := status.NewStorage(db, test.NewLogger(t))
	schedulerMock := mock.NewMockScheduler()
	dpMock := dmock.NewProducerMock()
	pMock := mqMock.NewProducerMock()
	s := newService(t, db, schedulerMock, dpMock, newConf(), pMock)

	for i := 1; i <= 5; i++ {
		runService(t, db, s, schedulerMock, statusStorage, fmt.Sprintf("test%d", i), "", common.Test, soxMapYml)
	}
	for i := 1; i <= 5; i++ {
		runService(t, db, s, schedulerMock, statusStorage, fmt.Sprintf("test%d", i), "", common.Prod, soxMapYml)
	}
	dpMock.Reset()

	brA, err := s.AdminBulkDeployment(common.Prod, staff_fixture.Owner, "", "VOID-1", []string{"test1", "test4", "test5"}, false, false)
	require.NoError(t, err)
	time.Sleep(time.Second)
	require.NoError(t, s.CancelBulkDeployment(staff_fixture.Owner, brA.ID))
	schedulerMock.Success(t, common.Run, "test1", "", version)
	schedulerMock.RevertSuccess(t, common.Run, "test4", "", version)
	time.Sleep(time.Second)

	dStorage := model.NewStorage(db, test.NewLogger(t))

	states := []*Status{}
	require.NoError(t, s.statusStorage.base.GetAll("", &states, ""))
	assert.Len(t, states, 3)
	for _, state := range states {
		switch state.Name {
		case "test1":
			assert.Equal(t, state.State.String(), Success.String())
			assert.NotZero(t, state.DeploymentId)
			d, err := dStorage.Get(state.DeploymentId)
			require.NoError(t, err)
			assert.Equal(t, "", d.Comment)
		case "test4":
			assert.Equal(t, state.State.String(), Failed.String())
			assert.Equal(t, state.FailReason, "Service reverted, description: , revert type: Undefined")
			assert.NotZero(t, state.DeploymentId)
		case "test5":
			assert.Equal(t, state.State.String(), Canceled.String())
			assert.Zero(t, state.DeploymentId)
		}
	}
	br, err := s.storage.Get(brA.ID)
	require.NoError(t, err)
	assert.Equal(t, bulk.State_Canceled.String(), br.State.String())
	assert.Equal(t, bulk.State_Canceled.String(), extract(t, pMock.Get(t)).State.String())
	assert.Empty(t, pMock.Msg)
}

func TestAllow(t *testing.T) {
	test.InitTestEnv()
	db := test_db.NewSeparatedDb(t)

	bulkRestarters := map[string]struct{}{staff_fixture.Owner: {}, "avkosorukov": {}}
	notBulkRestarters := []string{"someUser", "anotherUser"}

	statusStorage := status.NewStorage(db, test.NewLogger(t))
	schedulerMock := mock.NewMockScheduler()
	pMock := mqMock.NewProducerMock()
	conf := newConf()
	conf.Admins = bulkRestarters
	s := newService(t, db, schedulerMock, dmock.NewProducerMock(), conf, pMock)

	for i := 0; i < 5; i++ {
		runService(t, db, s, schedulerMock, statusStorage, fmt.Sprintf("test%d", i), "", common.Test, sMapYml)
	}

	for _, login := range notBulkRestarters {
		_, err := s.AdminBulkDeployment(common.Test, login, "", "VOID-1", []string{}, false, false)
		assert.Equal(t, ErrOperationNotPermitted, err)
	}

	i := 0
	for login := range bulkRestarters {
		_, err := s.AdminBulkDeployment(common.Test, login, "", "VOID-1", []string{"test1"}, false, false)
		if i == 0 {
			require.NoError(t, err)
		} else {
			assert.Equal(t, ErrOperationIsAlreadyRunning, err)
		}
		i++
	}

	test.Wait(t, func() error {
		_, st, err := s.BulkDeploymentState(0)
		if err != nil {
			return err
		}
		if len(st) != 1 {
			return errors.New("too many restarts")
		}
		if st[0].State == NotStarted {
			return errors.New("not started")
		}
		assert.Empty(t, pMock.Msg)
		return nil
	})
	schedulerMock.Success(t, common.Run, "test1", "", version)
	test.Wait(t, func() error {
		br, st, err := s.BulkDeploymentState(0)
		if err != nil {
			return err
		}
		if len(st) != 1 {
			return errors.New("too many restarts")
		}
		if !st[0].State.IsFinished() {
			return errors.New("deploy not finished")
		}
		if !br.State.IsFinished() {
			return errors.New("bulk deploy not finished")
		}
		return nil
	})
	br, st, err := s.BulkDeploymentState(0)
	require.NoError(t, err)
	assert.Len(t, st, 1)
	assert.Equal(t, st[0].State, Success)
	assert.Equal(t, bulk.State_Success.String(), br.State.String())
	assert.Equal(t, bulk.State_Success.String(), extract(t, pMock.Get(t)).State.String())
	assert.Empty(t, pMock.Msg)
}

func TestServiceBranchSet(t *testing.T) {
	set := make(ServiceBranchSet)
	set.Add(ServiceBranch{
		Service: "1",
		Branch:  "",
	})
	set.Add(ServiceBranch{
		Service: "2",
		Branch:  "",
	})
	set.Add(ServiceBranch{
		Service: "2",
		Branch:  "1",
	})

	set.Add(ServiceBranch{
		Service: "1",
		Branch:  "",
	})
	set.Add(ServiceBranch{
		Service: "2",
		Branch:  "1",
	})
	set.Add(ServiceBranch{
		Service: "2",
		Branch:  "",
	})

	set.Add(ServiceBranch{
		Service: "5",
		Branch:  "",
	})
	assert.Len(t, set, 4)
}

// TODO simplify me please
func TestReconnectProcess(t *testing.T) {
	test.InitTestEnv()
	db := test_db.NewSeparatedDb(t)

	statusStorage := status.NewStorage(db, test.NewLogger(t))
	schedulerMock := mock.NewMockScheduler()
	pMock := mqMock.NewProducerMock()
	s := newService(t, db, schedulerMock, dmock.NewProducerMock(), newConf(), pMock)
	for i := 0; i < 5; i++ {
		runService(t, db, s, schedulerMock, statusStorage, fmt.Sprintf("test%d", i), "", common.Test, sMapYml)
	}

	brA, err := s.AdminBulkDeployment(common.Test, staff_fixture.Owner, "", "VOID-1", []string{}, false, true)
	require.NoError(t, err)
	time.Sleep(time.Second)
	states := []*Status{}
	var firstName string
	var secondName string
	for firstName == "" {
		require.NoError(t, s.statusStorage.base.GetAll("", &states, ""))
		for _, state := range states {
			if state.State == Started {
				firstName = state.Name
			}
		}
		time.Sleep(time.Second / 4)
	}

	for secondName == "" {
		require.NoError(t, s.statusStorage.base.GetAll("", &states, ""))
		for _, state := range states {
			if state.State == Started && state.Name != firstName {
				secondName = state.Name
			}
		}
		time.Sleep(time.Second / 4)
	}

	s.Locker.(*SingleLocker).clearLocksAndReconnect()
	time.Sleep(time.Second)
	schedulerMock.Success(t, common.Run, firstName, "", version)
	schedulerMock.Success(t, common.Run, secondName, "", version)

	time.Sleep(time.Second)
	require.NoError(t, s.statusStorage.base.GetAll("", &states, ""))
	for _, state := range states {
		if state.Name == firstName || state.Name == secondName {
			assert.Equal(t, Started.String(), state.State.String())
		}
	}

	dStorage := model.NewStorage(db, test.NewLogger(t))

	// TODO need rewrite magic block
	for finishedCount := 0; finishedCount < 3; {
		require.NoError(t, s.statusStorage.base.GetAll("", &states, ""))
		for _, state := range states {

			version := version
			if state.Name == firstName || state.Name == secondName {
				d, err := dStorage.Get(state.DeploymentId)
				require.NoError(t, err)
				version = fmt.Sprintf("id:%d", d.NomadID)
			}

			if schedulerMock.CheckKey(common.Run, state.Name, "", version) {
				schedulerMock.Success(t, common.Run, state.Name, "", version)
				finishedCount++
			}
		}
		time.Sleep(time.Second)
	}

	time.Sleep(time.Second)
	require.NoError(t, s.statusStorage.base.GetAll("", &states, ""))
	for _, state := range states {
		assert.Equal(t, Success.String(), state.State.String())
	}
	br, err := s.storage.Get(brA.ID)
	require.NoError(t, err)
	assert.Equal(t, bulk.State_Success.String(), br.State.String())
	assert.Equal(t, bulk.State_Success.String(), extract(t, pMock.Get(t)).State.String())
	assert.Empty(t, pMock.Msg)
}

func TestService_AdminBulkRunActualConfiguration(t *testing.T) {
	test.InitTestEnv()
	db := test_db.NewSeparatedDb(t)

	statusStorage := status.NewStorage(db, test.NewLogger(t))
	schedulerMock := mock.NewMockScheduler()
	s := newService(t, db, schedulerMock, dmock.NewProducerMock(), newConf(), mqMock.NewProducerMock())
	services := []string{"test1", "test2"}
	for _, service := range services {
		runService(t, db, s, schedulerMock, statusStorage, service, "", common.Test, sMapYml)
	}

	newSMapAndManifest(t, db, services...)

	brA, err := s.AdminBulkDeployment(common.Test, staff_fixture.Owner, "", "VOID-1", services, true, false)
	require.NoError(t, err)
	br, _, err := s.BulkDeploymentState(brA.ID)
	require.NoError(t, err)
	schedulerMock.Success(t, common.Run, "test1", "", version)
	schedulerMock.Success(t, common.Run, "test2", "", version)
	WaitEnd(t, s, br)

	for _, service := range services {
		d := getDeployments(t, db, service)
		assert.Len(t, d, 2)
		assert.NotEqual(t, d[0].DeployManifestID, d[1].DeployManifestID)
		assert.NotEqual(t, d[0].ServiceMapsID, d[1].ServiceMapsID)
	}
}

func TestService_AdminBulkRunParentManifest(t *testing.T) {
	test.InitTestEnv()
	db := test_db.NewSeparatedDb(t)

	statusStorage := status.NewStorage(db, test.NewLogger(t))
	schedulerMock := mock.NewMockScheduler()
	s := newService(t, db, schedulerMock, dmock.NewProducerMock(), newConf(), mqMock.NewProducerMock())
	services := []string{"test1", "test2"}
	for _, service := range services {
		runService(t, db, s, schedulerMock, statusStorage, service, "", common.Test, sMapYml)
	}

	newSMapAndManifest(t, db, services...)

	brA, err := s.AdminBulkDeployment(common.Test, staff_fixture.Owner, "", "VOID-1", services, false, false)
	require.NoError(t, err)
	br, _, err := s.BulkDeploymentState(brA.ID)
	require.NoError(t, err)
	schedulerMock.Success(t, common.Run, "test1", "", version)
	schedulerMock.Success(t, common.Run, "test2", "", version)
	WaitEnd(t, s, br)

	for _, service := range services {
		d := getDeployments(t, db, service)
		assert.Len(t, d, 2)
		assert.Equal(t, d[0].DeployManifestID, d[1].DeployManifestID)
		assert.Equal(t, d[0].ServiceMapsID, d[1].ServiceMapsID)
	}
}

func getDeployments(t *testing.T, db *storage.Database, name string) []*model.Deployment {
	dStorage := model.NewStorage(db, test.NewLogger(t))
	d, err := dStorage.ListByNameAndStates(true, name, []model.State{model.Success})
	require.NoError(t, err)
	return d
}

func newSMapAndManifest(t *testing.T, db *storage.Database, names ...string) {
	log := test.NewLogger(t)

	sMapService := service_map.NewService(db, test.NewLogger(t), service_change.NewNotificationMock())
	manifestService := manifest.NewService(db, log, parser.NewService(log, reader.NewService(db, log)), include.NewService(db, log))

	for _, name := range names {
		result := fmt.Sprintf(newManifestYml, name)
		require.NoError(t, manifestService.ReadAndSave([]byte(result), test.AtomicNextUint(), ""))
		result = fmt.Sprintf(newSMapYml, name)
		path := fmt.Sprintf(sMapPath, name)
		require.NoError(t, sMapService.ReadAndSave([]byte(result), test.AtomicNextUint(), path))
	}
}

func runService(t *testing.T, db *storage.Database, s *Service, scheduler *mock.Scheduler, statusStorage *status.Storage, name, branch string, layer common.Layer, smap string) {
	log := test.NewLogger(t)
	includeService := include.NewService(db, log)
	notifySvc := service_change.NewNotificationMock()
	sMapservice := service_map.NewService(db, test.NewLogger(t), notifySvc)
	manifestService := manifest.NewService(db, log, parser.NewService(log, reader.NewService(db, log)), includeService)
	result := fmt.Sprintf(manifestYml, name)
	require.NoError(t, manifestService.ReadAndSave([]byte(result), test.AtomicNextUint(), ""))
	result = fmt.Sprintf(smap, name)
	path := fmt.Sprintf(sMapPath, name)
	require.NoError(t, sMapservice.ReadAndSave([]byte(result), test.AtomicNextUint(), path))

	runC, dctx, err := s.DeploySvc.Run(context.Background(), model.RunParams{
		Layer:   layer,
		Name:    name,
		Version: version,
		Branch:  branch,
		Login:   staff_fixture.Owner,
	})
	require.NoError(t, err)
	scheduler.Success(t, common.Run, name, branch, version)
	for range runC {
	}
	dctx.Deployment.State = model.Success
	require.NoError(t, statusStorage.Save(contextToStatus(dctx)))
}

func contextToStatus(dctx *dcontext.Context) *status.Status {

	state := status.StateRunning
	if dctx.Deployment.Type == common.Stop {
		state = status.StateNotRunning
	}
	newServiceStatus := &status.Status{
		DeploymentID: dctx.Deployment.ID,
		State:        state,
		Layer:        dctx.Deployment.Layer,
		Name:         dctx.Deployment.Name,
		Branch:       dctx.Deployment.Branch,
		Version:      dctx.Deployment.Version,
	}
	return newServiceStatus
}

func newService(t *testing.T, db *storage.Database, scheduler *mock.Scheduler, changedState notification.ChangedState, conf Conf, bdProducer mqMock.ProducerMock) *Service {
	log := test.NewLogger(t)

	flagSvc := feature_flags.NewService(db, mqMock.NewProducerMock(), log)
	deployDataSvc := data.NewService(db, log)
	trackerService := tracker.NewService(db, log, stAPI.NewApi(stAPI.NewConf(), log), deployDataSvc)
	includeSvc := include.NewService(db, log)
	envReader := reader.NewService(db, log)
	manifestSvc := manifest.NewService(db, log, parser.NewService(log, envReader), includeSvc)
	staffApi := staffapi.NewApi(staffapi.NewConf(), log)
	staffService := staff.NewService(db, staffApi, log)
	approveSvc := approve.NewService(db, log, staffService)
	registrySvc := registry.NewService(mock.NewRegistry(false))
	lockerMock := dmock.NewDMockLocker()
	mapSvc := service_map.NewService(db, log, service_change.NewNotificationMock())
	statusSvc := status.NewService(db, log, mapSvc)
	issueLinkSvc := issue_link.NewService(db, log, trackerService)
	kvMock := mock.NewKVMock()
	syncSvc := cSync.NewService(log, kvMock, cSync.NewConf(t.Name()))
	require.NoError(t, syncSvc.SetLastUpdate(time.Now().Add(time.Hour)))
	drillsSvc := drills.NewService(log, db)
	scaleSvc := scaler.NewService(deployDataSvc, manifestSvc, &mock.PrometheusApi{}, &mock.PrometheusApi{}, scaler.Conf{}, db, log)
	overrideSvc := override.NewService(db, log, includeSvc, manifestSvc)
	ssCliMock := &mocks.SecretClient{}
	ssCliMock.On("GetSecrets", mock2.Anything, mock2.Anything).Return(nil, nil)
	secretSvc := secret.NewService(log, ssCliMock)
	ssMock := mock.NewAccessClientMock(t)
	templateSvc := template.NewService(mapSvc, envReader)

	deploySvc := &deployment.Service{
		Conf:         deployment.NewConf(),
		DB:           db,
		Log:          log,
		ChangedState: changedState,
		Locker:       lockerMock,
		TestNomad:    scheduler,
		ProdNomad:    scheduler,
		TestYaDeploy: scheduler,
		ProdYaDeploy: scheduler,
		TestBatch:    nil,
		ProdBatch:    nil,
		FlagSvc:      flagSvc,
		StaffSvc:     staffService,
		IncludeSvc:   includeSvc,
		ApproveSvc:   approveSvc,
		Registry:     registrySvc,
		SyncSvc:      syncSvc,
		TrackerSvc:   trackerService,
		ScalerSvc:    scaleSvc,
		Drills:       drillsSvc,
		MapSvc:       mapSvc,
		ManifestSvc:  manifestSvc,
		IssueLinkSvc: issueLinkSvc,
		OverrideSvc:  overrideSvc,
		SecretSvc:    secretSvc,
		EnvResolver:  env_resolver.NewService(templateSvc, secrets.NewService(secrets.NewConf(0), ssMock, log)),
	}
	deploySvc.Init()
	return (&Service{
		Conf:      conf,
		DB:        db,
		Log:       test.NewLogger(t),
		DeploySvc: deploySvc,
		StatusSvc: statusSvc,
		Locker:    NewLocker(),
		Producer:  bdProducer,
	}).Init()
}

type SingleLocker struct {
	locks          map[int64]*LockContext
	reconnectChan  chan struct{}
	lockExpireChan chan struct{}
	mx             sync.Mutex
}

type Lock struct {
	c   chan struct{}
	ctx locker.Context
}

func (l *Lock) IsExpired() bool {
	select {
	case <-l.Expired():
		return true
	default:
		return false
	}
}

func (l *Lock) Expired() <-chan struct{} {
	return l.c
}

func (l *Lock) GetContext() locker.Context {
	return l.ctx
}

func NewLocker() *SingleLocker {
	return &SingleLocker{
		locks:          map[int64]*LockContext{},
		lockExpireChan: make(chan struct{}),
		mx:             sync.Mutex{},
	}
}

func (s *SingleLocker) Lock(ctx locker.Context) (locker.Lock, error) {
	s.mx.Lock()
	defer s.mx.Unlock()
	lCtx, ok := ctx.(*LockContext)
	if !ok {
		return nil, fmt.Errorf("fail cast context type")
	}
	if _, ok := s.locks[lCtx.ID]; ok {
		return nil, locker.ErrLockAlreadyExist
	}
	s.locks[lCtx.ID] = lCtx
	return &Lock{
		c:   s.lockExpireChan,
		ctx: ctx,
	}, nil
}

func (s *SingleLocker) Unlock(lock locker.Lock) error {
	s.mx.Lock()
	defer s.mx.Unlock()
	lCtx, ok := lock.GetContext().(*LockContext)
	if !ok {
		return fmt.Errorf("fail cast context type")
	}
	if err := s.checkLock(lock.GetContext()); err != nil {
		return err
	}
	delete(s.locks, lCtx.ID)
	return nil
}

func (s *SingleLocker) checkLock(ctx locker.Context) error {
	lCtx, ok := ctx.(*LockContext)
	if !ok {
		return fmt.Errorf("fail cast context type")
	}
	oldLock, ok := s.locks[lCtx.ID]
	if !ok {
		return common.ErrNotFound
	}
	if !ctx.Compare(oldLock) {
		return locker.ErrLockAlreadyExist
	}
	return nil
}

func (s *SingleLocker) Steal(locker.Context) (locker.Lock, error) {
	panic("implement me")
}

func (s *SingleLocker) Stop() {
	panic("implement me")
}

func (s *SingleLocker) clearLocksAndReconnect() {

	s.locks = map[int64]*LockContext{}
	oldLock := s.lockExpireChan
	s.lockExpireChan = make(chan struct{})
	close(oldLock)
}
