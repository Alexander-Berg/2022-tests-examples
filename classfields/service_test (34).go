package deployment_test

import (
	"context"
	"errors"
	"fmt"
	"strconv"
	"strings"
	"testing"
	"time"

	"github.com/YandexClassifieds/shiva/cmd/shiva/deployment"
	dcontext "github.com/YandexClassifieds/shiva/cmd/shiva/deployment/context"
	"github.com/YandexClassifieds/shiva/cmd/shiva/deployment/env_override"
	"github.com/YandexClassifieds/shiva/cmd/shiva/deployment/include_links"
	"github.com/YandexClassifieds/shiva/cmd/shiva/deployment/model"
	"github.com/YandexClassifieds/shiva/cmd/shiva/deployment/processor"
	"github.com/YandexClassifieds/shiva/cmd/shiva/env/external/reader"
	"github.com/YandexClassifieds/shiva/cmd/shiva/env/external/storage"
	"github.com/YandexClassifieds/shiva/cmd/shiva/env/external/writer"
	"github.com/YandexClassifieds/shiva/cmd/shiva/scaler"
	"github.com/YandexClassifieds/shiva/cmd/shiva/scheduler"
	"github.com/YandexClassifieds/shiva/common"
	"github.com/YandexClassifieds/shiva/common/config"
	"github.com/YandexClassifieds/shiva/common/user_error"
	spb "github.com/YandexClassifieds/shiva/pb/shiva/service_map"
	deployment2 "github.com/YandexClassifieds/shiva/pb/shiva/types/deployment"
	envPb "github.com/YandexClassifieds/shiva/pb/shiva/types/env"
	userError "github.com/YandexClassifieds/shiva/pb/shiva/types/error"
	"github.com/YandexClassifieds/shiva/pb/shiva/types/layer"
	"github.com/YandexClassifieds/shiva/pb/shiva/types/revert"
	"github.com/YandexClassifieds/shiva/pb/shiva/types/state"
	"github.com/YandexClassifieds/shiva/pb/ss/secret"
	"github.com/YandexClassifieds/shiva/pkg/feature_flags"
	"github.com/YandexClassifieds/shiva/pkg/include"
	"github.com/YandexClassifieds/shiva/pkg/include/model/data"
	"github.com/YandexClassifieds/shiva/pkg/manifest"
	mModel "github.com/YandexClassifieds/shiva/pkg/manifest/model"
	"github.com/YandexClassifieds/shiva/pkg/manifest/parser"
	"github.com/YandexClassifieds/shiva/pkg/service_map"
	"github.com/YandexClassifieds/shiva/pkg/staff"
	staffapi "github.com/YandexClassifieds/shiva/pkg/staff/api"
	stAPI "github.com/YandexClassifieds/shiva/pkg/tracker/api"
	"github.com/YandexClassifieds/shiva/test"
	"github.com/YandexClassifieds/shiva/test/mock/service_change"
	staff_fixture "github.com/YandexClassifieds/shiva/test/mock/staff"
	"github.com/YandexClassifieds/shiva/test/test_db"
	"github.com/google/uuid"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/mock"
	"github.com/stretchr/testify/require"
)

// real test approve case life circle with scheduler mock
func TestApprove(t *testing.T) {
	test.RunUp(t)

	T := newTest(t)
	T.prepare(false, true, false)

	ctx := context.Background()
	td := T.TestData()
	testCtx := T.fullRun(td)
	assert.Nil(t, testCtx.Previous)

	td = T.TestData()
	td.issues = []string{"VOID-102"}
	td.layer = common.Prod
	dCtx := T.fullRunSox(td)
	assert.Nil(t, dCtx.Previous)

	dCtx.Deployment.StartDate = dCtx.Deployment.StartDate.Add(-time.Duration(24) * time.Hour)
	err := T.storage.UpdateState(dCtx.Deployment, dCtx.Deployment.State, revert.RevertType_None, "")
	require.NoError(t, err)

	services, err := T.service.ApproveList("avkosorukov")
	require.NoError(t, err)
	assert.NotEmpty(t, services)
	assertContains(t, services, dCtx, true)
	assert.Equal(t, 0, T.lock.LockInc)

	d, err := T.service.ApproveList2("avkosorukov")
	require.NoError(t, err)
	assert.Len(t, d, 1)

	services, err = T.service.ApproveList(td.login)
	assert.Empty(t, services)
	require.NoError(t, err)
	assertContains(t, services, dCtx, false)
	assert.Equal(t, 0, T.lock.LockInc)
	assert.Equal(t, 0, T.lock.UnlockInc)

	d, err = T.service.ApproveList2(td.login)
	require.NoError(t, err)
	assert.Len(t, d, 0)

	T.producer.AssertEmpty(t)
	c, dCtx, err := T.service.Approve(ctx, model.ApproveParams{
		ID:     dCtx.Deployment.ID,
		Login:  td.login,
		Source: config.UnknownSource,
	})
	assert.Equal(t, processor.ErrApproverIsDeployAuthor, err)
	assert.Nil(t, c)
	T.producer.AssertEmpty(t)
	assert.True(t, time.Now().Sub(dCtx.Deployment.StartDate) > time.Duration(12)*time.Hour)

	c, dCtx, err = T.service.Approve(ctx, model.ApproveParams{
		ID:     dCtx.Deployment.ID,
		Login:  "avkosorukov",
		Source: config.UnknownSource,
	})
	require.NoError(t, err)
	assert.Nil(t, dCtx.Previous)
	T.producer.Assert(t, dCtx, model.Process)
	assert.True(t, time.Now().Sub(dCtx.Deployment.StartDate) < time.Duration(12)*time.Hour)
	T.prodNomad.Success(t, common.Run, td.name, td.branch, td.version)
	assertStateChan(t, scheduler.Success, model.Success, c)
	T.producer.Assert(t, dCtx, model.Success)
	assert.Equal(t, 1, len(T.prodNomad.RunCtxs))
	assert.Equal(t, 1, T.lock.LockInc)
	assert.Equal(t, 1, T.lock.UnlockInc)

	td = T.TestData()
	td.version = "v2"
	newTestCtx := T.fullRun(td)
	require.NotNil(t, newTestCtx.Previous)
	assert.Equal(t, testCtx.Deployment.ID, newTestCtx.Previous.Deployment.ID)

	td = T.TestData()
	td.issues = []string{"VOID-102"}
	td.layer = common.Prod
	td.version = "v2"
	newProdCtx := T.fullRunSox(td)
	require.NotNil(t, newProdCtx.Previous)
	assert.Equal(t, dCtx.Deployment.ID, newProdCtx.Previous.Deployment.ID)

	c, newProdCtx, err = T.service.Approve(ctx, model.ApproveParams{
		ID:     newProdCtx.Deployment.ID,
		Login:  "avkosorukov",
		Source: config.UnknownSource,
	})
	require.NoError(t, err)
	require.NotNil(t, newProdCtx.Previous)
	assert.Equal(t, dCtx.Deployment.ID, newProdCtx.Previous.Deployment.ID)
}

func TestLock(t *testing.T) {

	test.RunUp(t)
	defer test.Down(t)
	T := newTest(t)
	T.prepare(false, false, false)
	T.fullRun(T.TestData())
}

func TestCancel(t *testing.T) {

	test.RunUp(t)
	defer test.Down(t)
	T := newTest(t)
	makeManifest(t, testManifest, sName)
	makeMap(t, testMap, sName, false, false)

	td := T.TestData()
	td.name = sName
	firstRunCtx := T.fullRun(td)
	assert.Nil(t, firstRunCtx.Previous)

	ctx := context.Background()
	runC, runCtx, err := T.service.Run(ctx, model.RunParams{
		Layer:   common.Test,
		Name:    "yandex_vertis_example_service_d",
		Version: "1.0.1",
		Login:   td.login,
		Comment: comment,
		Source:  config.UnknownSource,
	})
	require.NoError(t, err)
	require.NotNil(t, runCtx.Previous)
	assert.Equal(t, firstRunCtx.Deployment.ID, runCtx.Previous.Deployment.ID)
	T.producer.Assert(t, runCtx, model.Process)

	cancelC, cancelCtx, err := T.service.Cancel(ctx, model.CancelParams{
		ID:     runCtx.Deployment.ID,
		Login:  "danevge",
		Source: config.UnknownSource,
	})
	require.NoError(t, err)
	require.NotNil(t, cancelCtx.Previous)
	assert.Equal(t, firstRunCtx.Deployment.ID, cancelCtx.Previous.Deployment.ID)
	T.producer.Assert(t, cancelCtx, model.Process)

	stateC, _, err := T.service.State(context.Background(), cancelCtx.Deployment.ID)
	require.NoError(t, err)

	assert.Equal(t, runCtx.Deployment.ID, cancelCtx.Parent.Deployment.ID)
	assert.True(t, cancelCtx.Parent.Deployment.Overridden)
	assert.False(t, cancelCtx.Deployment.Overridden)
	assert.Equal(t, model.Cancel, cancelCtx.Parent.Deployment.State)
	assert.Equal(t, model.Process, cancelCtx.Deployment.State)

	go T.testNomad.CancelAndReverted(t, common.Cancel, cancelCtx.Deployment.Name, "", fmt.Sprintf("%d/%d", cancelCtx.Parent.Deployment.NomadID, cancelCtx.Deployment.NomadID))
	assertStateChan(t, scheduler.Revert, model.Process, cancelC)
	assertStateChan(t, scheduler.RevertSuccess, model.Success, cancelC)
	T.assertUpdatedState(model.Canceled, runCtx)
	T.assertUpdatedState(model.Success, cancelCtx)
	T.producer.Assert(t, runCtx, model.Canceled)
	T.producer.Assert(t, cancelCtx, model.Success)

	go T.testNomad.CancelAndReverted(t, common.Run, cancelCtx.Deployment.Name, "", "1.0.1")
	assertStateChan(t, scheduler.Revert, model.Cancel, runC)
	require.NotNil(t, runCtx.Child)
	assert.Equal(t, cancelCtx.Deployment.ID, runCtx.Child.Deployment.ID)
	assertStateChan(t, scheduler.RevertSuccess, model.Canceled, runC)

	// TODO what?
	go T.testNomad.Process(t, common.Run, cancelCtx.Deployment.Name, "", fmt.Sprintf("id:%d", cancelCtx.Deployment.NomadID))
	assertStateChan(t, scheduler.Process, model.Process, stateC)
	require.NotNil(t, runCtx.Child)
	go T.testNomad.Success(t, common.Run, cancelCtx.Deployment.Name, "", fmt.Sprintf("id:%d", cancelCtx.Deployment.NomadID))
	assertStateChan(t, scheduler.Success, model.Success, stateC)
	require.NotNil(t, runCtx.Child)
	// CHECK
	T.assertUpdatedState(model.Success, cancelCtx)

	reloadRunDeployment, err := T.storage.Get(runCtx.Deployment.ID)
	require.NoError(t, err)
	reloadCancelDeployment, err := T.storage.Get(cancelCtx.Deployment.ID)
	require.NoError(t, err)
	assert.Equal(t, reloadRunDeployment.ID, reloadCancelDeployment.ParentId)
	assert.Equal(t, reloadRunDeployment.PreviousId, reloadCancelDeployment.PreviousId)

	assert.True(t, cancelCtx.Parent.Deployment.Overridden)
	assert.False(t, cancelCtx.Deployment.Overridden)
	assert.Equal(t, model.Canceled.String(), reloadRunDeployment.State.String())
	assert.Equal(t, model.Success.String(), reloadCancelDeployment.State.String())
}

func TestCancelFail(t *testing.T) {

	test.RunUp(t)
	defer test.Down(t)
	T := newTest(t)
	makeManifest(t, testManifest, sName)
	makeMap(t, testMap, sName, false, false)

	td := T.TestData()
	td.name = sName
	firstRunCtx := T.fullRun(td)
	assert.Nil(t, firstRunCtx.Previous)

	ctx := context.Background()
	runC, runCtx, err := T.service.Run(ctx, model.RunParams{
		Layer:   common.Test,
		Name:    "yandex_vertis_example_service_d",
		Version: "1.0.1",
		Login:   td.login,
		Comment: comment,
		Source:  config.UnknownSource,
	})
	require.NoError(t, err)
	require.NotNil(t, runCtx.Previous)
	assert.Equal(t, firstRunCtx.Deployment.ID, runCtx.Previous.Deployment.ID)
	T.producer.Assert(t, runCtx, model.Process)

	sherr := errors.New("cancel error")
	T.testNomad.Error = sherr
	_, _, err = T.service.Cancel(ctx, model.CancelParams{
		ID:     runCtx.Deployment.ID,
		Login:  "danevge",
		Source: config.UnknownSource,
	})
	assert.Equal(t, sherr, err)
	T.testNomad.Error = nil

	T.assertUpdatedState(model.Process, runCtx)
	T.testNomad.Success(t, common.Run, runCtx.Deployment.Name, "", "1.0.1")
	assertStateChan(t, scheduler.Success, model.Success, runC)
	T.testNomad.Success(t, common.Run, "yandex_vertis_example_service_d", "", fmt.Sprintf("id:%d", runCtx.Deployment.NomadID))
	T.assertUpdatedState(model.Success, runCtx)
}

func TestFailFirstRun(t *testing.T) {

	test.RunUp(t)
	defer test.Down(t)
	T := newTest(t)
	T.prepare(false, false, false)

	td := T.TestData()
	runC, runCtx, err := T.run(td)
	require.NoError(t, err)
	T.producer.Assert(t, runCtx, model.Process)

	T.testNomad.Fail(t, common.Run, td.name, td.branch, td.version)
	T.testNomad.SuccessStop(t, common.Stop, td.name, td.branch)
	assertStateChan(t, scheduler.Fail, model.Fail, runC)
	T.producer.Assert(t, runCtx, model.Fail)
}

func TestCancelFirstRun(t *testing.T) {

	test.RunUp(t)
	defer test.Down(t)
	T := newTest(t)
	makeManifest(t, testManifest, sName)
	makeMap(t, testMap, sName, false, false)

	ctx := context.Background()
	runC, runCtx, err := T.service.Run(ctx, paramsRun)
	require.NoError(t, err)
	T.producer.Assert(t, runCtx, model.Process)

	cancelC, cancelCtx, err := T.service.Cancel(ctx, model.CancelParams{
		ID:     runCtx.Deployment.ID,
		Login:  "danevge",
		Source: config.UnknownSource,
	})
	require.NoError(t, err)
	T.producer.Assert(t, cancelCtx, model.Process)

	assert.Equal(t, runCtx.Deployment.ID, cancelCtx.Parent.Deployment.ID)
	assert.True(t, cancelCtx.Parent.Deployment.Overridden)
	assert.False(t, cancelCtx.Deployment.Overridden)
	assert.Nil(t, cancelCtx.Previous)
	assert.Equal(t, model.Cancel, cancelCtx.Parent.Deployment.State)
	assert.Equal(t, model.Process, cancelCtx.Deployment.State)

	T.testNomad.Canceled(t, common.Cancel, cancelCtx.Deployment.Name, "", fmt.Sprintf("%d/%d", cancelCtx.Parent.Deployment.NomadID, cancelCtx.Deployment.NomadID))
	assertStateChan(t, scheduler.RevertSuccess, model.Success, cancelC)
	T.producer.Assert(t, runCtx, model.Canceled)
	T.producer.Assert(t, cancelCtx, model.Success)

	T.testNomad.Canceled(t, common.Run, cancelCtx.Deployment.Name, "", "1.0.0")
	assertStateChan(t, scheduler.RevertSuccess, model.Canceled, runC)

	T.assertUpdatedState(model.Canceled, runCtx)
	T.assertUpdatedState(model.Success, cancelCtx)
	assert.Equal(t, runCtx.Deployment.ID, cancelCtx.Parent.Deployment.ID)
	assert.True(t, cancelCtx.Parent.Deployment.Overridden)
	assert.False(t, cancelCtx.Deployment.Overridden)
}

