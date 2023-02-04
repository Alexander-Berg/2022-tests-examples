package branch

import (
	"database/sql"
	"errors"
	"testing"
	"time"

	"github.com/YandexClassifieds/shiva/cmd/cleaner/branch/store/issue"
	"github.com/YandexClassifieds/shiva/common"
	"github.com/YandexClassifieds/shiva/test"
	"github.com/YandexClassifieds/shiva/test/test_db"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
	"gorm.io/gorm"
)

func TestStorage_Save(t *testing.T) {
	test.InitTestEnv()

	db := test_db.NewSeparatedDb(t)
	storage := NewStorage(db, test.NewLogger(t))

	t.Run("create", func(t *testing.T) {
		deploymentEndTime := time.Now()
		model := Model{Layer: common.Test, Name: "svc1", Branch: "branch", DeploymentEndTime: deploymentEndTime}
		resultModel, err := storage.Save(&model)
		require.NoError(t, err)
		assert.Equal(t, common.Test, resultModel.Layer)
		assert.Equal(t, "svc1", resultModel.Name)
		assert.Equal(t, "branch", resultModel.Branch)
		assert.Equal(t, deploymentEndTime.Unix(), resultModel.DeploymentEndTime.Unix())
		assert.Equal(t, false, resultModel.Expires.Valid)
	})

	t.Run("update", func(t *testing.T) {
		deploymentEndTime := time.Now()
		expires := sql.NullTime{Time: deploymentEndTime.Add(time.Hour), Valid: true}
		err := db.GormDb.Create(&Model{Layer: common.Test, Name: "svc2", Branch: "b", DeploymentEndTime: deploymentEndTime}).Error
		require.NoError(t, err)
		model := Model{Layer: common.Test, Name: "svc2", Branch: "b", DeploymentEndTime: deploymentEndTime, Expires: expires}
		resultModel, err := storage.Save(&model)
		require.NoError(t, err)

		assert.False(t, errors.Is(err, gorm.ErrRecordNotFound))
		assert.Equal(t, common.Test, resultModel.Layer)
		assert.Equal(t, "svc2", resultModel.Name)
		assert.Equal(t, "b", resultModel.Branch)
		assert.Equal(t, deploymentEndTime.Unix(), resultModel.DeploymentEndTime.Unix())
		assert.Equal(t, expires.Time.Unix(), resultModel.Expires.Time.Unix())
	})

	t.Run("issues", func(t *testing.T) {
		// regression test to ensure issues are not saved directly by GORM
		m := &Model{
			Layer:  common.Test,
			Name:   "svc2",
			Branch: "bb",
			Issues: []*issue.Model{
				{Key: "VOID-1", Status: issue.Open},
			},
		}
		out, err := storage.Save(m)
		require.NoError(t, err)
		assert.Len(t, out.Issues, 0)
	})
}

func TestStorage_Get(t *testing.T) {
	test.InitTestEnv()

	db := test_db.NewSeparatedDb(t)
	storage := NewStorage(db, test.NewLogger(t))

	db.GormDb.Create(&Model{Layer: common.Test, Name: "svc1", Branch: "b1"})
	db.GormDb.Create(&Model{Layer: common.Test, Name: "svc2", Branch: "b2"})

	t.Run("get_existing", func(t *testing.T) {
		m, err := storage.Get(common.Test, "svc2", "B2")
		assert.NoError(t, err)
		assert.Equal(t, common.Test, m.Layer)
		assert.Equal(t, "svc2", m.Name)
		assert.Equal(t, "b2", m.Branch)
	})
	t.Run("get_not_found", func(t *testing.T) {
		_, err := storage.Get(common.Test, "non-existing", "b")
		assert.Error(t, err)
		assert.True(t, errors.Is(err, common.ErrNotFound), "should be ErrNotFound")
	})
}

