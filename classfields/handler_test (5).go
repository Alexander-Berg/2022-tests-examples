package deploy

import (
	"context"
	"fmt"
	"testing"
	"time"

	"github.com/YandexClassifieds/shiva/pkg/secrets"
	"github.com/YandexClassifieds/shiva/pkg/template"

	"github.com/YandexClassifieds/shiva/cmd/shiva/approve"
	"github.com/YandexClassifieds/shiva/cmd/shiva/deployment"
	"github.com/YandexClassifieds/shiva/cmd/shiva/deployment/data"
	"github.com/YandexClassifieds/shiva/cmd/shiva/deployment/issue_link"
	dm "github.com/YandexClassifieds/shiva/cmd/shiva/deployment/model"
	ds "github.com/YandexClassifieds/shiva/cmd/shiva/deployment/status"
	"github.com/YandexClassifieds/shiva/cmd/shiva/drills"
	"github.com/YandexClassifieds/shiva/cmd/shiva/env/external/reader"
	env_resolver "github.com/YandexClassifieds/shiva/cmd/shiva/env/manifest"
	"github.com/YandexClassifieds/shiva/cmd/shiva/env/override"
	"github.com/YandexClassifieds/shiva/cmd/shiva/scaler"
	"github.com/YandexClassifieds/shiva/common"
	"github.com/YandexClassifieds/shiva/common/logger"
	"github.com/YandexClassifieds/shiva/common/storage"
	dpb "github.com/YandexClassifieds/shiva/pb/shiva/api/deploy"
	sm "github.com/YandexClassifieds/shiva/pb/shiva/service_map"
	"github.com/YandexClassifieds/shiva/pb/shiva/types/dtype"
	"github.com/YandexClassifieds/shiva/pb/shiva/types/layer"
	"github.com/YandexClassifieds/shiva/pb/shiva/types/revert"
	"github.com/YandexClassifieds/shiva/pb/shiva/types/state"
	"github.com/YandexClassifieds/shiva/pb/shiva/types/traffic"
	"github.com/YandexClassifieds/shiva/pkg/consul/sync"
	"github.com/YandexClassifieds/shiva/pkg/feature_flags"
	"github.com/YandexClassifieds/shiva/pkg/include"
	"github.com/YandexClassifieds/shiva/pkg/manifest"
	mm "github.com/YandexClassifieds/shiva/pkg/manifest/model"
	"github.com/YandexClassifieds/shiva/pkg/manifest/parser"
	"github.com/YandexClassifieds/shiva/pkg/registry"
	"github.com/YandexClassifieds/shiva/pkg/secrets/secret"
	"github.com/YandexClassifieds/shiva/pkg/service_map"
	"github.com/YandexClassifieds/shiva/pkg/staff"
	staffapi "github.com/YandexClassifieds/shiva/pkg/staff/api"
	"github.com/YandexClassifieds/shiva/pkg/tracker"
	stAPI "github.com/YandexClassifieds/shiva/pkg/tracker/api"
	"github.com/YandexClassifieds/shiva/pkg/tracker/model/issue"
	"github.com/YandexClassifieds/shiva/test"
	smock "github.com/YandexClassifieds/shiva/test/mock"
	dmock "github.com/YandexClassifieds/shiva/test/mock/deployment"
	"github.com/YandexClassifieds/shiva/test/mock/mockery/mocks"
	"github.com/YandexClassifieds/shiva/test/mock/mq"
	"github.com/YandexClassifieds/shiva/test/mock/service_change"
	"github.com/YandexClassifieds/shiva/test/test_db"
	"github.com/golang/protobuf/proto"
	"github.com/pkg/errors"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/mock"
	"github.com/stretchr/testify/require"
	"google.golang.org/grpc"
	"google.golang.org/grpc/codes"
	"google.golang.org/grpc/status"
	"gopkg.in/yaml.v2"
)

