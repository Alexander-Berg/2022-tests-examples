package deployment_test

import (
	"context"
	"embed"
	"fmt"
	"testing"
	"time"

	storage2 "github.com/YandexClassifieds/shiva/cmd/shiva/env/external/storage"
	env_resolver "github.com/YandexClassifieds/shiva/cmd/shiva/env/manifest"
	"github.com/YandexClassifieds/shiva/pkg/secrets"
	"github.com/YandexClassifieds/shiva/pkg/template"

	"github.com/YandexClassifieds/shiva/cmd/shiva/approve"
	"github.com/YandexClassifieds/shiva/cmd/shiva/deployment"
	dcontext "github.com/YandexClassifieds/shiva/cmd/shiva/deployment/context"
	"github.com/YandexClassifieds/shiva/cmd/shiva/deployment/data"
	"github.com/YandexClassifieds/shiva/cmd/shiva/deployment/env_override"
	"github.com/YandexClassifieds/shiva/cmd/shiva/deployment/include_links"
	"github.com/YandexClassifieds/shiva/cmd/shiva/deployment/issue_link"
	"github.com/YandexClassifieds/shiva/cmd/shiva/deployment/model"
	"github.com/YandexClassifieds/shiva/cmd/shiva/drills"
	"github.com/YandexClassifieds/shiva/cmd/shiva/drills/store"
	drills2 "github.com/YandexClassifieds/shiva/cmd/shiva/drills/store/drills"
	"github.com/YandexClassifieds/shiva/cmd/shiva/env/external/reader"
	"github.com/YandexClassifieds/shiva/cmd/shiva/env/override"
	"github.com/YandexClassifieds/shiva/cmd/shiva/scaler"
	"github.com/YandexClassifieds/shiva/cmd/shiva/scheduler"
	"github.com/YandexClassifieds/shiva/common"
	"github.com/YandexClassifieds/shiva/common/config"
	"github.com/YandexClassifieds/shiva/common/storage"
	"github.com/YandexClassifieds/shiva/pb/shiva/types/layer"
	"github.com/YandexClassifieds/shiva/pkg/consul/sync"
	"github.com/YandexClassifieds/shiva/pkg/feature_flags"
	"github.com/YandexClassifieds/shiva/pkg/include"
	"github.com/YandexClassifieds/shiva/pkg/manifest"
	"github.com/YandexClassifieds/shiva/pkg/manifest/parser"
	"github.com/YandexClassifieds/shiva/pkg/registry"
	"github.com/YandexClassifieds/shiva/pkg/secrets/secret"
	"github.com/YandexClassifieds/shiva/pkg/service_map"
	"github.com/YandexClassifieds/shiva/pkg/staff"
	staffapi "github.com/YandexClassifieds/shiva/pkg/staff/api"
	"github.com/YandexClassifieds/shiva/pkg/tracker"
	stAPI "github.com/YandexClassifieds/shiva/pkg/tracker/api"
	"github.com/YandexClassifieds/shiva/test"
	shivaMock "github.com/YandexClassifieds/shiva/test/mock"
	mockDeployment "github.com/YandexClassifieds/shiva/test/mock/deployment"
	"github.com/YandexClassifieds/shiva/test/mock/mockery/mocks"
	"github.com/YandexClassifieds/shiva/test/mock/mq"
	"github.com/YandexClassifieds/shiva/test/mock/service_change"
	staff_fixture "github.com/YandexClassifieds/shiva/test/mock/staff"
	"github.com/YandexClassifieds/shiva/test/test_db"
	"github.com/google/uuid"
	"github.com/stretchr/testify/assert"
	mock2 "github.com/stretchr/testify/mock"
	"github.com/stretchr/testify/require"
)

