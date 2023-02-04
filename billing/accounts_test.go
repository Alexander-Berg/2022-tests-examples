package impl

import (
	"testing"
	"time"

	"github.com/stretchr/testify/suite"

	"a.yandex-team.ru/billing/hot/accounts/pkg/core/entities"
	"a.yandex-team.ru/billing/library/go/billingo/pkg/storage/sql/backends/pg"
	btesting "a.yandex-team.ru/billing/library/go/billingo/pkg/testing"
)

type CommonAccountsTestSuite struct {
	ActionTestSuite
}

type eventInfo struct {
	EventID       int64
	EventBatchID  int64
	Type          string
	AddAttributes [entities.AttributesCount]string
	Amount        string
	Dt            time.Time
	SeqID         int64
}

type subaccountInfo [entities.AttributesCount]string

func genAttrMap() map[string]*string {
	return map[string]*string{
		"a1": btesting.RandSP(100),
		"a2": btesting.RandSP(100),
		"a3": btesting.RandSP(100),
		"a4": btesting.RandSP(100),
	}
}

func attr2map(a []string, aa [entities.AttributesCount]string) map[string]*string {
	return map[string]*string{
		"a1": &a[0],
		"a2": &a[1],
		"a3": &aa[0],
		"a4": &aa[1],
	}
}

func mainAttrs(m map[string]*string) []string {
	return []string{*m["a1"], *m["a2"]}
}

func addAttrsFixed(m map[string]*string) [entities.AttributesCount]string {
	return [entities.AttributesCount]string{*m["a3"], *m["a4"]}
}

func (s *CommonAccountsTestSuite) SetupTest() {
	cfg := `
manifests:
- namespace: accounts_test
  accounts:
    account_type:
      attributes:
        - a1
        - a2
      add_attributes:
        - a3
        - a4
      shard:
        prefix: p
        attributes:
          - a1
      sub_accounts:
        - []
        - - a3
        - - a4
        - - a3
          - a4
      rollup_period:
        - "0 * * *"
- namespace: rt
  accounts:
    rt:
      attributes:
        - a1
        - a2
      add_attributes:
        - a3
        - a4
      shard:
        prefix: p
        attributes:
          - a1
      sub_accounts:
        - - a3
      rollup_period:
        - "*/15 * * *"
- namespace: rt1
  accounts:
    rt1:
      attributes:
        - a1
        - a2
      add_attributes:
        - a3
        - a4
      shard:
        prefix: p
        attributes:
          - a1
      sub_accounts:
        - - a3
      rollup_period:
        - "*/15 * * *"
`
	// do not change rollup_period - rollup_test.go depend on it to be once per hour

	s.ActionTestSuite.SetupTest()
	s.ActionTestSuite.loadCfg(cfg)
	s.ActionTestSuite.initActions()
	s.ActionTestSuite.initSequencer()
}

func (s *CommonAccountsTestSuite) map2attrs(attrsMap map[string]*string) entities.LocationAttributes {
	return entities.LocationAttributes{
		Namespace:  "accounts_test",
		Type:       "account_type",
		Attributes: attrsMap,
	}
}

func (s *CommonAccountsTestSuite) map2attrsWithLocationAttributes(aNamespace, aType string, attrsMap map[string]*string) entities.LocationAttributes {
	return entities.LocationAttributes{
		Namespace:  aNamespace,
		Type:       aType,
		Attributes: attrsMap,
	}
}

func (s *CommonAccountsTestSuite) slice2loc(attributes []string) entities.Location {
	attrsMap := map[string]*string{"a1": &attributes[0], "a2": &attributes[1]}
	locAttrs := s.map2attrs(attrsMap)
	loc, err := s.ctx.EntitySettings.Account().Location(&locAttrs)
	s.Require().NoError(err)
	return loc
}

func (s *CommonAccountsTestSuite) slice2locWithLocationAttributes(aNamespace, aType string, attributes []string) entities.Location {
	attrsMap := map[string]*string{"a1": &attributes[0], "a2": &attributes[1]}
	locAttrs := s.map2attrsWithLocationAttributes(aNamespace, aType, attrsMap)
	loc, err := s.ctx.EntitySettings.Account().Location(&locAttrs)
	s.Require().NoError(err)
	return loc
}

func (s *CommonAccountsTestSuite) createAccount(attributes []string) int64 {
	loc := s.slice2loc(attributes)
	return s.createAccountWithLocationAttributes(loc.Namespace, loc.Type, attributes)
}

func (s *CommonAccountsTestSuite) createAccountWithLocationAttributes(aNamespace, aType string, attributes []string) int64 {
	// nolinter: lll
	query := `
INSERT INTO acc.t_account (id, namespace, type, shard_key, attribute_1, attribute_2, attribute_3, attribute_4, attribute_5)
VALUES (nextval('acc.s_account_id'), $1, $2, $3, $4, $5, $6, $7, $8)
RETURNING id`

	loc := s.slice2locWithLocationAttributes(aNamespace, aType, attributes)
	arguments := []any{aNamespace, aType, loc.ShardKey}
	for i := 0; i < entities.AttributesCount; i++ {
		var attrVal string
		if i >= len(attributes) {
			attrVal = ""
		} else {
			attrVal = attributes[i]
		}
		arguments = append(arguments, attrVal)
	}

	shard, err := s.ctx.Shards.GetLastShard()
	s.Require().NoError(err)

	rows, err := shard.GetDatabase(s.ctx, pg.MasterPrefered).QueryxContext(s.ctx, query, arguments...)
	s.Require().NoError(err)
	//goland:noinspection GoUnhandledErrorResult
	defer rows.Close()

	var resID int64
	if rows.Next() {
		err = rows.Scan(&resID)
		s.Require().NoError(err)
	}
	return resID
}

func (s *CommonAccountsTestSuite) createSubAccount(accountID int64, attributes []string) int64 {
	query := `
INSERT INTO acc.t_subaccount (id, account_id, attribute_1, attribute_2, attribute_3, attribute_4, attribute_5)
VALUES (nextval('acc.s_subaccount_id'), $1, $2, $3, $4, $5, $6)
RETURNING id`

	arguments := []any{accountID}
	for i := 0; i < entities.AttributesCount; i++ {
		var attrVal string
		if i >= len(attributes) {
			attrVal = ""
		} else {
			attrVal = attributes[i]
		}
		arguments = append(arguments, attrVal)
	}

	shard, err := s.ctx.Shards.GetLastShard()
	s.Require().NoError(err)

	rows, err := shard.GetDatabase(s.ctx, pg.MasterPrefered).QueryxContext(s.ctx, query, arguments...)
	s.Require().NoError(err)
	//goland:noinspection GoUnhandledErrorResult
	defer rows.Close()

	var resID int64
	if rows.Next() {
		err = rows.Scan(&resID)
		s.Require().NoError(err)
	}
	return resID
}

func (s *CommonAccountsTestSuite) rollupAccount(accountID, subaccountID int64, dt time.Time, debit, credit string) {
	shard, err := s.ctx.Shards.GetLastShard()
	s.Require().NoError(err)
	db := shard.GetDatabase(s.ctx, pg.MasterPrefered)

	query := `
UPDATE acc.t_account
SET aggregated_sequence_pos = (SELECT COALESCE(MAX(seq_id), 0)
FROM acc.t_event e WHERE e.account_id = $1 and e.id between $2 and $3)
WHERE id = $1`
	_, err = db.ExecContext(s.ctx, query, accountID, shard.GetMinSeqID(), shard.GetMaxValidSeqID())
	s.Require().NoError(err)

	query = "INSERT INTO acc.t_subaccount_rollup (subaccount_id, dt, debit, credit) VALUES ($1, $2, $3, $4)"
	_, err = db.ExecContext(s.ctx, query, subaccountID, dt, debit, credit)
	s.Require().NoError(err)
}

func (s *CommonAccountsTestSuite) insertRollupTime(aNamespace, aType string, lowerBound, upperBound time.Time) {
	shard, err := s.ctx.Shards.GetLastShard()
	s.Require().NoError(err)
	db := shard.GetDatabase(s.ctx, pg.Master)

	query := `
insert into acc.t_rollup_time(id, namespace, type, lower_bound, upper_bound)
values (nextval('acc.s_rollup_time_id'), $1, $2, $3, $4)
`
	_, err = db.ExecContext(s.ctx, query, aNamespace, aType, lowerBound, upperBound)
	s.Require().NoError(err)
}

func (s *CommonAccountsTestSuite) genWriteRequestInfo(events []entities.EventAttributes,
	info []byte) entities.BatchWriteRequest {

	return entities.BatchWriteRequest{
		EventType:  "test",
		ExternalID: btesting.RandS(100),
		Info:       info,
		Events:     events,
	}
}

func (s *CommonAccountsTestSuite) genWriteRequest(events []entities.EventAttributes) entities.BatchWriteRequest {
	return s.genWriteRequestInfo(events, []byte("{}"))
}

func (s *CommonAccountsTestSuite) updateEventSeqID(expectedRows int) {
	rows, locked, err := s.ctx.Sequencer.UpdateEventSeqID(s.ctx,
		s.ctx.Shards.GetLastShardID())
	s.Require().NoError(err)

	s.Assert().Equal(int64(expectedRows), rows, "unexpected number of updated events")
	s.Assert().Equal(false, locked, "the shard should not have been locked")
}

type AccountsAddEventsTestSuite struct {
	CommonAccountsTestSuite
}

func (s *AccountsAddEventsTestSuite) TestNewAccount() {
	attrsMap := genAttrMap()
	onDt := time.Now().UTC()

	batchRes, err := s.ctx.Actions.WriteBatch(
		s.ctx,
		s.genWriteRequest([]entities.EventAttributes{{
			Loc:    s.map2attrs(attrsMap),
			Type:   "debit",
			Dt:     onDt,
			Amount: "666.666",
		}}))
	s.Require().NoError(err)

	accountID := s.getAccount("accounts_test", "account_type", mainAttrs(attrsMap))
	events := s.getEvents(accountID)
	subaccounts := s.getSubaccounts(accountID)
	shard, err := s.ctx.Shards.GetLastShard()
	s.Require().NoError(err)
	s.Assert().Equal(events, []eventInfo{{
		EventBatchID:  batchRes.BatchID,
		Type:          "debit",
		AddAttributes: addAttrsFixed(attrsMap),
		Amount:        "666.666000",
		Dt:            onDt.Truncate(time.Nanosecond * 1000),
		SeqID:         shard.GetMaxSeqID(),
	}})
	s.Assert().ElementsMatch(subaccounts, []subaccountInfo{
		{},
		{*attrsMap["a3"]},
		{"", *attrsMap["a4"]},
		{*attrsMap["a3"], *attrsMap["a4"]},
	})
}

func (s *AccountsAddEventsTestSuite) TestExistingAccount() {
	attrsMap := genAttrMap()
	onDt := time.Now().UTC()

	insertedAccountID := s.createAccount(mainAttrs(attrsMap))

	batchRes, err := s.ctx.Actions.WriteBatch(
		s.ctx,
		s.genWriteRequest([]entities.EventAttributes{{
			Loc:    s.map2attrs(attrsMap),
			Type:   "debit",
			Dt:     onDt,
			Amount: "666.66",
		}}))
	s.Require().NoError(err)

	accountID := s.getAccount("accounts_test", "account_type", mainAttrs(attrsMap))
	s.Assert().Equal(accountID, insertedAccountID)

	events := s.getEvents(accountID)
	subaccounts := s.getSubaccounts(accountID)
	s.Assert().Equal(events, []eventInfo{{
		EventBatchID:  batchRes.BatchID,
		Type:          "debit",
		AddAttributes: addAttrsFixed(attrsMap),
		Amount:        "666.660000",
		Dt:            onDt.Truncate(time.Nanosecond * 1000),
		SeqID:         events[0].SeqID,
	}})
	s.Assert().ElementsMatch(subaccounts, []subaccountInfo{
		{},
		{*attrsMap["a3"]},
		{"", *attrsMap["a4"]},
		{*attrsMap["a3"], *attrsMap["a4"]},
	})
}

