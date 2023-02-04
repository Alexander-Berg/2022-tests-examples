package impl

import (
	"context"
	"fmt"
	"strings"
	"testing"
	"time"

	"github.com/gofrs/uuid"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/suite"

	"a.yandex-team.ru/billing/hot/accounts/pkg/core/entities"
	coreerrors "a.yandex-team.ru/billing/hot/accounts/pkg/core/errors"
	bsql "a.yandex-team.ru/billing/library/go/billingo/pkg/storage/sql"
	"a.yandex-team.ru/billing/library/go/billingo/pkg/storage/sql/backends/pg"
	btesting "a.yandex-team.ru/billing/library/go/billingo/pkg/testing"
	"a.yandex-team.ru/library/go/core/xerrors"
)

type LockActionTestSuite struct {
	ActionTestSuite
}

func (s *LockActionTestSuite) SetupTest() {
	cfg := `
manifests:
- namespace: lock_test
  locks:
    lock_type:
      attributes:
        - a1
        - a2
        - a3
      shard:
        prefix: p
        attributes:
          - a1
      rollup_period:
        - "0 * * *"
`
	s.ActionTestSuite.SetupTest()
	s.ActionTestSuite.loadCfg(cfg)
	s.ActionTestSuite.initActions()
}

func (s *LockActionTestSuite) getRandomAttributes() entities.LocationAttributes {
	attrs := []string{
		btesting.RandS(100),
		btesting.RandS(100),
		btesting.RandS(100),
	}
	attrsMap := map[string]*string{"a1": &attrs[0], "a2": &attrs[1], "a3": &attrs[2]}
	return entities.LocationAttributes{
		Namespace:  "lock_test",
		Type:       "lock_type",
		Attributes: attrsMap,
	}
}

func (s *LockActionTestSuite) attrs2loc(attributes entities.LocationAttributes) entities.Location {
	return entities.Location{
		Namespace: attributes.Namespace,
		Type:      attributes.Type,
		Attributes: [entities.AttributesCount]string{
			*attributes.Attributes["a1"],
			*attributes.Attributes["a2"],
			*attributes.Attributes["a3"],
		},
	}
}

func (s *LockActionTestSuite) checkDateRange(dt time.Time, timeout int64) {
	now := time.Now().UTC()
	left := now.Add(time.Duration(timeout-1) * time.Second)
	right := now.Add(time.Duration(timeout+1) * time.Second)
	assert.True(s.T(), dt.After(left) && dt.Before(right),
		fmt.Sprintf("dt - %s\tleft boundary - %s\tright boundary - %s", dt, left, right))
}

func (s *LockActionTestSuite) validateAction(locks []entities.Lock) error {
	shard, err := s.ctx.Shards.GetLastShard()
	s.Require().NoError(err)
	ctx, cancelFunc := context.WithTimeout(s.ctx, time.Second)
	defer cancelFunc()
	return shard.GetLockStorage(s.ctx.Templates).ValidateDB(ctx, shard.GetDatabase(ctx, pg.Master), locks)
}

func (s *LockActionTestSuite) lockRow(ctx context.Context, db bsql.DBInterface, loc entities.Location) error {
	queryTmpl := "SELECT uid, dt FROM acc.t_lock WHERE type = $1 AND %s FOR UPDATE NOWAIT"

	predicates := make([]string, 0, 6)
	args := []any{
		loc.Type,
	}
	for i, val := range loc.Attributes {
		predicates = append(predicates, fmt.Sprintf("attribute_%d=$%d", i+1, i+2))
		args = append(args, val)
	}
	query := fmt.Sprintf(queryTmpl, strings.Join(predicates, " AND "))

	var uid string
	var dt time.Time
	row := db.QueryRowxContext(ctx, query, args...)
	err := row.Scan(&uid, &dt)
	return err
}

// Получение несуществующей блокировки
func (s *LockActionTestSuite) TestGetAbsentLock() {
	lockAttrs := s.getRandomAttributes()

	actualLock, err := s.ctx.Actions.GetLock(s.ctx, lockAttrs)
	s.Require().NoError(err)

	assert.True(s.T(), actualLock.Dt.Before(time.Now().UTC()))

	anotherActualLock, err := s.ctx.Actions.GetLock(s.ctx, lockAttrs)
	s.Require().NoError(err)

	assert.Equal(s.T(), actualLock, anotherActualLock)
}

