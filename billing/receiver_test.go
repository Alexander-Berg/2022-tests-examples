package oebsgate

import (
	"context"
	"database/sql"
	"database/sql/driver"
	"encoding/json"
	"fmt"
	"strconv"
	"strings"
	"testing"
	"time"

	sq "github.com/Masterminds/squirrel"
	"github.com/shopspring/decimal"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"

	"a.yandex-team.ru/billing/hot/accounts/pkg/core/entities"
	"a.yandex-team.ru/billing/hot/payout/internal/core"
	"a.yandex-team.ru/billing/hot/payout/internal/cpf"
	"a.yandex-team.ru/billing/hot/payout/internal/cpf/client"
	acc "a.yandex-team.ru/billing/hot/payout/internal/interactions"
	"a.yandex-team.ru/billing/hot/payout/internal/logbroker"
	"a.yandex-team.ru/billing/hot/payout/internal/netting"
	"a.yandex-team.ru/billing/hot/payout/internal/payout"
	"a.yandex-team.ru/billing/library/go/billingo/pkg/interactions"
	bt "a.yandex-team.ru/billing/library/go/billingo/pkg/testing"
	"a.yandex-team.ru/kikimr/public/sdk/go/persqueue"
)

func parseErrorMessage(t *testing.T, data []byte) logbroker.OebsErrorMessage {
	var errorMessage logbroker.OebsErrorMessage
	err := json.Unmarshal(data, &errorMessage)
	require.NoError(t, err, string(data))
	return errorMessage
}

// TestProcessARDOk проверяет, что если ответ из АРД положительный,
// то обновляем статус и ссылку на batch
func (s *OEBSGateTestSuite) TestProcessARDOk() {
	p, err := s.createRandomPayout()
	assert.NoError(s.T(), err)

	batchID := bt.RandN64()

	m := ARDResponse{
		StatusSystem:   OEBS,
		MessageID:      p.ID,
		StatusType:     ARDStatusNew,
		PaymentBatchID: &batchID,
	}

	r, _ := NewReceiver(s.ctx)
	retry, _, err := r.processARD(s.ctx, &m)
	assert.NoError(s.T(), err)
	assert.False(s.T(), retry)

	pn, err := payout.Get(s.ctx, p.ID)
	assert.NoError(s.T(), err)
	assert.Equal(s.T(), payout.StatusConfirmed, pn.Status)
	assert.Equal(s.T(), batchID, pn.OEBSBatchID)
	require.Equal(s.T(), ARDStatusNew, pn.OEBSStatus)
}

// TestProcessARDOkZeroPayout проверяет, что если ответ из АРД положительный,
// то обновляем статус на финальный для нулевой выплаты (так как других ответов не будет)
func (s *OEBSGateTestSuite) TestProcessARDOkZeroPayout() {
	p, err := s.createRandomPayout()
	require.NoError(s.T(), err)
	require.NoError(s.T(), payout.Update(s.ctx, p.ID, payout.AmountCol, decimal.Zero))

	m := ARDResponse{
		StatusSystem: OEBS,
		MessageID:    p.ID,
		StatusType:   ARDStatusNew,
	}

	// Успешная регистрация выплаты
	called := false
	s.ctx.Clients.AccountsBatch = &MockAccountBatch{
		TestWrite: func(r *entities.BatchWriteRequest) {
			// проверяем, что отправили в систему счетов
			called = true
			require.Equal(s.T(), 1, len(r.Events))
			require.Equal(s.T(), registerPayoutEvent, r.EventType)
			require.Greater(s.T(), strings.Index(r.ExternalID, ":"), 0)

			var info ConfirmInfo
			require.NoError(s.T(), json.Unmarshal(r.Info, &info))
			assert.Less(s.T(), info.OEBSPaymenID, int64(0))
			require.Equal(s.T(), Debit, r.Events[0].Type)
			require.Equal(s.T(), "0", r.Events[0].Amount)
			require.Equal(s.T(), acc.PayoutTmpAccountType, r.Events[0].Loc.Type)
		},
	}

	r, _ := NewReceiver(s.ctx)
	retry, _, err := r.processARD(s.ctx, &m)
	require.NoError(s.T(), err)
	require.False(s.T(), retry)

	pn, err := payout.Get(s.ctx, p.ID)
	require.NoError(s.T(), err)
	assert.Equal(s.T(), payout.StatusDone, pn.Status)
	assert.Equal(s.T(), "", pn.OEBSStatus)
	assert.NotZero(s.T(), pn.OEBSPaymentID)

	assert.True(s.T(), called)

}

// TestProcessARDOkWithNetting проверяет, что при подтверждении из АРД переводим ВЗ в статус done
func (s *OEBSGateTestSuite) TestProcessARDOkWithNetting() {
	p, err := s.createRandomPayout()
	assert.NoError(s.T(), err)

	_, err = s.createRandomNettingForPayout(p, netting.StatusNew)
	assert.NoError(s.T(), err)
	_, err = s.createRandomNettingForPayout(p, netting.StatusNew)
	assert.NoError(s.T(), err)

	batchID := bt.RandN64()

	m := ARDResponse{
		StatusSystem:   OEBS,
		MessageID:      p.ID,
		StatusType:     ARDStatusNew,
		PaymentBatchID: &batchID,
	}

	r, _ := NewReceiver(s.ctx)
	r.flags.syncNetting = true
	retry, _, err := r.processARD(s.ctx, &m)
	assert.NoError(s.T(), err)
	assert.False(s.T(), retry)

	where := core.WhereDesc{
		{Name: netting.PayoutIDCol, Value: p.ID},
	}
	ns, err := netting.Get(s.ctx, where)
	require.NoError(s.T(), err)
	require.Len(s.T(), ns, 2)
	for i := 0; i < 2; i++ {
		require.Equal(s.T(), netting.StatusDone, ns[i].Status)
	}
}

// TestProcessARDWrongSystem проверяет, что для неизвестной системы вернем ошибку
// с выплатой ничего не далаем
func (s *OEBSGateTestSuite) TestProcessARDWrongSystem() {
	p, err := s.createRandomPayout()
	assert.NoError(s.T(), err)

	batchID := bt.RandN64()

	m := ARDResponse{
		StatusSystem:   "some-other-system",
		MessageID:      p.ID,
		StatusType:     ARDStatusNew,
		PaymentBatchID: &batchID,
	}

	r, _ := NewReceiver(s.ctx)
	retry, errSource, err := r.processARD(s.ctx, &m)
	assert.Error(s.T(), err)
	assert.Equal(s.T(), ErrUnknownSystem, err)
	assert.False(s.T(), retry)
	assert.Equal(s.T(), logbroker.UnknownSystem, errSource)

	pn, err := payout.Get(s.ctx, p.ID)
	assert.NoError(s.T(), err)
	assert.Equal(s.T(), payout.StatusNew, pn.Status)
	z := int64(0)
	assert.Equal(s.T(), z, pn.OEBSBatchID)
}