func (s *AccountsAddEventsTestSuite) TestExistingSubAccount() {
	attrsMap := genAttrMap()
	onDt := time.Now().UTC()

	insertedAccountID := s.createAccount(mainAttrs(attrsMap))
	s.createSubAccount(insertedAccountID, []string{"", *attrsMap["a4"]})

	batchRes, err := s.ctx.Actions.WriteBatch(
		s.ctx,
		s.genWriteRequest([]entities.EventAttributes{{
			Loc:    s.map2attrs(attrsMap),
			Type:   "debit",
			Dt:     onDt,
			Amount: "666.66",
		}}))
	s.Require().NoError(err)

	accountID := s.getAccount("accounts_test", "account_type", mainAttrs(attrsMap))
	s.Assert().Equal(accountID, insertedAccountID)

	events := s.getEvents(accountID)
	subaccounts := s.getSubaccounts(accountID)
	s.Assert().Equal(events, []eventInfo{{
		EventBatchID:  batchRes.BatchID,
		Type:          "debit",
		AddAttributes: addAttrsFixed(attrsMap),
		Amount:        "666.660000",
		Dt:            onDt.Truncate(time.Nanosecond * 1000),
		SeqID:         events[0].SeqID,
	}})
	s.Assert().ElementsMatch(subaccounts, []subaccountInfo{
		{},
		{*attrsMap["a3"]},
		{"", *attrsMap["a4"]},
		{*attrsMap["a3"], *attrsMap["a4"]},
	})
}

func (s *AccountsAddEventsTestSuite) TestMultipleMixedExisting() {
	commonAttr := btesting.RandS(100)
	addAttrs := [entities.AttributesCount]string{btesting.RandS(100)}
	attrs1 := []string{commonAttr, btesting.RandS(100)}
	attrs2 := []string{commonAttr, btesting.RandS(100)}

	onDt := time.Now().UTC()

	insertedAccountID := s.createAccount(attrs1)

	batchRes, err := s.ctx.Actions.WriteBatch(
		s.ctx,
		s.genWriteRequest([]entities.EventAttributes{
			{
				Loc:    s.map2attrs(attr2map(attrs1, addAttrs)),
				Type:   "debit",
				Dt:     onDt,
				Amount: "123.456",
			},
			{
				Loc:    s.map2attrs(attr2map(attrs2, addAttrs)),
				Type:   "credit",
				Dt:     onDt,
				Amount: "654.321",
			},
		}))
	s.Require().NoError(err)

	accountID1 := s.getAccount("accounts_test", "account_type", attrs1)
	accountID2 := s.getAccount("accounts_test", "account_type", attrs2)
	s.Assert().Equal(accountID1, insertedAccountID)

	events1 := s.getEvents(accountID1)
	s.Assert().Equal(events1, []eventInfo{{
		EventBatchID:  batchRes.BatchID,
		Type:          "debit",
		AddAttributes: addAttrs,
		Amount:        "123.456000",
		Dt:            onDt.Truncate(time.Nanosecond * 1000),
		SeqID:         events1[0].SeqID,
	}})

	events2 := s.getEvents(accountID2)
	s.Assert().Equal(events2, []eventInfo{{
		EventBatchID:  batchRes.BatchID,
		Type:          "credit",
		AddAttributes: addAttrs,
		Amount:        "654.321000",
		Dt:            onDt.Truncate(time.Nanosecond * 1000),
		SeqID:         events2[0].SeqID,
	}})
}

func (s *AccountsAddEventsTestSuite) TestSingleAccountMultipleSubaccounts() {
	attrs := []string{
		btesting.RandS(100),
		btesting.RandS(100),
	}
	addAttrs1 := [entities.AttributesCount]string{
		btesting.RandS(100),
		btesting.RandS(100),
	}
	addAttrs2 := [entities.AttributesCount]string{
		btesting.RandS(100),
		btesting.RandS(100),
	}

	onDt := time.Now().UTC()

	batchRes, err := s.ctx.Actions.WriteBatch(
		s.ctx,
		s.genWriteRequest([]entities.EventAttributes{
			{
				Loc:    s.map2attrs(attr2map(attrs, addAttrs1)),
				Type:   "debit",
				Dt:     onDt,
				Amount: "123.456",
			},
			{
				Loc:    s.map2attrs(attr2map(attrs, addAttrs2)),
				Type:   "credit",
				Dt:     onDt,
				Amount: "654.321",
			},
		}))
	s.Require().NoError(err)

	accountID := s.getAccount("accounts_test", "account_type", attrs)
	events := s.getEvents(accountID)
	subaccounts := s.getSubaccounts(accountID)
	s.Assert().ElementsMatch(events, []eventInfo{
		{
			EventBatchID:  batchRes.BatchID,
			Type:          "debit",
			AddAttributes: addAttrs1,
			Amount:        "123.456000",
			Dt:            onDt.Truncate(time.Nanosecond * 1000),
			SeqID:         events[0].SeqID,
		},
		{
			EventBatchID:  batchRes.BatchID,
			Type:          "credit",
			AddAttributes: addAttrs2,
			Amount:        "654.321000",
			Dt:            onDt.Truncate(time.Nanosecond * 1000),
			SeqID:         events[0].SeqID,
		},
	})
	s.Assert().ElementsMatch(subaccounts, []subaccountInfo{
		{},
		{addAttrs1[0]},
		{"", addAttrs1[1]},
		{addAttrs2[0]},
		{"", addAttrs2[1]},
		{addAttrs1[0], addAttrs1[1]},
		{addAttrs2[0], addAttrs2[1]},
	})
}

func (s *AccountsAddEventsTestSuite) TestInvalidSettings() {
	attrs := []string{
		btesting.RandS(100),
		btesting.RandS(100),
		btesting.RandS(100),
	}
	attrsMap := map[string]*string{
		"a1": &attrs[0],
		"a2": &attrs[1],
		"a3": &attrs[2],
	}

	_, err := s.ctx.Actions.WriteBatch(
		s.ctx,
		s.genWriteRequestInfo([]entities.EventAttributes{{
			Loc:    s.map2attrs(attrsMap),
			Type:   "debit",
			Dt:     time.Now(),
			Amount: "666.666",
		}}, nil))
	s.Assert().EqualError(err, "failed to get shard: failed to validate attributes: no additional attribute a4")
}

func (s *AccountsAddEventsTestSuite) TestInvalidEventTime() {
	attrsMap := genAttrMap()

	_, err := s.ctx.Actions.WriteBatch(
		s.ctx,
		s.genWriteRequestInfo([]entities.EventAttributes{{
			Loc:    s.map2attrs(attrsMap),
			Type:   "debit",
			Dt:     time.Now().Add(365 * 24 * time.Hour),
			Amount: "666.666",
		}}, nil))
	s.Assert().EqualError(err, "transaction failed: error during evaluation of transaction callback: couldn't insert events: event is too old or in far future")

	_, err = s.ctx.Actions.WriteBatch(
		s.ctx,
		s.genWriteRequestInfo([]entities.EventAttributes{{
			Loc:    s.map2attrs(attrsMap),
			Type:   "debit",
			Dt:     time.Now().Add(-365 * 24 * time.Hour),
			Amount: "666.666",
		}}, nil))
	s.Assert().EqualError(err, "transaction failed: error during evaluation of transaction callback: couldn't insert events: event is too old or in far future")
}

func (s *AccountsAddEventsTestSuite) TestMultipleAccountsDifferentSubaccountsStruct() {
	cfg := `
manifests:
- namespace: accounts_test
  accounts:
    t1:
      attributes:
        - a1
      add_attributes:
        - a2
        - a3
      shard:
        prefix: p
        attributes:
          - a1
      sub_accounts:
        - []
        - - a2
      rollup_period:
        - "0 * * *"
    t2:
      attributes:
        - a1
      add_attributes:
        - a2
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
`
	ctx := s.ctxWithCfg(cfg)

	commonAttr := btesting.RandSP(100)
	attrsMap1 := map[string]*string{
		"a1": commonAttr,
		"a2": btesting.RandSP(100),
		"a3": btesting.RandSP(100),
	}
	attrsMap2 := map[string]*string{
		"a1": commonAttr,
		"a2": btesting.RandSP(100),
		"a3": btesting.RandSP(100),
	}
	attrsMap3 := map[string]*string{
		"a1": commonAttr,
		"a2": btesting.RandSP(100),
		"a3": btesting.RandSP(100),
	}

	onDt := time.Now().UTC()

	batchRes, err := ctx.Actions.WriteBatch(
		ctx,
		s.genWriteRequest([]entities.EventAttributes{
			{
				Loc: entities.LocationAttributes{
					Namespace:  "accounts_test",
					Type:       "t1",
					Attributes: attrsMap1,
				},
				Type:   "debit",
				Dt:     onDt,
				Amount: "1",
			},
			{
				Loc: entities.LocationAttributes{
					Namespace:  "accounts_test",
					Type:       "t2",
					Attributes: attrsMap2,
				},
				Type:   "credit",
				Dt:     onDt,
				Amount: "2",
			},
			{
				Loc: entities.LocationAttributes{
					Namespace:  "accounts_test",
					Type:       "t2",
					Attributes: attrsMap3,
				},
				Type:   "debit",
				Dt:     onDt,
				Amount: "666.666666",
			},
		}))
	s.Require().NoError(err)

	accountID1 := s.getAccount("accounts_test", "t1", []string{*commonAttr})
	events1 := s.getEvents(accountID1)
	subaccounts1 := s.getSubaccounts(accountID1)
	s.Assert().ElementsMatch(events1, []eventInfo{
		{
			EventBatchID:  batchRes.BatchID,
			Type:          "debit",
			AddAttributes: [5]string{*attrsMap1["a2"], *attrsMap1["a3"]},
			Amount:        "1.000000",
			Dt:            onDt.Truncate(time.Nanosecond * 1000),
			SeqID:         events1[0].SeqID,
		},
	})
	s.Assert().ElementsMatch(subaccounts1, []subaccountInfo{
		{},
		{*attrsMap1["a2"]},
	})

	accountID2 := s.getAccount("accounts_test", "t2", []string{*commonAttr})
	events2 := s.getEvents(accountID2)
	subaccounts2 := s.getSubaccounts(accountID2)
	s.Assert().ElementsMatch(events2, []eventInfo{
		{
			EventBatchID:  batchRes.BatchID,
			Type:          "credit",
			AddAttributes: [5]string{*attrsMap2["a2"], *attrsMap2["a3"]},
			Amount:        "2.000000",
			Dt:            onDt.Truncate(time.Nanosecond * 1000),
			SeqID:         events2[0].SeqID,
		},
		{
			EventBatchID:  batchRes.BatchID,
			Type:          "debit",
			AddAttributes: [5]string{*attrsMap3["a2"], *attrsMap3["a3"]},
			Amount:        "666.666666",
			Dt:            onDt.Truncate(time.Nanosecond * 1000),
			SeqID:         events2[1].SeqID,
		},
	})
	s.Assert().ElementsMatch(subaccounts2, []subaccountInfo{
		{},
		{"", *attrsMap2["a3"]},
		{"", *attrsMap3["a3"]},
	})
}

