package transformer_test

import (
	"fmt"
	"math/rand"
	"strconv"
	"testing"
	"time"

	"github.com/YandexClassifieds/shiva/cmd/shiva/approve"
	"github.com/YandexClassifieds/shiva/cmd/shiva/deployment/data"
	"github.com/YandexClassifieds/shiva/cmd/shiva/deployment/issue_link"
	"github.com/YandexClassifieds/shiva/cmd/shiva/deployment/model"
	"github.com/YandexClassifieds/shiva/cmd/shiva/deployment/status"
	"github.com/YandexClassifieds/shiva/cmd/shiva/scheduler/consul"
	"github.com/YandexClassifieds/shiva/cmd/shiva/transformer"
	"github.com/YandexClassifieds/shiva/common"
	"github.com/YandexClassifieds/shiva/pb/shiva/api/deploy2"
	dProto "github.com/YandexClassifieds/shiva/pb/shiva/types/deployment"
	"github.com/YandexClassifieds/shiva/pb/shiva/types/info"
	"github.com/YandexClassifieds/shiva/pb/shiva/types/layer"
	"github.com/YandexClassifieds/shiva/pb/shiva/types/revert"
	"github.com/YandexClassifieds/shiva/pb/shiva/types/traffic"
	"github.com/YandexClassifieds/shiva/pkg/include"
	"github.com/YandexClassifieds/shiva/pkg/manifest"
	"github.com/YandexClassifieds/shiva/pkg/manifest/parser"
	"github.com/YandexClassifieds/shiva/pkg/service_map"
	"github.com/YandexClassifieds/shiva/pkg/staff"
	staffapi "github.com/YandexClassifieds/shiva/pkg/staff/api"
	"github.com/YandexClassifieds/shiva/pkg/tracker"
	"github.com/YandexClassifieds/shiva/test"
	"github.com/YandexClassifieds/shiva/test/mock/service_change"
	"github.com/YandexClassifieds/shiva/test/test_db"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
)

var (
	r *rand.Rand
)

const (
	sMapYml = `
name: %s
`
	manifestYml = `
name: %s
test:
  datacenters:
    myt: 
      count: 2
    sas:
      count: 2
prod:
  datacenters:
    myt:
      count: 2
    sas:
      count: 2
`
)

func init() {
	test.InitTestEnv()
	r = rand.New(rand.NewSource(time.Now().UnixNano()))
}

func TestSimple(t *testing.T) {
	test.RunUp(t)
	h, statusS := prepare(t)
	ds := simple(t, newDeployment(t))
	statuses, err := statusS.ListByServiceName(t.Name())
	require.NoError(t, err)
	fullInfo, err := h.InfoByStatuses(t.Name(), statuses)
	require.NoError(t, err)

	require.Len(t, fullInfo, 2)
	for _, infoItem := range fullInfo {
		switch {
		case compare(t, ds["test"], infoItem):
			assertOldInfo(t, ds["test"], ds["second_test"], infoItem)
		case compare(t, ds["prod"], infoItem):
			assertOldInfo(t, ds["prod"], ds["second_prod"], infoItem)
		default:
			assert.FailNow(t, fmt.Sprintf("undefined info: %s/%s/%s", infoItem.Layer.String(), infoItem.Name, infoItem.Branch))
		}
	}
}

func TestLastRevert(t *testing.T) {
	test.RunUp(t)
	log := test.NewLogger(t)
	db := test_db.NewDb(t)
	h, statusS := prepare(t)
	dStore := model.NewStorage(db, log)
	example := newDeployment(t)
	ds := simple(t, example)
	lastFail := copyD(t, example)
	lastFail.Layer = common.Test
	lastFail.State = model.Fail
	lastFail.RevertType = revert.RevertType_Unhealthy
	lastFail.Status.Provides[0].Status = false
	require.NoError(t, dStore.Save(lastFail))
	ds["test_last_fail"] = lastFail

	statuses, err := statusS.ListByServiceName(t.Name())
	require.NoError(t, err)
	fullInfo, err := h.InfoByStatuses(t.Name(), statuses)
	require.NoError(t, err)
	require.Len(t, fullInfo, 2)
	for _, infoItem := range fullInfo {
		switch {
		case compare(t, ds["test"], infoItem):
			assertDInfo(t, ds["test"], infoItem.Now)
			assertDInfo(t, ds["second_test"], infoItem.Previous)
			assertDInfo(t, ds["test_last_fail"], infoItem.Last)
			assert.Nil(t, infoItem.New)
			assert.Nil(t, infoItem.Child)
		case compare(t, ds["prod"], infoItem):
			assertOldInfo(t, ds["prod"], ds["second_prod"], infoItem)
		default:
			assert.FailNow(t, fmt.Sprintf("undefined info: %s/%s/%s", infoItem.Layer.String(), infoItem.Name, infoItem.Branch))
		}
	}
}