// TestProcessARDRejected проверяет, что если ответ из АРД отрицательный,
// то обновляем статус и текст ошибки + обновляем связи с ВЗ.
func (s *OEBSGateTestSuite) TestProcessARDRejected() {
	p, _, err := s.createRandomPayoutWithRequest()
	assert.NoError(s.T(), err)
	amount := p.Amount.String()

	n1, err := s.createRandomNettingForPayout(p, netting.StatusNew)
	assert.NoError(s.T(), err)
	n2, err := s.createRandomNettingForPayout(p, netting.StatusNew)
	assert.NoError(s.T(), err)

	info := "Договор не найден"

	m := ARDResponse{
		StatusSystem: OEBS,
		MessageID:    p.ID,
		StatusType:   ARDStatusError,
		StatusInfo:   info,
	}

	called := false
	// Успешный возврат средств
	s.ctx.Clients.AccountsBatch = &MockAccountBatch{
		TestWrite: func(r *entities.BatchWriteRequest) {
			called = true
			// проверяем, что отправили в систему счетов
			assert.Equal(s.T(), 2, len(r.Events))
			assert.Equal(s.T(), cancelReservePayoutEvent, r.EventType)
			assert.Equal(s.T(), formatAccountExtID(p), r.ExternalID)
			info, _ := formatConfirmInfo(p, int64(0), []string{n1.ExtID, n2.ExtID})
			assert.Equal(s.T(), info, r.Info)
			assert.Equal(s.T(), Debit, r.Events[0].Type)
			assert.Equal(s.T(), Credit, r.Events[1].Type)
			assert.Equal(s.T(), amount, r.Events[0].Amount)
			assert.Equal(s.T(), amount, r.Events[1].Amount)
		},
	}

	n, prod, err := s.createNotifier()
	require.NoError(s.T(), err)

	r, _ := NewReceiver(s.ctx, ReceiverWithNotifier(n))
	retry, errSource, err := r.processARD(s.ctx, &m)
	assert.NoError(s.T(), err)
	assert.False(s.T(), retry)
	assert.True(s.T(), called)
	assert.Equal(s.T(), emptyErrSource, errSource)

	require.Len(s.T(), prod.Messages, 1)

	pn, err := payout.Get(s.ctx, p.ID)
	assert.NoError(s.T(), err)
	assert.Equal(s.T(), payout.StatusRejected, pn.Status)
	require.NotNil(s.T(), pn.Error)
	require.Equal(s.T(), info, *pn.Error)
	require.Equal(s.T(), ARDStatusError, pn.OEBSStatus)

	where := core.WhereDesc{
		{Name: netting.PayoutIDCol, Value: p.ID},
	}
	ns, err := netting.Get(s.ctx, where)
	require.NoError(s.T(), err)
	require.Len(s.T(), ns, 2)
	for i := 0; i < 2; i++ {
		require.Equal(s.T(), netting.StatusError, ns[i].Status)
	}
}

// TestProcessARDRejectedRetry проверяет, что если обновление счетов не прошло,
// то попросим повторить
func (s *OEBSGateTestSuite) TestProcessARDRejectedRetry() {
	p, _, err := s.createRandomPayoutWithRequest()
	assert.NoError(s.T(), err)

	info := "Договор не найден"

	m := ARDResponse{
		StatusSystem: OEBS,
		MessageID:    p.ID,
		StatusType:   ARDStatusError,
		StatusInfo:   info,
	}

	// Успешный возврат средств
	s.ctx.Clients.AccountsBatch = &MockAccountBatch{
		WriteErr: ErrOooops,
	}

	r, _ := NewReceiver(s.ctx)
	retry, errSource, err := r.processARD(s.ctx, &m)
	assert.Error(s.T(), err)
	assert.Equal(s.T(), err, ErrOooops)
	assert.True(s.T(), retry)
	assert.Equal(s.T(), emptyErrSource, errSource)

	pn, err := payout.Get(s.ctx, p.ID)
	assert.NoError(s.T(), err)
	assert.Equal(s.T(), payout.StatusNew, pn.Status)
	assert.Empty(s.T(), pn.Error)
}

// TestProcessOEBSWrongSystem проверяет, что для неизвестной системы вернем ошибку
func (s *OEBSGateTestSuite) TestProcessOEBSWrongSystem() {
	m := OEBSResponse{
		StatusSystem: "some-other-system",
		StatusType:   OEBSStatusCreated,
	}

	r, _ := NewReceiver(s.ctx)
	retry, errSource, err := r.processOEBS(s.ctx, &m)
	assert.Error(s.T(), err)
	assert.Equal(s.T(), ErrUnknownSystem, err)
	assert.False(s.T(), retry)
	assert.Equal(s.T(), logbroker.UnknownSystem, errSource)
}

// TestProcessOEBSWrongSystem проверяет, что для неизвестного статуса вернем ошибку
func (s *OEBSGateTestSuite) TestProcessOEBSUnknownStatus() {
	m := OEBSResponse{
		StatusSystem: OEBS,
		StatusType:   "some-new-status",
	}

	r, _ := NewReceiver(s.ctx)
	retry, errSource, err := r.processOEBS(s.ctx, &m)
	assert.Error(s.T(), err)
	assert.Equal(s.T(), ErrUnknownStatus, err)
	assert.False(s.T(), retry)
	assert.Equal(s.T(), emptyErrSource, errSource)
}

// TestProcessOEBSSkipStatuses проверяем, что некоторые статусы игнорируем из ОЕБС
func (s *OEBSGateTestSuite) TestProcessOEBSSkipStatuses() {
	for status := range OEBSStatusSkip {
		m := OEBSResponse{
			StatusDT:     time.Now().Format("2006-01-02 15:04:05"),
			StatusSystem: OEBS,
			StatusType:   status,
		}

		r, _ := NewReceiver(s.ctx)
		retry, errSource, err := r.processOEBS(s.ctx, &m)
		assert.NoError(s.T(), err)
		assert.False(s.T(), retry)
		assert.Equal(s.T(), emptyErrSource, errSource)
	}
}

