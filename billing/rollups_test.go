package impl

import (
	"context"
	"fmt"
	"strconv"
	"strings"
	"testing"
	"time"

	sq "github.com/Masterminds/squirrel"
	"github.com/golang/mock/gomock"
	"github.com/stretchr/testify/suite"
	"golang.org/x/exp/slices"

	"a.yandex-team.ru/billing/hot/accounts/mock/actionsmock"
	"a.yandex-team.ru/billing/hot/accounts/pkg/core"
	"a.yandex-team.ru/billing/hot/accounts/pkg/core/actions"
	"a.yandex-team.ru/billing/hot/accounts/pkg/core/cron"
	"a.yandex-team.ru/billing/hot/accounts/pkg/core/entities"
	"a.yandex-team.ru/billing/hot/accounts/pkg/core/entitysettings"
	corepart "a.yandex-team.ru/billing/hot/accounts/pkg/core/partitioning"
	"a.yandex-team.ru/billing/hot/accounts/pkg/storage"
	"a.yandex-team.ru/billing/hot/accounts/pkg/storage/db/rollup"
	"a.yandex-team.ru/billing/library/go/billingo/pkg/storage/sql/backends/pg"
	btesting "a.yandex-team.ru/billing/library/go/billingo/pkg/testing"
)

func init() {
	rollup.MagicCrutchDate = time.Date(2000, time.April, 1, 0, 0, 0, 0, time.UTC)
}

type rollupSchedulerMock struct {
	accounts entitysettings.AccountSettings
}

func (r *rollupSchedulerMock) RollupSchedule(accountNamespace, accountType string) (entities.RollupSchedule, error,
) {
	rollupPeriods, err := r.accounts.RollupPeriod(&entities.LocationAttributes{
		Namespace: accountNamespace,
		Type:      accountType,
	})
	if err != nil {
		// hourly by default
		scheduler, _ := cron.NewScheduler([]string{
			"0 * * *",
		})
		return scheduler, nil
	}
	scheduler, err := cron.NewScheduler(rollupPeriods)
	if err != nil {
		return nil, err
	}
	return scheduler, nil
}

type RollupTestSuite struct {
	CommonAccountsTestSuite
	Rollup       actions.Rollup
	RollupConfig core.RollupConfig
	// this assumes rollup_period in config is "0 * * *" in order to preserve old tests
	aggregateInterval time.Duration
	rollupScheduler   entities.RollupScheduler
}

func TestRollupTestSuite(t *testing.T) {
	suite.Run(t, new(RollupTestSuite))
}

func (s *RollupTestSuite) SetupTest() {
	s.CommonAccountsTestSuite.SetupTest()
	s.initRollup()
}

func (s *RollupTestSuite) initRollup() {
	s.RollupConfig = core.RollupConfig{
		LockID: 42,
	}
	s.rollupScheduler = &rollupSchedulerMock{
		accounts: s.ctx.EntitySettings.Account(),
	}
	s.Rollup = NewRollupActions(
		s.ctx.Shards,
		s.RollupConfig,
		s.rollupScheduler,
		s.ctx.Templates,
		nil,
	)
	// this assumes rollup_period in config is "0 * * *" in order to preserve old tests
	s.aggregateInterval = time.Hour
}

func (s *RollupTestSuite) writeEvent(eventType string, dt time.Time, amount string, attrsMap map[string]*string) {
	s.writeEventWithLocationAttributes(eventType, dt, amount, s.map2attrs(attrsMap))
}

func (s *RollupTestSuite) writeEventWithLocationAttributes(
	eventType string,
	dt time.Time,
	amount string,
	loc entities.LocationAttributes,
) {
	_, err := s.ctx.Actions.WriteBatch(
		s.ctx,
		s.genWriteRequest(
			[]entities.EventAttributes{
				{
					Loc:    loc,
					Type:   eventType,
					Dt:     dt,
					Amount: amount,
				},
			}))

	s.Require().NoError(err)
}

type extendedRollupStorage interface {
	storage.RollupStorage

	GetEventLogBounds(ctx context.Context) (
		int64,
		int64,
		error,
	)
}

// getRollupAccount destructively updates future_id and rollup_time's
func (s *RollupTestSuite) getRollupAccount(accountID int64) *entities.RollupAccount {
	return s.getRollupAccountScheduled(accountID, s.rollupScheduler)
}

// getRollupAccountScheduled destructively updates future_id and rollup_time's
func (s *RollupTestSuite) getRollupAccountScheduled(accountID int64, scheduler entities.RollupScheduler) *entities.RollupAccount {
	shard, err := s.ctx.Shards.GetLastShard()
	s.Require().NoError(err)

	storage := shard.GetRollupStorage(s.ctx.Templates)

	lastID, futureID, err := storage.GetEventLogBoundsAndUpdateFutureID(s.ctx)
	s.Require().NoError(err)

	rollupInfo, err := storage.UpdateRollupTimesAndGetRollups(s.ctx, &entities.GetRollupsRequest{
		LastID:          lastID,
		FutureID:        futureID,
		RollupScheduler: scheduler,
	})

	s.Require().NoError(err)

	rollupAccount, ok := rollupInfo.RollupAccounts[accountID]
	s.Require().True(ok, fmt.Sprintf("Rollups for accountID=%d were not generated", accountID))

	s.Assert().NotEqual(0, len(rollupAccount.Rollups))

	return rollupAccount
}

func (s *RollupTestSuite) getRollupAccountRaw(accountID int64) (*entities.RollupAccount, error) {
	shard, err := s.ctx.Shards.GetLastShard()
	s.Require().NoError(err)

	storage := shard.GetRollupStorage(s.ctx.Templates).(extendedRollupStorage)

	lastID, _, err := storage.GetRollupBounds(s.ctx)
	s.Require().NoError(err)

	rollupInfo, err := storage.GetRollups(s.ctx, lastID, shard.GetMaxSeqID())
	if err != nil {
		return nil, err
	}

	return rollupInfo.RollupAccounts[accountID], nil
}