func TestLastCanceled(t *testing.T) {
	test.RunUp(t)
	log := test.NewLogger(t)
	db := test_db.NewDb(t)
	h, statusS := prepare(t)
	dStore := model.NewStorage(db, log)
	example := newDeployment(t)
	ds := simple(t, example)
	lastFail := copyD(t, example)
	lastFail.Layer = common.Test
	lastFail.State = model.Canceled
	require.NoError(t, dStore.Save(lastFail))
	ds["test_last_canceled"] = lastFail

	statuses, err := statusS.ListByServiceName(t.Name())
	require.NoError(t, err)
	fullInfo, err := h.InfoByStatuses(t.Name(), statuses)
	require.NoError(t, err)
	require.Len(t, fullInfo, 2)
	for _, infoItem := range fullInfo {
		switch {
		case compare(t, ds["test"], infoItem):
			assertDInfo(t, ds["test"], infoItem.Now)
			assertDInfo(t, ds["second_test"], infoItem.Previous)
			assertDInfo(t, ds["test_last_canceled"], infoItem.Last)
			assert.Nil(t, infoItem.New)
			assert.Nil(t, infoItem.Child)
		case compare(t, ds["prod"], infoItem):
			assertOldInfo(t, ds["prod"], ds["second_prod"], infoItem)
		default:
			assert.FailNow(t, fmt.Sprintf("undefined info: %s/%s/%s", infoItem.Layer.String(), infoItem.Name, infoItem.Branch))
		}
	}
}

func TestRevert(t *testing.T) {
	test.RunUp(t)
	log := test.NewLogger(t)
	db := test_db.NewDb(t)
	dStore := model.NewStorage(db, log)
	h, statusS := prepare(t)
	example := newDeployment(t)
	ds := simple(t, example)
	prodProcess := copyD(t, example)
	prodProcess.Layer = common.Prod
	prodProcess.State = model.Process
	prodProcess.RevertType = revert.RevertType_Unhealthy
	prodProcess.Status.Provides[0].Status = false
	require.NoError(t, dStore.Save(prodProcess))
	ds["prod_process"] = prodProcess

	statuses, err := statusS.ListByServiceName(t.Name())
	require.NoError(t, err)
	fullInfo, err := h.InfoByStatuses(t.Name(), statuses)
	require.NoError(t, err)

	require.Len(t, fullInfo, 2)
	for _, infoItem := range fullInfo {
		switch {
		case compare(t, ds["test"], infoItem):
			assertOldInfo(t, ds["test"], ds["second_test"], infoItem)
		case compare(t, ds["prod"], infoItem):
			assertOldProcessInfo(t, ds["prod"], ds["second_prod"], ds["prod_process"], infoItem)
			infoItem.New.Deployment.RevertType = revert.RevertType_Unhealthy
		default:
			assert.FailNow(t, fmt.Sprintf("undefined info: %s/%s/%s", infoItem.Layer.String(), infoItem.Name, infoItem.Branch))
		}
	}
}