// TestProcessOEBSSkipStatusesWithUpdates проверяем, что некоторые статусы игнорируем из ОЕБС
// но ссылки на batch сохраняем
func (s *OEBSGateTestSuite) TestProcessOEBSSkipStatusesWithUpdates() {
	p, _, err := s.createRandomPayoutWithARD()
	assert.NoError(s.T(), err)

	paymentID := bt.RandN64()

	m := OEBSResponse{
		StatusDT:       time.Now().Format("2006-01-02 15:04:05"),
		StatusSystem:   OEBS,
		StatusType:     OEBSStatusTransmitted,
		PaymentBatchID: []int64{p.OEBSBatchID},
		PaymentID:      &paymentID,
	}

	r, _ := NewReceiver(s.ctx)
	retry, errSource, err := r.processOEBS(s.ctx, &m)
	assert.NoError(s.T(), err)
	assert.False(s.T(), retry)
	assert.Equal(s.T(), emptyErrSource, errSource)

	pn, err := payout.Get(s.ctx, p.ID)
	assert.NoError(s.T(), err)
	assert.Equal(s.T(), payout.StatusConfirmed, pn.Status)
	assert.Equal(s.T(), paymentID, pn.OEBSPaymentID)
}

// TestProcessOEBSCreateStatus проверяем, что при получении первого сообщения от ОЕБС
// отправляем нулевую проводку в систему счетов
func (s *OEBSGateTestSuite) TestProcessOEBSCreateStatus() {
	p, _, err := s.createRandomPayoutWithARD()
	assert.NoError(s.T(), err)
	paymentID := bt.RandN64()
	p.OEBSPaymentID = paymentID

	m := OEBSResponse{
		StatusDT:       time.Now().Format("2006-01-02 15:04:05"),
		StatusSystem:   OEBS,
		StatusType:     OEBSStatusCreated,
		PaymentBatchID: []int64{p.OEBSBatchID},
		PaymentID:      &paymentID,
	}

	// Успешное списание средств
	called := false
	s.ctx.Clients.AccountsBatch = &MockAccountBatch{
		TestWrite: func(r *entities.BatchWriteRequest) {
			// проверяем, что отправили в систему счетов
			called = true
			require.Equal(s.T(), 1, len(r.Events))
			require.Equal(s.T(), registerPayoutEvent, r.EventType)
			require.Equal(s.T(), formatAccountExtID(p), r.ExternalID)
			require.Greater(s.T(), strings.Index(r.ExternalID, ":"), 0)
			info, _ := formatConfirmInfo(p, paymentID, []string{})
			require.Equal(s.T(), info, r.Info)
			require.Equal(s.T(), Debit, r.Events[0].Type)
			require.Equal(s.T(), "0", r.Events[0].Amount)
			require.Equal(s.T(), acc.PayoutTmpAccountType, r.Events[0].Loc.Type)
		},
	}

	r, _ := NewReceiver(s.ctx)
	retry, errSource, err := r.processOEBS(s.ctx, &m)
	assert.NoError(s.T(), err)
	assert.False(s.T(), retry)
	require.True(s.T(), called)
	require.Equal(s.T(), emptyErrSource, errSource)

	pn, err := payout.Get(s.ctx, p.ID)
	assert.NoError(s.T(), err)
	assert.Equal(s.T(), payout.StatusConfirmed, pn.Status)
	assert.Equal(s.T(), paymentID, pn.OEBSPaymentID)
}

// TestProcessOEBSReconciledFail проверяем, что выплата подтверждена, делаем запрос в систему счетов
// но запрос в счета падает. Должны оставить выплаты нетронутыми, а так же попросить повторить сообщение
func (s *OEBSGateTestSuite) TestProcessOEBSReconciledFail() {
	p, _, err := s.createRandomPayoutWithARD()
	assert.NoError(s.T(), err)

	paymentID := bt.RandN64()
	m := OEBSResponse{
		StatusDT:       time.Now().Format("2006-01-02 15:04:05"),
		StatusSystem:   OEBS,
		StatusType:     OEBSStatusReconciled,
		PaymentBatchID: []int64{p.OEBSBatchID},
		PaymentID:      &paymentID,
	}

	// Ошибка при отправке запроса в систему счетов
	s.ctx.Clients.AccountsBatch = &MockAccountBatch{
		WriteErr: ErrOooops,
	}

	r, _ := NewReceiver(s.ctx)
	retry, errSource, err := r.processOEBS(s.ctx, &m)
	assert.Error(s.T(), err)
	assert.Equal(s.T(), ErrOooops, err)
	assert.True(s.T(), retry)
	assert.Equal(s.T(), emptyErrSource, errSource)

	pn, err := payout.Get(s.ctx, p.ID)
	assert.NoError(s.T(), err)
	assert.Equal(s.T(), payout.StatusConfirmed, pn.Status)
	assert.Equal(s.T(), int64(0), pn.OEBSPaymentID)
}

// TestProcessOEBSReconciled проверяем, что выплата подтверждена, делаем запрос в систему счетов
// (в info указан список ВЗ)
func (s *OEBSGateTestSuite) TestProcessOEBSReconciled() {
	p, _, err := s.createRandomPayoutWithARD()
	assert.NoError(s.T(), err)
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

	amount := p.Amount.String()

	statusDT := time.Now().Add(-5 * time.Minute)

	m := OEBSResponse{
		StatusDT:       statusDT.Format("2006-01-02 15:04:05"),
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
			require.Equal(s.T(), 1, len(r.Events))
			require.Equal(s.T(), confirmPayoutEvent, r.EventType)
			require.Equal(s.T(), formatAccountExtID(p), r.ExternalID)
			require.Greater(s.T(), strings.Index(r.ExternalID, ":"), 0)
			info, _ := formatConfirmInfo(p, paymentID, []string{n1.ExtID, n2.ExtID})
			require.Equal(s.T(), info, r.Info)
			require.Equal(s.T(), Debit, r.Events[0].Type)
			require.Equal(s.T(), amount, r.Events[0].Amount)
			require.Equal(s.T(), acc.PayoutTmpAccountType, r.Events[0].Loc.Type)
		},
	}

	r, _ := NewReceiver(s.ctx)
	retry, errSource, err := r.processOEBS(s.ctx, &m)
	assert.NoError(s.T(), err)
	assert.False(s.T(), retry)
	require.True(s.T(), called)
	require.Equal(s.T(), emptyErrSource, errSource)

	pn, err := payout.Get(s.ctx, p.ID)
	assert.NoError(s.T(), err)
	assert.Equal(s.T(), payout.StatusDone, pn.Status)
	assert.Equal(s.T(), paymentID, pn.OEBSPaymentID)
	require.True(s.T(), pn.OEBSStatusDt.Valid)
	assert.Equal(s.T(), statusDT.Truncate(time.Second), pn.OEBSStatusDt.Time.Truncate(time.Second))
}

