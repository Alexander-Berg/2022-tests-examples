package writer

import (
	"errors"
	"fmt"
	"strconv"
	"testing"
	"time"

	"github.com/YandexClassifieds/shiva/cmd/shiva/approve"
	"github.com/YandexClassifieds/shiva/cmd/shiva/bulk_deployment"
	"github.com/YandexClassifieds/shiva/cmd/shiva/deployment"
	"github.com/YandexClassifieds/shiva/cmd/shiva/deployment/data"
	"github.com/YandexClassifieds/shiva/cmd/shiva/deployment/issue_link"
	"github.com/YandexClassifieds/shiva/cmd/shiva/deployment/notification"
	"github.com/YandexClassifieds/shiva/cmd/shiva/deployment/status"
	"github.com/YandexClassifieds/shiva/cmd/shiva/drills"
	"github.com/YandexClassifieds/shiva/cmd/shiva/env/external/reader"
	"github.com/YandexClassifieds/shiva/cmd/shiva/env/external/storage"
	"github.com/YandexClassifieds/shiva/cmd/shiva/env/override"
	"github.com/YandexClassifieds/shiva/cmd/shiva/scaler"
	"github.com/YandexClassifieds/shiva/common"
	sm "github.com/YandexClassifieds/shiva/pb/shiva/service_map"
	"github.com/YandexClassifieds/shiva/pb/shiva/types/bulk"
	envTypes "github.com/YandexClassifieds/shiva/pb/shiva/types/env"
	"github.com/YandexClassifieds/shiva/pb/shiva/types/layer"
	"github.com/YandexClassifieds/shiva/pkg/consul/sync"
	"github.com/YandexClassifieds/shiva/pkg/feature_flags"
	"github.com/YandexClassifieds/shiva/pkg/include"
	"github.com/YandexClassifieds/shiva/pkg/manifest"
	"github.com/YandexClassifieds/shiva/pkg/manifest/parser"
	"github.com/YandexClassifieds/shiva/pkg/mq"
	"github.com/YandexClassifieds/shiva/pkg/registry"
	"github.com/YandexClassifieds/shiva/pkg/service_change/notitication"
	"github.com/YandexClassifieds/shiva/pkg/service_map"
	"github.com/YandexClassifieds/shiva/pkg/staff"
	staffapi "github.com/YandexClassifieds/shiva/pkg/staff/api"
	"github.com/YandexClassifieds/shiva/pkg/tracker"
	stAPI "github.com/YandexClassifieds/shiva/pkg/tracker/api"
	"github.com/YandexClassifieds/shiva/test"
	"github.com/YandexClassifieds/shiva/test/mock"
	dmock "github.com/YandexClassifieds/shiva/test/mock/deployment"
	mqMock "github.com/YandexClassifieds/shiva/test/mock/mq"
	"github.com/YandexClassifieds/shiva/test/mock/service_change"
	"github.com/YandexClassifieds/shiva/test/test_db"
	"github.com/golang/protobuf/proto"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
)

var (
	brId = int64(37)
)

func TestDoubleNew(t *testing.T) {

	s := newService(t, nil, nil, nil, mqMock.NewProducerMock())

	m := &storage.ExternalEnv{
		Service: "service",
		Layer:   common.Prod,
		Type:    envTypes.EnvType_GENERATED_TVM_ID,
		Key:     "key1",
		Value:   "value2",
	}
	require.NoError(t, s.New(m))
	time.Sleep(100 * time.Millisecond)
	err := s.New(m)
	assert.True(t, errors.Is(err, ErrEnvAlreadyExist))
}

func TestStartRegenerating(t *testing.T) {
	nMock := service_change.NewNotificationMock()
	s := newService(t, nMock, nil, nil, mqMock.NewProducerMock())

	m := &storage.ExternalEnv{
		Service: "service",
		Layer:   common.Prod,
		Type:    envTypes.EnvType_GENERATED_TVM_ID,
		Key:     "key1",
		Value:   "value2",
	}
	require.NoError(t, s.New(m))
	makeSimpleMap(t, s.sMapSvc, "service", sm.ServiceType_service.String())

	require.NoError(t, s.Regenerate("service", layer.Layer_PROD, "key1"))

	m, err := s.storage.Get("service", common.Prod, "key1")
	require.NoError(t, err)
	assert.Equal(t, true, m.Regenerating)
	nMock.AssertEnvCalls(t, 1, 0)
}