type updateRollupOpts struct {
	schedule entities.RollupScheduler
	metrics  actions.RollupMetricsService
}

type updateRollupOptsFunc func(r *updateRollupOpts)

func rollupWithScheduler(s entities.RollupScheduler) func(r *updateRollupOpts) {
	return func(r *updateRollupOpts) {
		r.schedule = s
	}
}

func rollupWithMetrics(m actions.RollupMetricsService) func(r *updateRollupOpts) {
	return func(r *updateRollupOpts) {
		r.metrics = m
	}
}

func (s *RollupTestSuite) updateRollupsExplicitError(optsFunc ...updateRollupOptsFunc) error {
	var opts updateRollupOpts

	for _, f := range optsFunc {
		f(&opts)
	}

	if opts.metrics == nil {
		opts.metrics = actionsmock.NewMockRollupMetricsService(gomock.NewController(s.T()))
	}
	if opts.schedule == nil {
		opts.schedule = s.rollupScheduler
	}

	return s.Rollup.
		WithMetrics(opts.metrics).
		WithScheduler(opts.schedule).
		UpdateRollups(s.ctx, s.ctx.Shards.GetLastShardID())
}

func (s *RollupTestSuite) updateRollups(opts ...updateRollupOptsFunc) {
	err := s.updateRollupsExplicitError(opts...)
	s.Require().NoError(err)
}

// TestSeqIDBoundaries проверяет, что мы учитываем верхнюю границу диапазона ID
// шарда в getMaxSeqID.
func (s *RollupTestSuite) TestSeqIDBoundaries() {
	attrsMap := genAttrMap()
	onDt := time.Now().UTC()
	s.writeEvent("debit", onDt, "13", attrsMap)
	s.writeEvent("debit", onDt, "0.666", attrsMap)

	s.updateEventSeqID(2)

	shard, err := s.ctx.Shards.GetLastShard()
	s.Require().NoError(err)

	updateSeqIDQuery := `
UPDATE acc.t_event
SET seq_id = $1
WHERE id = (
		SELECT id
		FROM acc.t_event
		LIMIT  1
	)
`
	// Устанавливаем ID за пределами диапазона шарда.
	_, err = shard.GetDatabase(s.ctx, pg.Master).ExecContext(s.ctx, updateSeqIDQuery, shard.GetMaxSeqID()+1)
	s.Require().NoError(err)

	storage := shard.GetRollupStorage(s.ctx.Templates)
	_, futureID, err := storage.GetEventLogBoundsAndUpdateFutureID(s.ctx)
	s.Require().NoError(err)
	s.Assert().LessOrEqual(futureID, shard.GetMaxSeqID())

	// Make lastID == futureID for future tests
	s.updateRollups()
}

func (s *RollupTestSuite) TestUpdateRollup() {
	// Для начала создадим несколько событий для одного счета с одинаковым набором атрибутов-
	// они должны сагрегироваться в одно
	attrsMap := genAttrMap()
	onDt := time.Now().UTC()

	accountInsertedID := s.createAccount(mainAttrs(attrsMap))
	s.writeEvent("debit", onDt, "13", attrsMap)
	s.writeEvent("debit", onDt, "0.666", attrsMap)
	s.writeEvent("credit", onDt, "0.666", attrsMap)

	// Первое обновление должно захватить все эти события
	s.updateEventSeqID(3)

	rollupAccount := s.getRollupAccount(accountInsertedID)
	s.Assert().NotEqual(0, len(rollupAccount.Rollups))
	nextDt := s.getNextDate(onDt)
	for _, rollup := range rollupAccount.Rollups {
		// дата должна быть началом следующих суток
		s.Assert().Equal(nextDt, rollup.Dt)

		debit, err := strconv.ParseFloat(rollup.Debit, 64)
		s.Require().NoError(err)
		s.Assert().Equal(13.666, debit)
		credit, err := strconv.ParseFloat(rollup.Credit, 64)
		s.Require().NoError(err)
		s.Assert().Equal(0.666, credit)
	}

	s.updateRollups()
}

func (s *RollupTestSuite) getNextDate(dt time.Time) time.Time {
	return dt.Add(s.aggregateInterval).Truncate(s.aggregateInterval)
}

// Кейс первый - предположим, есть остаток, и новое событие после даты этого остатка
// новое событие должно привести к созданию нового остатка, в который входит новое событие + старый остаток
func (s *RollupTestSuite) TestNewRollupContainsOldRollup() {
	attrsMap := genAttrMap()
	onDt := time.Now().UTC()

	// Сначала выполняем событие, которое приведет к созданию первого остатка
	s.writeEvent("debit", onDt, "13", attrsMap)
	s.writeEvent("credit", onDt, "13", attrsMap)

	s.updateEventSeqID(2)

	s.updateRollups()
	s.checkBalance(attrsMap, s.getNextDate(onDt), 13.0, 13.0)

	futureDt := onDt.Add(2 * s.aggregateInterval)
	s.writeEvent("debit", futureDt, "0.666", attrsMap)
	s.writeEvent("credit", futureDt, "0.666", attrsMap)
	s.updateEventSeqID(2)
	s.updateRollups()

	// Проверяем, что старый остаток не изменился
	s.checkBalance(attrsMap, s.getNextDate(onDt), 13.0, 13.0)
	// Проверяем новый остаток
	s.checkBalance(attrsMap, s.getNextDate(futureDt), 13.666, 13.666)
}

