package oebsgate

import (
	"context"
	"encoding/json"
	"fmt"
	"time"

	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"

	"a.yandex-team.ru/billing/hot/accounts/pkg/core/entities"
	"a.yandex-team.ru/billing/hot/payout/internal/core"
	"a.yandex-team.ru/billing/hot/payout/internal/logbroker"
	"a.yandex-team.ru/billing/hot/payout/internal/netting"
	"a.yandex-team.ru/billing/hot/payout/internal/payout"
	bt "a.yandex-team.ru/billing/library/go/billingo/pkg/testing"
)

// TestReserveMoneyDryRun проверяет, что признак dry_run прокидывается в событие reserve for payout
func (s *OEBSGateTestSuite) TestReserveMoneyDryRun() {
	s.SetProcessDryRun(true)
	p, _, err := s.createRandomPayoutWithRequest()
	assert.NoError(s.T(), err)

	lockID := "6666"
	// Успешное снятие блокировки в счетах
	s.ctx.Clients.AccountsLocker = &MockAccounterLocker{
		TestRemove: func(ctx context.Context, attributes *entities.LocationAttributes, uid string) {
			assert.Equal(s.T(), lockID, uid)
		},
	}

	// Успешная запись в LB (dry_run топик)
	lbWriters := logbroker.NewEmptyWriters()
	lbWriters.SetWriter(false, false, &logbroker.MockLogBrokerWriter{Err: ErrOooops})
	lbWriters.SetWriter(true, false, &logbroker.MockLogBrokerWriter{})
	namedWriters := logbroker.NamedWriters{"taxi": lbWriters}
	called := false

	s.ctx.Clients.AccountsBatch = &MockAccountBatch{
		// Деньги есть на счете
		ReadFn: func(r *entities.BatchReadRequest) (*entities.BatchReadResponse, error) {
			return &entities.BatchReadResponse{
				Balances: []entities.BalanceAttributesDt{
					{
						Debit:  "0",
						Credit: "1000.0",
					},
				},
				Locks: []entities.LockAttributes{
					{
						UID: lockID,
					},
				},
				DetailedTurnovers: []entities.DetailedTurnoverAttributesDt{
					{
						Events: []entities.EventDetails{
							{
								EventID:   "payout/taxi/11059644/2021-05-27T11:00:00+00:00",
								EventType: "taxi:payout",
								Info: []byte(fmt.Sprintf(`{
 "client_id": 92698540,
 "event_time": "2021-03-23T17:01:00+00:00",
 "transaction_id": "payout/taxi/92698540/2021-03-23T20:01:00+03:00",
 "tariffer_payload": {
	"dry_run": true,
	"common_ts": 1616518860,
	"contract_states": {
	  "2955592": {
		"netting_done": true,
		"payment_amount": 542.76,
		"payment_currency": "RUB"
	  },
	  "%d": {
		"netting_done": true,
		"payment_amount": 220,
		"payment_currency": "RUB",
		"invoices": [
			{"id": 1234, "operation_type": "INSERT", "external_id": "ЛСТ-2343434-1", "amount": 666.66},
			{"id": 1233, "operation_type": "UPSERT", "external_id": "ЛСТ-2343434-2", "amount": 222.22}
		]
	  },
	  "2955594": {
		"netting_done": true,
		"payment_amount": 0,
		"payment_currency": "RUB"
	  }
	}
 }
}`, p.ContractID)),
							},
						},
					},
				},
			}, nil
		},
		// Успешный перевод средств
		TestWrite: func(r *entities.BatchWriteRequest) {
			// проверяем, что отправили в систему счетов
			called = true
			assert.Equal(s.T(), []byte(`{"tariffer_payload":{"dry_run":true}}`), r.Info, string(r.Info))
			require.Len(s.T(), r.Locks, 1)
			require.Equal(s.T(), r.Locks[0].UID, lockID)
			require.Equal(s.T(), r.Locks[0].Mode, entities.LockRemoveMode)
		},
	}
	_, err = s.sendOneTx(p, &namedWriters)
	assert.NoError(s.T(), err)

	np, err := payout.Get(s.ctx, p.ID)
	assert.NoError(s.T(), err)
	assert.Equal(s.T(), payout.StatusPending, np.Status)
	assert.Equal(s.T(), payout.SyncStatusNew, np.SyncStatus)
	require.True(s.T(), called)
	require.Equal(s.T(), true, np.DryRun)
}

