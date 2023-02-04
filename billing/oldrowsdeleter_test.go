package impl

import (
	"context"
	"fmt"
	"testing"
	"time"

	"github.com/stretchr/testify/suite"

	"a.yandex-team.ru/billing/hot/accounts/pkg/core/actions"
	"a.yandex-team.ru/billing/hot/accounts/pkg/core/entities"
	bsql "a.yandex-team.ru/billing/library/go/billingo/pkg/storage/sql"
	"a.yandex-team.ru/billing/library/go/billingo/pkg/storage/sql/backends/pg"
	btesting "a.yandex-team.ru/billing/library/go/billingo/pkg/testing"
)

type OldRowsDeleterTestSuite struct {
	ActionTestSuite
}

func (s *OldRowsDeleterTestSuite) SetupTest() {
	cfg := `
manifests:
  - namespace: test_namespace
    accounts:
      test_account_type:
        attributes: [a1, a2]
        add_attributes: [a3]
        shard:
          prefix: p
          attributes: [a1]
        sub_accounts: [[]]
        rollup_period:
          - "0 * * *"
    states:
      test_state_type:
        attributes: [a1]
        shard:
          prefix: cl_id
          attributes: [a1]
`

	s.ActionTestSuite.SetupTest()
	s.ActionTestSuite.loadCfg(cfg)
	s.ActionTestSuite.initActions()
	s.ctx.OldRowsDeleter = NewOldRowsDeleter(s.ctx.Shards, s.ctx.Templates)
}

func writeEventBatch(ctx context.Context, actions actions.Actions, externalID string, dt time.Time,
	withEvent bool, withStateEvent bool) (int64, error) {
	events := make([]entities.EventAttributes, 0)
	if withEvent {
		events = append(events, entities.EventAttributes{
			Loc: entities.LocationAttributes{
				Namespace: "test_namespace",
				Type:      "test_account_type",
				Attributes: map[string]*string{
					"a1": btesting.RandSP(100),
					"a2": btesting.RandSP(100),
					"a3": btesting.RandSP(100),
				},
			},
			Type:   "debit",
			Dt:     dt,
			Amount: "666",
		})
	}

	states := make([]entities.StateAttributes, 0)
	if withStateEvent {
		states = append(states, entities.StateAttributes{
			Loc: entities.LocationAttributes{
				Namespace: "test_namespace",
				Type:      "test_state_type",
				Attributes: map[string]*string{
					"a1": btesting.RandSP(100),
				},
			},
			State: []byte(fmt.Sprintf(`["%s"]`, btesting.RandS(10))),
		})
	}

	batchRes, err := actions.WriteBatch(
		ctx,
		entities.BatchWriteRequest{
			EventType:  "event_type",
			ExternalID: externalID,
			Dt:         dt,
			Events:     events,
			States:     states,
		},
	)

	if err != nil {
		return 0, err
	}

	return batchRes.BatchID, nil
}

func (s *OldRowsDeleterTestSuite) TestDeleteEventBatches() {
	shard, err := s.ctx.Shards.GetLastShard()
	s.Assert().NoError(err)
	db := shard.GetDatabase(s.ctx, pg.Master)
	curDt := time.Now()
	oldDt := time.Now().AddDate(0, 0, -5)
	extIDPrefix := *btesting.RandSP(100) + ":"

	batchIDsToDelete := make([]any, 0, 4)

	batchID, err := writeEventBatch(
		s.ctx, s.ctx.Actions, extIDPrefix+"fresh", curDt, true, false)
	s.Assert().NoError(err)
	batchIDsToDelete = append(batchIDsToDelete, batchID)

	_, err = writeEventBatch(
		s.ctx, s.ctx.Actions, extIDPrefix+"old_with_event", oldDt, true, false)
	s.Assert().NoError(err)

	_, err = writeEventBatch(
		s.ctx, s.ctx.Actions, extIDPrefix+"old_with_state_event", oldDt, false, true)
	s.Assert().NoError(err)

	for i := 0; i < 3; i++ {
		extID := extIDPrefix + fmt.Sprintf("to_be_deleted_%v", i)
		batchID, err = writeEventBatch(s.ctx, s.ctx.Actions, extID, oldDt, true, false)
		s.Assert().NoError(err)
		batchIDsToDelete = append(batchIDsToDelete, batchID)
	}

	err = db.Tx(s.ctx, func(ctx context.Context, tx bsql.Transaction) error {
		_, err := tx.ExecContext(
			ctx, "delete from acc.t_event where event_batch_id in ($1, $2, $3, $4)",
			batchIDsToDelete...)
		return err
	})
	s.Assert().NoError(err)

	tableCfg := entities.DelOldRowsTableConfig{
		QueryTemplatePath: "templates/sql/delete_old_event_batches.sql",
		QueryParams:       []any{3, 2},
		ExecTimeout:       time.Minute,
		LockID:            100,
	}
	err = s.ctx.OldRowsDeleter.Delete(s.ctx, s.ctx.Shards.GetLastShardID(), tableCfg)
	s.Assert().NoError(err)

	expectedExtIDs := map[string]struct{}{
		extIDPrefix + "fresh":                {},
		extIDPrefix + "old_with_event":       {},
		extIDPrefix + "old_with_state_event": {},
	}
	rows, err := db.QueryxContext(s.ctx,
		`select external_id from acc.t_event_batch where external_id like $1 || '%'`, extIDPrefix)
	s.Assert().NoError(err)
	rc := 0
	var extID string
	for rows.Next() {
		rc += 1
		err := rows.Scan(&extID)
		s.Assert().NoError(err)
		s.Assert().Contains(expectedExtIDs, extID)
	}
	s.Assert().Equalf(3, rc, "Unexpected rows count.", rc)
}

func TestOldRowsDeleterTestSuite(t *testing.T) {
	suite.Run(t, new(OldRowsDeleterTestSuite))
}
