package payout

import (
	"database/sql"
	"time"

	"github.com/shopspring/decimal"
	"github.com/stretchr/testify/require"

	"a.yandex-team.ru/billing/hot/payout/internal/core"
	bt "a.yandex-team.ru/billing/library/go/billingo/pkg/testing"
	"a.yandex-team.ru/library/go/units"
)

func getRandomPayout() *NewPayout {
	return &NewPayout{
		ExternalID: bt.RandS(30),
		ServiceID:  124,
		ContractID: bt.RandN64(),
		Amount:     decimal.RequireFromString("124.666"),
		Currency:   "RUB",
	}
}

func (s *PayoutTestSuite) TestCreate() {
	np := getRandomPayout()
	p, err := Create(s.ctx, np)
	require.NoError(s.T(), err)
	require.Equal(s.T(), StatusNew, p.Status)

	pn, err := Get(s.ctx, p.ID)
	require.NoError(s.T(), err)
	require.Equal(s.T(), StatusNew, pn.Status)

	// такую же выплату сделать нельзя
	_, err = Create(s.ctx, np)
	require.Error(s.T(), err)
}

func (s *PayoutTestSuite) TestCreateTx() {
	tx, err := s.ctx.Storage.Backend.BeginTx(s.ctx)
	require.NoError(s.T(), err)

	np := getRandomPayout()
	p, err := CreateTx(s.ctx, tx, np)
	require.NoError(s.T(), err)
	require.Equal(s.T(), StatusNew, p.Status)

	_, err = Get(s.ctx, p.ID)
	require.Error(s.T(), err)
	require.Equal(s.T(), sql.ErrNoRows, err)

	require.NoError(s.T(), tx.Commit())

	pn, err := Get(s.ctx, p.ID)
	require.NoError(s.T(), err)
	require.Equal(s.T(), StatusNew, pn.Status)

	// такую же выплату сделать нельзя
	_, err = CreateTx(s.ctx, tx, np)
	require.Error(s.T(), err)
}

func (s *PayoutTestSuite) TestGetByServiceEID() {
	p, err := Create(s.ctx, getRandomPayout())
	require.NoError(s.T(), err)

	pn, err := GetByServiceEID(s.ctx, p.ServiceID, p.ExternalID)
	require.NoError(s.T(), err)
	require.Equal(s.T(), StatusNew, pn.Status)
	require.Equal(s.T(), p.ID, pn.ID)

	pn, err = GetByServiceEID(s.ctx, -1, "-1")
	require.Nil(s.T(), pn)
	require.NoError(s.T(), err)
}

func (s *PayoutTestSuite) TestFail() {
	p, err := Create(s.ctx, getRandomPayout())
	require.NoError(s.T(), err)

	tx, err := s.ctx.Storage.Backend.BeginTx(s.ctx)
	require.NoError(s.T(), err)

	reason := "we broke it, sorry"
	require.NoError(s.T(), Fail(s.ctx, tx, p.ID, p.CreateDt, reason))

	pn, err := Get(s.ctx, p.ID)
	require.NoError(s.T(), err)
	require.Equal(s.T(), StatusNew, pn.Status)

	require.NoError(s.T(), tx.Commit())

	pn, err = Get(s.ctx, p.ID)
	require.NoError(s.T(), err)
	require.Equal(s.T(), StatusRejected, pn.Status)
	require.NotNil(s.T(), pn.Error)
	require.Equal(s.T(), reason, *pn.Error)
}

func (s *PayoutTestSuite) TestGetNew() {
	_, err := Create(s.ctx, getRandomPayout())
	require.NoError(s.T(), err)

	payouts, tx, err := GetNew(s.ctx, 0)
	require.NoError(s.T(), err)
	require.Len(s.T(), payouts, 0)
	require.NoError(s.T(), tx.Rollback())

	payouts, tx, err = GetNew(s.ctx, 1)
	require.NoError(s.T(), err)
	require.Len(s.T(), payouts, 1)
	require.NoError(s.T(), tx.Rollback())
}

func (s *PayoutTestSuite) TestGetNewX() {
	ts := time.Now().UTC().Truncate(units.Day)
	p, err := Create(s.ctx, getRandomPayout())
	require.NoError(s.T(), err)
	require.NoError(s.T(), Update(s.ctx, p.ID, CreateDTCol, ts.AddDate(0, -1, 0)))

	pf, err := Create(s.ctx, getRandomPayout())
	require.NoError(s.T(), err)
	require.NoError(s.T(), Update(s.ctx, pf.ID, CreateDTCol, ts.AddDate(0, 0, 1)))

	payouts, tx, err := GetNewX(s.ctx, 1000, ts.AddDate(0, 0, -3), time.Now())
	require.NoError(s.T(), err)
	for _, payout := range payouts {
		if p.ID == payout.ID {
			require.Fail(s.T(), "found old payment")
		}
		if pf.ID == payout.ID {
			require.Fail(s.T(), "found future payment")
		}
	}
	require.NoError(s.T(), tx.Rollback())
}

