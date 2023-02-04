//go:build !unit || parallel
// +build !unit parallel

package scheduler_test

import (
	_ "embed"
	"errors"
	"fmt"
	"net/http"
	"strings"
	"sync"
	"testing"
	"time"

	"github.com/YandexClassifieds/go-common/tvm/tvmauth"
	"github.com/YandexClassifieds/shiva/cmd/shiva/env/external/reader"
	"github.com/YandexClassifieds/shiva/cmd/shiva/env/external/storage"
	"github.com/YandexClassifieds/shiva/cmd/shiva/env/manifest"
	"github.com/YandexClassifieds/shiva/cmd/shiva/scheduler"
	"github.com/YandexClassifieds/shiva/cmd/shiva/scheduler/nomad"
	"github.com/YandexClassifieds/shiva/cmd/shiva/scheduler/test_helpers"
	"github.com/YandexClassifieds/shiva/common"
	"github.com/YandexClassifieds/shiva/common/config"
	proto "github.com/YandexClassifieds/shiva/pb/shiva/service_map"
	"github.com/YandexClassifieds/shiva/pb/shiva/types/env"
	"github.com/YandexClassifieds/shiva/pb/shiva/types/revert"
	"github.com/YandexClassifieds/shiva/pkg/consul/kv"
	"github.com/YandexClassifieds/shiva/pkg/secrets"
	"github.com/YandexClassifieds/shiva/pkg/service_map"
	"github.com/YandexClassifieds/shiva/pkg/template"
	"github.com/YandexClassifieds/shiva/test"
	"github.com/YandexClassifieds/shiva/test/mock/service_change"
	"github.com/YandexClassifieds/shiva/test/test_db"
	"github.com/hashicorp/nomad/api"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
)

const (
	branch = "b"
)

var (
	//go:embed test_data/shiva.yml
	shivaMap string
)

type Wrapper interface {
	// For change Conf
	New(t *testing.T) Wrapper
	NewDownProgressDeadline(t *testing.T) Wrapper
	NewShutdownDelay(t *testing.T, delay time.Duration) Wrapper
	DefaultDC() []string
	GlobalClean(t *testing.T)
	Before(t *testing.T)
	After(t *testing.T)
	Scheduler() scheduler.Scheduler
	ProgressDeadline() time.Duration
	// TODO https://st.yandex-team.ru/VOID-1128
	Terminate() revert.RevertType
	// TODO https://st.yandex-team.ru/VOID-1150
	OOM() revert.RevertType
	// TODO https://st.yandex-team.ru/VOID-1151
	HasFailAllocations() bool
}

func TestNomad(t *testing.T) {
	test.InitTestEnv()
	w := NewNomadWrapper()
	globalClean(t, w)

	t.Run("Run", BaseTest(w, true, false, testRun))
	t.Run("Run-Fail", BaseTest(w, false, false, testFailFirstRun))
	t.Run("Run-Unhealthy", BaseTest(w, false, false, testUnhealthyFirstRun))
	t.Run("Run-Branch", BaseTest(w, true, true, testRunAndStopBranch))
	t.Run("Run-Canary", BaseTest(w, false, false, testRunCanary))
	t.Run("Restart", BaseTest(w, true, false, testRestart))
	t.Run("Restart-Branch", BaseTest(w, true, true, testRestartBranch))
	t.Run("Update-OOM", BaseTest(w, true, false, testUpdateOOM))
	t.Run("Update-Fail", BaseTest(w, true, false, testUpdateFail))
	t.Run("Update", BaseTest(w, true, false, testUpdate))
	t.Run("Update-Unhealthy", BaseTest(w, true, false, testUnhealthyAndRevert))
	t.Run("Update-OverrideCPU", BaseTest(w, true, false, testUpdateOverrideCPU))
	t.Run("State-Update", BaseTest(w, true, false, testState))
	t.Run("Envs", BaseTest(w, true, true, testEnvs))
	t.Run("Secrets", BaseTest(w, false, false, testServiceWithSecrets))
	t.Run("Templates", BaseTest(w, false, false, testServiceWithTemplates))
	t.Run("OldAddress", BaseTest(w, true, false, testOldAddress))
	t.Run("Error-StopNonexistent", BaseTest(w, false, false, testStopNonexistentService))
	t.Run("Error-RestartNonexistent", BaseTest(w, false, false, testRestartNonexistentService))
	t.Run("Run-Cancel", BaseTest(w, false, false, testCancelFirstRun))
	t.Run("Update-Cancel", BaseTest(w, true, false, testCancelRunProcess))
	t.Run("Error-CancelCompleted", BaseTest(w, false, false, testCancelCompleted))
	t.Run("RescheduleSuccess", BaseTest(w, false, false, testReschedule))
	t.Run("ServiceFlapAfterDeploy", BaseTest(w, false, false, testServiceFlapAfterDeploy))
	t.Run("RestartFailed", BaseTest(w, true, false, testRestartFailed))
	t.Run("Restart-Cancel", BaseTest(w, true, false, testCancelRestartProcess))
	t.Run("Update-MaxParallel", BaseTest(w, true, false, testMaxParallel))
	t.Run("State-Restart", BaseTest(w, true, false, testStateByRestart))
	t.Run("State-End", BaseTest(w, false, false, testEndState))
	t.Run("State-Fail", BaseTest(w, true, false, testStateFail))
	t.Run("HealthCheck-TCP", BaseTest(w, true, false, testTCPHealthCheck))
	t.Run("HealthCheck-HTTP", BaseTest(w, true, false, testHttpHealthCheck))
	t.Run("HealthCheck-GRPC", BaseTest(w, true, false, testGrpcHealthCheck))
	t.Run("HealthCheck-No", BaseTest(w, true, false, testWithoutHealthCheck))
	t.Run("Resize", BaseTest(w, true, false, testResizeOffAndReturn))
	t.Run("ResizeBranch", BaseTest(w, true, true, testResizeBranchAndReturn))
	t.Run("RunBranchOffDC", BaseTest(w, false, false, testRunBranchWithOffDC))
	t.Run("ResizeRunResize", BaseTest(w, true, false, testResizeRunResize))
	t.Run("ResizeWithoutChange", BaseTest(w, true, false, testResizeWithoutChange))
	t.Run("ShutdownDelay", BaseTest(w, false, false, testShutdownDelay))
	t.Run("RunAnyDC", BaseTest(w, false, false, testRunWithAnyDC))
	t.Run("FailedAllocationsCount", BaseTest(w, true, false, testFailedAllocationsCount))
}

func TestYD(t *testing.T) {
	t.Skip("Yandex deploy not support")
	test.InitTestEnv()
	w := NewYDWrapper()
	globalClean(t, w)

	// TODO https://st.yandex-team.ru/VOID-1129
	//t.Run("Update-MaxParallel", BaseTest(w, true, false, testMaxParallel))

	// TODO https://st.yandex-team.ru/VOID-1131
	// t.Run("State-Restart", BaseTest(w, true, false, testStateByRestart))
	//t.Run("State-End", BaseTest(w, false, false, testEndState))
	//t.Run("State-Fail", BaseTest(w, false, false, testStateFail))

	// TODO https://st.yandex-team.ru/VOID-1132
	//t.Run("HealthCheck-TCP", BaseTest(w, true, false, testTCPHealthCheck))
	//t.Run("HealthCheck-HTTP", BaseTest(w, true, false, testHttpHealthCheck))
	//t.Run("HealthCheck-GRPC", BaseTest(w, true, false, testGrpcHealthCheck))
	//t.Run("HealthCheck-No", BaseTest(w, true, false, testWithoutHealthCheck))

	// TODO https://st.yandex-team.ru/VOID-1134
	//t.Run("Sidecar", BaseTest(w, true, false, testMakeWithSidecar))
	//t.Run("WithoutSidecar", BaseTest(w, true, false, testMakeWithoutSidecar))
}

