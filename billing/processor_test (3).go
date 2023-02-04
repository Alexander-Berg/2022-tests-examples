package request

import (
	"context"
	"errors"
	"fmt"
	"os"
	"time"

	"github.com/shopspring/decimal"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"

	"a.yandex-team.ru/billing/hot/accounts/pkg/core/entities"
	acc "a.yandex-team.ru/billing/hot/payout/internal/interactions"
	"a.yandex-team.ru/billing/hot/payout/internal/notifier"
	"a.yandex-team.ru/billing/hot/payout/internal/payout"
	bt "a.yandex-team.ru/billing/library/go/billingo/pkg/testing"
)

// createRandomRequest создает случайный запрос на выплату
func createRandomRequest() *NewRequest {
	externalID := bt.RandS(50)
	clientID := bt.RandN64()

	r := NewRequest{
		ExternalID: externalID,
		ClientID:   clientID,
		Namespace:  notifier.DefaultNamespace,
	}
	return &r
}

// TestRequestErrorFromAccounts проверяем, что возвращаем корректную статистику и ошибку
func (s *ProcessorTestSuite) TestRequestErrorFromAccounts() {
	accountError := errors.New("something failed")
	r, err := Create(s.ctx, createRandomRequest())
	assert.NoError(s.T(), err)

	s.ctx.Clients.AccountsReader = &MockAccountReader{Err: accountError}
	p := NewProcessor(s.ctx, nil, nil)

	tx, err := s.ctx.Storage.Backend.BeginTx(s.ctx)
	require.NoError(s.T(), err)
	stats, err := p.processRequest(r, tx)
	require.NoError(s.T(), tx.Commit())
	assert.Error(s.T(), err)
	assert.Equal(s.T(), accountError, err)
	assert.Equal(s.T(), 0, stats.Total)
	assert.Equal(s.T(), 1, stats.Error)
	assert.Equal(s.T(), 0, stats.Processed)

	rt, err := Get(s.ctx, r.ID)
	assert.NoError(s.T(), err)
	assert.Equal(s.T(), StatusNew, rt.Status)
}

// TestProcessErrorFromAccounts проверяет, что при ошибке в системе счетов, мы
// ничего не даелаем с заявкой
func (s *ProcessorTestSuite) TestProcessErrorFromAccounts() {
	accountError := errors.New("something failed")
	r, err := Create(s.ctx, createRandomRequest())
	assert.NoError(s.T(), err)

	s.ctx.Clients.AccountsReader = &MockAccountReader{Err: accountError}
	p := NewProcessor(s.ctx, nil, nil)
	require.NotNil(s.T(), p)

	tx, err := s.ctx.Storage.Backend.BeginTx(s.ctx)
	require.NoError(s.T(), err)
	err = p.process(time.Now())
	require.NoError(s.T(), tx.Commit())
	require.NoError(s.T(), err)

	rt, err := Get(s.ctx, r.ID)
	assert.NoError(s.T(), err)
	assert.Equal(s.T(), StatusNew, rt.Status)
}

// TestErrorCreateZeroPayout проверяет, что по нулевому балансу создается выплата как обычно
func (s *ProcessorTestSuite) TestErrorCreateZeroPayout() {
	r, err := Create(s.ctx, createRandomRequest())
	assert.NoError(s.T(), err)

	currency := "RUB"
	contractID := "12345"
	serviceID := "124"
	clientID := fmt.Sprint(r.ClientID)

	attrs := entities.LocationAttributes{
		Type: acc.PayoutAccountType,
		Attributes: map[string]*string{
			acc.Client:    &clientID,
			"contract_id": &contractID,
			"service_id":  &serviceID,
			"currency":    &currency,
		},
	}

	data := []entities.BalanceAttributes{
		{
			Loc:    attrs,
			Debit:  "0.0",
			Credit: "0.0",
		},
	}
	s.ctx.Clients.AccountsReader = &MockAccountReader{Data: data}
	p := NewProcessor(s.ctx, nil, nil)

	tx, err := s.ctx.Storage.Backend.BeginTx(s.ctx)
	require.NoError(s.T(), err)
	stats, err := p.processRequest(r, tx)
	require.NoError(s.T(), tx.Commit())
	assert.NoError(s.T(), err)
	assert.Equal(s.T(), 1, stats.Total)
	assert.Equal(s.T(), 0, stats.Error)
	assert.Equal(s.T(), 1, stats.Processed)

	externalID, err := fmtExternalID(r, 12345, defaultServiceID, 0)
	assert.NoError(s.T(), err)

	pay, err := payout.GetByServiceEID(s.ctx, int64(124), externalID)
	assert.NoError(s.T(), err)

	assert.Equal(s.T(), r.ID, *pay.RequestID)
	assert.Equal(s.T(), decimal.RequireFromString("0"), pay.Amount)
	assert.Equal(s.T(), currency, pay.Currency)
}