func TestCancel(t *testing.T) {
	test.RunUp(t)
	log := test.NewLogger(t)
	db := test_db.NewDb(t)
	dStore := model.NewStorage(db, log)
	h, statusS := prepare(t)
	example := newDeployment(t)
	ds := simple(t, example)
	prodProcess := copyD(t, example)
	prodProcess.Layer = common.Prod
	prodProcess.State = model.Cancel
	require.NoError(t, dStore.Save(prodProcess))
	ds["prod_process"] = prodProcess
	cancelProcess := copyD(t, example)
	cancelProcess.Type = common.Cancel
	cancelProcess.Layer = common.Prod
	cancelProcess.State = model.Process
	cancelProcess.ParentId = prodProcess.ID
	require.NoError(t, dStore.Save(cancelProcess))
	ds["cancel_process"] = cancelProcess

	statuses, err := statusS.ListByServiceName(t.Name())
	require.NoError(t, err)
	fullInfo, err := h.InfoByStatuses(t.Name(), statuses)
	require.NoError(t, err)

	require.Len(t, fullInfo, 2)
	for _, infoItem := range fullInfo {
		switch {
		case compare(t, ds["test"], infoItem):
			assertOldInfo(t, ds["test"], ds["second_test"], infoItem)
		case compare(t, ds["prod"], infoItem):
			assertDInfo(t, ds["prod"], infoItem.Now)
			assertDInfo(t, ds["second_prod"], infoItem.Previous)
			assertDInfo(t, ds["second_prod"], infoItem.Last)
			assertDInfo(t, ds["prod_process"], infoItem.New)
			assertDInfo(t, ds["cancel_process"], infoItem.Child)
		default:
			assert.FailNow(t, fmt.Sprintf("undefined info: %s/%s/%s", infoItem.Layer.String(), infoItem.Name, infoItem.Branch))
		}
	}
}

func TestInProgress(t *testing.T) {
	test.RunUp(t)
	log := test.NewLogger(t)
	db := test_db.NewDb(t)
	dStore := model.NewStorage(db, log)
	h, statusS := prepare(t)
	example := newDeployment(t)
	ds := simple(t, example)
	branchProcess := copyD(t, example)
	branchProcess.Branch = "b1"
	branchProcess.Layer = common.Test
	branchProcess.State = model.Process
	require.NoError(t, dStore.Save(branchProcess))
	ds["test_branch_process"] = branchProcess
	prodProcess := copyD(t, example)
	prodProcess.Layer = common.Prod
	prodProcess.State = model.Process
	require.NoError(t, dStore.Save(prodProcess))
	ds["prod_process"] = prodProcess

	statuses, err := statusS.ListByServiceName(t.Name())
	require.NoError(t, err)
	fullInfo, err := h.InfoByStatuses(t.Name(), statuses)
	require.NoError(t, err)

	require.Len(t, fullInfo, 3)
	for _, infoItem := range fullInfo {
		switch {
		case compare(t, ds["test"], infoItem):
			assertOldInfo(t, ds["test"], ds["second_test"], infoItem)
		case compare(t, ds["prod"], infoItem):
			assertOldProcessInfo(t, ds["prod"], ds["second_prod"], ds["prod_process"], infoItem)
		case compare(t, ds["test_branch_process"], infoItem):
			assertNewProcessInfo(t, ds["test_branch_process"], infoItem)
		default:
			assert.FailNow(t, fmt.Sprintf("undefined info: %s/%s/%s", infoItem.Layer.String(), infoItem.Name, infoItem.Branch))
		}
	}
}

func TestFirstDeployment(t *testing.T) {
	test.RunUp(t)
	h, statusS := prepare(t)

	ds := firstDeployments(t)

	statuses, err := statusS.ListByServiceName(t.Name())
	require.NoError(t, err)
	fullInfo, err := h.InfoByStatuses(t.Name(), statuses)
	require.NoError(t, err)
	require.Len(t, fullInfo, 3)

	for _, infoItem := range fullInfo {
		switch {
		case compare(t, ds["inProgress_test"], infoItem):
			assertNewProcessInfo(t, ds["inProgress_test"], infoItem)
		case compare(t, ds["inProgress_prod"], infoItem):
			assertNewProcessInfo(t, ds["inProgress_prod"], infoItem)
		case compare(t, ds["inProgress_branch"], infoItem):
			assertNewProcessInfo(t, ds["inProgress_branch"], infoItem)
		default:
			assert.FailNow(t, fmt.Sprintf("undefined info: %s/%s/%s", infoItem.Layer.String(), infoItem.Name, infoItem.Branch))
		}
	}

}

