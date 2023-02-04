package impl

import (
	"fmt"
	"testing"
	"time"

	sq "github.com/Masterminds/squirrel"
	"github.com/gofrs/uuid"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/suite"

	"a.yandex-team.ru/billing/hot/accounts/pkg/core/entities"
	coreerrors "a.yandex-team.ru/billing/hot/accounts/pkg/core/errors"
	"a.yandex-team.ru/billing/library/go/billingo/pkg/storage/sql/backends/pg"
	btesting "a.yandex-team.ru/billing/library/go/billingo/pkg/testing"
	"a.yandex-team.ru/library/go/core/xerrors"
)

type CommonBatchOperationTestSuite struct {
	ActionTestSuite
	gen *uuid.Gen
}

func (s *CommonBatchOperationTestSuite) SetupTest() {
	cfg := `
manifests:
- namespace: test
  accounts:
    account_type:
      attributes:
        - a1
        - a2
      add_attributes:
        - a3
      shard:
        prefix: p
        attributes:
          - a1
      sub_accounts:
        - []
        - - a3
      rollup_period:
        - "0 * * *"
  states:
    state_type:
      attributes:
        - a1
        - a2
      shard:
        prefix: p
        attributes:
          - a1
  locks:
    lock_type:
      attributes:
        - a1
        - a2
      shard:
        prefix: p
        attributes:
          - a1
- namespace: add_test
  accounts:
    account_type:
      attributes:
        - a1
        - a2
      add_attributes:
        - a3
      shard:
        prefix: p
        attributes:
          - a1
      sub_accounts:
        - []
        - - a3
      rollup_period:
        - "0 * * *"
  states:
    state_type:
      attributes:
        - a1
        - a2
      shard:
        prefix: p
        attributes:
          - a1
  locks:
    lock_type:
      attributes:
        - a1
        - a2
      shard:
        prefix: p
        attributes:
          - a1
`
	s.ActionTestSuite.SetupTest()
	s.ActionTestSuite.loadCfg(cfg)
	s.ActionTestSuite.initActions()
	s.gen = uuid.NewGen()
}

func (s *CommonBatchOperationTestSuite) getEntityShardKey(query sq.SelectBuilder) string {
	shard, err := s.ctx.Shards.GetLastShard()
	s.Require().NoError(err)
	row, err := shard.GetDatabase(s.ctx, pg.Master).QueryRowSq(s.ctx, query)
	s.Require().NoError(err)
	var shardKey string
	err = row.Scan(&shardKey)
	s.Require().NoError(err)
	return shardKey
}

func (s *CommonBatchOperationTestSuite) getAccountShardKey(accountID int64) string {
	query := sq.Select("shard_key").
		From("acc.t_account").
		Where("id = ?", accountID)
	return s.getEntityShardKey(query)
}

func (s *CommonBatchOperationTestSuite) getStateShardKey(stateID int64) string {
	query := sq.Select("shard_key").
		From("acc.t_state_account").
		Where("id = ?", stateID)
	return s.getEntityShardKey(query)
}

func (s *CommonBatchOperationTestSuite) getEventBatchShardKey(eventBatchID int64) string {
	query := sq.Select("shard_key").
		From("acc.t_event_batch").
		Where("id = ?", eventBatchID)
	return s.getEntityShardKey(query)
}

func (s *CommonBatchOperationTestSuite) getEntityIDs(query sq.SelectBuilder) []int64 {
	shard, err := s.ctx.Shards.GetLastShard()
	s.Require().NoError(err)
	rows, err := shard.GetDatabase(s.ctx, pg.Master).QuerySq(s.ctx, query)
	s.Require().NoError(err)
	//goland:noinspection GoUnhandledErrorResult
	defer rows.Close()
	res := make([]int64, 0)
	var tmpID int64
	for rows.Next() {
		err = rows.Scan(&tmpID)
		s.Require().NoError(err)
		res = append(res, tmpID)
	}
	err = rows.Err()
	s.Require().NoError(err)
	return res
}

func (s *CommonBatchOperationTestSuite) getEventIDs(accountID int64) []int64 {
	query := sq.Select("id").
		From("acc.t_event").
		Where("account_id = ?", accountID)

	return s.getEntityIDs(query)
}

func (s *CommonBatchOperationTestSuite) getSubAccountIDs(accountID int64) []int64 {
	query := sq.Select("id").
		From("acc.t_subaccount").
		Where("account_id = ?", accountID)

	return s.getEntityIDs(query)
}

func (s *CommonBatchOperationTestSuite) getStateEventIDs(stateAccountID int64) []int64 {
	query := sq.Select("id").
		From("acc.t_state_event").
		Where("state_account_id = ?", stateAccountID)

	return s.getEntityIDs(query)
}

func (s *CommonBatchOperationTestSuite) getEventBatchCount(batchID int64) int {
	query := sq.Select("event_count").
		From("acc.t_event_batch").
		Where("id = ?", batchID)
	shard, err := s.ctx.Shards.GetLastShard()
	s.Require().NoError(err)
	rows, err := shard.GetDatabase(s.ctx, pg.Master).QuerySq(s.ctx, query)
	s.Require().NoError(err)
	//goland:noinspection GoUnhandledErrorResult
	defer rows.Close()
	var batchCount int32
	s.Require().True(rows.Next())
	s.Require().NoError(rows.Scan(&batchCount))
	s.Require().False(rows.Next())
	s.Require().NoError(rows.Err())
	return int(batchCount)
}

func (s *CommonBatchOperationTestSuite) genUID() string {
	uid, err := s.gen.NewV4()
	s.Require().NoError(err)
	return uid.String()
}

func (s *CommonBatchOperationTestSuite) attr2loc(attrs entities.LocationAttributes) entities.Location {
	return entities.Location{
		Namespace:  attrs.Namespace,
		Type:       attrs.Type,
		Attributes: [5]string{*attrs.Attributes["a1"], *attrs.Attributes["a2"]},
	}
}

type BatchWriteTestSuite struct {
	CommonBatchOperationTestSuite
}

