package deployment_test

import (
	"context"
	"errors"
	"fmt"
	"strconv"
	"testing"
	"time"

	"github.com/YandexClassifieds/shiva/cmd/shiva/deployment/model"
	"github.com/YandexClassifieds/shiva/cmd/shiva/deployment/processor"
	"github.com/YandexClassifieds/shiva/cmd/shiva/env/external/storage"
	"github.com/YandexClassifieds/shiva/cmd/shiva/scheduler"
	"github.com/YandexClassifieds/shiva/common"
	"github.com/YandexClassifieds/shiva/common/config"
	"github.com/YandexClassifieds/shiva/common/user_error"
	"github.com/YandexClassifieds/shiva/pb/shiva/types/env"
	"github.com/YandexClassifieds/shiva/pb/shiva/types/traffic"
	tmp_errors "github.com/YandexClassifieds/shiva/pkg/template/errors"
	"github.com/YandexClassifieds/shiva/test"
	staff_fixture "github.com/YandexClassifieds/shiva/test/mock/staff"
	"github.com/google/uuid"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
)

func TestCanaryFullPipeline(t *testing.T) {
	test.RunUp(t)
	defer test.Down(t)
	T := newTest(t)
	T.prepare(true, false, false)
	ctx := context.Background()
	T.fullRun(T.TestData().Prod().Version(sVersion0))

	data := T.TestData()
	data.issues = []string{"VOID-256"}
	runCtx := T.fullRun(data)

	promoteC, promoteCtx, err := T.service.Promote(ctx, model.PromoteParams{
		ID:     runCtx.Deployment.ID,
		Login:  staff_fixture.Owner,
		Source: config.UnknownSource,
	})
	require.NoError(t, err)
	T.producer.Assert(t, promoteCtx, model.CanaryProcess)

	T.prodNomad.Success(t, common.Run, t.Name(), config.Canary, sVersion)
	assertStateChan(t, scheduler.Success, model.CanaryOnePercent, promoteC)
	assert.Equal(t, model.CanaryOnePercent, promoteCtx.Deployment.State)
	T.producer.Assert(t, promoteCtx, model.CanaryOnePercent)
	assert.Equal(t, "", promoteCtx.Deployment.Branch)
	assert.Equal(t, traffic.Traffic_UNKNOWN, promoteCtx.Deployment.Traffic)

	secondPState, secondPContext, err := T.service.Promote(ctx, model.PromoteParams{
		ID:     promoteCtx.Deployment.ID,
		Login:  data.login,
		Source: config.UnknownSource,
	})
	require.NoError(t, err)
	assert.Nil(t, secondPState)
	assertDState(t, model.CanarySuccess, secondPContext.Deployment.State)
	T.producer.Assert(t, promoteCtx, model.CanarySuccess)

	promote2C, promote2Ctx, err := T.service.Promote(ctx, model.PromoteParams{
		ID:     promoteCtx.Deployment.ID,
		Login:  data.login,
		Source: config.UnknownSource,
	})
	require.NoError(t, err)
	T.producer.Assert(t, promote2Ctx, model.Process)
	T.prodNomad.Success(t, common.Run, t.Name(), "", sVersion)
	T.prodNomad.SuccessStop(t, common.Stop, t.Name(), config.Canary)
	assertStateChan(t, scheduler.Success, model.Success, promote2C)
	assertDState(t, model.Success, promote2Ctx.Deployment.State)
	T.producer.Assert(t, promote2Ctx, model.CanaryStopped)
	T.producer.Assert(t, promote2Ctx, model.Success)
}

func TestStopCanaryByFail(t *testing.T) {

	test.RunUp(t)
	defer test.Down(t)
	T := newTest(t)
	T.prepare(true, false, false)
	ctx := context.Background()
	T.fullRun(T.TestData().Prod().Version(sVersion0))

	data := T.TestData()
	data.issues = []string{"VOID-256"}
	runCtx := T.fullRun(data)

	promoteC, promoteCtx, err := T.service.Promote(ctx, model.PromoteParams{
		ID:     runCtx.Deployment.ID,
		Login:  data.login,
		Source: config.UnknownSource,
	})
	require.NoError(t, err)
	T.producer.Assert(t, promoteCtx, model.CanaryProcess)

	assert.Equal(t, 0, len(T.prodNomad.StopCtxs))
	T.prodNomad.Fail(t, common.Run, t.Name(), config.Canary, sVersion)
	T.prodNomad.SuccessStop(t, common.Stop, t.Name(), config.Canary)
	assertStateChan(t, scheduler.Fail, model.Fail, promoteC)
	assertDState(t, model.Fail, promoteCtx.Deployment.State)
	T.producer.Assert(t, promoteCtx, model.Fail)
	T.producer.AssertEmpty(t)
}