func TestCanary(t *testing.T) {
	test.RunUp(t)
	log := test.NewLogger(t)
	db := test_db.NewDb(t)
	dStore := model.NewStorage(db, log)
	h, statusS := prepare(t)
	example := newDeployment(t)
	ds := simple(t, example)
	branchProcess := copyD(t, example)
	branchProcess.Layer = common.Prod
	branchProcess.State = model.CanaryProcess
	require.NoError(t, dStore.Save(branchProcess))
	ds["canary_process"] = branchProcess

	statuses, err := statusS.ListByServiceName(t.Name())
	require.NoError(t, err)
	fullInfo, err := h.InfoByStatuses(t.Name(), statuses)
	require.NoError(t, err)

	require.Len(t, fullInfo, 2)
	for _, infoItem := range fullInfo {
		switch {
		case compare(t, ds["test"], infoItem):
			assertOldInfo(t, ds["test"], ds["second_test"], infoItem)
		case compare(t, ds["prod"], infoItem):
			assertOldProcessInfo(t, ds["prod"], ds["second_prod"], ds["canary_process"], infoItem)
		default:
			assert.FailNow(t, fmt.Sprintf("undefined info: %s/%s/%s", infoItem.Layer.String(), infoItem.Name, infoItem.Branch))
		}
	}
}

func TestCanarySuccess(t *testing.T) {
	test.RunUp(t)
	log := test.NewLogger(t)
	db := test_db.NewDb(t)
	statusStore := status.NewStorage(db, log)
	dStore := model.NewStorage(db, log)
	h, statusS := prepare(t)
	example := newDeployment(t)
	ds := simple(t, example)
	canary := copyD(t, example)
	canary.Layer = common.Prod
	canary.State = model.CanarySuccess
	require.NoError(t, dStore.Save(canary))
	ds["canary"] = canary
	newStatus(t, statusStore, canary)

	statuses, err := statusS.ListByServiceName(t.Name())
	require.NoError(t, err)
	fullInfo, err := h.InfoByStatuses(t.Name(), statuses)
	require.NoError(t, err)

	require.Len(t, fullInfo, 2)
	for _, infoItem := range fullInfo {
		switch {
		case compare(t, ds["test"], infoItem):
			assertOldInfo(t, ds["test"], ds["second_test"], infoItem)
		case compare(t, ds["prod"], infoItem):
			assertOldProcessInfo(t, ds["prod"], ds["second_prod"], ds["canary"], infoItem)
		default:
			assert.FailNow(t, fmt.Sprintf("undefined info: %s/%s/%s", infoItem.Layer.String(), infoItem.Name, infoItem.Branch))
		}
	}
}

func TestCanaryOnePercent(t *testing.T) {
	test.RunUp(t)
	log := test.NewLogger(t)
	db := test_db.NewDb(t)
	statusStore := status.NewStorage(db, log)
	dStore := model.NewStorage(db, log)
	h, statusS := prepare(t)
	example := newDeployment(t)
	ds := simple(t, example)
	canary := copyD(t, example)
	canary.Layer = common.Prod
	canary.State = model.CanaryOnePercent
	require.NoError(t, dStore.Save(canary))
	ds["canary"] = canary
	newStatus(t, statusStore, canary)

	statuses, err := statusS.ListByServiceName(t.Name())
	require.NoError(t, err)
	fullInfo, err := h.InfoByStatuses(t.Name(), statuses)
	require.NoError(t, err)

	require.Len(t, fullInfo, 2)
	for _, infoItem := range fullInfo {
		switch {
		case compare(t, ds["test"], infoItem):
			assertOldInfo(t, ds["test"], ds["second_test"], infoItem)
		case compare(t, ds["prod"], infoItem):
			assertOldProcessInfo(t, ds["prod"], ds["second_prod"], ds["canary"], infoItem)
		default:
			assert.FailNow(t, fmt.Sprintf("undefined info: %s/%s/%s", infoItem.Layer.String(), infoItem.Name, infoItem.Branch))
		}
	}
}