func TestCancelWaitApprove(t *testing.T) {
	test.RunUp(t)
	defer test.Down(t)
	T := newTest(t)
	T.prepare(false, true, false)
	td := T.TestData()
	T.fullRun(td)

	td = T.TestData()
	td.layer = common.Prod
	td.issues = []string{"VOID-102"}
	_, runCtx, err := T.run(td)
	require.NoError(t, err)

	cancelC, cancelCtx, err := T.service.Cancel(td.ctx, model.CancelParams{
		ID:     runCtx.Deployment.ID,
		Login:  td.login,
		Source: td.source,
	})
	assert.Nil(t, cancelC)
	require.NoError(t, err)
	assertDState(t, model.Canceled, cancelCtx.Parent.Deployment.State)
	assertDState(t, model.Success, cancelCtx.Deployment.State)
}

func TestRunDuplicate(t *testing.T) {
	test.RunUp(t)
	db := test_db.NewDb(t)
	log := test.NewLogger(t)

	makeManifest(t, testManifest, t.Name())
	makeMap(t, testMap, t.Name(), false, false)
	incSvc := include.NewService(db, log)
	manSvc := manifest.NewService(db, log, parser.NewService(log, reader.NewService(db, log)), incSvc)
	mapSvc := service_map.NewService(db, log, nil)
	_, mapId, err := mapSvc.GetByFullPath(fmt.Sprintf("maps/%s.yml", t.Name()))
	require.NoError(t, err)
	_, manId, err := manSvc.GetByNameWithId(common.Test, t.Name())
	require.NoError(t, err)

	id := uuid.New()
	db.GormDb.Create(&model.Deployment{
		UUID:             model.WrapUUID(id),
		Layer:            common.Test,
		Name:             t.Name(),
		Version:          sVersion,
		Author:           &staff.User{Login: "alexander-s"},
		Type:             common.Run,
		State:            model.Success,
		ServiceMapsID:    mapId,
		DeployManifestID: manId,
		StartDate:        time.Now(),
		EndDate:          time.Now(),
		Status:           &deployment2.Status{},
	})

	T := newTest(t)
	runC, dc, err := T.service.Run(context.TODO(), model.RunParams{
		UUID:    id,
		Layer:   common.Test,
		Name:    t.Name(),
		Version: sVersion,
		Login:   "alexander-s",
	})
	require.NoError(t, err)
	var s *deployment.StateChange
	select {
	case <-time.After(time.Second * 2):
		t.Fatal("expected deployment to be finished")
	case s = <-runC:
	}
	require.NotNil(t, s.Status)
	assert.Equal(t, state.DeploymentState_SUCCESS, dc.Deployment.GetStateProto())
	assert.Equal(t, revert.RevertType_None, s.Status.RevertType)
}

func TestRunWithoutVersion(t *testing.T) {

	test.RunUp(t)
	defer test.Down(t)
	T := newTest(t)
	T.prepare(false, false, false)
	td := T.TestData()
	firstCtx := T.fullRun(td)
	td.version = ""

	_, _, err := T.run(td)
	assert.Equal(t, err, processor.ErrNothingChanged)
	makeManifest(t, testManifest1, t.Name())

	runC, runCtx, err := T.run(td)
	require.NoError(T.t, err)
	T.testNomad.Success(T.t, common.Run, td.name, td.branch, firstCtx.Deployment.Version)
	assertStateChan(T.t, scheduler.Success, model.Success, runC)
	T.producer.Reset()
	assert.Equal(T.t, model.Success, runCtx.Deployment.State)

	stateC, stopCtx, err := T.stop(td)
	require.NoError(t, err)
	T.producer.Assert(t, stopCtx, model.Process)
	T.testNomad.SuccessStop(t, common.Stop, td.name, td.branch)
	assertStateChan(t, scheduler.Success, model.Success, stateC)
	T.producer.Assert(t, stopCtx, model.Success)

	_, _, err = T.run(td)
	assert.Equal(t, err, processor.ErrServiceNotDeployed)
	td.name = td.name + "1"
	_, _, err = T.run(td)
	assert.Equal(t, err, processor.ErrServiceNotDeployed)
}

func TestRunWithoutVersionWithOverride(t *testing.T) {

	test.RunUp(t)
	defer test.Down(t)
	T := newTest(t)
	T.prepare(false, false, false)
	td := T.TestData()
	version := td.version
	T.fullRun(td)
	td.envOverride = map[string]string{"env": "e"}
	td.version = ""
	runC, runCtx, err := T.run(td)
	require.NoError(t, err)
	T.producer.Assert(t, runCtx, model.Process)
	T.testNomad.Success(t, common.Run, td.name, td.branch, version)
	assertStateChan(t, scheduler.Success, model.Success, runC)
	T.producer.Assert(t, runCtx, model.Success)

	td.envOverride = nil
	runC, runCtx, err = T.run(td)
	assert.Equal(t, processor.ErrUpdateConfOverride, err)
}

func TestService_Run_BranchValidation(t *testing.T) {
	var err error
	test.RunUp(t)

	T := newTest(t)
	embedManifest(t, "testdata/manifest.yml", sName)
	embedMap(t, "testdata/map.yml", sName, false, false)

	testCases := []struct {
		name, branch string
		expected     error
	}{
		{name: "underscore", branch: "br_with_underscore", expected: processor.ErrBranchNameInvalid},
		{name: "double_dash", branch: "br--with--double--dashes", expected: processor.ErrBranchNameInvalid},
		{name: "prefix_dash", branch: "-br-beginning-with-dashes", expected: processor.ErrBranchNameInvalid},
		{name: "suffix_dash", branch: "br-finishing-with-dashes-", expected: processor.ErrBranchNameInvalid},
		{name: "valid", branch: "ValidBranch-42"},
	}
	for _, tc := range testCases {
		t.Run(tc.name, func(t *testing.T) {
			_, _, err = T.service.Run(context.Background(), model.RunParams{
				Layer:   common.Test,
				Name:    sName,
				Version: "v1",
				Branch:  tc.branch,
				Login:   staff_fixture.Owner,
			})
		})
		if tc.expected != nil {
			assert.True(t, errors.Is(err, tc.expected))
		} else {
			assert.NoError(t, err)
		}
	}
}

func TestService_RunServiceNameLenOverLimit(t *testing.T) {
	test.RunUp(t)
	T := newTest(t)

	const (
		svcWithoutProvides   = "service1"
		svcWithShortProvides = "service2"
		svcWithLongProvides  = "service3"
	)

	embedMap(t, "testdata/map_without_provides.yml", svcWithoutProvides, false, false)
	makeManifest(t, testManifest, svcWithoutProvides)
	embedMap(t, "testdata/map_short_provide_name.yml", svcWithShortProvides, false, false)
	makeManifest(t, testManifest, svcWithShortProvides)
	embedMap(t, "testdata/map_with_long_provide_name.yml", svcWithLongProvides, false, false)
	makeManifest(t, testManifest, svcWithLongProvides)

	testCases := []struct {
		name     string
		service  string
		branch   string
		expected error
	}{
		{
			name:     "without provides valid",
			service:  svcWithoutProvides,
			branch:   "short-branch-name",
			expected: nil,
		},
		//len 8 + 46 + 11 = 65
		{
			name:     "without provides invalid",
			service:  svcWithoutProvides,
			branch:   "check-that-our-service-name-consul-is-more-63",
			expected: processor.ErrServiceNameLenOverLimit,
		},
		//len 8 + 46 + 11 = 65
		{
			name:     "with short provides invalid",
			service:  svcWithShortProvides,
			branch:   "check-that-our-service-name-consul-is-more-63",
			expected: processor.ErrServiceNameLenOverLimit,
		},
		//len 8 + 46 + 17 = 71
		{
			name:     "with long provides invalid",
			service:  svcWithLongProvides,
			branch:   "check-that-our-service-name-consul-is-more-63",
			expected: processor.ErrServiceNameLenOverLimit,
		},
	}

	ctx := context.Background()
	for _, tc := range testCases {
		t.Run(tc.name, func(t *testing.T) {
			_, _, err := T.service.Run(ctx, model.RunParams{
				Layer:   common.Test,
				Name:    tc.service,
				Version: "v1",
				Branch:  tc.branch,
				Login:   staff_fixture.Owner,
			})
			assert.Equal(t, tc.expected, err)
		})
	}
}

func TestRunWithoutVersionServiceWithInclude(t *testing.T) {

	test.RunUp(t)
	defer test.Down(t)
	T := newTest(t)
	makeIncludes(t, commonYml, "/shiva/common.yml")
	makeIncludes(t, testYml, "/shiva/test.yml")
	makeIncludes(t, testYml, "/shiva/prod.yml")
	makeManifest(t, testManifestWithIncludes1, sName)
	makeMap(t, testMap, sName, false, false)

	ctx := context.Background()
	runC, runCtx, err := T.service.Run(ctx, paramsRun)
	require.NoError(t, err)
	T.producer.Assert(t, runCtx, model.Process)
	T.testNomad.Success(t, common.Run, "yandex_vertis_example_service_d", "", "1.0.0")
	<-runC
	T.producer.Assert(t, runCtx, model.Success)

	_, _, err = T.service.Run(ctx, model.RunParams{
		Layer:  common.Test,
		Name:   "yandex_vertis_example_service_d",
		Login:  "danevge",
		Source: config.UnknownSource,
	})
	assert.Equal(t, err, processor.ErrNothingChanged)
	makeIncludes(t, testYml1, "/shiva/test.yml")
	runC, runWvCtx, err := T.service.Run(ctx, model.RunParams{
		Layer:  common.Test,
		Name:   "yandex_vertis_example_service_d",
		Login:  "danevge",
		Source: config.UnknownSource,
	})
	require.NoError(t, err)
	assert.Equal(t, runWvCtx.Deployment.Version, "1.0.0")
	T.producer.Assert(t, runWvCtx, model.Process)
	T.testNomad.Success(t, common.Run, "yandex_vertis_example_service_d", "", "1.0.0")
	<-runC
	T.producer.Assert(t, runWvCtx, model.Success)
	assert.False(t, assertEqualIncludes(t, runWvCtx.Deployment.ID, runCtx.Deployment.ID))

	_, _, err = T.service.Run(ctx, model.RunParams{
		Layer:  common.Test,
		Name:   "yandex_vertis_example_service_d",
		Login:  "danevge",
		Source: config.UnknownSource,
	})
	assert.Equal(t, err, processor.ErrNothingChanged)

	makeManifest(t, testManifestWithIncludes2, sName)
	runC, runCtx, err = T.service.Run(ctx, model.RunParams{
		Layer:  common.Test,
		Name:   "yandex_vertis_example_service_d",
		Login:  "danevge",
		Source: config.UnknownSource,
	})
	require.NoError(t, err)
	assert.Equal(t, runCtx.Deployment.Version, "1.0.0")
	T.producer.Assert(t, runCtx, model.Process)
	T.testNomad.Success(t, common.Run, "yandex_vertis_example_service_d", "", "1.0.0")
	<-runC
	T.producer.Assert(t, runCtx, model.Success)
	assert.False(t, assertEqualIncludes(t, runWvCtx.Deployment.ID, runCtx.Deployment.ID))
}

func TestRevert(t *testing.T) {

	test.RunUp(t)
	defer test.Down(t)
	T := newTest(t)
	makeIncludes(t, commonYml, "/shiva/common.yml")
	makeIncludes(t, testYml, "/shiva/test.yml")
	makeIncludes(t, testYml, "/shiva/prod.yml")
	makeManifest(t, testManifestWithIncludes1, sName)
	makeMap(t, testMap, sName, false, false)

	ctx := context.Background()
	runC, runCtx, err := T.service.Run(ctx, paramsRun)
	require.NoError(t, err)
	require.Nil(t, runCtx.Previous)
	T.producer.Assert(t, runCtx, model.Process)
	T.testNomad.Success(t, common.Run, "yandex_vertis_example_service_d", "", "1.0.0")
	<-runC
	T.producer.Assert(t, runCtx, model.Success)

	runC, runCtx1, err := T.service.Run(ctx, paramsRunNext)
	require.NoError(t, err)
	require.NotNil(t, runCtx1.Previous)
	assert.Equal(t, runCtx.Deployment.ID, runCtx1.Previous.Deployment.ID)
	T.producer.Assert(t, runCtx1, model.Process)
	T.testNomad.Success(t, common.Run, "yandex_vertis_example_service_d", "", "1.0.1")
	<-runC
	T.producer.Assert(t, runCtx1, model.Success)

	makeIncludes(t, testYml1, "/shiva/test.yml")

	revertC, revertCtx, err := T.service.Revert(ctx, model.RevertParams{
		UUID:    uuid.New(),
		Layer:   common.Test,
		Name:    "yandex_vertis_example_service_d",
		Login:   "danevge",
		Comment: comment,
	})
	require.NoError(t, err)
	require.NotNil(t, revertCtx.Previous)
	assert.Equal(t, runCtx1.Deployment.ID, revertCtx.Previous.Deployment.ID)
	T.producer.Assert(t, revertCtx, model.Process)
	T.testNomad.Success(t, common.Run, "yandex_vertis_example_service_d", "", "1.0.0")
	<-revertC
	T.producer.Assert(t, revertCtx, model.Success)

	dCtx, err := T.ctxFactory.RestoreContext(revertCtx.Deployment.ID)
	require.NoError(t, err)
	assert.Equal(t, dCtx.Deployment.Version, "1.0.0")
	assert.Equal(t, comment, dCtx.Deployment.Comment)
	assert.Equal(t, runCtx.Deployment.DeployManifestID, dCtx.Deployment.DeployManifestID)
	assert.Equal(t, runCtx.Deployment.ServiceMapsID, dCtx.Deployment.ServiceMapsID)
	assert.Equal(t, runCtx.Deployment.Branch, dCtx.Deployment.Branch)
	assert.Equal(t, runCtx.Deployment.Layer, dCtx.Deployment.Layer)

	assert.True(t, assertEqualIncludes(t, dCtx.Deployment.ID, runCtx.Deployment.ID))
}