// BILLING-679:
// TestApproveRequired проверяет корректность выставления флага ApproveRequired
func (s *ProcessorTestSuite) TestApproveRequired() {
	manualNamespace := "RA"
	s.ctx.Config.ApproveRequired = map[string]struct{}{}

	// случайные запросы
	r, err := Create(s.ctx, createRandomRequest())
	assert.NoError(s.T(), err)
	r2, err := Create(s.ctx, createRandomRequest())
	assert.NoError(s.T(), err)

	// запросы с определённым namespace
	manualNewReq := createRandomRequest()
	manualNewReq.Namespace = manualNamespace
	manualR, err := Create(s.ctx, manualNewReq) // проверка для namespace
	assert.NoError(s.T(), err)
	manualNewReq2 := createRandomRequest()
	manualNewReq2.Namespace = manualNamespace
	manualR2, err := Create(s.ctx, manualNewReq2) // проверка для namespace
	assert.NoError(s.T(), err)

	// случайный запрос для проверки global-флага
	globalR, err := Create(s.ctx, createRandomRequest()) // проверка глобального флага
	assert.NoError(s.T(), err)

	currency := "RUB"
	contractID := "12345"
	serviceID := "124"
	clientID := fmt.Sprint(r.ClientID)

	attrs := entities.LocationAttributes{
		Type: acc.PayoutAccountType,
		Attributes: map[string]*string{
			acc.Client:    &clientID,
			"contract_id": &contractID,
			"service_id":  &serviceID,
			"currency":    &currency,
		},
	}

	data := []entities.BalanceAttributes{
		{
			Loc:    attrs,
			Debit:  "10000.0",
			Credit: "10000.0",
		},
	}
	s.ctx.Clients.AccountsReader = &MockAccountReader{Data: data}
	p := NewProcessor(s.ctx, nil, nil)

	var process = func(r *Request) *payout.Payout {
		tx, err := s.ctx.Storage.Backend.BeginTx(s.ctx)
		require.NoError(s.T(), err)
		stats, err := p.processRequest(r, tx)
		require.NoError(s.T(), tx.Commit())
		assert.NoError(s.T(), err)
		assert.Equal(s.T(), 1, stats.Total)
		assert.Equal(s.T(), 0, stats.Error)
		assert.Equal(s.T(), 1, stats.Processed)

		externalID, err := fmtExternalID(r, 12345, defaultServiceID, 0)
		assert.NoError(s.T(), err)
		pay, err := payout.GetByServiceEID(s.ctx, defaultServiceID, externalID)
		assert.NoError(s.T(), err)
		return pay
	}

	// пустой список namespace
	pay := process(r)
	require.False(s.T(), pay.ApproveRequired)
	pay = process(manualR)
	require.False(s.T(), pay.ApproveRequired)

	// список только с одним namespace
	s.ctx.Config.ApproveRequired[manualNamespace] = struct{}{}
	pay = process(manualR2)
	require.True(s.T(), pay.ApproveRequired)
	pay = process(r2)
	require.False(s.T(), pay.ApproveRequired)

	// добавляется global
	s.ctx.Config.ApproveRequired[PayoutApproveRequiredGlobal] = struct{}{}
	pay = process(globalR)
	require.True(s.T(), pay.ApproveRequired)
}

func (s *ProcessorTestSuite) TestCalculateApproveRequired() {
	s.ctx.Config.ApproveRequired = map[string]struct{}{}
	nsCommon := "common"
	nsOther := "other"
	require.False(s.T(), calculateApproveRequired(s.ctx.Config, nsCommon))

	s.ctx.Config.ApproveRequired[nsCommon] = struct{}{}
	require.True(s.T(), calculateApproveRequired(s.ctx.Config, nsCommon))
	require.False(s.T(), calculateApproveRequired(s.ctx.Config, nsOther))

	s.ctx.Config.ApproveRequired[PayoutApproveRequiredGlobal] = struct{}{}
	require.True(s.T(), calculateApproveRequired(s.ctx.Config, nsCommon))
	require.True(s.T(), calculateApproveRequired(s.ctx.Config, nsOther))
}

