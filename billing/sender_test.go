package oebsgate

import (
	"context"
	"database/sql/driver"
	"encoding/json"
	"fmt"
	"os"
	"strconv"
	"strings"
	"time"

	"github.com/shopspring/decimal"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"

	"a.yandex-team.ru/billing/hot/accounts/pkg/core/entities"
	pc "a.yandex-team.ru/billing/hot/payout/internal/context"
	"a.yandex-team.ru/billing/hot/payout/internal/core"
	"a.yandex-team.ru/billing/hot/payout/internal/cpf"
	"a.yandex-team.ru/billing/hot/payout/internal/interactions"
	"a.yandex-team.ru/billing/hot/payout/internal/logbroker"
	"a.yandex-team.ru/billing/hot/payout/internal/netting"
	"a.yandex-team.ru/billing/hot/payout/internal/notifier"
	"a.yandex-team.ru/billing/hot/payout/internal/payout"
	bt "a.yandex-team.ru/billing/library/go/billingo/pkg/testing"
)

// sendOneTx выполняет sendOne в транзакции
func (s *OEBSGateTestSuite) sendOneTx(p *payout.Payout, namedWriters *logbroker.NamedWriters) (bool, error) {
	return s.sendOneTxX(p, namedWriters, false)
}

func (s *OEBSGateTestSuite) sendOneTxX(p *payout.Payout, namedWriters *logbroker.NamedWriters, syncCPF bool) (bool, error) {
	return s.sendOneTxXX(p, namedWriters, syncCPF, false)
}

func (s *OEBSGateTestSuite) sendOneTxXX(p *payout.Payout, namedWriters *logbroker.NamedWriters, syncCPF bool, syncNetting bool) (bool, error) {
	ps := NewPaymentSender(s.ctx, namedWriters, SenderWithDB(s.pDB))
	ps.flags.syncCPF = syncCPF
	ps.flags.syncNetting = syncNetting
	tx := s.Tx()
	defer func() {
		_ = tx.Commit()
	}()
	return ps.sendOne(p, tx)
}

// resetPaymentID используется, чтобы вернуть выплате ее начальный id, с которым ее создали.
// Используется в тестах, где искусственно меняется порядок выплат.
func (s *OEBSGateTestSuite) resetPaymentID(oldID, curID int64) {
	update := core.UpdateDesc{
		{Name: payout.IDCol, Value: oldID},
	}
	require.NoError(s.T(), payout.UpdateX(s.ctx, curID, update))
}

// TestErrorFailReserve проверяет, что если не получилось сделать резерв денег (система счетов не доступна) мы сваливаемся со статусом
// синхронизации `acc`, чтобы когда снова будем обрабатывать эту выплату
func (s *OEBSGateTestSuite) TestErrorFailReserve() {
	p, _, err := s.createRandomPayoutWithRequest()
	assert.NoError(s.T(), err)

	lockID := "6666"
	// Успешное снятие блокировки в счетах
	s.ctx.Clients.AccountsLocker = &MockAccounterLocker{
		TestRemove: func(ctx context.Context, attributes *entities.LocationAttributes, uid string) {
			assert.Equal(s.T(), lockID, uid)
		},
	}

	s.ctx.Clients.AccountsBatch = &MockAccountBatch{
		// Имитируем ошибку при резервировании
		WriteErr: ErrOooops,
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
			}, nil
		},
	}
	lbWriters := logbroker.NewEmptyWriters()
	lbWriters.SetWriter(false, false, &logbroker.MockLogBrokerWriter{})
	lbWriters.SetWriter(true, false, &logbroker.MockLogBrokerWriter{Err: ErrOooops})
	namedWriters := logbroker.NamedWriters{"taxi": lbWriters}

	blocking, err := s.sendOneTx(p, &namedWriters)
	assert.Error(s.T(), err)
	assert.False(s.T(), blocking)
	assert.Equal(s.T(), ErrOooops, err)

	np, err := payout.Get(s.ctx, p.ID)
	assert.NoError(s.T(), err)

	assert.Equal(s.T(), payout.StatusNew, np.Status)
	assert.Equal(s.T(), payout.SyncStatusNew, np.SyncStatus)
}

// TestErrorFailLogBrokerWrite проверяет статус синхронизации, если не получилось отправить
// выплату в АРД (записать в LB)
func (s *OEBSGateTestSuite) TestErrorFailLogBrokerWrite() {
	p, _, err := s.createRandomPayoutWithRequest()
	assert.NoError(s.T(), err)

	lockID := "6666"
	// Успешное снятие блокировки в счетах
	s.ctx.Clients.AccountsLocker = &MockAccounterLocker{
		TestRemove: func(ctx context.Context, attributes *entities.LocationAttributes, uid string) {
			assert.Equal(s.T(), lockID, uid)
		},
	}

	// Ошибка при записи в LB
	lbWriters := logbroker.NewEmptyWriters()
	lbWriters.SetWriter(false, false, &logbroker.MockLogBrokerWriter{Err: ErrOooops})
	lbWriters.SetWriter(true, false, &logbroker.MockLogBrokerWriter{Err: ErrOooops})
	namedWriters := logbroker.NamedWriters{"taxi": lbWriters}

	// Успешное резервирование средств
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
			}, nil
		},
	}

	blocking, err := s.sendOneTx(p, &namedWriters)
	assert.Error(s.T(), err, p.ID)
	assert.True(s.T(), blocking)
	assert.Equal(s.T(), ErrOooops, err)

	np, err := payout.Get(s.ctx, p.ID)
	assert.NoError(s.T(), err)

	assert.Equal(s.T(), payout.StatusNew, np.Status)
	assert.Equal(s.T(), payout.SyncStatusAcc, np.SyncStatus)

	// убеждаемся, что второй раз не пойдем читать баланс или резервировать средства.
	// Имитируем ошибку, надеясь, что она не появится. Должна вернуться ошибки из LB
	s.ctx.Clients.AccountsBatch = &MockAccountBatch{
		WriteErr: ErrNoAccountBalances,
	}
	_, err = s.sendOneTx(p, &namedWriters)
	assert.Error(s.T(), err, p.ID)
	assert.Equal(s.T(), ErrOooops, err) // ошибка та, что из LB

	np, err = payout.Get(s.ctx, p.ID)
	assert.NoError(s.T(), err)

	assert.Equal(s.T(), payout.StatusNew, np.Status)
	assert.Equal(s.T(), payout.SyncStatusAcc, np.SyncStatus)
}

func (s *OEBSGateTestSuite) TestSendOne() {
	s.SetProcessDryRun(true)
	p, _, err := s.createRandomPayoutWithRequest()
	assert.NoError(s.T(), err)
	amount := p.Amount.String()

	lockID := "6666"
	// Успешное снятие блокировки в счетах
	s.ctx.Clients.AccountsLocker = &MockAccounterLocker{
		TestRemove: func(ctx context.Context, attributes *entities.LocationAttributes, uid string) {
			assert.Equal(s.T(), lockID, uid)
		},
	}

	// Успешная запись в LB
	lbWriters := logbroker.NewEmptyWriters()
	lbWriters.SetWriter(false, false, &logbroker.MockLogBrokerWriter{})
	lbWriters.SetWriter(true, false, &logbroker.MockLogBrokerWriter{Err: ErrOooops})
	namedWriters := logbroker.NamedWriters{"taxi": lbWriters}

	s.ctx.Clients.AccountsBatch = &MockAccountBatch{
		// Деньги есть на счете
		ReadFn: func(r *entities.BatchReadRequest) (*entities.BatchReadResponse, error) {
			return &entities.BatchReadResponse{
				Balances: []entities.BalanceAttributesDt{
					{
						Debit:  "0",
						Credit: "124.66",
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
								Info: []byte(`{
  "client_id": 92698540,
  "event_time": "2021-03-23T17:01:00+00:00",
  "transaction_id": "payout/taxi/92698540/2021-03-23T20:01:00+03:00",
  "tariffer_payload": {
	"dry_run": false
	}
}`),
							},
						},
					},
				},
			}, nil
		},
		// Успешный перевод средств
		TestWrite: func(r *entities.BatchWriteRequest) {
			// проверяем, что отправили в систему счетов
			require.Len(s.T(), r.Events, 2)
			assert.Equal(s.T(), reservePayoutEvent, r.EventType)
			assert.Equal(s.T(), fmt.Sprintf("%d", p.ID), r.ExternalID)
			assert.Equal(s.T(), []byte(`{"tariffer_payload":{"dry_run":false}}`), r.Info, string(r.Info))
			assert.Equal(s.T(), Debit, r.Events[0].Type)
			assert.Equal(s.T(), Credit, r.Events[1].Type)
			assert.Equal(s.T(), amount, r.Events[0].Amount)
			assert.Equal(s.T(), amount, r.Events[1].Amount)
		},
	}
	// Успешная запись в ЛБ
	_, err = s.sendOneTx(p, &namedWriters)
	assert.NoError(s.T(), err)

	// проверем, что платеж в статусе pending
	np, err := payout.Get(s.ctx, p.ID)
	assert.NoError(s.T(), err)
	assert.Equal(s.T(), payout.StatusPending, np.Status)
	// т.к. осн. статус поменялся, то статус синхронизации
	// должен в начальное состояние перейти - new
	assert.Equal(s.T(), payout.SyncStatusNew, np.SyncStatus)
	assert.Equal(s.T(), false, np.DryRun)
}