// TestProcessOEBSReturned проверяем, что выплата отменена (статус выплаты переводим в Confirmed,
//  т.к. если в АРД подтвердили, то потом уже ОЕБС докатит выплату сам)
func (s *OEBSGateTestSuite) TestProcessOEBSReturned() {
	p, _, err := s.createRandomPayoutWithARD()
	assert.NoError(s.T(), err)

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
	retry, errSource, err := r.processOEBS(s.ctx, &m)
	assert.NoError(s.T(), err)
	assert.False(s.T(), retry)
	require.Len(s.T(), prod.Messages, 1)
	require.Equal(s.T(), emptyErrSource, errSource)

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
			require.Equal(s.T(), 1, len(r.Events))
			require.Equal(s.T(), cancelPayoutEvent, r.EventType)
			require.Equal(s.T(), formatAccountExtID(p), r.ExternalID)
			info, _ := formatConfirmInfo(p, paymentID, []string{n1.ExtID})
			require.Equal(s.T(), string(info), string(r.Info))
			require.Equal(s.T(), Credit, r.Events[0].Type)
			require.Equal(s.T(), p.Amount.String(), r.Events[0].Amount)
			require.Equal(s.T(), acc.PayoutTmpAccountType, r.Events[0].Loc.Type)
		},
	}

	retry, errSource, err = r.processOEBS(s.ctx, &m)
	require.NoError(s.T(), err)
	require.False(s.T(), retry)
	require.Equal(s.T(), emptyErrSource, errSource)

	pn, err = payout.Get(s.ctx, p.ID)
	require.NoError(s.T(), err)
	require.Equal(s.T(), payout.StatusConfirmed, pn.Status)
	require.True(s.T(), called)

	// убеждаемся, что статус не изменился
	where := core.WhereDesc{
		{Name: netting.IDCol, Value: n1.ID},
	}
	ns, err := netting.Get(s.ctx, where)
	require.NoError(s.T(), err)
	require.Len(s.T(), ns, 1)
	require.Equal(s.T(), netting.StatusDone, ns[0].Status)
}

// TestParseArdResponse проверяем, что данные правильно читаются из json при лишних полях
func (s *OEBSGateTestSuite) TestParseArdResponse() {
	input := []byte(`{
		"status_id": 234,
		"status_dt": "2020-11-22 11:22:33",
        "status_system": "oebs",
		"message_id": 666,
		"status_type": "NEW",
		"ard_line_id": 77,
		"payment_batch_id": 88,
		"payment_check_id": 31
	}`)
	r, err := parseArdResponse(input)
	require.NoError(s.T(), err)
	require.Equal(s.T(), int64(234), r.StatusID)
	require.Equal(s.T(), int64(31), r.PaymentCheckID)
}

func (s *OEBSGateTestSuite) TestParseArdResponseFail() {
	input := []byte(`[{
		"status_id": 234,
		"status_dt": "2020-11-22 11:22:33",
        "status_system": "oebs",
		"message_id": 666,
		"status_type": "NEW",
		"ard_line_id": 77,
		"payment_batch_id": 88,
		"payment_check_id": 0
	}]`)
	r, err := parseArdResponse(input)
	require.Error(s.T(), err)
	require.Nil(s.T(), r)
}

func (s *OEBSGateTestSuite) TestNewReceiver() {
	r, err := NewReceiver(s.ctx)
	require.NoError(s.T(), err)
	require.NotNil(s.T(), r)
	require.Equal(s.T(), s.ctx, r.ctx)
}

// TestParseOEBSResponse проверяем, что данные правильно читаются из json при лишних полях
func (s *OEBSGateTestSuite) TestParseOEBSResponse() {
	rec, err := NewReceiver(s.ctx)
	require.NoError(s.T(), err)
	input := []byte(`{
		"status_dt": "2020-11-22 11:22:33",
        "status_system": "oebs",
		"status_type": "NEW",
		"ard_line_id": 77,
		"payment_batch_id": [88],
		"payment_id": 8877
	}`)
	r, err := parseOEBSResponse(input, rec.location)
	require.NoError(s.T(), err)
	require.Equal(s.T(), int64(8877), *r.PaymentID)
}

func (s *OEBSGateTestSuite) TestParseOEBSResponseFail() {
	rec, err := NewReceiver(s.ctx)
	require.NoError(s.T(), err)
	input := []byte(`[{
		"status_dt": "2020-11-22 11:22:33",
        "status_system": "oebs",
		"status_type": "NEW",
		"ard_line_id": 77,
		"payment_batch_id": [88],
		"payment_id": 8877
	}]`)
	r, err := parseOEBSResponse(input, rec.location)
	require.Error(s.T(), err)
	require.Nil(s.T(), r)
}

// TestHandleARDFailNonOEBS проверяет, что пере посылать сообщение не будем
// просить, если источник неверный
func (s *OEBSGateTestSuite) TestHandleARDFailNonOEBS() {
	m := persqueue.ReadMessage{
		Data: []byte(`{
			"status_id": 234,
			"status_dt": "2020-11-22 11:22:33",
			"status_system": "non-oebs",
			"message_id": 666,
			"status_type": "NEW",
			"ard_line_id": 77,
			"payment_batch_id": 88
		}`),
	}
	b := persqueue.MessageBatch{
		Topic:     "eeee-dry",
		Partition: uint32(1),
		Messages:  nil,
	}

	errWriter := logbroker.ErrorWriter{
		OebsErrors: &logbroker.MockLogBrokerWriter{},
	}

	r, _ := NewReceiver(s.ctx, ReceiverWithErrWriter(&errWriter))
	err := r.HandleARD(s.ctx, m, b)
	require.NoError(s.T(), err)

	messages := errWriter.OebsErrors.(*logbroker.MockLogBrokerWriter).Messages
	require.Equal(s.T(), 1, len(messages), messages)

	parsedMsg := parseErrorMessage(s.T(), messages[0])
	require.Equal(s.T(), m.Data, parsedMsg.SourceMessage)
	require.Equal(s.T(), logbroker.UnknownSystem, parsedMsg.Source)
	require.Equal(s.T(), true, parsedMsg.DryRun)
	require.Equal(s.T(), ARD, parsedMsg.Module)
}

// TestHandleARDFailEmptyMsg пере посылать сообщение не будем просить, если источник формат неверный
func (s *OEBSGateTestSuite) TestHandleARDFailEmptyMsg() {
	m := persqueue.ReadMessage{
		Data: []byte(`["id": "wrong-id?"]`),
	}
	b := persqueue.MessageBatch{
		Topic:     "eeee",
		Partition: uint32(1),
		Messages:  nil,
	}
	r, _ := NewReceiver(s.ctx)
	err := r.HandleARD(s.ctx, m, b)
	require.NoError(s.T(), err)
}