type AccountsBalanceTestSuite struct {
	CommonAccountsTestSuite
}

func (s *AccountsBalanceTestSuite) TestAccount() {
	emptyAttr := ""
	attrs := []string{
		btesting.RandS(100),
		btesting.RandS(100),
	}
	addAttrs := [][entities.AttributesCount]string{
		{btesting.RandS(100), btesting.RandS(100)},
		{btesting.RandS(100), btesting.RandS(100)},
		{btesting.RandS(100), btesting.RandS(100)},
	}
	attrsMaps := []map[string]*string{
		attr2map(attrs, addAttrs[0]),
		attr2map(attrs, addAttrs[1]),
		attr2map(attrs, addAttrs[2]),
	}

	events := []entities.EventAttributes{
		{
			Loc:    s.map2attrs(attrsMaps[0]),
			Type:   "debit",
			Dt:     time.Now(),
			Amount: "1",
		},
		{
			Loc:    s.map2attrs(attrsMaps[0]),
			Type:   "credit",
			Dt:     time.Now(),
			Amount: "2",
		},
		{
			Loc:    s.map2attrs(attrsMaps[0]),
			Type:   "debit",
			Dt:     time.Now(),
			Amount: "3",
		},
		{
			Loc:    s.map2attrs(attrsMaps[1]),
			Type:   "credit",
			Dt:     time.Now(),
			Amount: "4",
		},
		{
			Loc:    s.map2attrs(attrsMaps[2]),
			Type:   "debit",
			Dt:     time.Now(),
			Amount: "5",
		},
	}

	_, err := s.ctx.Actions.WriteBatch(s.ctx, s.genWriteRequest(events))
	s.Require().NoError(err)

	queryAttrs := s.map2attrs(map[string]*string{
		"a1": &attrs[0],
		"a2": &attrs[1],
		"a3": &addAttrs[0][4],
		"a4": &addAttrs[0][4],
	})
	balances, err := s.ctx.Actions.GetBalance(s.ctx, time.Now(), queryAttrs)
	s.Require().NoError(err)
	s.Assert().Len(balances, 1)
	s.Assert().Equal(balances[0].Debit, "9.000000")
	s.Assert().Equal(balances[0].Credit, "6.000000")
	s.Assert().Equal(balances[0].Loc.Type, "account_type")
	s.Assert().Equal(balances[0].Loc.Namespace, "accounts_test")
	s.Assert().Equal(balances[0].Loc.Attributes, map[string]*string{
		"a1": &attrs[0],
		"a2": &attrs[1],
		"a3": &emptyAttr,
		"a4": &emptyAttr,
	})
}

func (s *AccountsBalanceTestSuite) TestAccountWithoutEvents() {
	attrs := []string{
		btesting.RandS(100),
		btesting.RandS(100),
	}
	addAttrs := []string{
		btesting.RandS(100),
		btesting.RandS(100),
	}

	accountID := s.createAccount(attrs)
	s.createSubAccount(accountID, addAttrs)

	queryAttrs := s.map2attrs(map[string]*string{
		"a1": &attrs[0],
		"a2": &attrs[1],
		"a3": &addAttrs[0],
		"a4": &addAttrs[1],
	})
	balances, err := s.ctx.Actions.GetBalance(s.ctx, time.Now(), queryAttrs)
	s.Require().NoError(err)
	s.Assert().Len(balances, 1)
	s.Assert().Equal("0", balances[0].Debit)
	s.Assert().Equal("0", balances[0].Credit)
}

func (s *AccountsBalanceTestSuite) subaccountPairsCase() (attrs []string, addAttrs []string) {
	attrs = []string{
		btesting.RandS(100),
		btesting.RandS(100),
	}
	addAttrs = []string{
		btesting.RandS(100),
		btesting.RandS(100),
		btesting.RandS(100),
		btesting.RandS(100),
	}
	addAttrPairs := [][entities.AttributesCount]string{
		{addAttrs[0], addAttrs[1]},
		{addAttrs[0], addAttrs[2]},
		{addAttrs[3], addAttrs[1]},
	}
	attrsMaps := []map[string]*string{
		attr2map(attrs, addAttrPairs[0]),
		attr2map(attrs, addAttrPairs[1]),
		attr2map(attrs, addAttrPairs[2]),
	}

	events := []entities.EventAttributes{
		{
			Loc:    s.map2attrs(attrsMaps[0]),
			Type:   "debit",
			Dt:     time.Now(),
			Amount: "1",
		},
		{
			Loc:    s.map2attrs(attrsMaps[0]),
			Type:   "debit",
			Dt:     time.Now(),
			Amount: "2",
		},
		{
			Loc:    s.map2attrs(attrsMaps[0]),
			Type:   "credit",
			Dt:     time.Now(),
			Amount: "4",
		},
		{
			Loc:    s.map2attrs(attrsMaps[1]),
			Type:   "debit",
			Dt:     time.Now(),
			Amount: "8",
		},
		{
			Loc:    s.map2attrs(attrsMaps[1]),
			Type:   "credit",
			Dt:     time.Now(),
			Amount: "16",
		},
		{
			Loc:    s.map2attrs(attrsMaps[1]),
			Type:   "credit",
			Dt:     time.Now(),
			Amount: "32",
		},
		{
			Loc:    s.map2attrs(attrsMaps[2]),
			Type:   "debit",
			Dt:     time.Now(),
			Amount: "64",
		},
		{
			Loc:    s.map2attrs(attrsMaps[2]),
			Type:   "debit",
			Dt:     time.Now(),
			Amount: "128",
		},
	}

	_, err := s.ctx.Actions.WriteBatch(s.ctx, s.genWriteRequest(events))
	s.Require().NoError(err)

	return attrs, addAttrs
}

func (s *AccountsBalanceTestSuite) TestSubaccountFirstAttr() {
	emptyAttr := ""
	attrs, addAttrs := s.subaccountPairsCase()

	queryAttrs := s.map2attrs(map[string]*string{
		"a1": &attrs[0],
		"a2": &attrs[1],
		"a3": &addAttrs[0],
		"a4": &emptyAttr,
	})
	balances, err := s.ctx.Actions.GetBalance(s.ctx, time.Now(), queryAttrs)
	s.Require().NoError(err)
	s.Assert().Len(balances, 1)
	s.Assert().Equal(balances[0].Debit, "11.000000")
	s.Assert().Equal(balances[0].Credit, "52.000000")
	s.Assert().Equal(balances[0].Loc.Type, "account_type")
	s.Assert().Equal(balances[0].Loc.Namespace, "accounts_test")
	s.Assert().Equal(balances[0].Loc.Attributes, map[string]*string{
		"a1": &attrs[0],
		"a2": &attrs[1],
		"a3": &addAttrs[0],
		"a4": &emptyAttr,
	})
}

func (s *AccountsBalanceTestSuite) TestSubaccountSecondAttr() {
	emptyAttr := ""
	attrs, addAttrs := s.subaccountPairsCase()

	queryAttrs := s.map2attrs(map[string]*string{
		"a1": &attrs[0],
		"a2": &attrs[1],
		"a3": &emptyAttr,
		"a4": &addAttrs[1],
	})
	balances, err := s.ctx.Actions.GetBalance(s.ctx, time.Now(), queryAttrs)
	s.Require().NoError(err)
	s.Assert().Len(balances, 1)
	s.Assert().Equal(balances[0].Debit, "195.000000")
	s.Assert().Equal(balances[0].Credit, "4.000000")
	s.Assert().Equal(balances[0].Loc.Type, "account_type")
	s.Assert().Equal(balances[0].Loc.Namespace, "accounts_test")
	s.Assert().Equal(balances[0].Loc.Attributes, map[string]*string{
		"a1": &attrs[0],
		"a2": &attrs[1],
		"a3": &emptyAttr,
		"a4": &addAttrs[1],
	})
}

func (s *AccountsBalanceTestSuite) TestSubaccountBothAttrs() {
	attrs, addAttrs := s.subaccountPairsCase()

	queryAttrs := s.map2attrs(map[string]*string{
		"a1": &attrs[0],
		"a2": &attrs[1],
		"a3": &addAttrs[0],
		"a4": &addAttrs[1],
	})
	balances, err := s.ctx.Actions.GetBalance(s.ctx, time.Now(), queryAttrs)
	s.Require().NoError(err)
	s.Assert().Len(balances, 1)
	s.Assert().Equal(balances[0].Debit, "3.000000")
	s.Assert().Equal(balances[0].Credit, "4.000000")
	s.Assert().Equal(balances[0].Loc.Type, "account_type")
	s.Assert().Equal(balances[0].Loc.Namespace, "accounts_test")
	s.Assert().Equal(balances[0].Loc.Attributes, map[string]*string{
		"a1": &attrs[0],
		"a2": &attrs[1],
		"a3": &addAttrs[0],
		"a4": &addAttrs[1],
	})
}

func (s *AccountsBalanceTestSuite) TestSubaccountNonexistent() {
	emptyAttr := ""
	wrongAttr := btesting.RandS(100)
	attrs, _ := s.subaccountPairsCase()

	queryAttrs := s.map2attrs(map[string]*string{
		"a1": &attrs[0],
		"a2": &attrs[1],
		"a3": &wrongAttr,
		"a4": &emptyAttr,
	})
	balances, err := s.ctx.Actions.GetBalance(s.ctx, time.Now(), queryAttrs)
	s.Require().NoError(err)
	s.Assert().Len(balances, 0)
}

func (s *AccountsBalanceTestSuite) TestSubaccountMaskFirst() {
	emptyAttr := ""
	attrs, addAttrs := s.subaccountPairsCase()

	queryAttrs := s.map2attrs(map[string]*string{
		"a1": &attrs[0],
		"a2": &attrs[1],
		"a3": nil,
		"a4": &emptyAttr,
	})
	balances, err := s.ctx.Actions.GetBalance(s.ctx, time.Now(), queryAttrs)
	s.Require().NoError(err)
	s.Assert().Len(balances, 2)
	s.Assert().ElementsMatch(
		[]string{balances[0].Debit, balances[1].Debit},
		[]string{"192.000000", "11.000000"},
	)
	s.Assert().ElementsMatch(
		[]string{balances[0].Credit, balances[1].Credit},
		[]string{"0", "52.000000"},
	)
	s.Assert().ElementsMatch(
		[]string{balances[0].Loc.Type, balances[1].Loc.Type},
		[]string{"account_type", "account_type"},
	)
	s.Assert().ElementsMatch(
		[]string{balances[0].Loc.Namespace, balances[1].Loc.Namespace},
		[]string{"accounts_test", "accounts_test"},
	)
	s.Assert().ElementsMatch(
		[]map[string]*string{
			balances[0].Loc.Attributes,
			balances[1].Loc.Attributes,
		},
		[]map[string]*string{
			{
				"a1": &attrs[0],
				"a2": &attrs[1],
				"a3": &addAttrs[0],
				"a4": &emptyAttr,
			},
			{
				"a1": &attrs[0],
				"a2": &attrs[1],
				"a3": &addAttrs[3],
				"a4": &emptyAttr,
			},
		},
	)
}