const (
	sName        = "yandex_vertis_example_service_d"
	sVersion     = "1.0.0"
	testManifest = `
name: %s
image: %s

general:
  datacenters:
    sas:
      count: 1
    myt:
      count: 1
    memory: 256
`
	testMap = `
name: %s
description: Deployment system
is_external: false
owner: https://staff.yandex-team.ru/departments/yandex_personal_vertserv_infra
design_doc:
src:  https://github.com/YandexClassifieds/shiva
sox: %v
provides:
  - name: deploy
    protocol: grpc
    port: 80
    description: Основное апи для управления деплоем
    api_doc: https://wiki.yandex-team.ru/vertis-admin/shiva/
`
	depMap = `
name: depService
description: Deployment system
is_external: false
owner: https://staff.yandex-team.ru/departments/yandex_personal_vertserv_infra
design_doc:
src:  https://github.com/YandexClassifieds/shiva
provides:
  - name: deploy
    protocol: grpc
    port: 80
    description: Основное апи для управления деплоем
    api_doc: https://wiki.yandex-team.ru/vertis-admin/shiva/
depends_on:
  - service: yandex_vertis_example_service_d
    interface_name: deploy
    expected_rps: 100
    failure_reaction:
      missing: fatal
      timeout: severe
      unexpected_result: severe
      errors: fatal
`
)

func prepare(t *testing.T) (*Handler, *storage.Database, *smock.Scheduler, logger.Logger) {
	db := test_db.NewDb(t)
	mScheduler := smock.NewMockScheduler()
	log := test.NewLogger(t)
	deploymentService := newDeploymentService(t, db, mScheduler)
	deploymentDataSrv := data.NewService(db, log)
	mapService := service_map.NewService(db, log, service_change.NewNotificationMock())
	statusSvc := ds.NewService(db, log, mapService)
	featureFlagsService := feature_flags.NewService(db, mq.NewProducerMock(), log)
	issueLinkS := issue_link.NewService(db, log, tracker.NewService(db, log, stAPI.NewApi(stAPI.NewConf(), log), deploymentDataSrv))
	extEnv := reader.NewService(db, log)
	staffApi := staffapi.NewApi(staffapi.NewConf(), log)
	staffService := staff.NewService(db, staffApi, log)
	deployServer := NewHandler(
		extEnv,
		deploymentService,
		deploymentDataSrv,
		statusSvc,
		featureFlagsService, mapService,
		staffService, issueLinkS, log)

	return deployServer, db, mScheduler, log
}

func TestDeploy_RunWithBrokenTg(t *testing.T) {
	test.RunUp(t)
	defer test.Down(t)

	deployServer, db, mScheduler, _ := prepare(t)
	makeManifest(t, testManifest, sName)
	makeMap(t, testMap, sName, false)

	mockStream := newMock()
	mockStream.On("Send", mock.Anything).Return(errors.New("Broken mock error"))

	protoRunRequest := &dpb.RunRequest{
		Login:        "danevge",
		LayerEnum:    layer.Layer_TEST,
		Name:         sName,
		Version:      sVersion,
		Issues:       []string{"VOID-216"},
		Branch:       "",
		TrafficShare: false,
	}

	finishedDeployment := make(chan struct{})
	go func() {
		/* This loop need for waiting deployServer.Run below
		where will be created channel in scheduler mock */
		for {
			_, err := getRecordFromDB(db, sName, sVersion)
			if err == nil {
				break
			}
		}

		for i := 0; i < 15; i++ {
			mScheduler.Process(t, common.Run, sName, "", sVersion)
		}
		mScheduler.Success(t, common.Run, sName, "", sVersion)
		finishedDeployment <- struct{}{}
	}()
	err := deployServer.Run(protoRunRequest, mockStream)
	assert.Error(t, err)

	// This select need for sure that all processes in deployment emulation goroutine have finished
	select {
	case <-finishedDeployment:
	case <-time.After(3 * time.Second):
		assert.FailNow(t, "out of time")
	}

	// Checking state in DB as prove that deployment onEnd have worked up
	test.Wait(t, func() error {
		return waitSuccess(db, sName, sVersion)
	})
}

func TestDeploy_State_Retry(t *testing.T) {
	test.RunUp(t)
	defer test.Down(t)

	srv, db, _, _ := prepare(t)

	// test retries on db read errors
	db.GormDb.AddError(errors.New("test db error"))
	mockStream := new(mockServerStream)
	mockStream.On("Send", mock.Anything).Return(nil)
	err := srv.State(&dpb.StateRequest{Login: "vasya", Id: 12}, mockStream)
	s, _ := status.FromError(err)
	assert.Equal(t, codes.Unavailable.String(), s.Code().String())
}