func (s *RollupTestSuite) TestEventSeqIDPrefix() {
	onDt := time.Now().UTC()
	attrsMap := genAttrMap()
	accountID := s.createAccount(mainAttrs(attrsMap))
	s.writeEvent("debit", onDt, "1", attrsMap)
	s.updateEventSeqID(1)
	query := sq.Select("seq_id").
		From("acc.t_event").
		Where("account_id = ?", accountID)
	shard, err := s.ctx.Shards.GetLastShard()
	s.Require().NoError(err)
	row, err := shard.GetDatabase(s.ctx, pg.Master).QueryRowSq(s.ctx, query)
	s.Require().NoError(err)
	var eventSeqID int64
	err = row.Scan(&eventSeqID)
	s.Require().NoError(err)
	s.checkEntityID(eventSeqID, shard)
}

// Кейс второй - предположим, что есть остаток на какое-то число, и мы обрабатываем события до этого числа,
// они должны сгруппироваться и породить новые
func (s *RollupTestSuite) TestPastEventsInsideRollup() {
	attrsMap := genAttrMap()
	onDt := time.Now().UTC()
	accountInsertedID := s.createAccount(mainAttrs(attrsMap))

	// сначала запишем событие через пару дней
	futureDt := onDt.Add(2 * s.aggregateInterval)
	s.writeEvent("debit", futureDt, "1", attrsMap)
	s.updateEventSeqID(1)
	s.updateRollups()
	s.checkBalance(attrsMap, s.getNextDate(futureDt), 1.0, 0.0)

	// Теперь запишем два события, за сегодня и завтра - они должны попасть в агрегат, порожденный исходным событием
	s.writeEvent("debit", onDt, "1", attrsMap)
	s.writeEvent("debit", onDt.Add(s.aggregateInterval), "1", attrsMap)
	s.updateEventSeqID(2)

	// Такой способ проверки вызван тем, что получение баланса для предыдущих дат выдаст данные о событиях,
	// поэтому стоит проверить напрямую, что данный кусок лога подразумевает запись в уже имеющийся агрегат
	rollupAccount := s.getRollupAccount(accountInsertedID)
	amounts := make(map[time.Time]float64)
	amounts[s.getNextDate(onDt)] = 1.0
	amounts[s.getNextDate(onDt.Add(s.aggregateInterval))] = 2.0
	amounts[s.getNextDate(futureDt)] = 3.0
	s.checkRollups(rollupAccount, amounts)

	s.updateRollups()
	s.checkBalance(attrsMap, s.getNextDate(onDt), 1.0, 0.0)
	s.checkBalance(attrsMap, s.getNextDate(onDt.Add(s.aggregateInterval)), 2.0, 0.0)
	s.checkBalance(attrsMap, s.getNextDate(futureDt), 3.0, 0.0)
}

// Кейс второй с костылём - предположим, что есть остаток на какое-то число, и мы обрабатываем события до этого числа,
// они должны сгруппироваться и не порождать новый остаток, а влиться в новый. Костыль гарантирует это поведение
func (s *RollupTestSuite) TestMagicCrutchOldBehavior() {
	oldMagicCrutch := rollup.MagicCrutchDate
	defer func() { rollup.MagicCrutchDate = oldMagicCrutch }()

	attrsMap := genAttrMap()
	onDt := time.Now().UTC()
	rollup.MagicCrutchDate = onDt.Add(s.aggregateInterval)

	accountInsertedID := s.createAccount(mainAttrs(attrsMap))

	// сначала запишем событие через два дня
	futureDt := onDt.Add(2 * s.aggregateInterval)
	s.writeEvent("debit", futureDt, "1", attrsMap)
	s.updateEventSeqID(1)
	s.updateRollups()
	s.checkBalance(attrsMap, s.getNextDate(futureDt), 1.0, 0.0)

	// Теперь запишем два события, за сегодня и завтра - они должны попасть в агрегат, порожденный исходным событием
	// и создать лишь один новый роллап - тот, который начинается с даты костыля
	s.writeEvent("debit", onDt, "1", attrsMap)
	s.writeEvent("debit", onDt.Add(s.aggregateInterval), "1", attrsMap)
	s.updateEventSeqID(2)

	// Такой способ проверки вызван тем, что получение баланса для предыдущих дат выдаст данные о событиях,
	// поэтому стоит проверить напрямую, что данный кусок лога подразумевает запись в уже имеющийся агрегат
	rollupAccount := s.getRollupAccount(accountInsertedID)
	amounts := make(map[time.Time]float64)
	amounts[s.getNextDate(onDt.Add(s.aggregateInterval))] = 2.0
	amounts[s.getNextDate(futureDt)] = 3.0
	s.checkRollups(rollupAccount, amounts)

	s.updateRollups()
	s.checkBalance(attrsMap, s.getNextDate(onDt.Add(s.aggregateInterval)), 2.0, 0.0)
	s.checkBalance(attrsMap, s.getNextDate(futureDt), 3.0, 0.0)
}

func (s *RollupTestSuite) checkRollups(rollupAccount *entities.RollupAccount, amounts map[time.Time]float64) {
	datesOccurred := make(map[time.Time]bool)
	for _, rollup := range rollupAccount.Rollups {
		credit, err := strconv.ParseFloat(rollup.Credit, 64)
		s.Require().NoError(err)
		s.Assert().Equal(0.0, credit)

		if amount, found := amounts[rollup.Dt]; !found {
			s.Assert().Fail("unexpected dt", "account dt %s", rollup.Dt)
		} else {
			debit, err := strconv.ParseFloat(rollup.Debit, 64)
			s.Require().NoError(err)
			s.Assert().Equal(amount, debit)
		}

		datesOccurred[rollup.Dt] = true
	}
	s.Assert().Equal(len(amounts), len(datesOccurred), "actual dates %v", datesOccurred)
}

func (s *RollupTestSuite) checkBalance(attrsMap map[string]*string, dt time.Time, debit float64, credit float64) {
	balances, err := s.ctx.Actions.GetBalance(s.ctx, dt, s.map2attrs(attrsMap))
	s.Require().NoError(err)

	s.Assert().Equal(1, len(balances), "the actual balances are %v", balances)
	balance := balances[0]

	debitActual, err := strconv.ParseFloat(balance.Debit, 64)
	s.Require().NoError(err)
	s.Assert().Equal(debit, debitActual, "debit mismatch")

	creditActual, err := strconv.ParseFloat(balance.Credit, 64)
	s.Require().NoError(err)
	s.Assert().Equal(credit, creditActual, "credit mismatch")
}

