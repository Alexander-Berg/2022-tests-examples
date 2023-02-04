package model_test

import (
	"errors"
	"fmt"
	"testing"
	"time"

	"github.com/YandexClassifieds/shiva/cmd/shiva/approve"
	"github.com/YandexClassifieds/shiva/cmd/shiva/deployment/data"
	"github.com/YandexClassifieds/shiva/cmd/shiva/deployment/issue_link"
	"github.com/YandexClassifieds/shiva/cmd/shiva/deployment/model"
	"github.com/YandexClassifieds/shiva/cmd/shiva/scheduler/consul"
	"github.com/YandexClassifieds/shiva/common"
	"github.com/YandexClassifieds/shiva/common/storage"
	"github.com/YandexClassifieds/shiva/pb/shiva/types/deployment"
	"github.com/YandexClassifieds/shiva/pb/shiva/types/revert"
	"github.com/YandexClassifieds/shiva/pb/shiva/types/traffic"
	"github.com/YandexClassifieds/shiva/pkg/include"
	"github.com/YandexClassifieds/shiva/pkg/manifest"
	"github.com/YandexClassifieds/shiva/pkg/manifest/parser"
	"github.com/YandexClassifieds/shiva/pkg/service_map"
	"github.com/YandexClassifieds/shiva/pkg/staff"
	staffapi "github.com/YandexClassifieds/shiva/pkg/staff/api"
	"github.com/YandexClassifieds/shiva/pkg/tracker"
	stAPI "github.com/YandexClassifieds/shiva/pkg/tracker/api"
	"github.com/YandexClassifieds/shiva/pkg/tracker/model/issue"
	"github.com/YandexClassifieds/shiva/test"
	"github.com/YandexClassifieds/shiva/test/mock/service_change"
	"github.com/YandexClassifieds/shiva/test/test_db"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
)