func BaseTest(w Wrapper, isUpdate, isBranch bool, f func(t *testing.T, w Wrapper)) func(t *testing.T) {
	return func(t *testing.T) {
		wrapper := w.New(t)
		wrapper.Before(t)
		defer catchPanic(t)
		defer stop(t, wrapper, isBranch)
		if isUpdate {
			runDefault(t, wrapper, isBranch)
		}
		f(t, wrapper)
		w.After(t)
	}
}

func globalClean(t *testing.T, w Wrapper) {
	wrapper := w.New(t)
	wrapper.GlobalClean(t)
}

func testStateFail(t *testing.T, w Wrapper) {

	start := time.Now()
	m := defaultManifest(t, w)
	m.Config.Params["FAIL"] = "true"
	m.Config.Params["FAIL_TIME"] = "3s"

	ctx := scheduler.MakeContext(test_helpers.UpdateVersion, "", m, defaultMaps(t), r.Int63(), common.Test, m.DC, 0, map[string]string{}, map[string]secrets.YavSecretInfo{}, map[string]secrets.Token{})
	id, c, err := w.Scheduler().Run(ctx)
	require.NoError(t, err)

	wg := &sync.WaitGroup{}
	wg.Add(2)

	waitProcessState(t, c)
	go func(wg *sync.WaitGroup, c chan *scheduler.State) {
		defer wg.Done()
		assertState(t, w, c, []scheduler.StateType{},
			[]scheduler.StateType{scheduler.Prepare, scheduler.Process, scheduler.Revert, scheduler.RevertSuccess}, w.Terminate())
	}(wg, c)

	ctx.NomadId = id
	c, err = w.Scheduler().State(ctx)
	require.NoError(t, err)

	go func(wg *sync.WaitGroup, c chan *scheduler.State) {
		defer wg.Done()
		assertState(t, w, c, []scheduler.StateType{},
			[]scheduler.StateType{scheduler.Prepare, scheduler.Process, scheduler.Revert, scheduler.RevertSuccess}, w.Terminate())
	}(wg, c)

	wg.Wait()

	assert.Less(t, int64(time.Since(start)), int64(w.ProgressDeadline()))
}

func testUpdateFail(t *testing.T, w Wrapper) {

	start := time.Now()
	m := defaultManifest(t, w)
	m.Config.Params["FAIL"] = "true"
	m.Config.Params["FAIL_TIME"] = "3s"

	envs, err := m.Config.GetEnvs()
	require.NoError(t, err)

	ctx := scheduler.MakeContext(test_helpers.UpdateVersion, "", m, defaultMaps(t), r.Int63(), common.Test, m.DC, 0, envs, map[string]secrets.YavSecretInfo{}, map[string]secrets.Token{})
	_, c, err := w.Scheduler().Run(ctx)
	require.NoError(t, err)

	assertState(t, w, c, []scheduler.StateType{},
		[]scheduler.StateType{scheduler.Prepare, scheduler.Process, scheduler.Revert, scheduler.RevertSuccess}, w.Terminate())

	assert.Less(t, int64(time.Since(start)), int64(w.ProgressDeadline()))
}

func testUpdateOOM(t *testing.T, w Wrapper) {

	start := time.Now()
	m := defaultManifest(t, w)
	m.Config.Params["OOM"] = "true"

	envs, err := m.Config.GetEnvs()
	require.NoError(t, err)

	ctx := scheduler.MakeContext(test_helpers.UpdateVersion, "", m, defaultMaps(t), r.Int63(), common.Test, m.DC, 0, envs, map[string]secrets.YavSecretInfo{}, map[string]secrets.Token{})
	_, c, err := w.Scheduler().Run(ctx)
	require.NoError(t, err)

	assertState(t, w, c, []scheduler.StateType{},
		[]scheduler.StateType{scheduler.Prepare, scheduler.Process, scheduler.Revert, scheduler.RevertSuccess}, w.OOM())

	assert.Less(t, int64(time.Since(start)), int64(w.ProgressDeadline()))
}

func testUnhealthyAndRevert(t *testing.T, w Wrapper) {

	w = w.NewDownProgressDeadline(t)
	m := defaultManifest(t, w)
	m.Config.Params["API"] = "no"

	envs, err := m.Config.GetEnvs()
	require.NoError(t, err)

	ctx := scheduler.MakeContext(test_helpers.UpdateVersion, "", m, defaultMaps(t), r.Int63(), common.Test, m.DC, 0, envs, map[string]secrets.YavSecretInfo{}, map[string]secrets.Token{})
	_, c, err := w.Scheduler().Run(ctx)
	require.NoError(t, err)

	last := assertState(t, w, c, []scheduler.StateType{},
		[]scheduler.StateType{scheduler.Prepare, scheduler.Process, scheduler.Revert, scheduler.RevertSuccess}, revert.RevertType_Unhealthy)
	assertFailApiProvides(t, last.ProvidesStatus)
	assertSuccessProvides(t, last.RevertProvidesStatus, "api")
}

func testUnhealthyFirstRun(t *testing.T, w Wrapper) {
	w = w.NewDownProgressDeadline(t)
	m := defaultManifest(t, w)
	m.Config.Params["API"] = "no"

	envs, err := m.Config.GetEnvs()
	require.NoError(t, err)

	ctx := scheduler.MakeContext(test_helpers.Version, "", m, defaultMaps(t), r.Int63(), common.Test, m.DC, 0, envs, map[string]secrets.YavSecretInfo{}, map[string]secrets.Token{})
	_, c, err := w.Scheduler().Run(ctx)
	require.NoError(t, err)

	last := assertState(t, w, c, []scheduler.StateType{},
		[]scheduler.StateType{scheduler.Prepare, scheduler.Process, scheduler.Revert, scheduler.Fail}, revert.RevertType_Unhealthy)
	assertFailApiProvides(t, last.ProvidesStatus)
}

func testFailFirstRun(t *testing.T, w Wrapper) {

	m := defaultManifest(t, w)
	m.Config.Params["FAIL"] = "true"
	m.Config.Params["FAIL_TIME"] = "3s"

	ctx := scheduler.MakeContext(test_helpers.Version, "", m, defaultMaps(t), r.Int63(), common.Test, m.DC, 0, map[string]string{}, map[string]secrets.YavSecretInfo{}, map[string]secrets.Token{})
	_, c, err := w.Scheduler().Run(ctx)
	require.NoError(t, err)

	assertState(t, w, c, []scheduler.StateType{}, []scheduler.StateType{scheduler.Prepare, scheduler.Process, scheduler.Revert, scheduler.Fail}, revert.RevertType_Terminate)
}

// testReschedule require having more than 3 nodes in a cluster
func testReschedule(t *testing.T, w Wrapper) {
	m := defaultManifest(t, w)
	m.Config.Params["STABLE_ON_ATTEMPT"] = "3"
	// one dc, one instance
	m.DC = make(map[string]int)
	for _, dc := range w.DefaultDC() {
		m.DC[dc] = 1
		break
	}

	conf := kv.NewConf(test.CINamespace)
	conf.ServiceName = test_helpers.ServiceName(t)
	testInfoKV := kv.NewEphemeralKV(test.NewLogger(t), conf, &test.TestInfo{})

	envs, err := m.Config.GetEnvs()
	require.NoError(t, err)

	ctx := scheduler.MakeContext(test_helpers.Version, "", m, defaultMaps(t), r.Int63(), common.Test, m.DC, 0, envs, map[string]secrets.YavSecretInfo{}, map[string]secrets.Token{})
	_, c, err := w.Scheduler().Run(ctx)
	require.NoError(t, err)

	assertState(t, w, c, []scheduler.StateType{scheduler.Revert, scheduler.Fail}, []scheduler.StateType{scheduler.Prepare, scheduler.Process, scheduler.Success}, revert.RevertType_None)

	get, err := testInfoKV.Get(test.TestInfoKey)
	require.NoError(t, err)
	testInfo, ok := get.(*test.TestInfo)
	require.True(t, ok)
	require.Len(t, testInfo.Hosts, 3)
}