// TestCreatePayout проверяет, что по заявке создаются выплаты + меняется статус заявки
func (s *ProcessorTestSuite) TestCreatePayout() {
	r, err := Create(s.ctx, createRandomRequest())
	assert.NoError(s.T(), err)

	clientID := fmt.Sprint(r.ClientID)

	currency := "RUB"
	contractID := "12345"
	serviceID := fmt.Sprint(defaultServiceID)

	contractID2 := "123"
	serviceID2 := fmt.Sprint(defaultServiceID)

	data := []entities.BalanceAttributes{
		{
			Loc: entities.LocationAttributes{
				Type: acc.PayoutAccountType,
				Attributes: map[string]*string{
					acc.Client:    &clientID,
					"contract_id": &contractID,
					"service_id":  &serviceID,
					"currency":    &currency,
				},
			},
			Debit:  "10.5",
			Credit: "100.5",
		},
		{
			Loc: entities.LocationAttributes{
				Type: acc.PayoutAccountType,
				Attributes: map[string]*string{
					acc.Client:    &clientID,
					"contract_id": &contractID2,
					"service_id":  &serviceID2,
					"currency":    &currency,
				},
			},
			Debit:  "0.0",
			Credit: "0.0",
		},
	}
	s.ctx.Clients.AccountsReader = &MockAccountReader{TestFunc: func(ctx context.Context, loc *entities.LocationAttributes, dt *int64) ([]entities.BalanceAttributes, error) {
		require.Nil(s.T(), dt) // original client will place MaxDt value
		return data, nil
	}}

	require.NoError(s.T(), err)

	p := NewProcessor(s.ctx, nil, nil)

	tx, err := s.ctx.Storage.Backend.BeginTx(s.ctx)
	require.NoError(s.T(), err)
	stats, err := p.processRequest(r, tx)
	require.NoError(s.T(), tx.Commit())
	assert.NoError(s.T(), err)
	assert.Equal(s.T(), 2, stats.Total)
	assert.Equal(s.T(), 0, stats.Error)
	assert.Equal(s.T(), 2, stats.Processed)

	externalID, err := fmtExternalID(r, 12345, defaultServiceID, 0)
	assert.NoError(s.T(), err)

	pay, err := payout.GetByServiceEID(s.ctx, defaultServiceID, externalID)
	assert.NoError(s.T(), err)

	assert.Equal(s.T(), r.ID, *pay.RequestID)
	assert.Equal(s.T(), decimal.RequireFromString("90"), pay.Amount)
	assert.Equal(s.T(), currency, pay.Currency)

	ru, err := Get(s.ctx, r.ID)
	assert.NoError(s.T(), err)
	// статус не меняется в этой процедуре, меняется из-вне
	assert.Equal(s.T(), StatusNew, ru.Status)
}

// TestRequestCreateCpfOnly проверяет, что для cpf-only заявки корректно создаем cpf-only выплаты
func (s *ProcessorTestSuite) TestRequestCreateCpfOnly() {
	r, err := Create(s.ctx, createRandomRequest())
	require.NoError(s.T(), err)
	require.NoError(s.T(), Update(s.ctx, r.ID, CpfOnlyCol, true))
	r.CpfOnly = true

	clientID := fmt.Sprint(r.ClientID)

	contractID := "12345"
	invoiceID := fmt.Sprint(164892778)
	currency := "RUB"
	operationType := "CANCEL_FUEL_HOLD"

	contractID2 := "123"
	invoiceID2 := fmt.Sprint(164892777)
	operationType2 := "INSERT_YA_NETTING"

	data := []entities.BalanceAttributes{
		{
			Loc: entities.LocationAttributes{
				Type: acc.PayoutAccountType,
				Attributes: map[string]*string{
					acc.Client:        &clientID,
					acc.Contract:      &contractID,
					acc.Invoice:       &invoiceID,
					acc.Currency:      &currency,
					acc.OperationType: &operationType,
				},
			},
			Debit:  "10.5",
			Credit: "100.5",
		},
		{
			Loc: entities.LocationAttributes{
				Type: acc.PayoutAccountType,
				Attributes: map[string]*string{
					acc.Client:        &clientID,
					acc.Contract:      &contractID2,
					acc.Invoice:       &invoiceID2,
					acc.Currency:      &currency,
					acc.OperationType: &operationType2,
				},
			},
			Debit:  "0.0",
			Credit: "0.0",
		},
	}
	readerRequest := false
	s.ctx.Clients.AccountsReader = &MockAccountReader{TestFunc: func(ctx context.Context, loc *entities.LocationAttributes, dt *int64) ([]entities.BalanceAttributes, error) {
		require.Nil(s.T(), dt) // original client will place MaxDt value
		readerRequest = true
		return data, nil
	}}

	require.NoError(s.T(), err)

	p := NewProcessor(s.ctx, nil, nil)

	tx, err := s.ctx.Storage.Backend.BeginTx(s.ctx)
	require.NoError(s.T(), err)
	stats, err := p.processRequest(r, tx)
	require.NoError(s.T(), tx.Commit())
	require.NoError(s.T(), err)
	require.NoError(s.T(), stats.LastError)
	assert.Equal(s.T(), 2, stats.Total)
	require.Equal(s.T(), 0, stats.Error)
	assert.Equal(s.T(), 2, stats.Processed)

	externalID, err := fmtExternalID(r, 12345, 0, 164892778)
	require.NoError(s.T(), err)

	pay, err := payout.GetByServiceEID(s.ctx, 0, externalID)
	assert.NoError(s.T(), err)
	require.NotNil(s.T(), pay)

	assert.Equal(s.T(), r.ID, *pay.RequestID)
	assert.Equal(s.T(), decimal.RequireFromString("90"), pay.Amount)
	assert.Equal(s.T(), int64(0), pay.ServiceID)
	assert.Equal(s.T(), int64(164892778), pay.InvoiceID)
	assert.NotNil(s.T(), pay.OperationType)
	assert.Equal(s.T(), operationType, *pay.OperationType)
	assert.True(s.T(), pay.CpfOnly)

	ru, err := Get(s.ctx, r.ID)
	assert.NoError(s.T(), err)
	// статус не меняется в этой процедуре, меняется из-вне
	assert.Equal(s.T(), StatusNew, ru.Status)
	require.True(s.T(), readerRequest)
}