func TestStateToProto(t *testing.T) {
	assert.Equal(t, dpb.DeployState_IN_PROGRESS, stateToProto(&deployment.StateChange{DState: dm.Process}))
	assert.Equal(t, dpb.DeployState_UNKNOWN, stateToProto(&deployment.StateChange{DState: dm.Undefined}))
}

func TestDeploy_Status(t *testing.T) {
	test.RunUp(t)

	srv, db, _, log := prepare(t)
	staffApi := staffapi.NewApi(staffapi.NewConf(), log)
	staffService := staff.NewService(db, staffApi, log)
	user, err := staffService.GetByLogin("danevge")
	require.NoError(t, err)
	dStore := dm.NewStorage(db, log)
	statusStore := ds.NewStorage(db, log)
	_, mID := makeManifest(t, testManifest, sName)
	_, smID := makeMap(t, testMap, sName, false)
	d := &dm.Deployment{
		State:            dm.Success,
		Layer:            common.Test,
		Name:             "status-test-svc",
		Version:          "v42",
		Branch:           "testbranch42",
		Traffic:          traffic.Traffic_ONE_PERCENT,
		ServiceMapsID:    smID,
		DeployManifestID: mID,
		AuthorID:         user.ID,
	}
	require.NoError(t, dStore.Save(d))
	err = statusStore.Save(&ds.Status{
		DeploymentID: 2,
		State:        ds.StateRunning,
		Layer:        common.Test,
		Name:         "status-test-svc",
		Version:      "v42",
		Branch:       "testbranch42",
		Traffic:      traffic.Traffic_ONE_PERCENT,
	})

	err = statusStore.Save(&ds.Status{
		DeploymentID: d.ID,
		State:        ds.StateRunning,
		Layer:        common.Test,
		Name:         "status-test-svc",
		Version:      "v42",
		Branch:       "testbranch42",
		Traffic:      traffic.Traffic_ONE_PERCENT,
	})
	require.NoError(t, err)

	r, err := srv.Status(context.Background(), &dpb.StatusRequest{Service: "status-test-svc", Login: "alexander-s"})
	require.NoError(t, err)
	assert.Len(t, r.Info, 1)

	_, err = proto.Marshal(r)
	require.NoError(t, err)
}

func TestDeploy_AllStatus(t *testing.T) {
	test.RunUp(t)

	srv, db, _, log := prepare(t)

	dstore := ds.NewStorage(db, log)
	err := dstore.Save(&ds.Status{
		State:   ds.StateRunning,
		Layer:   common.Test,
		Name:    "status-test-svc",
		Version: "v42",
		Traffic: traffic.Traffic_ONE_PERCENT,
	})
	require.NoError(t, err)

	err = dstore.Save(&ds.Status{
		State:   ds.StateNotRunning,
		Layer:   common.Test,
		Name:    "status-test-svc",
		Version: "v42",
		Branch:  "testbranch42",
		Traffic: traffic.Traffic_ONE_PERCENT,
	})
	require.NoError(t, err)

	err = dstore.Save(&ds.Status{
		State:   ds.StateRunning,
		Layer:   common.Prod,
		Name:    "status-test-svc",
		Version: "v42",
		Branch:  "testbranch42",
		Traffic: traffic.Traffic_ONE_PERCENT,
	})
	require.NoError(t, err)

	r, err := srv.AllStatus(context.Background(), &dpb.AllStatusRequest{})
	require.NoError(t, err)
	assert.Len(t, r.Info, 2)

	_, err = proto.Marshal(r)
	require.NoError(t, err)
}

func TestHandler_ReleaseHistory_Deprecated(t *testing.T) {
	test.RunUp(t)

	srv, db, _, log := prepare(t)
	//db.GormDb.AutoMigrate(&dm.Deployment{})

	store := dm.NewStorage(db, log)
	staffApi := staffapi.NewApi(staffapi.NewConf(), log)
	staffService := staff.NewService(db, staffApi, log)
	user, err := staffService.GetByLogin("danevge")
	require.NoError(t, err)
	require.NoError(t, store.Save(&dm.Deployment{Name: "test-svc", AuthorID: user.ID}))

	req := &dpb.ReleaseHistoryRequest{Name: "test-svc"}
	res, err := srv.ReleaseHistory(context.Background(), req)
	require.NoError(t, err)
	require.Len(t, res.Info, 1)
}