func TestRevertWithOverride(t *testing.T) {

	test.RunUp(t)
	defer test.Down(t)
	T := newTest(t)
	makeIncludes(t, commonYml, "/shiva/common.yml")
	makeIncludes(t, testYml, "/shiva/test.yml")
	makeIncludes(t, testYml, "/shiva/prod.yml")
	makeManifest(t, testManifestWithIncludes1, sName)
	makeMap(t, testMap, sName, false, false)

	makeIncludes(t, prodYml, "/shiva/prod1.yml")
	confOverride := []string{"/shiva/prod1.yml"}
	envOverride := map[string]string{"env": "e"}
	ctx := context.Background()
	runC, runCtx, err := T.service.Run(ctx, model.RunParams{
		Layer:        common.Test,
		Name:         "yandex_vertis_example_service_d",
		Version:      "1.0.0",
		Branch:       "b1",
		Login:        "danevge",
		Comment:      comment,
		Source:       config.UnknownSource,
		OverrideConf: confOverride,
		OverrideEnv:  envOverride,
	})
	require.NoError(t, err)
	require.Nil(t, runCtx.Previous)
	T.producer.Assert(t, runCtx, model.Process)
	T.testNomad.Success(t, common.Run, "yandex_vertis_example_service_d", "b1", "1.0.0")
	<-runC
	T.producer.Assert(t, runCtx, model.Success)

	runC, runCtx1, err := T.service.Run(ctx, model.RunParams{
		Layer:   common.Test,
		Name:    "yandex_vertis_example_service_d",
		Version: "1.0.1",
		Branch:  "b1",
		Login:   "danevge",
		Comment: comment,
		Source:  config.UnknownSource,
	})
	require.NoError(t, err)
	require.NotNil(t, runCtx1.Previous)
	assert.Equal(t, runCtx.Deployment.ID, runCtx1.Previous.Deployment.ID)
	T.producer.Assert(t, runCtx1, model.Process)
	T.testNomad.Success(t, common.Run, "yandex_vertis_example_service_d", "b1", "1.0.1")
	<-runC
	T.producer.Assert(t, runCtx1, model.Success)

	makeIncludes(t, testYml1, "/shiva/test.yml")
	makeIncludes(t, commonYml, "/shiva/prod1.yml")

	revertC, revertCtx, err := T.service.Revert(ctx, model.RevertParams{
		UUID:    uuid.New(),
		Layer:   common.Test,
		Name:    "yandex_vertis_example_service_d",
		Branch:  "b1",
		Login:   "danevge",
		Comment: comment,
	})
	require.NoError(t, err)
	require.NotNil(t, revertCtx.Previous)
	assert.Equal(t, runCtx1.Deployment.ID, revertCtx.Previous.Deployment.ID)
	T.producer.Assert(t, revertCtx, model.Process)
	T.testNomad.Success(t, common.Run, "yandex_vertis_example_service_d", "b1", "1.0.0")
	<-revertC
	T.producer.Assert(t, revertCtx, model.Success)

	dCtx, err := T.ctxFactory.RestoreContext(revertCtx.Deployment.ID)
	require.NoError(t, err)
	assert.Equal(t, dCtx.Deployment.Version, "1.0.0")
	assert.Equal(t, comment, dCtx.Deployment.Comment)
	assert.Equal(t, runCtx.Deployment.DeployManifestID, dCtx.Deployment.DeployManifestID)
	assert.Equal(t, runCtx.Deployment.ServiceMapsID, dCtx.Deployment.ServiceMapsID)
	assert.Equal(t, runCtx.Deployment.Branch, dCtx.Deployment.Branch)
	assert.Equal(t, runCtx.Deployment.Layer, dCtx.Deployment.Layer)

	assert.True(t, assertEqualIncludes(t, dCtx.Deployment.ID, runCtx.Deployment.ID))
}

func TestRevertTwice(t *testing.T) {

	test.RunUp(t)
	defer test.Down(t)
	T := newTest(t)
	makeManifest(t, testManifest, sName)
	makeMap(t, testMap, sName, false, false)

	ctx := context.Background()
	runC, runCtx, err := T.service.Run(ctx, paramsRun)
	require.NoError(t, err)
	T.producer.Assert(t, runCtx, model.Process)
	T.testNomad.Success(t, common.Run, "yandex_vertis_example_service_d", "", "1.0.0")
	<-runC
	T.producer.Assert(t, runCtx, model.Success)

	runC, runCtx, err = T.service.Run(ctx, paramsRunNext)
	require.NoError(t, err)
	T.producer.Assert(t, runCtx, model.Process)
	T.testNomad.Success(t, common.Run, "yandex_vertis_example_service_d", "", "1.0.1")
	<-runC
	T.producer.Assert(t, runCtx, model.Success)

	revertC, revertCtx, err := T.service.Revert(ctx, model.RevertParams{
		UUID:  uuid.New(),
		Layer: common.Test,
		Name:  "yandex_vertis_example_service_d",
		Login: "danevge",
	})
	require.NoError(t, err)
	T.producer.Assert(t, revertCtx, model.Process)
	T.testNomad.Success(t, common.Run, "yandex_vertis_example_service_d", "", "1.0.0")
	<-revertC
	T.producer.Assert(t, revertCtx, model.Success)

	_, _, err = T.service.Revert(ctx, model.RevertParams{
		UUID:  uuid.New(),
		Layer: common.Test,
		Name:  "yandex_vertis_example_service_d",
		Login: "danevge",
	})
	assert.Equal(t, err, processor.ErrAlreadyReverted)
}

func TestRevertStoppedService(t *testing.T) {

	test.RunUp(t)
	defer test.Down(t)
	T := newTest(t)
	makeManifest(t, testManifest, sName)
	makeMap(t, testMap, sName, false, false)

	ctx := context.Background()

	_, _, err := T.service.Revert(ctx, model.RevertParams{
		UUID:  uuid.New(),
		Layer: common.Test,
		Name:  "yandex_vertis_example_service_d",
		Login: "danevge",
	})
	assert.Equal(t, err, processor.ErrServiceNotDeployed)

	runC, runCtx, err := T.service.Run(ctx, paramsRun)
	require.NoError(t, err)
	T.producer.Assert(t, runCtx, model.Process)
	T.testNomad.Success(t, common.Run, "yandex_vertis_example_service_d", "", "1.0.0")
	<-runC
	T.producer.Assert(t, runCtx, model.Success)

	runC, runCtx, err = T.service.Stop(ctx, model.StopParams{
		UUID:    uuid.New(),
		Layer:   common.Test,
		Name:    "yandex_vertis_example_service_d",
		Login:   "danevge",
		Comment: comment,
		Source:  config.UnknownSource,
	})
	require.NoError(t, err)
	T.producer.Assert(t, runCtx, model.Process)
	T.testNomad.Success(t, common.Stop, "yandex_vertis_example_service_d", "", "")
	<-runC
	T.producer.Assert(t, runCtx, model.Success)

	_, _, err = T.service.Revert(ctx, model.RevertParams{
		UUID:  uuid.New(),
		Layer: common.Test,
		Name:  "yandex_vertis_example_service_d",
		Login: "danevge",
	})
	assert.Equal(t, err, processor.ErrServiceNotDeployed)
}

func TestRevertFirstVersion(t *testing.T) {

	test.RunUp(t)
	defer test.Down(t)
	T := newTest(t)
	makeManifest(t, testManifest, sName)
	makeMap(t, testMap, sName, false, false)

	ctx := context.Background()

	runC, runCtx, err := T.service.Run(ctx, paramsRun)
	require.NoError(t, err)
	T.producer.Assert(t, runCtx, model.Process)
	T.testNomad.Success(t, common.Run, "yandex_vertis_example_service_d", "", "1.0.0")
	<-runC
	T.producer.Assert(t, runCtx, model.Success)

	_, _, err = T.service.Revert(ctx, model.RevertParams{
		UUID:  uuid.New(),
		Layer: common.Test,
		Name:  "yandex_vertis_example_service_d",
		Login: "danevge",
	})
	assert.Equal(t, err, processor.ErrNowhereToRevert)

	runC, runCtx, err = T.service.Run(ctx, paramsRunNext)
	require.NoError(t, err)
	T.producer.Assert(t, runCtx, model.Process)
	T.testNomad.Success(t, common.Run, "yandex_vertis_example_service_d", "", "1.0.1")
	<-runC
	T.producer.Assert(t, runCtx, model.Success)

	stopC, stopCtx, err := T.service.Stop(ctx, model.StopParams{
		UUID:    uuid.New(),
		Layer:   common.Test,
		Name:    "yandex_vertis_example_service_d",
		Login:   "danevge",
		Comment: comment,
		Source:  config.UnknownSource,
	})
	require.NoError(t, err)
	T.producer.Assert(t, stopCtx, model.Process)
	T.testNomad.Success(t, common.Stop, "yandex_vertis_example_service_d", "", "")
	<-stopC
	T.producer.Assert(t, stopCtx, model.Success)

	runC, runCtx, err = T.service.Run(ctx, paramsRun)
	require.NoError(t, err)
	T.producer.Assert(t, runCtx, model.Process)
	T.testNomad.Success(t, common.Run, "yandex_vertis_example_service_d", "", "1.0.0")
	<-runC
	T.producer.Assert(t, runCtx, model.Success)

	_, _, err = T.service.Revert(ctx, model.RevertParams{
		UUID:  uuid.New(),
		Layer: common.Test,
		Name:  "yandex_vertis_example_service_d",
		Login: "danevge",
	})
	assert.Equal(t, err, processor.ErrNowhereToRevert)
}

func TestRevertRunRevert(t *testing.T) {

	test.RunUp(t)
	defer test.Down(t)
	T := newTest(t)
	makeManifest(t, testManifest, sName)
	makeMap(t, testMap, sName, false, false)

	ctx := context.Background()
	runC, runCtx, err := T.service.Run(ctx, paramsRun)
	require.NoError(t, err)
	assert.Nil(t, runCtx.Previous)
	T.producer.Assert(t, runCtx, model.Process)
	T.testNomad.Success(t, common.Run, "yandex_vertis_example_service_d", "", "1.0.0")
	<-runC
	T.producer.Assert(t, runCtx, model.Success)

	runC, runCtx1, err := T.service.Run(ctx, paramsRunNext)
	require.NoError(t, err)
	require.NotNil(t, runCtx1.Previous)
	require.Equal(t, runCtx.Deployment.ID, runCtx1.Previous.Deployment.ID)
	T.producer.Assert(t, runCtx1, model.Process)
	T.testNomad.Success(t, common.Run, "yandex_vertis_example_service_d", "", "1.0.1")
	<-runC
	T.producer.Assert(t, runCtx1, model.Success)

	revertC, revertCtx, err := T.service.Revert(ctx, model.RevertParams{
		UUID:  uuid.New(),
		Layer: common.Test,
		Name:  "yandex_vertis_example_service_d",
		Login: "danevge",
	})
	require.NoError(t, err)
	require.NotNil(t, revertCtx.Previous)
	require.Equal(t, runCtx1.Deployment.ID, revertCtx.Previous.Deployment.ID)
	T.producer.Assert(t, revertCtx, model.Process)
	T.testNomad.Success(t, common.Run, "yandex_vertis_example_service_d", "", "1.0.0")
	<-revertC
	T.producer.Assert(t, revertCtx, model.Success)

	assert.Equal(t, revertCtx.Deployment.Version, "1.0.0")

	runC, runCtx, err = T.service.Run(ctx, model.RunParams{
		Layer:   common.Test,
		Name:    "yandex_vertis_example_service_d",
		Version: "1.0.2",
		Login:   "danevge",
		Comment: "It is comment",
		Source:  config.UnknownSource,
	})
	require.NoError(t, err)
	require.NotNil(t, runCtx.Previous)
	require.Equal(t, revertCtx.Deployment.ID, runCtx.Previous.Deployment.ID)
	T.producer.Assert(t, runCtx, model.Process)
	T.testNomad.Success(t, common.Run, "yandex_vertis_example_service_d", "", "1.0.2")
	<-runC
	T.producer.Assert(t, runCtx, model.Success)

	revertC, revertCtx, err = T.service.Revert(ctx, model.RevertParams{
		UUID:  uuid.New(),
		Layer: common.Test,
		Name:  "yandex_vertis_example_service_d",
		Login: "danevge",
	})
	require.NoError(t, err)
	require.NotNil(t, revertCtx.Previous)
	require.Equal(t, runCtx.Deployment.ID, revertCtx.Previous.Deployment.ID)
	T.producer.Assert(t, revertCtx, model.Process)
	T.testNomad.Success(t, common.Run, "yandex_vertis_example_service_d", "", "1.0.0")
	<-revertC
	T.producer.Assert(t, revertCtx, model.Success)

	assert.Equal(t, revertCtx.Deployment.Version, "1.0.0")
}

func TestPromote(t *testing.T) {
	test.RunUp(t)
	defer test.Down(t)
	T := newTest(t)
	T.prepare(false, false, false)

	var (
		testSecrets = map[string]string{"k1": "sec-1:ver-1:test_env"}
		prodSecrets = map[string]string{"k1": "sec-1:ver-1:prod_env"}
	)

	T.ssCliMock.ExpectedCalls = nil
	prepareSecretEnvs(T, layer.Layer_TEST, testSecrets)
	prepareSecretEnvs(T, layer.Layer_PROD, prodSecrets)

	td := T.TestData()
	td.issues = []string{"VOID-102"}
	td.metadata = "first_run"
	runCtx := T.fullRun(td)
	assert.Nil(t, runCtx.Previous)
	require.Equal(t, testSecrets, runCtx.Manifest.Config.SecretParams)

	makeManifest(t, testManifest1, sName)

	promoteC, promoteCtx, err := T.service.Promote(td.ctx, model.PromoteParams{
		ID:     runCtx.Deployment.ID,
		Login:  td.login,
		Source: td.source,
	})
	require.NoError(t, err)
	assert.Equal(t, "first_run", promoteCtx.Deployment.UserMetadata)
	assert.Nil(t, promoteCtx.Previous)
	T.producer.Assert(t, promoteCtx, model.Process)
	T.prodNomad.Success(t, common.Run, td.name, td.branch, td.version)
	assertStateChan(t, scheduler.Success, model.Success, promoteC)
	T.producer.Assert(t, promoteCtx, model.Success)
	require.Equal(t, prodSecrets, promoteCtx.Manifest.Config.SecretParams)

	assert.Equal(t, common.Test, runCtx.Deployment.Layer)
	assert.Equal(t, model.Success, runCtx.Deployment.State)
	assert.False(t, runCtx.Deployment.Overridden)

	promoteCtx, err = T.ctxFactory.RestoreContext(promoteCtx.Deployment.ID)
	require.NoError(t, err)

	assert.Equal(t, common.Prod, promoteCtx.Deployment.Layer)
	assert.Equal(t, model.Success, promoteCtx.Deployment.State)

	assert.Equal(t, runCtx.Deployment.Version, promoteCtx.Deployment.Version)
	assert.Equal(t, runCtx.Deployment.AuthorID, promoteCtx.Deployment.AuthorID)
	assert.Equal(t, runCtx.Deployment.ServiceMapsID, promoteCtx.Deployment.ServiceMapsID)
	assert.Equal(t, runCtx.Deployment.DeployManifestID, promoteCtx.Deployment.DeployManifestID)
	assert.Equal(t, runCtx.Deployment.ID, promoteCtx.Deployment.ParentId)
	assert.Equal(t, runCtx.Deployment.Comment, promoteCtx.Deployment.Comment)
	assert.Equal(t, comment, runCtx.Deployment.Comment)
}

func TestPromoteWithIncludes(t *testing.T) {

	test.RunUp(t)
	defer test.Down(t)
	T := newTest(t)
	makeIncludes(t, commonYml, "/shiva/common.yml")
	makeIncludes(t, testYml, "/shiva/test.yml")
	makeIncludes(t, testYml, "/shiva/prod.yml")
	makeManifest(t, testManifestWithIncludes1, t.Name())
	makeMap(t, testMap, t.Name(), false, false)

	td := T.TestData()
	runCtx := T.fullRun(td)
	assert.Nil(t, runCtx.Previous)

	makeIncludes(t, testYml1, "/shiva/test.yml")

	promoteC, promoteCtx, err := T.service.Promote(td.ctx, model.PromoteParams{
		ID:     runCtx.Deployment.ID,
		Login:  "danevge",
		Source: td.source,
	})
	require.NoError(t, err)
	assert.Nil(t, promoteCtx.Previous)
	T.producer.Assert(t, promoteCtx, model.Process)

	T.prodNomad.Success(t, common.Run, td.name, td.branch, td.version)
	assertStateChan(t, scheduler.Success, model.Success, promoteC)
	T.producer.Assert(t, promoteCtx, model.Success)

	assert.Equal(t, common.Test, runCtx.Deployment.Layer)
	assert.Equal(t, model.Success, runCtx.Deployment.State)
	assert.False(t, runCtx.Deployment.Overridden)

	promoteCtx, err = T.ctxFactory.RestoreContext(promoteCtx.Deployment.ID)
	require.NoError(t, err)

	assert.Equal(t, common.Prod, promoteCtx.Deployment.Layer)
	assert.Equal(t, model.Success, promoteCtx.Deployment.State)

	assert.Equal(t, runCtx.Deployment.Version, promoteCtx.Deployment.Version)
	assert.Equal(t, runCtx.Deployment.AuthorID, promoteCtx.Deployment.AuthorID)
	assert.Equal(t, runCtx.Deployment.ServiceMapsID, promoteCtx.Deployment.ServiceMapsID)
	assert.Equal(t, runCtx.Deployment.DeployManifestID, promoteCtx.Deployment.DeployManifestID)
	assert.Equal(t, runCtx.Deployment.ID, promoteCtx.Deployment.ParentId)
	assert.Equal(t, runCtx.Deployment.Comment, promoteCtx.Deployment.Comment)
	assert.Equal(t, comment, runCtx.Deployment.Comment)
	includeLinksStorage := include_links.NewStorage(test_db.NewDb(t), test.NewLogger(t))
	runLinks, err := includeLinksStorage.GetByDeploymentId(runCtx.Deployment.ID)
	require.NoError(t, err)
	promoteLinks, err := includeLinksStorage.GetByDeploymentId(promoteCtx.Deployment.ID)
	require.NoError(t, err)

	assert.Len(t, runLinks, 2)
	assert.Len(t, promoteLinks, 2)

	incService := include.NewService(test_db.NewDb(t), test.NewLogger(t))

	runLinksMap := map[string]int64{}
	for _, link := range runLinks {
		inc, err := incService.Get(link.IncludeId)
		require.NoError(t, err)
		runLinksMap[inc.Path] = link.IncludeId
	}

	promoteLinksMap := map[string]int64{}
	for _, link := range promoteLinks {
		inc, err := incService.Get(link.IncludeId)
		require.NoError(t, err)
		promoteLinksMap[inc.Path] = link.IncludeId
	}

	assert.Contains(t, runLinksMap, "shiva/common.yml")
	assert.Contains(t, promoteLinksMap, "shiva/common.yml")
	assert.Equal(t, runLinksMap["shiva/common.yml"], promoteLinksMap["shiva/common.yml"])

	assert.Contains(t, runLinksMap, "shiva/test.yml")
	assert.Contains(t, promoteLinksMap, "shiva/prod.yml")
}