func TestRunCanaryWithTemplates(t *testing.T) {
	test.RunUp(t)
	defer test.Down(t)

	T := newTest(t)

	embedMap(T.t, "testdata/map.yml", T.t.Name(), false, false)
	embedMap(t, "testdata/map.yml", "shiva", false, false)
	embedManifest(T.t, "testdata/manifest_canary_env.yml", T.t.Name())

	envStorage := storage.NewStorage(T.db, test.NewLogger(t))
	require.NoError(t, envStorage.Save(&storage.ExternalEnv{
		Service: "shiva",
		Layer:   common.Test,
		Type:    env.EnvType_GENERATED_TVM_ID,
		Key:     "tvm-id",
		Value:   "10",
	}))

	td := T.TestData()

	_, canaryCtx, err := T.run(td)
	require.NoError(t, err)

	assert.Equal(t, "10", canaryCtx.Envs["test_param1"])
	assert.Equal(t, "80", canaryCtx.Envs["test_param2"])
	assert.Equal(t, "http://shiva-deploy.vrts-slb.test.vertis.yandex.net:80", canaryCtx.Envs["test_param3"])
	assert.Equal(t, "shiva-deploy.vrts-slb.test.vertis.yandex.net", canaryCtx.Envs["test_param4"])
}

func TestRunCanaryInvalidTemplates(t *testing.T) {
	test.RunUp(t)
	defer test.Down(t)

	T := newTest(t)

	embedMap(T.t, "testdata/map.yml", T.t.Name(), false, false)
	embedMap(t, "testdata/map.yml", "shiva", false, false)
	embedManifest(T.t, "testdata/manifest_canary_invalid_env.yml", T.t.Name())

	td := T.TestData()

	_, _, err := T.run(td)

	var ue *user_error.UserErrors
	require.True(t, errors.As(err, &ue))

	errs := ue.Get()

	assert.Len(t, errs, 4)
	assert.Contains(t, errs, tmp_errors.NewErrUnknownService("test_param1", "shiva-ci"))
	assert.Contains(t, errs, tmp_errors.NewErrInvalidProvider("test_param2", "shiva", "admin"))
	assert.Contains(t, errs, tmp_errors.NewErrInvalidProvider("test_param3", "shiva", "api"))
	assert.Contains(t, errs, tmp_errors.NewErrInvalidProvider("test_param4", "shiva", "admin"))
}

func TestStopCanaryByCancelPromote(t *testing.T) {
	test.RunUp(t)
	defer test.Down(t)

	T := newTest(t)
	T.prepare(true, false, false)
	T.fullRun(T.TestData().Prod().Version(sVersion0))

	td := T.TestData()
	td.issues = []string{"VOID-256"}
	td.layer = common.Prod
	canaryCtx := T.fullRunCanarySuccess(td)

	_, promoteCtx, err := T.promote(td, canaryCtx)
	require.NoError(t, err)
	T.producer.Assert(t, promoteCtx, model.Process)

	_, cancelCtx, err := T.cancel(td, promoteCtx)
	require.NoError(t, err)
	T.producer.Assert(t, cancelCtx, model.Process)

	T.prodNomad.Canceled(t, common.Cancel, td.name, td.branch, fmt.Sprintf("%d/%d", cancelCtx.Parent.Deployment.NomadID, cancelCtx.Deployment.NomadID))
	T.prodNomad.SuccessStop(t, common.Stop, td.name, config.Canary)
	T.producer.Assert(t, promoteCtx, model.Canceled)
	T.producer.Assert(t, cancelCtx, model.Success)
}

func TestRunCanaryOverCanary(t *testing.T) {
	test.RunUp(t)
	defer test.Down(t)
	T := newTest(t)
	T.prepare(true, false, false)
	T.fullRun(T.TestData().Prod().Version(sVersion0))

	data := T.TestData()
	data.issues = []string{"VOID-256"}
	data.layer = common.Prod
	firstCanary := T.fullRunCanarySuccess(data)
	data.version = sVersion2
	secondCanary := T.fullRunCanarySuccess(data)
	require.NotNil(t, secondCanary.Previous)
	firstCanaryD, err := T.storage.Get(firstCanary.Deployment.ID)
	require.NoError(t, err)
	assert.Equal(t, model.CanaryCanceled, firstCanaryD.State)
	assert.Equal(t, sVersion2, secondCanary.Deployment.Version)
}