func testServiceFlapAfterDeploy(t *testing.T, w Wrapper) {
	m := defaultManifest(t, w)
	m.Config.Params["FAIL"] = "true"
	m.Config.Params["FAIL_TIME"] = "15s"
	// one dc, one instance
	m.DC = make(map[string]int)
	for _, dc := range w.DefaultDC() {
		m.DC[dc] = 1
		break
	}

	envs, err := m.Config.GetEnvs()
	require.NoError(t, err)

	ctx := scheduler.MakeContext(test_helpers.Version, "", m, defaultMaps(t), r.Int63(), common.Test, m.DC, 0, envs, map[string]secrets.YavSecretInfo{}, map[string]secrets.Token{})
	_, c, err := w.Scheduler().Run(ctx)
	require.NoError(t, err)

	assertState(t, w, c, []scheduler.StateType{scheduler.Revert, scheduler.Fail}, []scheduler.StateType{scheduler.Prepare, scheduler.Process, scheduler.Success}, revert.RevertType_None)
	restartCount := config.Int("NOMAD_RESTART_POLICY_ATTEMPTS") + 1
	rescheduleCount := config.Int("NOMAD_DEPLOY_RESCHEDULE_ATTEMPTS") + 1
	time.Sleep(15 * time.Second * time.Duration(restartCount) * time.Duration(rescheduleCount+1))

	serviceName := test_helpers.ServiceName(t)
	require.Eventually(t, func() bool {
		address := fmt.Sprintf("%s-%s.vrts-slb.%s.vertis.yandex.net", serviceName, "api", "test")
		resp, err := http.Get(fmt.Sprintf("http://%s:80/ping", address))
		require.NoError(t, err)
		return resp.StatusCode == http.StatusOK
	}, 15*time.Second, time.Second, "service should be alive")
}

func testRun(_ *testing.T, _ Wrapper) {
}

func testStopNonexistentService(t *testing.T, w Wrapper) {
	m := defaultManifest(t, w)
	ctx := scheduler.MakeContext(test_helpers.Version, "", m, defaultMaps(t), r.Int63(), common.Test, m.DC, 0, map[string]string{}, map[string]secrets.YavSecretInfo{}, map[string]secrets.Token{})
	_, state, err := w.Scheduler().Stop(ctx)
	require.NoError(t, err)
	oneState := <-state
	assert.Equal(t, oneState.StateType, scheduler.Success)
}

func testRestartNonexistentService(t *testing.T, w Wrapper) {
	m := defaultManifest(t, w)
	ctx := scheduler.MakeContext(test_helpers.Version, "", m, defaultMaps(t), r.Int63(), common.Test, m.DC, 0, map[string]string{}, map[string]secrets.YavSecretInfo{}, map[string]secrets.Token{})
	_, _, err := w.Scheduler().Restart(ctx)
	test.NewLogger(t).WithError(err).Error("err")
	assert.True(t, errors.Is(err, common.ErrNotFound))
}

func testOldAddress(t *testing.T, w Wrapper) {
	serviceName := test_helpers.ServiceName(t)
	sm := defaultMaps(t)
	oldAddress := serviceName + "-old.vrts-slb.test.vertis.yandex.net"
	sm.Provides = []*proto.ServiceProvides{{
		Name:           "api",
		Protocol:       proto.ServiceProvides_http,
		Port:           80,
		OldAddressTest: oldAddress,
	}}
	m := defaultManifest(t, w)

	envs, err := m.Config.GetEnvs()
	require.NoError(t, err)

	ctx := scheduler.MakeContext(test_helpers.UpdateVersion, "", m, sm, r.Int63(), common.Test, m.DC, 0, envs, map[string]secrets.YavSecretInfo{}, map[string]secrets.Token{})
	_, c, err := w.Scheduler().Run(ctx)
	require.NoError(t, err)
	assertState(t, w, c, []scheduler.StateType{scheduler.Revert, scheduler.Fail},
		[]scheduler.StateType{scheduler.Prepare, scheduler.Process, scheduler.Success}, revert.RevertType_None)
	assertPing(t, fmt.Sprintf("%s-%s.vrts-slb.%s.vertis.yandex.net", serviceName, "api", "test"))
	assertPing(t, oldAddress)
}

func testCancelRunProcess(t *testing.T, w Wrapper) {

	wg := &sync.WaitGroup{}

	m := defaultManifest(t, w)
	ctx := scheduler.MakeContext(test_helpers.UpdateVersion, "", m, defaultMaps(t), r.Int63(), common.Test, m.DC, 0, map[string]string{}, map[string]secrets.YavSecretInfo{}, map[string]secrets.Token{})
	id, runC, err := w.Scheduler().Run(ctx)
	require.NoError(t, err)
	waitProcessState(t, runC)
	wg.Add(1)
	go func(wg *sync.WaitGroup, c chan *scheduler.State) {
		defer wg.Done()
		assertState(t, w, runC, []scheduler.StateType{scheduler.Fail}, []scheduler.StateType{scheduler.Prepare, scheduler.Process, scheduler.Revert, scheduler.RevertSuccess}, revert.RevertType_Undefined)
	}(wg, runC)

	m = defaultManifest(t, w)

	envs, err := m.Config.GetEnvs()
	require.NoError(t, err)

	cancelCtx := scheduler.MakeContext(test_helpers.Version, "", m, defaultMaps(t), r.Int63(), common.Test, m.DC, 0, envs, map[string]secrets.YavSecretInfo{}, map[string]secrets.Token{})
	cancelCtx.ParentDeploymentId = ctx.DeploymentId
	cancelCtx.NomadId = id
	cancelC := cancel(t, w, cancelCtx)
	if cancelC != nil {
		wg.Add(1)
		go func(wg *sync.WaitGroup, c chan *scheduler.State) {
			defer wg.Done()
			assertState(t, w, cancelC, []scheduler.StateType{scheduler.Fail}, []scheduler.StateType{scheduler.Prepare, scheduler.Revert, scheduler.RevertSuccess}, revert.RevertType_Undefined)
		}(wg, cancelC)
	}
	wg.Wait()
}

func testCancelFirstRun(t *testing.T, w Wrapper) {

	m := defaultManifest(t, w)

	envs, err := m.Config.GetEnvs()
	require.NoError(t, err)

	ctx := scheduler.MakeContext(test_helpers.Version, "", m, defaultMaps(t), r.Int63(), common.Test, m.DC, 0, envs, map[string]secrets.YavSecretInfo{}, map[string]secrets.Token{})
	id, runC, err := w.Scheduler().Run(ctx)
	require.NoError(t, err)

	waitProcessState(t, runC)

	m = defaultManifest(t, w)

	envs, err = m.Config.GetEnvs()
	require.NoError(t, err)

	cancelCtx := scheduler.MakeContext(test_helpers.Version, "", m, defaultMaps(t), r.Int63(), common.Test, m.DC, 0, envs, map[string]secrets.YavSecretInfo{}, map[string]secrets.Token{})
	cancelCtx.ParentDeploymentId = ctx.DeploymentId
	cancelCtx.NomadId = id

	cancelC := cancel(t, w, cancelCtx)
	assertState(t, w, cancelC, []scheduler.StateType{scheduler.Success, scheduler.Fail}, []scheduler.StateType{scheduler.Prepare, scheduler.Process, scheduler.Canceled}, revert.RevertType_None)
}