func TestValidatePromote(t *testing.T) {

	var (
		name    = t.Name()
		nameSoX = t.Name() + "_sox"
	)

	test.RunUp(t)
	defer test.Down(t)
	T := newTest(t)
	makeManifest(t, testManifest, nameSoX)
	makeMap(t, testMap, nameSoX, true, false)
	makeManifest(t, testManifest, name)
	makeMap(t, testMap, name, false, false)

	ctx := context.Background()
	t.Run("Branch", func(t *testing.T) {
		c, dctx, err := T.service.Run(ctx, model.RunParams{
			Layer:   common.Test,
			Name:    name,
			Version: "1.0.1",
			Branch:  "branch",
			Login:   "danevge",
			Comment: "It is comment",
			Source:  config.UnknownSource,
		})
		require.NoError(t, err)
		T.testNomad.Success(t, common.Run, name, "branch", "1.0.1")
		<-c
		T.producer.Reset()
		_, _, err = T.service.Promote(ctx, model.PromoteParams{
			ID:    dctx.Deployment.ID,
			Login: "danevge",
		})
		assert.Equal(t, processor.ErrPromoteNotBranch, err)
		T.producer.AssertEmpty(t)
	})
	t.Run("Layer", func(t *testing.T) {
		c, dctx, err := T.service.Run(ctx, model.RunParams{
			Layer:   common.Prod,
			Name:    name,
			Version: "1.0.2",
			Login:   "danevge",
			Comment: comment,
			Source:  config.UnknownSource,
		})
		require.NoError(t, err)
		T.prodNomad.Success(t, common.Run, name, "", "1.0.2")
		<-c
		T.producer.Reset()
		_, _, err = T.service.Promote(ctx, model.PromoteParams{
			ID:    dctx.Deployment.ID,
			Login: "danevge",
		})
		assert.Equal(t, processor.ErrPromoteProhibited, err)
		T.producer.AssertEmpty(t)
	})
	t.Run("State", func(t *testing.T) {
		_, dctx, err := T.service.Run(ctx, model.RunParams{
			Layer:   common.Test,
			Name:    name,
			Version: "1.0.3",
			Login:   "danevge",
			Comment: "It is comment",
			Source:  config.UnknownSource,
		})
		require.NoError(t, err)
		T.producer.Reset()
		_, _, err = T.service.Promote(ctx, model.PromoteParams{
			ID:    dctx.Deployment.ID,
			Login: "danevge",
		})
		assert.Equal(t, processor.ErrPromoteNotSuccess, err)
		T.producer.AssertEmpty(t)
	})
	t.Run("SoX", func(t *testing.T) {
		c, dctx, err := T.service.Run(ctx, model.RunParams{
			Layer:   common.Test,
			Name:    nameSoX,
			Version: "1.0.4",
			Login:   "danevge",
			Comment: comment,
			Source:  config.UnknownSource,
		})
		require.NoError(t, err)
		T.testNomad.Success(t, common.Run, nameSoX, "", "1.0.4")
		<-c
		T.producer.Reset()
		_, promoteCtx, err := T.service.Promote(ctx, model.PromoteParams{
			ID:    dctx.Deployment.ID,
			Login: "danevge",
		})
		assert.Equal(t, processor.ErrSoxIssueNotFound, err)
		T.producer.Assert(t, promoteCtx, model.ValidateFail)
	})
}

func TestDuplicateWaitApprove(t *testing.T) {

	test.RunUp(t)
	defer test.Down(t)
	T := newTest(t)
	T.prepare(false, true, false)
	td := T.TestData()
	T.fullRun(td)
	td.layer = common.Prod
	td.issues = []string{"VOID-102"}
	T.fullRunSox(td)
	_, runCtx, err := T.run(td)
	assert.Equal(t, err, deployment.ErrAlreadyInWaitApprove)
	assertDState(t, model.ValidateFail, runCtx.Deployment.State)
}

func TestStateEnded(t *testing.T) {
	test.RunUp(t)
	defer test.Down(t)
	T := newTest(t)
	T.prepare(false, true, false)
	td := T.TestData()
	runCtx := T.fullRun(td)

	stateC, stateCtx, err := T.service.State(td.ctx, runCtx.Deployment.ID)
	require.NoError(t, err)
	assert.Nil(t, stateC)
	assert.Equal(t, stateCtx.Description, fmt.Sprintf("Deployment was Success %s", stateCtx.Deployment.UpdatedAt))
}

func TestInitProcess(t *testing.T) {

	// prepare
	type process struct {
		name string
		ctx  *dcontext.Context
		c    chan *deployment.StateChange
	}

	const (
		layer      = common.Test
		baseName   = "yandex_vertis_example_service_"
		branchName = ""
		version    = "1.0.0"
	)

	test.RunUp(t)
	defer test.Down(t)
	T := newTest(t)
	var ps []process
	for i := 0; i < 5; i++ {
		td := T.TestData()
		name := baseName + strconv.Itoa(i)
		makeManifest(t, testManifest, name)
		makeMap(t, testMap, name, false, false)
		td.name = name
		c, runCtx, err := T.run(td)
		require.NoError(t, err)
		ps = append(ps, process{
			name: name,
			ctx:  runCtx,
			c:    c,
		})
	}
	T.testNomad.Success(t, common.Run, ps[0].name, "", version)
	assertStateChan(t, scheduler.Success, model.Success, ps[0].c)
	T.assertUpdatedState(model.Success, ps[0].ctx)

	// test
	T2 := newTest(t)
	T2.lock.NotControl[strings.Join([]string{layer.String(), ps[1].name, branchName}, "_")] = common.Run
	T2.lock.NotControl[strings.Join([]string{layer.String(), ps[3].name, branchName}, "_")] = common.Run
	go T2.service.GlobalRestore()
	T2.lock.Wait(t, T2.lock.LockIncChan, 4)
	T2.testNomad.Success(t, common.Run, ps[2].name, "", fmt.Sprintf("id:%d", ps[2].ctx.Deployment.NomadID))
	T2.testNomad.Success(t, common.Run, ps[4].name, "", fmt.Sprintf("id:%d", ps[4].ctx.Deployment.NomadID))
	T2.lock.Wait(t, T2.lock.UnlockIncChan, 2)

	// asserts
	T.assertUpdatedState(model.Success, ps[0].ctx)
	T.assertUpdatedState(model.Process, ps[1].ctx)
	T.assertUpdatedState(model.Success, ps[2].ctx)
	T.assertUpdatedState(model.Process, ps[3].ctx)
	T.assertUpdatedState(model.Success, ps[4].ctx)

	assert.Equal(t, 5, len(T.testNomad.RunCtxs))
	assert.Equal(t, 0, len(T2.testNomad.RunCtxs))
	assert.Equal(t, 2, len(T2.testNomad.StateCtxs))
	assert.Equal(t, 2, T2.lock.LockInc)
	assert.Equal(t, 2, T2.lock.LockFailInc)
	assert.Equal(t, 2, T2.lock.UnlockInc)
}

func TestReconnectProcess(t *testing.T) {

	// prepare
	type process struct {
		td  *TestData
		ctx *dcontext.Context
		c   chan *deployment.StateChange
	}
	baseName := t.Name()
	test.RunUp(t)
	defer test.Down(t)
	T := newTest(t)
	var ps []process
	for i := 1; i < 6; i++ {
		td := T.TestData()
		name := baseName + strconv.Itoa(i)
		makeManifest(t, testManifest, name)
		makeMap(t, testMap, name, false, false)
		td.name = name
		c, dctx, err := T.run(td)
		require.NoError(t, err)
		ps = append(ps, process{
			td:  td,
			ctx: dctx,
			c:   c,
		})
	}

	T.lock.Wait(t, T.lock.LockIncChan, 5)
	T.lock.RemoveLocksAndReconnect()
	for _, p := range ps {
		T.sch(p.td).Process(t, common.Run, p.td.name, p.td.branch, p.td.version)
		assertStateChan(t, scheduler.Process, model.Process, p.c)
	}
	T.lock.Wait(t, T.lock.LockIncChan, 5)

	for _, p := range ps {
		T.testNomad.Success(t, common.Run, p.td.name, p.td.branch, p.td.version)
		assertStateChan(t, scheduler.Success, model.Success, p.c)
		assertDState(t, model.Process, p.ctx.Deployment.State)
	}
	for _, p := range ps {
		T.assertUpdatedState(model.Process, p.ctx)
	}
	for _, p := range ps {
		T.testNomad.Success(t, common.Run, p.td.name, p.td.branch, fmt.Sprintf("id:%d", p.ctx.Deployment.NomadID))
	}
	T.lock.Wait(t, T.lock.UnlockIncChan, 5)
	for _, p := range ps {
		T.assertUpdatedState(model.Success, p.ctx)
	}
}

func TestInitWithLockWait(t *testing.T) {

	// prepare
	test.RunUp(t)
	defer test.Down(t)
	T := newTest(t)
	T.prepare(false, false, false)
	td := T.TestData()
	_, runCtx, err := T.run(td)
	require.NoError(t, err)

	// test
	T2 := newTest(t)
	require.NoError(t, T2.lock.AddNotControl(runCtx.LockContext()))
	T2.service.GlobalRestore()
	T2.lock.Wait(t, T2.lock.LockIncChan, 1)
	assert.Equal(t, T2.lock.LockFailInc, 1)
	go require.NoError(t, T2.lock.DeleteNotControl(runCtx.LockContext()))
	T2.lock.Wait(t, T2.lock.LockIncChan, 1)
	T2.testNomad.Success(t, common.Run, td.name, td.branch, fmt.Sprintf("id:%d", runCtx.Deployment.NomadID))
	T2.lock.Wait(t, T2.lock.UnlockIncChan, 1)

	T2.assertUpdatedState(model.Success, runCtx)
	assert.Equal(t, 1, T2.lock.LockInc)
	assert.Equal(t, 1, T2.lock.LockFailInc)
	assert.Equal(t, 1, T2.lock.UnlockInc)
}

func TestAutoResizePromoteNewJobByDrills(t *testing.T) {
	test.RunUp(t)
	defer test.Down(t)
	T := newTest(t)
	T.prepare(false, false, false)
	td := T.TestData()
	runCtx := T.fullRun(td)
	T.DrillsEnable(layer.Layer_PROD, "sas")

	promoteC, promoteCtx, err := T.service.Promote(td.ctx, model.PromoteParams{
		ID:    runCtx.Deployment.ID,
		Login: td.login,
	})
	T.prodNomad.Success(t, common.Run, td.name, td.branch, td.version)
	require.NoError(t, err)
	assert.Nil(t, promoteCtx.Previous)
	assert.Equal(t, 0, len(T.prodNomad.UpdateCtxs))
	assert.Equal(t, 1, len(T.prodNomad.RunCtxs))
	assertStateChan(t, scheduler.Success, model.Success, promoteC)
	T.assertUpdatedState(model.Success, promoteCtx)
}

func TestAutoResizePromoteRepeatedByDrills(t *testing.T) {
	test.RunUp(t)
	defer test.Down(t)
	T := newTest(t)
	T.prepare(false, false, false)
	tdProd := T.TestData().Prod().Version(sVersion0)
	tdTest := T.TestData()
	T.DrillsEnable(layer.Layer_PROD, "sas")

	runCtx := T.fullRun(tdProd)
	assert.Equal(t, 0, len(T.testNomad.UpdateCtxs))
	T.assertUpdatedState(model.Success, runCtx)

	runCtx = T.fullRun(tdTest)
	assert.Equal(t, 0, len(T.testNomad.UpdateCtxs))
	T.assertUpdatedState(model.Success, runCtx)

	go T.prodNomad.Success(t, common.Update, tdTest.name, tdTest.branch, tdTest.version) //success inner resize process
	promoteC, promoteCtx, err := T.service.Promote(tdTest.ctx, model.PromoteParams{
		ID:     runCtx.Deployment.ID,
		Login:  tdTest.login,
		Source: tdTest.source,
	})
	T.prodNomad.Success(t, common.Run, tdTest.name, tdTest.branch, tdTest.version)
	require.NoError(t, err)
	assert.NotNil(t, promoteCtx.Previous)
	assert.Equal(t, 1, len(T.prodNomad.UpdateCtxs))
	assert.Equal(t, 1, len(T.prodNomad.RunCtxs))
	assertStateChan(t, scheduler.Success, model.Success, promoteC)
	T.assertUpdatedState(model.Success, promoteCtx)
}

func TestAutoResizeRevertByDrills(t *testing.T) {
	test.RunUp(t)
	defer test.Down(t)
	T := newTest(t)
	T.prepare(false, false, false)
	td := T.TestData()
	T.fullRun(td)
	td2 := T.TestData()
	td2.version = sVersion2
	T.fullRun(td2)
	T.DrillsEnable(layer.Layer_TEST, "sas")
	go T.testNomad.Success(t, common.Update, td.name, td.branch, td.version) //success inner resize process
	state, ctx, err := T.revert(td)
	require.NoError(t, err)
	T.testNomad.Success(t, common.Run, td.name, td.branch, td.version)
	assertStateChan(t, scheduler.Success, model.Success, state)
	assert.Equal(t, 1, len(T.testNomad.UpdateCtxs))
	assert.Equal(t, 1, len(T.testNomad.RunCtxs))
	T.assertUpdatedState(model.Success, ctx)
}

func TestAutoResizeRuntByDrillsAndEmptyCPU(t *testing.T) {
	test.RunUp(t)
	defer test.Down(t)
	T := newTest(t)
	T.prepare(false, false, false)
	// delete manual cpu value(use default auto_cpu)
	makeManifest(t, testManifest1, T.t.Name())
	td := T.TestData()
	T.fullRun(td)

	T.DrillsEnable(layer.Layer_TEST, "sas")
	td.version = sVersion2
	go T.testNomad.Success(t, common.Update, td.name, td.branch, td.version) //success inner resize process
	runCtx := T.fullRun(td)

	assert.Equal(t, config.Int("SCALER_MAX_CPU"), runCtx.OverrideCPU)
	assert.Equal(t, 1, len(T.testNomad.UpdateCtxs))
	T.assertUpdatedState(model.Success, runCtx)
}