func (s *BatchWriteTestSuite) testEverything(namespaces []string) {
	commonAttr := btesting.RandSP(100)
	startDt := time.Now().UTC()
	var shardKey string

	shard, err := s.ctx.Shards.GetLastShard()
	s.Require().NoError(err)

	lockModes := []entities.LockActionMode{
		entities.LockValidateMode,
		entities.LockRemoveMode,
	}
	var i int
	locks := make([]entities.LockAction, len(lockModes)*len(namespaces))
	for _, mode := range lockModes {
		for _, ns := range namespaces {
			locks[i] = entities.LockAction{
				Loc: entities.LocationAttributes{
					Namespace:  ns,
					Type:       "lock_type",
					Attributes: map[string]*string{"a1": commonAttr, "a2": btesting.RandSP(100)},
				},
				Mode: mode,
			}
			var timeout int64
			if mode == entities.LockValidateMode {
				timeout = 0
			} else {
				timeout = 666
			}
			location, err := s.ctx.EntitySettings.Lock().Location(&locks[i].Loc)
			s.Require().NoError(err)

			// Shard keys of all the entities in a batch request must be the same.
			shardKey = location.ShardKey

			newLocks, err := shard.GetLockStorage(s.ctx.Templates).Init(
				s.ctx,
				[]entities.Location{location},
				timeout,
			)
			s.Require().NoError(err)
			locks[i].UID = newLocks[0].UID
			i++
		}
	}

	onDt := time.Now().Truncate(time.Microsecond).UTC()
	events := make([]entities.EventAttributes, len(namespaces))
	for i, ns := range namespaces {
		events[i] = entities.EventAttributes{
			Loc: entities.LocationAttributes{
				Namespace: ns,
				Type:      "account_type",
				Attributes: map[string]*string{
					"a1": commonAttr,
					"a2": btesting.RandSP(100),
					"a3": btesting.RandSP(100),
				},
			},
			Type:   "debit",
			Dt:     onDt,
			Amount: "666",
		}
	}

	states := make([]entities.StateAttributes, len(namespaces))
	for i, ns := range namespaces {
		states[i] = entities.StateAttributes{
			Loc: entities.LocationAttributes{
				Namespace: ns,
				Type:      "state_type",
				Attributes: map[string]*string{
					"a1": commonAttr,
					"a2": btesting.RandSP(100),
				},
			},
			State: []byte(fmt.Sprintf(`["%s"]`, btesting.RandS(10))),
		}
	}

	batchRes, err := s.ctx.Actions.WriteBatch(
		s.ctx,
		entities.BatchWriteRequest{
			EventType:  "test",
			ExternalID: btesting.RandS(100),
			Dt:         time.Now(),
			Events:     events,
			States:     states,
			Locks:      locks,
		},
	)
	s.Require().NoError(err)
	s.checkEntityID(batchRes.BatchID, shard)
	s.Assert().Equal(shardKey, s.getEventBatchShardKey(batchRes.BatchID))
	s.Assert().Equal(len(namespaces), s.getEventBatchCount(batchRes.BatchID))

	for _, lock := range locks {
		location, err := s.ctx.EntitySettings.Lock().Location(&lock.Loc)
		s.Require().NoError(err)
		lockInfo := s.getLockInfo(location)
		s.Assert().Equal(lock.UID, lockInfo.UID)
		s.checkEntityID(lockInfo.ID, shard)
		if lock.Mode == entities.LockRemoveMode {
			s.Assert().Equal(time.Date(2000, 1, 1, 0, 0, 0, 0, time.UTC), lockInfo.DT)
		} else {
			s.Assert().True(lockInfo.DT.After(startDt))
		}

		s.Assert().Equal(shardKey, lockInfo.ShardKey)
	}

	for _, event := range events {
		loc, err := s.ctx.EntitySettings.Account().AccountLocation(&event.Loc)
		s.Require().NoError(err)
		accountID := s.getAccount(event.Loc.Namespace, event.Loc.Type, loc.Loc.Attributes[:])
		s.checkEntityID(accountID, shard)
		resEvents := s.getEvents(accountID)
		subaccounts := s.getSubaccounts(accountID)
		s.Assert().Equal(resEvents, []eventInfo{{
			EventBatchID:  batchRes.BatchID,
			Type:          "debit",
			AddAttributes: loc.AddAttributes,
			Amount:        "666.000000",
			Dt:            onDt,
			SeqID:         resEvents[0].SeqID,
		}})
		s.Assert().ElementsMatch(subaccounts, []subaccountInfo{
			{},
			{*event.Loc.Attributes["a3"]},
		})
		s.Assert().Equal(shardKey, s.getAccountShardKey(accountID))
		s.checkEntityIDs(s.getEventIDs(accountID), shard)
		s.checkEntityIDs(s.getSubAccountIDs(accountID), shard)
	}

	for _, state := range states {
		loc, err := s.ctx.EntitySettings.State().Location(&state.Loc)
		s.Require().NoError(err)
		stateData := s.getState(loc.Namespace, loc.Type, loc.Attributes[:])
		s.checkEntityID(stateData.ID, shard)
		s.Assert().Equal(state.State, stateData.State)
		stateEvents := s.getStateEvents(stateData.ID)
		s.Assert().Equal(stateEvents, []eventTechData{{
			State:   string(state.State),
			BatchID: batchRes.BatchID,
		}})
		s.Assert().Equal(shardKey, s.getStateShardKey(stateData.ID))
		s.checkEntityIDs(s.getStateEventIDs(stateData.ID), shard)
	}
}

func (s *BatchWriteTestSuite) TestEverythingSingleNamespace() {
	s.testEverything([]string{"test", "test"})
}

func (s *BatchWriteTestSuite) TestEverythingMultipleNamespaces() {
	s.testEverything([]string{"test", "add_test"})
}

func (s *BatchWriteTestSuite) TestLockValidateFail() {
	commonAttr := btesting.RandSP(100)

	shard, err := s.ctx.Shards.GetLastShard()
	if err != nil {
		s.T().Fatal(err)
	}

	lock := entities.LockAction{
		Loc: entities.LocationAttributes{
			Namespace:  "test",
			Type:       "lock_type",
			Attributes: map[string]*string{"a1": commonAttr, "a2": btesting.RandSP(100)},
		},
		Mode: entities.LockValidateMode,
	}
	createdLocks, err := shard.GetLockStorage(s.ctx.Templates).Init(
		s.ctx,
		[]entities.Location{s.attr2loc(lock.Loc)},
		666)
	if err != nil {
		s.T().Fatal(err)
	}
	lock.UID = s.genUID()

	events := []entities.EventAttributes{{
		Loc: entities.LocationAttributes{
			Namespace: "test",
			Type:      "account_type",
			Attributes: map[string]*string{
				"a1": commonAttr,
				"a2": btesting.RandSP(100),
				"a3": btesting.RandSP(100),
			},
		},
		Type:   "debit",
		Dt:     time.Now(),
		Amount: "666",
	}}

	states := []entities.StateAttributes{{
		Loc: entities.LocationAttributes{
			Namespace: "test",
			Type:      "state_type",
			Attributes: map[string]*string{
				"a1": commonAttr,
				"a2": btesting.RandSP(100),
			},
		},
		State: []byte(fmt.Sprintf(`["%s"]`, btesting.RandS(10))),
	}}

	_, err = s.ctx.Actions.WriteBatch(
		s.ctx,
		entities.BatchWriteRequest{
			EventType:  "test",
			ExternalID: btesting.RandS(100),
			Dt:         time.Now(),
			Events:     events,
			States:     states,
			Locks:      []entities.LockAction{lock},
		},
	)
	callDt := time.Now().UTC()

	if assert.Error(s.T(), err) {
		var codedErr coreerrors.CodedError
		assert.True(s.T(), xerrors.As(err, &codedErr))
		assert.Equal(s.T(), codedErr.CharCode(), "LOCK_VALIDATE_FAIL")

		lockInfo := s.getLockInfo(s.attr2loc(lock.Loc))
		assert.Equal(s.T(), createdLocks[0].UID, lockInfo.UID)
		assert.True(s.T(), lockInfo.DT.After(callDt))
		assert.False(s.T(), s.checkAccountExists(s.attr2loc(events[0].Loc)))
		assert.False(s.T(), s.checkStateExists(s.attr2loc(states[0].Loc)))
	}
}