func TestHandler_ReleaseHistory_Simple(t *testing.T) {
	test.RunUp(t)

	srv, db, _, log := prepare(t)

	store := dm.NewStorage(db, log)
	staffApi := staffapi.NewApi(staffapi.NewConf(), log)
	staffService := staff.NewService(db, staffApi, log)
	user, err := staffService.GetByLogin("danevge")
	require.NoError(t, err)
	require.NoError(t, store.Save(&dm.Deployment{Name: "test-svc", AuthorID: user.ID}))

	req := &dpb.ReleaseHistoryRequest{
		ServiceFilter: &dpb.ReleaseHistoryRequest_ServiceNames_{
			ServiceNames: &dpb.ReleaseHistoryRequest_ServiceNames{
				Names: []string{"test-svc"},
			},
		},
	}
	res, err := srv.ReleaseHistory(context.Background(), req)
	require.NoError(t, err)
	require.Len(t, res.Info, 1)
}

func TestHandler_ReleaseHistory_Revert(t *testing.T) {
	test.RunUp(t)

	srv, db, _, log := prepare(t)

	store := dm.NewStorage(db, log)
	staffApi := staffapi.NewApi(staffapi.NewConf(), log)
	staffService := staff.NewService(db, staffApi, log)
	user, err := staffService.GetByLogin("danevge")
	require.NoError(t, err)
	require.NoError(t, store.Save(&dm.Deployment{
		Name:       "test-svc",
		AuthorID:   user.ID,
		RevertType: revert.RevertType_Terminate,
	}))

	req := &dpb.ReleaseHistoryRequest{
		ServiceFilter: &dpb.ReleaseHistoryRequest_ServiceNames_{
			ServiceNames: &dpb.ReleaseHistoryRequest_ServiceNames{
				Names: []string{"test-svc"},
			},
		},
	}
	res, err := srv.ReleaseHistory(context.Background(), req)
	require.NoError(t, err)
	require.Len(t, res.Info, 1)
	assert.Equal(t, revert.RevertType_Terminate, res.Info[0].RevertType)
}

func TestHandler_ReleaseHistory_EmptyTypeRequest(t *testing.T) {
	test.RunUp(t)

	srv, db, _, log := prepare(t)

	store := dm.NewStorage(db, log)
	staffApi := staffapi.NewApi(staffapi.NewConf(), log)
	staffService := staff.NewService(db, staffApi, log)
	user, err := staffService.GetByLogin("danevge")
	require.NoError(t, err)
	saveDifferentTypeDeployments(t, user, store)

	req := &dpb.ReleaseHistoryRequest{
		ServiceFilter: &dpb.ReleaseHistoryRequest_ServiceNames_{
			ServiceNames: &dpb.ReleaseHistoryRequest_ServiceNames{
				Names: []string{"test-svc"},
			},
		},
	}
	res, err := srv.ReleaseHistory(context.Background(), req)
	require.NoError(t, err)
	require.Len(t, res.Info, 6)
}

func TestHandler_ReleaseHistory_TypeRequest(t *testing.T) {
	test.RunUp(t)

	srv, db, _, log := prepare(t)

	store := dm.NewStorage(db, log)
	staffApi := staffapi.NewApi(staffapi.NewConf(), log)
	staffService := staff.NewService(db, staffApi, log)
	user, err := staffService.GetByLogin("danevge")
	require.NoError(t, err)
	saveDifferentTypeDeployments(t, user, store)

	req := &dpb.ReleaseHistoryRequest{
		ServiceFilter: &dpb.ReleaseHistoryRequest_ServiceNames_{
			ServiceNames: &dpb.ReleaseHistoryRequest_ServiceNames{
				Names: []string{"test-svc"},
			},
		},
		Type: []dtype.DeploymentType{dtype.DeploymentType_RUN, dtype.DeploymentType_PROMOTE},
	}
	res, err := srv.ReleaseHistory(context.Background(), req)
	require.NoError(t, err)
	require.Len(t, res.Info, 2)
}

