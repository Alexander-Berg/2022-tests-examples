package issue

import (
	"testing"

	"github.com/YandexClassifieds/shiva/test"
	"github.com/YandexClassifieds/shiva/test/test_db"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
)

func TestStorage_FirstOrCreate(t *testing.T) {
	test.InitTestEnv()

	db := test_db.NewSeparatedDb(t)
	storage := NewStorage(db, test.NewLogger(t))

	t.Run("get with creating", func(t *testing.T) {
		model := Model{Key: "VOID-1"}
		resultModel, err := storage.FirstOrCreate(&model)
		require.NoError(t, err)
		assert.Equal(t, "VOID-1", resultModel.Key)
		var count int64
		err = db.GormDb.Model(&Model{}).Where("key = ?", "VOID-1").Count(&count).Error
		assert.NoError(t, err)
		assert.Equal(t, int64(1), count)
	})

	t.Run("get without creating", func(t *testing.T) {
		err := db.GormDb.Create(&Model{Key: "VOID-2"}).Error
		assert.NoError(t, err)
		resultModel, err := storage.FirstOrCreate(&Model{Key: "VOID-2"})
		require.NoError(t, err)
		assert.Equal(t, "VOID-2", resultModel.Key)
		var count int64
		db.GormDb.Model(&Model{}).Where("key = ?", "VOID-2").Count(&count)
		assert.Equal(t, int64(1), count)
	})
}

func TestStorage_BatchSetClosed(t *testing.T) {
	test.InitTestEnv()

	db := test_db.NewSeparatedDb(t)
	storage := NewStorage(db, test.NewLogger(t))

	db.GormDb.Create(&Model{Key: "VOID-1"})
	db.GormDb.Create(&Model{Key: "VOID-2"})
	db.GormDb.Create(&Model{Key: "VOID-3"})

	err := storage.BatchSetClosed([]string{"VOID-1", "VOID-3"})
	assert.NoError(t, err)

	issue, err := storage.GetByKey("VOID-1")
	assert.NoError(t, err)
	assert.Equal(t, Closed, issue.Status)

	issue, err = storage.GetByKey("VOID-2")
	assert.NoError(t, err)
	assert.Equal(t, Open, issue.Status)

	issue, err = storage.GetByKey("VOID-3")
	assert.NoError(t, err)
	assert.Equal(t, Closed, issue.Status)
}