func TestPromoteCanaryOverCanary(t *testing.T) {
	test.RunUp(t)
	defer test.Down(t)
	T := newTest(t)
	T.prepare(true, false, false)
	T.fullRun(T.TestData().Prod().Version(sVersion0))

	td := T.TestData()
	td.issues = []string{"VOID-256"}
	td.layer = common.Prod
	firstCanary := T.fullRunCanarySuccess(td)

	td = T.TestData()
	td.issues = []string{"VOID-256"}
	td.version = sVersion2
	secondOnTest := T.fullRun(td)
	require.Nil(t, secondOnTest.Previous)

	c, secondCanary, err := T.service.Promote(td.ctx, model.PromoteParams{
		ID:     secondOnTest.Deployment.ID,
		Login:  td.login,
		Source: td.source,
	})
	require.NoError(t, err)
	T.producer.Assert(t, secondCanary, model.CanaryProcess)
	T.prodNomad.Success(T.t, common.Run, td.name, config.Canary, td.version)
	assertStateChan(t, scheduler.Success, model.CanaryOnePercent, c)
	assert.Equal(T.t, model.CanaryOnePercent, secondCanary.Deployment.State)
	T.producer.Assert(t, secondCanary, model.CanaryOnePercent)

	firstCanaryD, err := T.storage.Get(firstCanary.Deployment.ID)
	require.NoError(t, err)
	assertDState(t, model.CanaryCanceled, firstCanaryD.State)
	assert.Equal(t, sVersion2, secondCanary.Deployment.Version)
}

func TestRunBranchWithCanaryName(t *testing.T) {
	test.RunUp(t)
	defer test.Down(t)
	T := newTest(t)
	T.prepare(true, false, false)
	T.fullRun(T.TestData().Prod().Version(sVersion0))
	td := T.TestData()
	td.branch = config.Canary
	_, _, err := T.run(td)
	assert.Equal(t, processor.ErrRunReservedBranchName, err)
}

func TestRunBranchCanaryOn(t *testing.T) {
	test.RunUp(t)
	defer test.Down(t)
	T := newTest(t)
	T.prepare(true, false, false)
	T.fullRun(T.TestData().Prod().Version(sVersion0))
	td := T.TestData()
	td.layer = common.Prod
	td.branch = "bbb"
	T.fullRun(td)
}

func TestCancelCanaryProcess(t *testing.T) {
	test.RunUp(t)
	defer test.Down(t)
	T := newTest(t)
	T.prepare(true, false, false)
	T.fullRun(T.TestData().Prod().Version(sVersion0))

	td := T.TestData()
	td.issues = []string{"VOID-256"}
	td.layer = common.Prod
	_, canaryCtx, err := T.run(td)
	require.NoError(t, err)
	assertDState(t, model.CanaryProcess, canaryCtx.Deployment.State)
	T.producer.Assert(t, canaryCtx, model.CanaryProcess)

	td = T.TestData()
	cancelC, cancelCtx, err := T.service.Cancel(td.ctx, model.CancelParams{
		ID:     canaryCtx.Deployment.ID,
		Login:  td.login,
		Source: td.source,
	})
	require.NoError(t, err)
	assertDState(t, model.Process, cancelCtx.Deployment.State)
	T.producer.Assert(t, cancelCtx, model.Process)

	T.prodNomad.Success(t, common.Stop, td.name, config.Canary, "")
	assertStateChan(t, scheduler.Success, model.Success, cancelC)
	T.assertUpdatedState(model.CanaryCanceled, canaryCtx)
	assertDState(t, model.Success, cancelCtx.Deployment.State)
	T.producer.Assert(t, canaryCtx, model.CanaryCanceled)
}