func TestStorage_GetFull(t *testing.T) {
	test.InitTestEnv()

	db := test_db.NewSeparatedDb(t)
	require.NoError(t, db.GormDb.AutoMigrate(Model{}))
	storage := NewStorage(db, test.NewLogger(t))

	issue1 := &issue.Model{Key: "VOID-1", Status: issue.Open}
	issue2 := &issue.Model{Key: "VOID-2", Status: issue.Open}
	branch := &Model{
		Layer:  common.Test,
		Name:   "svc1",
		Branch: "b1",
		Issues: []*issue.Model{issue1, issue2},
	}
	db.GormDb.Create(branch)

	m, err := storage.GetFull(common.Test, "svc1", "b1")
	assert.NoError(t, err)
	assert.Equal(t, common.Test, m.Layer)
	assert.Equal(t, "svc1", m.Name)
	assert.Equal(t, "b1", m.Branch)
	assert.Len(t, m.Issues, 2)
}

func TestStorage_ReplaceIssues(t *testing.T) {
	test.InitTestEnv()

	db := test_db.NewSeparatedDb(t)
	require.NoError(t, db.GormDb.AutoMigrate(Model{}))
	storage := NewStorage(db, test.NewLogger(t))

	issue1 := &issue.Model{Key: "VOID-1", Status: issue.Open}
	issue2 := &issue.Model{Key: "VOID-2", Status: issue.Open}
	issue3 := &issue.Model{Key: "VOID-3", Status: issue.Open}
	branch := &Model{
		Layer:  common.Test,
		Name:   "svc1",
		Branch: "b1",
		Issues: []*issue.Model{issue1, issue2},
	}
	db.GormDb.Create(branch)

	m, err := storage.GetFull(common.Test, "svc1", "b1")
	assert.NoError(t, err)
	assert.Len(t, m.Issues, 2)
	assert.Equal(t, "VOID-1", m.Issues[0].Key)
	assert.Equal(t, "VOID-2", m.Issues[1].Key)

	err = storage.ReplaceIssues(m, []*issue.Model{issue3})
	assert.NoError(t, err)

	m, err = storage.GetFull(common.Test, "svc1", "b1")
	assert.NoError(t, err)
	assert.Len(t, m.Issues, 1)
	assert.Equal(t, "VOID-3", m.Issues[0].Key)
}

func TestStorage_GetExpired(t *testing.T) {
	test.InitTestEnv()

	db := test_db.NewSeparatedDb(t)
	storage := NewStorage(db, test.NewLogger(t))

	db.GormDb.Create(&Model{Layer: common.Test, Name: "svc1", Branch: "b1", Expires: sql.NullTime{}})
	db.GormDb.Create(&Model{Layer: common.Test, Name: "svc2", Branch: "b2", Expires: sql.NullTime{Time: time.Now().Add(time.Hour), Valid: true}})
	db.GormDb.Create(&Model{Layer: common.Test, Name: "svc2", Branch: "b3", Expires: sql.NullTime{Time: time.Now().Add(-time.Hour), Valid: true}})
	db.GormDb.Create(&Model{Layer: common.Test, Name: "svc3", Branch: "b4", Expires: sql.NullTime{Time: time.Now().Add(-time.Hour), Valid: true}})

	list, err := storage.GetExpired(time.Now())
	require.NoError(t, err)
	assert.Len(t, list, 2)
	expectedModels := []*Model{
		{Layer: common.Test, Name: "svc2", Branch: "b3"},
		{Layer: common.Test, Name: "svc3", Branch: "b4"},
	}
	resultModels := make([]*Model, len(list))
	for i, item := range list {
		resultModels[i] = &Model{Layer: item.Layer, Name: item.Name, Branch: item.Branch}
	}
	assert.ElementsMatch(t, resultModels, expectedModels)
}

func TestStorage_MarkProcessed(t *testing.T) {
	test.InitTestEnv()

	db := test_db.NewSeparatedDb(t)
	storage := NewStorage(db, test.NewLogger(t))

	model := Model{Layer: common.Test, Name: "svc1", Branch: "b1"}
	db.GormDb.Create(&model)

	m, err := storage.Get(model.Layer, model.Name, model.Branch)
	assert.NoError(t, err)
	assert.Equal(t, Started, m.State)

	err = storage.MarkProcessed(&model)
	assert.NoError(t, err)

	m, err = storage.Get(model.Layer, model.Name, model.Branch)
	assert.NoError(t, err)
	assert.Equal(t, Finished, m.State)
}

