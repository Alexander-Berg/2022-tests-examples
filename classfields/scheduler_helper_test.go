//go:build !unit || parallel

package scheduler_test

import (
	"context"
	"fmt"
	"math/rand"
	"net/http"
	"regexp"
	"strconv"
	"sync/atomic"
	"testing"
	"time"

	"github.com/YandexClassifieds/go-common/tvm/tvmauth"
	"github.com/YandexClassifieds/shiva/cmd/shiva/scheduler"
	"github.com/YandexClassifieds/shiva/cmd/shiva/scheduler/consul"
	"github.com/YandexClassifieds/shiva/cmd/shiva/scheduler/nomad"
	"github.com/YandexClassifieds/shiva/cmd/shiva/scheduler/test_helpers"
	"github.com/YandexClassifieds/shiva/cmd/shiva/scheduler/yadeploy"
	ydClient "github.com/YandexClassifieds/shiva/cmd/shiva/scheduler/yadeploy/client"
	"github.com/YandexClassifieds/shiva/common"
	"github.com/YandexClassifieds/shiva/common/config"
	proto "github.com/YandexClassifieds/shiva/pb/shiva/service_map"
	"github.com/YandexClassifieds/shiva/pb/shiva/types/revert"
	"github.com/YandexClassifieds/shiva/pkg/feature_flags"
	manifest "github.com/YandexClassifieds/shiva/pkg/manifest/model"
	"github.com/YandexClassifieds/shiva/pkg/registry"
	"github.com/YandexClassifieds/shiva/pkg/secrets"
	"github.com/YandexClassifieds/shiva/test"
	"github.com/YandexClassifieds/shiva/test/test_db"
	"github.com/hashicorp/nomad/api"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
	"google.golang.org/grpc"
	"google.golang.org/grpc/examples/features/proto/echo"
)

var (
	r      *rand.Rand
	salt   = "-"
	count  *int64
	rState = map[scheduler.StateType]bool{
		scheduler.Revert:        true,
		scheduler.RevertSuccess: true,
		scheduler.Fail:          true,
		scheduler.Cancel:        true,
		scheduler.Canceled:      true,
	}
)

func init() {
	r = rand.New(rand.NewSource(time.Now().UnixNano()))
	salt += strconv.Itoa(time.Now().Minute()) + "-" + strconv.Itoa(r.Intn(100))
	zero := int64(0)
	count = &zero

}

type NomadWrapper struct {
	nomad            *nomad.Service
	client           *api.Client
	progressDeadline time.Duration
}

func NewNomadWrapper() Wrapper {

	return &NomadWrapper{}
}

func (w *NomadWrapper) New(t *testing.T) Wrapper {
	return w.new(t, nomad.NewConf(common.Test))
}

// new object for reconfigure
func (w *NomadWrapper) new(t *testing.T, conf nomad.Conf) Wrapper {
	// TODO use conf by env
	tvmCli := tvmauth.NewClient(
		tvmauth.WithIssueTicket(config.Int("SHIVA_TVM_ID"), []int{config.Int("SS_TVM_ID")}, config.Str("TVM_SECRET")),
	)
	require.Eventually(t, func() bool {
		_, err := tvmCli.ServiceTicket(config.Int("SHIVA_TVM_ID"), config.Int("SS_TVM_ID"))
		return err == nil
	}, time.Second*3, time.Second/10, "tvm init failed")

	ssConf := secrets.NewConf(config.Int("SHIVA_TVM_ID"))
	db := test_db.NewDb(t)
	log := test.NewLogger(t).WithField("test_name", t.Name())
	ssClient := secrets.NewAccessClient(ssConf, tvmCli, log)
	ss := secrets.NewService(ssConf, ssClient, log)
	flagSvc := feature_flags.NewService(db, nil, log)
	c := consul.NewService(consul.NewConf(common.Test))

	nomadSvc := nomad.NewService(conf, db, log, ss, flagSvc, c)
	nomadC, err := api.NewClient(&api.Config{Address: conf.Address, Region: conf.Region})
	require.NoError(t, err)
	return &NomadWrapper{
		nomad:            nomadSvc,
		client:           nomadC,
		progressDeadline: *conf.UpdateProgressDeadline,
	}
}

func (w *NomadWrapper) DefaultDC() []string {
	return config.StrList("NOMAD_DC")
}

