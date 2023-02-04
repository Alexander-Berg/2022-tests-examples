package scaler

import (
	"testing"
	"time"

	"github.com/YandexClassifieds/shiva/common"
	"github.com/YandexClassifieds/shiva/test"
	"github.com/YandexClassifieds/shiva/test/test_db"
	"github.com/stretchr/testify/require"
)

func TestStorage_Count(t *testing.T) {
	test.InitTestEnv()
	db := test_db.NewSeparatedDb(t)
	storage := NewStorage(db, test.NewLogger(t))

	result, err := storage.Count(common.Prod, "abc")
	require.NoError(t, err)
	require.Zero(t, result)

	res := db.GormDb.Save(&CalculatedResource{Service: "abcd", CPU: 200, Layer: common.Prod})
	require.NoError(t, res.Error)
	res = db.GormDb.Save(&CalculatedResource{Service: "abc", CPU: 300, Layer: common.Prod})
	require.NoError(t, res.Error)
	res = db.GormDb.Save(&CalculatedResource{Service: "abc", CPU: 200, Layer: common.Prod})
	require.NoError(t, res.Error)
	res = db.GormDb.Save(&CalculatedResource{Service: "abc", CPU: 100, Layer: common.Test})
	require.NoError(t, res.Error)

	result, err = storage.Count(common.Prod, "abc")
	require.NoError(t, err)
	require.Equal(t, int64(2), result)
}

func TestStorage_GetBiggest(t *testing.T) {
	test.InitTestEnv()
	db := test_db.NewSeparatedDb(t)
	storage := NewStorage(db, test.NewLogger(t))

	moreThanMonthAgo := time.Now().AddDate(0, -1, -1)
	res := db.GormDb.Save(&CalculatedResource{Service: "abc", CPU: 400, Layer: common.Prod, At: moreThanMonthAgo})
	require.NoError(t, res.Error)
	res = db.GormDb.Save(&CalculatedResource{Service: "abc", CPU: 300, Layer: common.Prod, At: time.Now()})
	require.NoError(t, res.Error)
	res = db.GormDb.Save(&CalculatedResource{Service: "abc", CPU: 200, Layer: common.Prod, At: time.Now()})
	require.NoError(t, res.Error)

	resource, err := storage.GetBiggest(common.Prod, "abc", time.Now().AddDate(0, -1, 0))
	require.NoError(t, err)
	require.NotNil(t, resource)
	require.Equal(t, "abc", resource.Service)
	require.Equal(t, 300, resource.CPU)

	resource, err = storage.GetBiggest(common.Test, "abc", time.Now().AddDate(0, -1, 0))
	require.Nil(t, resource)
	require.ErrorIs(t, err, common.ErrNotFound)
}

func TestStorage_Save(t *testing.T) {
	test.RunUp(t)
	db := test_db.NewSeparatedDb(t)
	storage := NewStorage(db, test.NewLogger(t))

	var resources []*CalculatedResource
	resources = append(
		resources, &CalculatedResource{
			Layer:   common.Prod,
			Service: "test-service",
			CPU:     200,
			At:      time.Now().Add(-time.Hour),
		})
	resources = append(
		resources, &CalculatedResource{
			Layer:   common.Test,
			Service: "test-service2",
			CPU:     300,
			At:      time.Now().Add(-time.Minute),
		})

	for _, resource := range resources {
		err := storage.Save(resource)
		require.NoError(t, err)
	}

	var results []*CalculatedResource
	db.GormDb.Order("id asc").Find(&results, "")
	require.Len(t, results, 2)
	for i, result := range results {
		require.Equal(t, resources[i].Service, result.Service)
		require.Equal(t, resources[i].CPU, result.CPU)
		require.Equal(t, resources[i].Layer, result.Layer)
		require.Equal(t, resources[i].At.Round(time.Second), result.At.In(time.Local).Round(time.Second))
	}
}