func (s *BatchWriteTestSuite) TestLockRemoveFail() {
	commonAttr := btesting.RandSP(100)

	shard, err := s.ctx.Shards.GetLastShard()
	if err != nil {
		s.T().Fatal(err)
	}

	lock := entities.LockAction{
		Loc: entities.LocationAttributes{
			Namespace:  "test",
			Type:       "lock_type",
			Attributes: map[string]*string{"a1": commonAttr, "a2": btesting.RandSP(100)},
		},
		Mode: entities.LockRemoveMode,
	}
	createdLocks, err := shard.GetLockStorage(s.ctx.Templates).Init(
		s.ctx,
		[]entities.Location{s.attr2loc(lock.Loc)},
		666)
	if err != nil {
		s.T().Fatal(err)
	}
	lock.UID = s.genUID()

	states := []entities.StateAttributes{{
		Loc: entities.LocationAttributes{
			Namespace: "test",
			Type:      "state_type",
			Attributes: map[string]*string{
				"a1": commonAttr,
				"a2": btesting.RandSP(100),
			},
		},
		State: []byte(fmt.Sprintf(`["%s"]`, btesting.RandS(10))),
	}}

	_, err = s.ctx.Actions.WriteBatch(
		s.ctx,
		entities.BatchWriteRequest{
			EventType:  "test",
			ExternalID: btesting.RandS(100),
			Dt:         time.Now(),
			States:     states,
			Locks:      []entities.LockAction{lock},
		},
	)
	callDt := time.Now().UTC()

	if assert.Error(s.T(), err) {
		var codedErr coreerrors.CodedError
		assert.True(s.T(), xerrors.As(err, &codedErr))
		assert.Equal(s.T(), codedErr.CharCode(), "LOCK_UPDATE_FAIL")

		lockInfo := s.getLockInfo(s.attr2loc(lock.Loc))
		assert.Equal(s.T(), createdLocks[0].UID, lockInfo.UID)
		assert.True(s.T(), lockInfo.DT.After(callDt))
		assert.False(s.T(), s.checkStateExists(s.attr2loc(states[0].Loc)))
	}
}

func (s *BatchWriteTestSuite) TestInvalidLock() {
	commonAttr := btesting.RandSP(100)

	shard, err := s.ctx.Shards.GetLastShard()
	if err != nil {
		s.T().Fatal(err)
	}

	loc := s.attr2loc(entities.LocationAttributes{
		Namespace:  "test",
		Type:       "lock_type",
		Attributes: map[string]*string{"a1": commonAttr, "a2": btesting.RandSP(100)},
	})
	createdLocks, err := shard.GetLockStorage(s.ctx.Templates).Init(s.ctx, []entities.Location{loc}, 666)
	if err != nil {
		s.T().Fatal(err)
	}

	states := []entities.StateAttributes{{
		Loc: entities.LocationAttributes{
			Namespace: "test",
			Type:      "state_type",
			Attributes: map[string]*string{
				"a1": commonAttr,
				"a2": btesting.RandSP(100),
			},
		},
		State: []byte(fmt.Sprintf(`["%s"]`, btesting.RandS(10))),
	}}

	_, err = s.ctx.Actions.WriteBatch(
		s.ctx,
		entities.BatchWriteRequest{
			EventType:  "test",
			ExternalID: btesting.RandS(100),
			Dt:         time.Now(),
			States:     states,
			Locks: []entities.LockAction{{
				Loc: entities.LocationAttributes{
					Namespace:  loc.Namespace,
					Type:       loc.Type,
					Attributes: map[string]*string{"a1": commonAttr},
				},
				Mode: entities.LockRemoveMode,
			}},
		},
	)
	callDt := time.Now().UTC()

	if assert.Error(s.T(), err) {
		var codedErr coreerrors.CodedError
		assert.True(s.T(), xerrors.As(err, &codedErr))
		assert.Equal(s.T(), codedErr.CharCode(), "INVALID_SETTINGS")
		assert.Equal(s.T(), err.Error(), "failed to get shard: failed to validate attributes: no attribute a2")

		lockInfo := s.getLockInfo(loc)
		assert.Equal(s.T(), createdLocks[0].UID, lockInfo.UID)
		assert.True(s.T(), lockInfo.DT.After(callDt))
		assert.False(s.T(), s.checkStateExists(s.attr2loc(states[0].Loc)))
	}
}

func (s *BatchWriteTestSuite) TestInvalidEvent() {
	commonAttr := btesting.RandSP(100)

	events := []entities.EventAttributes{{
		Loc: entities.LocationAttributes{
			Namespace: "test",
			Type:      "account_type",
			Attributes: map[string]*string{
				"a1": commonAttr,
				"a2": btesting.RandSP(100),
			},
		},
		Type:   "debit",
		Dt:     time.Now(),
		Amount: "666",
	}}

	states := []entities.StateAttributes{{
		Loc: entities.LocationAttributes{
			Namespace: "test",
			Type:      "state_type",
			Attributes: map[string]*string{
				"a1": commonAttr,
				"a2": btesting.RandSP(100),
			},
		},
		State: []byte(fmt.Sprintf(`["%s"]`, btesting.RandS(10))),
	}}

	_, err := s.ctx.Actions.WriteBatch(
		s.ctx,
		entities.BatchWriteRequest{
			EventType:  "test",
			ExternalID: btesting.RandS(100),
			Dt:         time.Now(),
			Events:     events,
			States:     states,
		},
	)

	if assert.Error(s.T(), err) {
		var codedErr coreerrors.CodedError
		assert.True(s.T(), xerrors.As(err, &codedErr))
		assert.Equal(s.T(), codedErr.CharCode(), "INVALID_SETTINGS")
		assert.Equal(s.T(), err.Error(),
			"failed to get shard: failed to validate attributes: no additional attribute a3")

		assert.False(s.T(), s.checkAccountExists(s.attr2loc(events[0].Loc)))
		assert.False(s.T(), s.checkStateExists(s.attr2loc(states[0].Loc)))
	}
}

func (s *BatchWriteTestSuite) TestInvalidState() {
	commonAttr := btesting.RandSP(100)

	events := []entities.EventAttributes{{
		Loc: entities.LocationAttributes{
			Namespace: "test",
			Type:      "account_type",
			Attributes: map[string]*string{
				"a1": commonAttr,
				"a2": btesting.RandSP(100),
				"a3": btesting.RandSP(100),
			},
		},
		Type:   "debit",
		Dt:     time.Now(),
		Amount: "666",
	}}

	states := []entities.StateAttributes{{
		Loc: entities.LocationAttributes{
			Namespace: "test",
			Type:      "state_type",
			Attributes: map[string]*string{
				"a1": commonAttr,
			},
		},
		State: []byte(fmt.Sprintf(`["%s"]`, btesting.RandS(10))),
	}}

	_, err := s.ctx.Actions.WriteBatch(
		s.ctx,
		entities.BatchWriteRequest{
			EventType:  "test",
			ExternalID: btesting.RandS(100),
			Dt:         time.Now(),
			Events:     events,
			States:     states,
		},
	)

	if assert.Error(s.T(), err) {
		var codedErr coreerrors.CodedError
		assert.True(s.T(), xerrors.As(err, &codedErr))
		assert.Equal(s.T(), codedErr.CharCode(), "INVALID_SETTINGS")
		assert.Equal(s.T(), err.Error(),
			"failed to get shard: failed to validate attributes: no attribute a2")

		assert.False(s.T(), s.checkAccountExists(s.attr2loc(events[0].Loc)))
	}
}