// А вот это интересный кейс. Нет никаких остатков, и пришло два события - за вчера, и за завтра.
// логика такова, что создается два остатка - за сегодня, и послезавтра
func (s *RollupTestSuite) TestNewEventsWithoutRollups() {
	attrsMap := genAttrMap()
	accountInsertedID := s.createAccount(mainAttrs(attrsMap))
	firstDt := time.Now().UTC().Add(-s.aggregateInterval)
	firstDay := s.getNextDate(firstDt)
	secondDt := firstDt.Add(2 * s.aggregateInterval)
	thirdDay := s.getNextDate(secondDt)

	s.writeEvent("debit", firstDt, "1", attrsMap)
	s.writeEvent("debit", secondDt, "2", attrsMap)
	s.updateEventSeqID(2)

	// Для начала проверим напрямую, соответствует ли реальный набор обновлений ожидаемому, без применения
	rollupAccount := s.getRollupAccount(accountInsertedID)
	amounts := make(map[time.Time]float64)
	amounts[firstDay] = 1.0
	amounts[thirdDay] = 3.0
	s.checkRollups(rollupAccount, amounts)

	s.updateRollups()

	s.checkBalance(attrsMap, firstDay, 1.0, 0.0)
	s.checkBalance(attrsMap, thirdDay, 3.0, 0.0)
}

// Собственно, кейс, который был указан в обзоре. Есть три остатка на три дня подряд. Приходит два события:
// на первый день и третий. Первое событие должно увеличить остатки второго и третьего дня,
// второе событие должно породить остаток на 4 день, включая остаток третьего дня и первое событие
func (s *RollupTestSuite) TestMix() {
	attrsMap := genAttrMap()
	zeroDt := time.Now().UTC()
	rollup.TimeNowFunc = func() time.Time {
		return time.Now().UTC()
	}
	defer func() {
		rollup.TimeNowFunc = func() time.Time {
			return time.Now()
		}
	}()

	firstDt := zeroDt.Add(s.aggregateInterval)
	secondDt := firstDt.Add(s.aggregateInterval)
	thirdDt := secondDt.Add(s.aggregateInterval)
	firstDay := s.getNextDate(zeroDt)
	secondDay := s.getNextDate(firstDt)
	thirdDay := s.getNextDate(secondDt)
	fourthDay := s.getNextDate(thirdDt)

	accountInsertedID := s.createAccount(mainAttrs(attrsMap))

	// Чтобы создать остатки первого, второго и третьего дня,
	// нужно соответственно делать события нулевого, первого и второго
	s.writeEvent("debit", zeroDt, "1", attrsMap)
	s.writeEvent("debit", firstDt, "1", attrsMap)
	s.writeEvent("debit", secondDt, "1", attrsMap)
	s.updateEventSeqID(3)

	s.updateRollups()

	s.checkBalance(attrsMap, firstDay, 1.0, 0.0)
	s.checkBalance(attrsMap, secondDay, 2.0, 0.0)
	s.checkBalance(attrsMap, thirdDay, 3.0, 0.0)

	s.writeEvent("debit", firstDt, "1", attrsMap)
	s.writeEvent("debit", thirdDt, "1", attrsMap)
	s.updateEventSeqID(2)

	// Для начала проверим напрямую, соответствует ли реальный набор обновлений ожидаемому, без применения
	rollupAccount := s.getRollupAccount(accountInsertedID)

	amounts := make(map[time.Time]float64)

	amounts[secondDay] = 3.0 // Событие первого дня должно привести к увеличению остатка второго
	amounts[thirdDay] = 4.0  // И это же событие должно привести к увеличению остатка третьего
	amounts[fourthDay] = 5.0 // новый остаток состоит из остатка третьего дня и двух остатков

	s.checkRollups(rollupAccount, amounts)
	s.updateRollups()
	// А вот теперь проверяем через API, что все так
	s.checkBalance(attrsMap, firstDay, 1.0, 0.0)
	s.checkBalance(attrsMap, secondDay, 3.0, 0.0)
	s.checkBalance(attrsMap, thirdDay, 4.0, 0.0)
	s.checkBalance(attrsMap, fourthDay, 5.0, 0.0)
}

func (s *RollupTestSuite) TestRollupOfLockedShard() {
	ctrl := gomock.NewController(s.T())

	metricsServiceMock := actionsmock.NewMockRollupMetricsService(ctrl)

	shard, err := s.ctx.Shards.GetLastShard()
	s.Require().NoError(err)

	infoChannel := make(chan bool)
	defer close(infoChannel)
	errChannel := make(chan error)

	storage := shard.GetRollupStorage(s.ctx.Templates)
	storage.GetAdvisoryLockInBackground(s.ctx, s.RollupConfig, infoChannel, errChannel)

	result := <-infoChannel
	s.Require().True(result)

	attrsMap := genAttrMap()
	currTime := time.Now().UTC()

	accountID := s.createAccount(mainAttrs(attrsMap))
	s.writeEvent("debit", currTime, "1", attrsMap)

	s.updateEventSeqID(1)
	events := s.getEvents(accountID)

	_, beforeFutureID, err := storage.GetRollupBounds(s.ctx)
	s.Require().NoError(err)

	s.Assert().Equal(1, len(events))
	s.Assert().Greater(events[0].SeqID, beforeFutureID)

	metricsServiceMock.EXPECT().UpdateRollupsFailed()
	err = s.updateRollupsExplicitError(rollupWithMetrics(metricsServiceMock))
	s.Require().NoError(err)

	_, afterFutureID, err := storage.GetRollupBounds(s.ctx)
	s.Require().NoError(err)

	s.Assert().Equal(beforeFutureID, afterFutureID)
}

