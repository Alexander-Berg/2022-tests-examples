package deployment_test

import (
	"fmt"
	"testing"

	"github.com/YandexClassifieds/shiva/cmd/shiva/deployment/model"
	"github.com/YandexClassifieds/shiva/cmd/shiva/deployment/processor"
	"github.com/YandexClassifieds/shiva/cmd/shiva/scheduler"
	"github.com/YandexClassifieds/shiva/common"
	"github.com/YandexClassifieds/shiva/common/user_error"
	"github.com/YandexClassifieds/shiva/test"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
)

func TestPromoteWithApprove(t *testing.T) {
	testCases := []struct {
		name   string
		canary bool
		sox    bool
		pciDss bool
	}{
		{name: "sox", canary: false, sox: true, pciDss: false},
		{name: "pci_dss", canary: false, sox: true, pciDss: true},
	}

	for _, tc := range testCases {
		t.Run(tc.name, func(t *testing.T) {
			test.RunUp(t)

			T := newTest(t)
			T.prepare(false, true, false)

			td := T.TestData()
			td.issues = []string{"VOID-102"}
			runCtx := T.fullRun(td)

			_, promoteCtx, err := T.service.Promote(td.ctx, model.PromoteParams{
				ID:     runCtx.Deployment.ID,
				Login:  td.login,
				Source: td.source,
			})
			require.NoError(t, err)
			T.producer.Assert(t, promoteCtx, model.WaitApprove)
			assert.Equal(t, common.Prod, promoteCtx.Deployment.Layer)
			assert.Equal(t, model.WaitApprove, promoteCtx.Deployment.State)
			assert.Equal(t, common.Promote, promoteCtx.Deployment.Type)

			assert.Equal(t, runCtx.Deployment.Version, promoteCtx.Deployment.Version)
			assert.Equal(t, runCtx.Deployment.AuthorID, promoteCtx.Deployment.AuthorID)
			assert.Equal(t, runCtx.Deployment.ServiceMapsID, promoteCtx.Deployment.ServiceMapsID)
			assert.Equal(t, runCtx.Deployment.DeployManifestID, promoteCtx.Deployment.DeployManifestID)
			assert.Equal(t, runCtx.Deployment.ID, promoteCtx.Deployment.ParentId)

			approveC, approveCtx, err := T.service.Approve(td.ctx, model.ApproveParams{
				ID:    promoteCtx.Deployment.ID,
				Login: "avkosorukov",
			})
			require.NoError(t, err)
			T.producer.Assert(t, approveCtx, model.Process)
			T.prodNomad.Success(t, common.Run, td.name, td.branch, td.version)
			assertStateChan(t, scheduler.Success, model.Success, approveC)

			T.producer.Assert(t, approveCtx, model.Success)
			assert.Equal(t, common.Prod, approveCtx.Deployment.Layer)
			assert.Equal(t, model.Success, approveCtx.Deployment.State)
			assert.Equal(t, common.Promote, promoteCtx.Deployment.Type)

			assert.Equal(t, runCtx.Deployment.Version, approveCtx.Deployment.Version)
			assert.Equal(t, runCtx.Deployment.AuthorID, approveCtx.Deployment.AuthorID)
			assert.Equal(t, runCtx.Deployment.ServiceMapsID, approveCtx.Deployment.ServiceMapsID)
			assert.Equal(t, runCtx.Deployment.DeployManifestID, approveCtx.Deployment.DeployManifestID)
			assert.Equal(t, runCtx.Deployment.ID, approveCtx.Deployment.ParentId)
		})
	}
}

func TestValidateEmptyIssue(t *testing.T) {

	test.RunUp(t)
	defer test.Down(t)
	T := newTest(t)
	T.prepare(false, true, false)
	td := T.TestData()
	td.layer = common.Prod
	_, _, err := T.run(td)
	assert.Equal(t, processor.ErrSoxIssueNotFound, err)
}

func TestValidateFakeIssue(t *testing.T) {

	test.RunUp(t)
	defer test.Down(t)
	T := newTest(t)
	T.prepare(false, true, false)
	td := T.TestData()
	td.layer = common.Prod
	td.issues = []string{"tatata-34626"}
	_, _, err := T.run(td)
	uErr := err.(*user_error.UserError)
	assert.Equal(t, "Задача \"tatata-34626\" не найдена", uErr.RusMessage)
}

func TestValidateDeployToProd(t *testing.T) {

	test.RunUp(t)
	defer test.Down(t)
	T := newTest(t)
	T.prepare(false, true, false)
	td := T.TestData()
	td.layer = common.Prod
	td.issues = []string{"VOID-102"}
	_, _, err := T.run(td)
	require.NotNil(t, err)
	assert.Equal(t, fmt.Sprintf("sox: deployment (%s:%s) not found in test layer", td.name, td.version), err.Error())
}

func TestSoxBranch(t *testing.T) {
	test.RunUp(t)

	T := newTest(t)
	T.prepare(false, true, false)
	td := T.TestData()
	td.branch = "test-branch"
	T.fullRun(td)

	td = T.TestData()
	td.issues = []string{"VOID-102"}
	td.layer = common.Prod
	td.branch = "test-branch"
	dCtx := T.fullRunSox(td)

	_, _, err := T.service.Approve(td.ctx, model.ApproveParams{
		ID:    dCtx.Deployment.ID,
		Login: "avkosorukov",
	})
	require.NoError(t, err)
}

func TestValidateBranchDeployToProd(t *testing.T) {
	test.RunUp(t)

	T := newTest(t)
	T.prepare(false, true, false)

	td := T.TestData()
	td.version = "other.ver"
	td.branch = "test-branch"
	T.fullRun(td)

	td = T.TestData()
	T.fullRun(td)

	td = T.TestData()
	td.issues = []string{"VOID-102"}
	td.layer = common.Prod
	td.branch = "test-branch"
	_, _, err := T.run(td)
	require.Equal(t,
		"Сервис находится под требованиями sox или pci-dss. (TestValidateBranchDeployToProd:test-branch:1.0.0) не найден в слое Test",
		user_error.ToReadableError(err))
}
