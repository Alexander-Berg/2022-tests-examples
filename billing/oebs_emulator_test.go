package tasks

import (
	"encoding/json"
	"fmt"

	"github.com/shopspring/decimal"
	"github.com/stretchr/testify/require"

	"a.yandex-team.ru/billing/hot/payout/internal/logbroker"
	"a.yandex-team.ru/billing/hot/payout/internal/notifier"
	"a.yandex-team.ru/billing/hot/payout/internal/oebsgate"
	"a.yandex-team.ru/billing/hot/payout/internal/payout"
	bt "a.yandex-team.ru/billing/library/go/billingo/pkg/testing"
	"a.yandex-team.ru/billing/library/go/billingo/pkg/xlog"
	"a.yandex-team.ru/kikimr/public/sdk/go/persqueue"
	"a.yandex-team.ru/library/go/core/log"
)

func (s *TasksTestSuite) createRandomPayout() (*payout.Payout, error) {
	return payout.Create(s.ctx, &payout.NewPayout{
		ExternalID: bt.RandS(30),
		ServiceID:  7,
		ContractID: bt.RandN64(),
		Amount:     decimal.RequireFromString("124.666"),
		Currency:   "RUB",
		Namespace:  notifier.DefaultNamespace,
		ClientID:   bt.RandN64(),
	})
}

func (s *TasksTestSuite) TestFormatARD() {
	plb := oebsgate.NewPayoutLB{
		MessageID: bt.RandN64(),
	}
	msg, err := formatARDMessage(s.ctx, &plb)
	require.NoError(s.T(), err)
	require.NotEmpty(s.T(), msg)

	var resp oebsgate.ARDResponse
	require.NoError(s.T(), json.Unmarshal(msg, &resp))

	require.Equal(s.T(), plb.MessageID, resp.MessageID)
	require.Greater(s.T(), int64(0), *resp.PaymentBatchID)
	require.Equal(s.T(), resp.PaymentCheckID, *resp.PaymentBatchID)
}

func (s *TasksTestSuite) TestReceiveNewPayment() {
	p, err := s.createRandomPayout()
	require.NoError(s.T(), err)
	require.NoError(s.T(), payout.Update(s.ctx, p.ID, payout.StatusCol, payout.StatusPending))
	xlog.Info(s.ctx, "emulate for payout", log.Any("payout", p))

	ardWriter := logbroker.MockLogBrokerWriter{}
	oebsWriter := logbroker.MockLogBrokerWriter{Err: ErrOooops}

	emulator, err := NewOEBSEmulator(s.ctx, nil)
	require.NoError(s.T(), err)
	emulator.ARDResponse.AddProducer(notifier.DefaultNamespace, &ardWriter)
	emulator.OEBSResponse.AddProducer(notifier.DefaultNamespace, &oebsWriter)

	var batchID int64
	go func() {
		for len(ardWriter.Messages) == 0 {
		}
		oebsWriter = logbroker.MockLogBrokerWriter{}
		emulator.OEBSResponse.AddProducer(notifier.DefaultNamespace, &oebsWriter)

		var resp oebsgate.ARDResponse
		require.Len(s.T(), ardWriter.Messages, 1)
		require.NoError(s.T(), json.Unmarshal(ardWriter.Messages[0], &resp))
		xlog.Info(s.ctx, "ard message", log.ByteString("msg", ardWriter.Messages[0]))
		batchID = *resp.PaymentBatchID

		require.NoError(s.T(), payout.Update(s.ctx, p.ID, payout.OEBSStatusCol, resp.StatusType))
		require.NoError(s.T(), payout.Update(s.ctx, p.ID, payout.BatchIDCol, batchID))
		require.NoError(s.T(), payout.Update(s.ctx, p.ID, payout.StatusCol, payout.StatusConfirmed))
	}()

	err = emulator.HandleNewPayment(s.ctx, persqueue.ReadMessage{Data: []byte(fmt.Sprintf(`{
"message_id": %v,
"service_id": %v,
"partner_currency": "%v",
"billing_contract_id": %v,
"billing_client_id": %v
}`,
		p.ID, p.ServiceID, p.Currency, p.ContractID, p.ClientID),
	)}, persqueue.MessageBatch{})
	require.NoError(s.T(), err)
	require.Len(s.T(), oebsWriter.Messages, 2)

	var created, reconciled oebsgate.OEBSResponse
	require.NoError(s.T(), json.Unmarshal(oebsWriter.Messages[0], &created))
	require.NoError(s.T(), json.Unmarshal(oebsWriter.Messages[1], &reconciled))
	xlog.Info(s.ctx, "created", log.Any("msg", created))
	xlog.Info(s.ctx, "reconciled", log.Any("msg", reconciled))

	require.Equal(s.T(), created.PaymentID, reconciled.PaymentID)
	require.Len(s.T(), created.PaymentBatchID, 1)
	require.Equal(s.T(), batchID, created.PaymentBatchID[0])
	require.Equal(s.T(), created.PaymentBatchID, reconciled.PaymentBatchID)
	require.Equal(s.T(), created.PaymentAmount, p.AmountOEBS)
}