func (s *BatchWriteTestSuite) TestMultipleShards() {
	events := []entities.EventAttributes{{
		Loc: entities.LocationAttributes{
			Namespace: "test",
			Type:      "account_type",
			Attributes: map[string]*string{
				"a1": btesting.RandSP(100),
				"a2": btesting.RandSP(100),
				"a3": btesting.RandSP(100),
			},
		},
		Type:   "debit",
		Dt:     time.Now(),
		Amount: "666",
	}}

	states := []entities.StateAttributes{{
		Loc: entities.LocationAttributes{
			Namespace: "test",
			Type:      "state_type",
			Attributes: map[string]*string{
				"a1": btesting.RandSP(100),
				"a2": btesting.RandSP(100),
			},
		},
		State: []byte(fmt.Sprintf(`["%s"]`, btesting.RandS(10))),
	}}

	_, err := s.ctx.Actions.WriteBatch(
		s.ctx,
		entities.BatchWriteRequest{
			EventType:  "test",
			ExternalID: btesting.RandS(100),
			Dt:         time.Now(),
			Events:     events,
			States:     states,
		},
	)

	if assert.Error(s.T(), err) {
		var codedErr coreerrors.CodedError
		assert.True(s.T(), xerrors.As(err, &codedErr))
		assert.Equal(s.T(), codedErr.CharCode(), "MULTIPLE_SHARDS")
		assert.Equal(s.T(), err.Error(), "failed to get shard: multiple shards")

		assert.False(s.T(), s.checkAccountExists(s.attr2loc(events[0].Loc)))
		assert.False(s.T(), s.checkStateExists(s.attr2loc(states[0].Loc)))
	}
}

func (s *BatchWriteTestSuite) TestIdempotence() {
	batchEID := btesting.RandS(100)

	genEvents := func() []entities.EventAttributes {
		return []entities.EventAttributes{{
			Loc: entities.LocationAttributes{
				Namespace: "test",
				Type:      "account_type",
				Attributes: map[string]*string{
					"a1": btesting.RandSP(100),
					"a2": btesting.RandSP(100),
					"a3": btesting.RandSP(100),
				},
			},
			Type:   "debit",
			Dt:     time.Now(),
			Amount: "666",
		}}
	}

	firstBatchRes, err := s.ctx.Actions.WriteBatch(
		s.ctx,
		entities.BatchWriteRequest{
			EventType:  "test",
			ExternalID: batchEID,
			Dt:         time.Now(),
			Events:     genEvents(),
		})
	if err != nil {
		s.T().Fatal(err)
	}
	if firstBatchRes.IsExisting {
		s.T().Fatal("first batch already exists!")
	}

	events := genEvents()
	secondBatchRes, err := s.ctx.Actions.WriteBatch(
		s.ctx,
		entities.BatchWriteRequest{
			EventType:  "test",
			ExternalID: batchEID,
			Dt:         time.Now(),
			Events:     events,
		})
	if err != nil {
		s.T().Fatal(err)
	}
	assert.Equal(s.T(), secondBatchRes, &entities.BatchWriteResponse{BatchID: firstBatchRes.BatchID, IsExisting: true})
	assert.False(s.T(), s.checkAccountExists(s.attr2loc(events[0].Loc)))
}

func (s *BatchWriteTestSuite) TestIdempotenceLocks() {
	shard, err := s.ctx.Shards.GetLastShard()
	if err != nil {
		s.T().Fatal(err)
	}

	commonAttr := btesting.RandSP(100)
	batchEID := btesting.RandS(100)

	genEvents := func() []entities.EventAttributes {
		return []entities.EventAttributes{{
			Loc: entities.LocationAttributes{
				Namespace: "test",
				Type:      "account_type",
				Attributes: map[string]*string{
					"a1": commonAttr,
					"a2": btesting.RandSP(100),
					"a3": btesting.RandSP(100),
				},
			},
			Type:   "debit",
			Dt:     time.Now(),
			Amount: "666",
		}}
	}

	lockLocation := entities.LocationAttributes{
		Namespace: "test",
		Type:      "lock_type",
		Attributes: map[string]*string{
			"a1": commonAttr,
			"a2": btesting.RandSP(100),
		},
	}
	newLocks, err := shard.GetLockStorage(s.ctx.Templates).Init(
		s.ctx,
		[]entities.Location{s.attr2loc(lockLocation)},
		666,
	)
	if err != nil {
		s.T().Fatal(err)
	}
	callDt := time.Now().UTC()

	firstBatchRes, err := s.ctx.Actions.WriteBatch(
		s.ctx,
		entities.BatchWriteRequest{
			EventType:  "test",
			ExternalID: batchEID,
			Dt:         time.Now(),
			Events:     genEvents(),
		})
	if err != nil {
		s.T().Fatal(err)
	}
	if firstBatchRes.IsExisting {
		s.T().Fatal("first batch already exists!")
	}

	events := genEvents()
	secondBatchRes, err := s.ctx.Actions.WriteBatch(
		s.ctx,
		entities.BatchWriteRequest{
			EventType:  "test",
			ExternalID: batchEID,
			Dt:         time.Now(),
			Events:     events,
			Locks: []entities.LockAction{{
				Loc:  lockLocation,
				UID:  newLocks[0].UID,
				Mode: entities.LockRemoveMode,
			}},
		})
	if err != nil {
		s.T().Fatal(err)
	}
	assert.Equal(s.T(), secondBatchRes, &entities.BatchWriteResponse{BatchID: firstBatchRes.BatchID, IsExisting: true})
	assert.False(s.T(), s.checkAccountExists(s.attr2loc(events[0].Loc)))

	lockInfo := s.getLockInfo(s.attr2loc(lockLocation))
	assert.True(s.T(), lockInfo.DT.After(callDt))
}

type BatchReadTestSuite struct {
	CommonBatchOperationTestSuite
}