func testCancelRestartProcess(t *testing.T, w Wrapper) {

	wg := &sync.WaitGroup{}
	m := defaultManifest(t, w)
	ctx := scheduler.MakeContext(test_helpers.Version, "", m, defaultMaps(t), r.Int63(), common.Test, m.DC, 0, map[string]string{}, map[string]secrets.YavSecretInfo{}, map[string]secrets.Token{})
	id, restartC, err := w.Scheduler().Restart(ctx)
	require.NoError(t, err)

	waitProcessState(t, restartC)
	wg.Add(1)
	go func(wg *sync.WaitGroup, c chan *scheduler.State) {
		defer wg.Done()
		assertState(t, w, c, []scheduler.StateType{scheduler.Revert},
			[]scheduler.StateType{scheduler.Prepare, scheduler.Process, scheduler.Fail}, revert.RevertType_None)
	}(wg, restartC)

	m = defaultManifest(t, w)
	cancelCtx := scheduler.MakeContext(test_helpers.Version, "", m, defaultMaps(t), r.Int63(), common.Test, m.DC, 0, map[string]string{}, map[string]secrets.YavSecretInfo{}, map[string]secrets.Token{})
	cancelCtx.ParentDeploymentId = ctx.DeploymentId
	cancelCtx.NomadId = id

	cancelC := cancel(t, w, cancelCtx)
	if cancelC != nil {
		wg.Add(1)
		go func(wg *sync.WaitGroup, c chan *scheduler.State) {
			defer wg.Done()
			assertState(t, w, c, []scheduler.StateType{scheduler.Revert}, []scheduler.StateType{scheduler.Fail}, revert.RevertType_None)
		}(wg, cancelC)
	}
	wg.Wait()
}

func cancel(t *testing.T, w Wrapper, ctx *scheduler.Context) chan *scheduler.State {
	var cancelC chan *scheduler.State
	test.Wait(t, func() error {
		var err error
		_, cancelC, err = w.Scheduler().Cancel(ctx)
		if err != nil {
			if strings.Contains(err.Error(), "deployment id not found by evaluation id") {
				return err
			}
			require.NoError(t, err)
			return nil
		}
		return nil
	})
	return cancelC
}

func testCancelCompleted(t *testing.T, w Wrapper) {

	ctx, id := runDefault(t, w, false)
	m := defaultManifest(t, w)
	cancelCtx := scheduler.MakeContext(test_helpers.Version, "", m, defaultMaps(t), r.Int63(), common.Test, m.DC, 0, map[string]string{}, map[string]secrets.YavSecretInfo{}, map[string]secrets.Token{})
	cancelCtx.ParentDeploymentId = ctx.DeploymentId
	cancelCtx.NomadId = id
	_, _, err := w.Scheduler().Cancel(cancelCtx)
	assert.Equal(t, scheduler.ErrCancelCompletedDeployment, err)
}

func testUpdate(t *testing.T, w Wrapper) {

	m := defaultManifest(t, w)

	envs, err := m.Config.GetEnvs()
	require.NoError(t, err)

	ctx := scheduler.MakeContext(test_helpers.UpdateVersion, "", m, defaultMaps(t), r.Int63(), common.Test, m.DC, 0, envs, map[string]secrets.YavSecretInfo{}, map[string]secrets.Token{})
	_, c, err := w.Scheduler().Run(ctx)
	require.NoError(t, err)

	assertState(t, w, c, []scheduler.StateType{scheduler.Revert, scheduler.Fail}, []scheduler.StateType{scheduler.Prepare, scheduler.Process, scheduler.Success}, revert.RevertType_None)
}

func testUpdateOverrideCPU(t *testing.T, w Wrapper) {
	const overrideCPU = 7000

	m := defaultManifest(t, w)

	envs, err := m.Config.GetEnvs()
	require.NoError(t, err)

	dctx := scheduler.MakeContext(test_helpers.UpdateVersion, "", m, defaultMaps(t), r.Int63(), common.Test, m.DC, overrideCPU, envs, map[string]secrets.YavSecretInfo{}, map[string]secrets.Token{})
	_, ch, err := w.Scheduler().Update(dctx)
	require.NoError(t, err)

	assertState(t, w, ch, []scheduler.StateType{scheduler.Revert, scheduler.Fail}, []scheduler.StateType{scheduler.Prepare, scheduler.Process, scheduler.Success}, revert.RevertType_None)

	conf := nomad.NewConf(common.Test)
	nomadApi, err := api.NewClient(&api.Config{Address: conf.Address, Region: conf.Region})
	require.NoError(t, err)

	job, _, err := nomadApi.Jobs().Info(dctx.GetFullName(), nil)
	require.NoError(t, err)

	for _, groups := range job.TaskGroups {
		for _, task := range groups.Tasks {
			if task.Name == fmt.Sprintf("%s-%s", dctx.GetName(), "task") {
				assert.Equal(t, overrideCPU, *task.Resources.CPU)
			}
		}
	}
}

func testState(t *testing.T, w Wrapper) {

	ctx := defaultContext(t, w, false)
	ctx.Version = test_helpers.UpdateVersion
	id, c, err := w.Scheduler().Run(ctx)
	require.NoError(t, err)

	var wg sync.WaitGroup
	wg.Add(2)

	waitProcessState(t, c)
	go func() {
		defer wg.Done()
		assertState(t, w, c, []scheduler.StateType{scheduler.Revert, scheduler.Fail}, []scheduler.StateType{scheduler.Prepare, scheduler.Process, scheduler.Success}, revert.RevertType_None)
	}()

	stateCtx := defaultContext(t, w, false)
	stateCtx.DeploymentId = ctx.DeploymentId
	stateCtx.NomadId = id
	stateC, err := w.Scheduler().State(stateCtx)
	require.NoError(t, err)
	go func() {
		defer wg.Done()
		assertState(t, w, stateC, []scheduler.StateType{scheduler.Revert, scheduler.Fail}, []scheduler.StateType{scheduler.Prepare, scheduler.Process, scheduler.Success}, revert.RevertType_None)
	}()
	wg.Wait()
}

func testStateByRestart(t *testing.T, w Wrapper) {

	m := defaultManifest(t, w)
	ctx := scheduler.MakeContext(test_helpers.Version, "", m, defaultMaps(t), r.Int63(), common.Test, m.DC, 0, map[string]string{}, map[string]secrets.YavSecretInfo{}, map[string]secrets.Token{})
	id, c, err := w.Scheduler().Restart(ctx)
	require.NoError(t, err)

	waitProcessState(t, c)
	var wg sync.WaitGroup
	wg.Add(2)
	go func() {
		defer wg.Done()
		assertState(t, w, c, []scheduler.StateType{scheduler.Revert, scheduler.Fail}, []scheduler.StateType{scheduler.Prepare, scheduler.Process, scheduler.Success}, revert.RevertType_None)
	}()

	m = defaultManifest(t, w)
	stateCtx := scheduler.MakeContext(test_helpers.UpdateVersion, "", m, defaultMaps(t), r.Int63(), common.Test, m.DC, 0, map[string]string{}, map[string]secrets.YavSecretInfo{}, map[string]secrets.Token{})
	stateCtx.NomadId = id
	stateCtx.DeploymentId = ctx.DeploymentId
	c2, err := w.Scheduler().State(stateCtx)
	require.NoError(t, err)
	go func() {
		defer wg.Done()
		assertState(t, w, c2, []scheduler.StateType{scheduler.Revert, scheduler.Fail}, []scheduler.StateType{scheduler.Prepare, scheduler.Process, scheduler.Success}, revert.RevertType_None)
	}()
	wg.Wait()
}

func testEndState(t *testing.T, w Wrapper) {

	ctx, id := runDefault(t, w, false)
	stateCtx := defaultContext(t, w, false)
	stateCtx.NomadId = id
	stateCtx.DeploymentId = ctx.DeploymentId
	c, err := w.Scheduler().State(stateCtx)
	require.NoError(t, err)
	state := <-c
	assert.Equal(t, scheduler.Success, state.StateType)
	_, ok := <-c
	assert.False(t, ok)
}

