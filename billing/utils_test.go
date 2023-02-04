package core

import (
	"testing"
	"time"

	"github.com/stretchr/testify/require"

	"a.yandex-team.ru/library/go/units"
)

// TestGetTodaysPartitionIndex проверяет корректность возвращаемого индекса сегодняшней партиции
func TestGetTodaysPartitionIndex(t *testing.T) {
	partitions := []string{"tp_payout_20210808", "tp_payout_20210809", "tp_payout_20210810", "tp_payout_20210811"}
	index := GetTodaysPartitionIndex(partitions, "tp_payout_20210808")
	require.Equal(t, 0, index)

	index = GetTodaysPartitionIndex(partitions, "tp_payout_20210810")
	require.Equal(t, 2, index)

	index = GetTodaysPartitionIndex(partitions, "tp_payout_20210811")
	require.Equal(t, 3, index)

	index = GetTodaysPartitionIndex(partitions, "tp_payout_20210908")
	require.Equal(t, -1, index)
}

// TestGetConsecutivePartitionsCount проверяет корректность возвращаемого количества
// непрерывно созданных партиций в будущее
func TestGetConsecutivePartitionsCount(t *testing.T) {
	// создаем список партиций на несколько дней назад от сегодня
	today := time.Now().UTC().Truncate(units.Day)
	partitions := []string{}
	tablePrefix := "tp_payout_"

	// если нет партиций, должны вернуть -1
	count := GetConsecutivePartitionsCount(partitions, tablePrefix)
	require.Equal(t, -1, count)

	for i := 3; i > 0; i-- {
		nextDate := today.AddDate(0, 0, -i)
		partitions = append(partitions, tablePrefix+nextDate.Format(dateFormat))
	}
	// если нет сегодняшней партиции, должны вернуть -1
	count = GetConsecutivePartitionsCount(partitions, tablePrefix)
	require.Equal(t, -1, count)

	// если сегодняшняя партиция последняя, должны вернуть 0
	partitions = append(partitions, tablePrefix+today.Format(dateFormat))
	count = GetConsecutivePartitionsCount(partitions, tablePrefix)
	require.Equal(t, 0, count)

	// добавляем еще 6 непрерывно идущих партиций
	for i := 1; i < 7; i++ {
		nextDate := today.AddDate(0, 0, i)
		partitions = append(partitions, tablePrefix+nextDate.Format(dateFormat))
	}
	count = GetConsecutivePartitionsCount(partitions, tablePrefix)
	require.Equal(t, 6, count, partitions)

	// добавляем одну партицию в далекое будущее, не должны ее учитывать
	partitions = append(partitions, tablePrefix+today.AddDate(0, 0, 30).Format(dateFormat))
	count = GetConsecutivePartitionsCount(partitions, tablePrefix)
	require.Equal(t, 6, count, partitions)
}