func (s *AccountsBalanceTestSuite) TestSubaccountMaskSecond() {
	emptyAttr := ""
	attrs, addAttrs := s.subaccountPairsCase()

	queryAttrs := s.map2attrs(map[string]*string{
		"a1": &attrs[0],
		"a2": &attrs[1],
		"a3": &emptyAttr,
		"a4": nil,
	})
	balances, err := s.ctx.Actions.GetBalance(s.ctx, time.Now(), queryAttrs)
	s.Require().NoError(err)
	s.Assert().Len(balances, 2)
	s.Assert().ElementsMatch(
		[]string{balances[0].Debit, balances[1].Debit},
		[]string{"195.000000", "8.000000"},
	)
	s.Assert().ElementsMatch(
		[]string{balances[0].Credit, balances[1].Credit},
		[]string{"4.000000", "48.000000"},
	)
	s.Assert().ElementsMatch(
		[]string{balances[0].Loc.Type, balances[1].Loc.Type},
		[]string{"account_type", "account_type"},
	)
	s.Assert().ElementsMatch(
		[]string{balances[0].Loc.Namespace, balances[1].Loc.Namespace},
		[]string{"accounts_test", "accounts_test"},
	)
	s.Assert().ElementsMatch(
		[]map[string]*string{
			balances[0].Loc.Attributes,
			balances[1].Loc.Attributes,
		},
		[]map[string]*string{
			{
				"a1": &attrs[0],
				"a2": &attrs[1],
				"a3": &emptyAttr,
				"a4": &addAttrs[1],
			},
			{
				"a1": &attrs[0],
				"a2": &attrs[1],
				"a3": &emptyAttr,
				"a4": &addAttrs[2],
			},
		},
	)
}

func (s *AccountsBalanceTestSuite) TestSubaccountMaskPairSecond() {
	attrs, addAttrs := s.subaccountPairsCase()

	queryAttrs := s.map2attrs(map[string]*string{
		"a1": &attrs[0],
		"a2": &attrs[1],
		"a3": &addAttrs[0],
		"a4": nil,
	})
	balances, err := s.ctx.Actions.GetBalance(s.ctx, time.Now(), queryAttrs)
	s.Require().NoError(err)
	s.Assert().Len(balances, 2)
	s.Assert().ElementsMatch(
		[]string{balances[0].Debit, balances[1].Debit},
		[]string{"3.000000", "8.000000"},
	)
	s.Assert().ElementsMatch(
		[]string{balances[0].Credit, balances[1].Credit},
		[]string{"4.000000", "48.000000"},
	)
	s.Assert().ElementsMatch(
		[]string{balances[0].Loc.Type, balances[1].Loc.Type},
		[]string{"account_type", "account_type"},
	)
	s.Assert().ElementsMatch(
		[]string{balances[0].Loc.Namespace, balances[1].Loc.Namespace},
		[]string{"accounts_test", "accounts_test"},
	)
	s.Assert().ElementsMatch(
		[]map[string]*string{
			balances[0].Loc.Attributes,
			balances[1].Loc.Attributes,
		},
		[]map[string]*string{
			{
				"a1": &attrs[0],
				"a2": &attrs[1],
				"a3": &addAttrs[0],
				"a4": &addAttrs[1],
			},
			{
				"a1": &attrs[0],
				"a2": &attrs[1],
				"a3": &addAttrs[0],
				"a4": &addAttrs[2],
			},
		},
	)
}

func (s *AccountsBalanceTestSuite) TestSubaccountMaskBoth() {
	attrs, addAttrs := s.subaccountPairsCase()

	queryAttrs := s.map2attrs(map[string]*string{
		"a1": &attrs[0],
		"a2": &attrs[1],
		"a3": nil,
		"a4": nil,
	})
	balances, err := s.ctx.Actions.GetBalance(s.ctx, time.Now(), queryAttrs)
	s.Require().NoError(err)
	s.Assert().Len(balances, 3)
	s.Assert().ElementsMatch(
		[]string{balances[0].Debit, balances[1].Debit, balances[2].Debit},
		[]string{"3.000000", "8.000000", "192.000000"},
	)
	s.Assert().ElementsMatch(
		[]string{balances[0].Credit, balances[1].Credit, balances[2].Credit},
		[]string{"4.000000", "48.000000", "0"},
	)
	s.Assert().ElementsMatch(
		[]string{balances[0].Loc.Type, balances[1].Loc.Type, balances[2].Loc.Type},
		[]string{"account_type", "account_type", "account_type"},
	)
	s.Assert().ElementsMatch(
		[]string{balances[0].Loc.Namespace, balances[1].Loc.Namespace, balances[2].Loc.Namespace},
		[]string{"accounts_test", "accounts_test", "accounts_test"},
	)
	s.Assert().ElementsMatch(
		[]map[string]*string{
			balances[0].Loc.Attributes,
			balances[1].Loc.Attributes,
			balances[2].Loc.Attributes,
		},
		[]map[string]*string{
			{
				"a1": &attrs[0],
				"a2": &attrs[1],
				"a3": &addAttrs[0],
				"a4": &addAttrs[1],
			},
			{
				"a1": &attrs[0],
				"a2": &attrs[1],
				"a3": &addAttrs[0],
				"a4": &addAttrs[2],
			},
			{
				"a1": &attrs[0],
				"a2": &attrs[1],
				"a3": &addAttrs[3],
				"a4": &addAttrs[1],
			},
		},
	)
}

func (s *AccountsBalanceTestSuite) TestAccountMask() {
	attr1 := btesting.RandS(100)
	attr21 := btesting.RandS(100)
	attr22 := btesting.RandS(100)
	addAttrs := [entities.AttributesCount]string{
		btesting.RandS(100),
		btesting.RandS(100),
	}
	attrsMaps := []map[string]*string{
		attr2map([]string{attr1, attr21}, addAttrs),
		attr2map([]string{attr1, attr22}, addAttrs),
	}

	events := []entities.EventAttributes{
		{
			Loc:    s.map2attrs(attrsMaps[0]),
			Type:   "debit",
			Dt:     time.Now(),
			Amount: "1",
		},
		{
			Loc:    s.map2attrs(attrsMaps[1]),
			Type:   "credit",
			Dt:     time.Now(),
			Amount: "2",
		},
	}

	_, err := s.ctx.Actions.WriteBatch(s.ctx, s.genWriteRequest(events))
	s.Require().NoError(err)

	queryAttrs := s.map2attrs(map[string]*string{
		"a1": &attr1,
		"a2": nil,
		"a3": nil,
		"a4": nil,
	})
	balances, err := s.ctx.Actions.GetBalance(s.ctx, time.Now(), queryAttrs)
	s.Require().NoError(err)
	s.Assert().Len(balances, 2)
	s.Assert().ElementsMatch(
		[]string{balances[0].Debit, balances[1].Debit},
		[]string{"1.000000", "0"},
	)
	s.Assert().ElementsMatch(
		[]string{balances[0].Credit, balances[1].Credit},
		[]string{"0", "2.000000"},
	)
	s.Assert().ElementsMatch(
		[]string{balances[0].Loc.Type, balances[1].Loc.Type},
		[]string{"account_type", "account_type"},
	)
	s.Assert().ElementsMatch(
		[]string{balances[0].Loc.Namespace, balances[1].Loc.Namespace},
		[]string{"accounts_test", "accounts_test"},
	)
	s.Assert().ElementsMatch(
		[]map[string]*string{
			balances[0].Loc.Attributes,
			balances[1].Loc.Attributes,
		},
		[]map[string]*string{
			{
				"a1": &attr1,
				"a2": &attr21,
				"a3": &addAttrs[0],
				"a4": &addAttrs[1],
			},
			{
				"a1": &attr1,
				"a2": &attr22,
				"a3": &addAttrs[0],
				"a4": &addAttrs[1],
			},
		},
	)
}

func (s *AccountsBalanceTestSuite) TestRolledUpMixed() {
	attrs := []string{
		btesting.RandS(100),
		btesting.RandS(100),
	}
	addAttrs := [5]string{
		btesting.RandS(100),
		btesting.RandS(100),
	}
	attrsMap := attr2map(attrs, addAttrs)

	accountID := s.createAccount(attrs)
	subaccountID := s.createSubAccount(accountID, addAttrs[:2])

	onDT := time.Now().UTC()

	_, err := s.ctx.Actions.WriteBatch(
		s.ctx,
		s.genWriteRequest([]entities.EventAttributes{
			{
				Loc:    s.map2attrs(attrsMap),
				Type:   "debit",
				Dt:     onDT.Add(-time.Minute),
				Amount: "1.23",
			},
			{
				Loc:    s.map2attrs(attrsMap),
				Type:   "credit",
				Dt:     onDT,
				Amount: "4.56",
			},
			{
				Loc:    s.map2attrs(attrsMap),
				Type:   "credit",
				Dt:     onDT.Add(time.Minute),
				Amount: "7.89",
			},
			{
				Loc:    s.map2attrs(attrsMap),
				Type:   "credit",
				Dt:     onDT.Add(time.Minute * 2),
				Amount: "100",
			},
		}))
	s.Require().NoError(err)

	s.updateEventSeqID(4)

	s.rollupAccount(accountID, subaccountID, onDT, "666", "666")

	_, err = s.ctx.Actions.WriteBatch(
		s.ctx,
		s.genWriteRequest([]entities.EventAttributes{
			{
				Loc:    s.map2attrs(attrsMap),
				Type:   "debit",
				Dt:     onDT.Add(-time.Minute),
				Amount: "3.21",
			},
			{
				Loc:    s.map2attrs(attrsMap),
				Type:   "credit",
				Dt:     onDT,
				Amount: "6.54",
			},
			{
				Loc:    s.map2attrs(attrsMap),
				Type:   "credit",
				Dt:     onDT.Add(time.Minute),
				Amount: "9.87",
			},
			{
				Loc:    s.map2attrs(attrsMap),
				Type:   "debit",
				Dt:     onDT.Add(time.Minute * 2),
				Amount: "100",
			},
		}))
	s.Require().NoError(err)

	s.updateEventSeqID(4)

	queryAttrs := s.map2attrs(map[string]*string{
		"a1": &attrs[0],
		"a2": &attrs[1],
		"a3": &addAttrs[0],
		"a4": &addAttrs[1],
	})
	balances, err := s.ctx.Actions.GetBalance(s.ctx, onDT.Add(time.Minute), queryAttrs)
	s.Require().NoError(err)
	s.Assert().Len(balances, 1)
	s.Assert().Equal(balances[0].Debit, "669.210000")
	s.Assert().Equal(balances[0].Credit, "677.100000")
}

func (s *AccountsBalanceTestSuite) TestRolledUpBeforeDT() {
	attrs := []string{
		btesting.RandS(100),
		btesting.RandS(100),
	}
	addAttrs := [5]string{
		btesting.RandS(100),
		btesting.RandS(100),
	}
	attrsMap := attr2map(attrs, addAttrs)

	accountID := s.createAccount(attrs)
	subaccountID := s.createSubAccount(accountID, addAttrs[:])

	onDT := time.Now().UTC()

	_, err := s.ctx.Actions.WriteBatch(
		s.ctx,
		s.genWriteRequest([]entities.EventAttributes{
			{
				Loc:    s.map2attrs(attrsMap),
				Type:   "debit",
				Dt:     onDT.Add(-time.Minute),
				Amount: "1.23",
			},
			{
				Loc:    s.map2attrs(attrsMap),
				Type:   "credit",
				Dt:     onDT,
				Amount: "4.56",
			},
		}))
	s.Require().NoError(err)

	s.rollupAccount(accountID, subaccountID, onDT, "666", "666")

	queryAttrs := s.map2attrs(map[string]*string{
		"a1": &attrs[0],
		"a2": &attrs[1],
		"a3": &addAttrs[0],
		"a4": &addAttrs[1],
	})
	balances, err := s.ctx.Actions.GetBalance(s.ctx, onDT.Add(-2*time.Minute), queryAttrs)
	s.Require().NoError(err)
	s.Assert().Len(balances, 1)
	s.Assert().Equal(balances[0].Debit, "0")
	s.Assert().Equal(balances[0].Credit, "0")
}

