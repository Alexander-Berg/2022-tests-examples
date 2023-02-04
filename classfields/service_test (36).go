package handler

import (
	"context"
	"errors"
	"fmt"
	"testing"
	"time"

	"github.com/YandexClassifieds/shiva/cmd/shiva/approve"
	"github.com/YandexClassifieds/shiva/cmd/shiva/bulk_deployment"
	"github.com/YandexClassifieds/shiva/cmd/shiva/deployment"
	"github.com/YandexClassifieds/shiva/cmd/shiva/deployment/data"
	"github.com/YandexClassifieds/shiva/cmd/shiva/deployment/issue_link"
	dModel "github.com/YandexClassifieds/shiva/cmd/shiva/deployment/model"
	"github.com/YandexClassifieds/shiva/cmd/shiva/deployment/notification"
	"github.com/YandexClassifieds/shiva/cmd/shiva/deployment/status"
	"github.com/YandexClassifieds/shiva/cmd/shiva/drills"
	"github.com/YandexClassifieds/shiva/cmd/shiva/drills/store"
	"github.com/YandexClassifieds/shiva/cmd/shiva/env/external/reader"
	env_resolver "github.com/YandexClassifieds/shiva/cmd/shiva/env/manifest"
	"github.com/YandexClassifieds/shiva/cmd/shiva/env/override"
	"github.com/YandexClassifieds/shiva/cmd/shiva/scaler"
	"github.com/YandexClassifieds/shiva/common"
	"github.com/YandexClassifieds/shiva/pb/shiva/types/bulk"
	"github.com/YandexClassifieds/shiva/pb/shiva/types/layer"
	"github.com/YandexClassifieds/shiva/pkg/consul/sync"
	"github.com/YandexClassifieds/shiva/pkg/feature_flags"
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
	"github.com/YandexClassifieds/shiva/test/test_db"
	"github.com/golang/protobuf/proto"
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
	batchManifestYml = `
name: %s
test:
  resources:
    cpu: 100
  periodic: '*/5 * * * *'
prod:
  resources:
    cpu: 300
  periodic: '*/5 * * * *'
`
	batchSMapYml = `
name: %s
type: batch
description: Test service for batch task
owners:
  - https://staff.yandex-team.ru/departments/yandex_personal_vertserv_infra
  - https://staff.yandex-team.ru/departments/yandex_personal_vertserv_infra_mnt_teamyaml
design_doc: 
src:  https://github.com/YandexClassifieds/shiva/cmd/testapp
`
	sMapPath = "maps/%s.yml"
)

const (
	version = "v1"
)

func TestLifeCircle(t *testing.T) {

	var err error
	test.RunUp(t)
	schedulerMock := mock.NewMockScheduler()
	pMock := mqMock.NewProducerMock()
	s, _ := newService(t, schedulerMock, dmock.NewProducerMock(), pMock)

	// Close
	assertEmptyDrills(t, s)
	require.NoError(t, s.Close(feature_flags.TestSasOff.String()))
	require.NoError(t, s.Close(feature_flags.TestYdSasOff.String()))
	require.NoError(t, s.Close(feature_flags.ProdYdSasOff.String()))
	require.NoError(t, s.Close(feature_flags.ProdSasOff.String()))
	assertDrills(t, s, layer.Layer_TEST, store.Nomad, "sas", true)
	assertDrills(t, s, layer.Layer_TEST, store.YD, "yd_sas", true)
	assertDrills(t, s, layer.Layer_PROD, store.Nomad, "sas", true)
	assertDrills(t, s, layer.Layer_PROD, store.YD, "yd_sas", true)
	assertDrills(t, s, layer.Layer_TEST, store.Nomad, "vla", false)
	assertDrills(t, s, layer.Layer_TEST, store.YD, "yd_vla", false)
	r, err := s.recoverySt.Get()
	require.NoError(t, err)
	require.NotNil(t, r)

	// Open
	require.NoError(t, s.Open(feature_flags.TestSasOff.String()))
	require.NoError(t, s.Open(feature_flags.TestYdSasOff.String()))
	require.NoError(t, s.Open(feature_flags.ProdYdSasOff.String()))
	r, err = s.recoverySt.Get()
	require.NoError(t, err)
	require.NotNil(t, r)
	assert.Empty(t, pMock.Msg)
	// open last drills
	require.NoError(t, s.Open(feature_flags.ProdSasOff.String()))
	test.Wait(t, func() error {
		r, err = s.recoverySt.Get()
		if errors.Is(err, common.ErrNotFound) {
			return nil
		}
		return fmt.Errorf("finded recovery")
	})
	assert.Len(t, pMock.Msg, 2)

	// End
	require.NoError(t, s.processBR(extractBR(t, pMock.Get(t))))
	require.NoError(t, s.processBR(extractBR(t, pMock.Get(t))))
}

