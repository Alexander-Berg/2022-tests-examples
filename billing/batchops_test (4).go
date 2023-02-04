package accounts

import (
	"context"
	"encoding/json"
	"fmt"
	"testing"
	"time"

	"github.com/stretchr/testify/assert"

	"a.yandex-team.ru/billing/hot/accounts/pkg/core/entities"
)

func TestClient_WriteBatch(t *testing.T) {
	batchID := int64(66666666666666666)
	attr1 := "v1"
	attr2 := "v2"
	attr3 := "v3"
	batchRequest := entities.BatchWriteRequest{
		EventType:  "batch",
		ExternalID: "1234",
		Dt:         time.Unix(1606666666, 0).In(time.UTC),
		Info:       []byte(`["some","info"]`),
		Events: []entities.EventAttributes{{
			Loc: entities.LocationAttributes{
				Type: "et",
				Attributes: map[string]*string{
					"a1": &attr1,
					"a2": &attr2,
				},
			},
			Type:   "debit",
			Dt:     time.Unix(1608091866, 0).In(time.UTC),
			Amount: "123.45",
		}},
		States: []entities.StateAttributes{{
			Loc: entities.LocationAttributes{
				Type: "st",
				Attributes: map[string]*string{
					"a1": &attr2,
					"a2": &attr3,
				},
			},
			State: []byte(`{"some":"json"}`),
		}},
		Locks: []entities.LockAction{
			{
				Loc: entities.LocationAttributes{
					Type:       "lt1",
					Attributes: map[string]*string{"a1": &attr1},
				},
				UID:  "123",
				Mode: "validate",
			},
			{
				Loc: entities.LocationAttributes{
					Type:       "lt2",
					Attributes: map[string]*string{"a2": &attr2},
				},
				UID:  "321",
				Mode: "remove",
			},
		},
	}

	rawBatchRequest, err := json.Marshal(batchRequest)
	assert.NoError(t, err)
	fmt.Printf("Json raw request: %s\n", rawBatchRequest)

	// Адрес, по которому будем в систему счетов ходить
	wantURL := testVersionPrefix + "/batch/write"
	rawResponse := StringMap{"status": StatusOK, "data": StringMap{"batch_id": batchID}}

	m := makeClient(t, wantURL, rawResponse)

	got, err := m.WriteBatch(context.Background(), &batchRequest)
	assert.NoError(t, err)

	want := batchID
	assert.EqualValues(t, want, got)
	fmt.Printf("%s result: %v\n", t.Name(), got)
}