// TestGetBalanceWithLockWithCPF проверяет получение инф-ции о счетах из INFO при включенном флаге
func (s *OEBSGateTestSuite) TestGetBalanceWithLockWithCPF() {
	p, _, err := s.createRandomPayoutWithRequest()
	assert.NoError(s.T(), err)

	// Успешная запись в LB
	lbWriters := logbroker.NewEmptyWriters()
	lbWriters.SetWriter(false, false, &logbroker.MockLogBrokerWriter{})
	lbWriters.SetWriter(true, false, &logbroker.MockLogBrokerWriter{Err: ErrOooops})
	namedWriters := logbroker.NamedWriters{"taxi": lbWriters}

	s.ctx.Clients.AccountsBatch = &MockAccountBatch{
		// Проверяем, что в режиме CPF передаем информацию по детализации
		TestRead: func(r *entities.BatchReadRequest) {
			require.NotNil(s.T(), r.DetailedTurnovers)
			require.Len(s.T(), r.DetailedTurnovers, 1)
			require.NotNil(s.T(), interactions.PayoutTmpAccountType, r.DetailedTurnovers[0].Loc.Type)
		},
		// Отдаем данные по детализации
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
						UID: "xyz",
					},
				},
				DetailedTurnovers: []entities.DetailedTurnoverAttributesDt{
					{
						Loc:            entities.LocationAttributes{Type: "some-type", Attributes: nil},
						DtFrom:         entities.APIDt(time.Now()),
						DtTo:           entities.APIDt(time.Now()),
						DebitInit:      "0",
						CreditInit:     "0",
						DebitTurnover:  "0",
						CreditTurnover: "0",
						Events: []entities.EventDetails{
							{
								Info: []byte(fmt.Sprintf(`{
  "client_id": 92698540,
  "event_time": "2021-03-23T17:01:00+00:00",
  "transaction_id": "payout/taxi/92698540/2021-03-23T20:01:00+03:00",
  "tariffer_payload": {
	"common_ts": 1616518860,
		"contract_states": {
	  "invoices": {
  		"%d": [
			{"id": 1234, "operation_type": "INSERT", "external_id": "ЛСТ-2343434-1", "amount": 666.66},
			{"id": 1233, "operation_type": "UPSERT", "external_id": "ЛСТ-2343434-2", "amount": 222.22}
		]
      },
	  "nettings": {
	  	"2955592": {
		  "netting_done": true,
   		  "payment_amount": 542.76,
   		  "payment_currency": "RUB"
   	    },
   	    "%d": {
   		  "netting_done": true,
   		  "payment_amount": 220,
   		  "payment_currency": "RUB"
   	    },
   	    "2955594": {
   		  "netting_done": true,
   		  "payment_amount": 0,
   		  "payment_currency": "RUB"
 		}
	  }
	}
  }
}`, p.ContractID, p.ContractID)),
							},
						},
					},
				},
			}, nil
		},
	}

	ps := NewPaymentSender(s.ctx, &namedWriters, SenderWithDB(s.pDB))
	ps.flags.syncCPF = true

	_, resp, _, _, err := ps.getBalanceWithLock(p)
	contractID := strconv.FormatInt(p.ContractID, 10)
	invoices := ps.parseCPF(resp, contractID)
	require.NoError(s.T(), err)
	require.Len(s.T(), invoices, 2)
	require.Equal(s.T(), "ЛСТ-2343434-1", invoices[0].ExternalID)
	require.Equal(s.T(), "INSERT", invoices[0].OperationType)
	require.Equal(s.T(), decimal.RequireFromString("666.66"), invoices[0].Amount)
	require.Equal(s.T(), "ЛСТ-2343434-2", invoices[1].ExternalID)
	require.Equal(s.T(), "UPSERT", invoices[1].OperationType)
	require.Equal(s.T(), decimal.RequireFromString("222.22"), invoices[1].Amount)
}

