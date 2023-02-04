package deployment_test

import (
	"context"
	"testing"
	"time"

	"github.com/YandexClassifieds/shiva/cmd/shiva/deployment"
	"github.com/YandexClassifieds/shiva/cmd/shiva/deployment/model"
	"github.com/YandexClassifieds/shiva/common"
	"github.com/YandexClassifieds/shiva/common/config"
	"github.com/YandexClassifieds/shiva/pb/shiva/types/revert"
	"github.com/YandexClassifieds/shiva/test"
	"github.com/YandexClassifieds/shiva/test/mock/election"
	"github.com/YandexClassifieds/shiva/test/test_db"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
)

func TestClean(t *testing.T) {

	// prepare
	test.RunUp(t)
	defer test.Down(t)
	T := newTest(t)
	cleaner := deployment.NewCleaner(test_db.NewDb(t), test.NewLogger(t), election.NewElectionStub())
	makeManifest(t, testManifest, sName)
	makeMap(t, testMap, sName, true, false)
	var (
		d1 = makeWaitApproveDeploy(t, T, "1.0.1")
		d2 = makeWaitApproveDeploy(t, T, "1.0.2")
		d3 = makeWaitApproveDeploy(t, T, "1.0.3")
		d4 = makeWaitApproveDeploy(t, T, "1.0.4")
		d5 = makeWaitApproveDeploy(t, T, "1.0.5")
		d6 = makeWaitApproveDeploy(t, T, "1.0.6")
	)
	addCreateTime(t, T, d3, 12*time.Hour)
	addCreateTime(t, T, d4, 25*time.Hour)
	addCreateTime(t, T, d5, 25*time.Hour)
	addCreateTime(t, T, d6, 48*time.Hour)

	// test
	cleaner.Clean()

	// asserts
	assertDeploymentState(t, T, d1, model.WaitApprove)
	assertDeploymentState(t, T, d2, model.WaitApprove)
	assertDeploymentState(t, T, d3, model.WaitApprove)
	assertDeploymentState(t, T, d4, model.Expired)
	assertDeploymentState(t, T, d5, model.Expired)
	assertDeploymentState(t, T, d5, model.Expired)
}

func assertDeploymentState(t *testing.T, T *Test, deployment *model.Deployment, state model.State) {

	d, err := T.storage.Get(deployment.ID)
	test.Check(t, err)
	assert.Equal(t, state, d.State)
}

func makeWaitApproveDeploy(t *testing.T, T *Test, v string) *model.Deployment {

	ctx := context.Background()
	c, dctx, err := T.service.Run(ctx, model.RunParams{
		Layer:        common.Test,
		Name:         "yandex_vertis_example_service_d",
		Version:      v,
		Branch:       "",
		Login:        "danevge",
		Source:       config.UnknownSource,
		OverrideConf: nil,
		OverrideEnv:  nil,
		Issues:       []string{"VOID-102"},
		TrafficShare: false,
		Force:        false,
	})
	require.NoError(t, err)
	T.testNomad.Success(t, common.Run, "yandex_vertis_example_service_d", "", v)
	result := <-c
	assert.True(t, result.SchState.StateType.IsEnd())

	_, dctx, err = T.service.Run(ctx, model.RunParams{
		Layer:        common.Prod,
		Name:         "yandex_vertis_example_service_d",
		Version:      v,
		Branch:       "",
		Login:        "danevge",
		Source:       config.UnknownSource,
		OverrideConf: nil,
		OverrideEnv:  nil,
		Issues:       []string{"VOID-102"},
		TrafficShare: false,
		Force:        false,
	})
	require.NoError(t, err)
	assert.Equal(t, model.WaitApprove, dctx.Deployment.State)
	return dctx.Deployment
}

func addCreateTime(t *testing.T, T *Test, d *model.Deployment, duration time.Duration) {

	d.CreatedAt = d.CreatedAt.Add(duration)
	err := T.storage.UpdateState(d, d.State, revert.RevertType_None, "")
	if err != nil {
		test.Check(t, err)
	}
}
