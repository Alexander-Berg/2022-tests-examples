package production_mirroring

import (
	"errors"
	"strconv"
	"testing"
	"time"

	"github.com/YandexClassifieds/shiva/pkg/secrets"
	"github.com/YandexClassifieds/shiva/pkg/secrets/secret"
	"github.com/YandexClassifieds/shiva/pkg/template"
	"github.com/YandexClassifieds/shiva/test/mock/mockery/mocks"
	mock2 "github.com/stretchr/testify/mock"

	"github.com/YandexClassifieds/shiva/pb/shiva/events/event2"
	"github.com/YandexClassifieds/shiva/pkg/consul/sync"

	"github.com/YandexClassifieds/shiva/cmd/shiva/approve"
	"github.com/YandexClassifieds/shiva/cmd/shiva/deployment"
	"github.com/YandexClassifieds/shiva/cmd/shiva/deployment/data"
	"github.com/YandexClassifieds/shiva/cmd/shiva/deployment/issue_link"
	"github.com/YandexClassifieds/shiva/cmd/shiva/deployment/model"
	"github.com/YandexClassifieds/shiva/cmd/shiva/drills"
	"github.com/YandexClassifieds/shiva/cmd/shiva/env/external/reader"
	env_resolver "github.com/YandexClassifieds/shiva/cmd/shiva/env/manifest"
	"github.com/YandexClassifieds/shiva/cmd/shiva/env/override"
	"github.com/YandexClassifieds/shiva/cmd/shiva/scaler"
	"github.com/YandexClassifieds/shiva/common"
	"github.com/YandexClassifieds/shiva/common/config"
	dproto "github.com/YandexClassifieds/shiva/pb/shiva/types/deployment"
	"github.com/YandexClassifieds/shiva/pb/shiva/types/state"
	"github.com/YandexClassifieds/shiva/pkg/feature_flags"
	"github.com/YandexClassifieds/shiva/pkg/include"
	"github.com/YandexClassifieds/shiva/pkg/manifest"
	"github.com/YandexClassifieds/shiva/pkg/manifest/parser"
	"github.com/YandexClassifieds/shiva/pkg/registry"
	"github.com/YandexClassifieds/shiva/pkg/service_map"
	"github.com/YandexClassifieds/shiva/pkg/staff"
	staffapi "github.com/YandexClassifieds/shiva/pkg/staff/api"
	"github.com/YandexClassifieds/shiva/pkg/tracker"
	stAPI "github.com/YandexClassifieds/shiva/pkg/tracker/api"
	"github.com/YandexClassifieds/shiva/test"
	"github.com/YandexClassifieds/shiva/test/mock"
	dmock "github.com/YandexClassifieds/shiva/test/mock/deployment"
	"github.com/YandexClassifieds/shiva/test/mock/mq"
	"github.com/YandexClassifieds/shiva/test/mock/service_change"
	"github.com/YandexClassifieds/shiva/test/test_db"
	"github.com/golang/protobuf/ptypes"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
)

const (
	name = "test-svc"

	mirrorManifest = `
name: test-svc
production_mirroring: true
general:
  datacenters: {sas: {count: 1}, myt: {count: 1}}
  memory: 256
`
	testMap = `
name: test-svc
owner: https://staff.yandex-team.ru/departments/yandex_personal_vertserv_infra
`
)

func TestHandleEvent(t *testing.T) {

	test.RunUp(t)
	defer test.Down(t)

	prodMirroringSvc, deploySvc, manifestSvc, sMapSvc, staffSvc, testSchedulerMock := makeServices(t)

	require.NoError(t, manifestSvc.ReadAndSave([]byte(mirrorManifest), 10, ""))

	testCases := []struct {
		Name  string
		dType common.Type
	}{
		{
			Name:  "Run_Success",
			dType: common.Run,
		},
		{
			Name:  "Revert_Success",
			dType: common.Revert,
		},
	}

	for _, tc := range testCases {
		t.Run(tc.Name, func(t *testing.T) {
			dm := makeDeployment(t, manifestSvc, sMapSvc, staffSvc, common.Prod, "", tc.dType, model.Success)

			go func() {
				err := prodMirroringSvc.handleEvent(makeEvent(dm))
				require.NoError(t, err)
			}()

			test.Wait(t, func() error {
				inProgress, err := deploySvc.InProgress(name)
				if err != nil {
					return err
				}
				if len(inProgress) == 0 {
					return errors.New("not in progress")
				}
				return nil
			})

			testSchedulerMock.Success(t, common.Run, name, config.ProductionMirroring, dm.Version)

			stopDm := makeDeployment(t, manifestSvc, sMapSvc, staffSvc, common.Prod, "", common.Stop, model.Success)

			go func() {
				err := prodMirroringSvc.handleEvent(makeEvent(stopDm))
				require.NoError(t, err)
			}()

			test.Wait(t, func() error {
				inProgress, err := deploySvc.InProgress(name)
				if err != nil {
					return err
				}
				if len(inProgress) == 0 {
					return errors.New("not in progress")
				}
				return nil
			})

			testSchedulerMock.Success(t, common.Stop, name, config.ProductionMirroring, "")
		})
	}
}