// TestGetBalanceWithLockWithoutCPF проверяет, что при выключенном флаге, данные из INFO не забираются.
// и мы при этом не ломаемся.
func (s *OEBSGateTestSuite) TestGetBalanceWithLockWithoutCPF() {
	p, _, err := s.createRandomPayoutWithRequest()
	assert.NoError(s.T(), err)

	// Успешная запись в LB
	lbWriters := logbroker.NewEmptyWriters()
	lbWriters.SetWriter(false, false, &logbroker.MockLogBrokerWriter{})
	lbWriters.SetWriter(true, false, &logbroker.MockLogBrokerWriter{Err: ErrOooops})
	namedWriters := logbroker.NamedWriters{"taxi": lbWriters}

	s.ctx.Clients.AccountsBatch = &MockAccountBatch{
		// Отдаем данные по детализации
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
						UID: "xyz",
					},
				},
				DetailedTurnovers: []entities.DetailedTurnoverAttributesDt{
					{
						Events: []entities.EventDetails{
							{
								Info: []byte(fmt.Sprintf(`{
  "client_id": 92698540,
  "event_time": "2021-03-23T17:01:00+00:00",
  "transaction_id": "payout/taxi/92698540/2021-03-23T20:01:00+03:00",
  "tariffer_payload": {
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
	}

	ps := NewPaymentSender(s.ctx, &namedWriters, SenderWithDB(s.pDB))
	ps.flags.syncCPF = false

	_, resp, _, _, err := ps.getBalanceWithLock(p)
	require.NoError(s.T(), err)
	contractID := strconv.FormatInt(p.ContractID, 10)
	invoices := ps.parseCPF(resp, contractID)
	require.Len(s.T(), invoices, 0)
}

// TestSendOneWithCPF проверяет, что при обработке новой выплаты в БД вставляется информация о CPF
func (s *OEBSGateTestSuite) TestSendOneWithCPF() {
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

	// Успешная запись в LB
	lbWriters := logbroker.NewEmptyWriters()
	lbWriters.SetWriter(false, false, &logbroker.MockLogBrokerWriter{Err: ErrOooops})
	lbWriters.SetWriter(true, false, &logbroker.MockLogBrokerWriter{})
	namedWriters := logbroker.NamedWriters{"taxi": lbWriters}

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
								Info: []byte(fmt.Sprintf(`{
  "client_id": 92698540,
  "event_time": "2021-03-23T17:01:00+00:00",
  "transaction_id": "payout/taxi/92698540/2021-03-23T20:01:00+03:00",
  "tariffer_payload": {
	"dry_run": true,
	"common_ts": 1616518860,
	"contract_states": {
	  "invoices": {
  		"%d": [
			{"id": 1234, "operation_type": "INSERT", "external_id": "ЛСТ-2343434-1", "amount": 666.66},
			{"id": 1233, "operation_type": "UPSERT", "external_id": "ЛСТ-2343434-2", "amount": 222.22}
		]
      },
	  "nettings": {
	  	"2955592": {
		  "netting_done": true,
   		  "payment_amount": 542.76,
   		  "payment_currency": "RUB"
   	    },
   	    "%d": {
   		  "netting_done": true,
   		  "payment_amount": 220,
   		  "payment_currency": "RUB"
   	    },
   	    "2955594": {
   		  "netting_done": true,
   		  "payment_amount": 0,
   		  "payment_currency": "RUB"
 		}
	  }
	}
  }
}`, p.ContractID, p.ContractID)),
							},
						},
					},
				},
			}, nil
		},
	}
	// Успешная запись в ЛБ
	_, err = s.sendOneTxX(p, &namedWriters, true)
	assert.NoError(s.T(), err)

	// проверяем, что платеж в статусе pending
	np, err := payout.Get(s.ctx, p.ID)
	assert.NoError(s.T(), err)
	assert.Equal(s.T(), payout.StatusPending, np.Status)
	// т.к. осн. статус поменялся, то статус синхронизации
	// должен в начальное состояние перейти - new
	assert.Equal(s.T(), payout.SyncStatusNew, np.SyncStatus)

	// Проверяем, что создались CPF записи
	where := core.WhereDesc{
		{Name: cpf.PayoutIDCol, Value: p.ID},
	}
	cpfs, err := cpf.Get(s.ctx, where, true)
	require.NoError(s.T(), err)
	require.Len(s.T(), cpfs, 2)
	testCPF := func(OpType string, Status string, ExtID string, amount string, dry_run bool, c cpf.CPF) {
		require.Equal(s.T(), OpType, c.OpType)
		require.Equal(s.T(), Status, c.Status)
		require.Equal(s.T(), ExtID, c.ExtID)
		require.Equal(s.T(), amount, c.Amount.String())
		require.Equal(s.T(), dry_run, c.DryRun)
	}
	testCPF("INSERT", cpf.StatusNew, "ЛСТ-2343434-1", "666.66", true, cpfs[0])
	testCPF("UPSERT", cpf.StatusNew, "ЛСТ-2343434-2", "222.22", true, cpfs[1])
}

// TestCPFIncrement проверяет, что новые CPF записи вставляются с инкрементом 10
func (s *OEBSGateTestSuite) TestCPFIncrement() {
	size := 5
	facts := make([]*cpf.CPF, size)

	tx := s.Tx()
	for i := range facts {
		p, _, err := s.createRandomPayoutWithRequest()
		require.NoError(s.T(), err)
		facts[i], err = cpf.CreateTx(s.ctx, tx, &cpf.NewCPF{
			PayoutID: p.ID,
			OpType:   "INSERT1",
			ExtID:    "ЛСТ-1",
			Amount:   decimal.RequireFromString("123.567"),
		})
		require.NoError(s.T(), err)
	}
	// в бд эти данные все равно не нужны
	require.NoError(s.T(), tx.Rollback())

	for i := range facts {
		if i > 0 {
			// шаг между значениями - 10
			require.Equal(s.T(), facts[i-1].ID+10, facts[i].ID)
			// оканчиваться должны на 2. Договоренность с OEBS
			require.EqualValues(s.T(), 2, facts[i-1].ID%10)
			require.EqualValues(s.T(), 2, facts[i].ID%10)
		}
	}
}

func (s *OEBSGateTestSuite) TestNewProcessor() {
	err := os.Unsetenv("PAYOUT_OEBS_GATE_INTERVAL")
	require.NoError(s.T(), err)

	lbWriters := logbroker.NewEmptyWriters()
	lbWriters.SetWriter(false, false, &logbroker.MockLogBrokerWriter{})
	lbWriters.SetWriter(true, false, &logbroker.MockLogBrokerWriter{Err: ErrOooops})
	namedWriters := logbroker.NamedWriters{"taxi": lbWriters}

	p := NewPaymentSender(s.ctx, &namedWriters, SenderWithDB(s.pDB))
	require.EqualValues(s.T(), 500, p.limit)
	require.Equal(s.T(), 5*time.Minute, p.interval)

	err = os.Setenv("PAYOUT_SENDER_LIMIT", "10")
	require.NoError(s.T(), err)
	err = os.Setenv("PAYOUT_OEBS_GATE_INTERVAL", "1m")
	require.NoError(s.T(), err)

	p1 := NewPaymentSender(s.ctx, &namedWriters, SenderWithDB(s.pDB))
	require.EqualValues(s.T(), 10, p1.limit)
	require.Equal(s.T(), 1*time.Minute, p1.interval)
}

// TestSendOneWithNewNetting проверяет, что при отправке выплаты мы сохраняем связь с ВЗ
// (ВЗ только новые)
func (s *OEBSGateTestSuite) TestSendOneWithNewNetting() {
	p, _, err := s.createRandomPayoutWithRequest()
	assert.NoError(s.T(), err)

	lockID := "6666"
	// Успешное снятие блокировки в счетах
	s.ctx.Clients.AccountsLocker = &MockAccounterLocker{
		TestRemove: func(ctx context.Context, attributes *entities.LocationAttributes, uid string) {
			assert.Equal(s.T(), lockID, uid)
		},
	}

	// Успешная запись в LB
	lbWriters := logbroker.NewEmptyWriters()
	lbWriters.SetWriter(false, false, &logbroker.MockLogBrokerWriter{})
	lbWriters.SetWriter(true, false, &logbroker.MockLogBrokerWriter{Err: ErrOooops})
	namedWriters := logbroker.NamedWriters{"taxi": lbWriters}

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
								Info: []byte(`{
 "client_id": 92698540,
 "event_time": "2021-03-23T17:01:00+00:00",
 "transaction_id": "payout/taxi/92698540/2021-03-23T20:01:00+03:00",
 "tariffer_payload": {
	"dry_run": false
 }
}`),
							},
							{
								EventID:   "25461",
								EventType: reservePayoutEvent,
								Info: []byte(`{
 "client_id": 92698540,
 "event_time": "2021-03-23T17:01:00+00:00",
 "transaction_id": "payout/taxi/92698540/2021-03-23T20:01:00+03:00",
 "tariffer_payload": {
	"dry_run": false
 }
}`),
							},
							{
								EventID:   "payout/taxi/11059644/2021-05-31T11:00:00+00:00",
								EventType: "taxi:payout",
								Info: []byte(`{
 "client_id": 92698540,
 "event_time": "2021-03-23T17:01:00+00:00",
 "transaction_id": "payout/taxi/92698540/2021-03-23T20:01:00+03:00",
 "tariffer_payload": {
	"dry_run": false
 }
}`),
							},
						},
					},
				},
			}, nil
		},
	}
	_, err = s.sendOneTxXX(p, &namedWriters, true, true)
	assert.NoError(s.T(), err)

	// проверяем, что платеж в статусе pending
	np, err := payout.Get(s.ctx, p.ID)
	assert.NoError(s.T(), err)
	assert.Equal(s.T(), payout.StatusPending, np.Status)
	// т.к. осн. статус поменялся, то статус синхронизации
	// должен в начальное состояние перейти - new
	assert.Equal(s.T(), payout.SyncStatusNew, np.SyncStatus)

	// Проверяем, что создались CPF не создали, т.к. их не было в событии
	where := core.WhereDesc{
		{Name: cpf.PayoutIDCol, Value: p.ID},
	}
	cpfs, err := cpf.Get(s.ctx, where, true)
	require.NoError(s.T(), err)
	require.Len(s.T(), cpfs, 0)

	// Проверяем, что создали связь с ВЗ
	where = core.WhereDesc{
		{Name: netting.PayoutIDCol, Value: p.ID},
	}
	ns, err := netting.Get(s.ctx, where)
	require.NoError(s.T(), err)
	require.Len(s.T(), ns, 2)

	prefix := "payout/taxi/11059644/2021-05"
	for i := 0; i < 2; i++ {
		require.Equal(s.T(), ns[i].ClientID, p.ClientID)
		require.Equal(s.T(), ns[i].ContractID, p.ContractID)
		require.Equal(s.T(), ns[i].ServiceID, p.ServiceID)
		require.Equal(s.T(), ns[i].PayoutID, p.ID)
		require.Equal(s.T(), ns[i].Status, netting.StatusNew)
		require.True(s.T(), strings.HasPrefix(ns[i].ExtID, prefix))
	}
}

// TestSendOneWithOldNetting проверяет, что при отправке выплаты мы сохраняем связь с ВЗ
// (ВЗ не только новые, но и старые)
func (s *OEBSGateTestSuite) TestSendOneWithOldNetting() {
	p, _, err := s.createRandomPayoutWithRequest()
	assert.NoError(s.T(), err)

	lockID := "6666"
	// Успешное снятие блокировки в счетах
	s.ctx.Clients.AccountsLocker = &MockAccounterLocker{
		TestRemove: func(ctx context.Context, attributes *entities.LocationAttributes, uid string) {
			assert.Equal(s.T(), lockID, uid)
		},
	}

	// Успешная запись в LB
	lbWriters := logbroker.NewEmptyWriters()
	lbWriters.SetWriter(false, false, &logbroker.MockLogBrokerWriter{})
	lbWriters.SetWriter(true, false, &logbroker.MockLogBrokerWriter{Err: ErrOooops})
	namedWriters := logbroker.NamedWriters{"taxi": lbWriters}

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
								Info: []byte(`{
 "client_id": 92698540,
 "event_time": "2021-03-23T17:01:00+00:00",
 "transaction_id": "payout/taxi/92698540/2021-03-23T20:01:00+03:00",
 "tariffer_payload": {
	"dry_run": false
 }
}`),
							},
						},
					},
				},
			}, nil
		},
	}

	// Связи с отмененной выплатой (предполагаем, что пришла отмена из АРД)
	tx := s.Tx()
	oldNetting := "payout/taxi/11059604/2021-05-22T11:00:00+00:00"
	_, err = netting.CreateTx(s.ctx, tx, &netting.NewNetting{
		PayoutID:   int64(-1),
		ExtID:      oldNetting,
		ServiceID:  p.ServiceID,
		ContractID: p.ContractID,
		ClientID:   p.ClientID,
	})
	require.NoError(s.T(), err)
	where := core.WhereDesc{
		{Name: netting.PayoutIDCol, Value: int64(-1)},
	}
	update := core.UpdateDesc{
		{Name: netting.StatusCol, Value: netting.StatusError},
	}
	require.NoError(s.T(), netting.UpdateTx(s.ctx, tx, where, update))
	require.NoError(s.T(), tx.Commit())

	_, err = s.sendOneTxXX(p, &namedWriters, true, true)
	require.NoError(s.T(), err)

	// проверяем, что платеж в статусе pending
	np, err := payout.Get(s.ctx, p.ID)
	assert.NoError(s.T(), err)
	assert.Equal(s.T(), payout.StatusPending, np.Status)
	// т.к. осн. статус поменялся, то статус синхронизации
	// должен в начальное состояние перейти - new
	assert.Equal(s.T(), payout.SyncStatusNew, np.SyncStatus)

	// Проверяем, что создались CPF не создали, т.к. их не было в событии
	where = core.WhereDesc{
		{Name: cpf.PayoutIDCol, Value: p.ID},
	}
	cpfs, err := cpf.Get(s.ctx, where, true)
	require.NoError(s.T(), err)
	require.Len(s.T(), cpfs, 0)

	// Проверяем, что создали связь с ВЗ
	where = core.WhereDesc{
		{Name: netting.PayoutIDCol, Value: p.ID},
	}
	ns, err := netting.Get(s.ctx, where)
	require.NoError(s.T(), err)
	require.Len(s.T(), ns, 2)

	prefix := "payout/taxi/11059644/2021-05"
	for i := 0; i < 2; i++ {
		require.Equal(s.T(), ns[i].ClientID, p.ClientID)
		require.Equal(s.T(), ns[i].ContractID, p.ContractID)
		require.Equal(s.T(), ns[i].ServiceID, p.ServiceID)
		require.Equal(s.T(), ns[i].PayoutID, p.ID)
		require.Equal(s.T(), ns[i].Status, netting.StatusNew)
		require.True(s.T(), strings.HasPrefix(ns[i].ExtID, prefix) || strings.HasPrefix(ns[i].ExtID, oldNetting))
	}
}

// TestSendOneZeroPayout проверяем, что в случае нулевой выплаты пишем сразу подтверждение
func (s *OEBSGateTestSuite) TestSendOneZeroPayout() {
	p, err := s.createRandomPayout()
	require.NoError(s.T(), err)
	require.NoError(s.T(), payout.Update(s.ctx, p.ID, payout.AmountCol, decimal.Zero))
	p.Amount = decimal.Zero
	lockID := "6666"

	// init mocks
	zpr := &logbroker.MockLogBrokerWriter{}
	lbWriters := logbroker.NewEmptyWriters()
	lbWriters.SetWriter(false, false, &logbroker.MockLogBrokerWriter{Err: ErrOooops})
	lbWriters.SetWriter(true, false, &logbroker.MockLogBrokerWriter{Err: ErrOooops})
	lbWriters.SetWriter(false, true, zpr)
	namedWriters := logbroker.NamedWriters{"taxi": lbWriters}

	s.ctx.Clients.AccountsLocker = &MockAccounterLocker{
		TestRemove: func(ctx context.Context, attributes *entities.LocationAttributes, uid string) {
			assert.Equal(s.T(), lockID, uid)
		},
	}
	s.ctx.Clients.AccountsBatch = &MockAccountBatch{
		ReadFn: func(r *entities.BatchReadRequest) (*entities.BatchReadResponse, error) {
			return &entities.BatchReadResponse{
				Balances: []entities.BalanceAttributesDt{
					{
						Debit:  "0",
						Credit: "0",
					},
				},
				Locks: []entities.LockAttributes{
					{
						UID: lockID,
					},
				},
			}, nil
		},
	}

	ps := NewPaymentSender(s.ctx, &namedWriters, SenderWithDB(s.pDB))
	ps.flags.syncCPF = true
	tx := s.Tx()
	_, err = ps.sendOne(p, tx)
	require.NoError(s.T(), err)
	require.NoError(s.T(), tx.Commit())

	require.Len(s.T(), zpr.Messages, 1)
	resp, err := parseArdResponse(zpr.Messages[0])
	require.NoError(s.T(), err)
	require.Equal(s.T(), p.ID, resp.MessageID)
}

// TestNoAccountBalance проверяем, что корректно обрабатываем случай, когда система счетов не отдала балансы
func (s *OEBSGateTestSuite) TestNoAccountBalance() {
	p, _, err := s.createRandomPayoutWithRequest()
	require.NoError(s.T(), err)
	lockID := "6666"

	// Успешное снятие блокировки в счетах
	s.ctx.Clients.AccountsLocker = &MockAccounterLocker{
		TestRemove: func(ctx context.Context, attributes *entities.LocationAttributes, uid string) {
			assert.Equal(s.T(), lockID, uid)
		},
	}

	// Нет средств на счете
	s.ctx.Clients.AccountsBatch = &MockAccountBatch{
		ReadFn: func(r *entities.BatchReadRequest) (*entities.BatchReadResponse, error) {
			return &entities.BatchReadResponse{
				Balances: []entities.BalanceAttributesDt{},
				Locks: []entities.LockAttributes{
					{
						UID: lockID,
					},
				},
			}, nil
		},
	}

	lbWriters := logbroker.NewEmptyWriters()
	lbWriters.SetWriter(false, false, &logbroker.MockLogBrokerWriter{})
	lbWriters.SetWriter(true, false, &logbroker.MockLogBrokerWriter{Err: ErrOooops})
	namedWriters := logbroker.NamedWriters{"taxi": lbWriters}

	blocking, err := s.sendOneTx(p, &namedWriters)
	require.Error(s.T(), err)
	require.False(s.T(), blocking)
	require.Equal(s.T(), ErrNoAccountBalances, err)

	np, err := payout.Get(s.ctx, p.ID)
	require.NoError(s.T(), err)

	require.Equal(s.T(), payout.StatusNew, np.Status)
	require.Equal(s.T(), payout.SyncStatusNew, np.SyncStatus)
}

// TestPayoutDryRun проверяет, что признак dry_run в ответе из системы счетов
// парсится и кладется в таблицу t_payout
func (s *OEBSGateTestSuite) TestPayoutDryRun() {
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

	// Успешная запись в LB (dry_run)
	lbWriters := logbroker.NewEmptyWriters()
	lbWriters.SetWriter(false, false, &logbroker.MockLogBrokerWriter{Err: ErrOooops})
	lbWriters.SetWriter(true, false, &logbroker.MockLogBrokerWriter{})
	namedWriters := logbroker.NamedWriters{"taxi": lbWriters}

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
	}
	// Успешная запись в ЛБ
	_, err = s.sendOneTx(p, &namedWriters)
	assert.NoError(s.T(), err)

	// проверяем, что платеж в статусе pending
	np, err := payout.Get(s.ctx, p.ID)
	assert.NoError(s.T(), err)
	assert.Equal(s.T(), payout.StatusPending, np.Status)
	// проверяем, что распарсили и положили в таблицу признак dry_run
	assert.Equal(s.T(), true, np.DryRun)
}

// TestPayoutDryRunNoEvents проверяет, что выплата отклоняется,
// если в ответе из системы счетов нет событий
func (s *OEBSGateTestSuite) TestPayoutDryRunNoEvents() {
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

	// Успешная запись в LB
	lbWriters := logbroker.NewEmptyWriters()
	lbWriters.SetWriter(false, false, &logbroker.MockLogBrokerWriter{})
	lbWriters.SetWriter(true, false, &logbroker.MockLogBrokerWriter{Err: ErrOooops})
	namedWriters := logbroker.NamedWriters{"taxi": lbWriters}

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
						Events: []entities.EventDetails{},
					},
				},
			}, nil
		},
	}
	blocking, err := s.sendOneTx(p, &namedWriters)
	assert.Equal(s.T(), err.Error(), "no events found")
	assert.False(s.T(), blocking)
	// проверяем, что выплата в статусе rejected
	np, err := payout.Get(s.ctx, p.ID)
	assert.NoError(s.T(), err)
	assert.Equal(s.T(), payout.StatusRejected, np.Status)
}

// TestPayoutDryRunDifferentEvents проверяет, что выплата отклоняется,
// если в разных события из системы счетов разный признак dry_run
func (s *OEBSGateTestSuite) TestPayoutDryRunDifferentEvents() {
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

	// Успешная запись в LB
	lbWriters := logbroker.NewEmptyWriters()
	lbWriters.SetWriter(false, false, &logbroker.MockLogBrokerWriter{})
	lbWriters.SetWriter(true, false, &logbroker.MockLogBrokerWriter{Err: ErrOooops})
	namedWriters := logbroker.NamedWriters{"taxi": lbWriters}
	// имитируем получение двух событий: одно с dry_run=true,
	// другое с dry_run=false
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
							{
								EventID:   "payout/taxi/11059644/2021-05-27T11:00:00+00:00",
								EventType: "taxi:payout",
								Info: []byte(`{
 "client_id": 92698540,
 "event_time": "2021-03-23T17:01:00+00:00",
 "transaction_id": "payout/taxi/92698540/2021-03-23T20:01:00+03:00",
 "tariffer_payload": {
	"dry_run": false
 }
}`),
							},
						},
					},
				},
			}, nil
		},
	}
	blocking, err := s.sendOneTx(p, &namedWriters)
	assert.Equal(s.T(), err.Error(), "dry_run attribute differs in events")
	assert.False(s.T(), blocking)
	// проверяем, что выплата в статусе rejected
	np, err := payout.Get(s.ctx, p.ID)
	assert.NoError(s.T(), err)
	assert.Equal(s.T(), payout.StatusRejected, np.Status)
}

// TestPayoutDryRunFlagFalse проверяет, что выплата не отклоняется,
// если выставлен флаг PAYOUT_PROCESS_DRY_RUN=false
func (s *OEBSGateTestSuite) TestPayoutDryRunFlagFalse() {
	s.SetProcessDryRun(false)
	p, _, err := s.createRandomPayoutWithRequest()
	assert.NoError(s.T(), err)

	lockID := "6666"
	// Успешное снятие блокировки в счетах
	s.ctx.Clients.AccountsLocker = &MockAccounterLocker{
		TestRemove: func(ctx context.Context, attributes *entities.LocationAttributes, uid string) {
			assert.Equal(s.T(), lockID, uid)
		},
	}

	// Успешная запись в LB
	lbWriters := logbroker.NewEmptyWriters()
	lbWriters.SetWriter(false, false, &logbroker.MockLogBrokerWriter{Err: ErrOooops})
	lbWriters.SetWriter(true, false, &logbroker.MockLogBrokerWriter{})
	namedWriters := logbroker.NamedWriters{"taxi": lbWriters}
	// имитируем получение ответа из системы счетов без событий
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
						Events: []entities.EventDetails{},
					},
				},
			}, nil
		},
	}
	_, err = s.sendOneTx(p, &namedWriters)
	assert.NoError(s.T(), err)
	// проверяем, что выплата в статусе Pending
	np, err := payout.Get(s.ctx, p.ID)
	assert.NoError(s.T(), err)
	assert.Equal(s.T(), payout.StatusPending, np.Status)
}

func (s *OEBSGateTestSuite) TestNamespaceTopics() {
	s.SetProcessDryRun(true)

	pBNPL, err := payout.Create(s.ctx, &payout.NewPayout{
		ExternalID: bt.RandS(30),
		ServiceID:  100,
		ContractID: bt.RandN64(),
		Amount:     decimal.RequireFromString("124.666"),
		Currency:   "RUB",
		Namespace:  "BNPL",
	})
	assert.NoError(s.T(), err)

	lockID := "6666"
	// Успешное снятие блокировки в счетах
	s.ctx.Clients.AccountsLocker = &MockAccounterLocker{
		TestRemove: func(ctx context.Context, attributes *entities.LocationAttributes, uid string) {
			assert.Equal(s.T(), lockID, uid)
		},
	}

	// мокаем только dry_run писателей, т.к. прописываем атрибут dry_run в ответе из системы счетов
	// не должны ходить в топик такси
	lbwritersTaxi := logbroker.NewEmptyWriters()
	lbwritersTaxi.SetWriter(true, false, &logbroker.MockLogBrokerWriter{Err: ErrOooops})
	lbwritersBNPL := logbroker.NewEmptyWriters()
	lbwritersBNPL.SetWriter(true, false, &logbroker.MockLogBrokerWriter{})
	namedWriters := logbroker.NamedWriters{
		"taxi": lbwritersTaxi,
		"BNPL": lbwritersBNPL,
	}

	// имитируем получение ответа из системы счетов без событий
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
}`, pBNPL.ContractID)),
							},
						},
					},
				},
			}, nil
		},
	}
	_, err = s.sendOneTx(pBNPL, &namedWriters)
	assert.NoError(s.T(), err)
}