func (w *NomadWrapper) Terminate() revert.RevertType {
	return revert.RevertType_Terminate
}

func (w *NomadWrapper) OOM() revert.RevertType {
	return revert.RevertType_OOM
}

func (w *NomadWrapper) HasFailAllocations() bool {
	return true
}

func (w *NomadWrapper) GlobalClean(t *testing.T) {
	regex := regexp.MustCompile("^" + test_helpers.JobPrefix + ".*")

	jobs, _, err := w.client.Jobs().List(nil)
	require.NoError(t, err)
	for _, job := range jobs {
		if time.Now().Sub(time.Unix(0, job.SubmitTime)) > time.Hour && regex.MatchString(job.Name) {
			_, _, err = w.client.Jobs().Deregister(job.ID, true, nil)
			require.NoError(t, err)
		}
	}
}

func (w *NomadWrapper) NewDownProgressDeadline(t *testing.T) Wrapper {
	conf := nomad.NewConf(common.Test)
	UpdateProgressDeadline := 1 * time.Minute
	UpdateHealthyDeadline := 30 * time.Second
	conf.UpdateProgressDeadline = &UpdateProgressDeadline
	conf.UpdateHealthyDeadline = &UpdateHealthyDeadline
	return w.new(t, conf)
}

func (w *NomadWrapper) NewShutdownDelay(t *testing.T, delay time.Duration) Wrapper {
	conf := nomad.NewConf(common.Test)
	conf.ShutdownDelay = delay
	return w.new(t, conf)
}

func (w *NomadWrapper) ProgressDeadline() time.Duration {
	return w.progressDeadline
}

func (w *NomadWrapper) Before(t *testing.T) {
	t.Parallel()
	currentCount := atomic.AddInt64(count, 1)
	time.Sleep(time.Duration(currentCount) * 3 * time.Second)
}

func (w *NomadWrapper) After(_ *testing.T) {
	// empty
}

func (w *NomadWrapper) Scheduler() scheduler.Scheduler {
	return w.nomad
}

type YDWrapper struct {
	yd               *yadeploy.Service
	client           *ydClient.Client
	progressDeadline time.Duration
}

func NewYDWrapper() Wrapper {
	return &YDWrapper{}
}

func (w *YDWrapper) New(t *testing.T) Wrapper {
	return w.new(t, yadeploy.NewConf(common.Test))
}

// new object for reconfigure
func (w *YDWrapper) new(t *testing.T, conf yadeploy.Conf) Wrapper {
	test.InitTestEnv()
	db := test_db.NewDb(t)
	sc := secrets.NewConf(config.Int("shiva_tvm_id"))
	log := test.NewLogger(t).WithField("TEST", t.Name()+strconv.Itoa(r.Int()))
	ac := secrets.NewAccessClient(sc, nil, log)
	ss := secrets.NewService(sc, ac, log)
	ra := registry.NewApi(registry.NewConf(), log)
	ydC := ydClient.NewClient(ydClient.NewConf())
	ydS := yadeploy.NewService(conf, ss, ydC, ra, db, log)
	return &YDWrapper{
		yd:               ydS,
		client:           ydC,
		progressDeadline: conf.ReadyDeadline,
	}
}

func (w *YDWrapper) DefaultDC() []string {
	return []string{"yd_sas", "yd_vla"}
}

func (w *YDWrapper) NewDownProgressDeadline(t *testing.T) Wrapper {
	conf := yadeploy.NewConf(common.Test)
	conf.ReadyDeadline = 4 * time.Minute
	return w.new(t, conf)
}

func (w *YDWrapper) NewShutdownDelay(t *testing.T, delay time.Duration) Wrapper {
	conf := yadeploy.NewConf(common.Test)
	return w.new(t, conf)
}

func (w *YDWrapper) ProgressDeadline() time.Duration {
	// TODO https://st.yandex-team.ru/VOID-1135
	return w.progressDeadline * 2
}

func (w *YDWrapper) Terminate() revert.RevertType {
	return revert.RevertType_Unhealthy
}

func (w *YDWrapper) OOM() revert.RevertType {
	return revert.RevertType_Unhealthy
}

func (w *YDWrapper) HasFailAllocations() bool {
	return false
}