func TestHandler_ReleaseHistory_ZeroReturn(t *testing.T) {
	test.RunUp(t)

	srv, db, _, log := prepare(t)

	store := dm.NewStorage(db, log)
	staffApi := staffapi.NewApi(staffapi.NewConf(), log)
	staffService := staff.NewService(db, staffApi, log)
	user, err := staffService.GetByLogin("danevge")
	require.NoError(t, err)
	require.NoError(t, store.Save(&dm.Deployment{Name: "test-svc", AuthorID: user.ID}))

	req := &dpb.ReleaseHistoryRequest{
		ServiceFilter: &dpb.ReleaseHistoryRequest_ServiceNames_{
			ServiceNames: &dpb.ReleaseHistoryRequest_ServiceNames{
				Names: []string{"non-exist"},
			},
		},
	}
	res, err := srv.ReleaseHistory(context.Background(), req)
	require.NoError(t, err)
	require.Len(t, res.Info, 0)
}

func TestHandler_ReleaseHistory_OwnFilter(t *testing.T) {
	test.RunUp(t)

	srv, db, _, log := prepare(t)
	makeMap(t, testMap, sName, false)

	store := dm.NewStorage(db, log)
	staffApi := staffapi.NewApi(staffapi.NewConf(), log)
	staffService := staff.NewService(db, staffApi, log)
	user, err := staffService.GetByLogin("danevge")
	require.NoError(t, err)
	require.NoError(t, store.Save(&dm.Deployment{Name: sName, AuthorID: user.ID}))

	req := &dpb.ReleaseHistoryRequest{
		ServiceFilter: &dpb.ReleaseHistoryRequest_ByOwner{ByOwner: "alexander-s"},
	}
	res, err := srv.ReleaseHistory(context.Background(), req)
	require.NoError(t, err)
	require.Len(t, res.Info, 1)
}

func TestHandler_ReleaseHistory_DependsFilter(t *testing.T) {
	test.RunUp(t)

	srv, db, _, log := prepare(t)
	makeMap(t, testMap, sName, false)
	makePlainMap(t, depMap)

	store := dm.NewStorage(db, log)
	staffApi := staffapi.NewApi(staffapi.NewConf(), log)
	staffService := staff.NewService(db, staffApi, log)
	user, err := staffService.GetByLogin("danevge")
	require.NoError(t, err)
	require.NoError(t, store.Save(&dm.Deployment{Name: sName, AuthorID: user.ID}))
	require.NoError(t, store.Save(&dm.Deployment{Name: "depService", AuthorID: user.ID}))

	req := &dpb.ReleaseHistoryRequest{
		ServiceFilter: &dpb.ReleaseHistoryRequest_ServiceNames_{
			ServiceNames: &dpb.ReleaseHistoryRequest_ServiceNames{
				Names: []string{"depService"},
			},
		},
		IncludeDepends: true,
	}
	res, err := srv.ReleaseHistory(context.Background(), req)
	require.NoError(t, err)
	require.Len(t, res.Info, 2)
}

func TestHandler_ReleaseHistory_Failed(t *testing.T) {
	test.RunUp(t)

	srv, db, _, log := prepare(t)

	store := dm.NewStorage(db, log)
	staffApi := staffapi.NewApi(staffapi.NewConf(), log)
	staffService := staff.NewService(db, staffApi, log)
	user, err := staffService.GetByLogin("danevge")
	require.NoError(t, err)
	require.NoError(t, store.Save(&dm.Deployment{
		Name:     "test-svc",
		AuthorID: user.ID,
		State:    dm.ValidateFail,
	}))

	req := &dpb.ReleaseHistoryRequest{
		ServiceFilter: &dpb.ReleaseHistoryRequest_ServiceNames_{
			ServiceNames: &dpb.ReleaseHistoryRequest_ServiceNames{
				Names: []string{"test-svc"},
			},
		},
		State: []state.DeploymentState{state.DeploymentState_FAILED},
	}
	res, err := srv.ReleaseHistory(context.Background(), req)
	require.NoError(t, err)
	require.Len(t, res.Info, 1)
	assert.Equal(t, state.DeploymentState_FAILED, res.Info[0].State)
}