// TestSendNew проверяет обработку батча в горутинах
func (s *OEBSGateTestSuite) TestSendNew() {
	lockID := "6666"
	// Успешное снятие блокировки в счетах
	s.ctx.Clients.AccountsLocker = &MockAccounterLocker{
		TestRemove: func(ctx context.Context, attributes *entities.LocationAttributes, uid string) {
			assert.Equal(s.T(), lockID, uid)
		},
	}

	lbwritersTaxi := logbroker.NewEmptyWriters()
	lbwritersTaxi.SetWriter(true, false, &logbroker.MockLogBrokerWriter{})
	namedWriters := logbroker.NamedWriters{
		"taxi": lbwritersTaxi,
	}

	ps := NewPaymentSender(s.ctx, &namedWriters, SenderWithDB(s.pDB))
	// размер батча
	ps.limit = 2

	// создаем две выплаты, делаем их самыми старыми (самые маленькие ID)
	p1, err := payout.Create(s.ctx, &payout.NewPayout{
		ExternalID: bt.RandS(30),
		ServiceID:  100,
		ContractID: bt.RandN64(),
		Amount:     decimal.RequireFromString("123.45"),
		Currency:   "RUB",
		Namespace:  "taxi",
		ClientID:   11,
	})
	require.NoError(s.T(), err)

	p2, err := payout.Create(s.ctx, &payout.NewPayout{
		ExternalID: bt.RandS(30),
		ServiceID:  100,
		ContractID: bt.RandN64(),
		Amount:     decimal.RequireFromString("123.45"),
		Currency:   "RUB",
		Namespace:  "taxi",
		ClientID:   22,
	})
	require.NoError(s.T(), err)
	require.Equal(s.T(), p1.ServiceID, p2.ServiceID)
	update := core.UpdateDesc{
		{Name: payout.IDCol, Value: -2},
	}
	err = payout.UpdateX(s.ctx, p1.ID, update)
	require.NoError(s.T(), err)
	defer s.resetPaymentID(p1.ID, -2)

	update = core.UpdateDesc{
		{Name: payout.IDCol, Value: -1},
	}
	err = payout.UpdateX(s.ctx, p2.ID, update)
	require.NoError(s.T(), err)
	defer s.resetPaymentID(p2.ID, -1)

	// ответ от системы счетов
	s.ctx.Clients.AccountsBatch = &MockAccountBatch{
		// Деньги есть на счете
		ReadFn: func(r *entities.BatchReadRequest) (*entities.BatchReadResponse, error) {
			return &entities.BatchReadResponse{
				Balances: []entities.BalanceAttributesDt{
					{
						Debit:  "0",
						Credit: "123.45",
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
	}`, 0)),
							},
						},
					},
				},
			}, nil
		},
	}

	err = ps.sendNew(time.Now())
	require.NoError(s.T(), err)

	// достаем выплаты, проверяем, что статус изменился, а сумма -- нет
	updated1, err := payout.Get(s.ctx, -2)
	require.NoError(s.T(), err)
	updated2, err := payout.Get(s.ctx, -1)
	require.NoError(s.T(), err)
	require.Equal(s.T(), payout.StatusPending, updated1.Status)
	require.Equal(s.T(), p1.Amount, updated1.Amount)
	require.Equal(s.T(), payout.StatusPending, updated2.Status)
	require.Equal(s.T(), p2.Amount, updated2.Amount)
}

// TestSendNewErrorBlocking проверяет, что sendNew возвращает ошибку,
// если sendOne вернул ошибку с blocking
func (s *OEBSGateTestSuite) TestSendNewErrorBlocking() {
	lockID := "6666"
	// Успешное снятие блокировки в счетах
	s.ctx.Clients.AccountsLocker = &MockAccounterLocker{
		TestRemove: func(ctx context.Context, attributes *entities.LocationAttributes, uid string) {
			require.Equal(s.T(), lockID, uid)
		},
	}

	lbwritersTaxi := logbroker.NewEmptyWriters()
	lbwritersTaxi.SetWriter(false, false, &logbroker.MockLogBrokerWriter{Err: ErrOooops})
	namedWriters := logbroker.NamedWriters{
		"taxi": lbwritersTaxi,
	}

	ps := NewPaymentSender(s.ctx, &namedWriters, SenderWithDB(s.pDB))
	// размер батча
	ps.limit = 2
	// создаем две выплаты, делаем их самыми старыми (самые маленькие ID)
	p1, err := payout.Create(s.ctx, &payout.NewPayout{
		ExternalID: bt.RandS(30),
		ServiceID:  100,
		ContractID: bt.RandN64(),
		Amount:     decimal.RequireFromString("777.99"),
		Currency:   "RUB",
		Namespace:  "taxi",
		ClientID:   15,
	})
	require.NoError(s.T(), err)

	p2, err := payout.Create(s.ctx, &payout.NewPayout{
		ExternalID: bt.RandS(30),
		ServiceID:  100,
		ContractID: bt.RandN64(),
		Amount:     decimal.RequireFromString("999.77"),
		Currency:   "RUB",
		Namespace:  "taxi",
		ClientID:   100,
	})
	require.NoError(s.T(), err)
	require.Equal(s.T(), p1.ServiceID, p2.ServiceID)
	update := core.UpdateDesc{
		{Name: payout.IDCol, Value: -2},
	}
	err = payout.UpdateX(s.ctx, p1.ID, update)
	require.NoError(s.T(), err)
	defer s.resetPaymentID(p1.ID, -2)

	update = core.UpdateDesc{
		{Name: payout.IDCol, Value: -1},
	}
	err = payout.UpdateX(s.ctx, p2.ID, update)
	require.NoError(s.T(), err)
	defer s.resetPaymentID(p2.ID, -1)

	// ответ от системы счетов
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
		"dry_run": false,
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
	}`, 0)),
							},
						},
					},
				},
			}, nil
		},
	}

	err = ps.sendNew(time.Now())
	require.Error(s.T(), err)
	require.Equal(s.T(), ErrOooops, err)

	// достаем выплаты, проверяем, что статус не изменился, т.к. вернули ошибку
	updated1, err := payout.Get(s.ctx, -2)
	require.NoError(s.T(), err)
	updated2, err := payout.Get(s.ctx, -1)
	require.NoError(s.T(), err)
	require.Equal(s.T(), payout.StatusNew, updated1.Status)
	require.Equal(s.T(), payout.StatusNew, updated2.Status)
}