func (s *BatchReadTestSuite) testEverything(namespaces [2]string) {
	commonAttr := btesting.RandSP(100)

	shard, err := s.ctx.Shards.GetLastShard()
	if err != nil {
		s.T().Fatal(err)
	}

	lockModes := []entities.LockActionMode{
		entities.LockGetMode,
		entities.LockInitMode,
	}
	locks := make([]entities.LockAction, len(lockModes)*len(namespaces))
	var i int
	for _, mode := range lockModes {
		for _, ns := range namespaces {
			locks[i] = entities.LockAction{
				Loc: entities.LocationAttributes{
					Namespace:  ns,
					Type:       "lock_type",
					Attributes: map[string]*string{"a1": commonAttr, "a2": btesting.RandSP(100)},
				},
				Mode: mode,
			}
			location, err := s.ctx.EntitySettings.Lock().Location(&locks[i].Loc)
			if err != nil {
				s.T().Fatal(err)
			}
			newLocks, err := shard.GetLockStorage(s.ctx.Templates).Init(
				s.ctx,
				[]entities.Location{location},
				-666)
			if err != nil {
				s.T().Fatal(err)
			}
			locks[i].UID = newLocks[0].UID
			i++
		}
	}

	startDT := time.Now().Truncate(time.Microsecond).UTC().Add(-10 * time.Second)
	events := make([]entities.EventAttributes, 20)
	eventLocs := make([]entities.LocationAttributes, len(namespaces))
	for i, ns := range namespaces {
		eventLocs[i] = entities.LocationAttributes{
			Namespace: ns,
			Type:      "account_type",
			Attributes: map[string]*string{
				"a1": commonAttr,
				"a2": btesting.RandSP(100),
				"a3": btesting.RandSP(100),
			},
		}
	}
	for i := range events {
		loc := eventLocs[i%2]
		events[i] = entities.EventAttributes{
			Loc:    loc,
			Type:   "debit",
			Dt:     startDT.Add(time.Duration(i) * time.Second),
			Amount: "666",
		}
	}

	states := make([]entities.StateAttributes, len(namespaces))
	for i, ns := range namespaces {
		states[i] = entities.StateAttributes{
			Loc: entities.LocationAttributes{
				Namespace: ns,
				Type:      "state_type",
				Attributes: map[string]*string{
					"a1": commonAttr,
					"a2": btesting.RandSP(100),
				},
			},
			State: []byte(fmt.Sprintf(`["%s"]`, btesting.RandS(10))),
		}
	}

	batchID := btesting.RandS(100)
	_, err = s.ctx.Actions.WriteBatch(
		s.ctx,
		entities.BatchWriteRequest{
			EventType:  "test",
			ExternalID: batchID,
			Dt:         time.Now(),
			Events:     events,
			States:     states,
		},
	)
	if err != nil {
		s.T().Fatal(err)
	}

	readRes, err := s.ctx.Actions.ReadBatch(
		s.ctx,
		entities.BatchReadRequest{
			LockTimeout: 666,
			Locks:       locks,
			Balances: []entities.DtRequestAttributes{
				{
					Loc: eventLocs[0],
					Dt:  startDT.Add(10 * time.Second),
				},
				{
					Loc: eventLocs[1],
					Dt:  startDT.Add(15 * time.Second),
				},
			},
			Turnovers: []entities.PeriodRequestAttributes{
				{
					Loc:    eventLocs[0],
					DtFrom: startDT.Add(5 * time.Second),
					DtTo:   startDT.Add(15 * time.Second),
				},
				{
					Loc:    eventLocs[1],
					DtFrom: startDT.Add(10 * time.Second),
					DtTo:   startDT.Add(18 * time.Second),
				},
			},
			DetailedTurnovers: []entities.PeriodRequestAttributes{
				{
					Loc:    eventLocs[0],
					DtFrom: startDT.Add(5 * time.Second),
					DtTo:   startDT.Add(7 * time.Second),
				},
				{
					Loc:    eventLocs[1],
					DtFrom: startDT.Add(8 * time.Second),
					DtTo:   startDT.Add(10 * time.Second),
				},
			},
			States: []entities.LocationAttributes{
				states[0].Loc,
				states[1].Loc,
				// Does not exist
				{
					Namespace: "test",
					Type:      "state_type",
					Attributes: map[string]*string{
						"a1": commonAttr,
						"a2": btesting.RandSP(100),
					},
				},
			},
		},
	)
	if err != nil {
		s.T().Fatal(err)
	}

	lockRes := make([]entities.LockAttributes, 0, len(locks))
	for _, lock := range locks {
		location, err := s.ctx.EntitySettings.Lock().Location(&lock.Loc)
		if err != nil {
			s.T().Fatal(err)
		}
		lockInfo := s.getLockInfo(location)
		if lock.Mode == entities.LockGetMode {
			assert.Equal(s.T(), lock.UID, lockInfo.UID)
			assert.True(s.T(), lockInfo.DT.Before(startDT))
		} else {
			assert.NotEqual(s.T(), lock.UID, lockInfo.UID)
			assert.True(s.T(), lockInfo.DT.After(startDT))
		}
		lockRes = append(lockRes, entities.LockAttributes{
			Loc: lock.Loc,
			Dt:  entities.APIDt(lockInfo.DT),
			UID: lockInfo.UID,
		})
	}

	assert.ElementsMatch(s.T(), readRes.Locks, lockRes)
	assert.ElementsMatch(s.T(), readRes.Balances, []entities.BalanceAttributesDt{
		{
			Loc:    eventLocs[0],
			Dt:     entities.APIDt(startDT.Add(10 * time.Second)),
			Debit:  "3330.000000",
			Credit: "0",
		},
		{
			Loc:    eventLocs[1],
			Dt:     entities.APIDt(startDT.Add(15 * time.Second)),
			Debit:  "4662.000000",
			Credit: "0",
		},
	})
	assert.ElementsMatch(s.T(), readRes.Turnovers, []entities.TurnoverAttributesDt{
		{
			Loc:            eventLocs[0],
			DtFrom:         entities.APIDt(startDT.Add(5 * time.Second)),
			DtTo:           entities.APIDt(startDT.Add(15 * time.Second)),
			DebitInit:      "1998.000000",
			CreditInit:     "0",
			DebitTurnover:  "3330.000000",
			CreditTurnover: "0",
		},
		{
			Loc:            eventLocs[1],
			DtFrom:         entities.APIDt(startDT.Add(10 * time.Second)),
			DtTo:           entities.APIDt(startDT.Add(18 * time.Second)),
			DebitInit:      "3330.000000",
			CreditInit:     "0",
			DebitTurnover:  "2664.000000",
			CreditTurnover: "0",
		},
	})
	assert.ElementsMatch(s.T(), readRes.DetailedTurnovers, []entities.DetailedTurnoverAttributesDt{
		{
			Loc:            eventLocs[0],
			DtFrom:         entities.APIDt(startDT.Add(5 * time.Second)),
			DtTo:           entities.APIDt(startDT.Add(7 * time.Second)),
			DebitInit:      "1998.000000",
			CreditInit:     "0",
			DebitTurnover:  "666.000000",
			CreditTurnover: "0",
			Events: []entities.EventDetails{
				{
					Type:      "debit",
					Dt:        startDT.Add(6 * time.Second),
					Amount:    "666.000000",
					EventType: "test",
					EventID:   batchID,
				},
			},
		},
		{
			Loc:            eventLocs[1],
			DtFrom:         entities.APIDt(startDT.Add(8 * time.Second)),
			DtTo:           entities.APIDt(startDT.Add(10 * time.Second)),
			DebitInit:      "2664.000000",
			CreditInit:     "0",
			DebitTurnover:  "666.000000",
			CreditTurnover: "0",
			Events: []entities.EventDetails{
				{
					Type:      "debit",
					Dt:        startDT.Add(9 * time.Second),
					Amount:    "666.000000",
					EventType: "test",
					EventID:   batchID,
				},
			},
		},
	})
	assert.ElementsMatch(s.T(), readRes.States, states)
}

func (s *BatchReadTestSuite) TestEverythingSingleNamespace() {
	s.testEverything([2]string{"test", "test"})
}

func (s *BatchReadTestSuite) TestEverythingMultipleNamespaces() {
	s.testEverything([2]string{"test", "add_test"})
}

