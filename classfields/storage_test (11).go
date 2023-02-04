package batch

import (
	"database/sql"
	"testing"
	"time"

	"github.com/YandexClassifieds/shiva/cmd/shiva/deployment/model"
	"github.com/YandexClassifieds/shiva/pb/shiva/types/batch"
	layerPb "github.com/YandexClassifieds/shiva/pb/shiva/types/layer"
	"github.com/YandexClassifieds/shiva/pkg/manifest"
	"github.com/YandexClassifieds/shiva/pkg/service_map"
	"github.com/YandexClassifieds/shiva/pkg/staff"
	"github.com/YandexClassifieds/shiva/test"
	"github.com/YandexClassifieds/shiva/test/test_db"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
)

func TestBatch_Assoc(t *testing.T) {
	test.InitTestEnv()
	db := test_db.NewSeparatedDb(t)
	db.GormDb.AutoMigrate(&staff.User{}, &Batch{}, &model.Deployment{})
	d := &model.Deployment{
		Author:     &staff.User{},
		ServiceMap: &service_map.Data{},
		Manifest:   &manifest.Data{},
	}
	d.ID = 42
	require.NoError(t, db.GormDb.Save(d).Error)

	b := &Batch{
		DeploymentID: 42,
		Deployment: &model.Deployment{
			Author:     &staff.User{},
			ServiceMap: &service_map.Data{},
			Manifest:   &manifest.Data{},
		},
	}
	s := NewStorage(db, test.NewLogger(t))
	require.NoError(t, s.Save(b))

	assert.Equal(t, int64(42), b.DeploymentID)
}

func TestStorage_GetWithoutPreload(t *testing.T) {
	test.InitTestEnv()
	db := test_db.NewSeparatedDb(t)

	require.NoError(t, db.GormDb.AutoMigrate(&staff.User{}, &service_map.Data{}, &manifest.Data{}, &model.Deployment{}))
	s := NewStorage(db, test.NewLogger(t))

	b := &Batch{
		Layer:    layerPb.Layer_TEST,
		Name:     "svc",
		Version:  "v1",
		State:    batch.State_Active,
		Periodic: "1 2 3 4 5",
		Next:     sql.NullTime{Valid: true, Time: time.Now()},
		Deployment: &model.Deployment{
			Author:     &staff.User{},
			ServiceMap: &service_map.Data{},
			Manifest:   &manifest.Data{},
		},
	}
	require.NoError(t, db.GormDb.Save(b).Error)

	// manual delete fields
	db.GormDb.Delete(b.Deployment.Author)
	db.GormDb.Delete(b.Deployment.ServiceMap)
	db.GormDb.Delete(b.Deployment.Manifest)

	result, err := s.GetByID(b.ID)
	require.NoError(t, err)
	require.NotNil(t, result.Deployment)
	require.NotNil(t, result.Deployment.ServiceMap)
	require.NotNil(t, result.Deployment.Manifest)
}

func TestStorage_SaveAndGet(t *testing.T) {
	test.InitTestEnv()
	db := test_db.NewSeparatedDb(t)

	require.NoError(t, db.GormDb.AutoMigrate(&staff.User{}, &service_map.Data{}, &manifest.Data{}, &model.Deployment{}))
	s := NewStorage(db, test.NewLogger(t))

	d := &model.Deployment{
		Author:     &staff.User{},
		ServiceMap: &service_map.Data{},
		Manifest:   &manifest.Data{},
	}
	require.NoError(t, db.GormDb.Save(d).Error)
	b := &Batch{
		Layer:        layerPb.Layer_TEST,
		Name:         "svc",
		Version:      "v1",
		State:        batch.State_Active,
		Periodic:     "1 2 3 4 5",
		Next:         sql.NullTime{Valid: true, Time: time.Now()},
		DeploymentID: d.ID,
	}
	err := s.Save(b)
	require.NoError(t, err)

	b2 := &Batch{
		Layer:        layerPb.Layer_TEST,
		Name:         "svc",
		Branch:       "br",
		Version:      "v1.1",
		State:        batch.State_Active,
		Periodic:     "1 2 3 4 5",
		Next:         sql.NullTime{Valid: true, Time: time.Now()},
		DeploymentID: d.ID,
	}
	require.NoError(t, s.Save(b2))

	result, err := s.GetByID(b.ID)
	assert.NoError(t, err)
	assert.NotNil(t, result)
	assert.Equal(t, d.ID, result.DeploymentID)
	assert.Equal(t, "", result.Branch)
}