func TestHandler_ReleaseHistoryIssue(t *testing.T) {
	test.RunUp(t)

	srv, db, _, log := prepare(t)

	store := dm.NewStorage(db, log)
	staffApi := staffapi.NewApi(staffapi.NewConf(), log)
	staffService := staff.NewService(db, staffApi, log)
	user, err := staffService.GetByLogin("danevge")
	require.NoError(t, err)

	d1 := &dm.Deployment{
		Name:     "test-svc",
		AuthorID: user.ID,
		Type:     common.Run,
	}

	d2 := &dm.Deployment{
		Name:     "test-svc",
		AuthorID: user.ID,
		Type:     common.Run,
	}

	require.NoError(t, store.Save(d1))
	require.NoError(t, store.Save(d2))

	i1 := prepareIssue(t, "VOID-1")
	i2 := prepareIssue(t, "VOID-2")
	issueSvc := issue_link.NewService(db, log, nil)
	require.NoError(t, issueSvc.LinkByIssues(d1.ID, []*issue.Issue{i1}))
	require.NoError(t, issueSvc.LinkByIssues(d2.ID, []*issue.Issue{i2}))

	testCases := []struct {
		name                  string
		issue                 string
		expectedIDeploymentId []int64
	}{
		{
			name:                  "With issue",
			issue:                 "VOID-1",
			expectedIDeploymentId: []int64{d1.ID},
		},
		{
			name:                  "With out issue",
			expectedIDeploymentId: []int64{d1.ID, d2.ID},
		},
	}

	for _, tc := range testCases {
		t.Run(tc.name, func(t *testing.T) {
			req := &dpb.ReleaseHistoryRequest{
				ServiceFilter: &dpb.ReleaseHistoryRequest_ServiceNames_{
					ServiceNames: &dpb.ReleaseHistoryRequest_ServiceNames{
						Names: []string{"test-svc"},
					},
				},
				Issue: tc.issue,
			}

			res, err := srv.ReleaseHistory(context.Background(), req)
			require.NoError(t, err)

			var actualDeploymentIds []int64
			for _, info := range res.Info {
				actualDeploymentIds = append(actualDeploymentIds, info.ID())
			}

			require.ElementsMatch(t, tc.expectedIDeploymentId, actualDeploymentIds)
		})
	}
}

type mockServerStream struct {
	mock.Mock
	grpc.ServerStream
	ctx context.Context
}

func newMock() *mockServerStream {
	return &mockServerStream{
		ctx: context.Background(),
	}
}

func (m *mockServerStream) Send(r *dpb.StateResponse) error {
	return m.MethodCalled("Send", r).Error(0)
}

func (m *mockServerStream) Context() context.Context {
	return m.ctx
}

func getRecordFromDB(db *storage.Database, name, version string) (*dm.Deployment, error) {
	n := &dm.Deployment{}
	q := db.GormDb.Where("name = ? AND version = ?", name, version).Find(n)
	err := (&storage.BaseStorage{}).ReadableReadError(q.Error)
	return n, err
}

func waitSuccess(db *storage.Database, name, version string) error {
	dbRecord, err := getRecordFromDB(db, name, version)

	switch {
	case err != nil:
		return err
	case dm.Success == dbRecord.State:
		return nil
	default:
		return errors.New(fmt.Sprintf("deployment state is still %s", dbRecord.State.String()))
	}
}

func makeManifest(t *testing.T, yml, name string) (*mm.Manifest, int64) {

	imageName := fmt.Sprintf("%s-image", name)
	db := test_db.NewDb(t)
	log := test.NewLogger(t)
	service := manifest.NewService(db, log, parser.NewService(log, reader.NewService(db, log)), include.NewService(db, log))
	result := fmt.Sprintf(yml, name, imageName)
	err := service.ReadAndSave([]byte(result), 10, "")
	require.NoError(t, err)
	m, id, err := service.GetByNameWithId(common.Prod, name)
	require.NoError(t, err)
	return m, id
}

func makeMap(t *testing.T, yml, name string, sox bool) (*sm.ServiceMap, int64) {

	db := test_db.NewDb(t)
	service := service_map.NewService(db, test.NewLogger(t), service_change.NewNotificationMock())
	result := fmt.Sprintf(yml, name, sox)
	path := sm.ToFullPath(name)
	err := service.ReadAndSave([]byte(result), 10, path)
	require.NoError(t, err)
	m, id, err := service.GetByFullPath(path)
	require.NoError(t, err)
	return m, id
}