func TestClient_ReadBatch(t *testing.T) {
	attr1 := "v1"
	attr2 := "v2"
	attr3 := "v3"
	data := entities.BatchReadRequest{
		LockTimeout: 666,
		Locks: []entities.LockAction{
			{
				Loc: entities.LocationAttributes{
					Namespace:  "ns",
					Type:       "lt1",
					Attributes: map[string]*string{"a1": &attr1},
				},
				UID:  "123",
				Mode: "init",
			},
			{
				Loc: entities.LocationAttributes{
					Namespace:  "ns",
					Type:       "lt2",
					Attributes: map[string]*string{"a2": &attr2},
				},
				UID:  "321",
				Mode: "get",
			},
		},
		Balances: []entities.DtRequestAttributes{
			{
				Loc: entities.LocationAttributes{
					Namespace:  "ns",
					Type:       "bt1",
					Attributes: map[string]*string{"a1": &attr1},
				},
				Dt: time.Unix(1700000000, 0).In(time.UTC),
			},
			{
				Loc: entities.LocationAttributes{
					Namespace:  "ns",
					Type:       "bt2",
					Attributes: map[string]*string{"a1": &attr1, "a2": nil},
				},
				Dt: time.Unix(1700000001, 0).In(time.UTC),
			},
		},
		Turnovers: []entities.PeriodRequestAttributes{
			{
				Loc: entities.LocationAttributes{
					Namespace:  "ns",
					Type:       "tt1",
					Attributes: map[string]*string{"a2": &attr2, "a3": &attr3},
				},
				DtFrom: time.Unix(1700000002, 0).In(time.UTC),
				DtTo:   time.Unix(1700000003, 0).In(time.UTC),
			},
			{
				Loc: entities.LocationAttributes{
					Namespace:  "ns",
					Type:       "tt2",
					Attributes: map[string]*string{"a2": nil, "a3": &attr3},
				},
				DtFrom: time.Unix(1700000004, 0).In(time.UTC),
				DtTo:   time.Unix(1700000005, 0).In(time.UTC),
			},
		},
		DetailedTurnovers: []entities.PeriodRequestAttributes{
			{
				Loc: entities.LocationAttributes{
					Namespace:  "ns",
					Type:       "dt1",
					Attributes: map[string]*string{"a2": &attr2, "a3": &attr3},
				},
				DtFrom: time.Unix(1700000002, 0).In(time.UTC),
				DtTo:   time.Unix(1700000003, 0).In(time.UTC),
			},
			{
				Loc: entities.LocationAttributes{
					Namespace:  "ns",
					Type:       "dt2",
					Attributes: map[string]*string{"a2": nil, "a3": &attr3},
				},
				DtFrom: time.Unix(1700000004, 0).In(time.UTC),
				DtTo:   time.Unix(1700000005, 0).In(time.UTC),
			},
		},
		States: []entities.LocationAttributes{
			{
				Namespace:  "ns",
				Type:       "st",
				Attributes: map[string]*string{"a1": &attr2, "a2": &attr3},
			},
			{
				Namespace:  "ns",
				Type:       "st",
				Attributes: map[string]*string{"a3": &attr3},
			},
		},
	}

	// Адрес, по которому будем в систему счетов ходить
	wantURL := testVersionPrefix + "/batch/read"
	rawResponse := []byte(`{"status":"ok","data":{"locks":[{"loc":{"a1":"v2","a2":"v3","type":"l","namespace":"ns"},"dt":12345667,"uid":"666"}],"balances":[{"loc":{"a1":"v2","type":"b","namespace":"ns"},"dt":112345667,"debit":"123","credit":"456"}],"turnovers":[{"loc":{"a1":"v1","type":"t","namespace":"ns"},"dt_from":2112345667,"dt_to":1112345667,"debit_init":"1.0","credit_init":"2.00","debit_turnover":"3.000","credit_turnover":"4.0000"}],"detailed_turnovers":[{"loc":{"aaaaaaa":"v1","type":"dt","namespace":"ns"},"dt_from":2112345668,"dt_to":2112345669,"debit_init":"666","credit_init":"6666","debit_turnover":"66666","credit_turnover":"666666","events":[{"info":{"reason":42},"dt":42424242424,"amount":"42","event_id":"42","event_type":"42","type":"huedit"}]}],"states":[{"loc":{"state":"v1","type":"s","namespace":"ns"},"state":["state"]}]}}`)

	m := makeClient(t, wantURL, rawResponse)

	got, err := m.ReadBatch(context.Background(), &data)
	assert.NoError(t, err)

	want := entities.BatchReadResponse{
		Locks: []entities.LockAttributes{{
			Loc: entities.LocationAttributes{
				Namespace:  "ns",
				Type:       "l",
				Attributes: map[string]*string{"a1": &attr2, "a2": &attr3},
			},
			Dt:  entities.APIDt(time.Unix(12345667, 0).In(time.UTC)),
			UID: "666",
		}},
		Balances: []entities.BalanceAttributesDt{{
			Loc: entities.LocationAttributes{
				Namespace:  "ns",
				Type:       "b",
				Attributes: map[string]*string{"a1": &attr2},
			},
			Dt:     entities.APIDt(time.Unix(112345667, 0).In(time.UTC)),
			Debit:  "123",
			Credit: "456",
		}},
		Turnovers: []entities.TurnoverAttributesDt{{
			Loc: entities.LocationAttributes{
				Namespace:  "ns",
				Type:       "t",
				Attributes: map[string]*string{"a1": &attr1},
			},
			DtFrom:         entities.APIDt(time.Unix(2112345667, 0).In(time.UTC)),
			DtTo:           entities.APIDt(time.Unix(1112345667, 0).In(time.UTC)),
			DebitInit:      "1.0",
			CreditInit:     "2.00",
			DebitTurnover:  "3.000",
			CreditTurnover: "4.0000",
		}},
		DetailedTurnovers: []entities.DetailedTurnoverAttributesDt{{
			Loc: entities.LocationAttributes{
				Namespace:  "ns",
				Type:       "dt",
				Attributes: map[string]*string{"aaaaaaa": &attr1},
			},
			DtFrom:         entities.APIDt(time.Unix(2112345668, 0).In(time.UTC)),
			DtTo:           entities.APIDt(time.Unix(2112345669, 0).In(time.UTC)),
			DebitInit:      "666",
			CreditInit:     "6666",
			DebitTurnover:  "66666",
			CreditTurnover: "666666",
			Events: []entities.EventDetails{{
				Type:      "huedit",
				Dt:        time.Unix(42424242424, 0).In(time.UTC),
				Amount:    "42",
				EventType: "42",
				EventID:   "42",
				Info:      []byte(`{"reason":42}`),
			}},
		}},
		States: []entities.StateAttributes{{
			Loc: entities.LocationAttributes{
				Namespace:  "ns",
				Type:       "s",
				Attributes: map[string]*string{"state": &attr1},
			},
			State: []byte(`["state"]`),
		}},
	}
	assert.EqualValues(t, want, *got)
	fmt.Printf("%s result: %v\n", t.Name(), got)
}