func TestHelper_InfoByStatuses(t *testing.T) {
	test.RunUp(t)
	log := test.NewLogger(t)
	db := test_db.NewDb(t)
	statusStore := status.NewStorage(db, log)
	dStore := model.NewStorage(db, log)
	h, statusS := prepare(t)
	example := newDeployment(t)
	ds := branches(t, example)
	canary := copyD(t, example)
	canary.Layer = common.Prod
	canary.State = model.CanarySuccess
	require.NoError(t, dStore.Save(canary))
	ds["canary"] = canary
	newStatus(t, statusStore, canary)

	running, err := statusS.GetAllRunning(true)
	require.NoError(t, err)
	require.Len(t, running, 7)
	dsInfo, err := h.BalancerInfoByStatuses(running)
	require.NoError(t, err)

	require.Len(t, dsInfo, 7)
	for _, dInfo := range dsInfo {
		d := dInfo.Deployment
		switch {
		case compareDInfo(t, ds["test"], dInfo):
			assertBalancerInfo(t, ds["test"], dInfo)
		case compareDInfo(t, ds["prod"], dInfo):
			assertBalancerInfo(t, ds["prod"], dInfo)
		case compareDInfo(t, ds["branch_production"], dInfo):
			assertBalancerInfo(t, ds["branch_production"], dInfo)
			assertTraffic(t, traffic.Traffic_ONE_PERCENT, dInfo.BalancerInfo.Traffic)
		case compareDInfo(t, ds["test_branch_1"], dInfo):
			assertBalancerInfo(t, ds["test_branch_1"], dInfo)
			assertTraffic(t, traffic.Traffic_ONE_PERCENT, dInfo.BalancerInfo.Traffic)
		case compareDInfo(t, ds["test_branch_2"], dInfo):
			assertBalancerInfo(t, ds["test_branch_2"], dInfo)
			assertTraffic(t, traffic.Traffic_ONE_PERCENT, dInfo.BalancerInfo.Traffic)
		case compareDInfo(t, ds["prod_branch"], dInfo):
			assertBalancerInfo(t, ds["prod_branch"], dInfo)
			assertTraffic(t, traffic.Traffic_ONE_PERCENT, dInfo.BalancerInfo.Traffic)
		case compareDInfo(t, ds["canary"], dInfo):
			assertBalancerInfo(t, ds["canary"], dInfo)
			assertTraffic(t, traffic.Traffic_REAL, dInfo.BalancerInfo.Traffic)
		default:
			assert.FailNow(t, fmt.Sprintf("undefined info: %s/%s/%s", d.Layer.String(), d.ServiceName, d.Branch))
		}
	}
}

func assertTraffic(t *testing.T, want, actual traffic.Traffic) {
	assert.Equal(t, want.String(), actual.String())
}

func assertBalancerInfo(t *testing.T, m *model.Deployment, i *info.DeploymentInfo) {
	d := i.Deployment
	require.NotNil(t, i)
	require.NotNil(t, m)
	iID, err := strconv.Atoi(i.Deployment.Id)
	require.NoError(t, err)
	assert.Equal(t, d.ID(), int64(iID))
	require.NotNil(t, i.BalancerInfo)
	assert.Equal(t, m.Branch, d.Branch)
	assert.Equal(t, m.Name, d.ServiceName)
	assert.Equal(t, m.Layer, d.Layer.GetCommonLayer())
	// check not  preload for balancer status
	assert.Nil(t, m.Author)
	assert.Nil(t, m.Approve)
	assert.Nil(t, m.Issues)
}

func TestSox(t *testing.T) {
	test.RunUp(t)
	log := test.NewLogger(t)
	db := test_db.NewDb(t)
	dStore := model.NewStorage(db, log)
	h, statusS := prepare(t)
	example := newDeployment(t)
	ds := simple(t, example)
	branchProcess := copyD(t, example)
	branchProcess.Layer = common.Prod
	branchProcess.State = model.WaitApprove
	require.NoError(t, dStore.Save(branchProcess))
	ds["sox"] = branchProcess

	statuses, err := statusS.ListByServiceName(t.Name())
	require.NoError(t, err)
	fullInfo, err := h.InfoByStatuses(t.Name(), statuses)
	require.NoError(t, err)

	require.Len(t, fullInfo, 2)
	for _, infoItem := range fullInfo {
		switch {
		case compare(t, ds["test"], infoItem):
			assertOldInfo(t, ds["test"], ds["second_test"], infoItem)
		case compare(t, ds["prod"], infoItem):
			assertOldProcessInfo(t, ds["prod"], ds["second_prod"], ds["sox"], infoItem)
		default:
			assert.FailNow(t, fmt.Sprintf("undefined info: %s/%s/%s", infoItem.Layer.String(), infoItem.Name, infoItem.Branch))
		}
	}
}