func (s *RollupTestSuite) TestZeroEventCreationSubaccount() {
	a1 := btesting.RandSP(100)
	a2 := btesting.RandSP(100)
	a3 := btesting.RandSP(100)
	Nil := ""
	accountInsertedID := s.createAccount([]string{*a1, *a2})
	s.writeEvent("debit", time.Now(), "0", map[string]*string{"a1": a1, "a2": a2, "a3": a3, "a4": &Nil})

	s.updateEventSeqID(1)
	s.updateRollups()

	subA3ID := s.getSubaccountID(accountInsertedID, *a3, Nil)
	subNoA3ID := s.getSubaccountID(accountInsertedID, Nil, Nil)

	// проверяем, что создаются аккаунт и сабаккаунты при нулевом event
	rollups := s.getRollups(accountInsertedID)
	count, err := strconv.ParseFloat(rollups[0].Debit, 64)
	s.Require().NoError(err)
	s.Require().Equal(int(count), 0)
	s.requireSubIDCount(subA3ID, 1, rollups)
	s.requireSubIDCount(subNoA3ID, 1, rollups)
}

// TestRollupsForSubaccounts проверяет, что не создаются rollup для subaccount в случае, если для него не было event
func (s *RollupTestSuite) TestRollupsForSubaccounts() {
	dt := time.Now().UTC()
	duration := s.aggregateInterval
	dtMinus2Days := dt.Add(-duration * 48)
	dtMinusDay := dtMinus2Days.Add(duration * 24)
	a1 := btesting.RandSP(100)
	a2 := btesting.RandSP(100)
	a3 := btesting.RandSP(100)
	Nil := ""

	accountInsertedID := s.createAccount([]string{*a1, *a2})

	saveNowFunc := rollup.TimeNowFunc
	defer func() { rollup.TimeNowFunc = saveNowFunc }()
	rollup.TimeNowFunc = func() time.Time { return dt.Add(-duration * 24) }

	// добавляем эвент
	s.writeEvent("debit", dtMinus2Days, "128", map[string]*string{"a1": a1, "a2": a2, "a3": a3, "a4": &Nil})
	s.updateEventSeqID(1)
	s.updateRollups()

	subA3ID := s.getSubaccountID(accountInsertedID, *a3, Nil)
	subNoA3ID := s.getSubaccountID(accountInsertedID, Nil, Nil)

	// проверяем, что создались роллапы, корректность значения debit
	// should be event dt -2days, rollup dt -2days
	rollups := s.getRollups(accountInsertedID)
	s.Require().NotEmpty(rollups)
	count, err := strconv.ParseFloat(rollups[0].Debit, 64)
	s.Require().NoError(err)
	s.Require().Equal(int(count), 128)
	s.requireSubIDCount(subA3ID, 1, rollups)
	s.requireSubIDCount(subNoA3ID, 1, rollups)

	rollup.TimeNowFunc = func() time.Time { return dt.Add(-duration * 22) }

	// добавляем эвент для одного сабаккаунта (attribute_3, он же attribute_1 в сабаккаунте == "")
	s.writeEvent("debit", dtMinusDay, "784", map[string]*string{"a1": a1, "a2": a2, "a3": &Nil, "a4": &Nil})
	s.updateEventSeqID(1)
	s.updateRollups()

	// new rollup should have -1day dt.
	rollups = s.getRollups(accountInsertedID)
	s.requireSubIDCount(subA3ID, 1, rollups)
	s.requireSubIDCount(subNoA3ID, 2, rollups)

	rollup.TimeNowFunc = func() time.Time { return dt.Add(-duration * 20) }
	// добавляем нулевой эвент для одного сабаккаунта
	s.writeEvent("debit", dt, "0", map[string]*string{"a1": a1, "a2": a2, "a3": &Nil, "a4": &Nil})
	s.updateEventSeqID(1)
	s.updateRollups()

	// проверяем так же, как и для ненулевого эвента
	// another rollup is created for current dt
	rollups = s.getRollups(accountInsertedID)
	s.requireSubIDCount(subA3ID, 1, rollups)
	s.requireSubIDCount(subNoA3ID, 3, rollups)

	s.updateRollups()
	// проверяем, что ничего не изменилось без эвентов
	rollups = s.getRollups(accountInsertedID)
	s.requireSubIDCount(subA3ID, 1, rollups)
	s.requireSubIDCount(subNoA3ID, 3, rollups)

	hour := dt.Add(-duration * 18).Truncate(duration)
	hourMinusSecond := hour.Add(-time.Second)
	rollup.TimeNowFunc = func() time.Time { return hourMinusSecond }
	s.writeEvent("debit", hourMinusSecond, "0", map[string]*string{"a1": a1, "a2": a2, "a3": a3, "a4": &Nil})
	s.updateEventSeqID(1)
	s.updateRollups()

	hourPlusSecond := hour.Add(time.Second)
	rollup.TimeNowFunc = func() time.Time { return hourPlusSecond }
	s.writeEvent("debit", dt, "0", map[string]*string{"a1": a1, "a2": a2, "a3": &Nil, "a4": &Nil})
	s.updateEventSeqID(1)
	s.updateRollups()

	// проверяем, что на границе часа +1сек создастся роллап на следующий час
	rollups = s.getRollups(accountInsertedID)
	s.requireSubIDCount(subA3ID, 2, rollups)
	s.requireSubIDCount(subNoA3ID, 4, rollups)

	hourPlus2Seconds := hour.Add(2 * time.Second)
	rollup.TimeNowFunc = func() time.Time { return hourPlus2Seconds }
	s.writeEvent("debit", dt, "0", map[string]*string{"a1": a1, "a2": a2, "a3": &Nil, "a4": &Nil})
	s.updateEventSeqID(1)
	s.updateRollups()

	// убеждаемся, что час +2сек не добавляет ещё одного роллапа при имеющемся от +1сек
	rollups = s.getRollups(accountInsertedID)
	s.requireSubIDCount(subA3ID, 2, rollups)
	s.requireSubIDCount(subNoA3ID, 4, rollups)

	// создаём роллап на hour+1ч
	rollup.TimeNowFunc = func() time.Time { return hourPlusSecond }
	s.writeEvent("debit", hourPlusSecond, "10000", map[string]*string{"a1": a1, "a2": a2, "a3": a3, "a4": &Nil})
	s.writeEvent("credit", hourPlusSecond, "20000", map[string]*string{"a1": a1, "a2": a2, "a3": a3, "a4": &Nil})
	s.updateEventSeqID(2)
	s.updateRollups()
	rollups = s.getRollups(accountInsertedID)

	hourPlusOne := hour.Add(duration)
	var originDebit, originCredit string
	for _, rollup := range rollups {
		if rollup.Dt == hourPlusOne && rollup.SubaccountID == subA3ID {
			originDebit = rollup.Debit // запоминаем значение
			originCredit = rollup.Credit
		}
	}

	// инициируем апдейт
	rollup.TimeNowFunc = func() time.Time { return hour }
	s.writeEvent("debit", dt.Add(duration), "0", map[string]*string{"a1": a1, "a2": a2, "a3": &Nil, "a4": &Nil})
	s.updateEventSeqID(1)
	s.updateRollups()
	rollups = s.getRollups(accountInsertedID)

	rollupFound := false
	for _, rollup := range rollups {
		if rollup.Dt == hourPlusOne && rollup.SubaccountID == subA3ID {
			s.Require().Equal(rollup.Debit, originDebit) // проверяем, что поле не изменилось
			s.Require().Equal(rollup.Credit, originCredit)
			rollupFound = true
		}
	}
	s.Require().True(rollupFound)
}