func (s *BatchReadTestSuite) TestMultipleMasks() {
	commonAttr := btesting.RandSP(100)

	startDT := time.Now().Truncate(time.Microsecond).UTC().Add(-10 * time.Second)
	events := make([]entities.EventAttributes, 10)
	eventLoc1 := entities.LocationAttributes{
		Namespace: "test",
		Type:      "account_type",
		Attributes: map[string]*string{
			"a1": commonAttr,
			"a2": btesting.RandSP(100),
			"a3": btesting.RandSP(100),
		},
	}
	eventLoc2 := entities.LocationAttributes{
		Namespace: "test",
		Type:      "account_type",
		Attributes: map[string]*string{
			"a1": commonAttr,
			"a2": btesting.RandSP(100),
			"a3": btesting.RandSP(100),
		},
	}
	for i := range events {
		var loc entities.LocationAttributes
		var eType string
		if i%2 == 0 {
			loc = eventLoc1
			eType = "debit"
		} else {
			loc = eventLoc2
			eType = "credit"
		}
		events[i] = entities.EventAttributes{
			Loc:    loc,
			Type:   eType,
			Dt:     startDT.Add(time.Duration(i) * time.Second),
			Amount: fmt.Sprintf("%d", i),
		}
	}

	batchID := btesting.RandS(100)
	_, err := s.ctx.Actions.WriteBatch(
		s.ctx,
		entities.BatchWriteRequest{
			EventType:  "test",
			ExternalID: batchID,
			Dt:         time.Now(),
			Events:     events,
		},
	)
	if err != nil {
		s.T().Fatal(err)
	}

	readRes, err := s.ctx.Actions.ReadBatch(
		s.ctx,
		entities.BatchReadRequest{
			LockTimeout: 666,
			Balances: []entities.DtRequestAttributes{
				{
					Loc: eventLoc1,
					Dt:  startDT.Add(10 * time.Second),
				},
				{
					Loc: entities.LocationAttributes{
						Namespace: "test",
						Type:      "account_type",
						Attributes: map[string]*string{
							"a1": commonAttr,
							"a2": eventLoc2.Attributes["a2"],
							"a3": nil,
						},
					},
					Dt: startDT.Add(10 * time.Second),
				},
			},
			Turnovers: []entities.PeriodRequestAttributes{
				{
					Loc:    eventLoc1,
					DtFrom: startDT.Add(7 * time.Second),
					DtTo:   startDT.Add(9 * time.Second),
				},
				{
					Loc: entities.LocationAttributes{
						Namespace: "test",
						Type:      "account_type",
						Attributes: map[string]*string{
							"a1": commonAttr,
							"a2": eventLoc2.Attributes["a2"],
							"a3": nil,
						},
					},
					DtFrom: startDT.Add(6 * time.Second),
					DtTo:   startDT.Add(10 * time.Second),
				},
			},
			DetailedTurnovers: []entities.PeriodRequestAttributes{
				{
					Loc:    eventLoc1,
					DtFrom: startDT.Add(5 * time.Second),
					DtTo:   startDT.Add(7 * time.Second),
				},
				{
					Loc: entities.LocationAttributes{
						Namespace: "test",
						Type:      "account_type",
						Attributes: map[string]*string{
							"a1": commonAttr,
							"a2": eventLoc2.Attributes["a2"],
							"a3": nil,
						},
					},
					DtFrom: startDT.Add(8 * time.Second),
					DtTo:   startDT.Add(10 * time.Second),
				},
			},
		},
	)
	if err != nil {
		s.T().Fatal(err)
	}

	assert.ElementsMatch(s.T(), readRes.Balances, []entities.BalanceAttributesDt{
		{
			Loc:    eventLoc1,
			Dt:     entities.APIDt(startDT.Add(10 * time.Second)),
			Debit:  "20.000000",
			Credit: "0",
		},
		{
			Loc:    eventLoc2,
			Dt:     entities.APIDt(startDT.Add(10 * time.Second)),
			Debit:  "0",
			Credit: "25.000000",
		},
	})
	assert.ElementsMatch(s.T(), readRes.Turnovers, []entities.TurnoverAttributesDt{
		{
			Loc:            eventLoc1,
			DtFrom:         entities.APIDt(startDT.Add(7 * time.Second)),
			DtTo:           entities.APIDt(startDT.Add(9 * time.Second)),
			DebitInit:      "12.000000",
			CreditInit:     "0",
			DebitTurnover:  "8.000000",
			CreditTurnover: "0",
		},
		{
			Loc:            eventLoc2,
			DtFrom:         entities.APIDt(startDT.Add(6 * time.Second)),
			DtTo:           entities.APIDt(startDT.Add(10 * time.Second)),
			DebitInit:      "0",
			CreditInit:     "9.000000",
			DebitTurnover:  "0",
			CreditTurnover: "16.000000",
		},
	})
	assert.ElementsMatch(s.T(), readRes.DetailedTurnovers, []entities.DetailedTurnoverAttributesDt{
		{
			Loc:            eventLoc1,
			DtFrom:         entities.APIDt(startDT.Add(5 * time.Second)),
			DtTo:           entities.APIDt(startDT.Add(7 * time.Second)),
			DebitInit:      "6.000000",
			CreditInit:     "0",
			DebitTurnover:  "6.000000",
			CreditTurnover: "0",
			Events: []entities.EventDetails{
				{
					Type:      "debit",
					Dt:        startDT.Add(6 * time.Second),
					Amount:    "6.000000",
					EventType: "test",
					EventID:   batchID,
				},
			},
		},
		{
			Loc:            eventLoc2,
			DtFrom:         entities.APIDt(startDT.Add(8 * time.Second)),
			DtTo:           entities.APIDt(startDT.Add(10 * time.Second)),
			DebitInit:      "0",
			CreditInit:     "16.000000",
			DebitTurnover:  "0",
			CreditTurnover: "9.000000",
			Events: []entities.EventDetails{
				{
					Type:      "credit",
					Dt:        startDT.Add(9 * time.Second),
					Amount:    "9.000000",
					EventType: "test",
					EventID:   batchID,
				},
			},
		},
	})
}