type Test struct {
	t           *testing.T
	db          *storage.Database
	prodNomad   *shivaMock.Scheduler
	testNomad   *shivaMock.Scheduler
	prodYD      *shivaMock.Scheduler
	testYD      *shivaMock.Scheduler
	testBatch   *shivaMock.Scheduler
	prodBatch   *shivaMock.Scheduler
	lock        *mockDeployment.DMockLocker
	producer    *mockDeployment.ProducerMock
	registry    *shivaMock.Registry
	ssCliMock   *mocks.SecretClient
	ssMock      *shivaMock.AccessClientMock
	service     *deployment.Service
	storage     *model.Storage
	envStorage  *storage2.Storage
	ctxFactory  *dcontext.Factory
	syncS       *sync.Service
	featureFlag *feature_flags.Service
}

func newTest(t *testing.T) *Test {
	testNomad := shivaMock.NewMockScheduler()
	prodNomad := shivaMock.NewMockScheduler()
	testYD := shivaMock.NewMockScheduler()
	prodYD := shivaMock.NewMockScheduler()
	testBatch := shivaMock.NewMockScheduler()
	prodBatch := shivaMock.NewMockScheduler()
	lMock := mockDeployment.NewDMockLocker()
	stateProducer := mockDeployment.NewProducerMock()
	registryM := shivaMock.NewRegistry(false)
	ssCliMock := &mocks.SecretClient{}
	ssCliMock.On("GetSecrets", mock2.Anything, mock2.Anything).Return(nil, nil)

	db := test_db.NewDb(t)
	log := test.NewLogger(t)
	envReader := reader.NewService(db, log)
	flagSvc := feature_flags.NewService(db, mq.NewProducerMock(), log)
	deployDataSvc := data.NewService(db, log)
	trackerSvc := tracker.NewService(db, log, stAPI.NewApi(stAPI.NewConf(), log), deployDataSvc)
	includeSvc := include.NewService(db, log)
	notifySvc := service_change.NewNotificationMock()
	mapSvc := service_map.NewService(db, log, notifySvc)
	manifestSvc := manifest.NewService(db, log, parser.NewService(log, envReader), includeSvc)
	staffApi := staffapi.NewApi(staffapi.NewConf(), log)
	staffService := staff.NewService(db, staffApi, log)
	registrySvc := registry.NewService(registryM)
	issueLinkSvc := issue_link.NewService(db, log, trackerSvc)
	drillsSvc := drills.NewService(log, db)
	overrideSvc := override.NewService(db, log, includeSvc, manifestSvc)
	conf := scaler.NewConf()
	conf.CooldownDays = 1
	scaleSvc := scaler.NewService(deployDataSvc, manifestSvc, &shivaMock.PrometheusApi{}, &shivaMock.PrometheusApi{}, conf, db, log)
	kvMock := shivaMock.NewKVMock()
	syncSvc := sync.NewService(log, kvMock, sync.NewConf(t.Name()))
	require.NoError(t, syncSvc.SetLastUpdate(time.Now().Add(time.Hour)))
	secretSvc := secret.NewService(log, ssCliMock)
	ssMock := shivaMock.NewAccessClientMock(t)
	tmpSvc := template.NewService(mapSvc, envReader)
	envResolver := env_resolver.NewService(tmpSvc, secrets.NewService(secrets.NewConf(0), ssMock, log))

	deploySvc := &deployment.Service{
		Conf:         deployment.NewConf(),
		DB:           db,
		Log:          log,
		ChangedState: stateProducer,
		Locker:       lMock,
		TestNomad:    testNomad,
		ProdNomad:    prodNomad,
		TestYaDeploy: testYD,
		ProdYaDeploy: prodYD,
		TestBatch:    testBatch,
		ProdBatch:    prodBatch,
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
		EnvResolver:  envResolver,
	}
	deploySvc.Init()
	return &Test{
		db:          db,
		t:           t,
		prodNomad:   prodNomad,
		testNomad:   testNomad,
		prodYD:      prodYD,
		testYD:      testYD,
		testBatch:   testBatch,
		prodBatch:   prodBatch,
		lock:        lMock,
		producer:    stateProducer,
		service:     deploySvc,
		registry:    registryM,
		storage:     model.NewStorage(db, log),
		syncS:       syncSvc,
		featureFlag: flagSvc,
		ssCliMock:   ssCliMock,
		ssMock:      ssMock,
		envStorage:  storage2.NewStorage(db, log),
		ctxFactory: &dcontext.Factory{
			Store:            model.NewStorage(db, log),
			IncludeLinkStore: include_links.NewStorage(db, log),
			EnvOverrideStore: env_override.NewStorage(db, log),
			IssueLink:        issueLinkSvc,
			MapSvc:           mapSvc,
			Manifest:         manifestSvc,
			OverrideSvc:      overrideSvc,
			IncludeSvc:       includeSvc,
			StaffSvc:         staffService,
			TrackerSvc:       trackerSvc,
			DrillsSvc:        drillsSvc,
			ScalerSvc:        scaleSvc,
			SecretSvc:        secretSvc,
			Log:              log,
			EnvResolver:      envResolver,
		},
	}
}