func (s *RollupTestSuite) requireSubIDCount(subaccountID int64, count int, rollups []entities.RollupSubaccount) {
	cnt := 0
	for _, rollup := range rollups {
		if rollup.SubaccountID == subaccountID {
			cnt++
		}
	}
	s.Require().Equal(count, cnt)
}

func (s *RollupTestSuite) getSubaccountID(subaccountID int64, a3, a4 string) (id int64) {
	shard, err := s.ctx.Shards.GetLastShard()
	s.Require().NoError(err)

	row := shard.GetDatabase(s.ctx, pg.MasterPrefered).QueryRowxContext(s.ctx, `select id from acc.t_subaccount
		where account_id=$1 and attribute_1=$2 and attribute_2=$3`,
		subaccountID, a3, a4)
	s.Require().NoError(row.Scan(&id))
	return
}

func (s *RollupTestSuite) getRollups(accountID int64) []entities.RollupSubaccount {
	shard, err := s.ctx.Shards.GetLastShard()
	s.Require().NoError(err)

	rows, err := shard.GetDatabase(s.ctx, pg.MasterPrefered).QueryxContext(s.ctx, `select * from acc.t_subaccount_rollup
		where subaccount_id in (select id from acc.t_subaccount where account_id=$1)`, accountID)
	s.Require().NoError(err)
	//goland:noinspection GoUnhandledErrorResult
	defer rows.Close()

	var res []entities.RollupSubaccount
	for rows.Next() {
		mRes := entities.RollupSubaccount{}
		err = rows.Scan(&mRes.SubaccountID, &mRes.Dt, &mRes.Debit, &mRes.Credit)
		s.Require().NoError(err)
		res = append(res, mRes)
	}
	s.Require().NoError(rows.Err())
	return res
}

func (s *RollupTestSuite) testRollup(rollup entities.RollupSubaccount, debit, credit int, dt time.Time) {
	d, err := strconv.ParseFloat(rollup.Debit, 64)
	s.Require().NoError(err)
	s.Require().Equal(debit, int(d))
	c, err := strconv.ParseFloat(rollup.Credit, 64)
	s.Require().NoError(err)
	s.Require().Equal(credit, int(c))
	s.Require().Equal(dt, rollup.Dt)
}

func (s *RollupTestSuite) initOldRollups() (accountInsertedID int64, from time.Time, shard storage.Shard) {
	shard, err := s.ctx.Shards.GetLastShard()
	s.Require().NoError(err)

	tableNames, err := shard.GetPartitioningStorage().GetTablePartitions(s.ctx, "event")
	s.Require().NoError(err)
	s.Require().NotEmpty(tableNames)

	slices.Sort(tableNames)

	// далее идёт частичное дублирование функционала отсюда
	// https://a.yandex-team.ru/arc_vcs/billing/hot/accounts/pkg/core/actions/impl/partmaint.go?rev=46d0176f5c#L108
	splittedOldestPartitionName := strings.Split(tableNames[0], "_")
	// формат либо tp_имя_дата либо tp_имя_дата1_дата2
	// нам нужен в любом случае третий элемент
	s.Require().GreaterOrEqual(len(splittedOldestPartitionName), 3)

	from, err = time.Parse(corepart.PartitionNameDateFormat, splittedOldestPartitionName[2])
	s.Require().NoError(err)

	a1 := btesting.RandSP(100)
	a2 := btesting.RandSP(100)
	Nil := ""

	accountInsertedID = s.createAccount([]string{*a1, *a2})

	// добавляем эвент, чтобы создать один сабаккаунт
	s.writeEvent("debit", time.Now(), "128", map[string]*string{"a1": a1, "a2": a2, "a3": &Nil, "a4": &Nil})

	return
}