func TestInverseOrder(t *testing.T) {

	test.RunUp(t)
	defer test.Down(t)

	prodMirroringSvc, _, manifestSvc, sMapSvc, staffSvc, _ := makeServices(t)
	require.NoError(t, manifestSvc.ReadAndSave([]byte(mirrorManifest), 10, ""))
	dm := makeDeployment(t, manifestSvc, sMapSvc, staffSvc, common.Prod, "", common.Run, model.Success)
	stopDm := makeDeployment(t, manifestSvc, sMapSvc, staffSvc, common.Prod, "", common.Stop, model.Success)

	errChan := make(chan error)

	go func() {
		err := prodMirroringSvc.handleEvent(makeEvent(stopDm))
		errChan <- err
	}()
	WaitForError(t, errChan)

	go func() {
		err := prodMirroringSvc.handleEvent(makeEvent(dm))
		errChan <- err
	}()

	WaitForError(t, errChan)
}

func TestDuplicateEvent(t *testing.T) {

	test.RunUp(t)
	defer test.Down(t)

	prodMirroringSvc, deploySvc, manifestSvc, sMapSvc, staffSvc, testSchedulerMock := makeServices(t)
	require.NoError(t, manifestSvc.ReadAndSave([]byte(mirrorManifest), 10, ""))
	dm := makeDeployment(t, manifestSvc, sMapSvc, staffSvc, common.Prod, "", common.Run, model.Success)

	errChan := make(chan error)

	go func() {
		err := prodMirroringSvc.handleEvent(makeEvent(dm))
		errChan <- err
	}()

	test.Wait(t, func() error {
		inProgress, err := deploySvc.InProgress(name)
		if err != nil {
			return err
		}
		if len(inProgress) == 0 {
			return errors.New("not in progress")
		}
		return nil
	})

	testSchedulerMock.Success(t, common.Run, name, config.ProductionMirroring, dm.Version)
	WaitForError(t, errChan)

	go func() {
		err := prodMirroringSvc.handleEvent(makeEvent(dm))
		errChan <- err
	}()
	WaitForError(t, errChan)

	stopDm := makeDeployment(t, manifestSvc, sMapSvc, staffSvc, common.Prod, "", common.Stop, model.Success)

	go func() {
		err := prodMirroringSvc.handleEvent(makeEvent(stopDm))
		errChan <- err
	}()

	test.Wait(t, func() error {
		inProgress, err := deploySvc.InProgress(name)
		if err != nil {
			return err
		}
		if len(inProgress) == 0 {
			return errors.New("not in progress")
		}
		return nil
	})

	testSchedulerMock.Success(t, common.Stop, name, config.ProductionMirroring, "")
	WaitForError(t, errChan)

	go func() {
		err := prodMirroringSvc.handleEvent(makeEvent(dm))
		errChan <- err
	}()
	WaitForError(t, errChan)
}

func makeEvent(dm *model.Deployment) *event2.Event {

	startTs, err := ptypes.TimestampProto(dm.StartDate)
	if err != nil {
		panic(err)
	}
	endTs, err := ptypes.TimestampProto(dm.EndDate)
	if err != nil {
		panic(err)
	}

	e := &event2.Event{
		Deployment: &dproto.Deployment{
			Id:          strconv.FormatInt(dm.ID, 10),
			ServiceName: name,
			Version:     dm.Version,
			User:        "ibiryulin",
			Comment:     dm.Comment,
			State:       state.DeploymentState_SUCCESS,
			Start:       startTs,
			End:         endTs,
		},
	}

	e.Deployment.SetCommonType(dm.Type)
	e.Deployment.SetCommonLayer(dm.Layer)

	return e
}