func TestBranches(t *testing.T) {
	test.RunUp(t)
	h, statusS := prepare(t)
	ds := branches(t, newDeployment(t))
	statuses, err := statusS.ListByServiceName(t.Name())
	require.NoError(t, err)
	fullInfo, err := h.InfoByStatuses(t.Name(), statuses)
	require.NoError(t, err)

	require.Len(t, fullInfo, 6)
	for _, infoItem := range fullInfo {
		switch {
		case compare(t, ds["test"], infoItem):
			assertOldInfo(t, ds["test"], ds["second_test"], infoItem)
		case compare(t, ds["prod"], infoItem):
			assertOldInfo(t, ds["prod"], ds["second_prod"], infoItem)
		case compare(t, ds["branch_production"], infoItem):
			assertOldInfo(t, ds["branch_production"], ds["second_branch_production"], infoItem)
		case compare(t, ds["test_branch_1"], infoItem):
			assertNewInfo(t, ds["test_branch_1"], infoItem)
		case compare(t, ds["test_branch_2"], infoItem):
			assertNewInfo(t, ds["test_branch_2"], infoItem)
		case compare(t, ds["prod_branch"], infoItem):
			assertNewInfo(t, ds["prod_branch"], infoItem)
		default:
			assert.FailNow(t, fmt.Sprintf("undefined info: %s/%s/%s", infoItem.Layer.String(), infoItem.Name, infoItem.Branch))
		}
	}
}

func TestHelper_DeploymentByModel(t *testing.T) {
	t.Run("empty_assoc", func(t *testing.T) {
		m := &model.Deployment{}
		h, _ := prepare(t)
		result, err := h.DeploymentByModel(m)
		require.NoError(t, err)
		assert.Equal(t, "", result.ServiceName)
	})
}

func compare(t *testing.T, d *model.Deployment, infoItem *deploy2.Info) bool {
	return infoItem.Layer == layer.FromCommonLayer(d.Layer) && infoItem.Name == t.Name() && infoItem.Branch == d.Branch
}

func compareDInfo(t *testing.T, d *model.Deployment, infoItem *info.DeploymentInfo) bool {
	infoD := infoItem.Deployment
	return infoD.Layer == layer.FromCommonLayer(d.Layer) && infoD.ServiceName == t.Name() &&
		infoD.Branch == d.Branch && d.GetStateProto() == infoD.State
}

func assertNewInfo(t *testing.T, d *model.Deployment, infoItem *deploy2.Info) {
	assertDInfo(t, d, infoItem.Now)
	assert.Nil(t, infoItem.New)
	assert.Nil(t, infoItem.Child)
	assert.Nil(t, infoItem.Previous)
	assert.Nil(t, infoItem.Last)
}

func assertOldInfo(t *testing.T, d, previous *model.Deployment, infoItem *deploy2.Info) {
	assertDInfo(t, d, infoItem.Now)
	assertDInfo(t, previous, infoItem.Previous)
	assertDInfo(t, previous, infoItem.Last)
	assert.Nil(t, infoItem.New)
	assert.Nil(t, infoItem.Child)
}

func assertOldProcessInfo(t *testing.T, d, previous, new *model.Deployment, infoItem *deploy2.Info) {
	assertDInfo(t, d, infoItem.Now)
	assertDInfo(t, previous, infoItem.Previous)
	assertDInfo(t, previous, infoItem.Last)
	assertDInfo(t, new, infoItem.New)
	assert.Nil(t, infoItem.Child)
}

func assertNewProcessInfo(t *testing.T, new *model.Deployment, infoItem *deploy2.Info) {
	assert.Nil(t, infoItem.Now)
	assertDInfo(t, new, infoItem.New)
	assert.Nil(t, infoItem.Child)
	assert.Nil(t, infoItem.Previous)
	assert.Nil(t, infoItem.Last)
}

func assertDInfo(t *testing.T, d *model.Deployment, info *info.DeploymentInfo) {
	require.NotNil(t, info)
	require.NotNil(t, d)
	assert.NotEmpty(t, info.GetMapUrl())
	assert.NotEmpty(t, info.GetManifestUrl())
	iID, err := strconv.Atoi(info.Deployment.Id)
	require.NoError(t, err)
	assert.Equal(t, d.ID, int64(iID))
}