func (s *AccountsBalanceTestSuite) TestRolledUpOnRollupDT() {
	attrs := []string{
		btesting.RandS(100),
		btesting.RandS(100),
	}
	addAttrs := [5]string{
		btesting.RandS(100),
		btesting.RandS(100),
	}
	attrsMap := attr2map(attrs, addAttrs)

	accountID := s.createAccount(attrs)
	subaccountID := s.createSubAccount(accountID, addAttrs[:])

	onDT := time.Now().UTC()

	_, err := s.ctx.Actions.WriteBatch(
		s.ctx,
		s.genWriteRequest([]entities.EventAttributes{
			{
				Loc:    s.map2attrs(attrsMap),
				Type:   "debit",
				Dt:     onDT.Add(-time.Minute),
				Amount: "1.23",
			},
			{
				Loc:    s.map2attrs(attrsMap),
				Type:   "credit",
				Dt:     onDT,
				Amount: "4.56",
			},
		}))
	s.Require().NoError(err)

	s.rollupAccount(accountID, subaccountID, onDT, "666", "666")

	queryAttrs := s.map2attrs(map[string]*string{
		"a1": &attrs[0],
		"a2": &attrs[1],
		"a3": &addAttrs[0],
		"a4": &addAttrs[1],
	})
	balances, err := s.ctx.Actions.GetBalance(s.ctx, onDT, queryAttrs)
	s.Require().NoError(err)
	s.Assert().Len(balances, 1)
	s.Assert().Equal(balances[0].Debit, "666.000000")
	s.Assert().Equal(balances[0].Credit, "666.000000")
}

func (s *AccountsBalanceTestSuite) TestRolledUpOnEventDT() {
	attrs := []string{
		btesting.RandS(100),
		btesting.RandS(100),
	}
	addAttrs := [5]string{
		btesting.RandS(100),
		btesting.RandS(100),
	}
	attrsMap := attr2map(attrs, addAttrs)

	accountID := s.createAccount(attrs)
	subaccountID := s.createSubAccount(accountID, addAttrs[:])

	onDT := time.Now().UTC()

	_, err := s.ctx.Actions.WriteBatch(
		s.ctx,
		s.genWriteRequest([]entities.EventAttributes{
			{
				Loc:    s.map2attrs(attrsMap),
				Type:   "debit",
				Dt:     onDT,
				Amount: "1.23",
			},
			{
				Loc:    s.map2attrs(attrsMap),
				Type:   "credit",
				Dt:     onDT.Add(time.Minute),
				Amount: "4.56",
			},
			{
				Loc:    s.map2attrs(attrsMap),
				Type:   "credit",
				Dt:     onDT.Add(2 * time.Minute),
				Amount: "7.89",
			},
		}))
	s.Require().NoError(err)

	s.rollupAccount(accountID, subaccountID, onDT, "666", "666")

	queryAttrs := s.map2attrs(map[string]*string{
		"a1": &attrs[0],
		"a2": &attrs[1],
		"a3": &addAttrs[0],
		"a4": &addAttrs[1],
	})
	balances, err := s.ctx.Actions.GetBalance(s.ctx, onDT.Add(2*time.Minute), queryAttrs)
	s.Require().NoError(err)
	s.Assert().Len(balances, 1)
	s.Assert().Equal(balances[0].Debit, "667.230000")
	s.Assert().Equal(balances[0].Credit, "670.560000")
}

// TestRolledUpAfterEventDt tests following case:
// event dt < rollup dt < get balance dt and event isn't rolled up.
// Also check 2 cases when seq_id is set or not.
func (s *AccountsBalanceTestSuite) TestRolledUpAfterEventDt() {
	attrs := []string{
		btesting.RandS(100),
		btesting.RandS(100),
	}
	addAttrs := [5]string{
		btesting.RandS(100),
		btesting.RandS(100),
	}
	attrsMap := attr2map(attrs, addAttrs)

	accountID := s.createAccount(attrs)
	subaccountID := s.createSubAccount(accountID, addAttrs[:])

	onDT := time.Now().UTC()

	s.rollupAccount(accountID, subaccountID, onDT.Add(time.Minute), "666", "666")

	// Add event with seq_id.
	_, err := s.ctx.Actions.WriteBatch(
		s.ctx,
		s.genWriteRequest([]entities.EventAttributes{
			{
				Loc:    s.map2attrs(attrsMap),
				Type:   "credit",
				Dt:     onDT,
				Amount: "4.56",
			},
		}))
	s.Require().NoError(err)

	s.updateEventSeqID(1)

	// Add event without seq_id.
	_, err = s.ctx.Actions.WriteBatch(
		s.ctx,
		s.genWriteRequest([]entities.EventAttributes{
			{
				Loc:    s.map2attrs(attrsMap),
				Type:   "debit",
				Dt:     onDT,
				Amount: "1.23",
			},
		}))
	s.Require().NoError(err)

	queryAttrs := s.map2attrs(map[string]*string{
		"a1": &attrs[0],
		"a2": &attrs[1],
		"a3": &addAttrs[0],
		"a4": &addAttrs[1],
	})
	balances, err := s.ctx.Actions.GetBalance(s.ctx, onDT.Add(2*time.Minute), queryAttrs)
	s.Require().NoError(err)
	s.Assert().Len(balances, 1)
	s.Assert().Equal(balances[0].Debit, "667.230000")
	s.Assert().Equal(balances[0].Credit, "670.560000")
}

func (s *AccountsBalanceTestSuite) TestOldRolledUpOnDT() {
	attrs := []string{
		btesting.RandS(100),
		btesting.RandS(100),
	}
	addAttrs := [5]string{
		btesting.RandS(100),
		btesting.RandS(100),
	}
	attrsMap := attr2map(attrs, addAttrs)

	accountID := s.createAccount(attrs)
	subaccountID := s.createSubAccount(accountID, addAttrs[:])

	onDT := time.Now().UTC()

	s.rollupAccount(accountID, subaccountID, onDT, "666", "666")

	_, err := s.ctx.Actions.WriteBatch(
		s.ctx,
		s.genWriteRequest([]entities.EventAttributes{
			{
				Loc:    s.map2attrs(attrsMap),
				Type:   "debit",
				Dt:     onDT.Add(-time.Minute),
				Amount: "1.23",
			},
			{
				Loc:    s.map2attrs(attrsMap),
				Type:   "credit",
				Dt:     onDT,
				Amount: "4.56",
			},
			{
				Loc:    s.map2attrs(attrsMap),
				Type:   "credit",
				Dt:     onDT.Add(time.Minute),
				Amount: "7.89",
			},
		}))
	s.Require().NoError(err)

	s.updateEventSeqID(3)

	queryAttrs := s.map2attrs(map[string]*string{
		"a1": &attrs[0],
		"a2": &attrs[1],
		"a3": &addAttrs[0],
		"a4": &addAttrs[1],
	})
	balances, err := s.ctx.Actions.GetBalance(s.ctx, onDT, queryAttrs)
	s.Require().NoError(err)
	s.Assert().Len(balances, 1)
	s.Assert().Equal(balances[0].Debit, "667.230000")
	s.Assert().Equal(balances[0].Credit, "666.000000")
}

func (s *AccountsBalanceTestSuite) TestRolledUpMultipleRollups() {
	attrs := []string{
		btesting.RandS(100),
		btesting.RandS(100),
	}
	addAttrs := [5]string{
		btesting.RandS(100),
		btesting.RandS(100),
	}
	attrsMap := attr2map(attrs, addAttrs)

	accountID := s.createAccount(attrs)
	subaccountID := s.createSubAccount(accountID, addAttrs[:])

	onDT := time.Now().UTC()

	_, err := s.ctx.Actions.WriteBatch(
		s.ctx,
		s.genWriteRequest([]entities.EventAttributes{
			{
				Loc:    s.map2attrs(attrsMap),
				Type:   "debit",
				Dt:     onDT.Add(10 * time.Minute),
				Amount: "1.23",
			},
			{
				Loc:    s.map2attrs(attrsMap),
				Type:   "credit",
				Dt:     onDT.Add(20 * time.Minute),
				Amount: "4.56",
			},
			{
				Loc:    s.map2attrs(attrsMap),
				Type:   "credit",
				Dt:     onDT.Add(30 * time.Minute),
				Amount: "7.89",
			},
			{
				Loc:    s.map2attrs(attrsMap),
				Type:   "credit",
				Dt:     onDT.Add(40 * time.Minute),
				Amount: "10.1112",
			},
		}))
	s.Require().NoError(err)

	s.rollupAccount(accountID, subaccountID, onDT.Add(15*time.Minute), "6", "6")
	s.rollupAccount(accountID, subaccountID, onDT.Add(25*time.Minute), "7", "7")
	s.rollupAccount(accountID, subaccountID, onDT.Add(35*time.Minute), "8", "8")
	s.rollupAccount(accountID, subaccountID, onDT.Add(45*time.Minute), "9", "9")

	queryAttrs := s.map2attrs(map[string]*string{
		"a1": &attrs[0],
		"a2": &attrs[1],
		"a3": &addAttrs[0],
		"a4": &addAttrs[1],
	})
	balances, err := s.ctx.Actions.GetBalance(s.ctx, onDT.Add(31*time.Minute), queryAttrs)
	s.Require().NoError(err)
	s.Assert().Len(balances, 1)
	s.Assert().Equal(balances[0].Debit, "7.000000")
	s.Assert().Equal(balances[0].Credit, "14.890000")
}