// TestCancelReserveDryRun проверяет, что признак dry_run прокидывается в событие cancel payout
func (s *OEBSGateTestSuite) TestCancelMoneyDryRun() {
	p, _, err := s.createRandomPayoutWithRequest()
	require.NoError(s.T(), err)

	// проставляем dry_run=true в базе и в структуре, чтобы не мокать ответ от системы счетов
	require.NoError(s.T(), payout.Update(s.ctx, p.ID, payout.DryRunCol, true))
	p.DryRun = true

	info := "Договор не найден"
	m := ARDResponse{
		StatusSystem: OEBS,
		MessageID:    p.ID,
		StatusType:   ARDStatusError,
		StatusInfo:   info,
	}

	r, _ := NewReceiver(s.ctx)

	called := false

	s.ctx.Clients.AccountsBatch = &MockAccountBatch{
		TestWrite: func(r *entities.BatchWriteRequest) {
			called = true
			// проверяем, что dry_run=true
			info, _ := formatConfirmInfo(p, p.OEBSPaymentID, []string{})
			infoUnmarshalled := ConfirmInfo{}
			_ = json.Unmarshal(info, &infoUnmarshalled)
			require.Equal(s.T(), true, infoUnmarshalled.TarifferPayload.DryRun)
			require.Equal(s.T(), string(info), string(r.Info), string(r.Info))
		},
	}

	retry, _, err := r.processARD(s.ctx, &m)
	require.NoError(s.T(), err)
	require.False(s.T(), retry)
	require.True(s.T(), called)

	pn, err := payout.Get(s.ctx, p.ID)
	assert.NoError(s.T(), err)
	assert.Equal(s.T(), payout.StatusRejected, pn.Status)
	require.NotNil(s.T(), pn.Error)
	require.Equal(s.T(), info, *pn.Error)
	require.Equal(s.T(), ARDStatusError, pn.OEBSStatus)
	require.Equal(s.T(), true, pn.DryRun)
}

// TestRegisterPaymentDryRun проверяет, что признак dry_run прокидывается в событие register payout
func (s *OEBSGateTestSuite) TestRegisterPaymentDryRun() {
	p, _, err := s.createRandomPayoutWithARD()
	assert.NoError(s.T(), err)

	// проставляем dry_run=true в базе и в структуре, чтобы не мокать ответ от системы счетов
	require.NoError(s.T(), payout.Update(s.ctx, p.ID, payout.DryRunCol, true))
	p.DryRun = true

	paymentID := bt.RandN64()

	m := OEBSResponse{
		StatusDT:       time.Now().Format("2006-01-02 15:04:05"),
		StatusSystem:   OEBS,
		StatusType:     OEBSStatusCreated,
		PaymentBatchID: []int64{p.OEBSBatchID},
		PaymentID:      &paymentID,
	}

	called := false

	// Успешное списание средств
	s.ctx.Clients.AccountsBatch = &MockAccountBatch{
		TestWrite: func(r *entities.BatchWriteRequest) {
			// проверяем, что отправили в систему счетов
			called = true
			info, _ := formatConfirmInfo(p, paymentID, []string{})
			infoUnmarshalled := ConfirmInfo{}
			_ = json.Unmarshal(info, &infoUnmarshalled)
			require.Equal(s.T(), true, infoUnmarshalled.TarifferPayload.DryRun)
			require.Equal(s.T(), info, r.Info)
		},
	}

	r, _ := NewReceiver(s.ctx)
	retry, _, err := r.processOEBS(s.ctx, &m)
	assert.NoError(s.T(), err)
	assert.False(s.T(), retry)
	require.True(s.T(), called)

	pn, err := payout.Get(s.ctx, p.ID)
	assert.NoError(s.T(), err)
	assert.Equal(s.T(), payout.StatusConfirmed, pn.Status)
	assert.Equal(s.T(), paymentID, pn.OEBSPaymentID)
	assert.Equal(s.T(), true, pn.DryRun)
}

// TestConfirmMoneyDryRun проверяет, что признак dry_run прокидывается в событие confirm payout
func (s *OEBSGateTestSuite) TestConfirmMoneyDryRun() {
	p, _, err := s.createRandomPayoutWithARD()
	assert.NoError(s.T(), err)

	// проставляем dry_run=true в базе и в структуре, чтобы не мокать ответ от системы счетов
	require.NoError(s.T(), payout.Update(s.ctx, p.ID, payout.DryRunCol, true))
	p.DryRun = true

	paymentID := bt.RandN64()
	require.NoError(s.T(), payout.Update(s.ctx, p.ID, payout.PaymentIDCol, paymentID))
	p.OEBSPaymentID = paymentID

	n1, err := s.createRandomNettingForPayout(p, netting.StatusDone)
	assert.NoError(s.T(), err)
	n2, err := s.createRandomNettingForPayout(p, netting.StatusDone)
	assert.NoError(s.T(), err)
	// этот ВЗ не должен попасть в info, тк статус == new
	_, err = s.createRandomNettingForPayout(p, netting.StatusNew)
	assert.NoError(s.T(), err)

	m := OEBSResponse{
		StatusDT:       time.Now().Format("2006-01-02 15:04:05"),
		StatusSystem:   OEBS,
		StatusType:     OEBSStatusReconciled,
		PaymentBatchID: []int64{p.OEBSBatchID},
		PaymentID:      &paymentID,
	}

	// Успешное списание средств
	called := false
	s.ctx.Clients.AccountsBatch = &MockAccountBatch{
		TestWrite: func(r *entities.BatchWriteRequest) {
			// проверяем, что отправили в систему счетов
			called = true
			info, _ := formatConfirmInfo(p, paymentID, []string{n1.ExtID, n2.ExtID})
			infoUnmarshalled := ConfirmInfo{}
			_ = json.Unmarshal(info, &infoUnmarshalled)
			require.Equal(s.T(), true, infoUnmarshalled.TarifferPayload.DryRun)
			require.Equal(s.T(), info, r.Info)
		},
	}

	r, _ := NewReceiver(s.ctx)
	retry, _, err := r.processOEBS(s.ctx, &m)
	assert.NoError(s.T(), err)
	assert.False(s.T(), retry)
	require.True(s.T(), called)

	pn, err := payout.Get(s.ctx, p.ID)
	assert.NoError(s.T(), err)
	assert.Equal(s.T(), payout.StatusDone, pn.Status)
	assert.Equal(s.T(), paymentID, pn.OEBSPaymentID)
	assert.Equal(s.T(), true, pn.DryRun)
}