func (T *Test) prepare(canary, sox, dss bool) {
	file := "testdata/manifest.yml"
	if canary {
		file = "testdata/manifest_canary.yml"
	}
	embedManifest(T.t, file, T.t.Name())
	embedMap(T.t, "testdata/map.yml", T.t.Name(), sox, dss)
}

func (T *Test) TestData() *TestData {
	return &TestData{
		ctx:     context.Background(),
		layer:   common.Test,
		login:   staff_fixture.Owner,
		name:    T.t.Name(),
		version: sVersion,
		comment: comment,
		source:  config.UnknownSource,
	}
}

func (T *TestData) Prod() *TestData {
	T.layer = common.Prod
	return T
}

func (T *TestData) Version(v string) *TestData {
	T.version = v
	return T
}

// work around `import cycle not allowed in test`
func (T *Test) DrillsEnable(l layer.Layer, dc string) {
	db := test.NewGorm(T.t)
	now := time.Now()
	require.NoError(T.t, db.Save(&drills2.Drills{
		Scheduler: store.Nomad,
		Layer:     l,
		DC:        dc,
		StartTime: now,
	}).Error)
}

func (T *Test) update(d *TestData) (chan *deployment.StateChange, *dcontext.Context, error) {
	return T.service.Update(d.ctx, model.UpdateParams{
		Layer:   d.layer,
		Name:    d.name,
		Branch:  d.branch,
		Login:   d.login,
		Comment: d.comment,
		Source:  config.AdminSource,
	})
}

func (T *Test) run(d *TestData) (chan *deployment.StateChange, *dcontext.Context, error) {
	return T.service.Run(d.ctx, model.RunParams{
		Layer:        d.layer,
		Name:         d.name,
		Version:      d.version,
		Branch:       d.branch,
		Login:        d.login,
		Comment:      d.comment,
		UserMetadata: d.metadata,
		Source:       d.source,
		OverrideConf: d.confOverride,
		OverrideEnv:  d.envOverride,
		Issues:       d.issues,
		TrafficShare: d.trafficShare,
		Force:        false,
	})
}

func (T *Test) restart(d *TestData) (chan *deployment.StateChange, *dcontext.Context, error) {
	return T.service.Restart(d.ctx, model.RestartParams{
		UUID:    uuid.New(),
		Layer:   d.layer,
		Name:    d.name,
		Branch:  d.branch,
		Login:   d.login,
		Comment: d.comment,
		Source:  d.source,
	})
}

func (T *Test) revert(d *TestData) (chan *deployment.StateChange, *dcontext.Context, error) {
	return T.service.Revert(d.ctx, model.RevertParams{
		UUID:    uuid.New(),
		Layer:   d.layer,
		Name:    d.name,
		Branch:  d.branch,
		Login:   d.login,
		Comment: d.comment,
		Source:  d.source,
	})
}