func (s *AccountsBalanceTestSuite) TestRollupTimeWithMissingRollup() {
	attrs := []string{
		btesting.RandS(100),
		btesting.RandS(100),
	}
	addAttrs := [5]string{
		btesting.RandS(100),
		btesting.RandS(100),
	}
	attrsMap := attr2map(attrs, addAttrs)
	accountNamespace := "rt1"
	accountType := "rt1"

	accountID := s.createAccountWithLocationAttributes(accountNamespace, accountType, attrs)
	subaccountID := s.createSubAccount(accountID, addAttrs[:2])

	baseDT := time.Now().UTC().Truncate(time.Hour * 24).Add(time.Hour * 15).Add(time.Minute * 10)

	// assume rollups very 10 minutes

	// add events in the past
	_, err := s.ctx.Actions.WriteBatch(
		s.ctx,
		s.genWriteRequest([]entities.EventAttributes{
			{
				Loc:    s.map2attrs(attrsMap),
				Type:   "debit",
				Dt:     baseDT.Add(-time.Minute * 5),
				Amount: "1",
			},
			{
				Loc:    s.map2attrs(attrsMap),
				Type:   "credit",
				Dt:     baseDT.Add(-time.Minute * 5),
				Amount: "1",
			},
		}))
	s.Require().NoError(err)

	s.updateEventSeqID(2)

	// first rollup
	s.insertRollupTime(accountNamespace, accountType, baseDT.Add(-time.Minute*5), baseDT)
	s.insertRollupTime(accountNamespace, accountType, baseDT, baseDT.Add(time.Minute*10))
	s.rollupAccount(accountID, subaccountID, baseDT, "1", "1")

	// add events that should be in 2 and 3 rollup
	// secind two should not affect
	_, err = s.ctx.Actions.WriteBatch(
		s.ctx,
		s.genWriteRequest([]entities.EventAttributes{
			{
				Loc:    s.map2attrs(attrsMap),
				Type:   "debit",
				Dt:     baseDT.Add(-time.Minute),
				Amount: "1",
			},
			{
				Loc:    s.map2attrs(attrsMap),
				Type:   "debit",
				Dt:     baseDT.Add(time.Minute),
				Amount: "1",
			},
			{
				Loc:    s.map2attrs(attrsMap),
				Type:   "debit",
				Dt:     baseDT.Add(time.Minute * 11),
				Amount: "1",
			},
		}))
	s.Require().NoError(err)

	s.updateEventSeqID(3)

	_, err = s.ctx.Actions.WriteBatch(
		s.ctx,
		s.genWriteRequest([]entities.EventAttributes{
			{
				Loc:    s.map2attrs(attrsMap),
				Type:   "credit",
				Dt:     baseDT.Add(-time.Minute),
				Amount: "1",
			},
			{
				Loc:    s.map2attrs(attrsMap),
				Type:   "credit",
				Dt:     baseDT.Add(time.Minute),
				Amount: "1",
			},
			{
				Loc:    s.map2attrs(attrsMap),
				Type:   "credit",
				Dt:     baseDT.Add(time.Minute * 11),
				Amount: "1",
			},
		}))
	s.Require().NoError(err)

	queryAttrs := s.map2attrs(map[string]*string{
		"a1": &attrs[0],
		"a2": &attrs[1],
		"a3": &addAttrs[0],
		"a4": &addAttrs[1],
	})

	balances, err := s.ctx.Actions.GetBalance(s.ctx, baseDT.Add(time.Minute*12), queryAttrs)
	s.Require().NoError(err)

	s.Assert().Len(balances, 1)
	s.Assert().Equal("4.000000", balances[0].Debit)
	s.Assert().Equal("4.000000", balances[0].Credit)

	s.updateEventSeqID(3)

	s.insertRollupTime(accountNamespace, accountType, baseDT.Add(time.Minute*10), baseDT.Add(time.Minute*20))
	s.insertRollupTime(accountNamespace, accountType, baseDT.Add(time.Minute*20), baseDT.Add(time.Minute*30))

	s.rollupAccount(accountID, subaccountID, baseDT.Add(time.Minute*10), "3", "3")
	s.rollupAccount(accountID, subaccountID, baseDT.Add(time.Minute*20), "4", "4")

	balances, err = s.ctx.Actions.GetBalance(s.ctx, baseDT.Add(time.Minute*22), queryAttrs)
	s.Require().NoError(err)

	s.Assert().Len(balances, 1)
	s.Assert().Equal("4.000000", balances[0].Debit)
	s.Assert().Equal("4.000000", balances[0].Credit)
}

func (s *AccountsBalanceTestSuite) TestNoRolluAndNoRollupTime() {
	attrs := []string{
		btesting.RandS(100),
		btesting.RandS(100),
	}
	addAttrs := [5]string{
		btesting.RandS(100),
		btesting.RandS(100),
	}
	attrsMap := attr2map(attrs, addAttrs)
	accountNamespace := "rt1"
	accountType := "rt1"

	_ = s.createAccountWithLocationAttributes(accountNamespace, accountType, attrs)

	baseDT := time.Now().UTC().Truncate(time.Hour * 24).Add(time.Hour * 15).Add(time.Minute * 10)

	// assume rollups very 10 minutes

	// add events in the past
	_, err := s.ctx.Actions.WriteBatch(
		s.ctx,
		s.genWriteRequest([]entities.EventAttributes{
			{
				Loc:    s.map2attrs(attrsMap),
				Type:   "debit",
				Dt:     baseDT.Add(-time.Minute * 5),
				Amount: "1",
			},
			{
				Loc:    s.map2attrs(attrsMap),
				Type:   "credit",
				Dt:     baseDT.Add(-time.Minute * 5),
				Amount: "1",
			},
		}))
	s.Require().NoError(err)

	s.updateEventSeqID(2)

	queryAttrs := s.map2attrs(map[string]*string{
		"a1": &attrs[0],
		"a2": &attrs[1],
		"a3": &addAttrs[0],
		"a4": &addAttrs[1],
	})

	balances, err := s.ctx.Actions.GetBalance(s.ctx, baseDT, queryAttrs)
	s.Require().NoError(err)

	s.Assert().Len(balances, 1)
	s.Assert().Equal("1.000000", balances[0].Debit)
	s.Assert().Equal("1.000000", balances[0].Credit)
}

type AccountsTurnoverTestSuite struct {
	CommonAccountsTestSuite
}

func (s *AccountsTurnoverTestSuite) TestBeforeEvents() {
	attrsMap := genAttrMap()
	onDt := time.Now()

	events := []entities.EventAttributes{
		{
			Loc:    s.map2attrs(attrsMap),
			Type:   "debit",
			Dt:     onDt.Add(-time.Minute),
			Amount: "1",
		},
		{
			Loc:    s.map2attrs(attrsMap),
			Type:   "credit",
			Dt:     onDt.Add(time.Minute),
			Amount: "2",
		},
	}

	_, err := s.ctx.Actions.WriteBatch(s.ctx, s.genWriteRequest(events))
	s.Require().NoError(err)

	queryAttrs := s.map2attrs(attrsMap)
	balances, err := s.ctx.Actions.GetTurnover(s.ctx, onDt.Add(-time.Hour), onDt.Add(-2*time.Minute), queryAttrs)
	s.Require().NoError(err)
	s.Assert().Len(balances, 1)
	s.Assert().Equal(balances[0].DebitInit, "0")
	s.Assert().Equal(balances[0].CreditInit, "0")
	s.Assert().Equal(balances[0].DebitTurnover, "0")
	s.Assert().Equal(balances[0].CreditTurnover, "0")
	s.Assert().Equal(balances[0].Loc.Type, "account_type")
	s.Assert().Equal(balances[0].Loc.Namespace, "accounts_test")
	s.Assert().Equal(balances[0].Loc.Attributes, attrsMap)
}

func (s *AccountsTurnoverTestSuite) TestAfterEvents() {
	attrsMap := genAttrMap()
	onDt := time.Now()

	events := []entities.EventAttributes{
		{
			Loc:    s.map2attrs(attrsMap),
			Type:   "debit",
			Dt:     onDt.Add(-time.Minute),
			Amount: "1",
		},
		{
			Loc:    s.map2attrs(attrsMap),
			Type:   "credit",
			Dt:     onDt.Add(time.Minute),
			Amount: "2",
		},
	}

	_, err := s.ctx.Actions.WriteBatch(s.ctx, s.genWriteRequest(events))
	s.Require().NoError(err)

	queryAttrs := s.map2attrs(attrsMap)
	balances, err := s.ctx.Actions.GetTurnover(s.ctx, onDt.Add(time.Hour), onDt.Add(2*time.Hour), queryAttrs)
	s.Require().NoError(err)
	s.Assert().Len(balances, 1)
	s.Assert().Equal(balances[0].DebitInit, "1.000000")
	s.Assert().Equal(balances[0].CreditInit, "2.000000")
	s.Assert().Equal(balances[0].DebitTurnover, "0.000000")
	s.Assert().Equal(balances[0].CreditTurnover, "0.000000")
	s.Assert().Equal(balances[0].Loc.Type, "account_type")
	s.Assert().Equal(balances[0].Loc.Namespace, "accounts_test")
	s.Assert().Equal(balances[0].Loc.Attributes, attrsMap)
}

func (s *AccountsTurnoverTestSuite) TestAllEvents() {
	attrsMap := genAttrMap()
	onDt := time.Now()

	events := []entities.EventAttributes{
		{
			Loc:    s.map2attrs(attrsMap),
			Type:   "debit",
			Dt:     onDt.Add(-time.Minute),
			Amount: "1",
		},
		{
			Loc:    s.map2attrs(attrsMap),
			Type:   "credit",
			Dt:     onDt.Add(time.Minute),
			Amount: "2",
		},
		{
			Loc:    s.map2attrs(attrsMap),
			Type:   "debit",
			Dt:     onDt.Add(2 * time.Minute),
			Amount: "3",
		},
		{
			Loc:    s.map2attrs(attrsMap),
			Type:   "credit",
			Dt:     onDt.Add(3 * time.Minute),
			Amount: "4.123456",
		},
		{
			Loc:    s.map2attrs(attrsMap),
			Type:   "debit",
			Dt:     onDt.Add(4 * time.Minute),
			Amount: "5.001",
		},
	}

	_, err := s.ctx.Actions.WriteBatch(s.ctx, s.genWriteRequest(events))
	s.Require().NoError(err)

	queryAttrs := s.map2attrs(attrsMap)
	balances, err := s.ctx.Actions.GetTurnover(s.ctx, onDt.Add(-time.Hour), onDt.Add(time.Hour), queryAttrs)
	s.Require().NoError(err)
	s.Assert().Len(balances, 1)
	s.Assert().Equal(balances[0].DebitInit, "0")
	s.Assert().Equal(balances[0].CreditInit, "0")
	s.Assert().Equal(balances[0].DebitTurnover, "9.001000")
	s.Assert().Equal(balances[0].CreditTurnover, "6.123456")
	s.Assert().Equal(balances[0].Loc.Type, "account_type")
	s.Assert().Equal(balances[0].Loc.Namespace, "accounts_test")
	s.Assert().Equal(balances[0].Loc.Attributes, attrsMap)
}

func (s *AccountsTurnoverTestSuite) TestSplitEvents() {
	attrsMap := genAttrMap()
	onDt := time.Now()

	events := []entities.EventAttributes{
		{
			Loc:    s.map2attrs(attrsMap),
			Type:   "debit",
			Dt:     onDt.Add(-2 * time.Minute),
			Amount: "1",
		},
		{
			Loc:    s.map2attrs(attrsMap),
			Type:   "credit",
			Dt:     onDt.Add(-time.Minute),
			Amount: "2",
		},
		{
			Loc:    s.map2attrs(attrsMap),
			Type:   "credit",
			Dt:     onDt,
			Amount: "3",
		},
		{
			Loc:    s.map2attrs(attrsMap),
			Type:   "debit",
			Dt:     onDt.Add(time.Minute),
			Amount: "4",
		},
		{
			Loc:    s.map2attrs(attrsMap),
			Type:   "credit",
			Dt:     onDt.Add(2 * time.Minute),
			Amount: "5",
		},
	}

	_, err := s.ctx.Actions.WriteBatch(s.ctx, s.genWriteRequest(events))
	s.Require().NoError(err)

	queryAttrs := s.map2attrs(attrsMap)
	balances, err := s.ctx.Actions.GetTurnover(s.ctx, onDt.Add(-30*time.Second), onDt.Add(90*time.Second), queryAttrs)
	s.Require().NoError(err)
	s.Assert().Len(balances, 1)
	s.Assert().Equal(balances[0].DebitInit, "1.000000")
	s.Assert().Equal(balances[0].CreditInit, "2.000000")
	s.Assert().Equal(balances[0].DebitTurnover, "4.000000")
	s.Assert().Equal(balances[0].CreditTurnover, "3.000000")
	s.Assert().Equal(balances[0].Loc.Type, "account_type")
	s.Assert().Equal(balances[0].Loc.Namespace, "accounts_test")
	s.Assert().Equal(balances[0].Loc.Attributes, attrsMap)
}