func TestStorage_GetWithOpenIssues(t *testing.T) {
	test.InitTestEnv()

	db := test_db.NewSeparatedDb(t)
	require.NoError(t, db.GormDb.AutoMigrate(Model{}))
	storage := NewStorage(db, test.NewLogger(t))

	db.GormDb.Create(&Model{Layer: common.Test, Name: "svc1", Branch: "b1", Expires: sql.NullTime{Time: time.Now(), Valid: true}})
	db.GormDb.Create(&Model{Layer: common.Test, Name: "svc1", Branch: "b2", State: Finished})
	db.GormDb.Create(&Model{
		Layer:  common.Test,
		Name:   "svc1",
		Branch: "b3",
		Issues: []*issue.Model{
			{Key: "VOID-1", Status: issue.Open},
			{Key: "VOID-2", Status: issue.Closed},
			{Key: "VOID-3", Status: issue.Closed},
		},
	})

	list, err := storage.GetWithOpenIssues()
	assert.NoError(t, err)
	assert.Len(t, list, 1)
	assert.Equal(t, "b3", list[0].Branch)
	assert.Len(t, list[0].Issues, 1)
	assert.Equal(t, "VOID-1", list[0].Issues[0].Key)
}

func TestStorage_GetWhereAllIssuesClosed(t *testing.T) {
	test.InitTestEnv()

	db := test_db.NewSeparatedDb(t)
	require.NoError(t, db.GormDb.AutoMigrate(Model{}))
	storage := NewStorage(db, test.NewLogger(t))
	now := time.Now()

	db.GormDb.Create(&Model{
		Layer:   common.Test,
		Name:    "svc1",
		Branch:  "b1",
		Expires: sql.NullTime{Time: now, Valid: true},
	})
	db.GormDb.Create(&Model{
		Layer:  common.Test,
		Name:   "svc1",
		Branch: "b2",
		State:  Finished,
		Issues: []*issue.Model{
			{Key: "VOID-1", Status: issue.Closed},
		},
	})
	db.GormDb.Create(&Model{
		Layer:  common.Test,
		Name:   "svc1",
		Branch: "b3",
		Issues: []*issue.Model{
			{Key: "VOID-2", Status: issue.Open},
			{Key: "VOID-3", Status: issue.Closed},
		},
	})
	db.GormDb.Create(&Model{
		Layer:  common.Test,
		Name:   "svc1",
		Branch: "b4",
		Issues: []*issue.Model{
			{Key: "VOID-4", Status: issue.Closed},
			{Key: "VOID-5", Status: issue.Closed},
		},
	})
	db.GormDb.Create(&Model{
		Layer:   common.Test,
		Name:    "svc1",
		Branch:  "b5",
		Expires: sql.NullTime{Time: now, Valid: true},
		Issues: []*issue.Model{
			{Key: "VOID-6", Status: issue.Closed},
			{Key: "VOID-7", Status: issue.Closed},
		},
	})

	models, err := storage.GetWhereAllIssuesClosed()
	assert.NoError(t, err)
	assert.Len(t, models, 1)
	assert.Equal(t, models[0].Branch, "b4")
}

func TestStorage_BatchSetTTL(t *testing.T) {
	test.InitTestEnv()

	db := test_db.NewSeparatedDb(t)
	require.NoError(t, db.GormDb.AutoMigrate(Model{}))
	storage := NewStorage(db, test.NewLogger(t))

	b1 := &Model{Layer: common.Test, Name: "svc1", Branch: "b1"}
	b2 := &Model{Layer: common.Test, Name: "svc1", Branch: "b2"}

	db.GormDb.Create(b1)
	db.GormDb.Create(b2)

	ttl := time.Now()

	err := storage.BatchSetTTL([]int64{b1.ID, b2.ID}, ttl)
	assert.NoError(t, err)

	for _, item := range []*Model{b1, b2} {
		result, err := storage.Get(item.Layer, item.Name, item.Branch)
		assert.NoError(t, err)
		assert.Equal(t, ttl.Unix(), result.Expires.Time.Unix())
	}
}