func testTCPHealthCheck(t *testing.T, w Wrapper) {

	m := defaultManifest(t, w)
	m.Config.Params["API"] = "tcp"
	maps := defaultMaps(t)
	maps.Provides = []*proto.ServiceProvides{
		{
			Name:        "tcp_api",
			Protocol:    proto.ServiceProvides_tcp,
			Port:        80,
			Description: "Test tcp api",
		},
	}

	envs, err := m.Config.GetEnvs()
	require.NoError(t, err)

	ctx := scheduler.MakeContext(test_helpers.Version, "", m, maps, r.Int63(), common.Test, m.DC, 0, envs, map[string]secrets.YavSecretInfo{}, map[string]secrets.Token{})
	_, c, err := w.Scheduler().Run(ctx)
	require.NoError(t, err)
	last := assertState(t, w, c, []scheduler.StateType{scheduler.Revert, scheduler.Fail}, []scheduler.StateType{scheduler.Prepare, scheduler.Process, scheduler.Success}, revert.RevertType_None)
	assertSuccessProvides(t, last.ProvidesStatus, "tcp_api")
}

func testHttpHealthCheck(t *testing.T, w Wrapper) {

	m := defaultManifest(t, w)
	m.Config.Params["API"] = "http"
	maps := defaultMaps(t)
	maps.Provides = []*proto.ServiceProvides{
		{
			Name:        "http_api",
			Protocol:    proto.ServiceProvides_http,
			Port:        80,
			Description: "Test http api",
		},
	}

	envs, err := m.Config.GetEnvs()
	require.NoError(t, err)

	ctx := scheduler.MakeContext(test_helpers.UpdateVersion, "", m, maps, r.Int63(), common.Test, m.DC, 0, envs, map[string]secrets.YavSecretInfo{}, map[string]secrets.Token{})
	_, c, err := w.Scheduler().Run(ctx)
	require.NoError(t, err)
	last := assertState(t, w, c, []scheduler.StateType{scheduler.Revert, scheduler.Fail}, []scheduler.StateType{scheduler.Prepare, scheduler.Process, scheduler.Success}, revert.RevertType_None)
	assertSuccessProvides(t, last.ProvidesStatus, "http_api")
}

func testWithoutHealthCheck(t *testing.T, w Wrapper) {

	m := defaultManifest(t, w)
	m.Config.Params["API"] = "no"
	maps := defaultMaps(t)
	maps.Provides = []*proto.ServiceProvides{}

	envs, err := m.Config.GetEnvs()
	require.NoError(t, err)

	ctx := scheduler.MakeContext(test_helpers.Version, "", m, maps, r.Int63(), common.Test, m.DC, 0, envs, map[string]secrets.YavSecretInfo{}, map[string]secrets.Token{})
	_, c, err := w.Scheduler().Run(ctx)
	require.NoError(t, err)
	last := assertState(t, w, c, []scheduler.StateType{scheduler.Revert, scheduler.Fail}, []scheduler.StateType{scheduler.Prepare, scheduler.Process, scheduler.Success}, revert.RevertType_None)
	assert.Len(t, last.ProvidesStatus, 1)
}

func testGrpcHealthCheck(t *testing.T, w Wrapper) {

	m := defaultManifest(t, w)
	m.Config.Params["API"] = "grpc"
	maps := defaultMaps(t)
	maps.Provides = []*proto.ServiceProvides{
		{
			Name:        "grpc_api",
			Protocol:    proto.ServiceProvides_grpc,
			Port:        80,
			Description: "Test grpc api",
		},
	}

	envs, err := m.Config.GetEnvs()
	require.NoError(t, err)

	ctx := scheduler.MakeContext(test_helpers.Version, "", m, maps, r.Int63(), common.Test, m.DC, 0, envs, map[string]secrets.YavSecretInfo{}, map[string]secrets.Token{})
	_, c, err := w.Scheduler().Run(ctx)
	require.NoError(t, err)
	last := assertState(t, w, c, []scheduler.StateType{scheduler.Revert, scheduler.Fail}, []scheduler.StateType{scheduler.Prepare, scheduler.Process, scheduler.Success}, revert.RevertType_None)
	assertSuccessProvides(t, last.ProvidesStatus, "grpc_api")
}

func testRestart(t *testing.T, w Wrapper) {

	m := defaultManifest(t, w)

	envs, err := m.Config.GetEnvs()
	require.NoError(t, err)

	ctx := scheduler.MakeContext(test_helpers.Version, "", m, defaultMaps(t), r.Int63(), common.Test, m.DC, 0, envs, map[string]secrets.YavSecretInfo{}, map[string]secrets.Token{})
	_, c, err := w.Scheduler().Restart(ctx)
	require.NoError(t, err)
	assertState(t, w, c, []scheduler.StateType{scheduler.Revert, scheduler.Fail}, []scheduler.StateType{scheduler.Prepare, scheduler.Process, scheduler.Success}, revert.RevertType_None)
}

func testResizeWithoutChange(t *testing.T, w Wrapper) {
	defaultDC := w.DefaultDC()
	assert.Len(t, defaultDC, 2)
	DCs := map[string]int{
		defaultDC[0]: 3,
		defaultDC[1]: 3,
	}

	m := defaultManifest(t, w)

	envs, err := m.Config.GetEnvs()
	require.NoError(t, err)

	sMap := defaultMaps(t)
	ctx := scheduler.MakeContext(test_helpers.Version, "", m, sMap, r.Int63(), common.Test, DCs, 0, envs, map[string]secrets.YavSecretInfo{}, map[string]secrets.Token{})
	_, c, err := w.Scheduler().Update(ctx)
	require.NoError(t, err)
	state := assertState(t, w, c, []scheduler.StateType{scheduler.Revert, scheduler.Fail}, []scheduler.StateType{scheduler.Prepare, scheduler.Process, scheduler.Success}, revert.RevertType_None)
	assert.Len(t, state.Number, 2)
	for _, n := range state.Number {
		want, ok := DCs[n.DC]
		assert.True(t, ok)
		assert.Equal(t, want, n.SuccessPlaced)
	}
}

func testResizeRunResize(t *testing.T, w Wrapper) {

	defaultDC := w.DefaultDC()
	assert.Len(t, defaultDC, 2)
	DCs := map[string]int{
		defaultDC[1]: 3,
		defaultDC[0]: 0,
	}

	m := defaultManifest(t, w)
	sMap := defaultMaps(t)

	envs, err := m.Config.GetEnvs()
	require.NoError(t, err)

	ctx := scheduler.MakeContext(test_helpers.Version, "", m, sMap, r.Int63(), common.Test, DCs, 0, envs, map[string]secrets.YavSecretInfo{}, map[string]secrets.Token{})
	_, c, err := w.Scheduler().Update(ctx)
	require.NoError(t, err)
	state := assertState(t, w, c, []scheduler.StateType{scheduler.Revert, scheduler.Fail}, []scheduler.StateType{scheduler.Prepare, scheduler.Process, scheduler.Success}, revert.RevertType_None)
	assert.Len(t, state.Number, 1)
	for _, n := range state.Number {
		want, ok := DCs[n.DC]
		assert.True(t, ok)
		assert.Equal(t, want, n.SuccessPlaced)
	}

	m = defaultManifest(t, w)
	sMap = defaultMaps(t)

	envs, err = m.Config.GetEnvs()
	require.NoError(t, err)

	ctx = scheduler.MakeContext(test_helpers.UpdateVersion, "", m, sMap, r.Int63(), common.Test, DCs, 0, envs, map[string]secrets.YavSecretInfo{}, map[string]secrets.Token{})
	_, c, err = w.Scheduler().Run(ctx)
	require.NoError(t, err)
	state = assertState(t, w, c, []scheduler.StateType{scheduler.Revert, scheduler.Fail}, []scheduler.StateType{scheduler.Prepare, scheduler.Process, scheduler.Success}, revert.RevertType_None)
	assert.Len(t, state.Number, 1)
	for _, n := range state.Number {
		want, ok := DCs[n.DC]
		assert.True(t, ok)
		assert.Equal(t, want, n.SuccessPlaced)
	}

	DCs = map[string]int{
		defaultDC[0]: 3,
		defaultDC[1]: 3,
	}
	ctx = scheduler.MakeContext(test_helpers.UpdateVersion, "", m, sMap, r.Int63(), common.Test, DCs, 0, map[string]string{}, map[string]secrets.YavSecretInfo{}, map[string]secrets.Token{})
	_, c, err = w.Scheduler().Update(ctx)
	require.NoError(t, err)
	state = assertState(t, w, c, []scheduler.StateType{scheduler.Revert, scheduler.Fail}, []scheduler.StateType{scheduler.Prepare, scheduler.Process, scheduler.Success}, revert.RevertType_None)
	assert.Len(t, state.Number, 2)
	for _, n := range state.Number {
		want, ok := DCs[n.DC]
		assert.True(t, ok)
		assert.Equal(t, want, n.SuccessPlaced)
	}
}