func extractBR(t *testing.T, msg *mq.Message) *bulk.BulkRun {
	br := &bulk.BulkRun{}
	b := msg.Payload
	err := proto.Unmarshal(b, br)
	require.NoError(t, err)
	return br
}

func assertEmptyDrills(t *testing.T, s *Service) {
	actual, err := s.drillsSt.Actual(layer.Layer_TEST)
	require.NoError(t, err)
	assert.Len(t, actual, 0)
	actual, err = s.drillsSt.Actual(layer.Layer_PROD)
	require.NoError(t, err)
	assert.Len(t, actual, 0)
}

func assertDrills(t *testing.T, s *Service, l layer.Layer, sc store.SchedulerType, dc string, exist bool) {

	actual, err := s.drillsSt.Actual(l)
	require.NoError(t, err)
	for _, dr := range actual {
		if dr.Layer == l && dr.DC == dc && dr.Scheduler == sc {
			if exist {
				return
			}
			assert.FailNow(t, fmt.Sprintf("Drills exist by Layer %s, dc %s and sc %s", l.String(), dc, sc.String()))
		}
	}
	if exist {
		assert.FailNow(t, fmt.Sprintf("Drills not exist by Layer %s, dc %s and sc %s", l.String(), dc, sc.String()))
	}
}

func newService(
	t *testing.T,
	scheduler *mock.Scheduler,
	changedState notification.ChangedState,
	bulkDeployEvent mqMock.ProducerMock,
) (*Service, *deployment.Service) {

	db := test_db.NewDb(t)
	log := test.NewLogger(t)

	envReader := reader.NewService(db, log)
	flagSvc := feature_flags.NewService(db, mqMock.NewProducerMock(), log)
	deployDataSvc := data.NewService(db, log)
	trackerSvc := tracker.NewService(db, log, stAPI.NewApi(stAPI.NewConf(), log), deployDataSvc)
	includeSvc := include.NewService(db, log)
	manifestSvc := manifest.NewService(db, log, parser.NewService(log, envReader), includeSvc)
	staffApi := staffapi.NewApi(staffapi.NewConf(), log)
	staffService := staff.NewService(db, staffApi, log)
	registrySvc := registry.NewService(mock.NewRegistry(false))
	lockerMock := dmock.NewDMockLocker()
	mapSvc := service_map.NewService(db, log, service_change.NewNotificationMock())
	statusSvc := status.NewService(db, log, mapSvc)
	issueLinkSvc := issue_link.NewService(db, log, trackerSvc)
	kvMock := mock.NewKVMock()
	syncSvc := sync.NewService(log, kvMock, sync.NewConf(t.Name()))
	require.NoError(t, syncSvc.SetLastUpdate(time.Now().Add(time.Hour)))
	drillsSvc := drills.NewService(log, db)
	overrideSvc := override.NewService(db, log, includeSvc, manifestSvc)

	scaleSvc := scaler.NewService(deployDataSvc, manifestSvc, &mock.PrometheusApi{}, &mock.PrometheusApi{}, scaler.Conf{}, db, log)

	ssCliMock := &mocks.SecretClient{}
	ssCliMock.On("GetSecrets", mock2.Anything, mock2.Anything).Return(nil, nil)
	secretSvc := secret.NewService(log, ssCliMock)
	tmpSvc := template.NewService(mapSvc, envReader)
	ssMock := mock.NewAccessClientMock(t)

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
		TestBatch:    scheduler,
		ProdBatch:    scheduler,
		FlagSvc:      flagSvc,
		StaffSvc:     staffService,
		IncludeSvc:   includeSvc,
		ApproveSvc:   approve.NewService(db, log, staffService),
		Registry:     registrySvc,
		SyncSvc:      syncSvc,
		TrackerSvc:   trackerSvc,
		ScalerSvc:    scaleSvc,
		Drills:       drillsSvc,
		MapSvc:       mapSvc,
		ManifestSvc:  manifestSvc,
		IssueLinkSvc: issueLinkSvc,
		OverrideSvc:  overrideSvc,
		SecretSvc:    secretSvc,
		EnvResolver:  env_resolver.NewService(tmpSvc, secrets.NewService(secrets.NewConf(0), ssMock, log)),
	}
	deploySvc.Init()

	bdSvc := (&bulk_deployment.Service{
		Conf:      bulk_deployment.NewConf(),
		DB:        db,
		Log:       test.NewLogger(t),
		DeploySvc: deploySvc,
		StatusSvc: statusSvc,
		Locker:    mock.NewMockSilentLocker(),
		Producer:  bulkDeployEvent,
	}).Init()

	return NewService(log, db, deployDataSvc, bdSvc), deploySvc
}