func (w *YDWrapper) GlobalClean(t *testing.T) {
	log := test.NewLogger(t).WithField("context", "GlobalClean")
	all, err := w.client.GetAllBySource(context.Background(), "CI")
	require.NoError(t, err)
	now, err := w.client.Timestamp(context.Background())
	require.NoError(t, err)
	for _, stageID := range all {
		log := log.WithField("stageID", stageID)
		_, ts, err := w.client.GetStageSpec(context.Background(), stageID)
		if err != nil {
			log.WithError(err).Info("Status no load")
			continue
		}
		// 3 hours for clean CI jobs
		if now-ts > 3*60*60*1000000000 {
			err := w.client.RemoveStage(context.Background(), stageID)
			if err != nil {
				log.WithError(err).Info("Remove fail")
				continue
			}
			log.Info("Remove success")
		}
	}
}

func (w *YDWrapper) Before(t *testing.T) {
	t.Parallel()
}

func (w *YDWrapper) After(t *testing.T) {
	// Empty
}

func (w *YDWrapper) Scheduler() scheduler.Scheduler {
	return w.yd
}

func assertState(t *testing.T, w Wrapper, c chan *scheduler.State, stop, expected []scheduler.StateType, rt revert.RevertType) *scheduler.State {

	log := test.NewLogger(t)
	t.Helper()
	stopMap := map[scheduler.StateType]bool{}
	for _, t := range stop {
		stopMap[t] = true
	}

	var actual []scheduler.StateType
	prepareCount := 0
	var lastState *scheduler.State
	ctx, cancel := context.WithTimeout(context.Background(), 5*time.Minute)
	defer cancel()
	for {
		var ok bool
		var state *scheduler.State
		select {
		case state, ok = <-c:
		case <-ctx.Done():
			require.FailNow(t, "deploy freeze")
			return lastState
		}

		if !ok {
			var i, j int
			for i < len(actual) && j < len(expected) {
				if actual[i] == expected[j] {
					i++
				}
				j++
			}
			assert.Equal(t, len(actual), i, "Status assert fail. Actual: %v, Expected: %v", actual, expected)
			return lastState
		}

		if state.StateType == scheduler.Fail {
			log.Errorf("Inner fail", state.Description)
		}

		switch {
		case rState[state.StateType]:
			if rt != state.RevertType {
				require.Equal(t, rt.String(), state.RevertType.String())
				return lastState
			}
			// check revert type for skip cancel
			if (rt == revert.RevertType_Terminate || rt == revert.RevertType_Unhealthy) && w.HasFailAllocations() {
				require.True(t, len(state.FailedAllocations) > 0)
			}
		case state.StateType == scheduler.Prepare:
			prepareCount++
			// wait max 3 minute
			if prepareCount > 90 {
				require.FailNow(t, "Prepare freeze")
				return lastState
			}
		case stopMap[state.StateType]:
			require.FailNow(t, "Fail by stop state", "State: %s, desc: %s", state.StateType, state.Description)
			return lastState
		default:
			require.Equal(t, revert.RevertType_None.String(), state.RevertType.String())
			prepareCount = 0
		}

		if lastState == nil || lastState.StateType != state.StateType {
			actual = append(actual, state.StateType)
		}

		lastState = state
	}
}

func runDefault(t *testing.T, w Wrapper, isBranch bool) (*scheduler.Context, int64) {
	ctx := defaultContext(t, w, isBranch)
	ctx.Manifest.Upgrade.Parallel = 3
	id, c, err := w.Scheduler().Run(ctx)
	require.NoError(t, err)
	last := assertState(t, w, c, []scheduler.StateType{scheduler.Revert, scheduler.Fail}, []scheduler.StateType{scheduler.Prepare, scheduler.Process, scheduler.Success}, revert.RevertType_None)
	assertSuccessProvides(t, last.ProvidesStatus, "api")
	return ctx, id
}

func stop(t *testing.T, w Wrapper, isBranch bool) {
	_, _, err := w.Scheduler().Stop(defaultContext(t, w, isBranch))
	require.NoError(t, err)
}

func defaultContext(t *testing.T, w Wrapper, isBranch bool) *scheduler.Context {
	branchName := ""
	if isBranch {
		branchName = branch
	}

	m := defaultManifest(t, w)

	envs, err := m.Config.GetEnvs()
	require.NoError(t, err)

	return scheduler.MakeContext(test_helpers.Version, branchName, m, defaultMaps(t), r.Int63(), common.Test, nil, 0, envs, map[string]secrets.YavSecretInfo{}, map[string]secrets.Token{})
}

