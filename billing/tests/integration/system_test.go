package integration

import (
	"context"
	"fmt"
	"os"
	"strconv"
	"testing"
	"time"

	"github.com/stretchr/testify/require"

	"a.yandex-team.ru/billing/hot/accounts/pkg/core/entities"
	"a.yandex-team.ru/billing/library/go/billingo/pkg/interactions"
	"a.yandex-team.ru/billing/library/go/billingo/pkg/interactions/accounts"
	"a.yandex-team.ru/billing/library/go/billingo/pkg/interactions/payout"
	bt "a.yandex-team.ru/billing/library/go/billingo/pkg/testing"
	"a.yandex-team.ru/library/go/core/metrics/solomon"
)

func getPayoutAccountAttrs(contractID, serviceID, clientID int64) map[string]*string {
	cid := strconv.FormatInt(contractID, 10)
	sid := strconv.FormatInt(serviceID, 10)
	clid := strconv.FormatInt(clientID, 10)
	cur := "RUB"

	return map[string]*string{
		"contract_id": &cid,
		"currency":    &cur,
		"service_id":  &sid,
		"client_id":   &clid,
	}
}

func getIncomingPaymentsAccountAttrs(contractID, clientID int64, operationType, invoiceID string) map[string]*string {
	cid := strconv.FormatInt(contractID, 10)
	clid := strconv.FormatInt(clientID, 10)
	cur := "RUB"

	return map[string]*string{
		"contract_id":    &cid,
		"currency":       &cur,
		"client_id":      &clid,
		"operation_type": &operationType,
		"invoice_id":     &invoiceID,
	}
}

func TestApi(t *testing.T) {
	registry := solomon.NewRegistry(nil)
	client, err := payout.New(payout.Config{Transport: interactions.Config{
		BaseURL: os.Getenv("PAYOUTS_BASE_URL"),
		Name:    "payouts",
	}}, nil, registry)
	require.NoError(t, err)

	aclient, err := accounts.New(accounts.Config{Transport: interactions.Config{
		BaseURL: os.Getenv("ACCOUNTS_BASE_URL"),
		Name:    "accounts",
	}}, nil, registry)
	require.NoError(t, err)

	clientID := bt.RandN64()
	serviceID := int64(124)
	contractID := bt.RandN64()
	cpfID := "ЛСТ-666153102-1"
	operationType := "INSERT_YA_NETTING"

	tests := []struct {
		testName  string
		namespace string
		batch     entities.BatchWriteRequest
		clientID  int64
		cpfOnly   bool
	}{
		{
			testName:  "default payout",
			namespace: "taxi",
			batch: entities.BatchWriteRequest{
				EventType:  "system:payout",
				ExternalID: bt.RandS(30),
				Dt:         time.Now().Add(-2 * time.Second),
				Info: []byte(fmt.Sprintf(`{
		 "client_id": 92698540,
		 "event_time": "2021-03-23T17:01:00+00:00",
		 "transaction_id": "5494732C-60E4-4E84-A727-83F9D0996170",
		 "tariffer_payload": {
			"dry_run": true,
			"common_ts": 1616518860,
			"contract_states": {
			  "%d": {
				"netting_done": true,
				"payment_amount": 100,
				"payment_currency": "RUB",
				"invoices": [
					{"id": 1231123, "operation_type": "INSERT", "external_id": "%s", "amount": 100}
				]
			  }
			}
		 }
		}`, contractID, cpfID)),
				Events: []entities.EventAttributes{{
					Loc: entities.LocationAttributes{
						Namespace:  "taxi",
						Type:       "payout",
						Attributes: getPayoutAccountAttrs(contractID, serviceID, clientID),
					},
					Type:   "credit",
					Dt:     time.Now().Add(-2 * time.Second),
					Amount: "100",
				}},
			},
			clientID: clientID,
			cpfOnly:  false,
		},
		{
			testName:  "cpf-only payout",
			namespace: "taxi_light",
			batch: entities.BatchWriteRequest{
				EventType:  "system:incoming_payments_sent",
				ExternalID: bt.RandS(30),
				Dt:         time.Now().Add(-2 * time.Second),
				Info: []byte(fmt.Sprintf(`{
		"client_id": 92698540,
		"event_time": "2021-03-23T17:01:00+00:00",
		"transaction_id": "5494732C-60E4-4E84-A727-83F9D0996170",
		"tariffer_payload": {
			"dry_run": true,
			"common_ts": 1616518860,
			"contract_states": {
			  "%d": {
				"netting_done": true,
				"payment_amount": 100,
				"payment_currency": "RUB",
				"invoices": [
					{"id": 1231123, "operation_type": "INSERT", "external_id": "%s", "amount": 100}
				]
			  }
			}
		}
		}`, contractID, cpfID)),
				Events: []entities.EventAttributes{{
					Loc: entities.LocationAttributes{
						Namespace:  "taxi_light",
						Type:       "incoming_payments_sent",
						Attributes: getIncomingPaymentsAccountAttrs(contractID, clientID, operationType, "ЛСТ-1"),
					},
					Type:   "credit",
					Dt:     time.Now().Add(-2 * time.Second),
					Amount: "100",
				}},
			},
			clientID: clientID,
			cpfOnly:  true,
		},
	}

	for _, test := range tests {
		test := test
		t.Run(test.testName, func(t *testing.T) {
			ctx, cancel := context.WithTimeout(context.Background(), 10*time.Second)
			defer cancel()
			_, err = aclient.WriteBatch(ctx, &test.batch)
			require.NoError(t, err)

			err = client.PayoutByClient(ctx, payout.ByClientRequest{
				ClientID:   clientID,
				ExternalID: bt.RandS(30),
				Namespace:  test.namespace,
				CpfOnly:    test.cpfOnly,
			})
			require.NoError(t, err)
		})
	}
}