func TestAutoResizeRestartByDrills2(t *testing.T) {
	test.RunUp(t)
	defer test.Down(t)
	T := newTest(t)
	T.prepare(false, false, false)
	td := T.TestData()
	T.fullRun(td)
	assert.Equal(t, 0, len(T.testNomad.UpdateCtxs))
	T.DrillsEnable(layer.Layer_TEST, "sas")
	go T.testNomad.Success(t, common.Update, td.name, td.branch, td.version) //success inner resize process
	state, ctx, err := T.restart(td)
	require.NoError(t, err)
	T.testNomad.Success(t, common.Restart, td.name, td.branch, "")
	assertStateChan(t, scheduler.Success, model.Success, state)
	assert.Equal(t, 1, len(T.testNomad.UpdateCtxs))
	assert.Equal(t, 1, len(T.testNomad.RestartCtxs))
	T.assertUpdatedState(model.Success, ctx)
}

func TestAutoResizeRunNewJobByDrills(t *testing.T) {
	test.RunUp(t)
	defer test.Down(t)
	T := newTest(t)
	T.prepare(false, false, false)
	td := T.TestData()
	T.DrillsEnable(layer.Layer_TEST, "sas")
	runCtx := T.fullRun(td)
	assert.Equal(t, 0, len(T.testNomad.UpdateCtxs))
	T.assertUpdatedState(model.Success, runCtx)
}

func TestNoServiceInstancesByDrills(t *testing.T) {
	test.RunUp(t)
	defer test.Down(t)
	T := newTest(t)
	T.prepare(false, false, false)
	td := T.TestData()

	runCtx := T.fullRun(td)
	T.assertUpdatedState(model.Success, runCtx)

	T.DrillsEnable(layer.Layer_TEST, "sas")
	T.DrillsEnable(layer.Layer_TEST, "myt")

	_, _, err := T.update(td)
	require.ErrorIs(t, err, processor.ErrResizeOffAll)
}

func TestUpdateSoxWithoutIssue(t *testing.T) {
	test.RunUp(t)
	defer test.Down(t)
	T := newTest(t)
	T.prepare(false, true, false)
	td := T.TestData()
	td.issues = []string{"VOID-102"}
	runCtx := T.fullRun(td)
	T.assertUpdatedState(model.Success, runCtx)

	_, dctx, err := T.update(td)
	require.NoError(t, err)
	require.Nil(t, dctx.Issues)
}

func TestAutoResizeRunRepeatedlyByDrills(t *testing.T) {
	test.RunUp(t)
	defer test.Down(t)
	T := newTest(t)
	T.prepare(false, false, false)
	td := T.TestData()
	T.DrillsEnable(layer.Layer_TEST, "sas")

	runCtx := T.fullRun(td)
	assert.Equal(t, 0, len(T.testNomad.UpdateCtxs))
	T.assertUpdatedState(model.Success, runCtx)

	td.version = sVersion2
	go T.testNomad.Success(t, common.Update, td.name, td.branch, td.version) //success inner resize process
	runCtx = T.fullRun(td)
	assert.Equal(t, 1, len(T.testNomad.UpdateCtxs))
	T.assertUpdatedState(model.Success, runCtx)
}

func TestRestoreCancelWithRetry(t *testing.T) {

	// prepare
	test.RunUp(t)
	defer test.Down(t)
	T := newTest(t)
	T.prepare(false, false, false)
	td := T.TestData()
	_, runCtx, err := T.run(td)
	require.NoError(t, err)
	_, cancelCtx, err := T.cancel(td, runCtx)
	require.NoError(t, err)

	// test
	T2 := newTest(t)
	require.NoError(t, T2.lock.AddNotControl(cancelCtx.LockContext()))
	go T2.service.GlobalRestore()
	T2.lock.Wait(t, T2.lock.StealIncChan, 1)
	assert.Equal(t, T2.lock.LockFailInc, 1)
	go require.NoError(t, T2.lock.DeleteNotControl(cancelCtx.LockContext()))
	T2.lock.Wait(t, T2.lock.StealIncChan, 1)
	assert.Equal(t, T2.lock.LockInc, 1)
	T2.testNomad.Success(t, common.Run, td.name, td.branch, fmt.Sprintf("id:%d", cancelCtx.Deployment.NomadID))
	T2.lock.Wait(t, T2.lock.UnlockIncChan, 1)

	T2.assertUpdatedState(model.Canceled, runCtx)
	T2.assertUpdatedState(model.Success, cancelCtx)
}

func TestRestoreByExpired(t *testing.T) {
	test.RunUp(t)
	T := newTest(t)
	T.prepare(false, false, false)
	td := T.TestData()
	sch := T.sch(td)
	runC, runCtx, err := T.run(td)
	require.NoError(t, err)
	sch.Process(t, common.Run, td.name, td.branch, td.version)
	assertStateChan(t, scheduler.Process, model.Process, runC)
	T.lock.RemoveLocksAndReconnect()
	sch.Process(t, common.Run, td.name, td.branch, td.version)
	assertStateChan(t, scheduler.Process, model.Process, runC)
	sch.Success(t, common.Run, td.name, td.branch, td.version)
	sch.Success(t, common.Run, td.name, td.branch, fmt.Sprintf("id:%d", runCtx.Deployment.NomadID))
	assertStateChan(t, scheduler.Success, model.Success, runC)
	T.assertUpdatedState(model.Success, runCtx)
	T.producer.Assert(t, runCtx, model.Process)
	T.producer.Assert(t, runCtx, model.Success)
	T.producer.AssertEmpty(t)
	assert.Equal(t, 1, len(sch.RunCtxs))
	assert.Equal(t, 1, len(sch.StateCtxs))
	assert.Equal(t, 2, T.lock.LockInc)
	assert.Equal(t, 1, T.lock.UnlockInc)
	T.assertUpdatedState(model.Success, runCtx)

	d := T.assertUpdatedState(model.Success, runCtx)
	assert.NotNil(t, d.Status)
}

func TestRestoreCanceledProcess(t *testing.T) {
	test.RunUp(t)
	T := newTest(t)
	T.prepare(false, false, false)
	td := T.TestData()
	sch := T.sch(td)
	runC, runCtx, err := T.run(td)
	require.NoError(t, err)
	sch.Process(t, common.Run, td.name, td.branch, td.version)
	assertStateChan(t, scheduler.Process, model.Process, runC)

	d := T.assertUpdatedState(model.Process, runCtx)
	d.State = model.Canceled
	require.NoError(t, T.storage.Save(d))

	T.lock.RemoveLocksAndReconnect()
	sch.Process(t, common.Run, td.name, td.branch, td.version)
	assertStateChan(t, scheduler.Process, model.Process, runC)

	require.Never(t, func() bool {
		return T.lock.LockInc > 1 && T.lock.LockFailInc > 0
	}, time.Second*5, time.Second/2)

	d, err = T.storage.Get(d.ID)
	require.NoError(t, err)
	require.Equal(t, model.Canceled, d.State)
}

func TestService_Run(t *testing.T) {
	var err error
	test.RunUp(t)
	T := newTest(t)
	T.registry.EnableTestify = true
	T.prepare(false, false, false)
	td := T.TestData()
	td.branch = "b1"
	td.metadata = "metadata"
	T.registry.On("CheckImageExists", td.name+"-image", td.version).Return(nil)
	runCtx := T.fullRun(td)
	T.registry.AssertCalled(t, "CheckImageExists", td.name+"-image", td.version)
	assert.Equal(t, 0, len(T.testNomad.UpdateCtxs))
	assert.Equal(t, td.branch, runCtx.Deployment.Branch)
	assert.Equal(t, td.version, runCtx.Deployment.Version)
	assert.Equal(t, td.comment, runCtx.Deployment.Comment)
	assert.Equal(t, "metadata", runCtx.Deployment.UserMetadata)
	assert.NotNil(t, runCtx.Deployment.Status)
	d := T.assertUpdatedState(model.Success, runCtx)
	assert.NotNil(t, d.Status)

	_, _, err = T.run(td)
	assert.NotNil(t, err)
	assert.Equal(t, processor.ErrNothingChanged, err)
}

func TestService_Run_BranchCase(t *testing.T) {
	var err error
	test.RunUp(t)
	T := newTest(t)
	T.registry.EnableTestify = true
	T.prepare(false, false, false)
	td := T.TestData()
	td.branch = "SoMe-StuFf"
	T.registry.On("CheckImageExists", td.name+"-image", td.version).Return(nil)
	runCtx := T.fullRun(td)
	T.registry.AssertCalled(t, "CheckImageExists", td.name+"-image", td.version)
	assert.Equal(t, 0, len(T.testNomad.UpdateCtxs))
	assert.Equal(t, td.branch, runCtx.Deployment.Branch)
	assert.Equal(t, td.version, runCtx.Deployment.Version)
	assert.Equal(t, td.comment, runCtx.Deployment.Comment)
	assert.NotNil(t, runCtx.Deployment.Status)
	d := T.assertUpdatedState(model.Success, runCtx)
	assert.NotNil(t, d.Status)

	td.branch = "some-stuff"
	_, _, err = T.run(td)
	assert.Equal(t, processor.ErrExistBranchDeploy, err)
}

func TestService_RerunWithTraffic(t *testing.T) {
	test.RunUp(t)
	T := newTest(t)
	T.registry.EnableTestify = true
	T.prepare(false, false, false)
	td := T.TestData()
	td.layer = common.Prod
	td.branch = "traffic-branch"
	T.registry.On("CheckImageExists", td.name+"-image", td.version).Return(nil)
	runCtx := T.fullRun(td)
	d := T.assertUpdatedState(model.Success, runCtx)
	assert.NotNil(t, d.Status)

	td.trafficShare = true
	runCtx = T.fullRun(td)
	d = T.assertUpdatedState(model.Success, runCtx)
	assert.NotNil(t, d.Status)
}

func TestService_DoubleRun(t *testing.T) {
	var err error
	test.RunUp(t)
	T := newTest(t)
	T.prepare(false, false, false)
	td := T.TestData()
	td.branch = "b1"
	_, _, err = T.run(td)
	require.NoError(t, err)

	_, runCtx, err := T.run(td)
	assert.Equal(t, err, deployment.ErrAnotherDeployInProgress)
	assertDState(t, model.ValidateFail, runCtx.Deployment.State)
}

func TestService_BulkRunActualConfiguration(t *testing.T) {
	// prepare
	var err error
	test.RunUp(t)
	T := newTest(t)
	T.prepare(false, false, false)
	td := T.TestData()
	td.branch = "b1"
	makeIncludes(t, commonYml, "shiva/common.yml")
	setSecretEnvs(T)
	td.confOverride = []string{"shiva/common.yml"}
	td.envOverride = map[string]string{"key1": "value1"}
	td.issues = []string{"VOID-256"}
	parentCtx := T.fullRun(td)
	d := parentCtx.Deployment

	// update user configuration
	makeManifest(t, testManifest1, T.t.Name())
	makeMap(T.t, testMap1, T.t.Name(), false, false)
	makeIncludes(t, commonYml1, "shiva/common.yml")
	updateSecretEnvs(T)

	// test
	_, ctx, err := T.service.Bulk(context.TODO(), model.BulkParams{
		Layer:        d.Layer,
		Name:         d.Name,
		Branch:       d.Branch,
		Login:        "danevge",
		ActualConfig: true,
	})
	require.NoError(t, err)

	// asserts
	assert.Equal(t, ctx.Deployment.ParentId, int64(0))
	assert.Equal(t, ctx.Deployment.PreviousId, parentCtx.Deployment.ID)
	assert.NotEqual(t, ctx.Deployment.ServiceMapsID, parentCtx.Deployment.ServiceMapsID)
	assert.NotEqual(t, ctx.Deployment.DeployManifestID, parentCtx.Deployment.DeployManifestID)
	assert.Empty(t, ctx.Issues)
	assert.Len(t, ctx.Manifest.Config.OverrideParams, 1)
	assert.Len(t, ctx.Manifest.Config.OverrideFiles, 1)
	assert.Equal(t, "value1", ctx.Manifest.Config.OverrideParams["key1"])
	assert.Equal(t, "shiva/common.yml", ctx.Manifest.Config.OverrideFiles[0].Path)
	assert.Equal(t, "common param 1", ctx.Manifest.Config.OverrideFiles[0].Value["COMMON_PARAM"])
	assert.Equal(t, secretEnvsNew, ctx.Manifest.Config.SecretParams)
}

func TestService_BulkRunParentConfiguration(t *testing.T) {
	//prepare
	test.RunUp(t)
	T := newTest(t)
	T.prepare(false, false, false)
	td := T.TestData()
	td.branch = "b1"
	makeIncludes(t, commonYml, "shiva/common.yml")
	setSecretEnvs(T)
	td.confOverride = []string{"shiva/common.yml"}
	td.envOverride = map[string]string{"key1": "value1"}
	td.issues = []string{"VOID-256"}
	parentCtx := T.fullRun(td)
	d := parentCtx.Deployment

	// update user configuration
	makeManifest(t, testManifest1, T.t.Name())
	makeMap(T.t, testMap1, T.t.Name(), false, false)
	makeIncludes(t, commonYml1, "shiva/common.yml")
	generateEnv(t, T.t.Name(), "testEnv", "testVal")
	updateSecretEnvs(T)

	// test
	_, ctx, err := T.service.Bulk(context.TODO(), model.BulkParams{
		Layer:        d.Layer,
		Name:         d.Name,
		Branch:       d.Branch,
		Login:        "danevge",
		ActualConfig: false,
	})
	require.NoError(t, err)

	// asserts
	assert.Equal(t, ctx.Deployment.ParentId, parentCtx.Deployment.ID)
	assert.Equal(t, ctx.Deployment.PreviousId, parentCtx.Deployment.ID)
	assert.Equal(t, ctx.Deployment.ServiceMapsID, parentCtx.Deployment.ServiceMapsID)
	assert.Equal(t, ctx.Deployment.DeployManifestID, parentCtx.Deployment.DeployManifestID)
	assert.Equal(t, parentCtx.Issues, ctx.Issues)
	assert.Len(t, ctx.Manifest.Config.OverrideParams, 1)
	assert.Len(t, ctx.Manifest.Config.OverrideFiles, 1)
	assert.Equal(t, "value1", ctx.Manifest.Config.OverrideParams["key1"])
	assert.Equal(t, "shiva/common.yml", ctx.Manifest.Config.OverrideFiles[0].Path)
	assert.Equal(t, "common param", ctx.Manifest.Config.OverrideFiles[0].Value["COMMON_PARAM"])
	assert.Equal(t, "testVal", ctx.Manifest.Config.GeneratedParams["testEnv"])
	assert.Equal(t, secretEnvs, ctx.Manifest.Config.SecretParams)
}

func TestService_RunNotControl(t *testing.T) {
	var err error
	test.RunUp(t)
	T := newTest(t)
	T.prepare(false, false, false)
	td := T.TestData()
	td.branch = "b1"
	ctx := T.fullRun(td)
	require.NoError(t, T.lock.AddNotControl(ctx.LockContext()))

	td.version = "2"
	_, _, err = T.run(td)
	assert.Equal(t, err, deployment.ErrAnotherDeployInProgress)
}

func TestService_RunDuringCancel(t *testing.T) {
	var err error
	test.RunUp(t)
	T := newTest(t)
	T.prepare(false, false, false)
	td := T.TestData()
	td.branch = "b1"
	_, ctx, err := T.run(td)
	require.NoError(t, err)
	_, _, err = T.cancel(td, ctx)
	require.NoError(t, err)

	_, _, err = T.run(td)
	assert.Equal(t, err, deployment.ErrAnotherDeployInProgress)
}

func TestRunWithEqualsBranch(t *testing.T) {
	test.RunUp(t)
	T := newTest(t)
	T.prepare(false, false, false)
	td := T.TestData()
	td.branch = "b1"
	T.fullRun(td)
	td.branch = ""
	T.fullRun(td)
}

