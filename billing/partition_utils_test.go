package tasks

import (
	"time"

	sq "github.com/Masterminds/squirrel"
	"github.com/shopspring/decimal"
	"github.com/stretchr/testify/require"

	"a.yandex-team.ru/billing/hot/payout/internal/core"
	"a.yandex-team.ru/billing/hot/payout/internal/payout"
	"a.yandex-team.ru/billing/library/go/billingo/pkg/storage/sql/backends/pg"
	bt "a.yandex-team.ru/billing/library/go/billingo/pkg/testing"
	"a.yandex-team.ru/library/go/units"
)

func (s *TasksTestSuite) TestCPFPartitionCreation() {
	p := NewPartitionCpfProcessor(s.ctx)

	err := p.process(time.Now())
	require.NoError(s.T(), err)

	db := p.ctx.Storage.Backend
	tx, err := db.BeginTx(p.ctx)
	require.NoError(s.T(), err)

	partitions, err := core.GetTablePartitions(s.ctx, tx, "cpf")
	require.NoError(s.T(), err)
	require.NoError(s.T(), tx.Commit())
	require.Len(s.T(), partitions, p.config.NextPeriods)
}

func (s *TasksTestSuite) TestPartitionHistoryTrigger() {
	p := NewPartitionPayoutProcessor(s.ctx)

	err := p.process(time.Now())
	require.NoError(s.T(), err)

	po, err := payout.Create(s.ctx, &payout.NewPayout{
		ExternalID: bt.RandS(30),
		ServiceID:  124,
		ContractID: bt.RandN64(),
		Amount:     decimal.Decimal{},
		Currency:   "RUB",
		RequestID:  nil,
		ClientID:   0,
	})
	require.NoError(s.T(), err)

	require.NoError(s.T(), payout.Update(s.ctx, po.ID, payout.StatusCol, "pending"))

	db := p.ctx.Storage.Backend
	q := sq.Select("count(1)").
		From("payout.t_" + PayoutHistoryTable).
		Where(sq.Eq{"id": po.ID})
	row, err := db.QueryRow(s.ctx, pg.Master, q)
	require.NoError(s.T(), err)

	var cnt int
	require.NoError(s.T(), row.Scan(&cnt))

	require.Equal(s.T(), 1, cnt)
}

// TestGetNextPartitionDate проверяет, что получаем корректную дату для первой создаваемой партиции
// в зависимости от списка существующих партиций
func (s *TasksTestSuite) TestGetNextPartitionDate() {
	existingPartitions := []string{"tp_payout_20211230", "tp_payout_20211231"}
	nextDate, err := getNextPartitionDate(s.ctx, existingPartitions)
	require.NoError(s.T(), err)
	expectedNextDate := time.Date(2022, 1, 1, 0, 0, 0, 0, time.UTC).Truncate(units.Day)
	require.Equal(s.T(), expectedNextDate, nextDate)

	existingPartitions = []string{}
	nextDate, err = getNextPartitionDate(s.ctx, existingPartitions)
	require.NoError(s.T(), err)
	expectedNextDate = time.Now().UTC().Truncate(units.Day)
	require.Equal(s.T(), expectedNextDate, nextDate)
}

// TestGetNewPartitionsNumber проверяет функцию получения количества создаваемых партиций
func (s *TasksTestSuite) TestGetNewPartitionsNumber() {
	nextPartitionDate := time.Now().UTC().Truncate(units.Day).AddDate(0, 0, 10)
	partitionsNumber := getNewPartitionsNumber(nextPartitionDate, 40)
	require.Equal(s.T(), 30, partitionsNumber)

	partitionsNumber = getNewPartitionsNumber(nextPartitionDate, 5)
	require.Equal(s.T(), 0, partitionsNumber)
}

// TestAdvisoryLock проверяет, что если взят advisory lock на нужный ID, второй взять не получится
func (s *TasksTestSuite) TestAdvisoryLock() {
	// в трех разных транзакциях пытаемся взять лок
	// в первой должно получиться
	tx1, err := s.ctx.Storage.Backend.BeginTx(s.ctx)
	require.NoError(s.T(), err)
	defer func() {
		err = tx1.Rollback()
		require.NoError(s.T(), err)
	}()

	success, err := tryAdvisoryLock(s.ctx, tx1, payoutAdvisoryLockID)
	require.NoError(s.T(), err)
	require.True(s.T(), success)

	//во второй должно не получиться
	tx2, err := s.ctx.Storage.Backend.BeginTx(s.ctx)
	require.NoError(s.T(), err)
	defer func() {
		err = tx2.Rollback()
		require.NoError(s.T(), err)
	}()
	success, err = tryAdvisoryLock(s.ctx, tx2, payoutAdvisoryLockID)
	require.NoError(s.T(), err)
	require.False(s.T(), success)

	//в третьей (создается внутри) проверяем, что процессор не упадет, а вернет nil
	p := NewPartitionPayoutProcessor(s.ctx)
	err = p.process(time.Now())
	require.NoError(s.T(), err)

}