func TestDoubleRegenerating(t *testing.T) {
	nMock := service_change.NewNotificationMock()
	s := newService(t, nMock, nil, nil, mqMock.NewProducerMock())

	m := &storage.ExternalEnv{
		Service: "service",
		Layer:   common.Prod,
		Type:    envTypes.EnvType_GENERATED_TVM_ID,
		Key:     "key1",
		Value:   "value2",
	}
	require.NoError(t, s.New(m))
	makeSimpleMap(t, s.sMapSvc, "service", sm.ServiceType_service.String())

	require.NoError(t, s.Regenerate("service", layer.Layer_PROD, "key1"))

	err := s.Regenerate("service", layer.Layer_PROD, "key1")

	require.ErrorAs(t, err, &ErrEnvAlreadyRegenerating)
	nMock.AssertEnvCalls(t, 1, 0)
}

func TestProcess_SkipByBrId(t *testing.T) {
	nMock := service_change.NewNotificationMock()
	s := newService(t, nMock, nil, nil, mqMock.NewProducerMock())
	prepareEnvs(t, s)

	nonExistentBrId := int64(7)
	msg := event(t, bulk.State_Success, nonExistentBrId)

	require.NoError(t, s.process(msg))
	nMock.AssertEnvCalls(t, 0, 0)
}

func TestProcess_BrStatusFail(t *testing.T) {
	nMock := service_change.NewNotificationMock()
	s := newService(t, nMock, nil, nil, mqMock.NewProducerMock())
	prepareEnvs(t, s)

	msg := event(t, bulk.State_Canceled, brId)

	require.NoError(t, s.process(msg))
	nMock.AssertEnvCalls(t, 0, 0)
}

func TestProcess_Success(t *testing.T) {
	nMock := service_change.NewNotificationMock()
	s := newService(t, nMock, nil, nil, mqMock.NewProducerMock())
	prepareEnvs(t, s)

	msg := event(t, bulk.State_Success, brId)

	require.NoError(t, s.process(msg))
	nMock.AssertEnvCalls(t, 0, 6)
}

func TestSetEnvs(t *testing.T) {
	pMock := mqMock.NewProducerMock()
	s := newService(t, service_change.NewNotificationMock(), mock.NewMockScheduler(), dmock.NewProducerMock(), pMock)
	prepareEnvs(t, s)

	envs := []*storage.ExternalEnv{
		{
			Service: "s1",
			Layer:   common.Prod,
			Type:    envTypes.EnvType_GENERATED_TVM_ID,
			Key:     "1",
			Value:   "updVal",
		},
		{Service: "s1",
			Layer: common.Prod,
			Type:  envTypes.EnvType_GENERATED_TVM_ID,
			Key:   "key1",
			Value: "newVal"},
	}

	err := s.SetEnvs(envs)
	require.NoError(t, err)

	envs, err = s.storage.GetAll([]envTypes.EnvType{envTypes.EnvType_GENERATED_TVM_ID}, []common.Layer{common.Prod}, []string{"s1"})
	require.NoError(t, err)
	assert.Equal(t, 2, len(envs))

	br := extractBR(t, pMock.Get(t))
	envs, err = s.storage.GetByValue([]envTypes.EnvType{envTypes.EnvType_GENERATED_TVM_ID}, "updVal")
	require.NoError(t, err)
	assert.Equal(t, 1, len(envs))
	assert.Equal(t, br.ID, *envs[0].BrId)
}

func prepareEnvs(t *testing.T, s *Service) {
	require.NoError(t, s.New(newEnv(common.Prod, envTypes.EnvType_GENERATED_TVM_ID, "s1", 1)))
	require.NoError(t, s.New(newEnv(common.Test, envTypes.EnvType_GENERATED_TVM_ID, "s2", 2)))
	require.NoError(t, s.New(newEnv(common.Test, envTypes.EnvType_GENERATED_TVM_SECRET, "s2", 3)))
	require.NoError(t, s.New(newEnv(common.Prod, envTypes.EnvType_GENERATED_TVM_ID, "s2", 4)))
	require.NoError(t, s.New(newEnv(common.Prod, envTypes.EnvType_GENERATED_TVM_SECRET, "s2", 5)))
	require.NoError(t, s.New(newEnv(common.Unknown, envTypes.EnvType_GENERATED_TVM_ID, "s2", 6)))

	makeSimpleMap(t, s.sMapSvc, "s1", sm.ServiceType_service.String())
	makeSimpleMap(t, s.sMapSvc, "s2", sm.ServiceType_service.String())
	makeSimpleMap(t, s.sMapSvc, "s3", sm.ServiceType_service.String())
	makeSimpleMap(t, s.sMapSvc, "s4", sm.ServiceType_service.String())
	makeSimpleMap(t, s.sMapSvc, "s5", sm.ServiceType_service.String())
	makeSimpleMap(t, s.sMapSvc, "s6", sm.ServiceType_service.String())

}