func TestCancelCanarySuccess(t *testing.T) {
	test.RunUp(t)
	defer test.Down(t)
	T := newTest(t)
	T.prepare(true, false, false)
	T.fullRun(T.TestData().Prod().Version(sVersion0))

	td := T.TestData()
	td.issues = []string{"VOID-256"}
	td.layer = common.Prod
	canary := T.fullRunCanarySuccess(td)
	T.cleanLocker()
	d := canary.Deployment

	cancelC, cancelCtx, err := T.service.Cancel(td.ctx, model.CancelParams{
		ID:     d.ID,
		Login:  td.login,
		Source: td.source,
	})
	require.NoError(t, err)
	assert.Equal(t, model.Process, cancelCtx.Deployment.State)
	T.producer.Assert(t, cancelCtx, model.Process)

	T.prodNomad.Success(t, common.Stop, d.Name, config.Canary, "")
	assertStateChan(t, scheduler.Success, model.Success, cancelC)
	assert.Equal(t, model.Success, cancelCtx.Deployment.State)
	T.producer.Assert(t, canary, model.CanaryStopped)
	T.producer.Assert(t, cancelCtx, model.Success)

	canaryD, err := T.storage.Get(d.ID)
	require.NoError(t, err)
	assert.Equal(t, model.CanaryStopped, canaryD.State)
	assert.Equal(t, 1, len(T.prodNomad.StopCtxs))

	_, _, err = T.service.Cancel(td.ctx, model.CancelParams{
		ID:     d.ID,
		Login:  td.login,
		Source: td.source,
	})
	assert.Equal(t, processor.ErrCannotCancelCompletedDeployment, err)
}

func TestCancelCanaryOnePercent(t *testing.T) {
	test.RunUp(t)
	defer test.Down(t)
	T := newTest(t)
	T.prepare(true, false, false)
	T.fullRun(T.TestData().Prod().Version(sVersion0))

	td := T.TestData()
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

	d := runCtx.Deployment
	cancelC, cancelCtx, err := T.service.Cancel(td.ctx, model.CancelParams{
		ID:     d.ID,
		Login:  td.login,
		Source: td.source,
	})
	require.NoError(t, err)
	assert.Equal(t, model.Process, cancelCtx.Deployment.State)
	T.producer.Assert(t, cancelCtx, model.Process)

	T.prodNomad.Success(t, common.Stop, d.Name, config.Canary, "")
	assertStateChan(t, scheduler.Success, model.Success, cancelC)
	assert.Equal(t, model.Success, cancelCtx.Deployment.State)
	T.producer.Assert(t, runCtx, model.CanaryStopped)
	T.producer.Assert(t, cancelCtx, model.Success)

	canaryD, err := T.storage.Get(d.ID)
	require.NoError(t, err)
	assert.Equal(t, model.CanaryStopped, canaryD.State)
	assert.Equal(t, 1, len(T.prodNomad.StopCtxs))
}

func TestRestoreCanaryProcess_Run(t *testing.T) {
	test.RunUp(t)
	defer test.Down(t)
	T := newTest(t)
	T.prepare(true, false, false)
	T.fullRun(T.TestData().Prod().Version(sVersion0))

	td := T.TestData()
	td.issues = []string{"VOID-256"}
	td.layer = common.Prod
	_, canaryCtx, err := T.run(td)
	require.NoError(t, err)
	assertDState(t, model.CanaryProcess, canaryCtx.Deployment.State)

	T2 := newTest(t)
	T2.service.GlobalRestore()
	T2.prodNomad.Success(T2.t, common.Run, td.name, "", "id:"+strconv.FormatInt(canaryCtx.Deployment.NomadID, 10))
	T2.assertUpdatedState(model.CanaryOnePercent, canaryCtx)
}

func TestRestoreCanaryProcess_Promote(t *testing.T) {
	test.RunUp(t)
	defer test.Down(t)
	T := newTest(t)
	T.prepare(true, false, false)
	T.fullRun(T.TestData().Prod().Version(sVersion0))

	td := T.TestData()
	td.issues = []string{"VOID-256"}
	td.version = sVersion2
	secondOnTest := T.fullRun(td)
	require.Nil(t, secondOnTest.Previous)

	_, canaryCtx, err := T.service.Promote(td.ctx, model.PromoteParams{
		ID:     secondOnTest.Deployment.ID,
		Login:  td.login,
		Source: td.source,
	})
	require.NoError(t, err)
	T.producer.Assert(t, canaryCtx, model.CanaryProcess)

	T2 := newTest(t)
	T2.service.GlobalRestore()
	T2.prodNomad.Success(T2.t, common.Run, td.name, "", "id:"+strconv.FormatInt(canaryCtx.Deployment.NomadID, 10))
	T2.assertUpdatedState(model.CanaryOnePercent, canaryCtx)
}