// TestSendNewErrorNonBlocking проверяет, что sendNew не возвращает ошибку,
// если она не блокирующая (sendOne вернул blocking=false)
func (s *OEBSGateTestSuite) TestSendNewErrorNonBlocking() {
	lockID := "12341"
	// Успешное снятие блокировки в счетах
	s.ctx.Clients.AccountsLocker = &MockAccounterLocker{
		TestRemove: func(ctx context.Context, attributes *entities.LocationAttributes, uid string) {
			require.Equal(s.T(), lockID, uid)
		},
	}

	lbwritersTaxi := logbroker.Writers{}
	namedWriters := logbroker.NamedWriters{
		"taxi": &lbwritersTaxi,
	}
	n, _, err := s.createNotifier()
	require.NoError(s.T(), err)
	ps := NewPaymentSender(s.ctx, &namedWriters, SenderWithDB(s.pDB), SenderWithNotifier(n))
	// размер батча
	ps.limit = 1
	// создаем две выплаты, делаем их самыми старыми (самые маленькие ID)
	p1, err := payout.Create(s.ctx, &payout.NewPayout{
		ExternalID: bt.RandS(30),
		ServiceID:  100,
		ContractID: bt.RandN64(),
		Amount:     decimal.RequireFromString("777.99"),
		Currency:   "RUB",
		Namespace:  "taxi",
		ClientID:   15,
	})
	require.NoError(s.T(), err)

	update := core.UpdateDesc{
		{Name: payout.IDCol, Value: -2},
	}
	err = payout.UpdateX(s.ctx, p1.ID, update)
	require.NoError(s.T(), err)
	defer s.resetPaymentID(p1.ID, -2)

	// ответ от системы счетов
	s.ctx.Clients.AccountsBatch = &MockAccountBatch{
		// Деньги есть на счете
		ReadFn: func(r *entities.BatchReadRequest) (*entities.BatchReadResponse, error) {
			return &entities.BatchReadResponse{
				Balances: []entities.BalanceAttributesDt{
					{
						Debit:  "20.0",
						Credit: "0",
					},
				},
				Locks: []entities.LockAttributes{
					{
						UID: lockID,
					},
				},
			}, nil
		},
	}

	err = ps.sendNew(time.Now())
	require.NoError(s.T(), err)

	// достаем выплаты, проверяем, что статус не изменился, т.к. внутри ошибка
	np, err := payout.Get(s.ctx, -2)
	require.NoError(s.T(), err)
	require.Equal(s.T(), payout.StatusRejected, np.Status)
	require.Equal(s.T(), payout.SyncStatusDB, np.SyncStatus)
}

// TestNoNamespace проверяет, что если у выплаты неизвестный нам namespace
// мы ничего не отправляем в LB, оставим ее в статусе new, но сохраним SyncStatus
func (s *OEBSGateTestSuite) TestNoNamespace() {
	s.SetProcessDryRun(true)

	p, err := payout.Create(s.ctx, &payout.NewPayout{
		ExternalID: bt.RandS(30),
		ServiceID:  100,
		ContractID: bt.RandN64(),
		Amount:     decimal.RequireFromString("124.666"),
		Currency:   "RUB",
		Namespace:  "BNPL",
	})
	assert.NoError(s.T(), err)

	lockID := "6666"
	// Успешное снятие блокировки в счетах
	s.ctx.Clients.AccountsLocker = &MockAccounterLocker{
		TestRemove: func(ctx context.Context, attributes *entities.LocationAttributes, uid string) {
			assert.Equal(s.T(), lockID, uid)
		},
	}

	// мокаем только dry_run писателей, т.к. прописываем атрибут dry_run в ответе из системы счетов
	// не должны ходить в топик такси
	lbwritersTaxi := logbroker.NewEmptyWriters()
	lbwritersTaxi.SetWriter(true, false, &logbroker.MockLogBrokerWriter{Err: ErrOooops})

	// Не создаем писателя для BNPL
	namedWriters := logbroker.NamedWriters{
		"taxi": lbwritersTaxi,
	}

	// имитируем получение ответа из системы счетов без событий
	s.ctx.Clients.AccountsBatch = &MockAccountBatch{
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
	}
	_, err = s.sendOneTx(p, &namedWriters)
	assert.NoError(s.T(), err)
	pn, err := payout.Get(s.ctx, p.ID)
	assert.NoError(s.T(), err)
	// проверяем, что выплата осталась в статусе New, а SyncStatus обновился
	assert.Equal(s.T(), payout.StatusNew, pn.Status)
	assert.Equal(s.T(), payout.SyncStatusAcc, pn.SyncStatus)
}