// TestReturnSentMoneyDryRun проверяет, что признак dry_run прокидывается в событие cancel money
func (s *OEBSGateTestSuite) TestReturnSentMoneyDryRun() {
	p, _, err := s.createRandomPayoutWithARD()
	assert.NoError(s.T(), err)

	// проставляем dry_run=true в базе и в структуре, чтобы не мокать ответ от системы счетов
	require.NoError(s.T(), payout.Update(s.ctx, p.ID, payout.DryRunCol, true))
	p.DryRun = true

	m := OEBSResponse{
		StatusDT:       time.Now().Format("2006-01-02 15:04:05"),
		StatusSystem:   OEBS,
		StatusType:     OEBSStatusReturned,
		PaymentBatchID: []int64{p.OEBSBatchID},
	}

	// возврата средств быть не должно
	s.ctx.Clients.AccountsBatch = &MockAccountBatch{
		TestWrite: func(r *entities.BatchWriteRequest) {
			assert.Fail(s.T(), "Trying to revert funds")
		},
	}

	// ВЗ
	n1, err := s.createRandomNettingForPayout(p, netting.StatusDone)
	assert.NoError(s.T(), err)
	// n2 переводим в статус ошибки, чтобы убедиться, что в info берем
	// только ВЗ в статусе done
	_, err = s.createRandomNettingForPayout(p, netting.StatusError)
	assert.NoError(s.T(), err)

	n, prod, err := s.createNotifier()
	require.NoError(s.T(), err)

	r, _ := NewReceiver(s.ctx, ReceiverWithNotifier(n))
	retry, _, err := r.processOEBS(s.ctx, &m)
	assert.NoError(s.T(), err)
	assert.False(s.T(), retry)
	require.Len(s.T(), prod.Messages, 1)

	pn, err := payout.Get(s.ctx, p.ID)
	assert.NoError(s.T(), err)
	assert.Equal(s.T(), payout.StatusConfirmed, pn.Status)

	// проверяем, что отправляем запрос в систему счетов и
	// меняем статус с финального (done) пред-финальный (confirmed)
	paymentID := bt.RandN64()
	update := core.UpdateDesc{
		{Name: payout.StatusCol, Value: payout.StatusDone},
		{Name: payout.PaymentIDCol, Value: paymentID},
	}
	require.NoError(s.T(), payout.UpdateX(s.ctx, p.ID, update))
	p.Status = payout.StatusDone
	p.OEBSPaymentID = paymentID

	called := false

	s.ctx.Clients.AccountsBatch = &MockAccountBatch{
		TestWrite: func(r *entities.BatchWriteRequest) {
			// проверяем, что отправили в систему счетов
			called = true
			info, _ := formatConfirmInfo(p, paymentID, []string{n1.ExtID})
			infoUnmarshalled := ConfirmInfo{}
			_ = json.Unmarshal(info, &infoUnmarshalled)
			require.Equal(s.T(), true, infoUnmarshalled.TarifferPayload.DryRun)
			require.Equal(s.T(), string(info), string(r.Info))
		},
	}

	retry, _, err = r.processOEBS(s.ctx, &m)
	require.NoError(s.T(), err)
	require.False(s.T(), retry)

	pn, err = payout.Get(s.ctx, p.ID)
	require.NoError(s.T(), err)
	require.Equal(s.T(), payout.StatusConfirmed, pn.Status)
	require.True(s.T(), called)
	require.Equal(s.T(), true, pn.DryRun)

	// убеждаемся, что статус не изменился
	where := core.WhereDesc{
		{Name: netting.IDCol, Value: n1.ID},
	}
	ns, err := netting.Get(s.ctx, where)
	require.NoError(s.T(), err)
	require.Len(s.T(), ns, 1)
	require.Equal(s.T(), netting.StatusDone, ns[0].Status)
}