func (T *Test) stop(d *TestData) (chan *deployment.StateChange, *dcontext.Context, error) {
	return T.service.Stop(d.ctx, model.StopParams{
		UUID:    uuid.New(),
		Layer:   d.layer,
		Name:    d.name,
		Branch:  d.branch,
		Login:   d.login,
		Comment: d.comment,
		Source:  d.source,
	})
}

func (T *Test) cancel(d *TestData, ctx *dcontext.Context) (chan *deployment.StateChange, *dcontext.Context, error) {
	return T.service.Cancel(d.ctx, model.CancelParams{
		ID:     ctx.Deployment.ID,
		Login:  d.login,
		Source: d.source,
	})
}

func (T *Test) promote(d *TestData, ctx *dcontext.Context) (chan *deployment.StateChange, *dcontext.Context, error) {
	return T.service.Promote(d.ctx, model.PromoteParams{
		ID:     ctx.Deployment.ID,
		Login:  d.login,
		Source: d.source,
	})
}

func (T *Test) mirror(d *TestData, ctx *dcontext.Context) (chan *deployment.StateChange, *dcontext.Context, error) {
	return T.service.Mirror(d.ctx, model.MirrorParams{
		ID:           ctx.Deployment.ID,
		TargetLayer:  d.layer,
		TargetBranch: d.branch,
	})
}

func (T *Test) fullRunCanarySuccess(td *TestData) *dcontext.Context {
	td.layer = common.Prod
	runC, runCtx, err := T.run(td)
	require.NoError(T.t, err)
	require.NotNil(T.t, runC)

	T.sch(td).Process(T.t, common.Run, td.name, config.Canary, td.version)
	assertStateChan(T.t, scheduler.Process, model.CanaryProcess, runC)
	T.producer.Assert(T.t, runCtx, model.CanaryProcess)
	T.sch(td).Success(T.t, common.Run, td.name, config.Canary, td.version)
	assertStateChan(T.t, scheduler.Success, model.CanaryOnePercent, runC)
	T.producer.Assert(T.t, runCtx, model.CanaryOnePercent)
	pState, pCtx, err := T.service.Promote(context.Background(), model.PromoteParams{
		ID:     runCtx.Deployment.ID,
		Login:  td.login,
		Source: td.source,
	})
	require.NoError(T.t, err)
	require.Nil(T.t, pState)
	T.producer.Assert(T.t, runCtx, model.CanarySuccess)
	T.producer.AssertEmpty(T.t)
	T.lock.LockInc = 0
	T.lock.UnlockInc = 0
	T.producer.Reset()
	assert.Equal(T.t, model.CanarySuccess, pCtx.Deployment.State)
	return pCtx
}

func (T *Test) cleanLocker() {
	lMock := mockDeployment.NewDMockLocker()
	T.lock = lMock
	T.service.Locker = lMock
}

func (T *Test) fullRunCanary(td *TestData) *dcontext.Context {
	t := T.t

	td.layer = common.Prod
	runC, runCtx, err := T.run(td)
	require.NoError(t, err)
	require.NotNil(t, runC)

	T.sch(td).Process(t, common.Run, td.name, config.Canary, td.version)
	assertStateChan(t, scheduler.Process, model.CanaryProcess, runC)
	T.producer.Assert(t, runCtx, model.CanaryProcess)

	T.sch(td).Success(t, common.Run, td.name, config.Canary, td.version)
	assertStateChan(t, scheduler.Success, model.CanaryOnePercent, runC)
	T.producer.Assert(t, runCtx, model.CanaryOnePercent)

	_, _, err = T.service.Promote(context.Background(), model.PromoteParams{
		ID:     runCtx.Deployment.ID,
		Login:  td.login,
		Source: td.source,
	})
	require.NoError(t, err)
	T.producer.Assert(t, runCtx, model.CanarySuccess)

	_, p2Ctx, err := T.service.Promote(context.Background(), model.PromoteParams{
		ID:     runCtx.Deployment.ID,
		Login:  td.login,
		Source: td.source,
	})
	require.NoError(t, err)
	T.producer.Assert(t, p2Ctx, model.Process)
	T.sch(td).Success(t, common.Run, td.name, td.branch, td.version)
	T.sch(td).SuccessStop(t, common.Stop, td.name, config.Canary)
	T.producer.Assert(t, p2Ctx, model.CanaryStopped)
	T.producer.Assert(t, p2Ctx, model.Success)

	T.producer.AssertEmpty(t)
	T.lock.LockInc = 0
	T.lock.UnlockInc = 0
	T.producer.Reset()
	assert.Equal(t, model.Success, p2Ctx.Deployment.State)
	return p2Ctx
}