func makeDeployment(t *testing.T, manifestSvc *manifest.Service, sMapSvc *service_map.Service, staffSvc *staff.Service, layer common.Layer, branch string, dtype common.Type, state model.State) *model.Deployment {

	store := model.NewStorage(test_db.NewDb(t), test.NewLogger(t))

	_, mid, err := manifestSvc.GetByNameWithId(layer, name)
	require.NoError(t, err)

	_, sid, err := sMapSvc.GetByFullPath("maps/test-svc.yml")
	require.NoError(t, err)

	user, err := staffSvc.GetByLogin("ibiryulin")
	require.NoError(t, err)

	d := &model.Deployment{
		Name:             name,
		DeployManifestID: mid,
		ServiceMapsID:    sid,
		Layer:            layer,
		Branch:           branch,
		Version:          "42",
		AuthorID:         user.ID,
		State:            state,
		Type:             dtype,
		Comment:          "comment",
		StartDate:        time.Now(),
		EndDate:          time.Now().Add(time.Minute),
	}
	require.NoError(t, store.Save(d))
	return d
}

func makeServices(t *testing.T) (*Service, *deployment.Service, *manifest.Service, *service_map.Service, *staff.Service, *mock.Scheduler) {
	db := test_db.NewDb(t)
	log := test.NewLogger(t)
	testSchedulerMock := mock.NewMockScheduler()
	prodSchedulerMock := mock.NewMockScheduler()
	includeSvc := include.NewService(db, log)
	envReader := reader.NewService(db, log)
	manifestSvc := manifest.NewService(db, log, parser.NewService(log, envReader), includeSvc)
	featureFlagsService := feature_flags.NewService(db, mq.NewProducerMock(), log)
	deployDataSvc := data.NewService(db, log)
	trackerSvc := tracker.NewService(db, log, stAPI.NewApi(stAPI.NewConf(), log), deployDataSvc)
	staffApi := staffapi.NewApi(staffapi.NewConf(), log)
	staffService := staff.NewService(db, staffApi, log)
	locker := dmock.NewDMockLocker()
	sMapSvc := service_map.NewService(db, log, service_change.NewNotificationMock())
	kvMock := mock.NewKVMock()
	syncSvc := sync.NewService(log, kvMock, sync.NewConf(t.Name()))
	drillsSvc := drills.NewService(log, db)
	scaleSvc := scaler.NewService(deployDataSvc, manifestSvc, &mock.PrometheusApi{}, &mock.PrometheusApi{}, scaler.Conf{}, db, log)
	overrideSvc := override.NewService(db, log, includeSvc, manifestSvc)
	ssCliMock := &mocks.SecretClient{}
	ssCliMock.On("GetSecrets", mock2.Anything, mock2.Anything).Return(nil, nil)
	secretSvc := secret.NewService(log, ssCliMock)
	ssMock := mock.NewAccessClientMock(t)
	secretsSvc := secrets.NewService(secrets.NewConf(0), ssMock, log)
	tmpSvc := template.NewService(sMapSvc, envReader)

	require.NoError(t, syncSvc.SetLastUpdate(time.Now().Add(time.Hour)))
	require.NoError(t, sMapSvc.ReadAndSave([]byte(testMap), 10, "maps/test-svc.yml"))

	deploySvc := &deployment.Service{
		Conf:         deployment.NewConf(),
		DB:           db,
		Log:          log,
		ChangedState: dmock.NewProducerMock(),
		Locker:       locker,
		TestNomad:    testSchedulerMock,
		ProdNomad:    prodSchedulerMock,
		TestYaDeploy: nil,
		ProdYaDeploy: nil,
		TestBatch:    nil,
		ProdBatch:    nil,
		FlagSvc:      featureFlagsService,
		StaffSvc:     staffService,
		IncludeSvc:   includeSvc,
		ApproveSvc:   approve.NewService(db, log, staffService),
		Registry:     registry.NewService(mock.NewRegistry(false)),
		SyncSvc:      syncSvc,
		TrackerSvc:   trackerSvc,
		ScalerSvc:    scaleSvc,
		Drills:       drillsSvc,
		MapSvc:       sMapSvc,
		ManifestSvc:  manifestSvc,
		IssueLinkSvc: issue_link.NewService(db, log, trackerSvc),
		OverrideSvc:  overrideSvc,
		SecretSvc:    secretSvc,
		EnvResolver:  env_resolver.NewService(tmpSvc, secretsSvc),
	}
	deploySvc.Init()
	prodMirroringSvc := NewService(db, deploySvc, manifestSvc, log)

	return prodMirroringSvc, deploySvc, manifestSvc, sMapSvc, staffService, testSchedulerMock
}

func WaitForError(t *testing.T, errChan chan error) {

	timer := time.NewTimer(time.Second * 3)

	select {
	case <-timer.C:
		assert.FailNow(t, "error wait timeout")
	case err := <-errChan:
		require.NoError(t, err)
	}
}
