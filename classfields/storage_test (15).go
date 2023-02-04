package status

import (
	"fmt"
	"sort"
	"strings"
	"testing"
	"time"

	dm "github.com/YandexClassifieds/shiva/cmd/shiva/deployment/model"
	"github.com/YandexClassifieds/shiva/common"
	"github.com/YandexClassifieds/shiva/pkg/manifest"
	"github.com/YandexClassifieds/shiva/pkg/service_map"
	"github.com/YandexClassifieds/shiva/pkg/staff"
	"github.com/YandexClassifieds/shiva/test"
	"github.com/YandexClassifieds/shiva/test/test_db"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
)

func TestStorage_ListByServiceName(t *testing.T) {
	var err error
	test.RunUp(t)
	db := test_db.NewDb(t)
	require.NoError(t, db.GormDb.AutoMigrate(Status{}, dm.Deployment{}, staff.User{}, service_map.Data{}, manifest.Data{}))

	ds := dm.NewStorage(db, test.NewLogger(t))
	dl := []dm.Deployment{
		{
			Layer: common.Test,
			Name:  "state-service",
			Type:  common.Run,
			State: dm.Success,
		},
		{
			Layer:  common.Test,
			Name:   "state-service",
			Branch: "b1",
			Type:   common.Run,
			State:  dm.Success,
		},
		{
			Layer: common.Prod,
			Name:  "state-service",
			Type:  common.Run,
			State: dm.Success,
		},
	}
	for _, d := range dl {
		require.NoError(t, ds.Save(&d))
	}

	s := NewStorage(db, test.NewLogger(t))
	u := staff.User{Login: "test-user"}
	require.NoError(t, db.GormDb.Save(&u).Error)
	models := []*Status{
		{
			State:        StateRunning,
			Layer:        common.Test,
			Name:         "state-service",
			DeploymentID: dl[0].ID,
		},
		{
			State:        StateRunning,
			Layer:        common.Test,
			Name:         "state-service",
			Branch:       "b1",
			DeploymentID: dl[1].ID,
		},
		{
			State:        StateRunning,
			Layer:        common.Prod,
			Name:         "state-service",
			DeploymentID: dl[2].ID,
		},
	}
	for _, m := range models {
		fmt.Println("tick")
		require.NoError(t, s.Save(m))
	}

	result, err := s.ListByServiceName("state-service")
	require.NoError(t, err)
	if !assert.Equal(t, len(models), len(result), "expected models and result len mismatch") {
		return
	}
	sort.Slice(result, func(i, j int) bool {
		return result[i].ID < result[j].ID
	})
	for i := 0; i < len(models); i++ {
		assert.Equal(t, models[i].DeploymentID, result[i].DeploymentID)
	}
}

func TestStorage_GetByService(t *testing.T) {
	test.InitTestEnv()
	db := test_db.NewDb(t)

	s := NewStorage(db, test.NewLogger(t))

	db.GormDb.Save(&Status{
		Layer: common.Prod,
		Name:  "svc1",
		State: StateRunning,
	})
	db.GormDb.Save(&Status{
		Layer:  common.Prod,
		Name:   "svc1",
		Branch: "SoMe-BrAnCh",
		State:  StateRunning,
	})

	st1, err := s.GetByService(common.Prod, "svc1", "some-branch")
	require.NoError(t, err)
	assert.NotNil(t, st1)

	st2, err := s.GetByService(common.Prod, "svc1", "SOME-BRANCH")
	require.NoError(t, err)
	assert.NotNil(t, st2)
}

func TestStorage_GetAllRunning(t *testing.T) {
	var err error
	test.RunUp(t)
	db := test_db.NewDb(t)
	db.GormDb.AutoMigrate(Status{}, dm.Deployment{}, staff.User{}, service_map.Data{}, manifest.Data{})

	ds := dm.NewStorage(db, test.NewLogger(t))
	dl := []dm.Deployment{
		{
			Layer: common.Test,
			Name:  "state-service",
			Type:  common.Run,
			State: dm.Success,
		},
		{
			Layer:  common.Test,
			Name:   "state-service",
			Branch: "b1",
			Type:   common.Run,
			State:  dm.Success,
		},
		{
			Layer: common.Prod,
			Name:  "state-service",
			Type:  common.Run,
			State: dm.Success,
		},
	}
	for _, d := range dl {
		require.NoError(t, ds.Save(&d))
	}

	s := NewStorage(db, test.NewLogger(t))
	u := staff.User{Login: "test-user"}
	require.NoError(t, db.GormDb.Save(&u).Error)
	models := []*Status{
		{
			State:        StateRunning,
			Layer:        common.Test,
			Name:         "state-service",
			DeploymentID: dl[0].ID,
		},
		{
			State:        StateNotRunning,
			Layer:        common.Test,
			Name:         "state-service",
			Branch:       "b1",
			DeploymentID: dl[1].ID,
		},
		{
			State:        StateRunning,
			Layer:        common.Prod,
			Name:         "state-service",
			DeploymentID: dl[2].ID,
		},
	}
	for _, m := range models {
		require.NoError(t, s.Save(m))
	}

	result, err := s.GetAllRunning()
	require.NoError(t, err)
	assert.Equal(t, 2, len(result))
}