func TestErrSyncTimeout(t *testing.T) {
	test.RunUp(t)
	defer test.Down(t)
	T := newTest(t)
	require.NoError(t, T.syncS.SetLastUpdate(time.Now().Add(-time.Hour)))
	T.prepare(false, false, false)
	_, _, err := T.run(T.TestData())
	assert.Equal(t, deployment.ErrSyncTimeout, err)
}

func TestValidateGithubSyncOffFlag(t *testing.T) {
	test.RunUp(t)
	defer test.Down(t)
	T := newTest(t)
	require.NoError(t, T.syncS.SetLastUpdate(time.Now().Add(-time.Hour)))
	T.prepare(false, false, false)
	require.NoError(t, T.featureFlag.Set(&feature_flags.FeatureFlag{
		Flag:  feature_flags.ValidateGithubSyncOff.String(),
		Value: true,
	}))
	T.fullRun(T.TestData())
}

func TestService_RunWithConfOverride(t *testing.T) {
	var err error
	test.RunUp(t)
	defer test.Down(t)

	makeManifest(t, testManifestEnv, t.Name())
	makeMap(t, testMap, t.Name(), false, false)
	makeMap(t, testMap, "shiva", false, false)

	T := newTest(t)
	td := T.TestData()
	td.confOverride = []string{}
	td.envOverride = map[string]string{
		"param1":         "p1",
		"test_param1":    "t2",
		"test_param2":    "t3",
		"secret_param2":  "${sec-10:ver-12345:abc}",
		"secret_param":   "s1",
		"template_param": "${url:shiva:deploy}",
	}
	td.branch = "b1"

	runC, runCtx, err := T.run(td)
	require.NoError(t, err)

	envs, err := runCtx.Manifest.Config.GetEnvs()
	require.NoError(t, err)

	assert.Len(t, envs, 7)
	assert.Equal(t, "p1", envs["param1"])
	assert.Equal(t, "g1", envs["general_param1"])
	assert.Equal(t, "t2", envs["test_param1"])
	assert.Equal(t, "t3", envs["test_param2"])
	assert.Equal(t, "s1", envs["secret_param"])
	assert.Equal(t, "${sec-10:ver-12345:abc}", envs["secret_param2"])
	assert.Equal(t, "${url:shiva:deploy}", envs["template_param"])

	T.producer.Assert(t, runCtx, model.Process)
	assert.Equal(t, runCtx.Deployment.State, model.Process)

	T.testNomad.Success(t, common.Run, td.name, td.branch, td.version)
	assertStateChan(t, scheduler.Success, model.Success, runC)
	T.producer.Assert(t, runCtx, model.Success)
	assert.Equal(t, 1, len(T.testNomad.RunCtxs))
	assert.Equal(t, td.branch, runCtx.Deployment.Branch)
	assert.Equal(t, td.version, runCtx.Deployment.Version)
	assert.Equal(t, comment, runCtx.Deployment.Comment)

	td.layer = common.Prod
	runC, runCtx, err = T.run(td)
	require.NoError(t, err)

	envs, err = runCtx.Manifest.Config.GetEnvs()
	require.NoError(t, err)

	assert.Len(t, envs, 8)
	assert.Equal(t, "p1", envs["param1"])
	assert.Equal(t, "g1", envs["general_param1"])
	assert.Equal(t, "t2", envs["test_param1"])
	assert.Equal(t, "t3", envs["test_param2"])
	assert.Equal(t, "s1", envs["secret_param"])
	assert.Equal(t, "p1", envs["prod_param1"])
	assert.Equal(t, "${sec-10:ver-12345:abc}", envs["secret_param2"])
	assert.Equal(t, "${url:shiva:deploy}", envs["template_param"])

	T.producer.Assert(t, runCtx, model.Process)
	assert.Equal(t, runCtx.Deployment.State, model.Process)

	T.prodNomad.Success(t, common.Run, td.name, td.branch, td.version)
	assertStateChan(t, scheduler.Success, model.Success, runC)
	T.producer.Assert(t, runCtx, model.Success)
	assert.Equal(t, 1, len(T.prodNomad.RunCtxs))
	assert.Equal(t, td.branch, runCtx.Deployment.Branch)
	assert.Equal(t, td.version, runCtx.Deployment.Version)
	assert.Equal(t, comment, runCtx.Deployment.Comment)

	td.layer = common.Test
	_, _, err = T.run(td)
	assert.Equal(t, processor.ErrNothingChanged, err)
	td.envOverride["new_param"] = "key"
	_, _, err = T.run(td)
	require.NoError(t, err)
}

func TestService_RunNonBranchWithOverride(t *testing.T) {
	var err error
	test.RunUp(t)
	T := newTest(t)
	T.prepare(false, false, false)
	ctx := context.Background()
	_, _, err = T.service.Run(ctx, model.RunParams{
		Layer:        common.Test,
		Name:         "test-svc",
		Version:      "v1",
		Comment:      comment,
		Source:       config.UnknownSource,
		OverrideConf: []string{"env:e"},
	})
	assert.Equal(t, processor.ErrConfOverrideNotBranch, err)
}

func TestService_RunWithIncludes(t *testing.T) {
	var err error
	test.RunUp(t)
	T := newTest(t)
	makeIncludes(t, commonYml, "/shiva/common.yml")
	makeIncludes(t, testYml, "/shiva/test.yml")
	makeIncludes(t, testYml, "/shiva/prod.yml")
	makeManifest(t, testManifestWithIncludes1, t.Name())
	makeMap(t, testMap, t.Name(), false, false)

	td := T.TestData()
	td.branch = "b1"
	runC, runCtx, err := T.run(td)
	require.NoError(t, err)
	T.producer.Assert(t, runCtx, model.Process)
	assert.Equal(t, runCtx.Deployment.State, model.Process)

	T.testNomad.Success(t, common.Run, td.name, td.branch, td.version)
	assertStateChan(t, scheduler.Success, model.Success, runC)
	T.producer.Assert(t, runCtx, model.Success)
	assert.Equal(t, 1, len(T.testNomad.RunCtxs))
	assert.Equal(t, td.branch, runCtx.Deployment.Branch)
	assert.Equal(t, td.version, runCtx.Deployment.Version)
	assert.Equal(t, comment, runCtx.Deployment.Comment)

	_, _, err = T.run(td)
	assert.Equal(t, err, processor.ErrNothingChanged)

	makeIncludes(t, testYml1, "/shiva/test.yml")
	runC, runCtx, err = T.run(td)
	require.NoError(t, err)
	T.producer.Assert(t, runCtx, model.Process)
	assertDState(t, model.Process, runCtx.Deployment.State)

	T.testNomad.Success(t, common.Run, td.name, td.branch, td.version)
	assertStateChan(t, scheduler.Success, model.Success, runC)
	T.producer.Assert(t, runCtx, model.Success)
	assert.Equal(t, 2, len(T.testNomad.RunCtxs))
	assert.Equal(t, td.branch, runCtx.Deployment.Branch)
	assert.Equal(t, td.version, runCtx.Deployment.Version)
	assert.Equal(t, comment, runCtx.Deployment.Comment)

	makeManifest(t, testManifestWithIncludes2, t.Name())
	// имитация удаленного конфига в старом деплое, см. VOID-1735
	T.db.GormDb.Delete(&data.Data{}, "path = 'shiva/common.yml'")
	runC, runCtx, err = T.run(td.Version("3"))
	require.NoError(t, err)
	T.testNomad.Success(t, common.Run, td.name, td.branch, td.version)
}

func TestService_MirrorWithIncludes(t *testing.T) {
	var err error
	test.RunUp(t)
	T := newTest(t)
	makeIncludes(t, commonYml, "/shiva/common.yml")
	makeIncludes(t, testYml, "/shiva/test.yml")
	makeIncludes(t, testYml, "/shiva/prod.yml")
	makeManifest(t, testManifestWithIncludes1, t.Name())
	makeMap(t, testMap, t.Name(), false, false)

	td := T.TestData()
	td.layer = common.Prod
	_, runCtx, err := T.run(td)
	require.NoError(t, err)

	// имитация удаленного конфига в старом деплое, см. VOID-1735
	T.db.GormDb.Delete(&data.Data{}, "path = 'shiva/common.yml'")

	td.layer = common.Test
	_, runCtx, err = T.mirror(td, runCtx)
	require.NoError(t, err)
	require.Len(t, runCtx.Manifest.Config.Files, 2)
	require.Equal(t, runCtx.Manifest.Config.Files[0].Path, "shiva/common.yml")
	require.Equal(t, runCtx.Manifest.Config.Files[1].Path, "shiva/test.yml")
}

func TestService_RunWithIncludesAndOverride(t *testing.T) {
	var err error

	test.RunUp(t)
	defer test.Down(t)

	T := newTest(t)

	makeIncludes(t, commonYml, "/shiva/common.yml")
	makeIncludes(t, testYml, "/shiva/test.yml")
	makeIncludes(t, testYml1, "/shiva/test1.yml")
	makeIncludes(t, prodYml, "/shiva/prod.yml")
	makeManifest(t, testManifestWithIncludes1, t.Name())
	makeMap(t, testMap, t.Name(), false, false)
	makeMap(t, testMap, "shiva-tg", false, false)

	for _, v := range []common.Layer{common.Test, common.Prod} {
		require.NoError(t, T.envStorage.Save(&storage.ExternalEnv{
			Service: "shiva-tg",
			Layer:   v,
			Key:     "tvm-id",
			Type:    envPb.EnvType_GENERATED_TVM_ID,
			Value:   "10",
		}))
	}

	td := T.TestData()
	td.branch = "b1"
	td.confOverride = []string{"/shiva/test1.yml"}
	td.envOverride = map[string]string{"param1": "p1", "TEST_PARAM": "t1", "secret_param2": "${sec-10:ver-12345:abc}", "SECRET_PARAM": "s1", "template": "${tvm-id:shiva-tg}"}

	runC, runCtx, err := T.run(td)
	require.NoError(t, err)

	envs, err := runCtx.Manifest.Config.GetEnvs()
	require.NoError(t, err)

	assert.Len(t, envs, 6)
	assert.Equal(t, "p1", envs["param1"])
	assert.Equal(t, "common param", envs["COMMON_PARAM"])
	assert.Equal(t, "t1", envs["TEST_PARAM"])
	assert.Equal(t, "s1", envs["SECRET_PARAM"])
	assert.Equal(t, "${sec-10:ver-12345:abc}", envs["secret_param2"])
	assert.Equal(t, "${tvm-id:shiva-tg}", envs["template"])

	T.producer.Assert(t, runCtx, model.Process)
	assert.Equal(t, runCtx.Deployment.State, model.Process)

	T.testNomad.Success(t, common.Run, td.name, td.branch, td.version)
	assertStateChan(t, scheduler.Success, model.Success, runC)
	T.producer.Assert(t, runCtx, model.Success)
	assert.Equal(t, 1, len(T.testNomad.RunCtxs))
	assert.Equal(t, td.branch, runCtx.Deployment.Branch)
	assert.Equal(t, td.version, runCtx.Deployment.Version)
	assert.Equal(t, comment, runCtx.Deployment.Comment)

	td.layer = common.Prod
	runC, runCtx, err = T.run(td)

	envs, err = runCtx.Manifest.Config.GetEnvs()
	require.NoError(t, err)

	assert.Len(t, envs, 7)
	assert.Equal(t, "p1", envs["param1"])
	assert.Equal(t, "common param", envs["COMMON_PARAM"])
	assert.Equal(t, "prod param", envs["PROD_PARAM"])
	assert.Equal(t, "t1", envs["TEST_PARAM"])
	assert.Equal(t, "s1", envs["SECRET_PARAM"])
	assert.Equal(t, "${sec-10:ver-12345:abc}", envs["secret_param2"])
	assert.Equal(t, "${tvm-id:shiva-tg}", envs["template"])

	T.producer.Assert(t, runCtx, model.Process)
	assert.Equal(t, runCtx.Deployment.State, model.Process)

	T.prodNomad.Success(t, common.Run, td.name, td.branch, td.version)
	assertStateChan(t, scheduler.Success, model.Success, runC)
	T.producer.Assert(t, runCtx, model.Success)
	assert.Equal(t, 1, len(T.prodNomad.RunCtxs))
	assert.Equal(t, td.branch, runCtx.Deployment.Branch)
	assert.Equal(t, td.version, runCtx.Deployment.Version)
	assert.Equal(t, comment, runCtx.Deployment.Comment)

	td.layer = common.Test
	_, _, err = T.run(td)
	assert.Equal(t, processor.ErrNothingChanged, err)
	td.envOverride["new_param"] = "n"
	_, _, err = T.run(td)
	require.NoError(t, err)
}

func TestService_Run_MigrateScheduler(t *testing.T) {
	test.RunUp(t)
	T := newTest(t)
	T.prepare(false, false, false)
	td := T.TestData()
	T.fullRun(td)

	makeManifest(t, testManifestDroog, td.name)
	runC, _, err := T.run(td)
	require.NoError(t, err)
	go T.testYD.Success(t, common.Run, td.name, td.branch, td.version)
	go T.testNomad.SuccessStop(t, common.Stop, td.name, td.branch)
	assertStateChan(t, scheduler.Success, model.Success, runC)
	assert.Equal(t, 1, len(T.testNomad.StopCtxs))
}

func TestService_Run_YaDeploy(t *testing.T) {
	test.RunUp(t)
	T := newTest(t)
	makeManifest(t, testManifestDroog, t.Name())
	makeMap(t, testMap, t.Name(), false, false)

	td := T.TestData()
	runC, runCtx, err := T.run(td)
	require.NoError(t, err)
	T.producer.Assert(t, runCtx, model.Process)

	T.testYD.Success(t, common.Run, td.name, td.branch, td.version)
	assertStateChan(t, scheduler.Success, model.Success, runC)
	T.producer.Assert(t, runCtx, model.Success)
}

func TestService_State(t *testing.T) {
	test.RunUp(t)
	defer test.Down(t)
	T := newTest(t)
	T.prepare(false, false, false)
	T2 := newTest(t)

	td := T.TestData()
	_, runCtx, err := T.run(td)
	require.NoError(t, err)
	T.producer.Assert(t, runCtx, model.Process)
	assertDState(t, model.Process, runCtx.Deployment.State)
	assert.Equal(t, 1, len(T.testNomad.RunCtxs))
	runCtx.Deployment.StartDate = runCtx.Deployment.StartDate.Add(-time.Duration(24) * time.Hour)
	err = T.storage.UpdateState(runCtx.Deployment, runCtx.Deployment.State, revert.RevertType_None, "")
	require.NoError(t, err)

	stateC, stateCtx, err := T2.service.State(td.ctx, runCtx.Deployment.ID)
	require.NoError(t, err)
	assert.True(t, time.Now().Sub(stateCtx.Deployment.StartDate) > time.Duration(12)*time.Hour)
	T2.producer.AssertEmpty(t)
	T2.testNomad.Success(t, common.Run, td.name, td.branch, fmt.Sprintf("id:%d", stateCtx.Deployment.NomadID))
	assertStateChan(t, scheduler.Success, model.Success, stateC)
	assertDState(t, model.Process, runCtx.Deployment.State)
	assert.Equal(t, 1, len(T2.testNomad.StateCtxs))
	assert.Equal(t, td.version, runCtx.Deployment.Version)
	T.producer.AssertEmpty(t)
}

func TestService_Restart(t *testing.T) {
	var err error
	test.RunUp(t)
	T := newTest(t)
	T.prepare(false, false, false)

	td := T.TestData()
	restartC, restartCtx, err := T.restart(td)
	assert.Equal(t, processor.ErrServiceNotDeployed, err)
	runCtx := T.fullRun(td)
	restartC, restartCtx, err = T.restart(td)
	require.NoError(t, err)
	require.NotNil(t, restartCtx.Previous)
	require.Equal(t, runCtx.Deployment.ID, restartCtx.Previous.Deployment.ID)
	T.testNomad.Success(t, common.Restart, td.name, td.branch, "")
	assertStateChan(t, scheduler.Success, model.Success, restartC)
	assert.Equal(t, 1, len(T.testNomad.RestartCtxs))
	assert.Equal(t, td.branch, restartCtx.Deployment.Branch)
	assert.Equal(t, common.Restart, restartCtx.Deployment.Type)
	assert.Equal(t, td.comment, restartCtx.Deployment.Comment)
}