func (T *Test) fullRunCanaryOnePercent(td *TestData) *dcontext.Context {
	td.layer = common.Prod
	runC, runCtx, err := T.run(td)
	require.NoError(T.t, err)
	require.NotNil(T.t, runC)

	T.sch(td).Process(T.t, common.Run, td.name, config.Canary, td.version)
	assertStateChan(T.t, scheduler.Process, model.CanaryProcess, runC)
	T.producer.Assert(T.t, runCtx, model.CanaryProcess)
	T.sch(td).Success(T.t, common.Run, td.name, config.Canary, td.version)
	assertStateChan(T.t, scheduler.Success, model.CanaryOnePercent, runC)
	T.producer.Assert(T.t, runCtx, model.CanaryOnePercent)
	T.producer.AssertEmpty(T.t)
	T.lock.LockInc = 0
	T.lock.UnlockInc = 0
	T.producer.Reset()
	assert.Equal(T.t, model.CanaryOnePercent, runCtx.Deployment.State)
	return runCtx
}

func (T *Test) sch(td *TestData) *shivaMock.Scheduler {
	switch td.layer {
	case common.Prod:
		return T.prodNomad
	case common.Test:
		return T.testNomad
	default:
		assert.FailNow(T.t, "unknown layer")
		return nil
	}
}

func (T *Test) fullRunSox(td *TestData) *dcontext.Context {
	t := T.t
	T.lock.LockInc = 0
	T.lock.UnlockInc = 0
	runC, runCtx, err := T.run(td)
	require.NoError(t, err)
	require.Nil(t, runC)
	assert.Equal(t, 0, T.lock.LockInc)
	assert.Equal(t, 0, T.lock.UnlockInc)
	T.producer.Assert(t, runCtx, model.WaitApprove)
	require.True(t, runCtx.Deployment.ID > 0)
	assert.Equal(t, model.WaitApprove, runCtx.Deployment.State)
	T.producer.AssertEmpty(t)
	return runCtx
}

func (T *Test) fullRun(td *TestData) *dcontext.Context {
	t := T.t
	sch := T.sch(td)
	T.lock.LockInc = 0
	T.lock.UnlockInc = 0
	runC, runCtx, err := T.run(td)
	require.NoError(t, err)

	sch.Prepare(t, common.Run, td.name, td.branch, td.version)
	assertStateChan(t, scheduler.Prepare, model.Prepare, runC)
	sch.Success(t, common.Run, td.name, td.branch, td.version)
	assertStateChan(t, scheduler.Success, model.Success, runC)
	d := T.assertUpdatedState(model.Success, runCtx)
	require.Equal(t, "", d.Description)
	T.producer.Assert(t, runCtx, model.Process)
	T.producer.Assert(t, runCtx, model.Success)
	assert.Equal(t, 1, len(sch.RunCtxs))
	assert.Equal(t, 1, T.lock.LockInc)
	assert.Equal(t, 1, T.lock.UnlockInc)
	sch.RunCtxs = nil
	T.lock.LockInc = 0
	T.lock.UnlockInc = 0
	T.producer.Reset()
	assert.Equal(t, model.Success, runCtx.Deployment.State)
	return runCtx
}