func testRunBranchWithOffDC(t *testing.T, w Wrapper) {

	// down
	defaultDC := w.DefaultDC()
	assert.Len(t, defaultDC, 2)
	DCs := map[string]int{
		defaultDC[1]: 3,
		defaultDC[0]: 0,
	}

	m := defaultManifest(t, w)
	sMap := defaultMaps(t)

	envs, err := m.Config.GetEnvs()
	require.NoError(t, err)

	ctx := scheduler.MakeContext(test_helpers.UpdateVersion, "br", m, sMap, r.Int63(), common.Test, DCs, 0, envs, map[string]secrets.YavSecretInfo{}, map[string]secrets.Token{})
	_, c, err := w.Scheduler().Run(ctx)
	require.NoError(t, err)
	state := assertState(t, w, c, []scheduler.StateType{scheduler.Revert, scheduler.Fail}, []scheduler.StateType{scheduler.Prepare, scheduler.Process, scheduler.Success}, revert.RevertType_None)
	assert.Len(t, state.Number, 1)
	for _, n := range state.Number {
		_, ok := DCs[n.DC]
		assert.True(t, ok)
		assert.Equal(t, 1, n.SuccessPlaced)
	}
}

func testResizeOffAndReturn(t *testing.T, w Wrapper) {

	// down
	defaultDC := w.DefaultDC()
	assert.Len(t, defaultDC, 2)
	DCs := map[string]int{
		defaultDC[1]: 3,
		defaultDC[0]: 0,
	}

	m := defaultManifest(t, w)
	sMap := defaultMaps(t)
	ctx := scheduler.MakeContext(test_helpers.Version, "", m, sMap, r.Int63(), common.Test, DCs, 0, map[string]string{}, map[string]secrets.YavSecretInfo{}, map[string]secrets.Token{})
	_, c, err := w.Scheduler().Update(ctx)
	require.NoError(t, err)
	state := assertState(t, w, c, []scheduler.StateType{scheduler.Revert, scheduler.Fail}, []scheduler.StateType{scheduler.Prepare, scheduler.Process, scheduler.Success}, revert.RevertType_None)
	assert.Len(t, state.Number, 1)
	for _, n := range state.Number {
		want, ok := DCs[n.DC]
		assert.True(t, ok)
		assert.Equal(t, want, n.SuccessPlaced)
	}

	// up (return size)
	DCs = map[string]int{
		defaultDC[0]: 3,
		defaultDC[1]: 3,
	}
	ctx = scheduler.MakeContext(test_helpers.Version, "", m, sMap, r.Int63(), common.Test, DCs, 0, map[string]string{}, map[string]secrets.YavSecretInfo{}, map[string]secrets.Token{})
	_, c, err = w.Scheduler().Update(ctx)
	require.NoError(t, err)
	state = assertState(t, w, c, []scheduler.StateType{scheduler.Revert, scheduler.Fail}, []scheduler.StateType{scheduler.Prepare, scheduler.Process, scheduler.Success}, revert.RevertType_None)
	assert.Len(t, state.Number, 2)
	for _, n := range state.Number {
		want, ok := DCs[n.DC]
		assert.True(t, ok)
		assert.Equal(t, want, n.SuccessPlaced)
	}
}

func testResizeBranchAndReturn(t *testing.T, w Wrapper) {

	// down
	defaultDC := w.DefaultDC()
	assert.Len(t, defaultDC, 2)
	DCs := map[string]int{
		defaultDC[1]: 3,
		defaultDC[0]: 0,
	}

	m := defaultManifest(t, w)
	sMap := defaultMaps(t)
	ctx := scheduler.MakeContext(test_helpers.Version, branch, m, sMap, r.Int63(), common.Test, DCs, 0, map[string]string{}, map[string]secrets.YavSecretInfo{}, map[string]secrets.Token{})
	_, c, err := w.Scheduler().Update(ctx)
	require.NoError(t, err)
	state := assertState(t, w, c, []scheduler.StateType{scheduler.Revert, scheduler.Fail}, []scheduler.StateType{scheduler.Prepare, scheduler.Process, scheduler.Success}, revert.RevertType_None)
	assert.Len(t, state.Number, 1)
	for _, n := range state.Number {
		_, ok := DCs[n.DC]
		assert.True(t, ok)
		assert.Equal(t, 1, n.SuccessPlaced)
	}

	// up (return size)
	DCs = map[string]int{
		defaultDC[0]: 3,
		defaultDC[1]: 3,
	}
	ctx = scheduler.MakeContext(test_helpers.Version, branch, m, sMap, r.Int63(), common.Test, DCs, 0, map[string]string{}, map[string]secrets.YavSecretInfo{}, map[string]secrets.Token{})
	_, c, err = w.Scheduler().Update(ctx)
	require.NoError(t, err)
	state = assertState(t, w, c, []scheduler.StateType{scheduler.Revert, scheduler.Fail}, []scheduler.StateType{scheduler.Prepare, scheduler.Process, scheduler.Success}, revert.RevertType_None)
	assert.Len(t, state.Number, 2)
	for _, n := range state.Number {
		_, ok := DCs[n.DC]
		assert.True(t, ok)
		assert.Equal(t, 1, n.SuccessPlaced)
	}
}

func testServiceWithSecrets(t *testing.T, w Wrapper) {

	name := "shiva-ci"
	branch := "secret" + salt

	svcMap := defaultMaps(t)
	svcManifest := defaultManifest(t, w)
	svcMap.Name = name
	svcMap.Provides = []*proto.ServiceProvides{{
		Name:     "rpc",
		Protocol: proto.ServiceProvides_grpc,
		Port:     80,
	}}
	svcManifest.Name = "shiva-ci"
	svcManifest.Config.Params["API"] = "grpc"
	svcManifest.Config.Params["SERVICE_NAME"] = name
	// link to UI: https://yav.yandex-team.ru/secret/sec-01e2dwnfawqmxcsapn2g6tk3y1
	svcManifest.Config.Params["SECRET_VAR"] = "${sec-01e2dwnfawqmxcsapn2g6tk3y1:ver-01e2dwnfbbn20j2w9y43225p71:the_key}"

	envs, err := svcManifest.Config.GetEnvs()
	require.NoError(t, err)

	ctx := scheduler.MakeContext(test_helpers.Version, branch, svcManifest, svcMap, r.Int63(), common.Test, svcManifest.DC, 0, envs, map[string]secrets.YavSecretInfo{
		"SECRET_VAR": {
			SecretId:  "sec-01e2dwnfawqmxcsapn2g6tk3y1",
			VersionId: "ver-01e2dwnfbbn20j2w9y43225p71",
			SecretKey: "the_key",
		},
	}, map[string]secrets.Token{})
	defer w.Scheduler().Stop(ctx)
	_, c, err := w.Scheduler().Run(ctx)
	require.NoError(t, err)
	assertState(t, w, c, []scheduler.StateType{scheduler.Revert, scheduler.Fail}, []scheduler.StateType{scheduler.Prepare, scheduler.Process, scheduler.Success}, revert.RevertType_None)

	// check service response
	svcName := fmt.Sprintf("%s-%s", svcManifest.Name, branch)
	addr := fmt.Sprintf("%s-%s.vrts-slb.test.vertis.yandex.net:80", svcName, "rpc")
	assertEnv(t, addr, "SECRET_VAR", "the_value")
}