func prepare(t *testing.T) (*transformer.Helper, *status.Service) {
	log := test.NewLogger(t)
	db := test_db.NewDb(t)
	sMapS := service_map.NewService(db, log, service_change.NewNotificationMock())
	statusS := status.NewService(db, log, sMapS)
	// for create table
	ts := tracker.NewService(db, log, nil, nil)
	issue_link.NewService(db, log, ts)
	staffApi := staffapi.NewApi(staffapi.NewConf(), log)
	staffService := staff.NewService(db, staffApi, log)
	approve.NewService(db, log, staffService)

	dataSvc := data.NewService(db, log)
	h := transformer.NewHelper(dataSvc, db.GormDb, log)
	return h, statusS
}

func copyD(t *testing.T, d *model.Deployment) *model.Deployment {
	return &model.Deployment{
		Name:             d.Name,
		Branch:           d.Branch,
		DeployManifestID: d.DeployManifestID,
		ServiceMapsID:    d.ServiceMapsID,
		AuthorID:         d.AuthorID,
		Layer:            d.Layer,
		Version:          strconv.Itoa(r.Int()),
		StartDate:        time.Now().Add(-10 * time.Second),
		EndDate:          time.Now(),
		State:            model.Success,
		Type:             common.Run,
		Status:           successStatus(),
	}
}

func successStatus() *dProto.Status {
	return &dProto.Status{
		Numbers: []*dProto.Number{
			{
				Dc:            "sas",
				Total:         2,
				Placed:        2,
				SuccessPlaced: 2,
			},
			{
				Dc:            "myt",
				Total:         2,
				Placed:        2,
				SuccessPlaced: 2,
			},
		},
		Provides: []*dProto.ProvideStatus{
			{
				Provide: consul.MonitoringProvide,
				Status:  true,
			},
		},
	}
}

func newDeployment(t *testing.T) *model.Deployment {
	name := t.Name()
	log := test.NewLogger(t)
	db := test_db.NewDb(t)
	sMapS := service_map.NewService(db, log, service_change.NewNotificationMock())
	includeS := include.NewService(db, log)
	manifestS := manifest.NewService(db, log, parser.NewService(log, nil), includeS)
	staffApi := staffapi.NewApi(staffapi.NewConf(), log)
	staffService := staff.NewService(db, staffApi, log)
	require.NoError(t, sMapS.ReadAndSave([]byte(fmt.Sprintf(sMapYml, name)), 10, fmt.Sprintf("maps/%s.yml", name)))
	require.NoError(t, manifestS.ReadAndSave([]byte(fmt.Sprintf(manifestYml, name)), 10, fmt.Sprintf("deploy/%s.yml", name)))
	_, sMapID, err := sMapS.GetByFullPath(fmt.Sprintf("maps/%s.yml", name))
	require.NoError(t, err)
	_, manifestID, err := manifestS.GetByNameWithId(common.Prod, name)
	require.NoError(t, err)
	user, err := staffService.GetByLogin("danevge")
	require.NoError(t, err)
	return &model.Deployment{
		Name:             name,
		DeployManifestID: manifestID,
		ServiceMapsID:    sMapID,
		AuthorID:         user.ID,
		Layer:            common.Prod,
		Version:          "0.0.1",
		StartDate:        time.Now().Add(-10 * time.Second),
		EndDate:          time.Now(),
		State:            model.Success,
		Type:             common.Run,
		Status:           successStatus(),
	}
}