func (s *BatchReadTestSuite) TestSameAccountDifferentDates() {
	startDT := time.Now().Truncate(time.Microsecond).UTC().Add(-10 * time.Second)
	events := make([]entities.EventAttributes, 20)
	eventLoc := entities.LocationAttributes{
		Namespace: "test",
		Type:      "account_type",
		Attributes: map[string]*string{
			"a1": btesting.RandSP(100),
			"a2": btesting.RandSP(100),
			"a3": btesting.RandSP(100),
		},
	}
	for i := range events {
		var eType string
		if i%2 == 0 {
			eType = "debit"
		} else {
			eType = "credit"
		}
		events[i] = entities.EventAttributes{
			Loc:    eventLoc,
			Type:   eType,
			Dt:     startDT.Add(time.Duration(i) * time.Second),
			Amount: "666",
		}
	}

	batchID := btesting.RandS(100)
	_, err := s.ctx.Actions.WriteBatch(
		s.ctx,
		entities.BatchWriteRequest{
			EventType:  "test",
			ExternalID: batchID,
			Dt:         time.Now(),
			Events:     events,
		},
	)
	if err != nil {
		s.T().Fatal(err)
	}

	readRes, err := s.ctx.Actions.ReadBatch(
		s.ctx,
		entities.BatchReadRequest{
			LockTimeout: 666,
			Balances: []entities.DtRequestAttributes{
				{
					Loc: eventLoc,
					Dt:  startDT.Add(10 * time.Second),
				},
				{
					Loc: eventLoc,
					Dt:  startDT.Add(15 * time.Second),
				},
			},
			Turnovers: []entities.PeriodRequestAttributes{
				{
					Loc:    eventLoc,
					DtFrom: startDT.Add(5 * time.Second),
					DtTo:   startDT.Add(15 * time.Second),
				},
				{
					Loc:    eventLoc,
					DtFrom: startDT.Add(10 * time.Second),
					DtTo:   startDT.Add(18 * time.Second),
				},
			},
			DetailedTurnovers: []entities.PeriodRequestAttributes{
				{
					Loc:    eventLoc,
					DtFrom: startDT.Add(5 * time.Second),
					DtTo:   startDT.Add(6 * time.Second),
				},
				{
					Loc:    eventLoc,
					DtFrom: startDT.Add(6 * time.Second),
					DtTo:   startDT.Add(7 * time.Second),
				},
			},
		},
	)
	if err != nil {
		s.T().Fatal(err)
	}

	assert.ElementsMatch(s.T(), readRes.Balances, []entities.BalanceAttributesDt{
		{
			Loc:    eventLoc,
			Dt:     entities.APIDt(startDT.Add(10 * time.Second)),
			Debit:  "3330.000000",
			Credit: "3330.000000",
		},
		{
			Loc:    eventLoc,
			Dt:     entities.APIDt(startDT.Add(15 * time.Second)),
			Debit:  "5328.000000",
			Credit: "4662.000000",
		},
	})
	assert.ElementsMatch(s.T(), readRes.Turnovers, []entities.TurnoverAttributesDt{
		{
			Loc:            eventLoc,
			DtFrom:         entities.APIDt(startDT.Add(5 * time.Second)),
			DtTo:           entities.APIDt(startDT.Add(15 * time.Second)),
			DebitInit:      "1998.000000",
			CreditInit:     "1332.000000",
			DebitTurnover:  "3330.000000",
			CreditTurnover: "3330.000000",
		},
		{
			Loc:            eventLoc,
			DtFrom:         entities.APIDt(startDT.Add(10 * time.Second)),
			DtTo:           entities.APIDt(startDT.Add(18 * time.Second)),
			DebitInit:      "3330.000000",
			CreditInit:     "3330.000000",
			DebitTurnover:  "2664.000000",
			CreditTurnover: "2664.000000",
		},
	})
	assert.ElementsMatch(s.T(), readRes.DetailedTurnovers, []entities.DetailedTurnoverAttributesDt{
		{
			Loc:            eventLoc,
			DtFrom:         entities.APIDt(startDT.Add(5 * time.Second)),
			DtTo:           entities.APIDt(startDT.Add(6 * time.Second)),
			DebitInit:      "1998.000000",
			CreditInit:     "1332.000000",
			DebitTurnover:  "0",
			CreditTurnover: "666.000000",
			Events: []entities.EventDetails{
				{
					Type:      "credit",
					Dt:        startDT.Add(5 * time.Second),
					Amount:    "666.000000",
					EventType: "test",
					EventID:   batchID,
				},
			},
		},
		{
			Loc:            eventLoc,
			DtFrom:         entities.APIDt(startDT.Add(6 * time.Second)),
			DtTo:           entities.APIDt(startDT.Add(7 * time.Second)),
			DebitInit:      "1998.000000",
			CreditInit:     "1998.000000",
			DebitTurnover:  "666.000000",
			CreditTurnover: "0",
			Events: []entities.EventDetails{
				{
					Type:      "debit",
					Dt:        startDT.Add(6 * time.Second),
					Amount:    "666.000000",
					EventType: "test",
					EventID:   batchID,
				},
			},
		},
	})
}

func (s *BatchReadTestSuite) TestUnknownGetLock() {
	lock := entities.LockAction{
		Loc: entities.LocationAttributes{
			Namespace:  "test",
			Type:       "lock_type",
			Attributes: map[string]*string{"a1": btesting.RandSP(100), "a2": btesting.RandSP(100)},
		},
		Mode: entities.LockGetMode,
	}

	readRes, err := s.ctx.Actions.ReadBatch(
		s.ctx,
		entities.BatchReadRequest{
			LockTimeout: 666,
			Locks:       []entities.LockAction{lock},
		},
	)
	if err != nil {
		s.T().Fatal(err)
	}

	assert.Len(s.T(), readRes.Locks, 1)
	assert.Equal(s.T(), readRes.Locks[0].Loc, lock.Loc)
	assert.True(s.T(), time.Time(readRes.Locks[0].Dt).Before(time.Now().UTC()))
}

func (s *BatchReadTestSuite) TestActiveInitLock() {
	shard, err := s.ctx.Shards.GetLastShard()
	if err != nil {
		s.T().Fatal(err)
	}

	lock := entities.LockAction{
		Loc: entities.LocationAttributes{
			Namespace:  "test",
			Type:       "lock_type",
			Attributes: map[string]*string{"a1": btesting.RandSP(100), "a2": btesting.RandSP(100)},
		},
		Mode: entities.LockInitMode,
	}

	_, err = shard.GetLockStorage(s.ctx.Templates).Init(s.ctx, []entities.Location{s.attr2loc(lock.Loc)}, 666)
	if err != nil {
		s.T().Fatal(err)
	}

	_, err = s.ctx.Actions.ReadBatch(
		s.ctx,
		entities.BatchReadRequest{
			LockTimeout: 666,
			Locks:       []entities.LockAction{lock},
		},
	)

	if assert.Error(s.T(), err) {
		var codedErr coreerrors.CodedError
		assert.True(s.T(), xerrors.As(err, &codedErr))
		assert.Equal(s.T(), codedErr.CharCode(), "LOCK_UPDATE_FAIL")
	}
}

func (s *BatchReadTestSuite) TestInvalidLock() {
	lock := entities.LockAction{
		Loc: entities.LocationAttributes{
			Namespace:  "test",
			Type:       "lock_type",
			Attributes: map[string]*string{"a1": btesting.RandSP(100), "a3": btesting.RandSP(100)},
		},
		Mode: entities.LockInitMode,
	}

	_, err := s.ctx.Actions.ReadBatch(
		s.ctx,
		entities.BatchReadRequest{
			LockTimeout: 666,
			Locks:       []entities.LockAction{lock},
		},
	)

	if assert.Error(s.T(), err) {
		var codedErr coreerrors.CodedError
		assert.True(s.T(), xerrors.As(err, &codedErr))
		assert.Equal(s.T(), codedErr.CharCode(), "INVALID_SETTINGS")
		assert.Equal(s.T(), err.Error(), "failed to get shard: failed to validate attributes: no attribute a2")
	}
}

func (s *BatchReadTestSuite) TestInvalidBalance() {
	req := entities.DtRequestAttributes{
		Loc: entities.LocationAttributes{
			Namespace:  "test",
			Type:       "account_type",
			Attributes: map[string]*string{"a1": btesting.RandSP(100), "a2": btesting.RandSP(100)},
		},
		Dt: time.Unix(1610535126, 0).UTC(),
	}

	_, err := s.ctx.Actions.ReadBatch(
		s.ctx,
		entities.BatchReadRequest{
			LockTimeout: 666,
			Balances:    []entities.DtRequestAttributes{req},
		},
	)

	if assert.Error(s.T(), err) {
		var codedErr coreerrors.CodedError
		assert.True(s.T(), xerrors.As(err, &codedErr))
		assert.Equal(s.T(), codedErr.CharCode(), "INVALID_SETTINGS")
		assert.Equal(s.T(), err.Error(),
			"failed to get shard: failed to validate attributes: no additional attribute a3")
	}
}

func (s *BatchReadTestSuite) TestInvalidTurnovers() {
	req := entities.PeriodRequestAttributes{
		Loc: entities.LocationAttributes{
			Namespace:  "test",
			Type:       "account_type",
			Attributes: map[string]*string{"a2": btesting.RandSP(100), "a3": btesting.RandSP(100)},
		},
		DtFrom: time.Unix(1610535126, 0).UTC(),
		DtTo:   time.Unix(1610535127, 0).UTC(),
	}

	_, err := s.ctx.Actions.ReadBatch(
		s.ctx,
		entities.BatchReadRequest{
			LockTimeout: 666,
			Turnovers:   []entities.PeriodRequestAttributes{req},
		},
	)

	if assert.Error(s.T(), err) {
		var codedErr coreerrors.CodedError
		assert.True(s.T(), xerrors.As(err, &codedErr))
		assert.Equal(s.T(), codedErr.CharCode(), "INVALID_SETTINGS")
		assert.Equal(s.T(), err.Error(), "failed to get shard: failed to validate attributes: no attribute a1")
	}
}

