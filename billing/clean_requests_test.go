package tasks

import (
	"time"

	sq "github.com/Masterminds/squirrel"
	"github.com/stretchr/testify/require"

	"a.yandex-team.ru/billing/hot/payout/internal/context"
	"a.yandex-team.ru/billing/hot/payout/internal/request"
	"a.yandex-team.ru/billing/library/go/billingo/pkg/storage/sql/backends/pg"
	bt "a.yandex-team.ru/billing/library/go/billingo/pkg/testing"
)

const (
	newOldRequests    = 10
	newOldNewRequests = 10
)

func createRandomRequests(ctx *context.PayoutContext) (*request.Request, error) {
	externalID := bt.RandS(50)
	clientID := bt.RandN64()

	nr := request.NewRequest{
		ExternalID: externalID,
		ClientID:   clientID,
	}

	r, err := request.Create(ctx, &nr)
	if err != nil {
		return nil, err
	}
	return r, nil
}

func (s *TasksTestSuite) TestCleanRequests() {
	var ids []int64
	var newIds []int64

	ts := time.Now().AddDate(0, -1, 0)
	for cnt := newOldRequests; cnt > 0; cnt-- {
		r, err := createRandomRequests(s.ctx)
		require.NoError(s.T(), err)
		err = request.Update(s.ctx, r.ID, request.CreateDTCol, ts)
		require.NoError(s.T(), err)
		err = request.Update(s.ctx, r.ID, request.StatusCol, request.StatusDone)
		require.NoError(s.T(), err)
		ids = append(ids, r.ID)
	}
	for cnt := newOldNewRequests; cnt > 0; cnt-- {
		r, err := createRandomRequests(s.ctx)
		require.NoError(s.T(), err)
		err = request.Update(s.ctx, r.ID, request.CreateDTCol, ts)
		require.NoError(s.T(), err)
		newIds = append(newIds, r.ID)
	}

	p := NewCleanRequestProcessor(s.ctx)
	require.NoError(s.T(), p.process(time.Now()))

	db := s.ctx.Storage.Backend
	q := sq.Select("count(1)").From(request.Table).Where(sq.Eq{"id": ids})
	row, err := db.QueryRow(s.ctx, pg.Master, q)
	require.NoError(s.T(), err)

	var cnt int
	require.NoError(s.T(), row.Scan(&cnt))
	require.Equal(s.T(), 0, cnt)

	q = sq.Select("count(1)").From(request.Table).Where(sq.Eq{"id": newIds})
	row, err = db.QueryRow(s.ctx, pg.Master, q)
	require.NoError(s.T(), err)

	require.NoError(s.T(), row.Scan(&cnt))
	require.Equal(s.T(), newOldNewRequests, cnt)
}