func makePlainMap(t *testing.T, yml string) {
	db := test_db.NewDb(t)
	service := service_map.NewService(db, test.NewLogger(t), service_change.NewNotificationMock())
	template := &parser.YamlTemplate{}
	err := yaml.Unmarshal([]byte(yml), template)
	require.NoError(t, err)
	err = service.ReadAndSave([]byte(yml), 10, sm.ToFullPath(template.Name))
	require.NoError(t, err)
}

func newDeploymentService(t *testing.T, db *storage.Database, ms *smock.Scheduler) *deployment.Service {
	log := test.NewLogger(t)
	staffApi := staffapi.NewApi(staffapi.NewConf(), log)
	staffService := staff.NewService(db, staffApi, log)
	kvMock := smock.NewKVMock()
	syncS := sync.NewService(log, kvMock, sync.NewConf(t.Name()))
	require.NoError(t, syncS.SetLastUpdate(time.Now().Add(time.Hour)))
	flagSvc := feature_flags.NewService(db, mq.NewProducerMock(), log)
	deployDataSvc := data.NewService(db, log)
	trackerSvc := tracker.NewService(db, log, stAPI.NewApi(stAPI.NewConf(), log), deployDataSvc)
	drillsSvc := drills.NewService(log, db)
	envReader := reader.NewService(db, log)
	mS := manifest.NewService(db, log, parser.NewService(log, envReader), include.NewService(db, log))
	scaleSvc := scaler.NewService(deployDataSvc, mS, &smock.PrometheusApi{}, &smock.PrometheusApi{}, scaler.Conf{}, db, log)
	mapSvc := service_map.NewService(db, log, service_change.NewNotificationMock())
	includeSvc := include.NewService(db, log)
	overrideSvc := override.NewService(db, log, includeSvc, mS)
	ssCliMock := &mocks.SecretClient{}
	ssCliMock.On("GetSecrets", mock.Anything, mock.Anything).Return(nil, nil)
	ssMock := smock.NewAccessClientMock(t)
	secretSvc := secret.NewService(log, ssCliMock)
	tmpSvc := template.NewService(mapSvc, envReader)
	svc := &deployment.Service{
		Conf:         deployment.NewConf(),
		DB:           db,
		Log:          log,
		ChangedState: dmock.NewProducerMock(),
		Locker:       dmock.NewDMockLocker(),
		TestNomad:    ms,
		ProdNomad:    ms,
		TestYaDeploy: nil,
		ProdYaDeploy: nil,
		TestBatch:    nil,
		ProdBatch:    nil,
		FlagSvc:      flagSvc,
		StaffSvc:     staffService,
		IncludeSvc:   includeSvc,
		ApproveSvc:   approve.NewService(db, log, staffService),
		Registry:     registry.NewService(smock.NewRegistry(false)),
		SyncSvc:      syncS,
		TrackerSvc:   trackerSvc,
		ScalerSvc:    scaleSvc,
		Drills:       drillsSvc,
		MapSvc:       mapSvc,
		ManifestSvc:  mS,
		IssueLinkSvc: issue_link.NewService(db, log, trackerSvc),
		OverrideSvc:  overrideSvc,
		SecretSvc:    secretSvc,
		EnvResolver:  env_resolver.NewService(tmpSvc, secrets.NewService(secrets.NewConf(0), ssMock, log)),
	}
	svc.Init()
	return svc
}

func saveDifferentTypeDeployments(t *testing.T, user *staff.User, store *dm.Storage) {
	allTypes := []common.Type{common.Run, common.Update, common.Promote, common.Restart, common.Revert, common.Stop}

	for _, cType := range allTypes {
		require.NoError(t, store.Save(&dm.Deployment{
			Name:     "test-svc",
			AuthorID: user.ID,
			Type:     cType,
		}))
	}
}

func prepareIssue(t *testing.T, key string) *issue.Issue {

	db := test_db.NewDb(t)
	log := test.NewLogger(t)
	issueStorage := issue.NewStorage(db, log)

	i := &issue.Issue{
		Key: key,
	}

	require.NoError(t, issueStorage.Save(i))

	return i
}
