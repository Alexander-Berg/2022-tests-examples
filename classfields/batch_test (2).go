//go:build !unit || parallel
// +build !unit parallel

package nomad

import (
	"context"
	"sync"
	"testing"
	"time"

	"github.com/YandexClassifieds/shiva/cmd/shiva/scheduler"
	"github.com/YandexClassifieds/shiva/cmd/shiva/scheduler/test_helpers"
	"github.com/YandexClassifieds/shiva/common"
	"github.com/YandexClassifieds/shiva/common/config"
	spb "github.com/YandexClassifieds/shiva/pb/shiva/service_map"
	"github.com/YandexClassifieds/shiva/pkg/consul/kv"
	"github.com/YandexClassifieds/shiva/pkg/feature_flags"
	manifest "github.com/YandexClassifieds/shiva/pkg/manifest/model"
	"github.com/YandexClassifieds/shiva/test"
	"github.com/YandexClassifieds/shiva/test/test_db"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
)

var (
	_ scheduler.BatchScheduler = &BatchScheduler{}
)

func TestNomadBatch(t *testing.T) {
	test.RunUp(t)
	db := test_db.NewDb(t)
	log := test.NewLogger(t)
	flagSvc := feature_flags.NewService(db, nil, log)
	cfg := NewConf(common.Test)

	bs := NewBatchScheduler(cfg, nil, flagSvc, log)

	name := test_helpers.ServiceName(t)
	bc := makeContext(t, 42)
	_, err := bs.Run(bc)
	require.NoError(t, err)

	job, _, err := bs.cli.Jobs().Info(name, nil)
	require.NoError(t, err)

	assert.Equal(t, "42", job.Meta["batch_id"])
	require.NoError(t, bs.Stop(bc))
}

func TestNomadBatchState(t *testing.T) {
	t.Parallel()
	test.RunUp(t)
	db := test_db.NewDb(t)
	log := test.NewLogger(t)
	cfg := NewConf(common.Test)
	flagSvc := feature_flags.NewService(db, nil, test.NewLogger(t))

	bs := NewBatchScheduler(cfg, nil, flagSvc, log)

	t.Run("base_success", func(t *testing.T) {
		bc := makeContext(t, 43)
		stateC, err := bs.Run(bc)
		require.NoError(t, err)

		assertLastState(t, stateC, scheduler.Success, time.Minute)
	})
	t.Run("fail", func(t *testing.T) {
		bc := makeContext(t, 42)
		bc.Envs["FAIL"] = "true"

		stateC, err := bs.Run(bc)
		require.NoError(t, err)

		assertLastState(t, stateC, scheduler.Fail, time.Minute*10)
	})
	t.Run("stop", func(t *testing.T) {
		bc := makeContext(t, 42)
		bc.Envs["BATCH_TIME"] = "60s"

		stateC, err := bs.Run(bc)
		require.NoError(t, err)

		go func() {
			time.Sleep(time.Second * 20)
			require.NoError(t, bs.Stop(bc))
		}()

		assertLastState(t, stateC, scheduler.Canceled, time.Minute*10)
	})
	t.Run("stop_branch", func(t *testing.T) {
		ctx := makeContext(t, 42)
		ctx.Branch = "br"
		ctx.Envs["BATCH_TIME"] = "120s"

		stateC, err := bs.Run(ctx)
		require.NoError(t, err)

		go func() {
			time.Sleep(time.Second * 20)
			require.NoError(t, bs.Stop(ctx))
		}()

		assertLastState(t, stateC, scheduler.Canceled, time.Minute*10)
	})
	t.Run("cancel_old", func(t *testing.T) {
		bc := makeContext(t, 42)
		bc.Envs["BATCH_TIME"] = "30s"
		stateC, err := bs.Run(bc)
		require.NoError(t, err)

		wg := sync.WaitGroup{}
		wg.Add(2)
		go func() {
			defer wg.Done()
			assertLastState(t, stateC, scheduler.Canceled, time.Minute)
		}()

		<-time.After(time.Second * 10)
		bc2 := makeContext(t, 43)
		stateC2, err := bs.Run(bc2)
		require.NoError(t, err)
		go func() {
			defer wg.Done()
			assertLastState(t, stateC2, scheduler.Success, time.Minute)
		}()
		wg.Wait()
	})
	t.Run("resched_ok", func(t *testing.T) {
		name := test_helpers.ServiceName(t)
		bc := makeContext(t, 42)
		bc.Envs["STABLE_ON_ATTEMPT"] = "3"

		stateC, err := bs.Run(bc)
		require.NoError(t, err)

		assertLastState(t, stateC, scheduler.Success, time.Minute*10)

		conf := kv.NewConf(test.CINamespace)
		conf.ServiceName = name
		kvTestInfo := kv.NewEphemeralKV(test.NewLogger(t), conf, &test.TestInfo{})
		value, err := kvTestInfo.Get(test.TestInfoKey)
		require.NoError(t, err)
		testInfo, ok := value.(*test.TestInfo)
		require.True(t, ok)
		require.Len(t, testInfo.Hosts, 3)
	})
	t.Run("not_found", func(t *testing.T) {
		bc := makeContext(t, 22)
		stateC, err := bs.Run(bc)
		require.NoError(t, err)
		assertLastState(t, stateC, scheduler.Success, time.Minute*5)

		// ask for non-existent batch version
		bc = makeContext(t, 33)
		s2, err := bs.State(bc)
		require.Error(t, err)
		assert.Nil(t, s2)
	})
}

func assertLastState(t *testing.T, stateC chan *scheduler.State, stateType scheduler.StateType, timeout time.Duration) bool {
	t.Helper()
	deadline := time.After(timeout)
	lastState := &scheduler.State{}
	log := test.NewLogger(t)
	for run := true; run; {
		select {
		case <-deadline:
			t.Fatal("state read timeout")
		case state, ok := <-stateC:
			if ok {
				log.Printf("%+v", state)
				lastState = state
			} else {
				run = false
			}
		}
	}
	return assert.Equal(t, stateType, lastState.StateType)
}

func makeContext(t *testing.T, batchId int) *scheduler.BatchContext {
	name := test_helpers.ServiceName(t)
	cfg := manifest.NewConfig()
	cfg.Params = map[string]string{
		"IS_BATCH":             "true",
		"BATCH_TIME":           "15s",
		"API":                  "http",
		"SERVICE_NAME":         name,
		"OPS_PORT":             "81",
		"API_PORT":             "80",
		"_DEPLOY_G_SENTRY_DSN": config.Str("TESTAPP_SENTRY_DSN"),
		"CONSUL_KV_TTL":        "600s",
		"CI_MODE":              "true",
		"CONSUL_API_TOKEN":     config.Str("CONSUL_API_TOKEN"),
		"CONSUL_ADDRESS":       "consul-server.service.common.consul:8500",
	}

	envs, err := cfg.GetEnvs()
	require.NoError(t, err)

	return &scheduler.BatchContext{
		BatchId: int64(batchId),
		Version: test_helpers.Version,
		ServiceMap: &spb.ServiceMap{
			Name: name,
		},
		Manifest: &manifest.Manifest{
			Name:   test_helpers.ServiceName(t),
			Image:  test_helpers.Image,
			Config: cfg,
			Resources: manifest.Resources{
				CPU:    142,
				Memory: 128,
			},
		},
		Envs:    envs,
		Context: context.Background(),
	}
}