func (T *Test) assertUpdatedState(state model.State, ctx *dcontext.Context) *model.Deployment {

	return assertUpdatedState(T.t, T.storage, ctx, state)
}

func assertUpdatedState(t *testing.T, s *model.Storage, ctx *dcontext.Context, state model.State) *model.Deployment {

	var result *model.Deployment
	test.Wait(t, func() error {
		d, err := s.Get(ctx.Deployment.ID)
		if err != nil {
			return err
		}
		if d.State != state {
			return fmt.Errorf("deployment '%d' have '%s' state but want '%s'", ctx.Deployment.ID, d.State.String(), state.String())
		}
		result = d
		return nil
	})
	return result
}

func assertDState(t *testing.T, expected, actual model.State) {

	assert.Equal(t, expected.String(), actual.String())
}

func assertStateChan(t *testing.T, s scheduler.StateType, dS model.State, c chan *deployment.StateChange) {
	t.Helper()
	select {
	case result := <-c:
		assert.Equal(t, s.String(), result.SchState.StateType.String())
		assert.Equal(t, dS.String(), result.DState.String())
	case <-time.After(3 * time.Second):
		assert.FailNow(t, "Result not found", "%s, %s", s.String(), dS.String())
	}
}

type TestData struct {
	ctx                                           context.Context
	layer                                         common.Layer
	login, name, version, branch, comment, source string
	metadata                                      string
	confOverride, issues                          []string
	envOverride                                   map[string]string
	trafficShare                                  bool
}

var (
	paramsRun = model.RunParams{
		Layer:   common.Test,
		Name:    "yandex_vertis_example_service_d",
		Version: "1.0.0",
		Login:   staff_fixture.Owner,
		Comment: "It is comment",
		Source:  config.UnknownSource,
	}
	paramsRunNext = model.RunParams{
		Layer:   common.Test,
		Name:    "yandex_vertis_example_service_d",
		Version: "1.0.1",
		Login:   staff_fixture.Owner,
		Comment: "It is comment",
		Source:  config.UnknownSource,
	}

	secretEnvs    = map[string]string{"k1": "${sec_id-1:ver-1:k1}"}
	secretEnvsNew = map[string]string{"k1": "${sec_id-1:ver-2:k1}", "k2": "${sec_id-1:ver-2:k2}"}

	//go:embed testdata/*
	testFS embed.FS

	//go:embed testdata/map.yml
	testMap string
	//go:embed testdata/map1.yml
	testMap1 string

	//go:embed testdata/manifest.yml
	testManifest string
	//go:embed testdata/manifest1.yml
	testManifest1 string
	//go:embed testdata/manifest_include1.yml
	testManifestWithIncludes1 string
	//go:embed testdata/manifest_include2.yml
	testManifestWithIncludes2 string
	//go:embed testdata/manifest_env.yml
	testManifestEnv string
	//go:embed testdata/manifest_droog.yml
	testManifestDroog string
	//go:embed testdata/manifest_any.yml
	testManifestAny string
	//go:embed testdata/manifest_auto_cpu.yml
	testManifestAutoCpu string
	//go:embed testdata/manifest_secret.yml
	testManifestSecret string
)

const (
	sName     = "yandex_vertis_example_service_d"
	sVersion0 = "0.0.1"
	sVersion  = "1.0.0"
	sVersion2 = "1.0.2"
	comment   = "It is comment"

	commonYml  = `COMMON_PARAM: common param`
	commonYml1 = `COMMON_PARAM: common param 1`
	testYml    = `TEST_PARAM: test param`
	testYml1   = `TEST_PARAM: test param1`

	prodYml = `
PROD_PARAM: prod param
SECRET_PARAM: ${sec-42:ver-42123:aaa}
`

	secVersionOld = "ver-1"
)
