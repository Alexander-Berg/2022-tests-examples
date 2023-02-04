package task

import (
	"database/sql"
	"testing"
	"time"

	"github.com/YandexClassifieds/shiva/cmd/shiva/batch/store/batch"
	dModel "github.com/YandexClassifieds/shiva/cmd/shiva/deployment/model"
	"github.com/YandexClassifieds/shiva/common/storage"
	"github.com/YandexClassifieds/shiva/pb/shiva/api/deploy2"
	batchPb "github.com/YandexClassifieds/shiva/pb/shiva/types/batch"
	btPb "github.com/YandexClassifieds/shiva/pb/shiva/types/batch_task"
	"github.com/YandexClassifieds/shiva/pb/shiva/types/layer"
	"github.com/YandexClassifieds/shiva/pkg/manifest"
	"github.com/YandexClassifieds/shiva/pkg/service_map"
	"github.com/YandexClassifieds/shiva/pkg/staff"
	"github.com/YandexClassifieds/shiva/test"
	"github.com/YandexClassifieds/shiva/test/test_db"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
)

func TestStorage_List(t *testing.T) {
	test.InitTestEnv()

	testCases := []struct {
		name   string
		force  bool
		params ListParams

		expectedLen int
	}{
		{
			name: "Get by service",
			params: ListParams{
				Service: "test-svc",
			},
			expectedLen: 1,
		},
		{
			name: "State no found",
			params: ListParams{
				Service: "test-svc",
				State:   []btPb.State{btPb.State_Skipped},
			},
			expectedLen: 0,
		},
		{
			name: "Get by service with state",
			params: ListParams{
				Service: "test-svc",
				State:   []btPb.State{btPb.State_Success},
			},
			expectedLen: 1,
		},
		{
			name: "Service branches not found",
			params: ListParams{
				Service:    "test-svc",
				BranchType: deploy2.BranchType_BRANCH_ONLY,
			},
			expectedLen: 0,
		},
		{
			name: "Get without branch",
			params: ListParams{
				Service:    "other-svc",
				BranchType: deploy2.BranchType_BRANCH_MAIN,
			},
			expectedLen: 0,
		},
		{
			name: "Get by branch",
			params: ListParams{
				Service: "other-svc",
				Branch:  "br1",
			},
			expectedLen: 1,
		},
		{
			name: "Get force task by branch",
			params: ListParams{
				Service: "other-svc",
				Branch:  "br1",
			},
			force:       true,
			expectedLen: 1,
		},
	}

	for _, tc := range testCases {
		t.Run(tc.name, func(t *testing.T) {
			db := test_db.NewSeparatedDb(t)
			s := NewStorage(db, test.NewLogger(t))
			require.NoError(t, db.GormDb.AutoMigrate(dModel.Deployment{}, staff.User{}, batch.Batch{}))
			prepare(t, db, s, tc.force)

			lst, err := s.List(tc.params)
			require.NoError(t, err)
			require.Len(t, lst, tc.expectedLen)

			for _, task := range lst {
				pb := task.GetProto()
				if tc.params.Branch != "" {
					require.Equal(t, tc.params.Branch, pb.Branch)
				}
				if tc.force {
					require.Equal(t, "test-usr", pb.Author)
				} else {
					require.Empty(t, pb.Author)
				}
			}
		})
	}

}

func prepare(t *testing.T, db *storage.Database, s *Storage, force bool) {
	user := &staff.User{Login: "test-usr"}
	d := &dModel.Deployment{
		Name:       "test-svc",
		Author:     user,
		ServiceMap: &service_map.Data{},
		Manifest:   &manifest.Data{},
	}
	require.NoError(t, db.GormDb.Save(d).Error)
	d2 := &dModel.Deployment{
		Name:       "other-svc",
		Branch:     "br1",
		Author:     user,
		ServiceMap: &service_map.Data{},
		Manifest:   &manifest.Data{},
	}
	require.NoError(t, db.GormDb.Save(d2).Error)

	b := &batch.Batch{
		Layer:        layer.Layer_TEST,
		Name:         "test-svc",
		DeploymentID: d.ID,
	}
	db.GormDb.Create(b)
	b2 := &batch.Batch{
		Layer:        layer.Layer_TEST,
		Name:         "other-svc",
		Branch:       "br1",
		DeploymentID: d2.ID,
	}
	db.GormDb.Save(b2)

	d1Id := int64(0)
	d2Id := int64(0)
	if force {
		d1Id = d.ID
		d2Id = d2.ID
	}
	err := s.Save(&Task{
		StartDate:    time.Now(),
		EndDate:      sql.NullTime{Time: time.Now(), Valid: true},
		State:        btPb.State_Success,
		BatchID:      b.ID,
		DeploymentID: d1Id,
	})
	require.NoError(t, err)

	err = s.Save(&Task{
		StartDate:    time.Now(),
		EndDate:      sql.NullTime{Time: time.Now(), Valid: true},
		State:        btPb.State_Success,
		BatchID:      b2.ID,
		DeploymentID: d2Id,
	})
	require.NoError(t, err)
}

func TestLoadProcess(t *testing.T) {
	test.RunUp(t)
	db := test_db.NewDb(t)
	log := test.NewLogger(t)
	taskStorage := NewStorage(db, log)
	batchStorage := batch.NewStorage(db, log)
	bTest := &batch.Batch{
		Layer:        layer.Layer_TEST,
		Name:         "service1",
		Version:      "0.1.1",
		State:        batchPb.State_Active,
		Periodic:     "",
		Next:         sql.NullTime{},
		DeploymentID: 0,
		Deployment:   nil,
	}
	bProd := &batch.Batch{
		Layer:        layer.Layer_PROD,
		Name:         "service1",
		Version:      "0.1.1",
		State:        batchPb.State_Active,
		Periodic:     "",
		Next:         sql.NullTime{},
		DeploymentID: 0,
		Deployment:   nil,
	}
	require.NoError(t, batchStorage.Save(bTest))
	require.NoError(t, batchStorage.Save(bProd))
	testProcessTask := &Task{
		BatchID:    bTest.ID,
		State:      btPb.State_Process,
		StartDate:  time.Time{},
		EndDate:    sql.NullTime{},
		Deployment: nil,
	}
	require.NoError(t, taskStorage.Save(testProcessTask))
	testFailedTask := &Task{
		BatchID:    bTest.ID,
		State:      btPb.State_Failed,
		StartDate:  time.Time{},
		EndDate:    sql.NullTime{},
		Deployment: nil,
	}
	require.NoError(t, taskStorage.Save(testFailedTask))
	prodProcessTask := &Task{
		BatchID:    bProd.ID,
		State:      btPb.State_Process,
		StartDate:  time.Time{},
		EndDate:    sql.NullTime{},
		Deployment: nil,
	}
	require.NoError(t, taskStorage.Save(prodProcessTask))
	result, err := taskStorage.GetProcess(bTest)
	require.NoError(t, err)
	assert.NotNil(t, result)
	assert.Equal(t, result.ID, testProcessTask.ID)

	process, err := taskStorage.AllProcess(layer.Layer_TEST)
	require.NoError(t, err)
	assert.Len(t, process, 1)
	assert.Equal(t, process[0].ID, testProcessTask.ID)
}