// TestHandleOEBSFailNonOEBS пере посылать сообщение не будем просить, если источник неверный
func (s *OEBSGateTestSuite) TestHandleOEBSFailNonOEBS() {
	m := persqueue.ReadMessage{
		Data: []byte(`{
			"status_dt": "2020-11-22 11:22:33",
			"status_system": "non-oebs",
			"status_type": "NEW",
			"ard_line_id": 77,
			"payment_batch_id": [88],
			"payment_id": 8877
		}`),
	}
	b := persqueue.MessageBatch{
		Topic:     "eeee",
		Partition: uint32(1),
		Messages:  nil,
	}
	errWriter := logbroker.ErrorWriter{
		OebsErrors: &logbroker.MockLogBrokerWriter{},
	}
	r, _ := NewReceiver(s.ctx, ReceiverWithErrWriter(&errWriter))
	err := r.HandleOEBS(s.ctx, m, b)
	require.NoError(s.T(), err)

	messages := errWriter.OebsErrors.(*logbroker.MockLogBrokerWriter).Messages
	require.Equal(s.T(), 1, len(messages), messages)

	parsedMsg := parseErrorMessage(s.T(), messages[0])
	require.Equal(s.T(), m.Data, parsedMsg.SourceMessage)
	require.Equal(s.T(), logbroker.UnknownSystem, parsedMsg.Source)
	require.Equal(s.T(), false, parsedMsg.DryRun)
	require.Equal(s.T(), OEBS, parsedMsg.Module)
	require.Equal(s.T(), "billing", parsedMsg.System)
}

// TestHandleOEBSFailEmptyMsg пере посылать сообщение не будем просить, если источник формат неверный
func (s *OEBSGateTestSuite) TestHandleOEBSFailEmptyMsg() {
	m := persqueue.ReadMessage{
		Data: []byte(`["id": "wrong-id?"]`),
	}
	b := persqueue.MessageBatch{
		Topic:     "eeee-dry",
		Partition: uint32(1),
		Messages:  nil,
	}
	errWriter := logbroker.ErrorWriter{
		OebsErrors: &logbroker.MockLogBrokerWriter{},
	}
	r, _ := NewReceiver(s.ctx, ReceiverWithErrWriter(&errWriter))
	err := r.HandleOEBS(s.ctx, m, b)
	require.NoError(s.T(), err)

	messages := errWriter.OebsErrors.(*logbroker.MockLogBrokerWriter).Messages
	require.Equal(s.T(), 1, len(messages), messages)

	parsedMsg := parseErrorMessage(s.T(), messages[0])
	require.Equal(s.T(), m.Data, parsedMsg.SourceMessage)
	require.Equal(s.T(), logbroker.ParseMessage, parsedMsg.Source)
	require.Equal(s.T(), true, parsedMsg.DryRun)
	require.Equal(s.T(), OEBS, parsedMsg.Module)
}

// TestDoubleARDResponse проверяем, что два отказа из АРД не приводит к двойному возврату средств
//    - отказ - вернули деньги
//    - new   - обновил статус в confirmed
//    - отказ - опять вернем денег?
//
//    - rejected, done - финал статусы. ничего больше делать нельзя
func (s *OEBSGateTestSuite) TestDoubleARDResponse() {
	p, _, err := s.createRandomPayoutWithRequest()
	assert.NoError(s.T(), err)
	amount := p.Amount.String()

	info := "Договор не найден"

	m := ARDResponse{
		StatusSystem: OEBS,
		MessageID:    p.ID,
		StatusType:   ARDStatusError,
		StatusInfo:   info,
	}

	// Успешный возврат средств
	s.ctx.Clients.AccountsBatch = &MockAccountBatch{
		TestWrite: func(r *entities.BatchWriteRequest) {
			// проверяем, что отправили в систему счетов
			assert.Equal(s.T(), 2, len(r.Events))
			assert.Equal(s.T(), cancelReservePayoutEvent, r.EventType)
			assert.Equal(s.T(), fmt.Sprintf("%d", p.ID), r.ExternalID)
			assert.Equal(s.T(), Debit, r.Events[0].Type)
			assert.Equal(s.T(), Credit, r.Events[1].Type)
			info, _ := formatConfirmInfo(p, p.OEBSPaymentID, []string{})
			require.Equal(s.T(), string(info), string(r.Info))
			assert.Equal(s.T(), amount, r.Events[0].Amount)
			assert.Equal(s.T(), amount, r.Events[1].Amount)
		},
	}

	var CheckPayoutIsRejected = func(id int64) {
		pn, err := payout.Get(s.ctx, id)
		assert.NoError(s.T(), err)
		assert.Equal(s.T(), payout.StatusRejected, pn.Status)
		require.NotNil(s.T(), pn.Error)
		assert.Equal(s.T(), info, *pn.Error)
		assert.Equal(s.T(), int64(0), p.OEBSBatchID)
	}

	r, _ := NewReceiver(s.ctx)
	retry, errSource, err := r.processARD(s.ctx, &m)
	assert.NoError(s.T(), err)
	assert.False(s.T(), retry)
	assert.Equal(s.T(), emptyErrSource, errSource)

	CheckPayoutIsRejected(p.ID)

	// сообщение об успехе
	batchID := bt.RandN64()
	mNew := ARDResponse{
		StatusSystem:   OEBS,
		MessageID:      p.ID,
		StatusType:     ARDStatusNew,
		StatusInfo:     info,
		PaymentBatchID: &batchID,
	}
	retry, errSource, err = r.processARD(s.ctx, &mNew)
	assert.NoError(s.T(), err)
	assert.False(s.T(), retry)
	assert.Equal(s.T(), fmt.Sprintf(logbroker.WrongPayoutStatus, payout.StatusRejected), errSource)

	CheckPayoutIsRejected(p.ID)

	// отправляем второй возврат, (просто повтор, например)
	// не должны еще раз переводить средства
	s.ctx.Clients.AccountsBatch = &MockAccountBatch{
		TestWrite: func(r *entities.BatchWriteRequest) {
			assert.Fail(s.T(), "Double amount restore")
		},
	}

	retry, errSource, err = r.processARD(s.ctx, &m)
	assert.NoError(s.T(), err)
	assert.False(s.T(), retry)
	assert.Equal(s.T(), fmt.Sprintf(logbroker.WrongPayoutStatus, payout.StatusRejected), errSource)

	CheckPayoutIsRejected(p.ID)
}

