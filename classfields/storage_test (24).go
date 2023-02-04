package manifest

import (
	"io/ioutil"
	"testing"

	"github.com/YandexClassifieds/shiva/cmd/shiva/env/external/reader"
	storage2 "github.com/YandexClassifieds/shiva/common/storage"
	"github.com/YandexClassifieds/shiva/pkg/include"
	"github.com/YandexClassifieds/shiva/pkg/manifest/parser"
	"github.com/YandexClassifieds/shiva/test"
	"github.com/YandexClassifieds/shiva/test/test_db"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
)

func TestSaveAndGet(t *testing.T) {
	test.InitTestEnv()
	db := test_db.NewSeparatedDb(t)

	storage := newStorage(db, test.NewLogger(t))
	data, err := ioutil.ReadFile("parser/example.yml")
	require.NoError(t, err)
	m0 := &Data{
		Name: "test1",
		Blob: data,
	}
	err = storage.Save(m0)
	require.NoError(t, err)

	assert.NotNil(t, m0)
	assert.NotNil(t, m0.CreatedAt)
	assert.True(t, m0.ID > 0)
	assert.Equal(t, "test1", m0.Name)
	assert.Equal(t, string(data), string(m0.Blob))

	m2, err := storage.Get(m0.ID)
	require.NoError(t, err)

	assertManifest(t, m0, m2)
}

func assertManifest(t *testing.T, m1 *Data, m2 *Data) {
	assert.NotNil(t, m2)
	assert.True(t, m2.ID > 0)
	assert.Equal(t, m1.ID, m2.ID)
	assert.Equal(t, m1.Name, m2.Name)
	assert.Equal(t, m1.CreatedAt.Unix(), m2.CreatedAt.Unix())
	assert.Equal(t, m1.Name, m2.Name)
}

func newService(t *testing.T, db *storage2.Database) *Service {
	log := test.NewLogger(t)
	confS := include.NewService(db, log)
	return NewService(db, log, parser.NewService(log, reader.NewService(db, log)), confS)
}