func branches(t *testing.T, d *model.Deployment) map[string]*model.Deployment {
	log := test.NewLogger(t)
	db := test_db.NewDb(t)
	s := model.NewStorage(db, log)
	statusStore := status.NewStorage(db, log)
	result := simple(t, d)

	firstProdBranch := copyD(t, d)
	firstProdBranch.Layer = common.Test
	firstProdBranch.Branch = "production"
	require.NoError(t, s.Save(firstProdBranch))
	result["first_branch_production"] = firstProdBranch

	secondProdBranch := copyD(t, firstProdBranch)
	secondProdBranch.PreviousId = firstProdBranch.ID
	require.NoError(t, s.Save(secondProdBranch))
	result["second_branch_production"] = secondProdBranch

	finalProdBranch := copyD(t, secondProdBranch)
	finalProdBranch.PreviousId = secondProdBranch.ID
	require.NoError(t, s.Save(finalProdBranch))
	result["branch_production"] = finalProdBranch
	newStatus(t, statusStore, finalProdBranch)

	testBranch1 := copyD(t, secondProdBranch)
	testBranch1.Branch = "b1"
	require.NoError(t, s.Save(testBranch1))
	result["test_branch_1"] = testBranch1
	newStatus(t, statusStore, testBranch1)

	testBranch2 := copyD(t, secondProdBranch)
	testBranch2.Branch = "b2"
	require.NoError(t, s.Save(testBranch2))
	result["test_branch_2"] = testBranch2
	newStatus(t, statusStore, testBranch2)

	prodBranch := copyD(t, secondProdBranch)
	prodBranch.Branch = "b3"
	prodBranch.Layer = common.Prod
	require.NoError(t, s.Save(prodBranch))
	result["prod_branch"] = prodBranch
	newStatus(t, statusStore, prodBranch)

	return result
}

func simple(t *testing.T, example *model.Deployment) map[string]*model.Deployment {
	log := test.NewLogger(t)
	db := test_db.NewDb(t)
	s := model.NewStorage(db, log)
	statusStore := status.NewStorage(db, log)
	result := map[string]*model.Deployment{}

	firstTest := copyD(t, example)
	firstTest.Layer = common.Test
	require.NoError(t, s.Save(firstTest))
	result["first_test"] = firstTest

	// second Test Deployment
	secondTest := copyD(t, firstTest)
	secondTest.PreviousId = firstTest.ID
	require.NoError(t, s.Save(secondTest))
	result["second_test"] = secondTest

	// final Test Deployment
	finalTest := copyD(t, secondTest)
	finalTest.PreviousId = secondTest.ID
	require.NoError(t, s.Save(finalTest))
	result["test"] = finalTest
	newStatus(t, statusStore, finalTest)

	firstProd := copyD(t, example)
	firstProd.Layer = common.Prod
	require.NoError(t, s.Save(firstProd))
	result["first_prod"] = firstProd

	// second Test Deployment
	secondProd := copyD(t, firstProd)
	secondProd.PreviousId = firstProd.ID
	require.NoError(t, s.Save(secondProd))
	result["second_prod"] = secondProd

	// final Test Deployment
	finalProd := copyD(t, secondProd)
	finalProd.PreviousId = secondProd.ID
	require.NoError(t, s.Save(finalProd))
	result["prod"] = finalProd
	newStatus(t, statusStore, finalProd)

	return result
}

func firstDeployments(t *testing.T) map[string]*model.Deployment {
	log := test.NewLogger(t)
	db := test_db.NewDb(t)
	s := model.NewStorage(db, log)
	result := map[string]*model.Deployment{}

	d := newDeployment(t)

	inProgressTest := copyD(t, d)
	inProgressTest.Layer = common.Test
	inProgressTest.State = model.Process
	require.NoError(t, s.Save(inProgressTest))
	result["inProgress_test"] = inProgressTest

	inProgressProd := copyD(t, d)
	inProgressProd.State = model.Process
	require.NoError(t, s.Save(inProgressProd))
	result["inProgress_prod"] = inProgressProd

	inProgressBranch := copyD(t, d)
	inProgressBranch.Branch = "br1"
	inProgressBranch.State = model.Process
	require.NoError(t, s.Save(inProgressBranch))
	result["inProgress_branch"] = inProgressBranch

	return result
}

func newStatus(t *testing.T, s *status.Storage, d *model.Deployment) {
	tr := traffic.Traffic_UNKNOWN
	switch {
	case d.Branch != "":
		tr = traffic.Traffic_ONE_PERCENT
	case d.State.IsCanary():
		tr = traffic.Traffic_REAL
	}
	require.NoError(t, s.Save(&status.Status{
		DeploymentID:    d.ID,
		State:           status.StateRunning,
		Layer:           d.Layer,
		Name:            d.Name,
		Branch:          d.Branch,
		Version:         d.Version,
		MainDomainNames: "domain.name",
		Canary:          d.State.IsCanary(),
		Traffic:         tr,
	}))
}