const (
	sMapYml = `
name: %s
owners:
  - https://staff.yandex-team.ru/danevge
  - https://staff.yandex-team.ru/alexander-s
`
	sMapYml2 = `
name: %s
description: update
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
	manifestYml2 = `
name: %s
test:
  datacenters:
    myt: 
      count: 3
    sas:
      count: 3
prod:
  datacenters:
    myt:
      count: 3
    sas:
      count: 3
`
)

func TestStorage_ErrNotFound(t *testing.T) {
	test.RunUp(t)
	db := test_db.NewDb(t)
	storage := model.NewStorage(db, test.NewLogger(t))
	t.Run("not_found", func(t *testing.T) {
		_, err := storage.Get(-1)
		assert.Error(t, err)
		assert.True(t, errors.Is(err, common.ErrNotFound))
	})
}

func TestStorage_List(t *testing.T) {
	name := "ListTest"
	test.InitTestEnv()
	db := test_db.NewSeparatedDb(t)

	log := test.NewLogger(t)
	// newService for create table
	tracker.NewService(db, log, nil, nil)
	staffApi := staffapi.NewApi(staffapi.NewConf(), log)
	staffService := staff.NewService(db, staffApi, log)
	user, err := staffService.GetByLogin("danevge")
	require.NoError(t, err)
	user2, err := staffService.GetByLogin("alexander-s")
	require.NoError(t, err)
	approve.NewService(db, log, nil)
	sMapS := service_map.NewService(db, log, service_change.NewNotificationMock())
	includeService := include.NewService(db, log)
	mS := manifest.NewService(db, log, parser.NewService(log, nil), includeService)
	require.NoError(t, sMapS.ReadAndSave([]byte(fmt.Sprintf(sMapYml, name)), 10, fmt.Sprintf("maps/%s.yml", name)))
	require.NoError(t, mS.ReadAndSave([]byte(fmt.Sprintf(manifestYml, name)), 10, fmt.Sprintf("deploy/%s.yml", name)))
	_, sMapID, err := sMapS.GetByFullPath(fmt.Sprintf("maps/%s.yml", name))
	require.NoError(t, err)
	_, mID, err := mS.GetByNameWithId(common.Test, name)
	require.NoError(t, err)
	storage := model.NewStorage(db, test.NewLogger(t))
	baseTs := time.Date(2020, 02, 03, 5, 0, 3, 0, time.UTC)
	fixture := []*model.Deployment{
		{Name: "svc1", AuthorID: user.ID, ServiceMapsID: sMapID, DeployManifestID: mID, Branch: "", Layer: common.Prod, Type: common.Run, State: model.Success, StartDate: baseTs, EndDate: baseTs}, // should be matched
		{Name: "svc1", AuthorID: user.ID, ServiceMapsID: sMapID, DeployManifestID: mID, Branch: "", Layer: common.Prod, Type: common.Run, State: model.Success, StartDate: baseTs.Add(-time.Minute * 30)},
		{Name: "svc1", AuthorID: user2.ID, ServiceMapsID: sMapID, DeployManifestID: mID, Branch: "", Layer: common.Prod, Type: common.Run, State: model.Success, StartDate: baseTs.Add(time.Minute * 30)},
		{Name: "svc1", AuthorID: user2.ID, ServiceMapsID: sMapID, DeployManifestID: mID, Branch: "b1", Layer: common.Prod, Type: common.Run, State: model.Success, StartDate: baseTs.Add(time.Minute), EndDate: baseTs.Add(time.Minute)}, // should be matched
		{Name: "svc1", AuthorID: user2.ID, ServiceMapsID: sMapID, DeployManifestID: mID, Branch: "", Layer: common.Test, Type: common.Run, State: model.Success, StartDate: baseTs},
		{Name: "svc1", AuthorID: user.ID, ServiceMapsID: sMapID, DeployManifestID: mID, Branch: "", Layer: common.Prod, Type: common.Cancel, State: model.Success, StartDate: baseTs},
		{Name: "svc1", AuthorID: user2.ID, ServiceMapsID: sMapID, DeployManifestID: mID, Branch: "", Layer: common.Prod, Type: common.Run, State: model.Prepare, StartDate: baseTs.Add(time.Hour)},
		{Name: "svc1", AuthorID: user2.ID, ServiceMapsID: sMapID, DeployManifestID: mID, Branch: "b1"},
		{Name: "svc2", AuthorID: user2.ID, ServiceMapsID: sMapID, DeployManifestID: mID, Branch: "", Layer: common.Prod, Type: common.Run, State: model.Success, StartDate: baseTs},
		{Name: "svc2", AuthorID: user.ID, ServiceMapsID: sMapID, DeployManifestID: mID, Branch: "b1", Layer: common.Prod, Type: common.Run, State: model.Success, StartDate: baseTs},
	}
	for _, d := range fixture {
		require.NoError(t, storage.Save(d))
	}

	t.Run("all", func(t *testing.T) {
		lst, err := storage.List(model.ListQueryParams{})
		require.NoError(t, err)
		assert.True(t, len(lst) > 0)
		for _, d := range lst {
			assert.NotNil(t, d.Author)
			assert.Nil(t, d.Manifest)
			assert.Nil(t, d.ServiceMap)
		}
	})
	t.Run("svc", func(t *testing.T) {
		lst, err := storage.List(model.ListQueryParams{
			Layer:        common.Prod,
			Names:        []string{"svc1"},
			Branch:       "",
			Types:        []common.Type{common.Run, common.Promote},
			States:       model.EndStates(),
			MinStartTime: baseTs,
			MaxStartTime: baseTs.Add(time.Minute * 10),
		})
		require.NoError(t, err)
		require.Len(t, lst, 2)
		assert.Equal(t, "svc1", lst[0].Name)
		assert.Equal(t, "svc1", lst[1].Name)
		assert.True(t, lst[0].StartDate.After(lst[1].StartDate), "result order mismatch: %s %s", lst[0].StartDate, lst[1].StartDate)
	})
	t.Run("related", func(t *testing.T) {
		i := &issue.Issue{Key: "VOID-42"}
		require.NoError(t, db.GormDb.Create(i).Error)
		dr := &model.Deployment{
			Name:             "svc-related",
			AuthorID:         user.ID,
			ServiceMapsID:    sMapID,
			DeployManifestID: mID,
		}
		require.NoError(t, storage.Save(dr))
		require.NoError(t, db.GormDb.Create(&approve.Approve{DeploymentId: dr.ID, StaffID: user2.ID}).Error)

		lst, err := storage.List(model.ListQueryParams{Names: []string{"svc-related"}})
		require.NoError(t, err)
		require.Len(t, lst, 1)
		require.NotNil(t, lst[0].Approve)
		assert.Equal(t, user2.ID, lst[0].Approve.StaffID)
		assert.Equal(t, user2.Login, lst[0].GetApprover())
		assert.Equal(t, user.Login, lst[0].GetAuthorLogin())
	})
	t.Run("only_branches", func(t *testing.T) {
		lst, err := storage.List(model.ListQueryParams{
			Layer:      common.Prod,
			Names:      []string{"svc1"},
			BranchType: model.BranchOnly,
		})
		require.NoError(t, err)
		assert.Len(t, lst, 1)
	})
	t.Run("only_main", func(t *testing.T) {
		lst, err := storage.List(model.ListQueryParams{
			Layer:      common.Prod,
			Names:      []string{"svc1"},
			BranchType: model.BranchMain,
		})
		require.NoError(t, err)
		assert.Len(t, lst, 5)
		for _, v := range lst {
			assert.Equal(t, "", v.Branch)
		}
	})
	t.Run("branch", func(t *testing.T) {
		lst, err := storage.List(model.ListQueryParams{
			Layer:  common.Prod,
			Names:  []string{"svc1"},
			Branch: "b1",
		})
		require.NoError(t, err)
		assert.Len(t, lst, 1)
	})
	t.Run("branch_case", func(t *testing.T) {
		lst, err := storage.List(model.ListQueryParams{
			Layer:  common.Prod,
			Names:  []string{"svc1"},
			Branch: "B1", // branch search lowercase
		})
		require.NoError(t, err)
		assert.Len(t, lst, 1)
	})
}

func TestStorage_Count(t *testing.T) {
	test.RunUp(t)
	db := test_db.NewDb(t)
	db.GormDb.Model(&staff.User{})
	storage := model.NewStorage(db, test.NewLogger(t))
	baseTs := time.Now()
	fixture := []*model.Deployment{
		{Name: "svc1", State: model.Success, StartDate: baseTs.Add(-2 * time.Hour)},
		{Name: "svc1", State: model.Process, StartDate: baseTs.Add(-time.Hour)},
		{Name: "svc3", State: model.Process, StartDate: baseTs.Add(-time.Hour)},
		{Name: "svc3", State: model.Process, StartDate: baseTs},
	}
	for _, d := range fixture {
		require.NoError(t, storage.Save(d))
	}
	cnt, err := storage.Count(model.ListQueryParams{
		States:       []model.State{model.Process},
		MaxStartTime: baseTs.Add(-30 * time.Minute),
	})
	require.NoError(t, err)
	assert.Equal(t, int64(2), cnt)
}

func TestStorage_UpdateState(t *testing.T) {
	test.RunUp(t)
	db := test_db.NewDb(t)
	storage := model.NewStorage(db, test.NewLogger(t))

	state := model.Success
	revertType := revert.RevertType_Terminate
	desc := "Test description"

	d := newDeployment()
	err := storage.Save(d)
	require.NoError(t, err)

	err = storage.UpdateState(d, state, revertType, desc)
	require.NoError(t, err)

	stanza, err := storage.Get(d.ID)
	require.NoError(t, err)
	assert.Equal(t, state, stanza.State)
	assert.Equal(t, revertType, stanza.RevertType)
	assert.Equal(t, desc, stanza.Description)
}

func newDeployment() *model.Deployment {
	return &model.Deployment{
		Name:             "svcUpdate",
		DeployManifestID: 1,
		ServiceMapsID:    2,
		AuthorID:         1,
		Version:          "44",
		StartDate:        time.Now().Add(-2 * time.Hour),
		Type:             common.Run,
		Overridden:       false,
		Traffic:          traffic.Traffic_UNKNOWN,
		Comment:          "Test comment",
		State:            model.Prepare,
	}
}

func TestStatus(t *testing.T) {
	test.RunUp(t)
	db := test_db.NewDb(t)
	storage := model.NewStorage(db, test.NewLogger(t))

	status := &deployment.Status{
		Numbers: []*deployment.Number{
			{
				Dc:            "sas",
				Total:         10,
				Placed:        5,
				SuccessPlaced: 4,
			},
			{
				Dc:            "sas",
				Total:         10,
				Placed:        6,
				SuccessPlaced: 5,
			},
		},
		Provides: []*deployment.ProvideStatus{
			{Provide: consul.MonitoringProvide,
				Status: true,
			},
		},
		Description: "Description text",
	}
	d := newDeployment()
	d.Status = status
	require.NoError(t, storage.Save(d))
	savedD, err := storage.Get(d.ID)
	require.NoError(t, err)
	require.NotNil(t, savedD.Status)
	require.NotNil(t, savedD.Status.Numbers)
	require.NotNil(t, savedD.Status.Provides)
	assert.Equal(t, status.Description, savedD.Status.Description)
	assert.Len(t, savedD.Status.Provides, 1)
	assert.Len(t, savedD.Status.Numbers, 2)
}

func TestOverrides(t *testing.T) {
	test.RunUp(t)
	db := test_db.NewDb(t)
	storage := model.NewStorage(db, test.NewLogger(t))

	d := newDeployment()
	d.Overrides = &model.Overrides{
		DC: map[string]int{
			"sas": 10,
		},
		CPU: 5,
	}
	require.NoError(t, storage.Save(d))
	savedD, err := storage.Get(d.ID)
	require.NoError(t, err)
	require.Equal(t, d.Overrides, savedD.Overrides)
}

func TestOverrides_nil(t *testing.T) {
	test.RunUp(t)
	db := test_db.NewDb(t)
	storage := model.NewStorage(db, test.NewLogger(t))

	d := newDeployment()
	d.Overrides = &model.Overrides{
		DC:  nil,
		CPU: 0,
	}
	require.NoError(t, storage.Save(d))
	savedD, err := storage.Get(d.ID)
	require.NoError(t, err)
	require.Equal(t, d.Overrides, savedD.Overrides)
}

func TestGetFull(t *testing.T) {
	name := "GetFull"
	test.RunUp(t)
	log := test.NewLogger(t)
	db := test_db.NewDb(t)
	storage := model.NewStorage(db, log)
	includeService := include.NewService(db, log)
	sMapS := service_map.NewService(db, log, service_change.NewNotificationMock())
	mS := manifest.NewService(db, log, parser.NewService(log, nil), includeService)

	d := prepareDeployment(t, db, name)
	full, err := storage.GetFull(d.ID)
	require.NoError(t, err)
	require.NotNil(t, full.Manifest)
	require.NotNil(t, full.ServiceMap)

	require.NoError(t, sMapS.ReadAndSave([]byte(fmt.Sprintf(sMapYml2, name)), 10, fmt.Sprintf("maps/%s.yml", name)))
	require.NoError(t, mS.ReadAndSave([]byte(fmt.Sprintf(manifestYml2, name)), 10, fmt.Sprintf("deploy/%s.yml", name)))
	full, err = storage.GetFull(d.ID)
	require.NoError(t, err)
	require.NotNil(t, full.Manifest)
	require.NotNil(t, full.ServiceMap)
	assert.NotNil(t, full.Author)
	assert.NotNil(t, full.Approve)
	assert.NotEmpty(t, full.Issues)
	assert.Len(t, full.Issues, 2)
	for _, i := range full.Issues {
		assert.NotNil(t, i)
	}
	time.Sleep(time.Second)
}

func TestGetFullWithCache(t *testing.T) {
	name := "GetFull"
	test.InitTestEnv()
	log := test.NewLogger(t)
	db := test_db.NewSeparatedDb(t)
	storage := model.NewStorage(db, log)

	d := prepareDeployment(t, db, name)
	full, err := storage.GetFull(d.ID)
	require.NoError(t, err)

	rewriteDeployment(t, storage, full)

	cachedFull, err := storage.GetFull(d.ID)
	require.NoError(t, err)

	full.UpdatedAt = cachedFull.UpdatedAt
	require.Equal(t, full, cachedFull)
}

func TestGetAll(t *testing.T) {
	name := "GetAll-%d"

	test.InitTestEnv()
	log := test.NewLogger(t)
	db := test_db.NewSeparatedDb(t)
	storage := model.NewStorage(db, log)

	var dIds []int64
	var expectedDeployments []*model.Deployment

	for i := 0; i < 3; i++ {
		d := prepareDeployment(t, db, fmt.Sprintf(name, i))
		dIds = append(dIds, d.ID)

		fullD := &model.Deployment{}
		err := db.GormDb.
			Joins("Manifest").
			Joins("ServiceMap").Where("deployment.id = ?", d.ID).Take(fullD).Error
		require.NoError(t, err)

		expectedDeployments = append(expectedDeployments, fullD)
	}

	//add to cache
	lite, err := storage.GetFull(dIds[0])
	require.NoError(t, err)

	rewriteDeployment(t, storage, lite)

	actualDeployments, err := storage.GetAll(dIds)
	require.NoError(t, err)

	for i := range expectedDeployments {
		actualDeployments[i].UpdatedAt = time.Time{}
		expectedDeployments[i].UpdatedAt = time.Time{}

		//clean full cache fields
		actualDeployments[i].Issues = nil
		actualDeployments[i].Author = nil
		actualDeployments[i].Approve = nil
	}

	require.ElementsMatch(t, expectedDeployments, actualDeployments)
}

func TestGetFullWithLiteCache(t *testing.T) {
	name := "GetFull"
	test.InitTestEnv()
	log := test.NewLogger(t)
	db := test_db.NewSeparatedDb(t)
	storage := model.NewStorage(db, log)

	d := prepareDeployment(t, db, name)

	//lite caching
	lite, err := storage.GetAll([]int64{d.ID})
	require.NoError(t, err)

	rewriteDeployment(t, storage, lite[0])

	expectedDeployment := &model.Deployment{}
	require.NoError(t, storage.Preload(nil, nil).GetById("", expectedDeployment, d.ID))

	actualDeployment, err := storage.GetFull(d.ID)

	actualDeployment.UpdatedAt = expectedDeployment.UpdatedAt
	require.Equal(t, expectedDeployment, actualDeployment)
}

func rewriteDeployment(t *testing.T, s *model.Storage, d *model.Deployment) {
	copyD := &model.Deployment{}
	*copyD = *d

	copyD.ServiceMap = nil
	require.NoError(t, s.Save(copyD))
}

func TestGetByParent(t *testing.T) {
	name := "GetFull"
	test.RunUp(t)
	log := test.NewLogger(t)
	db := test_db.NewDb(t)
	storage := model.NewStorage(db, log)
	includeService := include.NewService(db, log)
	sMapS := service_map.NewService(db, log, service_change.NewNotificationMock())
	mS := manifest.NewService(db, log, parser.NewService(log, nil), includeService)
	require.NoError(t, sMapS.ReadAndSave([]byte(fmt.Sprintf(sMapYml, name)), 10, fmt.Sprintf("maps/%s.yml", name)))
	require.NoError(t, mS.ReadAndSave([]byte(fmt.Sprintf(manifestYml, name)), 10, fmt.Sprintf("deploy/%s.yml", name)))
	_, sMapID, err := sMapS.GetByFullPath(fmt.Sprintf("maps/%s.yml", name))
	require.NoError(t, err)
	_, mID, err := mS.GetByNameWithId(common.Test, name)
	require.NoError(t, err)

	d := newDeployment()
	d.ParentId = 999999
	d.ServiceMapsID = sMapID
	d.DeployManifestID = mID
	require.NoError(t, storage.Save(d))
	full, err := storage.GetByParent(d.ParentId)
	require.NoError(t, err)
	require.NotNil(t, full.Manifest)
	require.NotNil(t, full.ServiceMap)

	require.NoError(t, sMapS.ReadAndSave([]byte(fmt.Sprintf(sMapYml2, name)), 10, fmt.Sprintf("maps/%s.yml", name)))
	require.NoError(t, mS.ReadAndSave([]byte(fmt.Sprintf(manifestYml2, name)), 10, fmt.Sprintf("deploy/%s.yml", name)))
	full, err = storage.GetByParent(d.ParentId)
	require.NoError(t, err)
	require.NotNil(t, full.Manifest)
	require.NotNil(t, full.ServiceMap)
}

func prepareDeployment(t *testing.T, db *storage.Database, name string) *model.Deployment {
	log := test.NewLogger(t)
	storage := model.NewStorage(db, log)
	includeService := include.NewService(db, log)
	sMapS := service_map.NewService(db, log, service_change.NewNotificationMock())
	mS := manifest.NewService(db, log, parser.NewService(log, nil), includeService)
	require.NoError(t, sMapS.ReadAndSave([]byte(fmt.Sprintf(sMapYml, name)), 10, fmt.Sprintf("maps/%s.yml", name)))
	require.NoError(t, mS.ReadAndSave([]byte(fmt.Sprintf(manifestYml, name)), 10, fmt.Sprintf("deploy/%s.yml", name)))
	sMap, sMapID, err := sMapS.GetByFullPath(fmt.Sprintf("maps/%s.yml", name))
	require.NoError(t, err)
	_, mID, err := mS.GetByNameWithId(common.Test, name)
	require.NoError(t, err)
	staffApi := staffapi.NewApi(staffapi.NewConf(), log)
	staffService := staff.NewService(db, staffApi, log)
	author, err := staffService.GetByLogin("danevge")
	require.NoError(t, err)
	approver, err := staffService.GetByLogin("alexander-s")
	require.NoError(t, err)
	approveS := approve.NewService(db, log, staffService)
	trackerApi := stAPI.NewApi(stAPI.NewConf(), log)
	dataSvc := data.NewService(db, log)
	issueLinkS := issue_link.NewService(db, log, tracker.NewService(db, log, trackerApi, dataSvc))
	trackerS := tracker.NewService(db, log, trackerApi, dataSvc)
	i1, err := trackerS.GetIssueByKey("VOID-1")
	require.NoError(t, err)
	i2, err := trackerS.GetIssueByKey("VOID-2")
	require.NoError(t, err)

	d := &model.Deployment{
		Name:             "svcUpdate",
		DeployManifestID: mID,
		ServiceMapsID:    sMapID,
		AuthorID:         author.ID,
		Version:          "44",
		StartDate:        time.Now().Add(-2 * time.Hour),
		Type:             common.Run,
		Overridden:       false,
		Traffic:          traffic.Traffic_UNKNOWN,
		Comment:          "Test comment",
		State:            model.Prepare,
	}
	require.NoError(t, storage.Save(d))

	b, err := approveS.Approve(d.ID, "UNKNOWN", approver, sMap)
	require.NoError(t, err)
	require.True(t, b)
	require.NoError(t, issueLinkS.LinkByIssues(d.ID, []*issue.Issue{i1, i2}))

	return d
}
