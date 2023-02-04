package deployment_test

import (
	"context"
	"errors"
	"testing"

	"github.com/YandexClassifieds/shiva/cmd/shiva/deployment/model"
	"github.com/YandexClassifieds/shiva/cmd/shiva/deployment/processor"
	"github.com/YandexClassifieds/shiva/cmd/shiva/env/external/storage"
	"github.com/YandexClassifieds/shiva/cmd/shiva/scheduler"
	"github.com/YandexClassifieds/shiva/common"
	"github.com/YandexClassifieds/shiva/common/user_error"
	"github.com/YandexClassifieds/shiva/pb/shiva/types/env"
	tmp_errors "github.com/YandexClassifieds/shiva/pkg/template/errors"
	"github.com/YandexClassifieds/shiva/test"
	"github.com/YandexClassifieds/shiva/test/mock/staff"
	"github.com/google/uuid"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
)

func TestDeployBatch_Run(t *testing.T) {
	test.RunUp(t)

	embedMap(t, "testdata/map_batch.yml", "batch-svc", false, false)
	embedManifest(t, "testdata/manifest_batch.yml", "batch-svc")
	T := newTest(t)
	stateC, dc, err := T.service.Run(context.Background(), model.RunParams{
		Layer:   common.Test,
		Name:    "batch-svc",
		Version: "v1",
		Login:   "danevge",
	})
	require.NoError(t, err)
	T.testBatch.Success(t, common.Run, "batch-svc", "", "v1")
	<-stateC
	assert.Equal(t, model.Success, dc.Deployment.State)
}

func TestDeployBatch_RunForce(t *testing.T) {
	test.RunUp(t)

	embedMap(t, "testdata/map_batch.yml", "batch-svc", false, false)
	embedManifest(t, "testdata/manifest_batch.yml", "batch-svc")
	T := newTest(t)
	// test that ForceRun is called when force flag is set
	stateC, dc, err := T.service.Run(context.Background(), model.RunParams{
		UUID:    uuid.New(),
		Layer:   common.Test,
		Name:    "batch-svc",
		Version: "v1",
		Login:   staff.Owner,
		Force:   true,
	})
	require.NoError(t, err)
	T.testBatch.Success(t, common.Run, "batch-svc", "", "v1")
	assertStateChan(t, scheduler.Success, model.Success, stateC)
	assert.Equal(t, model.Success, dc.Deployment.State)
	assert.Equal(t, 1, len(T.testBatch.ForceRunCtxs))

	// test for follow-up run without version or force
	stateC2, dc, err := T.service.Run(context.Background(), model.RunParams{
		UUID:  uuid.New(),
		Layer: common.Test,
		Name:  "batch-svc",
		Login: staff.Owner,
	})
	require.NoError(t, err)
	T.testBatch.Success(t, common.Run, "batch-svc", "", "v1")
	assertStateChan(t, scheduler.Success, model.Success, stateC2)
	assert.Equal(t, model.Success, dc.Deployment.State)
}

func TestDeployBatch_ForceRunConfig(t *testing.T) {
	test.RunUp(t)

	testCases := []struct {
		name            string
		version         string
		isConfigUpdated bool
	}{
		{
			name:            "use old config",
			version:         "",
			isConfigUpdated: false,
		},
		{
			name:            "use new config",
			version:         "v1",
			isConfigUpdated: true,
		},
	}

	embedMap(t, "testdata/map_batch.yml", "batch-svc", false, false)
	embedManifest(t, "testdata/manifest_batch_env1.yml", "batch-svc")

	for _, tc := range testCases {
		t.Run(tc.name, func(t *testing.T) {
			//prepare
			T := newTest(t)
			stateC, dc, err := T.service.Run(context.Background(), model.RunParams{
				Layer:   common.Test,
				Name:    "batch-svc",
				Version: "v1",
				Login:   "danevge",
				Branch:  "test-br",
				OverrideEnv: map[string]string{
					"test_param1": "override-env",
				},
			})
			require.NoError(t, err)
			T.testBatch.Success(t, common.Run, "batch-svc", "test-br", "v1")
			<-stateC
			require.Equal(t, model.Success, dc.Deployment.State)
			require.Equal(t, "override-env", dc.Envs["test_param1"])

			//test
			stateC, dc, err = T.service.Run(context.Background(), model.RunParams{
				Layer:   common.Test,
				Name:    "batch-svc",
				Login:   "danevge",
				Branch:  "test-br",
				Version: tc.version,
				Force:   true,
			})

			//assert
			require.NoError(t, err)

			expectedVersion := tc.version
			if expectedVersion == "" {
				expectedVersion = "v1"
			}

			T.testBatch.Success(t, common.Run, "batch-svc", "test-br", expectedVersion)
			<-stateC
			require.Equal(t, model.Success, dc.Deployment.State)

			if tc.isConfigUpdated {
				require.Equal(t, "t1", dc.Envs["test_param1"])
			} else {
				require.Equal(t, "override-env", dc.Envs["test_param1"])
			}

		})
	}
}

