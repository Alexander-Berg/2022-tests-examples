package oebsgate

import (
	"fmt"
	"time"

	sq "github.com/Masterminds/squirrel"
	"github.com/shopspring/decimal"
	"github.com/stretchr/testify/require"

	"a.yandex-team.ru/billing/hot/accounts/pkg/core/entities"
	"a.yandex-team.ru/billing/hot/payout/internal/core"
	"a.yandex-team.ru/billing/hot/payout/internal/cpf"
	"a.yandex-team.ru/billing/hot/payout/internal/interactions"
	"a.yandex-team.ru/billing/hot/payout/internal/logbroker"
	"a.yandex-team.ru/billing/hot/payout/internal/notifier"
	"a.yandex-team.ru/billing/hot/payout/internal/payout"
	bt "a.yandex-team.ru/billing/library/go/billingo/pkg/testing"
	"a.yandex-team.ru/billing/library/go/billingo/pkg/xlog"
	"a.yandex-team.ru/library/go/core/log"
)

// TestReversePaymentOrder эмулируют проблему с дублированием cpf с реальной системой счетов.
// Проблема возникает при обратном порядке обработки выплат, сначала новая, потом старая
// В случае отсутствия проблем у нас должно быть только 2 cpf
func (s *OEBSGateTestSuite) TestReversePaymentOrder() {
	dt := time.Now()
	prevDT := dt.AddDate(0, 0, -1)
	cpfID := "ЛСТ-666153102-1"

	require.NoError(s.T(), s.setClients())

	// создаем первый взаимозачет и первую выплату
	p1, _, err := s.createRandomPayoutWithRequestAmount("100")
	require.NoError(s.T(), err)
	require.NoError(s.T(), payout.UpdateX(s.ctx, p1.ID, core.UpdateDesc{
		{Name: payout.CreateDTCol, Value: prevDT.Add(1 * time.Second)},
	},
	))
	p1, err = payout.Get(s.ctx, p1.ID)
	require.NoError(s.T(), err)

	loc, err := getAccountLoc(p1, interactions.PayoutAccountType)
	require.NoError(s.T(), err)

	_, err = s.ctx.Clients.AccountsBatch.WriteBatch(s.ctx, &entities.BatchWriteRequest{
		EventType:  "complex:payout",
		ExternalID: bt.RandS(30),
		Dt:         prevDT,
		Info: []byte(fmt.Sprintf(`{
  "client_id": 92698540,
  "event_time": "2021-03-23T17:01:00+00:00",
  "transaction_id": "5494732C-60E4-4E84-A727-83F9D0996170",
  "tariffer_payload": {
	"dry_run": true,
	"common_ts": 1616518860,
	"contract_states": {
  		"invoices":{
			"%d": [
				{"id": 1231123, "operation_type": "INSERT", "external_id": "%s", "amount": 100}
			]
		},
  		"nettings": {
		}
	}
  }
}`, p1.ContractID, cpfID)),
		Events: []entities.EventAttributes{{
			Loc:    *loc,
			Type:   Credit,
			Dt:     prevDT,
			Amount: p1.Amount.String(),
		}},
		States: nil,
		Locks:  nil,
	})
	require.NoError(s.T(), err)

	// создаем второй взаимозачет и вторую выплату
	p2, err := payout.Create(s.ctx, &payout.NewPayout{
		ExternalID: bt.RandS(30),
		ServiceID:  p1.ServiceID,
		ContractID: p1.ContractID,
		Amount:     decimal.RequireFromString("200"),
		Currency:   "RUB",
		ClientID:   p1.ClientID,
		Namespace:  notifier.DefaultNamespace,
	})
	require.NoError(s.T(), err)

	_, err = s.ctx.Clients.AccountsBatch.WriteBatch(s.ctx, &entities.BatchWriteRequest{
		EventType:  "complex:payout",
		ExternalID: bt.RandS(30),
		Dt:         dt.Add(-1 * time.Second),
		Info: []byte(fmt.Sprintf(`{
  "client_id": 92698540,
  "event_time": "2021-03-23T17:01:00+00:00",
  "transaction_id": "A4DCAD91-FC74-4B03-B6DD-5ED397295553",
   "tariffer_payload": {
	"dry_run": true,
	"common_ts": 1616518860,
	"contract_states": {
  		"invoices":{
			"%d": [
				{"id": 1231124, "operation_type": "INSERT", "external_id": "%s", "amount": 200}
			]
		},
  		"nettings": {
		}
	}
  }
}`, p1.ContractID, cpfID)),
		Events: []entities.EventAttributes{{
			Loc:    *loc,
			Type:   Credit,
			Dt:     dt.Add(-1 * time.Second),
			Amount: p2.Amount.String(),
		}},
		States: nil,
		Locks:  nil,
	})
	require.NoError(s.T(), err)

	lbWriters := logbroker.NewEmptyWriters()
	lbWriters.SetWriter(false, false, &logbroker.MockLogBrokerWriter{Err: ErrOooops})
	lbWriters.SetWriter(true, false, &logbroker.MockLogBrokerWriter{})
	lbWriters.SetWriter(false, true, &logbroker.MockLogBrokerWriter{Err: ErrOooops})
	lbWriters.SetWriter(true, true, &logbroker.MockLogBrokerWriter{})
	namedWriters := logbroker.NamedWriters{"taxi": lbWriters}

	_, err = s.sendOneTxX(p2, &namedWriters, true)
	require.NoError(s.T(), err)

	// если сделать запрос баланса в ту же секунду, когда произошло событие, то мы не увидим изменения на счете
	time.Sleep(2 * time.Second)

	_, err = s.sendOneTxX(p1, &namedWriters, true)
	require.NoError(s.T(), err)

	cpfs, err := cpf.GetX(s.ctx, core.WhereDescX{sq.Eq{cpf.ExtIDCol: cpfID}}, false)
	require.NoError(s.T(), err)
	xlog.Info(s.ctx, "get cpf", log.Array("data", cpfs))
	require.Len(s.T(), cpfs, 3) // TODO: correct value of length is 2
}