// проверяет, что если есть выплаты в конечном статусе, то мы их исключаем из запроса
// в систему счетов и "дырок" в теле запроса нет. Так же, убеждаемся, что подтверждения идут
// отдельными запросами в систему счетов, а не общим запросом
func (s *OEBSGateTestSuite) TestConfirmPaymentWithSomeRejected() {
	p1, _, err := s.createRandomPayoutWithARD() // подтвержден
	assert.NoError(s.T(), err)
	p2, err := s.createRandomPayout() // выполнен
	assert.NoError(s.T(), err)
	p3, _, err := s.createRandomPayoutWithARD() // подтвержден
	assert.NoError(s.T(), err)

	// p2 уже выполнен и в тело запроса к системе счетов попасть не должен
	update := core.UpdateDesc{
		{Name: payout.StatusCol, Value: payout.StatusDone},
		{Name: payout.BatchIDCol, Value: p1.OEBSBatchID},
	}
	assert.NoError(s.T(), payout.UpdateX(s.ctx, p2.ID, update))

	// p3 ссылается на тот же batch
	update = core.UpdateDesc{
		{Name: payout.BatchIDCol, Value: p1.OEBSBatchID},
	}
	assert.NoError(s.T(), payout.UpdateX(s.ctx, p3.ID, update))

	// Успешное списание средств (вызывается по одному разу на каждую выплату)
	paymentID := bt.RandN64()
	testCount := 0
	s.ctx.Clients.AccountsBatch = &MockAccountBatch{
		TestWrite: func(r *entities.BatchWriteRequest) {
			// проверяем, что отправили в систему счетов
			assert.Equal(s.T(), confirmPayoutEvent, r.EventType)
			assert.Equal(s.T(), 1, len(r.Events))
			info, _ := formatConfirmInfo(p1, paymentID, []string{})
			assert.Equal(s.T(), info, r.Info)
			if testCount == 0 {
				assert.Equal(s.T(), formatAccountExtID(p1), r.ExternalID)
				assert.Equal(s.T(), *r.Events[0].Loc.Attributes["contract_id"], strconv.FormatInt(p1.ContractID, 10))
			}
			if testCount == 1 {
				assert.Equal(s.T(), formatAccountExtID(p3), r.ExternalID)
				assert.Equal(s.T(), *r.Events[0].Loc.Attributes["contract_id"], strconv.FormatInt(p3.ContractID, 10))
			}
			if testCount >= 2 {
				assert.Fail(s.T(), "Called more than 2 times")
			}
			testCount++
		},
	}

	m := OEBSResponse{
		StatusDT:       time.Now().Format("2006-01-02 15:04:05"),
		StatusSystem:   OEBS,
		StatusType:     OEBSStatusReconciled,
		PaymentBatchID: []int64{p1.OEBSBatchID},
		PaymentID:      &paymentID,
	}

	r, _ := NewReceiver(s.ctx)
	retry, errSource, err := r.processOEBS(s.ctx, &m)
	assert.NoError(s.T(), err)
	assert.False(s.T(), retry)
	assert.Equal(s.T(), emptyErrSource, errSource)

	// проверяем, что статусы выплат изменились
	for _, pID := range []int64{p1.ID, p3.ID} {
		pn, err := payout.Get(s.ctx, pID)
		assert.NoError(s.T(), err)
		assert.Equal(s.T(), payout.StatusDone, pn.Status)
	}
}

// TestHandleARDWithCPF проверяем, что при подтверждении из АРД мы отправляем в баланс данные
func (s *OEBSGateTestSuite) TestHandleARDWithCPF() {
	p, err := s.createRandomPayout()
	require.NoError(s.T(), err)

	batchID := bt.RandN64()
	m := ARDResponse{
		StatusSystem:   OEBS,
		MessageID:      p.ID,
		StatusType:     ARDStatusNew,
		PaymentBatchID: &batchID,
	}

	tx := s.Tx()
	_, err = cpf.CreateTx(s.ctx, tx, &cpf.NewCPF{
		PayoutID: p.ID,
		OpType:   "INSERT1",
		ExtID:    "ЛСТ-1122",
		Amount:   decimal.RequireFromString("123.55"),
		DryRun:   false,
	})
	require.NoError(s.T(), err)
	_, err = cpf.CreateTx(s.ctx, tx, &cpf.NewCPF{
		PayoutID: p.ID,
		OpType:   "INSERT2",
		ExtID:    "ЛСТ-666",
		Amount:   decimal.RequireFromString("666.77"),
		DryRun:   false,
	})
	require.NoError(s.T(), tx.Commit())
	require.NoError(s.T(), err)

	r, _ := NewReceiver(s.ctx)
	r.flags.syncCPF = true

	s.ctx.Clients.CpfClient = &client.SenderMock{
		TestSend: func(facts []any) {
			require.Len(s.T(), facts, 2)
			require.Equal(s.T(), "INSERT1", facts[0].(cpf.BalanceCPF).OpType)
			require.Equal(s.T(), "INSERT2", facts[1].(cpf.BalanceCPF).OpType)
		},
	}

	retry, errSource, err := r.processARD(s.ctx, &m)
	require.NoError(s.T(), err)
	require.False(s.T(), retry)
	assert.Equal(s.T(), emptyErrSource, errSource)

	// Проверяем, что статус CPF в БД поменялся
	where := core.WhereDesc{
		{Name: cpf.PayoutIDCol, Value: p.ID},
	}
	facts, err := cpf.Get(s.ctx, where, false)
	require.NoError(s.T(), err)
	require.Len(s.T(), facts, 2)
	require.Equal(s.T(), cpf.StatusDone, facts[0].Status)
	require.Equal(s.T(), cpf.StatusDone, facts[1].Status)
}

// TestHandleARDWithCPFForProcess проверяем, что если приходит сообщение PROCESSED, то мы
// не отправляем ничего в баланс (перед этим пришло NEW и тогда уже отправили все в баланс)
func (s *OEBSGateTestSuite) TestHandleARDWithCPFForProcess() {
	p, err := s.createRandomPayout()
	require.NoError(s.T(), err)

	batchID := bt.RandN64()
	m := ARDResponse{
		StatusSystem:   OEBS,
		MessageID:      p.ID,
		StatusType:     ARDStatusProcessed,
		PaymentBatchID: &batchID,
	}

	r, _ := NewReceiver(s.ctx)
	r.flags.syncCPF = true

	s.ctx.Clients.CpfClient = &client.SenderMock{
		TestSend: func(_ []any) {
			require.Fail(s.T(), "Should not be called")
		},
	}

	retry, errSource, err := r.processARD(s.ctx, &m)
	require.NoError(s.T(), err)
	require.False(s.T(), retry)
	assert.Equal(s.T(), emptyErrSource, errSource)
}

// TestHandleARDWithCPFWith0CPFinDB проверяем, что если в БД 0 записей для CPF, мы не ломаемся
func (s *OEBSGateTestSuite) TestHandleARDWithCPFWith0CPFinDB() {
	p, err := s.createRandomPayout()
	require.NoError(s.T(), err)

	batchID := bt.RandN64()
	m := ARDResponse{
		StatusSystem:   OEBS,
		MessageID:      p.ID,
		StatusType:     ARDStatusNew,
		PaymentBatchID: &batchID,
	}

	r, _ := NewReceiver(s.ctx)
	r.flags.syncCPF = true

	s.ctx.Clients.CpfClient = &client.SenderMock{
		TestSend: func(facts []any) {
			require.Len(s.T(), facts, 0)
		},
	}

	retry, errSource, err := r.processARD(s.ctx, &m)
	require.NoError(s.T(), err)
	require.False(s.T(), retry)
	assert.Equal(s.T(), emptyErrSource, errSource)
}