// отдельные функции

func (s *ProcessorTestSuite) TestBalancePassiveAccount() {
	tests := []struct {
		Debit  string
		Credit string
		Err    error
		Result decimal.Decimal
	}{
		{
			Debit: "10", Credit: "100", Result: decimal.RequireFromString("90"),
		},
		{
			Debit: "eeee", Credit: "aaa", Err: errors.New("can't convert eeee to decimal: exponent is not numeric"),
		},
		{
			Debit: "10", Credit: "aaa", Err: errors.New("can't convert aaa to decimal"),
		},
		{
			Debit: "200.4", Credit: "100.3", Result: decimal.RequireFromString("-100.1"),
		},
	}

	for idx, t := range tests {
		got, err := BalancePassiveAccount(t.Debit, t.Credit)
		if t.Err != nil {
			assert.Error(s.T(), err)
			assert.Equal(s.T(), t.Err, err)
		} else {
			assert.NoError(s.T(), err)
			assert.Equal(s.T(), t.Result, got, idx)
		}
	}
}

func (s *ProcessorTestSuite) TestNewProcessor() {
	err := os.Unsetenv("PAYOUT_REQ_GATE_INTERVAL")
	require.NoError(s.T(), err)

	p := NewProcessor(s.ctx, nil, nil)
	require.EqualValues(s.T(), 500, p.limit)
	require.Equal(s.T(), 5*time.Minute, p.interval)

	err = os.Setenv("PAYOUT_REQUEST_LIMIT", "10")
	require.NoError(s.T(), err)
	err = os.Setenv("PAYOUT_REQ_GATE_INTERVAL", "1m")
	require.NoError(s.T(), err)

	p1 := NewProcessor(s.ctx, nil, nil)
	require.EqualValues(s.T(), 10, p1.limit)
	require.Equal(s.T(), 1*time.Minute, p1.interval)
}

//TestAllowedClients проверяет, что если выставлен флаг и заявка не от клиента из списка,
//мы ее не обрабатываем (переводим в статус done)
func (s *ProcessorTestSuite) TestAllowedClients() {
	s.SetAllowedClients()
	err := os.Setenv("PAYOUT_REQUEST_LIMIT", "500")
	require.NoError(s.T(), err)

	accountError := errors.New("account Error")

	s.ctx.Clients.AccountsReader = &MockAccountReader{Err: accountError}

	// создаем две заявки: от запрещенного и разрешенного клиентов
	r, err := Create(s.ctx, createRandomRequest())
	assert.NoError(s.T(), err)

	rFromAllowed, err := Create(s.ctx, &NewRequest{
		ExternalID: bt.RandS(50),
		ClientID:   s.ctx.Config.AllowedClients[0],
		Namespace:  notifier.DefaultNamespace,
	})
	assert.NoError(s.T(), err)

	p := NewProcessor(s.ctx, nil, nil)

	require.NoError(s.T(), err)
	err = p.process(time.Now())
	require.NoError(s.T(), err, r.ClientID)

	rProcessed, err := Get(s.ctx, r.ID)
	require.NoError(s.T(), err)
	require.Equal(s.T(), StatusDone, rProcessed.Status, rProcessed.ID)
	require.EqualValues(s.T(),
		"client not on the list, request won't be processed",
		rProcessed.Error.String,
		rProcessed.ID)

	// заявка от разрешенного клиента должна быть в статусе new, т.к. мокаем ошибку в ответе системы счетов
	rFromAllowedProcessed, err := Get(s.ctx, rFromAllowed.ID)
	require.NoError(s.T(), err)
	require.Equal(s.T(), StatusNew, rFromAllowedProcessed.Status, rFromAllowedProcessed.ID)
}
