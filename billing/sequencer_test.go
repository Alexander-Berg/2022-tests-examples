package impl

import (
	"context"
	"testing"
	"time"

	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/suite"

	"a.yandex-team.ru/billing/hot/accounts/pkg/core/entities"
	dbutils "a.yandex-team.ru/billing/hot/accounts/pkg/storage/db/utils"
	bsql "a.yandex-team.ru/billing/library/go/billingo/pkg/storage/sql"
	"a.yandex-team.ru/billing/library/go/billingo/pkg/storage/sql/backends/pg"
)

type SequencerTestSuite struct {
	CommonAccountsTestSuite
}

func TestAggregateTestSuite(t *testing.T) {
	suite.Run(t, new(SequencerTestSuite))
}

func (s *SequencerTestSuite) TestEventUpdate() {
	// Для начала создадим хотя бы одно событие - хотя скорее всего в базе они будут и без этого, от предыдущих тестов
	attrsMap := genAttrMap()
	onDt := time.Now().UTC()

	_, err := s.ctx.Actions.WriteBatch(
		s.ctx,
		s.genWriteRequest(
			[]entities.EventAttributes{{
				Loc:    s.map2attrs(attrsMap),
				Type:   "debit",
				Dt:     onDt,
				Amount: "666.666",
			}}))
	if err != nil {
		s.T().Fatal(err)
	}

	// Первое обновление должно захватить одну строку
	rows, locked, err := s.ctx.Sequencer.UpdateEventSeqID(s.ctx,
		s.ctx.Shards.GetLastShardID())
	if err != nil {
		s.T().Fatal(err)
	}

	assert.NotEqual(s.T(), int64(0), rows)
	assert.Equal(s.T(), false, locked)

	// Второе обновление должно ничего не обновить
	rows, locked, err = s.ctx.Sequencer.UpdateEventSeqID(s.ctx,
		s.ctx.Shards.GetLastShardID())
	if err != nil {
		s.T().Fatal(err)
	}

	assert.Equal(s.T(), int64(0), rows)
	assert.Equal(s.T(), false, locked)
}

func (s *SequencerTestSuite) TestShardAlreadyLocked() {
	shard, err := s.ctx.Shards.GetLastShard()
	if err != nil {
		s.T().Fatal(err)
	}

	err = shard.GetDatabase(s.ctx, pg.Master).Tx(s.ctx, func(ctx context.Context, db bsql.Transaction) error {
		advisoryLock, err := dbutils.TryGetAdvisoryLock(ctx, db, s.ctx.SequencerConfig.LockID)
		if err != nil {
			return err
		}

		assert.Equal(s.T(), true, advisoryLock)

		rows, locked, err := s.ctx.Sequencer.UpdateEventSeqID(s.ctx,
			s.ctx.Shards.GetLastShardID())
		if err != nil {
			return err
		}

		assert.Equal(s.T(), int64(0), rows)
		assert.Equal(s.T(), true, locked)

		return nil
	})

	if err != nil {
		s.T().Fatal(err)
	}
}