func (s *AccountsTurnoverTestSuite) TestSubaccount() {
	attrs := []string{
		btesting.RandS(100),
		btesting.RandS(100),
	}
	emptyAttr := ""
	addAttr1 := btesting.RandS(100)
	addAttr21 := btesting.RandS(100)
	addAttr22 := btesting.RandS(100)
	addAttrs1 := [entities.AttributesCount]string{addAttr1, addAttr21}
	addAttrs2 := [entities.AttributesCount]string{addAttr1, addAttr22}

	onDt := time.Now()

	events := []entities.EventAttributes{
		{
			Loc:    s.map2attrs(attr2map(attrs, addAttrs1)),
			Type:   "debit",
			Dt:     onDt,
			Amount: "1",
		},
		{
			Loc:    s.map2attrs(attr2map(attrs, addAttrs2)),
			Type:   "credit",
			Dt:     onDt,
			Amount: "2",
		},
	}

	_, err := s.ctx.Actions.WriteBatch(s.ctx, s.genWriteRequest(events))
	s.Require().NoError(err)

	queryAttrs := s.map2attrs(map[string]*string{
		"a1": &attrs[0],
		"a2": &attrs[1],
		"a3": &addAttr1,
		"a4": &emptyAttr,
	})
	balances, err := s.ctx.Actions.GetTurnover(s.ctx, onDt.Add(-time.Second), onDt.Add(time.Second), queryAttrs)
	s.Require().NoError(err)
	s.Assert().Len(balances, 1)
	s.Assert().Equal(balances[0].DebitInit, "0")
	s.Assert().Equal(balances[0].CreditInit, "0")
	s.Assert().Equal(balances[0].DebitTurnover, "1.000000")
	s.Assert().Equal(balances[0].CreditTurnover, "2.000000")
	s.Assert().Equal(balances[0].Loc.Type, "account_type")
	s.Assert().Equal(balances[0].Loc.Namespace, "accounts_test")
	s.Assert().Equal(balances[0].Loc.Attributes, map[string]*string{
		"a1": &attrs[0],
		"a2": &attrs[1],
		"a3": &addAttr1,
		"a4": &emptyAttr,
	})
}

func (s *AccountsTurnoverTestSuite) TestSubaccountMask() {
	attrs := []string{
		btesting.RandS(100),
		btesting.RandS(100),
	}
	addAttr1 := btesting.RandS(100)
	addAttr21 := btesting.RandS(100)
	addAttr22 := btesting.RandS(100)
	addAttrs1 := [entities.AttributesCount]string{addAttr1, addAttr21}
	addAttrs2 := [entities.AttributesCount]string{addAttr1, addAttr22}

	onDt := time.Now()

	events := []entities.EventAttributes{
		{
			Loc:    s.map2attrs(attr2map(attrs, addAttrs1)),
			Type:   "debit",
			Dt:     onDt,
			Amount: "1",
		},
		{
			Loc:    s.map2attrs(attr2map(attrs, addAttrs2)),
			Type:   "credit",
			Dt:     onDt,
			Amount: "2",
		},
	}

	_, err := s.ctx.Actions.WriteBatch(s.ctx, s.genWriteRequest(events))
	s.Require().NoError(err)

	queryAttrs := s.map2attrs(map[string]*string{
		"a1": &attrs[0],
		"a2": &attrs[1],
		"a3": &addAttr1,
		"a4": nil,
	})
	balances, err := s.ctx.Actions.GetTurnover(s.ctx, onDt.Add(-time.Second), onDt.Add(time.Second), queryAttrs)
	s.Require().NoError(err)
	s.Assert().Len(balances, 2)
	s.Assert().ElementsMatch(
		[]string{balances[0].DebitInit, balances[1].DebitInit},
		[]string{"0", "0"},
	)
	s.Assert().ElementsMatch(
		[]string{balances[0].CreditInit, balances[1].CreditInit},
		[]string{"0", "0"},
	)
	s.Assert().ElementsMatch(
		[]string{balances[0].DebitTurnover, balances[1].DebitTurnover},
		[]string{"1.000000", "0"},
	)
	s.Assert().ElementsMatch(
		[]string{balances[0].CreditTurnover, balances[1].CreditTurnover},
		[]string{"2.000000", "0"},
	)
	s.Assert().ElementsMatch(
		[]string{balances[0].Loc.Type, balances[1].Loc.Type},
		[]string{"account_type", "account_type"},
	)
	s.Assert().ElementsMatch(
		[]string{balances[0].Loc.Namespace, balances[1].Loc.Namespace},
		[]string{"accounts_test", "accounts_test"},
	)
	s.Assert().ElementsMatch(
		[]map[string]*string{
			balances[0].Loc.Attributes,
			balances[1].Loc.Attributes,
		},
		[]map[string]*string{
			{
				"a1": &attrs[0],
				"a2": &attrs[1],
				"a3": &addAttr1,
				"a4": &addAttr21,
			},
			{
				"a1": &attrs[0],
				"a2": &attrs[1],
				"a3": &addAttr1,
				"a4": &addAttr22,
			},
		},
	)
}

type AccountsTransactionsTestSuite struct {
	CommonAccountsTestSuite
}

func (s *AccountsTransactionsTestSuite) TestSplitEvents() {
	attrsMap := genAttrMap()
	onDt := time.Now().UTC().Truncate(time.Nanosecond * 1000)

	events := []entities.EventAttributes{
		{
			Loc:    s.map2attrs(attrsMap),
			Type:   "debit",
			Dt:     onDt.Add(-2 * time.Minute),
			Amount: "1",
			Info:   []byte("1"),
		},
		{
			Loc:    s.map2attrs(attrsMap),
			Type:   "credit",
			Dt:     onDt.Add(-time.Minute),
			Amount: "2",
			Info:   []byte("2"),
		},
		{
			Loc:    s.map2attrs(attrsMap),
			Type:   "credit",
			Dt:     onDt,
			Amount: "3",
			Info:   []byte("3"),
		},
		{
			Loc:    s.map2attrs(attrsMap),
			Type:   "debit",
			Dt:     onDt.Add(time.Minute),
			Amount: "4",
			Info:   []byte("4"),
		},
		{
			Loc:    s.map2attrs(attrsMap),
			Type:   "credit",
			Dt:     onDt.Add(2 * time.Minute),
			Amount: "5",
			Info:   []byte("5"),
		},
	}

	writeRequest := s.genWriteRequestInfo(events, []byte(`{"some": "info"}`))
	_, err := s.ctx.Actions.WriteBatch(s.ctx, writeRequest)
	s.Require().NoError(err)

	queryAttrs := s.map2attrs(attrsMap)
	transactions, err := s.ctx.Actions.GetDetailedTurnover(
		s.ctx,
		onDt.Add(-30*time.Second),
		onDt.Add(90*time.Second),
		queryAttrs)
	s.Require().NoError(err)
	s.Assert().Len(transactions, 1)
	s.Assert().Equal(transactions[0].DebitInit, "1.000000")
	s.Assert().Equal(transactions[0].CreditInit, "2.000000")
	s.Assert().Equal(transactions[0].DebitTurnover, "4.000000")
	s.Assert().Equal(transactions[0].CreditTurnover, "3.000000")
	s.Assert().Equal(transactions[0].Loc.Type, "account_type")
	s.Assert().Equal(transactions[0].Loc.Namespace, "accounts_test")
	s.Assert().Equal(transactions[0].Loc.Attributes, attrsMap)
	s.Assert().ElementsMatch(transactions[0].Events, []entities.EventDetails{
		{
			Type:      "credit",
			Dt:        onDt,
			Amount:    "3.000000",
			EventType: "test",
			EventID:   writeRequest.ExternalID,
			EventInfo: []byte("3"),
			Info:      []byte(`{"some": "info"}`),
		},
		{
			Type:      "debit",
			Dt:        onDt.Add(time.Minute),
			Amount:    "4.000000",
			EventType: "test",
			EventID:   writeRequest.ExternalID,
			EventInfo: []byte("4"),
			Info:      []byte(`{"some": "info"}`),
		},
	})
}

func (s *AccountsTransactionsTestSuite) TestBefore() {
	attrsMap := genAttrMap()
	onDt := time.Now().UTC().Truncate(time.Nanosecond * 1000)

	events := []entities.EventAttributes{
		{
			Loc:    s.map2attrs(attrsMap),
			Type:   "debit",
			Dt:     onDt,
			Amount: "1",
		},
	}

	_, err := s.ctx.Actions.WriteBatch(s.ctx, s.genWriteRequest(events))
	s.Require().NoError(err)

	queryAttrs := s.map2attrs(attrsMap)
	transactions, err := s.ctx.Actions.GetDetailedTurnover(
		s.ctx,
		onDt.Add(-30*time.Second),
		onDt.Add(-20*time.Second),
		queryAttrs)
	s.Require().NoError(err)
	s.Assert().Len(transactions, 1)
	s.Assert().Equal(transactions[0].DebitInit, "0")
	s.Assert().Equal(transactions[0].CreditInit, "0")
	s.Assert().Equal(transactions[0].DebitTurnover, "0")
	s.Assert().Equal(transactions[0].CreditTurnover, "0")
	s.Assert().Equal(transactions[0].Loc.Type, "account_type")
	s.Assert().Equal(transactions[0].Loc.Namespace, "accounts_test")
	s.Assert().Equal(transactions[0].Loc.Attributes, attrsMap)
	s.Assert().Len(transactions[0].Events, 0)
}

func (s *AccountsTransactionsTestSuite) TestAfter() {
	attrsMap := genAttrMap()
	onDt := time.Now().UTC().Truncate(time.Nanosecond * 1000)

	events := []entities.EventAttributes{
		{
			Loc:    s.map2attrs(attrsMap),
			Type:   "debit",
			Dt:     onDt,
			Amount: "1",
		},
	}

	_, err := s.ctx.Actions.WriteBatch(s.ctx, s.genWriteRequest(events))
	s.Require().NoError(err)

	queryAttrs := s.map2attrs(attrsMap)
	transactions, err := s.ctx.Actions.GetDetailedTurnover(
		s.ctx,
		onDt.Add(time.Minute),
		onDt.Add(2*time.Minute),
		queryAttrs)
	s.Require().NoError(err)
	s.Assert().Len(transactions, 1)
	s.Assert().Equal(transactions[0].DebitInit, "1.000000")
	s.Assert().Equal(transactions[0].CreditInit, "0")
	s.Assert().Equal(transactions[0].DebitTurnover, "0")
	s.Assert().Equal(transactions[0].CreditTurnover, "0")
	s.Assert().Equal(transactions[0].Loc.Type, "account_type")
	s.Assert().Equal(transactions[0].Loc.Namespace, "accounts_test")
	s.Assert().Equal(transactions[0].Loc.Attributes, attrsMap)
	s.Assert().Len(transactions[0].Events, 0)
}