// TestHandleARDWrongPayout проверяет, что если указанный message_id не существует,
// то мы не повторяем.  Иначе остановится обработка всей очереди
func (s *OEBSGateTestSuite) TestHandleARDWrongPayout() {
	m := ARDResponse{
		StatusSystem: OEBS,
		MessageID:    int64(-1),
		StatusType:   ARDStatusNew,
	}

	r, _ := NewReceiver(s.ctx)
	retry, errSource, err := r.processARD(s.ctx, &m)
	require.Error(s.T(), err, sql.ErrNoRows)
	require.False(s.T(), retry)
	require.Equal(s.T(), logbroker.NoPayout, errSource)
}

// TestHandleARDDBError проверяет, что если ошибка в базе любая, кроме отсутствия результата,
// мы ретраим processARD
func (s *OEBSGateTestSuite) TestHandleARDDBError() {
	m := ARDResponse{
		StatusSystem: OEBS,
		MessageID:    int64(1),
		StatusType:   ARDStatusNew,
	}

	r, _ := NewReceiver(s.ctx)
	r.payoutDB = &MockPayoutDB{
		GetDBErr: driver.ErrBadConn,
	}
	retry, errSource, err := r.processARD(s.ctx, &m)
	require.Error(s.T(), err, driver.ErrBadConn)
	require.True(s.T(), retry)
	assert.Equal(s.T(), emptyErrSource, errSource)
}

// TestProcessOEBSWrongBatch проверяет, что для неизвестной выплаты повторять не будем
func (s *OEBSGateTestSuite) TestProcessOEBSWrongBatch() {
	paymentID := bt.RandN64()
	m := OEBSResponse{
		StatusDT:       time.Now().Format("2006-01-02 15:04:05"),
		StatusSystem:   OEBS,
		StatusType:     OEBSStatusReconciled,
		PaymentBatchID: []int64{-2},
		PaymentID:      &paymentID,
	}

	r, _ := NewReceiver(s.ctx)
	retry, errSource, err := r.processOEBS(s.ctx, &m)
	require.NoError(s.T(), err)
	require.False(s.T(), retry)
	require.Equal(s.T(), emptyErrSource, errSource)
}

// TestBalanceCPFToJSON проверяет конвертацию в JSON
func (s *OEBSGateTestSuite) TestBalanceCPFToJSON() {
	var i any = cpf.BalanceCPF{
		ID:     int64(100),
		Amount: decimal.RequireFromString("123.789"),
		ExtID:  "ЛСТ-123444",
		DT:     "2020-05-01",
		OpType: "INSERT_NETTING",
	}
	expected := `{"id":100,"amount":"123.789","receipt_number":"ЛСТ-123444","receipt_date":"2020-05-01","operation_type":"INSERT_NETTING"}`

	// $ TVM_TICKET=$(ya tool tvmknife get_service_ticket sshkey -s 2025544 -d 2000601 -l shorrty)
	// $ curl -i -k -X POST https://balance-xmlrpc-tvm-tm.paysys.yandex.net:8004/httpapitvm/create_cash_payment_fact \
	//		-H "Content-Type: application/json" \
	//		-H "X-Ya-Service-Ticket: ${TVM_TICKET}" \
	//		-d '{"id":100,"amount":"123.789","receipt_number":"ЛСТ-123444","receipt_date":"2020-05-01","operation_type":"INSERT_NETTING"}'
	// HTTP/1.1 200 OK
	// Server: nginx
	// Date: Fri, 16 Apr 2021 08:01:40 GMT
	// Content-Type: text/plain
	// Content-Length: 2
	// Connection: keep-alive
	// Strict-Transport-Security: max-age=31536000
	//
	// OK

	j, err := json.Marshal(i)
	require.NoError(s.T(), err)
	require.Equal(s.T(), expected, string(j))
}

// TestOEBSPaymentFieldsSave проверяет, что сохраняем поля с информацией про платеж, которые получает от OEBS
func (s *OEBSGateTestSuite) TestOEBSPaymentFieldsSave() {
	p, _, err := s.createRandomPayoutWithARD()
	assert.NoError(s.T(), err)

	m := OEBSResponse{
		StatusDT:        time.Now().Format("2006-01-02 15:04:05"),
		StatusSystem:    OEBS,
		StatusType:      OEBSStatusTransmitted,
		PaymentBatchID:  []int64{p.OEBSBatchID},
		PaymentAmount:   p.AmountOEBS,
		PaymentPurpose:  "purpose",
		TotalBatchCount: 2,
	}

	// на этот статус не должны ходить в систему счетов
	s.ctx.Clients.AccountsBatch = &MockAccountBatch{
		TestWrite: func(r *entities.BatchWriteRequest) {
			assert.Fail(s.T(), "Trying to revert funds")
		},
	}

	require.NoError(s.T(), err)

	r, _ := NewReceiver(s.ctx)
	retry, errSource, err := r.processOEBS(s.ctx, &m)
	require.NoError(s.T(), err)
	require.False(s.T(), retry)
	require.Equal(s.T(), emptyErrSource, errSource)

	pn, err := payout.Get(s.ctx, p.ID)
	require.NoError(s.T(), err)
	require.Equal(s.T(), payout.StatusConfirmed, pn.Status)
	require.Equal(s.T(), m.PaymentPurpose, pn.OEBSPaymentPurpose)
	require.Equal(s.T(), OEBSStatusTransmitted, pn.OEBSStatus)
	require.Equal(s.T(), m.TotalBatchCount, pn.OEBSTotalBatchCount)
	require.Equal(s.T(), p.AmountOEBS, pn.OEBSPaymentAmount)
}