// Получение существующей активной блокировки
func (s *LockActionTestSuite) TestSearchForExistingLock() {
	lockAttrs := s.getRandomAttributes()
	uid, err := s.ctx.Actions.InitLock(s.ctx, lockAttrs, 10)
	s.Require().NoError(err)

	actualLock, err := s.ctx.Actions.GetLock(s.ctx, lockAttrs)
	s.Require().NoError(err)

	assert.NotNil(s.T(), actualLock)
	assert.Equal(s.T(), uid, actualLock.UID)
}

// Пинг несуществующей блокировки
func (s *LockActionTestSuite) TestPingAbsentLock() {
	err := s.ctx.Actions.PingLock(s.ctx, s.getRandomAttributes(), 20, "15c1865a-5ff4-46f4-82d6-8926bfcaeaf4")
	// nolinter: lll
	assert.EqualError(s.T(), err, "failed to ping lock: error during evaluation of transaction callback: failed to ping lock: affected unexpected number of rows - 1 expected, 0 actual")
}

// Пинг существующей активной блокировки + создание новой блокировки (не существовавшей до этого в базе)
func (s *LockActionTestSuite) TestPingOfExistingLock() {
	lockAttrs := s.getRandomAttributes()
	uid, err := s.ctx.Actions.InitLock(s.ctx, lockAttrs, 10)
	s.Require().NoError(err)

	actualLock, err := s.ctx.Actions.GetLock(s.ctx, lockAttrs)
	s.Require().NoError(err)
	s.checkDateRange(actualLock.Dt, 10)

	// Пинг блокировки не должен вызвать проблем
	err = s.ctx.Actions.PingLock(s.ctx, lockAttrs, 20, uid)
	s.Require().NoError(err)

	actualLock, err = s.ctx.Actions.GetLock(s.ctx, lockAttrs)
	s.Require().NoError(err)
	assert.NotNil(s.T(), actualLock)
	assert.Equal(s.T(), uid, actualLock.UID)

	s.checkDateRange(actualLock.Dt, 20)
}

// Пинг протухшей блокировки
func (s *LockActionTestSuite) TestPingObsoleteLock() {
	lockAttrs := s.getRandomAttributes()
	uid, err := s.ctx.Actions.InitLock(s.ctx, lockAttrs, -10)
	s.Require().NoError(err)

	err = s.ctx.Actions.PingLock(s.ctx, lockAttrs, 10, uid)
	// nolinter: lll
	assert.EqualError(s.T(), err, "failed to ping lock: error during evaluation of transaction callback: failed to ping lock: affected unexpected number of rows - 1 expected, 0 actual")
}

// Получение активной блокировки
func (s *LockActionTestSuite) TestInitActiveLock() {
	lockAttrs := s.getRandomAttributes()
	_, err := s.ctx.Actions.InitLock(s.ctx, lockAttrs, 10)
	s.Require().NoError(err)

	_, err = s.ctx.Actions.InitLock(s.ctx, lockAttrs, 15)
	// nolinter: lll
	assert.EqualError(s.T(), err, "failed to init lock: error during evaluation of transaction callback: failed to initialize locks")
}

// Получение протухшей блокировки
func (s *LockActionTestSuite) TestInitObsoleteLock() {
	lockAttrs := s.getRandomAttributes()
	uid, err := s.ctx.Actions.InitLock(s.ctx, lockAttrs, -10)
	s.Require().NoError(err)

	newUID, err := s.ctx.Actions.InitLock(s.ctx, lockAttrs, 10)
	s.Require().NoError(err)
	s.Assert().NotEqual(newUID, uid)
}

// Cоздание уже существующей блокировки
func (s *LockActionTestSuite) TestInitNewDublicateLock() {
	lockAttrs := s.getRandomAttributes()
	loc, err := s.ctx.EntitySettings.Lock().Location(&lockAttrs)
	s.Require().NoError(err)

	shard, err := s.ctx.Shards.GetLastShard()
	s.Require().NoError(err)

	// создаём новую блокировку
	err = shard.GetDatabase(s.ctx, pg.Master).Tx(s.ctx, func(ctx context.Context, db bsql.Transaction) error {
		_, err := shard.GetLockStorage(s.ctx.Templates).InitDB(ctx, db, []entities.Location{loc}, 666)
		return err
	})
	s.Require().NoError(err)

	// запускаем следующий инсерт и виснем
	_, err = s.ctx.Actions.InitLock(s.ctx, lockAttrs, 15)

	s.Require().Error(err)
	var codedErr coreerrors.CodedError
	s.Require().True(xerrors.As(err, &codedErr))
	s.Assert().Equal(codedErr.CharCode(), "LOCK_UPDATE_FAIL")
}