func (s *AccountsTransactionsTestSuite) TestLeftBorder() {
	attrsMap := genAttrMap()
	onDt := time.Now().UTC().Truncate(time.Nanosecond * 1000)

	events := []entities.EventAttributes{
		{
			Loc:    s.map2attrs(attrsMap),
			Type:   "debit",
			Dt:     onDt,
			Amount: "1",
		},
	}

	writeRequest := s.genWriteRequest(events)
	_, err := s.ctx.Actions.WriteBatch(s.ctx, writeRequest)
	s.Require().NoError(err)

	queryAttrs := s.map2attrs(attrsMap)
	transactions, err := s.ctx.Actions.GetDetailedTurnover(s.ctx, onDt, onDt.Add(2*time.Minute), queryAttrs)
	s.Require().NoError(err)
	s.Assert().Len(transactions, 1)
	s.Assert().Equal(transactions[0].DebitInit, "0")
	s.Assert().Equal(transactions[0].CreditInit, "0")
	s.Assert().Equal(transactions[0].DebitTurnover, "1.000000")
	s.Assert().Equal(transactions[0].CreditTurnover, "0")
	s.Assert().Equal(transactions[0].Loc.Type, "account_type")
	s.Assert().Equal(transactions[0].Loc.Namespace, "accounts_test")
	s.Assert().Equal(transactions[0].Loc.Attributes, attrsMap)
	s.Assert().ElementsMatch(transactions[0].Events, []entities.EventDetails{
		{
			Type:      "debit",
			Dt:        onDt,
			Amount:    "1.000000",
			EventType: "test",
			EventID:   writeRequest.ExternalID,
			Info:      []byte(`{}`),
		},
	})
}

func (s *AccountsTransactionsTestSuite) TestLeftBorderRollup() {
	attrs := []string{
		btesting.RandS(100),
		btesting.RandS(100),
	}
	addAttrs := [entities.AttributesCount]string{
		btesting.RandS(100),
		btesting.RandS(100),
	}
	attrsMap := attr2map(attrs, addAttrs)

	accountID := s.createAccount(attrs)
	subaccountID := s.createSubAccount(accountID, addAttrs[:])

	onDt := time.Now().UTC().Truncate(time.Nanosecond * 1000)

	events := []entities.EventAttributes{
		{
			Loc:    s.map2attrs(attrsMap),
			Type:   "credit",
			Dt:     onDt.Add(-time.Minute),
			Amount: "1",
			Info:   []byte(`{"cred":1}`),
		},
		{
			Loc:    s.map2attrs(attrsMap),
			Type:   "debit",
			Dt:     onDt,
			Amount: "1",
			Info:   []byte(`{"deb":1}`),
		},
	}

	writeRequest := s.genWriteRequest(events)
	_, err := s.ctx.Actions.WriteBatch(s.ctx, writeRequest)
	s.Require().NoError(err)
	s.rollupAccount(accountID, subaccountID, onDt, "123", "456")

	queryAttrs := s.map2attrs(attrsMap)
	transactions, err := s.ctx.Actions.GetDetailedTurnover(s.ctx, onDt, onDt.Add(2*time.Minute), queryAttrs)
	s.Require().NoError(err)
	s.Assert().Len(transactions, 1)
	s.Assert().Equal(transactions[0].DebitInit, "123.000000")
	s.Assert().Equal(transactions[0].CreditInit, "456.000000")
	s.Assert().Equal(transactions[0].DebitTurnover, "1.000000")
	s.Assert().Equal(transactions[0].CreditTurnover, "0")
	s.Assert().Equal(transactions[0].Loc.Type, "account_type")
	s.Assert().Equal(transactions[0].Loc.Namespace, "accounts_test")
	s.Assert().Equal(transactions[0].Loc.Attributes, attrsMap)
	s.Assert().ElementsMatch(transactions[0].Events, []entities.EventDetails{
		{
			Type:      "debit",
			Dt:        onDt,
			Amount:    "1.000000",
			EventType: "test",
			EventID:   writeRequest.ExternalID,
			Info:      []byte(`{}`),
			EventInfo: []byte(`{"deb":1}`),
		},
	})
}

func (s *AccountsTransactionsTestSuite) TestRightBorder() {
	attrsMap := genAttrMap()
	onDt := time.Now().UTC().Truncate(time.Nanosecond * 1000)
	events := []entities.EventAttributes{
		{
			Loc:    s.map2attrs(attrsMap),
			Type:   "debit",
			Dt:     onDt,
			Amount: "1",
		},
	}

	_, err := s.ctx.Actions.WriteBatch(s.ctx, s.genWriteRequest(events))
	s.Require().NoError(err)

	queryAttrs := s.map2attrs(attrsMap)
	transactions, err := s.ctx.Actions.GetDetailedTurnover(s.ctx, onDt.Add(-time.Minute), onDt, queryAttrs)
	s.Require().NoError(err)
	s.Assert().Len(transactions, 1)
	s.Assert().Equal(transactions[0].DebitInit, "0")
	s.Assert().Equal(transactions[0].CreditInit, "0")
	s.Assert().Equal(transactions[0].DebitTurnover, "0")
	s.Assert().Equal(transactions[0].CreditTurnover, "0")
	s.Assert().Equal(transactions[0].Loc.Type, "account_type")
	s.Assert().Equal(transactions[0].Loc.Namespace, "accounts_test")
	s.Assert().Equal(transactions[0].Loc.Attributes, attrsMap)
	s.Assert().Len(transactions[0].Events, 0)
}

func (s *AccountsTransactionsTestSuite) TestSubaccount() {
	attrs := []string{
		btesting.RandS(100),
		btesting.RandS(100),
	}
	emptyAttr := ""
	addAttr1 := btesting.RandS(100)
	addAttr21 := btesting.RandS(100)
	addAttr22 := btesting.RandS(100)
	addAttrs1 := [entities.AttributesCount]string{addAttr1, addAttr21}
	addAttrs2 := [entities.AttributesCount]string{addAttr1, addAttr22}

	onDt := time.Now().UTC().Truncate(time.Nanosecond * 1000)

	events := []entities.EventAttributes{
		{
			Loc:    s.map2attrs(attr2map(attrs, addAttrs1)),
			Type:   "debit",
			Dt:     onDt,
			Amount: "1",
		},
		{
			Loc:    s.map2attrs(attr2map(attrs, addAttrs2)),
			Type:   "credit",
			Dt:     onDt,
			Amount: "2",
		},
	}

	writeRequest := s.genWriteRequest(events)
	_, err := s.ctx.Actions.WriteBatch(s.ctx, writeRequest)
	s.Require().NoError(err)

	queryAttrs := s.map2attrs(map[string]*string{
		"a1": &attrs[0],
		"a2": &attrs[1],
		"a3": &addAttr1,
		"a4": &emptyAttr,
	})
	transactions, err := s.ctx.Actions.GetDetailedTurnover(
		s.ctx,
		onDt.Add(-time.Second),
		onDt.Add(time.Second),
		queryAttrs)
	s.Require().NoError(err)
	s.Assert().Len(transactions, 1)
	s.Assert().Equal(transactions[0].DebitInit, "0")
	s.Assert().Equal(transactions[0].CreditInit, "0")
	s.Assert().Equal(transactions[0].DebitTurnover, "1.000000")
	s.Assert().Equal(transactions[0].CreditTurnover, "2.000000")
	s.Assert().Equal(transactions[0].Loc.Type, "account_type")
	s.Assert().Equal(transactions[0].Loc.Namespace, "accounts_test")
	s.Assert().Equal(transactions[0].Loc.Attributes, map[string]*string{
		"a1": &attrs[0],
		"a2": &attrs[1],
		"a3": &addAttr1,
		"a4": &emptyAttr,
	})
	s.Assert().ElementsMatch(transactions[0].Events, []entities.EventDetails{
		{
			Type:      "debit",
			Dt:        onDt,
			Amount:    "1.000000",
			EventType: "test",
			EventID:   writeRequest.ExternalID,
			Info:      []byte(`{}`),
		},
		{
			Type:      "credit",
			Dt:        onDt,
			Amount:    "2.000000",
			EventType: "test",
			EventID:   writeRequest.ExternalID,
			Info:      []byte(`{}`),
		},
	})
}

func (s *AccountsTransactionsTestSuite) TestSubaccountMask() {
	attrs := []string{
		btesting.RandS(100),
		btesting.RandS(100),
	}
	addAttr := btesting.RandS(100)
	attrsMap1 := attr2map(attrs, [entities.AttributesCount]string{addAttr, btesting.RandS(100)})
	attrsMap2 := attr2map(attrs, [entities.AttributesCount]string{addAttr, btesting.RandS(100)})
	attrsMap3 := attr2map(attrs, [entities.AttributesCount]string{addAttr, btesting.RandS(100)})

	onDt := time.Now().UTC().Truncate(time.Nanosecond * 1000)

	events := []entities.EventAttributes{
		{
			Loc:    s.map2attrs(attrsMap1),
			Type:   "debit",
			Dt:     onDt.Add(-time.Hour),
			Amount: "1",
		},
		{
			Loc:    s.map2attrs(attrsMap1),
			Type:   "credit",
			Dt:     onDt.Add(-time.Hour),
			Amount: "2",
		},
		{
			Loc:    s.map2attrs(attrsMap2),
			Type:   "debit",
			Dt:     onDt,
			Amount: "3",
		},
		{
			Loc:    s.map2attrs(attrsMap2),
			Type:   "credit",
			Dt:     onDt,
			Amount: "4",
		},
		{
			Loc:    s.map2attrs(attrsMap3),
			Type:   "debit",
			Dt:     onDt.Add(time.Hour),
			Amount: "3",
		},
	}

	writeRequest := s.genWriteRequest(events)
	_, err := s.ctx.Actions.WriteBatch(s.ctx, writeRequest)
	s.Require().NoError(err)

	queryAttrs := s.map2attrs(map[string]*string{
		"a1": &attrs[0],
		"a2": &attrs[1],
		"a3": &addAttr,
		"a4": nil,
	})
	transactions, err := s.ctx.Actions.GetDetailedTurnover(
		s.ctx,
		onDt.Add(-time.Second),
		onDt.Add(time.Second),
		queryAttrs)
	s.Require().NoError(err)
	res := []entities.DetailedTurnoverAttributes{
		{
			Loc:            s.map2attrs(attrsMap1),
			DebitInit:      "1.000000",
			CreditInit:     "2.000000",
			DebitTurnover:  "0",
			CreditTurnover: "0",
			Events:         make([]entities.EventDetails, 0),
		},
		{
			Loc:            s.map2attrs(attrsMap2),
			DebitInit:      "0",
			CreditInit:     "0",
			DebitTurnover:  "3.000000",
			CreditTurnover: "4.000000",
			Events: []entities.EventDetails{
				{
					Type:      "credit",
					Dt:        onDt,
					Amount:    "4.000000",
					EventType: "test",
					EventID:   writeRequest.ExternalID,
					Info:      []byte("{}"),
				},
				{
					Type:      "debit",
					Dt:        onDt,
					Amount:    "3.000000",
					EventType: "test",
					EventID:   writeRequest.ExternalID,
					Info:      []byte("{}"),
				},
			},
		},
		{
			Loc:            s.map2attrs(attrsMap3),
			DebitInit:      "0",
			CreditInit:     "0",
			DebitTurnover:  "0",
			CreditTurnover: "0",
			Events:         make([]entities.EventDetails, 0),
		},
	}
	s.Assert().ElementsMatch(res, transactions)
}

func TestAccountsAddEventsTestSuite(t *testing.T) {
	suite.Run(t, new(AccountsAddEventsTestSuite))
}

func TestAccountsBalanceTestSuite(t *testing.T) {
	suite.Run(t, new(AccountsBalanceTestSuite))
}

func TestAccountsTurnoverTestSuite(t *testing.T) {
	suite.Run(t, new(AccountsTurnoverTestSuite))
}

func TestAccountsTransactionTestSuite(t *testing.T) {
	suite.Run(t, new(AccountsTransactionsTestSuite))
}