func (s *PayoutTestSuite) TestApproveRequired() {
	ts := time.Now().UTC().Truncate(units.Day)

	newPAR := getRandomPayout()
	newPAR.ApproveRequired = true
	pAR, err := Create(s.ctx, newPAR)
	require.NoError(s.T(), err)

	payouts, tx, err := GetNewX(s.ctx, 1000, ts.AddDate(0, 0, -3), time.Now())
	require.NoError(s.T(), err)
	for _, payout := range payouts {
		require.NotEqual(s.T(), pAR.ID, payout.ID, "found approve required payout in GetNewX result")
	}
	require.NoError(s.T(), tx.Rollback())
}

func (s *PayoutTestSuite) TestGetOEBSData() {
	messageCount := 5

	oebsCheck := bt.RandN64()
	sum := decimal.Decimal{}

	// init data
	for cnt := messageCount; cnt > 0; cnt-- {
		p, err := Create(s.ctx, getRandomPayout())
		require.NoError(s.T(), err)
		err = UpdateX(s.ctx, p.ID, core.UpdateDesc{
			{Name: CheckIDCol, Value: oebsCheck},
			{Name: AmountOEBSCol, Value: p.Amount},
		})
		require.NoError(s.T(), err)
		sum = sum.Add(p.Amount)
	}

	data, err := GetOEBSCheckData(s.ctx, []int64{oebsCheck, bt.RandN64()})
	require.NoError(s.T(), err)

	value, ok := data[oebsCheck]
	require.True(s.T(), ok)
	amount, _ := sum.Float64()
	valueAmount, _ := value.TotalSum.Float64()
	require.EqualValues(s.T(), amount, valueAmount)
	require.EqualValues(s.T(), messageCount, value.MessageCount)
}

// TestDefaultDryRun проверяет, что дефолтное значение признака dry_run = true
func (s *PayoutTestSuite) TestDefaultDryRun() {
	np := NewPayout{
		ExternalID: bt.RandS(30),
		ServiceID:  222,
		ContractID: bt.RandN64(),
		Amount:     decimal.RequireFromString("13.37"),
		Currency:   "RUB",
		ClientID:   333,
		Namespace:  "taxi",
	}
	p, err := Create(s.ctx, &np)
	require.NoError(s.T(), err)

	getP, err := Get(s.ctx, p.ID)
	require.NoError(s.T(), err)
	require.True(s.T(), getP.DryRun)
}

// TestGetPrevPayoutDt проверяет логику поиска даты предыдущей выплаты
// Создаем 3 выплаты (p0, p1, p2) у которых одинаковые клиенты (в request)
// и одинаковые сервис и договор. p0, p1 подтверждены.
// Убеждаемся, что возвращается дата создания p1, как самой последней выплаты.
func (s *PayoutTestSuite) TestGetPrevPayoutDt() {
	// Если ничего не передали, то 0 на выходе
	dt, err := s.pDB.GetPrevPayoutDt(s.ctx, nil)
	require.NoError(s.T(), err)
	require.Equal(s.T(), int64(0), dt)

	p0, err := Create(s.ctx, getRandomPayout())
	require.NoError(s.T(), err)
	// т.к. точность unix timestamp - 1 секунда, делаем строчки в БД
	// с разницей в 1 секунду, чтобы можно между ними найти разницу
	time.Sleep(1 * time.Second)

	// нет ничего ранее, вернем 0
	dt, err = s.pDB.GetPrevPayoutDt(s.ctx, p0)
	require.NoError(s.T(), err)
	require.Equal(s.T(), int64(0), dt)

	p1, err := Create(s.ctx, getRandomPayout())
	require.NoError(s.T(), err)
	time.Sleep(1 * time.Second)

	// пред последняя, но не подтверждена
	p1nc, err := Create(s.ctx, getRandomPayout())
	require.NoError(s.T(), err)
	time.Sleep(1 * time.Second)

	// самая последняя, для которой будем искать пред. выплату
	p2, err := Create(s.ctx, getRandomPayout())
	require.NoError(s.T(), err)

	update := core.UpdateDesc{
		{Name: ServiceIDCol, Value: p2.ServiceID},
		{Name: ContractIDCol, Value: p2.ContractID},
		{Name: ClientIDCol, Value: p2.ClientID},
		{Name: StatusCol, Value: StatusConfirmed},
	}
	where := core.WhereDesc{
		{Name: IDCol, Value: []int64{p1.ID, p0.ID}},
	}
	// вторая выплата по тем же договору и сервису, что и первая
	require.NoError(s.T(), UpdateXX(s.ctx, where, update))

	// третья выплата по тем же договору и сервису, что и первая. но не подтверждена
	update = core.UpdateDesc{
		{Name: ServiceIDCol, Value: p2.ServiceID},
		{Name: ContractIDCol, Value: p2.ContractID},
		{Name: ClientIDCol, Value: p2.ClientID},
	}
	where = core.WhereDesc{
		{Name: IDCol, Value: []int64{p1nc.ID}},
	}
	require.NoError(s.T(), UpdateXX(s.ctx, where, update))

	dt, err = s.pDB.GetPrevPayoutDt(s.ctx, p2)
	require.NoError(s.T(), err)
	require.Equal(s.T(), p1.CreateDt.Unix(), dt)
}