// TestPayoutReturnAndCreate эмулирует случай доставки выплаты до OEBS, ее отмены внутри OEBS и повторной отправки
// Тест проверяет:
// 1. Что суммы на счетах получаются правильные на каждом шаге
// 2. Что в итоге будет два события "register payout", которые является ключевой для детализации платежек
func (s *OEBSGateTestSuite) TestPayoutReturnAndCreate() {
	dt := time.Now()
	prevDT := dt.AddDate(0, 0, -1)

	require.NoError(s.T(), s.setClients())

	// создаем первый взаимозачет и первую выплату
	p1, _, err := s.createRandomPayoutWithRequestAmount("300")
	require.NoError(s.T(), err)
	require.NoError(s.T(), payout.UpdateX(s.ctx, p1.ID, core.UpdateDesc{
		{Name: payout.CreateDTCol, Value: prevDT.Add(1 * time.Second)},
	},
	))
	p1, err = payout.Get(s.ctx, p1.ID)
	require.NoError(s.T(), err)

	loc, err := getAccountLoc(p1, interactions.PayoutAccountType)
	require.NoError(s.T(), err)
	locTmp, err := getAccountLoc(p1, interactions.PayoutTmpAccountType)
	require.NoError(s.T(), err)

	_, err = s.ctx.Clients.AccountsBatch.WriteBatch(s.ctx, &entities.BatchWriteRequest{
		EventType:  "complex:payout",
		ExternalID: bt.RandS(30),
		Dt:         prevDT,
		Info:       nil,
		Events: []entities.EventAttributes{{
			Loc:    *loc,
			Type:   Credit,
			Dt:     prevDT,
			Amount: p1.Amount.String(),
		}},
		States: nil,
		Locks:  nil,
	})
	require.NoError(s.T(), err)

	lbWriters := logbroker.NewEmptyWriters()
	lbWriters.SetWriter(false, false, &logbroker.MockLogBrokerWriter{Err: ErrOooops})
	lbWriters.SetWriter(true, false, &logbroker.MockLogBrokerWriter{})
	lbWriters.SetWriter(false, true, &logbroker.MockLogBrokerWriter{Err: ErrOooops})
	lbWriters.SetWriter(true, true, &logbroker.MockLogBrokerWriter{})
	namedWriters := logbroker.NamedWriters{"taxi": lbWriters}

	// проверяем баланс на счете и проводим выплату
	data, err := s.ctx.Clients.AccountsReader.GetAccountBalance(s.ctx, loc, nil)
	require.NoError(s.T(), err)
	require.Len(s.T(), data, 1)
	require.True(s.T(), decimal.RequireFromString(data[0].Credit).Equal(p1.Amount))
	require.True(s.T(), decimal.Zero.Equal(decimal.RequireFromString(data[0].Debit)))

	_, err = s.sendOneTxX(p1, &namedWriters, true)
	require.NoError(s.T(), err)

	time.Sleep(1 * time.Second)
	data, err = s.ctx.Clients.AccountsReader.GetAccountBalance(s.ctx, loc, nil)
	require.NoError(s.T(), err)
	require.Len(s.T(), data, 1)
	require.True(s.T(), decimal.RequireFromString(data[0].Credit).Equal(p1.Amount))
	require.True(s.T(), decimal.RequireFromString(data[0].Debit).Equal(p1.Amount))

	data, err = s.ctx.Clients.AccountsReader.GetAccountBalance(s.ctx, locTmp, nil)
	require.NoError(s.T(), err)
	require.Len(s.T(), data, 1)
	require.True(s.T(), decimal.RequireFromString(data[0].Credit).Equal(p1.Amount))
	require.True(s.T(), decimal.Zero.Equal(decimal.RequireFromString(data[0].Debit)))

	// подтверждаем выплату из ARD
	batchID := bt.RandN64()

	am := ARDResponse{
		StatusSystem:   OEBS,
		MessageID:      p1.ID,
		StatusType:     ARDStatusNew,
		PaymentBatchID: &batchID,
	}

	r, _ := NewReceiver(s.ctx)
	retry, _, err := r.processARD(s.ctx, &am)
	require.NoError(s.T(), err)
	require.False(s.T(), retry)

	pn, err := payout.Get(s.ctx, p1.ID)
	require.NoError(s.T(), err)
	require.Equal(s.T(), payout.StatusConfirmed, pn.Status)
	require.Equal(s.T(), batchID, pn.OEBSBatchID)
	require.Equal(s.T(), ARDStatusNew, pn.OEBSStatus)

	// проводим выплату в OEBS
	paymentID := bt.RandN64()

	amount := p1.Amount
	confirmOEBS := func(paymentID int64, amount decimal.Decimal) {
		om := OEBSResponse{
			StatusDT:       time.Now().Format("2006-01-02 15:04:05"),
			StatusSystem:   OEBS,
			StatusType:     OEBSStatusCreated,
			PaymentBatchID: []int64{pn.OEBSBatchID},
			PaymentID:      &paymentID,
		}

		retry, _, err = r.processOEBS(s.ctx, &om)
		require.NoError(s.T(), err)
		require.False(s.T(), retry)

		pn, err = payout.Get(s.ctx, p1.ID)
		require.NoError(s.T(), err)
		require.Equal(s.T(), payout.StatusConfirmed, pn.Status)
		require.Equal(s.T(), paymentID, pn.OEBSPaymentID)

		om = OEBSResponse{
			StatusDT:       time.Now().Format("2006-01-02 15:04:05"),
			StatusSystem:   OEBS,
			StatusType:     OEBSStatusReconciled,
			PaymentBatchID: []int64{pn.OEBSBatchID},
			PaymentID:      &paymentID,
		}

		retry, _, err = r.processOEBS(s.ctx, &om)
		require.NoError(s.T(), err)
		require.False(s.T(), retry)

		pn, err = payout.Get(s.ctx, p1.ID)
		require.NoError(s.T(), err)
		require.Equal(s.T(), payout.StatusDone, pn.Status)
		require.Equal(s.T(), paymentID, pn.OEBSPaymentID)

		data, err = s.ctx.Clients.AccountsReader.GetAccountBalance(s.ctx, locTmp, nil)
		require.NoError(s.T(), err)
		require.Len(s.T(), data, 1)
		require.True(s.T(), decimal.RequireFromString(data[0].Credit).Equal(amount))
		require.True(s.T(), decimal.RequireFromString(data[0].Debit).Equal(amount))
	}

	confirmOEBS(paymentID, amount)

	// отменяем выплату из OEBS
	om := OEBSResponse{
		StatusDT:       time.Now().Format("2006-01-02 15:04:05"),
		StatusSystem:   OEBS,
		StatusType:     OEBSStatusReturned,
		PaymentBatchID: []int64{pn.OEBSBatchID},
	}

	retry, _, err = r.processOEBS(s.ctx, &om)
	require.NoError(s.T(), err)
	require.False(s.T(), retry)

	pn, err = payout.Get(s.ctx, p1.ID)
	require.NoError(s.T(), err)
	require.Equal(s.T(), payout.StatusConfirmed, pn.Status)

	amount = p1.Amount.Mul(decimal.NewFromInt(2))
	data, err = s.ctx.Clients.AccountsReader.GetAccountBalance(s.ctx, locTmp, nil)
	require.NoError(s.T(), err)
	require.Len(s.T(), data, 1)
	require.True(s.T(), decimal.RequireFromString(data[0].Credit).Equal(amount))
	require.True(s.T(), decimal.RequireFromString(data[0].Debit).Equal(p1.Amount))

	// пробуем снова подтвердить выплату в OEBS
	paymentID = bt.RandN64()
	confirmOEBS(paymentID, amount)

	detail, err := s.ctx.Clients.AccountsReader.GetAccountDetailedTurnover(s.ctx, locTmp, 0, time.Now().Add(1*time.Second).Unix())
	require.NoError(s.T(), err)
	register := 0
	for _, turnover := range detail {
		for _, event := range turnover.Events {
			if event.EventType == registerPayoutEvent {
				register += 1
			}
		}
	}
	require.Equal(s.T(), 2, register)
}

