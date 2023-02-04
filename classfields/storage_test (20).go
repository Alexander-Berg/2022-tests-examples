package storage

import (
	"errors"
	"testing"

	"github.com/YandexClassifieds/shiva/common/config"
	common "github.com/YandexClassifieds/shiva/common/storage/model"
	"github.com/YandexClassifieds/shiva/common/user_error"
	"github.com/YandexClassifieds/shiva/test"
	"github.com/stretchr/testify/require"

	"github.com/stretchr/testify/assert"
	"gorm.io/gorm"
)

func TestStorageReadableError(t *testing.T) {
	docsMock := []string{"doc1", "doc2"}

	tests := []struct {
		tName                string
		err                  error
		generic              user_error.Generic
		method               string
		errorInfo            *ErrDisplayInfo
		params               []interface{}
		nilExpected          bool
		expectedRusMessage   string
		expectedInnerMessage string
		expectedDocs         []string
	}{{
		tName:       "Nil error",
		err:         nil,
		nilExpected: true,
	},
		{
			tName:   "Service map not found",
			err:     gorm.ErrRecordNotFound,
			generic: ErrStorageRead,
			method:  "GetServiceMap",
			errorInfo: &ErrDisplayInfo{
				RusObjName: "карта сервисов",
				Docs:       docsMock,
			},
			params:               []interface{}{"par"},
			expectedRusMessage:   "Не найден(а) карта сервисов (par)",
			expectedInnerMessage: "some_service:GetServiceMap([par]) failed: record not found",
			expectedDocs:         docsMock,
		},
		{
			tName:   "Manifest not found",
			err:     gorm.ErrRecordNotFound,
			generic: ErrStorageRead,
			method:  "GetManifest",
			errorInfo: &ErrDisplayInfo{
				RusObjName: "манифест сервиса",
				Docs:       docsMock,
			},
			params:               []interface{}{"par1", "par2"},
			expectedRusMessage:   "Не найден(а) манифест сервиса (par1, par2)",
			expectedInnerMessage: "some_service:GetManifest([par1 par2]) failed: record not found",
			expectedDocs:         docsMock,
		},
		{
			tName:                "Generic not found",
			err:                  gorm.ErrRecordNotFound,
			generic:              ErrStorageRead,
			method:               "GetSomething",
			errorInfo:            nil,
			expectedRusMessage:   "Внутренняя ошибка",
			expectedInnerMessage: "some_service:GetSomething() failed: record not found",
			expectedDocs:         []string{config.TgNewDeploy},
		},
		{
			tName:                "Default",
			err:                  errors.New("fail"),
			generic:              ErrStorageRead,
			method:               "GetSomething",
			errorInfo:            nil,
			expectedRusMessage:   "Внутренняя ошибка",
			expectedInnerMessage: "some_service:GetSomething() failed: fail",
			expectedDocs:         []string{config.TgNewDeploy},
		}}
	bs := BaseStorage{Storage{Name: "some_service"}}
	for _, test := range tests {
		t.Run(test.tName, func(t *testing.T) {
			result := bs.ReadableError(test.err, test.generic, test.method, test.errorInfo, test.params)

			if test.nilExpected {
				assert.Nil(t, result)
			} else {
				userErrorResult := result.(*user_error.UserError)
				innerError := userErrorResult.Unwrap()
				assert.Equal(t, test.expectedRusMessage, userErrorResult.RusMessage)
				assert.Equal(t, userErrorResult.Docs, test.expectedDocs)
				assert.True(t, errors.Is(innerError, test.err))
				assert.Equal(t, test.expectedInnerMessage, innerError.Error())
			}
		})
	}
}

func TestOptimisticLock(t *testing.T) {
	test.InitTestEnv()
	db := test.NewSeparatedGorm(t)
	log := test.NewLogger(t)
	storage := NewBaseStorage(test.NewLogger(t), NewDatabase(db, log, nil, nil, nil), &model{})

	m := &model{
		Info: "some info",
	}
	require.NoError(t, storage.OptimisticLock().Save(t.Name(), m))

	newModel := new(model)
	*newModel = *m
	newModel.Info = "new info"
	require.NoError(t, storage.Save(t.Name(), newModel))

	m.Info = "update info"
	require.Equal(t, ErrOptimisticLockFail, storage.OptimisticLock().Save(t.Name(), m))

	actualModel := &model{}
	require.NoError(t, storage.GetById(t.Name(), actualModel, m.ID))
	require.Equal(t, newModel.Info, actualModel.Info)
	require.Equal(t, false, storage.enableOptimisticLock)
}

func TestUpdateZeroValue(t *testing.T) {
	test.InitTestEnv()
	db := test.NewSeparatedGorm(t)
	log := test.NewLogger(t)
	storage := NewBaseStorage(test.NewLogger(t), NewDatabase(db, log, nil, nil, nil), &model{})

	m := &model{
		Info: "some info",
	}

	require.NoError(t, storage.Save(t.Name(), m))

	m.Info = ""
	require.NoError(t, storage.OptimisticLock().Save(t.Name(), m))

	actualModel := &model{}
	require.NoError(t, storage.GetById(t.Name(), actualModel, m.ID))
	require.Equal(t, "", actualModel.Info)
}

func TestBatchSave(t *testing.T) {
	test.InitTestEnv()

	m := []*model{
		{
			Info: "info1",
		},
		{
			Info: "info2",
		},
	}

	testCases := []struct {
		name                 string
		enableOptimisticLock bool
		expectedErr          error
	}{
		{
			name:                 "simple batch save",
			enableOptimisticLock: false,
			expectedErr:          nil,
		},
		{
			name:                 "batch save with optimistic lock",
			enableOptimisticLock: true,
			expectedErr:          ErrUnsupportedType,
		},
	}

	for _, tc := range testCases {
		t.Run(tc.name, func(t *testing.T) {
			db := test.NewSeparatedGorm(t)
			log := test.NewLogger(t)
			storage := NewBaseStorage(test.NewLogger(t), NewDatabase(db, log, nil, nil, nil), &model{})
			if tc.enableOptimisticLock {
				storage = *storage.OptimisticLock()
			}

			err := storage.Save(t.Name(), &m)
			require.Equal(t, tc.expectedErr, err)
		})
	}
}

type model struct {
	common.Model
	Info string `gorm:"column:info;type:varchar(64)"`
}

func (m model) TableName() string {
	return "test_model"
}