func TestService_RestartWithChangedOwnerInMap(t *testing.T) {
	test.RunUp(t)
	defer test.Down(t)

	T := newTest(t)
	T.prepare(false, false, false)

	makeMap(T.t, testMap1, T.t.Name(), false, false)
	_, _, err := T.service.Run(context.Background(), model.RunParams{
		Layer:   common.Prod,
		Name:    T.t.Name(),
		Version: "v1",
		Login:   "devreggs",
	})
	require.NoError(t, err)
	T.prodNomad.Success(t, common.Run, T.t.Name(), "", "v1")

	makeMap(T.t, testMap, T.t.Name(), false, false)
	_, _, err = T.service.Restart(context.Background(), model.RestartParams{
		UUID:  uuid.New(),
		Layer: common.Prod,
		Name:  T.t.Name(),
		Login: staff_fixture.Owner,
	})
	require.NoError(t, err)
}

func TestRestartFailed(t *testing.T) {
	test.RunUp(t)
	T := newTest(t)
	T.prepare(false, false, false)

	td := T.TestData()
	runCtx := T.fullRun(td)
	restartC, restartCtx, err := T.restart(td)
	require.NoError(t, err)
	require.NotNil(t, restartCtx.Previous)
	require.Equal(t, runCtx.Deployment.ID, restartCtx.Previous.Deployment.ID)
	T.testNomad.Fail(t, common.Restart, td.name, td.branch, "")
	assertStateChan(t, scheduler.Fail, model.Fail, restartC)

	assert.Equal(t, 1, len(T.testNomad.RestartCtxs))
	assert.Equal(t, td.branch, restartCtx.Deployment.Branch)
	assert.Equal(t, common.Restart, restartCtx.Deployment.Type)
	assert.Equal(t, td.comment, restartCtx.Deployment.Comment)
}

func TestRestartCancelled(t *testing.T) {
	test.RunUp(t)
	T := newTest(t)
	T.prepare(false, false, false)

	td := T.TestData()
	T.fullRun(td)
	restartC, restartCtx, err := T.restart(td)
	require.NoError(t, err)

	cancelC, cancelCtx, err := T.cancel(td, restartCtx)
	require.NoError(t, err)
	T.testNomad.Fail(t, common.Restart, td.name, td.branch, "")
	T.testNomad.CancelAndCanceled(t, common.Cancel, td.name, td.branch, fmt.Sprintf("%d/%d", cancelCtx.Parent.Deployment.NomadID, cancelCtx.Deployment.NomadID))
	assertStateChan(t, scheduler.Fail, model.Terminated, restartC)
	assertStateChan(t, scheduler.Fail, model.Success, cancelC)

	assert.Equal(t, 1, len(T.testNomad.RestartCtxs))
	assert.Equal(t, td.branch, restartCtx.Deployment.Branch)
	assert.Equal(t, common.Restart, restartCtx.Deployment.Type)
	assert.Equal(t, td.comment, restartCtx.Deployment.Comment)
}

func TestRunCancelledRestartPipeline(t *testing.T) {
	test.RunUp(t)
	T := newTest(t)
	T.prepare(false, false, false)

	td := T.TestData()
	T.fullRun(td)
	restartC, restartCtx, err := T.restart(td)
	require.NoError(t, err)

	cancelC, cancelCtx, err := T.cancel(td, restartCtx)
	require.NoError(t, err)
	requireDeploymentExist(T, restartCtx.Deployment.ID, common.Restart, model.Cancel)
	requireDeploymentExist(T, cancelCtx.Deployment.ID, common.Cancel, model.Process)
	T.testNomad.Fail(t, common.Restart, td.name, td.branch, "")
	T.testNomad.CancelAndCanceled(t, common.Cancel, td.name, td.branch, fmt.Sprintf("%d/%d", cancelCtx.Parent.Deployment.NomadID, cancelCtx.Deployment.NomadID))
	assertStateChan(t, scheduler.Fail, model.Terminated, restartC)
	assertStateChan(t, scheduler.Fail, model.Success, cancelC)
	requireDeploymentExist(T, cancelCtx.Deployment.ID, common.Cancel, model.Success)
	requireDeploymentExist(T, restartCtx.Deployment.ID, common.Restart, model.Terminated)

	assert.Equal(t, 1, len(T.testNomad.RestartCtxs))
	assert.Equal(t, td.branch, restartCtx.Deployment.Branch)
	assert.Equal(t, common.Restart, restartCtx.Deployment.Type)
	assert.Equal(t, td.comment, restartCtx.Deployment.Comment)

	runC, runCtx, err := T.run(td)
	require.NoError(t, err)
	T.testNomad.Success(t, common.Run, td.name, td.branch, td.version)
	assertStateChan(t, scheduler.Success, model.Success, runC)
	requireDeploymentExist(T, runCtx.Deployment.ID, common.Run, model.Success)

	_, _, err = T.run(td)
	require.Error(t, err)
	require.Equal(t, processor.ErrNothingChanged, err)
}

func TestService_UpdateAfterAutoScale(t *testing.T) {
	const overrideCpu = 7000

	test.RunUp(t)
	defer test.Down(t)

	T := newTest(t)

	testData := T.TestData()
	testData.metadata = "first_run_metadata"
	makeMap(t, testMap, testData.name, false, false)
	makeManifest(t, testManifestAutoCpu, testData.name)

	runCtx := T.fullRun(testData)
	T.assertUpdatedState(model.Success, runCtx)

	scaleStorage := scaler.NewStorage(T.db, test.NewLogger(t))
	require.NoError(t, scaleStorage.Save(&scaler.CalculatedResource{
		Layer:   testData.layer,
		Service: testData.name,
		CPU:     overrideCpu,
		At:      time.Now().Add(-1 * time.Hour),
	}))

	_, updateCtx, err := T.update(testData)

	require.NoError(t, err)
	assert.Equal(t, overrideCpu, updateCtx.OverrideCPU)
	assert.Equal(t, 1, len(T.testNomad.UpdateCtxs))
	assert.Equal(t, overrideCpu, T.testNomad.UpdateCtxs[0].OverrideCPU)
	assert.Equal(t, "first_run_metadata", updateCtx.Deployment.UserMetadata)
}

func requireDeploymentExist(T *Test, id int64, deployType common.Type, state model.State) {
	t := T.t
	t.Helper()

	d, err := T.storage.Get(id)
	require.NoError(t, err)
	require.NotNil(t, d)
	require.Equalf(t, deployType.String(), d.Type.String(), "deploy(id=%d) with type %s not found", id, deployType)
	require.Equalf(t, state.String(), d.State.String(), "deploy(id=%d) %s in state %s not found", id, deployType, state)
}

func TestService_Stop(t *testing.T) {
	var err error
	test.RunUp(t)
	T := newTest(t)
	T.prepare(false, false, false)
	td := T.TestData()
	stopC, stopCtx, err := T.stop(td)
	assert.Equal(t, processor.ErrServiceNotDeployed, err)
	runCtx := T.fullRun(td)
	stopC, stopCtx, err = T.stop(td)
	require.NoError(t, err)
	require.NotNil(t, stopCtx.Previous)
	require.Equal(t, runCtx.Deployment.ID, stopCtx.Previous.Deployment.ID)
	T.producer.Assert(t, stopCtx, model.Process)
	T.testNomad.SuccessStop(t, common.Stop, td.name, td.branch)
	assertStateChan(t, scheduler.Success, model.Success, stopC)
	T.producer.Assert(t, stopCtx, model.Success)
	assert.Equal(t, 1, len(T.testNomad.StopCtxs))
	assert.Equal(t, td.branch, stopCtx.Deployment.Branch)
	assert.Equal(t, common.Stop, stopCtx.Deployment.Type)
	assert.Equal(t, td.comment, stopCtx.Deployment.Comment)
}

func TestInProgress(t *testing.T) {

	test.RunUp(t)
	T := newTest(t)
	_, mID := makeManifest(t, testManifest, sName)
	_, mapID := makeMap(t, testMap, sName, true, false)
	log := test.NewLogger(t)
	staffApi := staffapi.NewApi(staffapi.NewConf(), log)
	staffService := staff.NewService(test_db.NewDb(t), staffApi, log)
	user, err := staffService.GetByLogin("danevge")
	require.NoError(t, err)
	exist := []*model.Deployment{
		NewD(t, T, common.Run, model.WaitApprove, mapID, mID, user.ID),
		NewD(t, T, common.Run, model.Prepare, mapID, mID, user.ID),
		NewD(t, T, common.Run, model.CanarySuccess, mapID, mID, user.ID),
		NewD(t, T, common.Run, model.CanaryProcess, mapID, mID, user.ID),
		NewD(t, T, common.Run, model.Process, mapID, mID, user.ID),
		NewD(t, T, common.Run, model.Cancel, mapID, mID, user.ID),
		NewD(t, T, common.Stop, model.Process, mapID, mID, user.ID),
		NewD(t, T, common.Restart, model.Process, mapID, mID, user.ID),
		NewD(t, T, common.Cancel, model.Process, mapID, mID, user.ID),
		NewD(t, T, common.Promote, model.Process, mapID, mID, user.ID),
		NewD(t, T, common.Revert, model.Process, mapID, mID, user.ID),
	}
	skip := []*model.Deployment{
		NewD(t, T, common.Run, model.Success, mapID, mID, user.ID),
		NewD(t, T, common.Run, model.Fail, mapID, mID, user.ID),
		NewD(t, T, common.Run, model.Expired, mapID, mID, user.ID),
		NewD(t, T, common.Run, model.ValidateFail, mapID, mID, user.ID),
		NewD(t, T, common.Run, model.Canceled, mapID, mID, user.ID),
	}
	ds, err := T.service.InProgress(sName)
	require.NoError(t, err)
	for _, ctx := range ds {
		assert.True(t, contains(exist, ctx.Deployment))
		assert.False(t, contains(skip, ctx.Deployment))
	}
}

func TestDeployMaxCpuWarning(t *testing.T) {
	test.RunUp(t)
	defer test.Down(t)

	T := newTest(t)
	makeMap(t, testMap, "test-svc", false, false)
	manFile := `
name: %s
image: %s
general:
  datacenters:
    sas: { count: 1 }
  upgrade: { parallel: 1 }
  resources:
    cpu: 200
    memory: 256
    auto_cpu: true
`
	makeManifest(t, manFile, "test-svc")
	logger := test.NewLogger(t)
	scaleStorage := scaler.NewStorage(T.db, logger)
	require.NoError(t, scaleStorage.Save(&scaler.CalculatedResource{
		Layer:   common.Prod,
		Service: "test-svc",
		CPU:     13000,
		At:      time.Now().Add(-1 * time.Hour),
	}))

	_, ctx, err := T.service.Run(context.Background(), model.RunParams{
		Layer:   common.Prod,
		Name:    "test-svc",
		Version: "v2",
		Login:   "ibiryulin",
	})
	require.NoError(t, err)
	require.Len(t, ctx.Deployment.Status.Warnings, 1)
	require.Contains(t, ctx.Deployment.Status.Warnings[0].RuMessage, scaler.ErrCpuLimited.RusMessage)
}

func TestDeployAnyDC(t *testing.T) {
	test.RunUp(t)
	defer test.Down(t)

	T := newTest(t)
	makeMap(t, testMap, "test-svc", false, false)
	makeManifest(t, testManifestAny, "test-svc")

	_, _, err := T.service.Run(context.Background(), model.RunParams{
		Layer:   common.Prod,
		Name:    "test-svc",
		Version: "v2",
		Login:   "ibiryulin",
	})
	require.NoError(t, err)
}

func TestDeployForbiddenIssue(t *testing.T) {
	test.RunUp(t)
	defer test.Down(t)

	T := newTest(t)
	makeManifest(t, testManifest, "test-svc")
	makeMap(t, testMap, "test-svc", false, false)

	issue := "VSSEC-366"
	_, ctx, err := T.service.Run(context.Background(), model.RunParams{
		Layer:   common.Prod,
		Name:    "test-svc",
		Version: "v2",
		Login:   "ibiryulin",
		Issues:  []string{issue},
	})
	assert.NoError(t, err)
	assert.Len(t, ctx.Deployment.Status.Warnings, 1)
	assert.Contains(t, ctx.Deployment.Status.Warnings[0].RuMessage, stAPI.ErrIssueForbidden(issue).RusMessage)
}

func TestDeployProdMissingIssue(t *testing.T) {
	test.RunUp(t)
	defer test.Down(t)

	T := newTest(t)
	makeManifest(t, testManifest, "test-svc")
	makeMap(t, testMap, "test-svc", false, false)

	issue := "abc"
	_, ctx, err := T.service.Run(context.Background(), model.RunParams{
		Layer:   common.Prod,
		Name:    "test-svc",
		Version: "v2",
		Login:   "ibiryulin",
		Issues:  []string{issue},
	})
	assert.NoError(t, err)
	assert.Len(t, ctx.Deployment.Status.Warnings, 1)
	assert.Contains(t, ctx.Deployment.Status.Warnings[0].RuMessage, stAPI.ErrIssueNotFound(issue).RusMessage)
}

func TestDeployTestMissingIssue(t *testing.T) {
	test.RunUp(t)
	defer test.Down(t)

	T := newTest(t)
	makeManifest(t, testManifest, "test-svc")
	makeMap(t, testMap, "test-svc", false, false)

	issue := "abc"
	_, ctx, err := T.service.Run(context.Background(), model.RunParams{
		Layer:   common.Test,
		Name:    "test-svc",
		Version: "v2",
		Login:   "ibiryulin",
		Issues:  []string{issue},
	})
	assert.NoError(t, err)
	assert.Len(t, ctx.Deployment.Status.Warnings, 1)
	assert.Contains(t, ctx.Deployment.Status.Warnings[0].RuMessage, stAPI.ErrIssueNotFound(issue).RusMessage)
}

func TestDeployTwoBranchInDiffBranches(t *testing.T) {
	const (
		svc    = "test-svc"
		branch = "void-1"
		login  = "danevge"
		layer  = common.Test
	)

	test.RunUp(t)
	defer test.Down(t)

	T := newTest(t)
	makeMap(t, testMap, svc, false, false)
	makeManifest(t, testManifestAny, svc)

	_, _, err := T.service.Run(context.Background(), model.RunParams{
		Layer:   layer,
		Name:    svc,
		Version: "v1",
		Login:   login,
		Branch:  branch,
	})
	require.NoError(t, err)

	_, _, err = T.service.Run(context.Background(), model.RunParams{
		Layer:   layer,
		Name:    svc,
		Version: "v2",
		Login:   login,
		Branch:  strings.ToUpper(branch),
	})

	assert.Equal(t, deployment.ErrAnotherDeployInProgress, err)
}

func TestDeployBranchWithSuccessAnotherDeploy(t *testing.T) {
	const (
		svc    = "test-svc"
		branch = "void-1"
		login  = "danevge"
		layer  = common.Test
	)

	test.RunUp(t)
	defer test.Down(t)

	T := newTest(t)
	makeMap(t, testMap, svc, false, false)
	makeManifest(t, testManifestAny, svc)

	runParams := model.RunParams{
		Layer:   layer,
		Name:    svc,
		Version: "v1",
		Login:   login,
		Branch:  branch,
	}

	_, _, err := T.service.Run(context.Background(), runParams)
	require.NoError(t, err)

	td := &TestData{
		layer: layer,
	}
	T.sch(td).Success(t, common.Run, svc, runParams.Branch, runParams.Version)

	_, _, err = T.service.Run(context.Background(), model.RunParams{
		Layer:   layer,
		Name:    svc,
		Version: "v1",
		Login:   login,
		Branch:  strings.ToUpper(branch),
	})

	assert.Equal(t, processor.ErrExistBranchDeploy, err)
}