// Проверка того, что пинг одной блокировки не задевает другую
func (s *LockActionTestSuite) TestPingMultipleLocks() {
	lockAttrs1 := s.getRandomAttributes()
	lockAttrs2 := s.getRandomAttributes()

	uid1, err := s.ctx.Actions.InitLock(s.ctx, lockAttrs1, 10)
	s.Require().NoError(err)

	uid2, err := s.ctx.Actions.InitLock(s.ctx, lockAttrs2, 10)
	s.Require().NoError(err)

	err = s.ctx.Actions.PingLock(s.ctx, lockAttrs1, 20, uid1)
	s.Require().NoError(err)

	lock, err := s.ctx.Actions.GetLock(s.ctx, lockAttrs2)
	s.Require().NoError(err)
	s.Assert().Equal(uid2, lock.UID)
	s.checkDateRange(lock.Dt, 10)
}

// Снятие активной блокировки
func (s *LockActionTestSuite) TestRemoveActiveLock() {
	lockAttrs := s.getRandomAttributes()
	uid, err := s.ctx.Actions.InitLock(s.ctx, lockAttrs, 10)
	s.Require().NoError(err)

	err = s.ctx.Actions.RemoveLock(s.ctx, lockAttrs, uid)
	s.Require().NoError(err)
}

// Снятие протухшей блокировки
func (s *LockActionTestSuite) TestRemoveObsoleteLock() {
	lockAttrs := s.getRandomAttributes()
	uid, err := s.ctx.Actions.InitLock(s.ctx, lockAttrs, -10)
	s.Require().NoError(err)

	err = s.ctx.Actions.RemoveLock(s.ctx, lockAttrs, uid)
	// nolinter: lll
	s.Assert().EqualError(err, "failed to remove lock: error during evaluation of transaction callback: failed to remove active locks: affected unexpected number of rows - 1 expected, 0 actual")
}

// Снятие блокировки, которой нет в базе вообще
func (s *LockActionTestSuite) TestRemoveAbsentLock() {
	lockAttrs := s.getRandomAttributes()
	uid, err := uuid.NewGen().NewV4()
	if err != nil {
		s.T().Fatal(err)
	}

	err = s.ctx.Actions.RemoveLock(s.ctx, lockAttrs, uid.String())
	// nolinter: lll
	s.Assert().EqualError(err, "failed to remove lock: error during evaluation of transaction callback: failed to remove active locks: affected unexpected number of rows - 1 expected, 0 actual")
}

func (s *LockActionTestSuite) TestValidateActive() {
	attrs1 := s.getRandomAttributes()
	attrs2 := s.getRandomAttributes()

	uid1, err := s.ctx.Actions.InitLock(s.ctx, attrs1, -666)
	s.Require().NoError(err)
	uid2, err := s.ctx.Actions.InitLock(s.ctx, attrs2, 666)
	s.Require().NoError(err)

	err = s.validateAction(
		[]entities.Lock{
			{Loc: s.attrs2loc(attrs1), UID: uid1},
			{Loc: s.attrs2loc(attrs2), UID: uid2},
		})
	s.Require().Error(err)
	var codedErr coreerrors.CodedError
	s.Assert().EqualError(err, "Some locks are active or invalid")
	s.Require().True(xerrors.As(err, &codedErr))
	s.Assert().Equal(codedErr.CharCode(), "LOCK_VALIDATE_FAIL")
}

func (s *LockActionTestSuite) TestValidateExpired() {
	attrs1 := s.getRandomAttributes()
	attrs2 := s.getRandomAttributes()

	uid1, err := s.ctx.Actions.InitLock(s.ctx, attrs1, -666)
	s.Require().NoError(err)
	uid2, err := s.ctx.Actions.InitLock(s.ctx, attrs2, -666)
	s.Require().NoError(err)

	err = s.validateAction(
		[]entities.Lock{
			{Loc: s.attrs2loc(attrs1), UID: uid1},
			{Loc: s.attrs2loc(attrs2), UID: uid2},
		})
	s.Require().NoError(err)
}