func (s *BatchReadTestSuite) TestInvalidDetailedTurnovers() {
	req := entities.PeriodRequestAttributes{
		Loc: entities.LocationAttributes{
			Namespace:  "test",
			Type:       "account_type",
			Attributes: map[string]*string{"a1": btesting.RandSP(100), "a3": btesting.RandSP(100)},
		},
		DtFrom: time.Unix(1610535126, 0).UTC(),
		DtTo:   time.Unix(1610535127, 0).UTC(),
	}

	_, err := s.ctx.Actions.ReadBatch(
		s.ctx,
		entities.BatchReadRequest{
			LockTimeout:       666,
			DetailedTurnovers: []entities.PeriodRequestAttributes{req},
		},
	)

	if assert.Error(s.T(), err) {
		var codedErr coreerrors.CodedError
		assert.True(s.T(), xerrors.As(err, &codedErr))
		assert.Equal(s.T(), codedErr.CharCode(), "INVALID_SETTINGS")
		assert.Equal(s.T(), err.Error(), "failed to get shard: failed to validate attributes: no attribute a2")
	}
}

func (s *BatchReadTestSuite) TestInvalidState() {
	req := entities.LocationAttributes{
		Namespace:  "test",
		Type:       "account_type",
		Attributes: map[string]*string{"a1": btesting.RandSP(100), "a2": btesting.RandSP(100)},
	}

	_, err := s.ctx.Actions.ReadBatch(
		s.ctx,
		entities.BatchReadRequest{
			LockTimeout: 666,
			States:      []entities.LocationAttributes{req},
		},
	)

	if assert.Error(s.T(), err) {
		var codedErr coreerrors.CodedError
		assert.True(s.T(), xerrors.As(err, &codedErr))
		assert.Equal(s.T(), codedErr.CharCode(), "INVALID_SETTINGS")
		assert.Equal(s.T(), err.Error(),
			"failed to get shard: failed to validate attributes: unknown state type '[test account_type]'")
	}
}

func (s *BatchReadTestSuite) TestMultipleShards() {
	_, err := s.ctx.Actions.ReadBatch(
		s.ctx,
		entities.BatchReadRequest{
			LockTimeout: 666,
			Turnovers: []entities.PeriodRequestAttributes{{
				Loc: entities.LocationAttributes{
					Namespace: "test",
					Type:      "account_type",
					Attributes: map[string]*string{
						"a1": btesting.RandSP(100),
						"a2": btesting.RandSP(100),
						"a3": btesting.RandSP(100),
					},
				},
				DtFrom: time.Unix(1610535126, 0).UTC(),
				DtTo:   time.Unix(1610535127, 0).UTC(),
			}},
			DetailedTurnovers: []entities.PeriodRequestAttributes{{
				Loc: entities.LocationAttributes{
					Namespace: "test",
					Type:      "account_type",
					Attributes: map[string]*string{
						"a1": btesting.RandSP(100),
						"a2": btesting.RandSP(100),
						"a3": btesting.RandSP(100),
					},
				},
				DtFrom: time.Unix(1610535126, 0).UTC(),
				DtTo:   time.Unix(1610535127, 0).UTC(),
			}},
		},
	)

	if assert.Error(s.T(), err) {
		var codedErr coreerrors.CodedError
		assert.True(s.T(), xerrors.As(err, &codedErr))
		assert.Equal(s.T(), codedErr.CharCode(), "MULTIPLE_SHARDS")
		assert.Equal(s.T(), err.Error(), "failed to get shard: multiple shards")
	}
}

func (s *BatchReadTestSuite) TestDuplicateRequests() {
	baseDT := time.Now().Add(-time.Hour).Truncate(time.Second).UTC()

	loc := entities.LocationAttributes{
		Namespace: "test",
		Type:      "account_type",
		Attributes: map[string]*string{
			"a1": btesting.RandSP(100),
			"a2": btesting.RandSP(100),
			"a3": btesting.RandSP(100),
		},
	}

	batchID := btesting.RandS(100)
	_, err := s.ctx.Actions.WriteBatch(
		s.ctx,
		entities.BatchWriteRequest{
			EventType:  "test",
			ExternalID: batchID,
			Dt:         time.Now(),
			Events: []entities.EventAttributes{
				{
					Loc:    loc,
					Type:   "debit",
					Dt:     baseDT.Add(-1 * time.Second),
					Amount: "666",
				},
			},
		},
	)
	s.Require().NoError(err)

	res, err := s.ctx.Actions.ReadBatch(
		s.ctx,
		entities.BatchReadRequest{
			Balances: []entities.DtRequestAttributes{
				{
					Loc: loc,
					Dt:  baseDT,
				},
				{
					Loc: loc,
					Dt:  baseDT,
				},
			},
			Turnovers: []entities.PeriodRequestAttributes{
				{
					Loc:    loc,
					DtFrom: baseDT.Add(-10 * time.Second),
					DtTo:   baseDT.Add(10 * time.Second),
				},
				{
					Loc:    loc,
					DtFrom: baseDT.Add(-10 * time.Second),
					DtTo:   baseDT.Add(10 * time.Second),
				},
			},
			DetailedTurnovers: []entities.PeriodRequestAttributes{
				{
					Loc:    loc,
					DtFrom: baseDT.Add(-10 * time.Second),
					DtTo:   baseDT.Add(10 * time.Second),
				},
				{
					Loc:    loc,
					DtFrom: baseDT.Add(-10 * time.Second),
					DtTo:   baseDT.Add(10 * time.Second),
				},
			},
		},
	)
	s.Require().NoError(err)

	s.Assert().ElementsMatch(res.Balances, []entities.BalanceAttributesDt{
		{
			Loc:    loc,
			Dt:     entities.APIDt(baseDT),
			Debit:  "666.000000",
			Credit: "0",
		},
	})
	s.Assert().ElementsMatch(res.Turnovers, []entities.TurnoverAttributesDt{
		{
			Loc:            loc,
			DtFrom:         entities.APIDt(baseDT.Add(-10 * time.Second)),
			DtTo:           entities.APIDt(baseDT.Add(10 * time.Second)),
			DebitInit:      "0",
			DebitTurnover:  "666.000000",
			CreditInit:     "0",
			CreditTurnover: "0",
		},
	})
	s.Assert().ElementsMatch(res.DetailedTurnovers, []entities.DetailedTurnoverAttributesDt{
		{
			Loc:            loc,
			DtFrom:         entities.APIDt(baseDT.Add(-10 * time.Second)),
			DtTo:           entities.APIDt(baseDT.Add(10 * time.Second)),
			DebitInit:      "0",
			DebitTurnover:  "666.000000",
			CreditInit:     "0",
			CreditTurnover: "0",
			Events: []entities.EventDetails{{
				Type:      "debit",
				Dt:        baseDT.Add(-1 * time.Second),
				Amount:    "666.000000",
				EventType: "test",
				EventID:   batchID,
			}},
		},
	})
}

func TestBatchWriteTestSuite(t *testing.T) {
	suite.Run(t, new(BatchWriteTestSuite))
}

func TestBatchReadTestSuite(t *testing.T) {
	suite.Run(t, new(BatchReadTestSuite))
}