// TestNoNamespaceZeroPayout проверяет, что если у нулевой выплаты неизвестный нам namespace
// мы ничего не отправляем в LB, оставим ее в статусе new, но сохраним SyncStatus
func (s *OEBSGateTestSuite) TestNoNamespaceZeroPayout() {
	s.SetProcessDryRun(true)

	// создаем нулевую выплату
	p, err := payout.Create(s.ctx, &payout.NewPayout{
		ExternalID: bt.RandS(30),
		ServiceID:  100,
		ContractID: bt.RandN64(),
		Amount:     decimal.RequireFromString("0.0"),
		Currency:   "RUB",
		Namespace:  "BNPL",
	})
	assert.NoError(s.T(), err)

	lockID := "6666"
	// Успешное снятие блокировки в счетах
	s.ctx.Clients.AccountsLocker = &MockAccounterLocker{
		TestRemove: func(ctx context.Context, attributes *entities.LocationAttributes, uid string) {
			assert.Equal(s.T(), lockID, uid)
		},
	}

	// мокаем только dry_run писателей, т.к. прописываем атрибут dry_run в ответе из системы счетов
	// не должны ходить в топик такси
	lbwritersTaxi := logbroker.NewEmptyWriters()
	lbwritersTaxi.SetWriter(true, false, &logbroker.MockLogBrokerWriter{Err: ErrOooops})

	// Не создаем писателя для BNPL
	namedWriters := logbroker.NamedWriters{
		"taxi": lbwritersTaxi,
	}

	// имитируем получение ответа из системы счетов без событий
	s.ctx.Clients.AccountsBatch = &MockAccountBatch{
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
	}
	_, err = s.sendOneTx(p, &namedWriters)
	assert.NoError(s.T(), err)
	pn, err := payout.Get(s.ctx, p.ID)
	assert.NoError(s.T(), err)
	// проверяем, что выплата осталась в статусе New, а SyncStatus обновился
	assert.Equal(s.T(), payout.StatusNew, pn.Status)
	assert.Equal(s.T(), payout.SyncStatusAcc, pn.SyncStatus)
}

// TestDefaultDryRun проверяет, что если info пустое, dry_run=true
func (s *OEBSGateTestSuite) TestDefaultDryRun() {
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

	// Успешная запись в LB
	lbWriters := logbroker.NewEmptyWriters()
	lbWriters.SetWriter(false, false, &logbroker.MockLogBrokerWriter{Err: ErrOooops})
	lbWriters.SetWriter(true, false, &logbroker.MockLogBrokerWriter{})
	namedWriters := logbroker.NamedWriters{"taxi": lbWriters}

	var event entities.EventDetails

	require.NoError(s.T(), json.Unmarshal([]byte(`{
"event_id":   "payout/taxi/11059644/2021-05-27T11:00:00+00:00",
"event_type": "taxi:payout",
"info":      null
}`), &event))

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
							event,
						},
					},
				},
			}, nil
		},
	}
	// Успешная запись в ЛБ
	_, err = s.sendOneTx(p, &namedWriters)
	assert.NoError(s.T(), err)

	// проверем, что платеж в статусе pending
	np, err := payout.Get(s.ctx, p.ID)
	assert.NoError(s.T(), err)
	assert.Equal(s.T(), payout.StatusPending, np.Status)
	// т.к. осн. статус поменялся, то статус синхронизации
	// должен в начальное состояние перейти - new
	assert.Equal(s.T(), payout.SyncStatusNew, np.SyncStatus)
	assert.Equal(s.T(), true, np.DryRun)
}

// TestAllowedNs проверяет, что мы отклоняем выплату, если dry_run=false, и клиент не разрешен внутри namespace,
// а в остальных случаях -- обрабатываем ее
func (s *OEBSGateTestSuite) TestAllowedNs() {
	envVar := "{\"taxi\": [88005553535, 1234567], \"oplata\": []}"
	allowedNs, err := core.ParseAllowedNamespaces(s.ctx, envVar)
	assert.NoError(s.T(), err)

	s.SetAllowedNs(allowedNs)
	s.SetProcessDryRun(true)

	pTaxi, err := payout.Create(s.ctx, &payout.NewPayout{
		ExternalID: bt.RandS(30),
		ServiceID:  100,
		ContractID: bt.RandN64(),
		Amount:     decimal.RequireFromString("555.35"),
		Currency:   "RUB",
		Namespace:  "taxi",
		ClientID:   88005553535,
	})
	assert.NoError(s.T(), err)

	pTaxiRejected, err := payout.Create(s.ctx, &payout.NewPayout{
		ExternalID: bt.RandS(30),
		ServiceID:  100,
		ContractID: bt.RandN64(),
		Amount:     decimal.RequireFromString("555.35"),
		Currency:   "RUB",
		Namespace:  "taxi",
		ClientID:   98765,
	})
	assert.NoError(s.T(), err)

	pOplata, err := payout.Create(s.ctx, &payout.NewPayout{
		ExternalID: bt.RandS(30),
		ServiceID:  100,
		ContractID: bt.RandN64(),
		Amount:     decimal.RequireFromString("555.35"),
		Currency:   "RUB",
		Namespace:  "oplata",
		ClientID:   98765,
	})
	assert.NoError(s.T(), err)

	pBNPL, err := payout.Create(s.ctx, &payout.NewPayout{
		ExternalID: bt.RandS(30),
		ServiceID:  100,
		ContractID: bt.RandN64(),
		Amount:     decimal.RequireFromString("555.35"),
		Currency:   "RUB",
		Namespace:  "BNPL",
		ClientID:   98765,
	})
	assert.NoError(s.T(), err)

	lockID := "6666"
	// Успешное снятие блокировки в счетах
	s.ctx.Clients.AccountsLocker = &MockAccounterLocker{
		TestRemove: func(ctx context.Context, attributes *entities.LocationAttributes, uid string) {
			assert.Equal(s.T(), lockID, uid)
		},
	}

	lbWriters := logbroker.NewEmptyWriters()
	lbWriters.SetWriter(false, false, &logbroker.MockLogBrokerWriter{})
	lbWriters.SetWriter(true, false, &logbroker.MockLogBrokerWriter{Err: ErrOooops})

	namedWriters := logbroker.NamedWriters{"taxi": lbWriters, "BNPL": lbWriters}

	// ответ из системы счетов один на всех, dry_run=false
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
								Info: []byte(`{
  "client_id": 88005553535,
  "event_time": "2021-03-23T17:01:00+00:00",
  "transaction_id": "payout/taxi/92698540/2021-03-23T20:01:00+03:00",
  "tariffer_payload": {
	"dry_run": false
	}
}`),
							},
						},
					},
				},
			}, nil
		},
	}
	_, err = s.sendOneTx(pTaxi, &namedWriters)
	assert.NoError(s.T(), err)

	blocking, err := s.sendOneTx(pTaxiRejected, &namedWriters)
	assert.Error(s.T(), err)
	assert.False(s.T(), blocking)

	blocking, err = s.sendOneTx(pOplata, &namedWriters)
	assert.Error(s.T(), err)
	assert.False(s.T(), blocking)

	_, err = s.sendOneTx(pBNPL, &namedWriters)
	assert.NoError(s.T(), err)

	// выплата по разрешенному клиенту taxi в статусе pending
	np, err := payout.Get(s.ctx, pTaxi.ID)
	assert.NoError(s.T(), err)
	assert.Equal(s.T(), payout.StatusPending, np.Status)
	assert.Equal(s.T(), false, np.DryRun)

	// выплата по запрещенному клиенту taxi в статусе rejected
	np, err = payout.Get(s.ctx, pTaxiRejected.ID)
	assert.NoError(s.T(), err)
	assert.Equal(s.T(), payout.StatusRejected, np.Status)
	assert.Equal(s.T(), false, np.DryRun)

	// выплата по клиенту oplata в статусе rejected, т.к. список разрешенных клиентов пустой
	np, err = payout.Get(s.ctx, pOplata.ID)
	assert.NoError(s.T(), err)
	assert.Equal(s.T(), payout.StatusRejected, np.Status)
	assert.Equal(s.T(), false, np.DryRun)

	// выплата по клиенту BNPL в статусе pending, т.к. BNPl нет в списке namespace
	np, err = payout.Get(s.ctx, pBNPL.ID)
	assert.NoError(s.T(), err)
	assert.Equal(s.T(), payout.StatusPending, np.Status)
	s.SetAllowedNs(nil)
	assert.Equal(s.T(), false, np.DryRun)

}