// TestGetPrevPayoutDt проверяет поиск пред. выплаты, учитывая переданный статус выплаты.
// Создаем p1(done),p2(pending),p3(reject),p4(new). Для p4 должны найти p2, т.к. p3 отменен.
func (s *PayoutTestSuite) TestGetPrevPayoutDtX() {
	p1, err := Create(s.ctx, getRandomPayout())
	require.NoError(s.T(), err)
	require.NoError(s.T(), Update(s.ctx, p1.ID, StatusCol, StatusDone))

	p2, err := Create(s.ctx, getRandomPayout())
	require.NoError(s.T(), err)
	require.NoError(s.T(), Update(s.ctx, p2.ID, CreateDTCol, p1.CreateDt.Add(1*time.Second)))
	require.NoError(s.T(), Update(s.ctx, p2.ID, StatusCol, StatusPending))
	p2, err = Get(s.ctx, p2.ID)
	require.NoError(s.T(), err)

	p3, err := Create(s.ctx, getRandomPayout())
	require.NoError(s.T(), err)
	require.NoError(s.T(), Update(s.ctx, p3.ID, CreateDTCol, p1.CreateDt.Add(2*time.Second)))
	require.NoError(s.T(), Update(s.ctx, p3.ID, StatusCol, StatusRejected))

	p4, err := Create(s.ctx, getRandomPayout())
	require.NoError(s.T(), err)
	require.NoError(s.T(), Update(s.ctx, p4.ID, CreateDTCol, p1.CreateDt.Add(3*time.Second)))

	// Перезабираем данные из БД
	p4, err = Get(s.ctx, p4.ID)
	require.NoError(s.T(), err)

	update := core.UpdateDesc{
		{Name: ServiceIDCol, Value: p4.ServiceID},
		{Name: ContractIDCol, Value: p4.ContractID},
		{Name: ClientIDCol, Value: p4.ClientID},
	}
	where := core.WhereDesc{
		{Name: IDCol, Value: []int64{p3.ID, p2.ID, p1.ID}},
	}
	// все выплаты по тем же договору и сервису, что и последняя
	require.NoError(s.T(), UpdateXX(s.ctx, where, update))

	statuses := []string{StatusPending, StatusConfirmed, StatusDone}
	dt, err := s.pDB.GetPrevPayoutDtX(s.ctx, p4, statuses)
	require.NoError(s.T(), err)
	require.Equal(s.T(), p2.CreateDt.Unix(), dt.Unix())
}

// TestGetPrevPayoutDtXOldPayout проверяет поиск предыдущей выплаты,
// если она была создана более 45 дней назад
func (s *PayoutTestSuite) TestGetPrevPayoutDtXOldPayout() {
	p1, err := Create(s.ctx, getRandomPayout())
	require.NoError(s.T(), err)

	p2, err := Create(s.ctx, getRandomPayout())
	require.NoError(s.T(), err)

	// у выплат должны совпадать serviceID, contractID, clientID; create_dt -- 80 дней назад
	update := core.UpdateDesc{
		{Name: ServiceIDCol, Value: p1.ServiceID},
		{Name: ContractIDCol, Value: p1.ContractID},
		{Name: ClientIDCol, Value: p1.ClientID},
		{Name: StatusCol, Value: StatusDone},
		{Name: CreateDTCol, Value: p1.CreateDt.AddDate(0, 0, -80)},
	}

	require.NoError(s.T(), UpdateX(s.ctx, p2.ID, update))
	p2, err = Get(s.ctx, p2.ID)
	require.NoError(s.T(), err)

	statuses := []string{StatusPending, StatusConfirmed, StatusDone}
	dt, err := s.pDB.GetPrevPayoutDtX(s.ctx, p1, statuses)
	require.NoError(s.T(), err)
	require.Equal(s.T(), p2.CreateDt.Unix(), dt.Unix())

}