func (s *LockActionTestSuite) TestValidateChanged() {
	attrs1 := s.getRandomAttributes()
	attrs2 := s.getRandomAttributes()

	uid1, err := s.ctx.Actions.InitLock(s.ctx, attrs1, 666)
	s.Require().NoError(err)
	_, err = s.ctx.Actions.InitLock(s.ctx, attrs2, 666)
	s.Require().NoError(err)

	gen := uuid.NewGen()
	altUID, err := gen.NewV4()
	s.Require().NoError(err)
	err = s.validateAction(
		[]entities.Lock{
			{Loc: s.attrs2loc(attrs1), UID: uid1},
			{Loc: s.attrs2loc(attrs2), UID: altUID.String()},
		})

	s.Require().Error(err)
	var codedErr coreerrors.CodedError
	s.Assert().EqualError(err, "Some locks are active or invalid")
	s.Require().True(xerrors.As(err, &codedErr))
	s.Assert().Equal(codedErr.CharCode(), "LOCK_VALIDATE_FAIL")
}

func (s *LockActionTestSuite) TestValidateAbsent() {
	attrs1 := s.getRandomAttributes()
	attrs2 := s.getRandomAttributes()

	uid1, err := s.ctx.Actions.InitLock(s.ctx, attrs1, 666)
	s.Require().NoError(err)

	gen := uuid.NewGen()
	altUID, err := gen.NewV4()
	s.Require().NoError(err)
	err = s.validateAction(
		[]entities.Lock{
			{Loc: s.attrs2loc(attrs1), UID: uid1},
			{Loc: s.attrs2loc(attrs2), UID: altUID.String()},
		})
	s.Require().Error(err)
	var codedErr coreerrors.CodedError
	s.Assert().EqualError(err, "Some locks are active or invalid")
	s.Require().True(xerrors.As(err, &codedErr))
	s.Assert().Equal(codedErr.CharCode(), "LOCK_VALIDATE_FAIL")
}

func (s *LockActionTestSuite) TestValidateLocked() {
	attrs1 := s.getRandomAttributes()
	attrs2 := s.getRandomAttributes()

	uid1, err := s.ctx.Actions.InitLock(s.ctx, attrs1, -666)
	s.Require().NoError(err)
	uid2, err := s.ctx.Actions.InitLock(s.ctx, attrs2, -666)
	s.Require().NoError(err)

	shard, err := s.ctx.Shards.GetLastShard()
	s.Require().NoError(err)

	err = shard.GetDatabase(s.ctx, pg.Master).Tx(s.ctx, func(ctx context.Context, db bsql.Transaction) error {
		err := s.lockRow(ctx, db, s.attrs2loc(attrs1))
		if err != nil {
			return err
		}

		err = s.validateAction(
			[]entities.Lock{
				{Loc: s.attrs2loc(attrs1), UID: uid1},
				{Loc: s.attrs2loc(attrs2), UID: uid2},
			})

		return err
	})
	s.Require().Error(err)
	var codedErr coreerrors.CodedError
	s.Require().True(xerrors.As(err, &codedErr))
	s.Assert().Equal(codedErr.CharCode(), "DATABASE_ERROR")
	s.Assert().Contains(codedErr.Error(), "context deadline exceeded")
}

func (s *LockActionTestSuite) TestValidateDBLock() {
	attrs := s.getRandomAttributes()

	uid, err := s.ctx.Actions.InitLock(s.ctx, attrs, -666)
	s.Require().NoError(err)

	shard, err := s.ctx.Shards.GetLastShard()
	s.Require().NoError(err)

	err = shard.GetDatabase(s.ctx, pg.Master).Tx(s.ctx, func(ctx context.Context, db bsql.Transaction) error {
		err := shard.GetLockStorage(s.ctx.Templates).ValidateDB(
			s.ctx, db,
			[]entities.Lock{{Loc: s.attrs2loc(attrs), UID: uid}})
		s.Require().NoError(err)

		return s.lockRow(ctx, shard.GetDatabase(s.ctx, pg.Master), s.attrs2loc(attrs))
	})
	s.Require().Error(err)
	s.Assert().Contains(err.Error(), `could not obtain lock on row in relation "t_lock"`)
}

func TestLockActionTestSuite(t *testing.T) {
	suite.Run(t, new(LockActionTestSuite))
}