func TestRestoreCanaryOnePercent(t *testing.T) {
	test.RunUp(t)
	defer test.Down(t)
	T := newTest(t)
	T.prepare(true, false, false)
	T.fullRun(T.TestData().Prod().Version(sVersion0))
	T.fullRunCanaryOnePercent(T.TestData())

	T2 := newTest(t)
	assert.True(t, T2.prodNomad.IsEmpty())
	assert.True(t, T2.testNomad.IsEmpty())
}

func TestStateCanaryOnePercent(t *testing.T) {
	test.RunUp(t)
	defer test.Down(t)
	T := newTest(t)
	T.prepare(true, false, false)
	T.fullRun(T.TestData().Prod().Version(sVersion0))
	td := T.TestData()
	ctx := T.fullRunCanaryOnePercent(td)

	stateC, stateCtx, err := T.service.State(td.ctx, ctx.Deployment.ID)
	require.NoError(t, err)
	assert.Nil(t, stateC)
	assert.Equal(t, "", stateCtx.Description)
	assertDState(t, model.CanaryOnePercent, stateCtx.Deployment.State)
}

func TestFirstRunSkipCanary(t *testing.T) {
	test.RunUp(t)
	defer test.Down(t)
	T := newTest(t)
	T.prepare(true, false, false)
	td := T.TestData().Prod()
	T.fullRun(td)

	stateC, stopCtx, err := T.stop(td)
	require.NoError(t, err)
	T.producer.Assert(t, stopCtx, model.Process)
	T.prodNomad.SuccessStop(t, common.Stop, td.name, td.branch)
	T.prodNomad.SuccessStop(t, common.Stop, td.name, config.Canary)
	assertStateChan(t, scheduler.Success, model.Success, stateC)
	T.producer.Assert(t, stopCtx, model.Success)

	T.fullRun(td)
}

func TestStopCanary(t *testing.T) {
	test.RunUp(t)
	defer test.Down(t)
	T := newTest(t)
	T.prepare(true, false, false)
	T.fullRun(T.TestData().Prod().Version(sVersion0))
	td := T.TestData().Prod()
	T.fullRunCanary(td)

	stateC, stopCtx, err := T.stop(td)
	require.NoError(t, err)
	T.producer.Assert(t, stopCtx, model.Process)
	T.prodNomad.SuccessStop(t, common.Stop, td.name, td.branch)
	T.prodNomad.SuccessStop(t, common.Stop, td.name, config.Canary)
	assertStateChan(t, scheduler.Success, model.Success, stateC)
	T.producer.Assert(t, stopCtx, model.Success)
}

func TestRevertCanary(t *testing.T) {
	test.RunUp(t)
	defer test.Down(t)
	T := newTest(t)
	T.prepare(true, false, false)
	T.fullRun(T.TestData().Prod().Version(sVersion0))
	td := T.TestData()
	T.fullRunCanary(td)
	td.version = sVersion2
	T.fullRunCanary(td)

	revertC, revertCtx, err := T.service.Revert(td.ctx, model.RevertParams{
		UUID:    uuid.New(),
		Layer:   td.layer,
		Name:    td.name,
		Branch:  td.branch,
		Login:   td.login,
		Comment: td.comment,
		Source:  td.source,
	})
	require.NoError(t, err)
	T.producer.Assert(t, revertCtx, model.Process)
	T.sch(td).Success(t, common.Run, td.name, td.branch, sVersion)
	assertStateChan(t, scheduler.Success, model.Success, revertC)
	T.producer.Assert(t, revertCtx, model.Success)
}

func TestStateCanarySuccess(t *testing.T) {
	test.RunUp(t)
	defer test.Down(t)
	T := newTest(t)
	T.prepare(true, false, false)
	T.fullRun(T.TestData().Prod().Version(sVersion0))
	td := T.TestData()
	ctx := T.fullRunCanarySuccess(td)

	stateC, stateCtx, err := T.service.State(td.ctx, ctx.Deployment.ID)
	require.NoError(t, err)
	assert.Nil(t, stateC)
	assert.Equal(t, "", stateCtx.Description)
	assertDState(t, model.CanarySuccess, stateCtx.Deployment.State)
}

func TestRestoreCanarySuccess(t *testing.T) {
	test.RunUp(t)
	defer test.Down(t)
	T := newTest(t)
	T.prepare(true, false, false)
	T.fullRun(T.TestData().Prod().Version(sVersion0))
	T.fullRunCanarySuccess(T.TestData())

	T2 := newTest(t)
	time.Sleep(500 * time.Millisecond)
	assert.True(t, T2.prodNomad.IsEmpty())
	assert.True(t, T2.testNomad.IsEmpty())
}