func TestDeploymentList(t *testing.T) {
	test.RunUp(t)
	defer test.Down(t)
	schedulerMock := mock.NewMockScheduler()
	s, deploymentSrv := newService(t, schedulerMock, dmock.NewProducerMock(), mqMock.NewProducerMock())
	runService(t, deploymentSrv, schedulerMock, "service4", manifestYml, sMapYml, common.Prod, common.Run)
	start := time.Now()
	testS, prodS, err := s.deployments(start)
	require.Len(t, testS, 0)
	require.Len(t, prodS, 0)

	runService(t, deploymentSrv, schedulerMock, "service1", manifestYml, sMapYml, common.Test, common.Run)
	runService(t, deploymentSrv, schedulerMock, "service2", manifestYml, sMapYml, common.Prod, common.Run)
	runService(t, deploymentSrv, schedulerMock, "service3", batchManifestYml, batchSMapYml, common.Prod, common.Run)
	runService(t, deploymentSrv, schedulerMock, "service4", manifestYml, sMapYml, common.Prod, common.Update)
	testS, prodS, err = s.deployments(start)
	require.NoError(t, err)
	require.Len(t, testS, 1)
	require.Len(t, prodS, 2)
	for d := range testS {
		assert.Equal(t, "service1", d.Service)
		assert.Equal(t, "", d.Branch)
	}
	want := map[string]struct{}{
		"service2": {},
		"service4": {},
	}
	for d := range prodS {
		_, actual := want[d.Service]
		assert.True(t, actual)
		assert.Equal(t, "", d.Branch)
	}
}

func runService(
	t *testing.T,
	deploymentSrv deployment.IService,
	scheduler *mock.Scheduler,
	name string,
	mYml string,
	sYml string,
	layer common.Layer,
	dType common.Type,
) {
	db := test_db.NewDb(t)
	log := test.NewLogger(t)
	includeService := include.NewService(db, log)
	manifestService := manifest.NewService(db, log, parser.NewService(log, reader.NewService(db, log)), includeService)
	result := fmt.Sprintf(mYml, name)
	require.NoError(t, manifestService.ReadAndSave([]byte(result), 10, ""))
	sMapS := service_map.NewService(db, test.NewLogger(t), service_change.NewNotificationMock())
	result = fmt.Sprintf(sYml, name)
	path := fmt.Sprintf(sMapPath, name)
	require.NoError(t, sMapS.ReadAndSave([]byte(result), 10, path))

	var err error
	var stateC chan *deployment.StateChange
	switch dType {
	case common.Run:
		stateC, _, err = deploymentSrv.Run(context.Background(), dModel.RunParams{
			Layer:   layer,
			Name:    name,
			Version: version,
			Login:   "danevge",
		})
	case common.Update:
		stateC, _, err = deploymentSrv.Update(context.Background(), dModel.UpdateParams{
			Layer: layer,
			Name:  name,
			Login: "danevge",
		})
	}
	require.NoError(t, err)
	scheduler.Success(t, dType, name, "", version)
	for _ = range stateC {
	}
}