func newEnv(l common.Layer, t envTypes.EnvType, s string, n int) *storage.ExternalEnv {
	strI := strconv.Itoa(n)
	return &storage.ExternalEnv{
		Service:      s,
		Layer:        l,
		Type:         t,
		Key:          strI,
		Value:        "v" + strI,
		Regenerating: true,
		BrId:         &brId,
	}
}

func makeSimpleMap(t *testing.T, s *service_map.Service, name, sType string) {
	mapStr := fmt.Sprintf(`
name: %s
type: %s`, name, sType)
	path := fmt.Sprintf("maps/%s.yml", name)
	require.NoError(t, s.ReadAndSave([]byte(mapStr), test.AtomicNextUint(), path))
}

func event(t *testing.T, state bulk.State, brId int64) *mq.Message {
	e := &bulk.BulkRun{ID: brId, State: state}
	b, err := proto.Marshal(e)
	require.NoError(t, err)
	return mq.NewMessage("", b, nil)
}

func extractBR(t *testing.T, msg *mq.Message) *bulk.BulkRun {
	br := &bulk.BulkRun{}
	b := msg.Payload
	err := proto.Unmarshal(b, br)
	require.NoError(t, err)
	return br
}

func newService(t *testing.T, notification notitication.Notification, scheduler *mock.Scheduler, changedState notification.ChangedState, bulkStateProducer mqMock.ProducerMock) *Service {

	test.RunUp(t)
	defer test.Down(t)
	db := test_db.NewDb(t)
	log := test.NewLogger(t)

	flagSvc := feature_flags.NewService(db, mqMock.NewProducerMock(), log)
	deployDataSvc := data.NewService(db, log)
	trackerSvc := tracker.NewService(db, log, stAPI.NewApi(stAPI.NewConf(), log), deployDataSvc)
	includeSvc := include.NewService(db, log)
	manifestSvc := manifest.NewService(db, log, parser.NewService(log, reader.NewService(db, log)), includeSvc)
	staffApi := staffapi.NewApi(staffapi.NewConf(), log)
	staffService := staff.NewService(db, staffApi, log)
	lockerMock := dmock.NewDMockLocker()
	mapSvc := service_map.NewService(db, log, service_change.NewNotificationMock())
	statusSvc := status.NewService(db, log, mapSvc)
	issueLinkSvc := issue_link.NewService(db, log, trackerSvc)
	kvMock := mock.NewKVMock()
	syncSvc := sync.NewService(log, kvMock, sync.NewConf(t.Name()))
	require.NoError(t, syncSvc.SetLastUpdate(time.Now().Add(time.Hour)))
	drillsSvc := drills.NewService(log, db)
	scaleSvc := scaler.NewService(deployDataSvc, manifestSvc, &mock.PrometheusApi{}, &mock.PrometheusApi{}, scaler.Conf{}, db, log)
	overrideSvc := override.NewService(db, log, includeSvc, manifestSvc)

	deploySvc := &deployment.Service{
		Conf:         deployment.NewConf(),
		DB:           db,
		Log:          log,
		ChangedState: changedState,
		Locker:       lockerMock,
		TestNomad:    scheduler,
		ProdNomad:    scheduler,
		TestYaDeploy: nil,
		ProdYaDeploy: nil,
		TestBatch:    nil,
		ProdBatch:    nil,
		FlagSvc:      flagSvc,
		StaffSvc:     staffService,
		IncludeSvc:   includeSvc,
		ApproveSvc:   approve.NewService(db, log, staffService),
		Registry:     registry.NewService(mock.NewRegistry(false)),
		SyncSvc:      syncSvc,
		TrackerSvc:   trackerSvc,
		ScalerSvc:    scaleSvc,
		Drills:       drillsSvc,
		MapSvc:       mapSvc,
		ManifestSvc:  manifestSvc,
		IssueLinkSvc: issueLinkSvc,
		OverrideSvc:  overrideSvc,
	}
	deploySvc.Init()

	bdSvc := (&bulk_deployment.Service{
		Conf:      bulk_deployment.NewConf(),
		DB:        db,
		Log:       test.NewLogger(t),
		DeploySvc: deploySvc,
		StatusSvc: statusSvc,
		Locker:    mock.NewMockSilentLocker(),
		Producer:  bulkStateProducer,
	}).Init()

	return NewService(db, log, mapSvc, bdSvc, notification)
}