// TestAllowedNsDryRun проверяет, что все dry_run выплаты обрабатываются
func (s *OEBSGateTestSuite) TestAllowedNsDryRun() {
	envVar := "{\"taxi\": [88005553535, 1234567], \"oplata\": []}"
	allowedNs, err := core.ParseAllowedNamespaces(s.ctx, envVar)
	assert.NoError(s.T(), err)

	s.SetAllowedNs(allowedNs)
	s.SetProcessDryRun(true)

	pTaxi, err := payout.Create(s.ctx, &payout.NewPayout{
		ExternalID: bt.RandS(30),
		ServiceID:  100,
		ContractID: bt.RandN64(),
		Amount:     decimal.RequireFromString("555.35"),
		Currency:   "RUB",
		Namespace:  "taxi",
		ClientID:   88005553535,
	})
	assert.NoError(s.T(), err)

	pTaxiRejected, err := payout.Create(s.ctx, &payout.NewPayout{
		ExternalID: bt.RandS(30),
		ServiceID:  100,
		ContractID: bt.RandN64(),
		Amount:     decimal.RequireFromString("555.35"),
		Currency:   "RUB",
		Namespace:  "taxi",
		ClientID:   98765,
	})
	assert.NoError(s.T(), err)

	pOplata, err := payout.Create(s.ctx, &payout.NewPayout{
		ExternalID: bt.RandS(30),
		ServiceID:  100,
		ContractID: bt.RandN64(),
		Amount:     decimal.RequireFromString("555.35"),
		Currency:   "RUB",
		Namespace:  "oplata",
		ClientID:   98765,
	})
	assert.NoError(s.T(), err)

	pBNPL, err := payout.Create(s.ctx, &payout.NewPayout{
		ExternalID: bt.RandS(30),
		ServiceID:  100,
		ContractID: bt.RandN64(),
		Amount:     decimal.RequireFromString("555.35"),
		Currency:   "RUB",
		Namespace:  "BNPL",
		ClientID:   98765,
	})
	assert.NoError(s.T(), err)

	lockID := "6666"
	// Успешное снятие блокировки в счетах
	s.ctx.Clients.AccountsLocker = &MockAccounterLocker{
		TestRemove: func(ctx context.Context, attributes *entities.LocationAttributes, uid string) {
			assert.Equal(s.T(), lockID, uid)
		},
	}

	lbWriters := logbroker.NewEmptyWriters()
	lbWriters.SetWriter(false, false, &logbroker.MockLogBrokerWriter{Err: ErrOooops})
	lbWriters.SetWriter(true, false, &logbroker.MockLogBrokerWriter{})
	namedWriters := logbroker.NamedWriters{"taxi": lbWriters, "oplata": lbWriters, "BNPL": lbWriters}

	// ответ из системы счетов один на всех, dry_run true, когда info пустое
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
								Info:      []byte(``),
							},
						},
					},
				},
			}, nil
		},
	}

	_, err = s.sendOneTx(pTaxi, &namedWriters)
	assert.NoError(s.T(), err)
	_, err = s.sendOneTx(pTaxiRejected, &namedWriters)
	assert.NoError(s.T(), err)
	_, err = s.sendOneTx(pOplata, &namedWriters)
	assert.NoError(s.T(), err)
	_, err = s.sendOneTx(pBNPL, &namedWriters)
	assert.NoError(s.T(), err)

	// проверяем, что все выплаты в статусе pending, а dry_run = true
	np, err := payout.Get(s.ctx, pTaxi.ID)
	assert.NoError(s.T(), err)
	assert.Equal(s.T(), payout.StatusPending, np.Status)
	assert.Equal(s.T(), true, np.DryRun)

	np, err = payout.Get(s.ctx, pTaxiRejected.ID)
	assert.NoError(s.T(), err)
	assert.Equal(s.T(), payout.StatusPending, np.Status)
	assert.Equal(s.T(), true, np.DryRun)

	np, err = payout.Get(s.ctx, pOplata.ID)
	assert.NoError(s.T(), err)
	assert.Equal(s.T(), payout.StatusPending, np.Status)
	assert.Equal(s.T(), true, np.DryRun)

	np, err = payout.Get(s.ctx, pBNPL.ID)
	assert.NoError(s.T(), err)
	assert.Equal(s.T(), payout.StatusPending, np.Status)
	assert.Equal(s.T(), true, np.DryRun)

	s.SetAllowedNs(nil)
}

// TestOnlyOurEventDetalized когда в детализации только наше событие, отклоняем выплату
// с ошибкой NoEvents
func (s *OEBSGateTestSuite) TestOnlyOurEventDetalized() {
	p, _, err := s.createRandomPayoutWithRequest()
	require.NoError(s.T(), err)

	lockID := "6666"
	// Успешное снятие блокировки в счетах
	s.ctx.Clients.AccountsLocker = &MockAccounterLocker{
		TestRemove: func(ctx context.Context, attributes *entities.LocationAttributes, uid string) {
			require.Equal(s.T(), lockID, uid)
		},
	}

	// Неуспешная запись в LB (dry_run)
	lbWriters := logbroker.NewEmptyWriters()
	lbWriters.SetWriter(false, false, &logbroker.MockLogBrokerWriter{Err: ErrOooops})
	lbWriters.SetWriter(true, false, &logbroker.MockLogBrokerWriter{Err: ErrOooops})
	namedWriters := logbroker.NamedWriters{"taxi": lbWriters}
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
								EventID:   "1930371",
								EventType: "reserve for payout",
								Info: []byte(`{
"previous_dt": 1633348895,
"tariffer_payload": {
	"dry_run": true
 }
}`),
							},
						},
					},
				},
			}, nil
		},
	}

	blocking, err := s.sendOneTx(p, &namedWriters)
	require.Error(s.T(), err)
	require.Equal(s.T(), ErrNoEvents, err)
	require.False(s.T(), blocking)

	// проверяем, что платеж в статусе rejected
	np, err := payout.Get(s.ctx, p.ID)
	require.NoError(s.T(), err)
	require.Equal(s.T(), payout.StatusRejected, np.Status)
}

// TestOurEventWithDifferentDryRun если в нашем событии dry_run отличается от остальных,
// не учитываем его
func (s *OEBSGateTestSuite) TestOurEventWithDifferentDryRun() {
	p, _, err := s.createRandomPayoutWithRequest()
	require.NoError(s.T(), err)

	lockID := "6666"
	// Успешное снятие блокировки в счетах
	s.ctx.Clients.AccountsLocker = &MockAccounterLocker{
		TestRemove: func(ctx context.Context, attributes *entities.LocationAttributes, uid string) {
			require.Equal(s.T(), lockID, uid)
		},
	}

	// Успешная запись в LB (dry_run)
	lbWriters := logbroker.NewEmptyWriters()
	lbWriters.SetWriter(false, false, &logbroker.MockLogBrokerWriter{Err: ErrOooops})
	lbWriters.SetWriter(true, false, &logbroker.MockLogBrokerWriter{})
	namedWriters := logbroker.NamedWriters{"taxi": lbWriters}
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
								EventID:   "1930371",
								EventType: "reserve for payout",
								Info: []byte(`{
"previous_dt": 1633348895,
"tariffer_payload": {
	"dry_run": false
}
}`),
							},
							{
								EventID:   "payout/taxi/11059644/2021-05-27T11:00:00+00:00",
								EventType: "taxi:payout",
								Info: []byte(`{
 "client_id": 88005553535,
 "event_time": "2021-03-23T17:01:00+00:00",
 "transaction_id": "payout/taxi/92698540/2021-03-23T20:01:00+03:00",
 "tariffer_payload": {
	"dry_run": true
	}
}`),
							},
						},
					},
				},
			}, nil
		},
	}

	_, err = s.sendOneTx(p, &namedWriters)
	require.NoError(s.T(), err)

	// проверяем, что платеж в статусе pending
	np, err := payout.Get(s.ctx, p.ID)
	require.NoError(s.T(), err)
	require.Equal(s.T(), payout.StatusPending, np.Status)
	require.True(s.T(), np.DryRun)
}

// TestAskDetailedTurnover проверяет границы диапазона для детализации
func (s *OEBSGateTestSuite) TestAskDetailedTurnover() {
	p, err := payout.Create(s.ctx, &payout.NewPayout{
		ExternalID: bt.RandS(30),
		ServiceID:  defaultServiceID,
		ContractID: bt.RandN64(),
		ClientID:   bt.RandN64(),
		Amount:     decimal.RequireFromString("124.666"),
		Currency:   "RUB",
		Namespace:  notifier.DefaultNamespace,
	})

	require.NoError(s.T(), err)
	require.NoError(s.T(), payout.Update(s.ctx, p.ID, payout.CreateDTCol, p.CreateDt.AddDate(0, 0, -2)))
	p.CreateDt = p.CreateDt.AddDate(0, 0, -2)
	require.NoError(s.T(), payout.Update(s.ctx, p.ID, payout.StatusCol, payout.StatusPending))

	np, err := payout.Create(s.ctx, &payout.NewPayout{
		ExternalID: bt.RandS(30),
		ServiceID:  p.ServiceID,
		ContractID: p.ContractID,
		ClientID:   p.ClientID,
		Amount:     decimal.RequireFromString("124.666"),
		Currency:   "RUB",
		Namespace:  notifier.DefaultNamespace,
	})
	require.NoError(s.T(), err)

	lbWriters := logbroker.NewEmptyWriters()
	lbWriters.SetWriter(false, false, &logbroker.MockLogBrokerWriter{Err: ErrOooops})
	lbWriters.SetWriter(true, false, &logbroker.MockLogBrokerWriter{Err: ErrOooops})
	namedWriters := logbroker.NamedWriters{"taxi": lbWriters}
	ps := NewPaymentSender(s.ctx, &namedWriters)
	ps.flags.syncCPF = true
	ps.flags.syncNetting = true
	ps.flags.processDryRun = true

	req := entities.BatchReadRequest{}
	loc, err := getAccountLoc(p, interactions.PayoutAccountType)
	require.NoError(s.T(), err)
	err = ps.askTurnoverDetails(&req, loc, np)
	require.NoError(s.T(), err)

	require.NotEmpty(s.T(), req.DetailedTurnovers)
	require.True(s.T(), req.DetailedTurnovers[0].DtFrom.Before(req.DetailedTurnovers[0].DtTo))
	require.True(s.T(), np.CreateDt.Add(1*time.Second).Equal(req.DetailedTurnovers[0].DtTo))
}

// TestGetPrevDtErr проверяет, что ошибка при получении даты пред. выплаты останавливает приложение
func (s *OEBSGateTestSuite) TestGetPrevDtErr() {
	p, err := s.createRandomPayout()
	require.NoError(s.T(), err)
	lbWriters := logbroker.NewEmptyWriters()
	lbWriters.SetWriter(false, false, &logbroker.MockLogBrokerWriter{Err: ErrOooops})
	lbWriters.SetWriter(true, false, &logbroker.MockLogBrokerWriter{Err: ErrOooops})
	namedWriters := logbroker.NamedWriters{"taxi": lbWriters}
	DB := &MockPayoutDB{
		GetDBErr: driver.ErrBadConn,
	}
	ps := NewPaymentSender(s.ctx, &namedWriters, SenderWithDB(DB))
	ps.flags.processDryRun = true
	tx := s.Tx()
	defer func() {
		_ = tx.Commit()
	}()
	blocking, err := ps.sendOne(p, tx)
	require.True(s.T(), blocking)
	require.Equal(s.T(), driver.ErrBadConn, err)

	np, err := payout.Get(s.ctx, p.ID)
	require.NoError(s.T(), err)
	require.Equal(s.T(), payout.StatusNew, np.Status)
}