// TestRemoveOldRollups проверка, что остаётся только последний роллап из тех, которые вне партиций
func (s *RollupTestSuite) TestRemoveOldRollups() {
	accountInsertedID, from, shard := s.initOldRollups()
	Nil := ""

	// добавдляем три роллапа до первой партиции эвентов
	s.createRollup(accountInsertedID, Nil, Nil, 10, 0, from.Add(-time.Hour*24))
	s.createRollup(accountInsertedID, Nil, Nil, 20, 0, from.Add(-time.Hour*23))
	savingTime := from.Add(-time.Hour * 22)
	s.createRollup(accountInsertedID, Nil, Nil, 30, 0, savingTime)
	// проверяем корректность добавления
	s.Require().Equal(3, len(s.getRollups(accountInsertedID)))

	// запускаем проверяемый функционал
	s.Require().NoError(shard.GetPartitioningStorage().DropOldRollups(s.ctx, from))

	// проверяем корректность оставшегося роллапа
	rollups := s.getRollups(accountInsertedID)
	s.Require().Equal(1, len(rollups))
	s.testRollup(rollups[0], 30, 0, savingTime)

	// проверяем пограничный роллап
	s.createRollup(accountInsertedID, Nil, Nil, 40, 0, from)
	s.Require().NoError(shard.GetPartitioningStorage().DropOldRollups(s.ctx, from))
	rollups = s.getRollups(accountInsertedID)
	s.Require().Equal(1, len(rollups))
}

// TestSaveBorderlineRollup проверка того, что сохраняется роллап, который на начале самой ранней партиции
func (s *RollupTestSuite) TestSaveBorderlineRollup() {
	accountInsertedID, from, shard := s.initOldRollups()
	Nil := ""

	s.createRollup(accountInsertedID, Nil, Nil, 10, 0, from.Add(-time.Hour*24))
	s.createRollup(accountInsertedID, Nil, Nil, 40, 0, from)
	s.Require().NoError(shard.GetPartitioningStorage().DropOldRollups(s.ctx, from))
	rollups := s.getRollups(accountInsertedID)
	s.Require().Equal(1, len(rollups))
	s.testRollup(rollups[0], 40, 0, from)
}

// TestSavePartitionRollups проверка того, роллапы при существующих партициях не удаляются
func (s *RollupTestSuite) TestSavePartitionRollups() {
	accountInsertedID, from, shard := s.initOldRollups()
	Nil := ""

	now := time.Now()
	s.createRollup(accountInsertedID, Nil, Nil, 20, 0, now.Add(-time.Hour*2))
	s.createRollup(accountInsertedID, Nil, Nil, 30, 0, now.Add(-time.Hour))
	s.createRollup(accountInsertedID, Nil, Nil, 40, 0, now)

	s.Require().NoError(shard.GetPartitioningStorage().DropOldRollups(s.ctx, from))
	rollups := s.getRollups(accountInsertedID)
	s.Require().Equal(3, len(rollups))

	// также добавляем роллап на границе
	s.createRollup(accountInsertedID, Nil, Nil, 10, 0, from)
	s.Require().NoError(shard.GetPartitioningStorage().DropOldRollups(s.ctx, from))
	rollups = s.getRollups(accountInsertedID)
	s.Require().Equal(4, len(rollups)) // и проверяем, что он не удалён
}

func (s *RollupTestSuite) createRollup(accountID int64, a3, a4 string, debit, credit float64, dt time.Time) {
	subaccountID := s.getSubaccountID(accountID, a3, a4)
	shard, err := s.ctx.Shards.GetLastShard()
	s.Require().NoError(err)

	res, err := shard.GetDatabase(s.ctx, pg.MasterPrefered).ExecContext(s.ctx, `
insert into acc.t_subaccount_rollup values ($1,$2,$3,$4)`, subaccountID, dt, debit, credit)
	s.Require().NoError(err)
	affected, err := res.RowsAffected()
	s.Require().NoError(err)
	s.Require().Equal(affected, int64(1))
}

type nonPeriodicRollupCheck struct {
	subaccountID *int64
	dt           *time.Time
	debit        *string
	credit       *string
}

func (s *RollupTestSuite) checkNonPeriodicRollupsForAccount(
	rollups *entities.RollupAccount,
	checks []nonPeriodicRollupCheck,
) {
	if rollups == nil {
		s.Require().Len(checks, 0)
		return
	}
	s.Require().Equal(len(checks), len(rollups.Rollups))
	slices.SortFunc(rollups.Rollups, func(l, r entities.RollupSubaccount) bool {
		if l.Dt.Before(r.Dt) {
			return true
		}
		if l.Dt.After(r.Dt) {
			return false
		}
		return l.SubaccountID < r.SubaccountID
	})

	for i, r := range rollups.Rollups {
		check := checks[i]

		if check.subaccountID != nil {
			s.Assert().Equal(*check.subaccountID, r.SubaccountID)
		}
		if check.dt != nil {
			s.Assert().Equal(*check.dt, r.Dt)
		}
		if check.debit != nil {
			s.Assert().Equal(*check.debit, r.Debit)
		}
		if check.credit != nil {
			s.Assert().Equal(*check.credit, r.Credit)
		}
	}
}

func stringToPtr(v string) *string {
	return &v
}

func timeToPtr(t time.Time) *time.Time {
	return &t
}