func TestDeployProdSoxMissingIssue(t *testing.T) {
	test.RunUp(t)
	defer test.Down(t)

	T := newTest(t)
	makeManifest(t, testManifest, "test-svc")
	makeMap(t, testMap, "test-svc", true, false)

	issue := "abc"
	_, _, err := T.service.Run(context.Background(), model.RunParams{
		Layer:   common.Prod,
		Name:    "test-svc",
		Version: "v2",
		Login:   "ibiryulin",
		Issues:  []string{issue},
	})
	assert.Error(t, err)
	var ue *user_error.UserError
	assert.True(t, errors.As(err, &ue))
	assert.Equal(t, stAPI.ErrIssueNotFound(issue).RusMessage, ue.RusMessage)
}

func TestDeployTestSoxMissingIssue(t *testing.T) {
	test.RunUp(t)
	defer test.Down(t)

	T := newTest(t)
	makeManifest(t, testManifest, "test-svc")
	makeMap(t, testMap, "test-svc", true, false)

	issue := "abc"
	_, ctx, err := T.service.Run(context.Background(), model.RunParams{
		Layer:   common.Test,
		Name:    "test-svc",
		Version: "v2",
		Login:   "ibiryulin",
		Issues:  []string{issue},
	})
	assert.NoError(t, err)
	assert.Len(t, ctx.Deployment.Status.Warnings, 1)
	assert.Contains(t, ctx.Deployment.Status.Warnings[0].RuMessage, stAPI.ErrIssueNotFound(issue).RusMessage)
}

func TestPromoteSoxCanary(t *testing.T) {
	test.RunUp(t)

	h := newTest(t)
	h.prepare(false, false, false)
	h.fullRun(h.TestData().Prod())
	h.prepare(true, true, false)
	ctx, cancel := context.WithTimeout(context.Background(), time.Second*5)
	defer cancel()

	td := &TestData{
		ctx:     ctx,
		layer:   common.Test,
		name:    t.Name(),
		version: "v1",
		login:   staff_fixture.Owner,
		issues:  []string{"VOID-102"},
	}
	dt := h.fullRun(td)

	dpSoxState, dpSox, err := h.service.Promote(ctx, model.PromoteParams{
		ID:    dt.Deployment.ID,
		Login: td.login,
	})
	require.NoError(t, err)
	assert.Nil(t, dpSoxState, "promote chan should be nil")
	assert.Equal(t, model.WaitApprove, dpSox.Deployment.State, "deployment should be in wait approve")

	dpcState, dpc, err := h.service.Approve(ctx, model.ApproveParams{
		ID:    dpSox.Deployment.ID,
		Login: "avkosorukov",
	})
	require.NoError(t, err)
	h.prodNomad.Success(t, common.Run, t.Name(), "canary", "v1")
	assertStateChan(t, scheduler.Success, model.CanaryOnePercent, dpcState)

	canaryPState, canaryPCtx, err := h.service.Promote(ctx, model.PromoteParams{
		ID:    dpc.Deployment.ID,
		Login: "avkosorukov",
	})
	assert.Nil(t, canaryPState)
	require.NoError(t, err)
	assertDState(t, canaryPCtx.Deployment.State, model.CanarySuccess)

	dpState, dp, err := h.service.Promote(ctx, model.PromoteParams{
		ID:    dpc.Deployment.ID,
		Login: "avkosorukov",
	})
	require.NoError(t, err)
	h.prodNomad.Success(t, common.Run, t.Name(), "", "v1")
	h.prodNomad.SuccessStop(t, common.Stop, t.Name(), "canary")
	assertStateChan(t, scheduler.Success, model.Success, dpState)
	assert.Equal(t, model.Success, dp.Deployment.State)
}

func TestRunSoxCanary(t *testing.T) {
	test.RunUp(t)

	h := newTest(t)
	h.prepare(false, false, false)
	h.fullRun(h.TestData().Prod())
	h.prepare(true, true, false)

	ctx, cancel := context.WithTimeout(context.Background(), time.Second*5)
	defer cancel()

	h.fullRun(&TestData{
		ctx:     ctx,
		layer:   common.Test,
		name:    t.Name(),
		version: "v1",
		login:   staff_fixture.Owner,
		issues:  []string{"VOID-102"},
	})

	td := &TestData{
		ctx:     ctx,
		layer:   common.Prod,
		name:    t.Name(),
		version: "v1",
		login:   staff_fixture.Owner,
		issues:  []string{"VOID-102"},
	}
	dpSoxState, dpSox, err := h.run(td)
	require.NoError(t, err)
	assert.Nil(t, dpSoxState, "promote chan should be nil")
	assert.Equal(t, model.WaitApprove, dpSox.Deployment.State, "deployment should be in wait approve")

	dpcState, dpc, err := h.service.Approve(ctx, model.ApproveParams{
		ID:    dpSox.Deployment.ID,
		Login: "avkosorukov",
	})
	require.NoError(t, err)
	h.prodNomad.Success(t, common.Run, t.Name(), "canary", "v1")
	assertStateChan(t, scheduler.Success, model.CanaryOnePercent, dpcState)

	canaryPState, canaryPCtx, err := h.service.Promote(ctx, model.PromoteParams{
		ID:    dpc.Deployment.ID,
		Login: staff_fixture.Owner,
	})
	assert.Nil(t, canaryPState)
	require.NoError(t, err)
	assertDState(t, canaryPCtx.Deployment.State, model.CanarySuccess)

	dpState, dp, err := h.service.Promote(ctx, model.PromoteParams{
		ID:    dpc.Deployment.ID,
		Login: staff_fixture.Owner,
	})
	require.NoError(t, err)
	h.prodNomad.Success(t, common.Run, t.Name(), "", "v1")
	h.prodNomad.SuccessStop(t, common.Stop, t.Name(), "canary")
	assertStateChan(t, scheduler.Success, model.Success, dpState)
	assert.Equal(t, model.Success, dp.Deployment.State)
}

func contains(ds []*model.Deployment, actual *model.Deployment) bool {
	for _, d := range ds {
		if d.ID == actual.ID {
			return true
		}
	}
	return false
}

func NewD(t *testing.T, T *Test, dT common.Type, state model.State, sMap int64, m int64, uID int64) *model.Deployment {

	d := &model.Deployment{
		Type:             dT,
		State:            state,
		Layer:            common.Test,
		Name:             sName,
		Version:          "version",
		AuthorID:         uID,
		ServiceMapsID:    sMap,
		DeployManifestID: m,
	}
	require.NoError(t, T.storage.Save(d))
	return d
}

func assertContains(t *testing.T, services []*dcontext.Context, service *dcontext.Context, exist bool) {
	var contains bool
	for _, s := range services {
		if s.Deployment.ID == service.Deployment.ID {
			contains = true
			break
		}
	}
	assert.Equal(t, exist, contains)
}

func makeManifest(t *testing.T, yml, name string) (*mModel.Manifest, int64) {

	imageName := fmt.Sprintf("%s-image", name)
	db := test_db.NewDb(t)
	log := test.NewLogger(t)
	includeService := include.NewService(db, log)
	service := manifest.NewService(db, log, parser.NewService(log, reader.NewService(db, log)), includeService)
	result := fmt.Sprintf(yml, name, imageName)
	err := service.ReadAndSave([]byte(result), test.AtomicNextUint(), "")
	require.NoError(t, err)
	m, id, err := service.GetByNameWithId(common.Test, name)
	require.NoError(t, err)
	return m, id
}

func embedManifest(t *testing.T, filename, svc string) (*mModel.Manifest, int64) {
	b, err := testFS.ReadFile(filename)
	require.NoError(t, err, "failed to read embed file")
	return makeManifest(t, string(b), svc)
}

func makeMap(t *testing.T, yml, name string, sox, dss bool) (*spb.ServiceMap, int64) {
	db := test_db.NewDb(t)
	service := service_map.NewService(db, test.NewLogger(t), service_change.NewNotificationMock())
	result := fmt.Sprintf(yml, name, sox, dss)
	path := spb.ToFullPath(name)
	err := service.ReadAndSave([]byte(result), test.AtomicNextUint(), path)
	require.NoError(t, err)

	sMap, id, err := service.GetByFullPath(path)
	require.NoError(t, err)
	return sMap, id
}

func embedMap(t *testing.T, file, svc string, sox, dss bool) (*spb.ServiceMap, int64) {
	b, err := testFS.ReadFile(file)
	require.NoError(t, err, "failed to read embed file")
	return makeMap(t, string(b), svc, sox, dss)
}

func makeIncludes(t *testing.T, includeFile, path string) {

	db := test_db.NewDb(t)
	log := test.NewLogger(t)
	service := include.NewService(db, log)
	err := service.ReadAndSave([]byte(includeFile), test.AtomicNextUint(), path)
	require.NoError(t, err)
}

func generateEnv(t *testing.T, name, key, value string) {
	db := test_db.NewDb(t)
	service := writer.NewService(db, test.NewLogger(t), nil, nil, nil)

	extEnv := &storage.ExternalEnv{
		Service: name,
		Type:    envPb.EnvType_GENERATED_TVM_SECRET,
		Layer:   common.Test,
		Key:     key,
		Value:   value}

	service.New(extEnv)
}

func setSecretEnvs(T *Test) {
	T.ssCliMock.ExpectedCalls = nil

	T.ssCliMock.On("GetSecrets", mock.Anything, &secret.GetSecretsRequest{
		ServiceName: T.t.Name(),
		Layer:       layer.Layer_TEST,
	}).Return(&secret.GetSecretResponse{SecretEnvs: secretEnvs}, nil)
}

func updateSecretEnvs(T *Test) {
	T.ssCliMock.ExpectedCalls = nil

	T.ssCliMock.On("GetSecrets", mock.Anything, &secret.GetSecretsRequest{
		ServiceName: T.t.Name(),
		Layer:       layer.Layer_TEST,
	}).Return(&secret.GetSecretResponse{SecretEnvs: secretEnvsNew}, nil)
	T.ssCliMock.On("GetSecrets", mock.Anything, &secret.GetSecretsRequest{
		ServiceName: T.t.Name(),
		Layer:       layer.Layer_TEST,
		VersionId:   secVersionOld,
	}).Return(&secret.GetSecretResponse{SecretEnvs: secretEnvs}, nil)
}

func prepareSecretEnvs(T *Test, layer layer.Layer, envs map[string]string) {
	version := ""
	for _, v := range envs {
		version = strings.Split(v, ":")[1]
	}

	T.ssCliMock.On("GetSecrets", mock.Anything, &secret.GetSecretsRequest{
		ServiceName: T.t.Name(),
		Layer:       layer,
	}).Return(&secret.GetSecretResponse{SecretEnvs: envs}, nil)
	T.ssCliMock.On("GetSecrets", mock.Anything, &secret.GetSecretsRequest{
		ServiceName: T.t.Name(),
		Layer:       layer,
		VersionId:   version,
	}).Return(&secret.GetSecretResponse{SecretEnvs: envs}, nil)
}

func assertEqualIncludes(t *testing.T, id1, id2 int64) bool {
	includeLinksStorage := include_links.NewStorage(test_db.NewDb(t), test.NewLogger(t))
	coStorage := env_override.NewStorage(test_db.NewDb(t), test.NewLogger(t))
	log := test.NewLogger(t)
	includeS := include.NewService(test_db.NewDb(t), log)
	prevIncs, err := includeLinksStorage.GetByDeploymentId(id1)
	require.NoError(t, err)
	curIncs, err := includeLinksStorage.GetByDeploymentId(id2)
	require.NoError(t, err)

	if len(prevIncs) != len(curIncs) {
		log.Infof("TEST: Length not equal")
		return false
	}
	incMap := map[int64]struct{}{}
	for _, inc := range prevIncs {
		incMap[inc.IncludeId] = struct{}{}
	}
	for _, inc := range curIncs {
		if _, ok := incMap[inc.IncludeId]; !ok {
			i, err := includeS.Get(inc.IncludeId)
			require.NoError(t, err)
			log.Infof("TEST: Include %s not find", i.Path)
			return false
		}
	}

	prevConf, err := coStorage.GetByDeploymentId(id1)
	require.NoError(t, err)
	selfConf, err := coStorage.GetByDeploymentId(id2)
	require.NoError(t, err)

	if len(prevConf) != len(selfConf) {
		log.Infof("TEST: Length conf not equal")
		return false
	}
	envMap := map[string]string{}
	for _, env := range prevConf {
		envMap[env.Key] = env.Value
	}
	for _, env := range selfConf {
		if val, ok := envMap[env.Key]; !ok || val != env.Value {
			log.Infof("TEST: Conf %s not find", env.Key)
			return false
		}
	}
	return true
}

func TestGet(t *testing.T) {
	test.RunUp(t)
	T := newTest(t)
	T.prepare(false, false, false)
	data := T.TestData()
	data.issues = []string{"VOID-102"}
	ctx := T.fullRun(data)
	d, err := T.service.Get(ctx.Deployment.ID)
	require.NoError(t, err)
	require.NotNil(t, d)
	require.NotNil(t, d.Manifest)
	require.NotNil(t, d.ServiceMap)
	require.Len(t, d.Issues, 1)
	require.NotNil(t, d.Author)
}

func TestFailStatus(t *testing.T) {
	test.RunUp(t)
	T := newTest(t)
	T2 := newTest(t)
	T.prepare(false, false, false)
	td := T.TestData()
	c, ctx, err := T.run(td)
	require.NoError(t, err)

	stateC, stateCtx, err := T2.service.State(td.ctx, ctx.Deployment.ID)
	require.NoError(t, err)

	T.testNomad.Terminate(t, common.Run, td.name, td.branch, td.version)
	assertStateChan(t, scheduler.Revert, model.Fail, c)
	d := T.assertUpdatedState(model.Process, ctx)
	assert.Len(t, d.Status.FailAllocations, 2)
	assert.Len(t, d.Status.Numbers, 2)
	assert.Equal(t, revert.RevertType_Terminate, d.Status.RevertType)
	d = ctx.Deployment
	assert.Len(t, d.Status.FailAllocations, 2)
	assert.Len(t, d.Status.Numbers, 2)
	assert.Equal(t, revert.RevertType_Terminate, d.Status.RevertType)

	T2.testNomad.Terminate(t, common.Run, td.name, td.branch, fmt.Sprintf("id:%d", stateCtx.Deployment.NomadID))
	assertStateChan(t, scheduler.Revert, model.Fail, stateC)
	d = stateCtx.Deployment
	assert.Len(t, d.Status.FailAllocations, 2)
	assert.Len(t, d.Status.Numbers, 2)
	assert.Equal(t, revert.RevertType_Terminate, d.Status.RevertType)
}

func TestNomadError(t *testing.T) {
	var err error
	test.RunUp(t)
	T := newTest(t)
	T.prepare(false, false, false)
	td := T.TestData()
	T.testNomad.Error = &user_error.UserError{RusMessage: "test msg"}
	_, ctx, err := T.run(td)
	userError := &user_error.UserError{}
	require.True(t, errors.As(err, &userError))
	assert.Equal(t, "test msg", ctx.Deployment.Description)
}

func TestRunWithRevokedSecret(t *testing.T) {
	test.RunUp(t)
	defer test.Down(t)
	T := newTest(t)

	makeManifest(t, testManifestSecret, sName)
	makeMap(t, testMap, sName, false, false)

	td := T.TestData()
	td.name = sName
	T.fullRun(td)

	makeManifest(t, testManifest, sName)
	T.ssMock.Add(sName, "sec-16", "ver-42", &userError.UserError{Error: "secret not delegated"}, false)

	T.fullRun(td)
}