// TestUpdateAmount проверяет обновление суммы выплаты после получения детализации из системы счетов
func (s *OEBSGateTestSuite) TestUpdateAmount() {

	// создаем выплату на 800р и детализацию с балансом 999.99
	p, err := payout.Create(s.ctx, &payout.NewPayout{
		ExternalID: bt.RandS(30),
		ServiceID:  defaultServiceID,
		ContractID: bt.RandN64(),
		ClientID:   bt.RandN64(),
		Amount:     decimal.RequireFromString("800.00"),
		Currency:   "RUB",
		Namespace:  notifier.DefaultNamespace,
	})
	require.NoError(s.T(), err)

	lockID := "6666"
	// Успешное снятие блокировки в счетах
	s.ctx.Clients.AccountsLocker = &MockAccounterLocker{
		TestRemove: func(ctx context.Context, attributes *entities.LocationAttributes, uid string) {
			assert.Equal(s.T(), lockID, uid)
		},
	}

	lbwritersTaxi := logbroker.NewEmptyWriters()
	lbwritersTaxi.SetWriter(true, false, &logbroker.MockLogBrokerWriter{})
	namedWriters := logbroker.NamedWriters{
		"taxi": lbwritersTaxi,
	}

	// ответ от системы счетов
	s.ctx.Clients.AccountsBatch = &MockAccountBatch{
		// Деньги есть на счете
		ReadFn: func(r *entities.BatchReadRequest) (*entities.BatchReadResponse, error) {
			return &entities.BatchReadResponse{
				Balances: []entities.BalanceAttributesDt{
					{
						Debit:  "0",
						Credit: "999.999",
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
	}`, 0)),
							},
						},
					},
				},
			}, nil
		},
	}

	_, err = s.sendOneTx(p, &namedWriters)
	assert.NoError(s.T(), err)

	// проверяем, что сумма изменилась
	np, err := payout.Get(s.ctx, p.ID)
	require.NoError(s.T(), err)
	require.Equal(s.T(), decimal.RequireFromString("999.999"), np.Amount)
	require.Equal(s.T(), decimal.RequireFromString("1000"), np.AmountOEBS)
}

// TestNegativeAmount проверяет, что выплаты с отрицательной суммой отклоняются
func (s *OEBSGateTestSuite) TestNegativeAmount() {

	// создаем выплату и детализацию на сумму -300р
	p, err := payout.Create(s.ctx, &payout.NewPayout{
		ExternalID: bt.RandS(30),
		ServiceID:  defaultServiceID,
		ContractID: bt.RandN64(),
		ClientID:   bt.RandN64(),
		Amount:     decimal.RequireFromString("-300.00"),
		Currency:   "RUB",
		Namespace:  notifier.DefaultNamespace,
	})
	require.NoError(s.T(), err)

	lockID := "6666"
	// Успешное снятие блокировки в счетах
	s.ctx.Clients.AccountsLocker = &MockAccounterLocker{
		TestRemove: func(ctx context.Context, attributes *entities.LocationAttributes, uid string) {
			assert.Equal(s.T(), lockID, uid)
		},
	}

	lbwritersTaxi := logbroker.NewEmptyWriters()
	lbwritersTaxi.SetWriter(true, false, &logbroker.MockLogBrokerWriter{})
	namedWriters := logbroker.NamedWriters{
		"taxi": lbwritersTaxi,
	}

	// ответ от системы счетов
	s.ctx.Clients.AccountsBatch = &MockAccountBatch{
		// Деньги есть на счете
		ReadFn: func(r *entities.BatchReadRequest) (*entities.BatchReadResponse, error) {
			return &entities.BatchReadResponse{
				Balances: []entities.BalanceAttributesDt{
					{
						Debit:  "600",
						Credit: "300",
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
	}`, 0)),
							},
						},
					},
				},
			}, nil
		},
	}

	_, err = s.sendOneTx(p, &namedWriters)
	require.Error(s.T(), err)
	require.Equal(s.T(), ErrNegativeAmount, err)

	// проверяем, что сумма изменилась
	np, err := payout.Get(s.ctx, p.ID)
	require.NoError(s.T(), err)
	require.Equal(s.T(), decimal.RequireFromString("-300"), np.Amount)
	require.Equal(s.T(), payout.StatusRejected, np.Status)
}

// TestUnlockContext проверяем, что для отмены блокировки в системе счетов используется новый контекст
func (s *OEBSGateTestSuite) TestUnlockContext() {
	was := false
	s.ctx.Clients.AccountsLocker = &MockAccounterLocker{
		TestRemove: func(ctx context.Context, attributes *entities.LocationAttributes, uid string) {
			_, ok := ctx.(*pc.PayoutContext)
			require.False(s.T(), ok)
			was = true
		},
	}
	info := LockInfo{}
	info.Unlock(s.ctx)
	require.True(s.T(), was)
}

// TestSendOneWithCPFOnly проверяет, что при обработке новой cpf-only выплаты в БД вставляется информация о CPF
func (s *OEBSGateTestSuite) TestSendOneWithCPFOnly() {
	s.SetProcessDryRun(true)
	p, _, err := s.createRandomPayoutWithRequest()
	require.NoError(s.T(), err)
	update := core.UpdateDesc{
		{Name: payout.CpfOnlyCol, Value: true},
		{Name: payout.ServiceIDCol, Value: 0},
		{Name: payout.InvoiceIDCol, Value: p.ServiceID},
		{Name: payout.CurrencyCol, Value: ""},
	}
	require.NoError(s.T(), payout.UpdateX(s.ctx, p.ID, update))
	p.CpfOnly = true
	p.InvoiceID = p.ServiceID
	p.ServiceID = 0
	p.Currency = ""

	lockID := "6666"
	// Успешное снятие блокировки в счетах
	s.ctx.Clients.AccountsLocker = &MockAccounterLocker{
		TestRemove: func(ctx context.Context, attributes *entities.LocationAttributes, uid string) {
			assert.Equal(s.T(), lockID, uid)
		},
	}

	// Успешная запись в LB
	// init mocks
	zpr := &logbroker.MockLogBrokerWriter{}
	lbWriters := logbroker.NewEmptyWriters()
	lbWriters.SetWriter(false, false, &logbroker.MockLogBrokerWriter{Err: ErrOooops})
	lbWriters.SetWriter(true, false, &logbroker.MockLogBrokerWriter{Err: ErrOooops})
	lbWriters.SetWriter(true, true, zpr)
	namedWriters := logbroker.NamedWriters{"taxi": lbWriters}

	s.ctx.Clients.AccountsBatch = &MockAccountBatch{
		// Деньги есть на счете
		ReadFn: func(r *entities.BatchReadRequest) (*entities.BatchReadResponse, error) {
			require.Len(s.T(), r.DetailedTurnovers, 1)
			// client_id, invoice_id, contract_id
			require.Len(s.T(), r.DetailedTurnovers[0].Loc.Attributes, 3)
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
								Info: []byte(fmt.Sprintf(`{
  "client_id": 92698540,
  "event_time": "2021-03-23T17:01:00+00:00",
  "transaction_id": "payout/taxi/92698540/2021-03-23T20:01:00+03:00",
  "tariffer_payload": {
	"dry_run": true,
	"common_ts": 1616518860,
	"contract_states": {
	  "invoices": {
  		"%d": [
			{"id": 1234, "operation_type": "INSERT", "external_id": "ЛСТ-2343434-1", "amount": 666.66},
			{"id": 1233, "operation_type": "UPSERT", "external_id": "ЛСТ-2343434-2", "amount": 222.22}
		]
      },
	  "nettings": {
	  	"2955592": {
		  "netting_done": true,
   		  "payment_amount": 542.76,
   		  "payment_currency": "RUB"
   	    },
   	    "%d": {
   		  "netting_done": true,
   		  "payment_amount": 220,
   		  "payment_currency": "RUB"
   	    },
   	    "2955594": {
   		  "netting_done": true,
   		  "payment_amount": 0,
   		  "payment_currency": "RUB"
 		}
	  }
	}
  }
}`, p.ContractID, p.ContractID)),
							},
						},
					},
				},
			}, nil
		},
	}
	// Успешная запись в ЛБ
	_, err = s.sendOneTxX(p, &namedWriters, true)
	assert.NoError(s.T(), err)

	// проверяем, что платеж в статусе pending
	np, err := payout.Get(s.ctx, p.ID)
	assert.NoError(s.T(), err)
	assert.Equal(s.T(), payout.StatusPending, np.Status)
	// т.к. осн. статус поменялся, то статус синхронизации
	// должен в начальное состояние перейти - new
	assert.Equal(s.T(), payout.SyncStatusNew, np.SyncStatus)

	// Проверяем, что создались CPF записи
	where := core.WhereDesc{
		{Name: cpf.PayoutIDCol, Value: p.ID},
	}
	cpfs, err := cpf.Get(s.ctx, where, true)
	require.NoError(s.T(), err)
	require.Len(s.T(), cpfs, 2)
	testCPF := func(OpType string, Status string, ExtID string, amount string, dryRun bool, c cpf.CPF) {
		require.Equal(s.T(), OpType, c.OpType)
		require.Equal(s.T(), Status, c.Status)
		require.Equal(s.T(), ExtID, c.ExtID)
		require.Equal(s.T(), amount, c.Amount.String())
		require.Equal(s.T(), dryRun, c.DryRun)
	}
	testCPF("INSERT", cpf.StatusNew, "ЛСТ-2343434-1", "666.66", true, cpfs[0])
	testCPF("UPSERT", cpf.StatusNew, "ЛСТ-2343434-2", "222.22", true, cpfs[1])
}