func testServiceWithTemplates(t *testing.T, w Wrapper) {
	const (
		name     = "shiva-ci"
		branch   = "secret-"
		provider = "rpc"
		tvmID    = "10"
	)

	defer test.Down(t)

	db := test_db.NewDb(t)
	log := test.NewLogger(t)
	mapSvc := service_map.NewService(db, log, service_change.NewNotificationMock())
	tvmCli := tvmauth.NewClient(tvmauth.WithIssueTicket(config.Int("SHIVA_TVM_ID"), []int{config.Int("SS_TVM_ID")}, config.Str("TVM_SECRET")))
	require.Eventually(t, func() bool {
		_, err := tvmCli.ServiceTicket(config.Int("SHIVA_TVM_ID"), config.Int("SS_TVM_ID"))
		return err == nil
	}, time.Second*3, time.Second/10, "tvm init failed")

	ssConf := secrets.NewConf(config.Int("SHIVA_TVM_ID"))
	ss := secrets.NewService(ssConf, secrets.NewAccessClient(ssConf, tvmCli, log), log)
	envResolver := manifest.NewService(template.NewService(mapSvc, reader.NewService(db, log)), ss)

	require.NoError(t, mapSvc.ReadAndSave([]byte(shivaMap), 10, proto.ToFullPath("shiva")))

	envStorage := storage.NewStorage(db, log)
	require.NoError(t, envStorage.Save(&storage.ExternalEnv{
		Service: "shiva",
		Layer:   common.Test,
		Type:    env.EnvType_GENERATED_TVM_ID,
		Key:     "tvm-id",
		Value:   tvmID,
	}))

	svcMap := defaultMaps(t)
	svcMap.Name = name
	svcMap.Provides = []*proto.ServiceProvides{{
		Name:     provider,
		Protocol: proto.ServiceProvides_grpc,
		Port:     80,
	}}

	svcManifest := defaultManifest(t, w)
	svcManifest.Name = name
	svcManifest.Config.Params["API"] = "grpc"
	svcManifest.Config.Params["SERVICE_NAME"] = name
	svcManifest.Config.Params["SHIVA_PORT"] = "${port:shiva:api}"
	svcManifest.Config.Params["SHIVA_HOST"] = "${host:shiva:api}"
	svcManifest.Config.Params["SHIVA_URL"] = "${url:shiva:api}"
	svcManifest.Config.Params["SHIVA_TVM"] = "${tvm-id:shiva}"

	envs, err := envResolver.ResolveEnvs(svcManifest, common.Test)
	require.NoError(t, err)

	ctx := scheduler.MakeContext(test_helpers.Version, branch, svcManifest, svcMap, r.Int63(), common.Test, svcManifest.DC, 0, envs.Envs, map[string]secrets.YavSecretInfo{}, map[string]secrets.Token{})
	defer w.Scheduler().Stop(ctx)

	_, c, err := w.Scheduler().Run(ctx)
	require.NoError(t, err)

	assertState(t, w, c, []scheduler.StateType{scheduler.Revert, scheduler.Fail}, []scheduler.StateType{scheduler.Prepare, scheduler.Process, scheduler.Success}, revert.RevertType_None)

	addr := fmt.Sprintf("%s-%s-%s.vrts-slb.test.vertis.yandex.net:80", name, branch, provider)
	assertEnv(t, addr, "SHIVA_PORT", "80")
	assertEnv(t, addr, "SHIVA_HOST", "shiva-api.vrts-slb.test.vertis.yandex.net")
	assertEnv(t, addr, "SHIVA_URL", "http://shiva-api.vrts-slb.test.vertis.yandex.net:80")
	assertEnv(t, addr, "SHIVA_TVM", tvmID)
}

func testEnvs(t *testing.T, w Wrapper) {
	svcMap := defaultMaps(t)
	name := test_helpers.ServiceName(t)
	svcMap.Name = name
	svcMap.Provides = []*proto.ServiceProvides{{Name: "rpc", Protocol: proto.ServiceProvides_grpc, Port: 80}}
	svcManifest := defaultManifest(t, w)
	svcManifest.Name = name
	svcManifest.Config.Params["API"] = "grpc"
	svcManifest.Config.Params["SERVICE_NAME"] = svcManifest.Name

	envs, err := svcManifest.Config.GetEnvs()
	require.NoError(t, err)

	ctx := scheduler.MakeContext(test_helpers.Version, branch, svcManifest, svcMap, r.Int63(), common.Test, svcManifest.DC, 0, envs, map[string]secrets.YavSecretInfo{}, map[string]secrets.Token{})
	_, c, err := w.Scheduler().Run(ctx)
	require.NoError(t, err)
	assertState(t, w, c, []scheduler.StateType{scheduler.Revert, scheduler.Fail}, []scheduler.StateType{scheduler.Prepare, scheduler.Process, scheduler.Success}, revert.RevertType_None)

	// check service response
	svcName := fmt.Sprintf("%s-%s", svcManifest.Name, branch)
	addr := fmt.Sprintf("%s-%s.vrts-slb.test.vertis.yandex.net:80", svcName, "rpc")

	assertEnv(t, addr, "_DEPLOY_SERVICE_NAME", svcManifest.Name)
	assertEnv(t, addr, "_DEPLOY_APP_VERSION", test_helpers.Version)
	assertEnv(t, addr, "_DEPLOY_BRANCH", branch)
}

func testMaxParallel(t *testing.T, w Wrapper) {

	nomadW, ok := w.(*NomadWrapper)
	require.True(t, ok)
	m := defaultManifest(t, w)
	for _, num := range m.DC {
		if m.Upgrade.Parallel < num {
			m.Upgrade.Parallel = num
		}
	}
	sm := defaultMaps(t)

	envs, err := m.Config.GetEnvs()
	require.NoError(t, err)

	ctx := scheduler.MakeContext(test_helpers.Version, "", m, sm, r.Int63(), common.Test, m.DC, 0, envs, map[string]secrets.YavSecretInfo{}, map[string]secrets.Token{})
	_, c, err := nomadW.Scheduler().Run(ctx)
	require.NoError(t, err)

	assertState(t, w, c, []scheduler.StateType{scheduler.Revert, scheduler.Fail}, []scheduler.StateType{scheduler.Prepare, scheduler.Process, scheduler.Success}, revert.RevertType_None)

	ctx = scheduler.MakeContext(test_helpers.UpdateVersion, "", m, sm, r.Int63(), common.Test, m.DC, 0, envs, map[string]secrets.YavSecretInfo{}, map[string]secrets.Token{})
	_, c, err = w.Scheduler().Run(ctx)
	require.NoError(t, err)

	state := waitProcessState(t, c)
	for _, number := range state.Number {
		assert.Equal(t, number.Placed, number.Total)
	}
	assertState(t, w, c, []scheduler.StateType{scheduler.Revert, scheduler.Fail}, []scheduler.StateType{scheduler.Process, scheduler.Success}, revert.RevertType_None)
}

func testRestartFailed(t *testing.T, w Wrapper) {

	conf := kv.NewConf(test.CINamespace)
	conf.TTL = "600s"
	conf.ServiceName = test_helpers.ServiceName(t)
	testInfoKV := kv.NewEphemeralKV(test.NewLogger(t), conf, &test.TestInfo{})
	require.NoError(t, testInfoKV.Save(test.TestInfoKey, &test.TestInfo{OOM: true}))
	m := defaultManifest(t, w)
	ctx := scheduler.MakeContext(test_helpers.Version, "", m, defaultMaps(t), r.Int63(), common.Test, m.DC, 0, map[string]string{}, map[string]secrets.YavSecretInfo{}, map[string]secrets.Token{})
	_, restartCh, err := w.Scheduler().Restart(ctx)
	require.NoError(t, err)
	assertState(t, w, restartCh, []scheduler.StateType{scheduler.Revert}, []scheduler.StateType{scheduler.Prepare, scheduler.Process, scheduler.Fail}, revert.RevertType_None)
}