// TestHandleARDWithCPFDryRun проверяем, что при запуске в dry_run
// отправляем CPF с помощью dry_run клиента
func (s *OEBSGateTestSuite) TestHandleARDWithCPFDryRun() {
	p, err := s.createRandomPayout()
	require.NoError(s.T(), err)

	batchID := bt.RandN64()
	m := ARDResponse{
		StatusSystem:   OEBS,
		MessageID:      p.ID,
		StatusType:     ARDStatusNew,
		PaymentBatchID: &batchID,
	}

	tx := s.Tx()
	_, err = cpf.CreateTx(s.ctx, tx, &cpf.NewCPF{
		PayoutID: p.ID,
		OpType:   "INSERT1",
		ExtID:    "ЛСТ-1337",
		Amount:   decimal.RequireFromString("13.37"),
		DryRun:   true,
	})
	require.NoError(s.T(), tx.Commit())
	require.NoError(s.T(), err)

	r, _ := NewReceiver(s.ctx)
	r.flags.syncCPF = true
	clientTrue := client.InteractionsClientMock{
		TestMakeRequestRaw: func(ctx context.Context, request interactions.Request) *interactions.RawResponse {
			require.Fail(s.T(), "Should not be called in dry run")
			return nil
		},
	}
	clientDryRun := client.InteractionsClientMock{
		TestMakeRequestRaw: func(ctx context.Context, request interactions.Request) *interactions.RawResponse {
			return &interactions.RawResponse{
				Error:    nil,
				Response: []byte("OK"),
			}
		},
	}

	cpfClient := client.HTTPSender{
		Client:       &clientTrue,
		ClientDryRun: &clientDryRun,
	}

	s.ctx.Clients.CpfClient = &cpfClient

	retry, errSource, err := r.processARD(s.ctx, &m)
	require.NoError(s.T(), err)
	require.False(s.T(), retry)
	assert.Equal(s.T(), emptyErrSource, errSource)
}

// TestPayoutLock проверяет, что receiver ждет, пока sender не отпустит блокировку строк в t_payout
func (s *OEBSGateTestSuite) TestPayoutLock() {
	s.SetProcessDryRun(false)
	p, _, err := s.createRandomPayoutWithRequest()
	require.NoError(s.T(), err)

	// Берем лок на выплату, как это делает sender
	p, tx, err := s.pDB.GetTx(s.ctx, p.ID, true)
	require.NoError(s.T(), err)
	accLockID := "888"
	s.ctx.Clients.AccountsLocker = &MockAccounterLocker{
		TestRemove: func(ctx context.Context, attributes *entities.LocationAttributes, uid string) {
			assert.Equal(s.T(), accLockID, uid)
		},
	}
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
						UID: accLockID,
					},
				},
			}, nil
		},
	}

	lbWriters := logbroker.NewEmptyWriters()
	lbWriters.SetWriter(false, false, &logbroker.MockLogBrokerWriter{Err: ErrOooops})
	lbWriters.SetWriter(true, false, &logbroker.MockLogBrokerWriter{})

	namedWriters := logbroker.NamedWriters{"taxi": lbWriters}
	ps := NewPaymentSender(s.ctx, &namedWriters)
	ps.flags.syncCPF = false
	ps.flags.syncNetting = false
	_, err = ps.sendOne(p, tx)
	require.NoError(s.T(), err)

	batchID := bt.RandN64()
	m := ARDResponse{
		StatusSystem:   OEBS,
		MessageID:      p.ID,
		StatusType:     ARDStatusNew,
		PaymentBatchID: &batchID,
	}

	r, _ := NewReceiver(s.ctx)

	c := make(chan error, 1)
	go func() {
		_, _, err := r.processARD(s.ctx, &m)
		c <- err
	}()
	var timeout bool

	// проверяем, что processARD ждет снятия блокировки
	select {
	case <-c:
		timeout = false
	case <-time.After(5 * time.Second):
		timeout = true
	}
	require.True(s.T(), timeout)

	// до снятия блокировки выплата должна все еще находиться в статусе New
	pn, err := payout.Get(s.ctx, p.ID)
	require.NoError(s.T(), err)
	require.Equal(s.T(), payout.StatusNew, pn.Status, pn.ID)

	// после снятия блокировки ждем, пока отработает processARD, проверяем, что выплата в статусе Confirmed
	_ = tx.Commit()
	errRes := <-c
	require.NoError(s.T(), errRes)
	pn, err = payout.Get(s.ctx, p.ID)
	require.NoError(s.T(), err)
	require.Equal(s.T(), payout.StatusConfirmed, pn.Status, pn.ID)

}

// TestGetCPFLowerBound проверяет, что по выплате мы достаем CPF'ы, созданные не ранее, чем сама выплата
func (s *OEBSGateTestSuite) TestGetCPFLowerBound() {
	p, err := s.createRandomPayout()
	require.NoError(s.T(), err)
	require.NoError(s.T(), payout.Update(s.ctx, p.ID, payout.StatusCol, payout.StatusPending))
	require.NoError(s.T(), payout.Update(s.ctx, p.ID, payout.DryRunCol, false))

	// создаем два CPF'а по выплате, одному меняем дату создания на дату ранее, чем у выплаты
	tx := s.Tx()
	c1, err := cpf.CreateTx(s.ctx, tx, &cpf.NewCPF{
		PayoutID: p.ID,
		OpType:   "INSERT1",
		ExtID:    "ЛСТ-1337",
		Amount:   decimal.RequireFromString("13.37"),
		DryRun:   false,
	})
	require.NoError(s.T(), err)
	c2, err := cpf.CreateTx(s.ctx, tx, &cpf.NewCPF{
		PayoutID: p.ID,
		OpType:   "INSERT1",
		ExtID:    "ЛСТ-1337",
		Amount:   decimal.RequireFromString("13.37"),
		DryRun:   false,
	})

	require.NoError(s.T(), tx.Commit())
	require.NoError(s.T(), err)
	where := core.WhereDescX{
		sq.Eq{cpf.IDCol: c1.ID},
	}
	update := core.UpdateDesc{
		{Name: cpf.CreateDtCol, Value: p.CreateDt.Add(time.Second * (-20))},
	}
	require.NoError(s.T(), cpf.UpdateXX(s.ctx, where, update))

	batchID := bt.RandN64()
	m := ARDResponse{
		StatusSystem:   OEBS,
		MessageID:      p.ID,
		StatusType:     ARDStatusNew,
		PaymentBatchID: &batchID,
	}

	clientTrue := client.InteractionsClientMock{
		TestMakeRequestRaw: func(ctx context.Context, request interactions.Request) *interactions.RawResponse {
			return &interactions.RawResponse{
				Error:    nil,
				Response: []byte("OK"),
			}
		},
	}
	s.ctx.Clients.CpfClient = &client.HTTPSender{Client: &clientTrue}

	r, _ := NewReceiver(s.ctx)
	r.flags.syncCPF = true
	_, _, err = r.processARD(s.ctx, &m)
	require.NoError(s.T(), err)

	// проверяем, что первый CPF в статусе New, т.е. не обработан
	nc1, err := cpf.Get(s.ctx, core.WhereDesc{{Name: cpf.IDCol, Value: c1.ID}}, false)
	require.NoError(s.T(), err)
	require.Equal(s.T(), cpf.StatusNew, nc1[0].Status)

	// второй CPF в статусе Done, т.е. обработан
	nc2, err := cpf.Get(s.ctx, core.WhereDesc{{Name: cpf.IDCol, Value: c2.ID}}, false)
	require.NoError(s.T(), err)
	require.Equal(s.T(), cpf.StatusDone, nc2[0].Status)

}