func TestDeployBatch_ForceRunWithoutVersionWithOverride(t *testing.T) {
	test.RunUp(t)

	embedMap(t, "testdata/map_batch.yml", "batch-svc", false, false)
	embedManifest(t, "testdata/manifest_batch_env1.yml", "batch-svc")
	T := newTest(t)
	stateC, dc, err := T.service.Run(context.Background(), model.RunParams{
		Layer:   common.Test,
		Name:    "batch-svc",
		Version: "v1",
		Login:   "danevge",
		Branch:  "test-br",
		OverrideEnv: map[string]string{
			"test_param1": "override-env",
		},
	})
	require.NoError(t, err)
	T.testBatch.Success(t, common.Run, "batch-svc", "test-br", "v1")
	<-stateC
	require.Equal(t, model.Success, dc.Deployment.State)
	require.Equal(t, "override-env", dc.Envs["test_param1"])

	//test
	stateC, dc, err = T.service.Run(context.Background(), model.RunParams{
		Layer:  common.Test,
		Name:   "batch-svc",
		Login:  "danevge",
		Branch: "test-br",
		OverrideEnv: map[string]string{
			"test_param2": "override-env2",
		},
		Force: true,
	})

	require.Equal(t, processor.ErrUpdateConfOverride, err)

}
func TestDeployBatch_RunForceRepeat(t *testing.T) {
	test.RunUp(t)
	embedMap(t, "testdata/map_batch.yml", "batch-svc-repeat", false, false)
	embedManifest(t, "testdata/manifest_batch.yml", "batch-svc-repeat")
	T := newTest(t)

	_, _, err := T.service.Run(context.Background(), model.RunParams{
		Layer:   common.Test,
		Name:    "batch-svc-repeat",
		Version: "v1",
		Login:   "danevge",
	})
	require.NoError(t, err)
	T.testBatch.Success(t, common.Run, "batch-svc-repeat", "", "v1")

	// test that ForceRun is called when force flag is set
	stateC, dc, err := T.service.Run(context.Background(), model.RunParams{
		Layer:   common.Test,
		Name:    "batch-svc-repeat",
		Version: "v1",
		Login:   "danevge",
		Force:   true,
	})
	require.NoError(t, err)
	T.testBatch.Success(t, common.Run, "batch-svc-repeat", "", "v1")
	assertStateChan(t, scheduler.Success, model.Success, stateC)
	assert.Equal(t, model.Success, dc.Deployment.State)
	assert.Equal(t, 1, len(T.testBatch.RunCtxs))
	assert.Equal(t, 1, len(T.testBatch.ForceRunCtxs))
}

func TestDeployBatch_RunTemplates_Success(t *testing.T) {
	const (
		svcName = "batch-svc"
		layer   = common.Test
		version = "v1"
	)

	test.RunUp(t)
	defer test.Down(t)

	embedMap(t, "testdata/map_batch.yml", svcName, false, false)
	embedMap(t, "testdata/map.yml", "shiva", false, false)
	embedManifest(t, "testdata/manifest_batch_env.yml", svcName)

	T := newTest(t)

	envStorage := storage.NewStorage(T.db, test.NewLogger(t))
	require.NoError(t, envStorage.Save(&storage.ExternalEnv{
		Service: "shiva",
		Layer:   layer,
		Type:    env.EnvType_GENERATED_TVM_ID,
		Key:     "tvm-id",
		Value:   "10",
	}))

	stateC, dctx, err := T.service.Run(context.Background(), model.RunParams{
		Layer:   layer,
		Name:    svcName,
		Version: version,
		Login:   "danevge",
	})
	require.NoError(t, err)

	T.testBatch.Success(t, common.Run, svcName, "", version)
	<-stateC

	assert.Equal(t, "10", dctx.Envs["test_param1"])
	assert.Equal(t, "80", dctx.Envs["test_param2"])
	assert.Equal(t, "http://shiva-deploy.vrts-slb.test.vertis.yandex.net:80", dctx.Envs["test_param3"])
	assert.Equal(t, "shiva-deploy.vrts-slb.test.vertis.yandex.net", dctx.Envs["test_param4"])
}

func TestDeployBatch_RunTemplates_Fail(t *testing.T) {
	const (
		svcName = "batch-svc"
		layer   = common.Test
		version = "v1"
	)

	test.RunUp(t)
	defer test.Down(t)

	embedMap(t, "testdata/map_batch.yml", svcName, false, false)
	embedMap(t, "testdata/map.yml", "shiva", false, false)
	embedManifest(t, "testdata/manifest_batch_invalid_env.yml", svcName)

	T := newTest(t)

	_, _, err := T.service.Run(context.Background(), model.RunParams{
		Layer:   layer,
		Name:    svcName,
		Version: version,
		Login:   "danevge",
	})

	var ue *user_error.UserErrors
	require.True(t, errors.As(err, &ue))

	errs := ue.Get()

	assert.Len(t, errs, 4)
	assert.Contains(t, errs, tmp_errors.NewErrUnknownService("test_param1", "shiva-ci"))
	assert.Contains(t, errs, tmp_errors.NewErrInvalidProvider("test_param2", "shiva", "admin"))
	assert.Contains(t, errs, tmp_errors.NewErrInvalidProvider("test_param3", "shiva", "api"))
	assert.Contains(t, errs, tmp_errors.NewErrInvalidProvider("test_param4", "shiva", "admin"))
}