func testRunAndStopBranch(_ *testing.T, _ Wrapper) {}

/*
	В рамках тестам проверяем, что во время деплоя canary не встает под нагрузку
*/
func testRunCanary(t *testing.T, w Wrapper) {

	// Запускаем сервис, но уменьшим количество инстансов, чтобы увеличить вероятность попадания в canary
	m := defaultManifest(t, w)
	svcMap := defaultMaps(t)
	updateDC := map[string]int{}
	for k := range m.DC {
		updateDC[k] = 1
	}
	m.DC = updateDC
	name := test_helpers.ServiceName(t)
	svcMap.Name = name
	svcMap.Provides = []*proto.ServiceProvides{{Name: "rpc", Protocol: proto.ServiceProvides_grpc, Port: 80}}
	svcManifest := defaultManifest(t, w)
	svcManifest.Name = name
	svcManifest.Config.Params["API"] = "grpc"
	svcManifest.Config.Params["SERVICE_NAME"] = svcManifest.Name

	envs, err := svcManifest.Config.GetEnvs()
	require.NoError(t, err)

	ctx := scheduler.MakeContext(test_helpers.Version, "", svcManifest, svcMap, r.Int63(), common.Test, svcManifest.DC, 0, envs, map[string]secrets.YavSecretInfo{}, map[string]secrets.Token{})
	_, c, err := w.Scheduler().Run(ctx)
	require.NoError(t, err)
	assertState(t, w, c, []scheduler.StateType{scheduler.Revert, scheduler.Fail}, []scheduler.StateType{scheduler.Prepare, scheduler.Process, scheduler.Success}, revert.RevertType_None)

	ctx = scheduler.MakeContext(test_helpers.Version, "canary", svcManifest, svcMap, r.Int63(), common.Test, svcManifest.DC, 0, envs, map[string]secrets.YavSecretInfo{}, map[string]secrets.Token{})
	defer w.Scheduler().Stop(ctx)
	_, c, err = w.Scheduler().Run(ctx)
	require.NoError(t, err)
	waitProcessState(t, c)
	resultC := make(chan *scheduler.State, 1)
	go func() {
		result := assertState(t, w, c, []scheduler.StateType{scheduler.Revert, scheduler.Fail}, []scheduler.StateType{scheduler.Prepare, scheduler.Process, scheduler.Success}, revert.RevertType_None)
		resultC <- result
	}()

	checks := 0
	addr := fmt.Sprintf("%s-%s.vrts-slb.test.vertis.yandex.net:80", name, "rpc")
	for {
		response := makeGRPCRequest(t, addr, "_DEPLOY_BRANCH")
		time.Sleep(1 / 2 * time.Second)
		select {
		case <-resultC:
			assert.True(t, checks >= 10)
			return
		default:
			checks++
			require.Equal(t, fmt.Sprintf("key='%s' value='%s'", "_DEPLOY_BRANCH", ""), response)
		}
	}
}

func testRestartBranch(t *testing.T, w Wrapper) {
	m := defaultManifest(t, w)
	ctx := scheduler.MakeContext(test_helpers.Version, branch, m, defaultMaps(t), r.Int63(), common.Test, m.DC, 0, map[string]string{}, map[string]secrets.YavSecretInfo{}, map[string]secrets.Token{})
	_, c, err := w.Scheduler().Restart(ctx)
	require.NoError(t, err)
	assertState(t, w, c, []scheduler.StateType{scheduler.Revert, scheduler.Fail}, []scheduler.StateType{scheduler.Prepare, scheduler.Process, scheduler.Success}, revert.RevertType_None)
}

func testShutdownDelay(t *testing.T, w Wrapper) {

	wg := &sync.WaitGroup{}

	w = w.NewShutdownDelay(t, 5*time.Second)
	m := defaultManifest(t, w)
	// one dc, one instance
	m.DC = make(map[string]int)
	for _, dc := range w.DefaultDC() {
		m.DC[dc] = 1
		break
	}

	envs, err := m.Config.GetEnvs()
	require.NoError(t, err)

	ctx := scheduler.MakeContext(test_helpers.Version, "", m, defaultMaps(t), r.Int63(), common.Test, m.DC, 0, envs, map[string]secrets.YavSecretInfo{}, map[string]secrets.Token{})

	_, c, err := w.Scheduler().Run(ctx)
	require.NoError(t, err)
	assertState(t, w, c, []scheduler.StateType{scheduler.Revert, scheduler.Fail}, []scheduler.StateType{scheduler.Prepare, scheduler.Process, scheduler.Success}, revert.RevertType_None)

	serviceName := test_helpers.ServiceName(t)
	address := fmt.Sprintf("%s-%s.vrts-slb.%s.vertis.yandex.net", serviceName, "api", "test")

	wg.Add(1)
	go func(wg *sync.WaitGroup) {
		defer wg.Done()
		resp, err := http.Get(fmt.Sprintf("http://%s:80/keep5", address))
		require.NoError(t, err)
		require.Equal(t, resp.StatusCode, http.StatusOK)
	}(wg)

	wg.Add(1)
	go func(wg *sync.WaitGroup) {
		defer wg.Done()
		resp, err := http.Get(fmt.Sprintf("http://%s:80/keep10", address))
		require.NoError(t, err)
		require.Equal(t, resp.StatusCode, http.StatusServiceUnavailable)
	}(wg)

	ctx = scheduler.MakeContext(test_helpers.UpdateVersion, "", m, defaultMaps(t), r.Int63(), common.Test, m.DC, 0, envs, map[string]secrets.YavSecretInfo{}, map[string]secrets.Token{})
	_, c, err = w.Scheduler().Run(ctx)
	require.NoError(t, err)
	assertState(t, w, c, []scheduler.StateType{scheduler.Revert, scheduler.Fail}, []scheduler.StateType{scheduler.Prepare, scheduler.Process, scheduler.Success}, revert.RevertType_None)

	wg.Wait()
}

func testRunWithAnyDC(t *testing.T, w Wrapper) {
	m := defaultManifest(t, w)
	m.DC = map[string]int{
		"any": 1,
	}
	maps := defaultMaps(t)

	envs, err := m.Config.GetEnvs()
	require.NoError(t, err)

	ctx := scheduler.MakeContext(test_helpers.Version, "", m, maps, r.Int63(), common.Test, m.DC, 0, envs, map[string]secrets.YavSecretInfo{}, map[string]secrets.Token{})
	_, c, err := w.Scheduler().Run(ctx)

	require.NoError(t, err)
	assertState(t, w, c, []scheduler.StateType{scheduler.Revert, scheduler.Fail}, []scheduler.StateType{scheduler.Prepare, scheduler.Process, scheduler.Success}, revert.RevertType_None)
}

func testFailedAllocationsCount(t *testing.T, w Wrapper) {
	m := defaultManifest(t, w)
	for _, dc := range w.DefaultDC() {
		m.DC[dc] = 1
	}
	maps := defaultMaps(t)
	m.Config.Params["NUM_STABLE_INSTANCES"] = "1"

	envs, err := m.Config.GetEnvs()
	require.NoError(t, err)

	ctx := scheduler.MakeContext(test_helpers.UpdateVersion, "", m, maps, r.Int63(), common.Test, m.DC, 0, envs, map[string]secrets.YavSecretInfo{}, map[string]secrets.Token{})
	_, stateC, err := w.Scheduler().Run(ctx)

	require.NoError(t, err)

	lastState := assertState(t, w, stateC, []scheduler.StateType{}, []scheduler.StateType{scheduler.Prepare, scheduler.Process, scheduler.Revert, scheduler.RevertSuccess}, revert.RevertType_OOM)

	// 3 allocations (first + two rescheduled)
	require.Equal(t, 3, len(lastState.FailedAllocations))
}