// TestPayoutReturnAndCreate эмулирует ситуацию, когда в рамках одной секундны пришло событие в систему счетов и мы обработали выплату
//
// В старой версии сумма выплаты обновлялась в 0, сейчас она не должна меняться
func (s *OEBSGateTestSuite) TestOneMomentCreateAndProcess() {
	dt := time.Now()
	//prevDT := dt.AddDate(0, 0, -1)

	require.NoError(s.T(), s.setClients())

	// создаем первый взаимозачет и первую выплату
	p1, _, err := s.createRandomPayoutWithRequestAmount("300")
	require.NoError(s.T(), err)

	loc, err := getAccountLoc(p1, interactions.PayoutAccountType)
	require.NoError(s.T(), err)
	locTmp, err := getAccountLoc(p1, interactions.PayoutTmpAccountType)
	require.NoError(s.T(), err)

	_, err = s.ctx.Clients.AccountsBatch.WriteBatch(s.ctx, &entities.BatchWriteRequest{
		EventType:  "complex:payout",
		ExternalID: bt.RandS(30),
		Dt:         dt,
		Info:       nil,
		Events: []entities.EventAttributes{{
			Loc:    *loc,
			Type:   Credit,
			Dt:     dt,
			Amount: p1.Amount.String(),
		}},
		States: nil,
		Locks:  nil,
	})
	require.NoError(s.T(), err)

	lbWriters := logbroker.NewEmptyWriters()
	lbWriters.SetWriter(false, false, &logbroker.MockLogBrokerWriter{Err: ErrOooops})
	lbWriters.SetWriter(true, false, &logbroker.MockLogBrokerWriter{})
	lbWriters.SetWriter(false, true, &logbroker.MockLogBrokerWriter{Err: ErrOooops})
	lbWriters.SetWriter(true, true, &logbroker.MockLogBrokerWriter{})
	namedWriters := logbroker.NamedWriters{"taxi": lbWriters}

	// проверяем баланс на счете и проводим выплату
	data, err := s.ctx.Clients.AccountsReader.GetAccountBalance(s.ctx, loc, nil)
	require.NoError(s.T(), err)
	require.Len(s.T(), data, 1)
	require.True(s.T(), decimal.RequireFromString(data[0].Credit).Equal(p1.Amount))
	require.True(s.T(), decimal.Zero.Equal(decimal.RequireFromString(data[0].Debit)))

	_, err = s.sendOneTxX(p1, &namedWriters, true)
	require.NoError(s.T(), err)

	result, err := payout.Get(s.ctx, p1.ID)
	require.NoError(s.T(), err)
	require.False(s.T(), decimal.Zero.Equal(result.Amount))

	time.Sleep(1 * time.Second)
	data, err = s.ctx.Clients.AccountsReader.GetAccountBalance(s.ctx, loc, nil)
	require.NoError(s.T(), err)
	require.Len(s.T(), data, 1)
	require.True(s.T(), decimal.RequireFromString(data[0].Credit).Equal(p1.Amount))
	require.True(s.T(), decimal.RequireFromString(data[0].Debit).Equal(p1.Amount))

	data, err = s.ctx.Clients.AccountsReader.GetAccountBalance(s.ctx, locTmp, nil)
	require.NoError(s.T(), err)
	require.Len(s.T(), data, 1)
	require.True(s.T(), decimal.RequireFromString(data[0].Credit).Equal(p1.Amount))
	require.True(s.T(), decimal.Zero.Equal(decimal.RequireFromString(data[0].Debit)))
}