func defaultManifest(t *testing.T, w Wrapper) *manifest.Manifest {
	c := manifest.NewConfig()
	c.Params = map[string]string{
		"FAIL":                 "false",
		"API":                  "http",
		"OPS_PORT":             "81",
		"API_PORT":             "80",
		"SERVICE_NAME":         test_helpers.ServiceName(t),
		"_DEPLOY_G_SENTRY_DSN": config.Str("TESTAPP_SENTRY_DSN"),
		"CONSUL_KV_TTL":        "600s",
		"CI_MODE":              "true",
		"CONSUL_API_TOKEN":     config.Str("CONSUL_API_TOKEN"),
		"CONSUL_ADDRESS":       "consul-server.service.common.consul:8500",
	}

	m := &manifest.Manifest{
		Name:  test_helpers.ServiceName(t),
		Image: test_helpers.Image,
		Resources: manifest.Resources{
			CPU:    500,
			Memory: 128,
		},
		DC: map[string]int{},
		Upgrade: manifest.Upgrade{
			Parallel: 1,
		},
		Config: c,
	}

	for _, dc := range w.DefaultDC() {
		m.DC[dc] = 3
	}

	return m
}

func defaultMaps(t *testing.T) *proto.ServiceMap {
	return &proto.ServiceMap{
		Name: test_helpers.ServiceName(t),
		Provides: []*proto.ServiceProvides{
			{
				Name:        "api",
				Protocol:    proto.ServiceProvides_http,
				Port:        80,
				Description: "Test api",
			},
		},
	}
}

func assertSuccessProvides(t *testing.T, ps scheduler.ProvidesStatus, wantPs ...string) {
	for provides, v := range ps {
		switch provides.Name {
		case consul.MonitoringProvide.Name:
			assert.True(t, v)
		default:
			var found bool
			for _, p := range wantPs {
				if provides.Name == p {
					assert.True(t, v)
					found = true
					break
				}
			}
			if !found {
				assert.FailNow(t, fmt.Sprintf("provides %s undefined", provides.Name))
			}
		}
	}
}

func assertFailApiProvides(t *testing.T, ps scheduler.ProvidesStatus) {
	for provides, v := range ps {
		switch provides.Name {
		case consul.MonitoringProvide.Name:
			assert.True(t, v)
		case "api":
			assert.False(t, v)
		default:
			assert.FailNow(t, fmt.Sprintf("provides %s undefined", provides.Name))
		}
	}
}

func makeGRPCRequest(t *testing.T, address string, msg string) string {
	var message string
	test.Wait(t, func() error {
		ctx, cancel := context.WithTimeout(context.Background(), time.Millisecond*250)
		defer cancel()

		conn, err := grpc.DialContext(ctx, address, grpc.WithBlock(), grpc.WithInsecure())
		if err != nil {
			return err
		}
		echoCli := echo.NewEchoClient(conn)

		if response, err := echoCli.UnaryEcho(ctx, &echo.EchoRequest{Message: msg}); err == nil {
			message = response.Message
			return nil
		} else {
			return err
		}
	})
	return message
}

func assertPing(t *testing.T, address string) {

	c := http.Client{}
	resp, err := c.Get(fmt.Sprintf("http://%s:80/ping", address))
	require.NoError(t, err)
	assert.Equal(t, http.StatusOK, resp.StatusCode, address)
}

func assertEnv(t *testing.T, address, key, value string) {
	response := makeGRPCRequest(t, address, key)
	assert.Equal(t, fmt.Sprintf("key='%s' value='%s'", key, value), response)
}

func waitProcessState(t *testing.T, stateC chan *scheduler.State) *scheduler.State {
	var lastState = scheduler.Prepare
	timer := time.NewTimer(3 * time.Minute)
	defer timer.Stop()
	for {
		select {
		case state, ok := <-stateC:
			switch {
			case !ok:
				require.FailNow(t, fmt.Sprintf("state is nil while waiting for process state, last state is %s", lastState.String()))
				return nil
			case state.StateType == scheduler.Process:
				return state
			default:
				lastState = state.StateType
			}
		case <-timer.C:
			require.FailNow(t, fmt.Sprintf("request is not processed, last state is %s", lastState.String()))
			return nil
		}
	}
}

func catchPanic(t *testing.T) {
	if r := recover(); r != nil {
		test.NewLogger(t).Error(r)
		t.Fatal(r)
	}
}