func TestStorage_Save(t *testing.T) {
	t.Run("new", testSaveNew)
	t.Run("update_existing", testSaveExisting)
	t.Run("dedup", testSaveDedup)
	t.Run("error", testSaveError)
}

func testSaveNew(t *testing.T) {
	m := Status{
		DeploymentID: 42,
		State:        StateRunning,
		Layer:        common.Prod,
		Name:         "status-save-svc",
		Version:      "v42",
		EventTime:    time.Now(),
	}
	test.RunUp(t)
	db := test_db.NewDb(t)
	s := NewStorage(db, test.NewLogger(t))
	require.NoError(t, s.Save(&m))
	result, err := s.GetByService(m.Layer, m.Name, m.Branch)
	require.NoError(t, err)
	assert.Equal(t, m.ID, result.ID)
}

// TODO Что проверяет этот тест? Мб удалить?
func testSaveError(t *testing.T) {
	m := Status{
		DeploymentID: 42,
		State:        StateRunning,
		Layer:        common.Prod,
		Name:         "status-save-svc",
		Version:      strings.Repeat("1", 129),
		EventTime:    time.Now(),
	}
	test.InitTestEnv()
	db := test_db.NewDb(t)

	s := NewStorage(db, test.NewLogger(t))
	err := s.Save(&m)
	require.Error(t, err)
	assert.Contains(t, err.Error(), "current_state:Save()")
	assert.Contains(t, err.Error(), "value too long for type character varying(128)")
}

func testSaveExisting(t *testing.T) {
	test.RunUp(t)
	db := test_db.NewDb(t)
	db.GormDb.AutoMigrate(Status{}, dm.Deployment{})
	s := NewStorage(db, test.NewLogger(t))

	oldDeployment := dm.Deployment{Layer: common.Prod,
		Name:    "status-existing",
		State:   dm.Success,
		Type:    common.Run,
		EndDate: time.Now().Add(-time.Minute),
	}
	require.NoError(t, db.GormDb.Save(&oldDeployment).Error)
	newDeployment := dm.Deployment{
		Layer:   common.Prod,
		Name:    "status-existing",
		State:   dm.Success,
		Type:    common.Run,
		EndDate: time.Now(),
	}
	require.NoError(t, db.GormDb.Save(&newDeployment).Error)

	m := &Status{
		DeploymentID: oldDeployment.ID,
		Layer:        common.Prod,
		Name:         "status-existing",
	}
	require.NoError(t, s.Save(m))

	newStatus := &Status{
		Layer:        common.Prod,
		Name:         "status-existing",
		DeploymentID: newDeployment.ID,
	}
	require.NoError(t, s.Save(newStatus))

	result := &Status{}
	q := db.GormDb.Find(&result, "name = ? and layer = ? and branch = ''", newStatus.Name, newStatus.Layer)
	require.NoError(t, q.Error)
	assert.Equal(t, newDeployment.ID, result.DeploymentID)
}

func testSaveDedup(t *testing.T) {
	test.RunUp(t)
	db := test_db.NewDb(t)
	db.GormDb.AutoMigrate(Status{}, dm.Deployment{})
	s := NewStorage(db, test.NewLogger(t))

	now := time.Now()
	d1 := dm.Deployment{Name: "status-dedup", State: dm.Success, Type: common.Run, EndDate: now.Add(time.Minute)}
	d2 := dm.Deployment{Name: "status-dedup", State: dm.Success, Type: common.Run, EndDate: now}
	require.NoError(t, db.GormDb.Save(&d1).Error)
	require.NoError(t, db.GormDb.Save(&d2).Error)

	m := &Status{
		DeploymentID: d1.ID,
		Layer:        common.Prod,
		Name:         "status-dedup",
		EventTime:    d1.EndDate,
	}
	require.NoError(t, s.Save(m))

	newStatus := &Status{
		DeploymentID: d2.ID,
		Layer:        common.Prod,
		Name:         "status-dedup",
		EventTime:    d2.EndDate,
	}
	require.NoError(t, s.Save(newStatus))

	// check update deployment id
	result := &Status{}
	q := db.GormDb.Find(&result, "name = ? and layer = ? and branch = '' and canary = false", "status-dedup", common.Prod)
	require.NoError(t, q.Error)
	assert.Equal(t, d1.ID, result.DeploymentID)
}

func TestStorage_Save_BranchCase(t *testing.T) {
	test.InitTestEnv()
	db := test_db.NewSeparatedDb(t)

	s := NewStorage(db, test.NewLogger(t))

	err := s.Save(&Status{
		Layer:  common.Prod,
		Name:   "svc1",
		Branch: "QUEUE-1337",
		State:  StateRunning,
	})
	require.NoError(t, err)

	err = s.Save(&Status{
		Layer:  common.Prod,
		Name:   "svc1",
		Branch: "queue-1337",
		State:  StateRunning,
	})
	require.NoError(t, err)

	lst, err := s.ListByServiceName("svc1")
	require.NoError(t, err)
	assert.Len(t, lst, 1)
}