func (s *RollupTestSuite) TestGetRollupsWithNotRollupTimes() {
	// random account/subaccount attributes
	attrsMap := genAttrMap()
	baseDt := time.Now().UTC().Truncate(time.Hour * 24).Add(time.Hour * 15)

	// substitute time.Now for known value without interface interference
	rollup.TimeNowFunc = func() time.Time {
		return time.Now().UTC().Truncate(time.Hour * 24).Add(time.Hour * 15).Add(time.Minute * 2)
	}
	defer func() {
		rollup.TimeNowFunc = func() time.Time {
			return time.Now()
		}
	}()

	// this account should have 15 minute cron
	accountInsertedID := s.createAccountWithLocationAttributes("rt", "rt", mainAttrs(attrsMap))

	// set attrs so that events correspond to a single subaccount
	emptyString := ""
	attrsMap["a4"] = &emptyString
	loc := entities.LocationAttributes{
		Namespace:  "rt",
		Type:       "rt",
		Attributes: attrsMap,
	}

	// 15:00 - 15:15
	s.writeEventWithLocationAttributes("debit", baseDt.Add(time.Minute), "1", loc)
	s.writeEventWithLocationAttributes("debit", baseDt.Add(time.Minute*2), "1", loc)
	s.writeEventWithLocationAttributes("debit", baseDt.Add(time.Minute*3), "1", loc)

	// 15:15 - 15:30
	s.writeEventWithLocationAttributes("debit", baseDt.Add(time.Minute*15).Add(time.Minute), "1", loc)
	s.writeEventWithLocationAttributes("debit", baseDt.Add(time.Minute*15).Add(time.Minute*2), "1", loc)
	s.writeEventWithLocationAttributes("debit", baseDt.Add(time.Minute*15).Add(time.Minute*3), "1", loc)

	// 15:30 - 15:45
	s.writeEventWithLocationAttributes("debit", baseDt.Add(time.Minute*15*2).Add(time.Minute), "1", loc)
	s.writeEventWithLocationAttributes("debit", baseDt.Add(time.Minute*15*2).Add(time.Minute*2), "1", loc)
	s.writeEventWithLocationAttributes("debit", baseDt.Add(time.Minute*15*2).Add(time.Minute*3), "1", loc)

	s.updateEventSeqID(9)

	// last rollup should be 15:30 - 15:45
	// last rollup_time should be 15:45 - 16:00

	// check that rollup query fails if rollup_times are broken(not consistently with events min-max dt)
	_, err := s.getRollupAccountRaw(accountInsertedID)
	s.Require().Error(err)

	// check rollups without updating them
	s.checkNonPeriodicRollupsForAccount(
		s.getRollupAccount(accountInsertedID),
		[]nonPeriodicRollupCheck{
			{
				dt:    timeToPtr(baseDt.Add(time.Minute * 15)),
				debit: stringToPtr("3.000000"),
			},
			{
				dt:    timeToPtr(baseDt.Add(time.Minute * 15 * 2)),
				debit: stringToPtr("6.000000"),
			},
			{
				dt:    timeToPtr(baseDt.Add(time.Minute * 15 * 3)),
				debit: stringToPtr("9.000000"),
			},
		},
	)

	// update rollups
	s.updateRollups()

	// 15:15 - 15:30
	s.writeEventWithLocationAttributes("debit", baseDt.Add(time.Minute*15).Add(time.Minute*4), "1", loc)
	s.writeEventWithLocationAttributes("debit", baseDt.Add(time.Minute*15).Add(time.Minute*5), "1", loc)
	s.writeEventWithLocationAttributes("debit", baseDt.Add(time.Minute*15).Add(time.Minute*6), "1", loc)

	// 16:00 - 16:15
	s.writeEventWithLocationAttributes("debit", baseDt.Add(time.Minute*15*9).Add(time.Minute), "1", loc)
	s.writeEventWithLocationAttributes("debit", baseDt.Add(time.Minute*15*9).Add(time.Minute*2), "1", loc)
	s.writeEventWithLocationAttributes("debit", baseDt.Add(time.Minute*15*9).Add(time.Minute*3), "1", loc)

	s.updateEventSeqID(6)

	// last rollup should be 17:15 - 17:30
	// last rollup_time should be 17:30 - 17:45

	// check that rollup query fails if rollup_times are broken(not consistently with events min-max dt)
	_, err = s.getRollupAccountRaw(accountInsertedID)
	s.Require().Error(err)

	// check rollups without updating them
	s.checkNonPeriodicRollupsForAccount(
		s.getRollupAccount(accountInsertedID),
		[]nonPeriodicRollupCheck{
			{
				dt:    timeToPtr(baseDt.Add(time.Minute * 15 * 2)),
				debit: stringToPtr("9.000000"),
			},
			{
				dt:    timeToPtr(baseDt.Add(time.Minute * 15 * 3)),
				debit: stringToPtr("12.000000"),
			},
			{
				dt:    timeToPtr(baseDt.Add(time.Minute * 15 * 10)),
				debit: stringToPtr("15.000000"),
			},
		},
	)

	// update rollups
	s.updateRollups()

	// check rollup_times are consistent
	rs, err := s.getRollupAccountRaw(accountInsertedID)
	s.Require().NoError(err)

	// check rollups
	s.checkNonPeriodicRollupsForAccount(
		rs,
		[]nonPeriodicRollupCheck{},
	)

	// 14:00 - 14:15
	s.writeEventWithLocationAttributes("debit", baseDt.Add(-time.Minute*15*4).Add(time.Minute*1), "1", loc)
	s.writeEventWithLocationAttributes("debit", baseDt.Add(-time.Minute*15*4).Add(time.Minute*2), "1", loc)
	s.writeEventWithLocationAttributes("debit", baseDt.Add(-time.Minute*15*4).Add(time.Minute*3), "1", loc)

	s.updateEventSeqID(3)

	// check rollups without updating them
	s.checkNonPeriodicRollupsForAccount(
		s.getRollupAccount(accountInsertedID),
		[]nonPeriodicRollupCheck{
			{
				dt:    timeToPtr(baseDt.Add(-time.Minute * 15 * 3)),
				debit: stringToPtr("3.000000"),
			},
			{
				dt:    timeToPtr(baseDt.Add(time.Minute * 15)),
				debit: stringToPtr("6.000000"),
			},
			{
				dt:    timeToPtr(baseDt.Add(time.Minute * 15 * 2)),
				debit: stringToPtr("12.000000"),
			},
			{
				dt:    timeToPtr(baseDt.Add(time.Minute * 15 * 3)),
				debit: stringToPtr("15.000000"),
			},
			{
				dt:    timeToPtr(baseDt.Add(time.Minute * 15 * 10)),
				debit: stringToPtr("18.000000"),
			},
		},
	)

	// update rollups and check that rollup_times are created before previous earliest rollup(should not error)
	s.updateRollups()
}
