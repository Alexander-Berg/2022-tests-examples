package handlers

import (
	"context"
	"net/http"
	"net/http/httptest"
	"strings"
	"testing"
	"time"

	"github.com/golang/mock/gomock"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"

	"a.yandex-team.ru/billing/hot/accounts/mock/actionsmock"
	"a.yandex-team.ru/billing/hot/accounts/pkg/core/entities"
	coreerrors "a.yandex-team.ru/billing/hot/accounts/pkg/core/errors"
	metricsmock "a.yandex-team.ru/library/go/core/metrics/mock"
)

func TestWriteBatchOkResponse(t *testing.T) {
	ctrl := gomock.NewController(t)
	defer ctrl.Finish()

	mockActions := actionsmock.NewMockActions(ctrl)
	ctx := context.Background()
	reg := metricsmock.NewRegistry(metricsmock.NewRegistryOpts())

	attr1 := "v1"
	attr2 := "v2"
	attr3 := "v3"
	mockActions.EXPECT().WriteBatch(
		gomock.Eq(ctx),
		gomock.Eq(entities.BatchWriteRequest{
			EventType:  "batch",
			ExternalID: "1234",
			Info:       []byte(`["some","info"]`),
			Dt:         time.Unix(123, 0).UTC(),
			Events: []entities.EventAttributes{{
				Loc: entities.LocationAttributes{
					Namespace: "sn",
					Type:      "et",
					Attributes: map[string]*string{
						"a1": &attr1,
						"a2": &attr2,
					},
				},
				Type:   "debit",
				Dt:     time.Unix(1608091866, 0).UTC(),
				Amount: "123.45",
			}},
			States: []entities.StateAttributes{{
				Loc: entities.LocationAttributes{
					Namespace: "sn",
					Type:      "st",
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
						Namespace:  "sn",
						Type:       "lt1",
						Attributes: map[string]*string{"a1": &attr1},
					},
					UID:  "123",
					Mode: "validate",
				},
				{
					Loc: entities.LocationAttributes{
						Namespace:  "ns",
						Type:       "lt2",
						Attributes: map[string]*string{"a2": &attr2},
					},
					UID:  "321",
					Mode: "remove",
				},
			},
		}),
	).Return(&entities.BatchWriteResponse{BatchID: 666, IsExisting: false}, nil)

	// nolint: lll
	body := strings.NewReader(`{"event_type":"batch","external_id":"1234","dt":123,"info":["some","info"],"events":[{"loc":{"namespace":"sn","type":"et","a1":"v1","a2":"v2"},"type":"debit","dt":1608091866,"amount":"123.45"}],"states":[{"loc":{"namespace":"sn","type":"st","a1":"v2","a2":"v3"},"state":{"some":"json"}}],"locks":[{"loc":{"namespace":"sn","type":"lt1","a1":"v1"},"uid":"123","mode":"validate"},{"loc":{"namespace":"ns","type":"lt2","a2":"v2"},"uid":"321","mode":"remove"}]}`)
	req := httptest.NewRequest("POST", "http://localhost:9000", body)
	resp := httptest.NewRecorder()
	err := WriteBatch(ctx, resp, req, mockActions, reg)
	require.NoError(t, err)

	assert.Equal(t, http.StatusCreated, resp.Code)
	assert.Equal(t, `{"status":"ok","data":{"batch_id":666}}`, resp.Body.String())
}

func TestWriteBatchNullableAttrs(t *testing.T) {
	ctrl := gomock.NewController(t)
	defer ctrl.Finish()

	mockActions := actionsmock.NewMockActions(ctrl)
	ctx := context.Background()
	reg := metricsmock.NewRegistry(metricsmock.NewRegistryOpts())

	attr := "v1"
	mockActions.EXPECT().WriteBatch(
		gomock.Eq(ctx),
		gomock.Eq(entities.BatchWriteRequest{
			EventType:  "b",
			ExternalID: "1",
			Info:       []byte(`["s"]`),
			Dt:         time.Unix(12345, 0).UTC(),
			Events: []entities.EventAttributes{{
				Loc: entities.LocationAttributes{
					Namespace: "ns",
					Type:      "e",
					Attributes: map[string]*string{
						"a1": &attr,
						"a2": nil,
					},
				},
				Type:   "debit",
				Dt:     time.Unix(1, 0).UTC(),
				Amount: "1",
			}},
			States: []entities.StateAttributes{{
				Loc: entities.LocationAttributes{
					Namespace: "ns",
					Type:      "s",
					Attributes: map[string]*string{
						"a1": nil,
						"a2": &attr,
					},
				},
				State: []byte(`null`),
			}},
			Locks: []entities.LockAction{{
				Loc: entities.LocationAttributes{
					Namespace:  "ns",
					Type:       "l",
					Attributes: map[string]*string{"a1": nil},
				},
				UID:  "1",
				Mode: "validate",
			}},
		}),
	).Return(&entities.BatchWriteResponse{BatchID: 666, IsExisting: false}, nil)

	// nolint: lll
	body := strings.NewReader(`{"event_type":"b","external_id":"1","dt":12345,"info":["s"],"events":[{"loc":{"namespace":"ns","type":"e","a1":"v1","a2":null},"type":"debit","dt":1,"amount":"1"}],"states":[{"loc":{"namespace":"ns","type":"s","a1":null,"a2":"v1"},"state":null}],"locks":[{"loc":{"namespace":"ns","type":"l","a1":null},"uid":"1","mode":"validate"}]}`)
	req := httptest.NewRequest("POST", "http://localhost:9000", body)
	resp := httptest.NewRecorder()
	err := WriteBatch(ctx, resp, req, mockActions, reg)
	require.NoError(t, err)

	assert.Equal(t, http.StatusCreated, resp.Code)
	assert.Equal(t, `{"status":"ok","data":{"batch_id":666}}`, resp.Body.String())
}

func TestWriteBatchNullParts(t *testing.T) {
	ctrl := gomock.NewController(t)
	defer ctrl.Finish()

	mockActions := actionsmock.NewMockActions(ctrl)
	ctx := context.Background()
	reg := metricsmock.NewRegistry(metricsmock.NewRegistryOpts())

	mockActions.EXPECT().WriteBatch(
		gomock.Eq(ctx),
		gomock.Eq(entities.BatchWriteRequest{
			EventType:  "b",
			ExternalID: "1",
			Dt:         time.Unix(1, 0).UTC(),
			Info:       nil,
			Events:     nil,
			States:     nil,
			Locks:      nil,
		})).Return(&entities.BatchWriteResponse{BatchID: 666, IsExisting: false}, nil)

	body := strings.NewReader(`{"event_type":"b","external_id":"1","dt":1}`)
	req := httptest.NewRequest("POST", "http://localhost:9000", body)
	resp := httptest.NewRecorder()
	err := WriteBatch(ctx, resp, req, mockActions, reg)
	require.NoError(t, err)

	assert.Equal(t, http.StatusCreated, resp.Code)
	assert.Equal(t, `{"status":"ok","data":{"batch_id":666}}`, resp.Body.String())
}

func TestWriteBatchParseFail(t *testing.T) {
	ctrl := gomock.NewController(t)
	defer ctrl.Finish()

	mockActions := actionsmock.NewMockActions(ctrl)
	ctx := context.Background()
	reg := metricsmock.NewRegistry(metricsmock.NewRegistryOpts())

	// nolinter: lll
	body := strings.NewReader(`{"event_type":"b","external_id":"1","events":[{"loc":{"namespace":"ns","type":"t","a1":"v1"},"type":"debit","dt":1}]}`)
	req := httptest.NewRequest("POST", "http://localhost:9000", body)
	resp := httptest.NewRecorder()
	err := WriteBatch(ctx, resp, req, mockActions, reg)
	require.NoError(t, err)

	assert.Equal(t, http.StatusBadRequest, resp.Code)
	// nolinter: lll
	assert.Equal(t, `{"status":"error","data":{"code":"INVALID_REQUEST","description":"failed to parse request: no required 'amount' attribute"}}`, resp.Body.String())
}

func TestWriteBatchActionError(t *testing.T) {
	ctrl := gomock.NewController(t)
	defer ctrl.Finish()

	mockActions := actionsmock.NewMockActions(ctrl)
	ctx := context.Background()
	reg := metricsmock.NewRegistry(metricsmock.NewRegistryOpts())

	mockActions.EXPECT().WriteBatch(gomock.Any(), gomock.Any()).Return(nil, coreerrors.NewCodedError(418, "418", "418"))

	body := strings.NewReader(`{"event_type":"b","external_id":"1","dt":1}`)
	req := httptest.NewRequest("POST", "http://localhost:9000", body)
	resp := httptest.NewRecorder()
	err := WriteBatch(ctx, resp, req, mockActions, reg)
	require.NoError(t, err)

	assert.Equal(t, 418, resp.Code)
	assert.Equal(t, `{"status":"error","data":{"code":"418","description":"418"}}`, resp.Body.String())
}

func TestWriteBatchExisting(t *testing.T) {
	ctrl := gomock.NewController(t)
	defer ctrl.Finish()

	mockActions := actionsmock.NewMockActions(ctrl)
	ctx := context.Background()
	reg := metricsmock.NewRegistry(metricsmock.NewRegistryOpts())

	mockActions.EXPECT().WriteBatch(gomock.Any(), gomock.Any()).Return(&entities.BatchWriteResponse{
		BatchID:    667,
		IsExisting: true,
	}, nil)

	body := strings.NewReader(`{"event_type":"b","external_id":"1","dt":1}`)
	req := httptest.NewRequest("POST", "http://localhost:9000", body)
	resp := httptest.NewRecorder()
	err := WriteBatch(ctx, resp, req, mockActions, reg)
	require.NoError(t, err)

	assert.Equal(t, http.StatusOK, resp.Code)
	assert.Equal(t, `{"status":"ok","data":{"batch_id":667}}`, resp.Body.String())
}

func TestReadBatchOkResponse(t *testing.T) {
	ctrl := gomock.NewController(t)
	defer ctrl.Finish()

	mockActions := actionsmock.NewMockActions(ctrl)
	ctx := context.Background()
	reg := metricsmock.NewRegistry(metricsmock.NewRegistryOpts())

	attr1 := "v1"
	attr2 := "v2"
	attr3 := "v3"
	mockActions.EXPECT().ReadBatch(
		gomock.Eq(ctx),
		entities.BatchReadRequest{
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
					Dt: time.Unix(1700000000, 0).UTC(),
				},
				{
					Loc: entities.LocationAttributes{
						Namespace:  "ns",
						Type:       "bt2",
						Attributes: map[string]*string{"a1": &attr1, "a2": nil},
					},
					Dt: time.Unix(1700000001, 0).UTC(),
				},
			},
			Turnovers: []entities.PeriodRequestAttributes{
				{
					Loc: entities.LocationAttributes{
						Namespace:  "ns",
						Type:       "tt1",
						Attributes: map[string]*string{"a2": &attr2, "a3": &attr3},
					},
					DtFrom: time.Unix(1700000002, 0).UTC(),
					DtTo:   time.Unix(1700000003, 0).UTC(),
				},
				{
					Loc: entities.LocationAttributes{
						Namespace:  "ns",
						Type:       "tt2",
						Attributes: map[string]*string{"a2": nil, "a3": &attr3},
					},
					DtFrom: time.Unix(1700000004, 0).UTC(),
					DtTo:   time.Unix(1700000005, 0).UTC(),
				},
			},
			DetailedTurnovers: []entities.PeriodRequestAttributes{
				{
					Loc: entities.LocationAttributes{
						Namespace:  "ns",
						Type:       "dt1",
						Attributes: map[string]*string{"a2": &attr2, "a3": &attr3},
					},
					DtFrom: time.Unix(1700000002, 0).UTC(),
					DtTo:   time.Unix(1700000003, 0).UTC(),
				},
				{
					Loc: entities.LocationAttributes{
						Namespace:  "ns",
						Type:       "dt2",
						Attributes: map[string]*string{"a2": nil, "a3": &attr3},
					},
					DtFrom: time.Unix(1700000004, 0).UTC(),
					DtTo:   time.Unix(1700000005, 0).UTC(),
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
		},
	).Return(&entities.BatchReadResponse{
		Locks: []entities.LockAttributes{{
			Loc: entities.LocationAttributes{
				Namespace:  "ns",
				Type:       "l",
				Attributes: map[string]*string{"a1": &attr2, "a2": &attr3},
			},
			Dt:  entities.APIDt(time.Unix(12345667, 0)),
			UID: "666",
		}},
		Balances: []entities.BalanceAttributesDt{{
			Loc: entities.LocationAttributes{
				Namespace:  "ns",
				Type:       "b",
				Attributes: map[string]*string{"a1": &attr2},
			},
			Dt:     entities.APIDt(time.Unix(112345667, 0)),
			Debit:  "123",
			Credit: "456",
		}},
		Turnovers: []entities.TurnoverAttributesDt{{
			Loc: entities.LocationAttributes{
				Namespace:  "ns",
				Type:       "t",
				Attributes: map[string]*string{"a1": &attr1},
			},
			DtFrom:         entities.APIDt(time.Unix(2112345667, 0)),
			DtTo:           entities.APIDt(time.Unix(1112345667, 0)),
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
			DtFrom:         entities.APIDt(time.Unix(2112345668, 0)),
			DtTo:           entities.APIDt(time.Unix(2112345669, 0)),
			DebitInit:      "666",
			CreditInit:     "6666",
			DebitTurnover:  "66666",
			CreditTurnover: "666666",
			Events: []entities.EventDetails{{
				Type:      "huedit",
				Dt:        time.Unix(42424242424, 0),
				Amount:    "42",
				EventType: "42",
				EventID:   "42",
				EventInfo: []byte(`{"found":42}`),
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
	}, nil)

	// nolint: lll
	body := strings.NewReader(`{"lock_timeout":666,"locks":[{"loc":{"namespace":"ns","type":"lt1","a1":"v1"},"uid":"123","mode":"init"},{"loc":{"namespace":"ns","type":"lt2","a2":"v2"},"uid":"321","mode":"get"}],"balances":[{"loc":{"namespace":"ns","type":"bt1","a1":"v1"},"dt":1700000000},{"loc":{"namespace":"ns","type":"bt2","a1":"v1","a2":null},"dt":1700000001}],"turnovers":[{"loc":{"namespace":"ns","type":"tt1","a2":"v2","a3":"v3"},"dt_from":1700000002,"dt_to":1700000003},{"loc":{"namespace":"ns","type":"tt2","a2":null,"a3":"v3"},"dt_from":1700000004,"dt_to":1700000005}],"detailed_turnovers":[{"loc":{"namespace":"ns","type":"dt1","a2":"v2","a3":"v3"},"dt_from":1700000002,"dt_to":1700000003},{"loc":{"namespace":"ns","type":"dt2","a2":null,"a3":"v3"},"dt_from":1700000004,"dt_to":1700000005}],"states":[{"namespace":"ns","type":"st","a1":"v2","a2":"v3"},{"namespace":"ns","type":"st","a3":"v3"}]}`)
	req := httptest.NewRequest("POST", "http://localhost:9000", body)
	resp := httptest.NewRecorder()
	err := ReadBatch(ctx, resp, req, mockActions, reg)
	require.NoError(t, err)

	assert.Equal(t, http.StatusOK, resp.Code)
	// nolint: lll
	assert.Equal(t, `{"status":"ok","data":{"locks":[{"loc":{"a1":"v2","a2":"v3","type":"l","namespace":"ns"},"dt":12345667,"uid":"666"}],"balances":[{"loc":{"a1":"v2","type":"b","namespace":"ns"},"dt":112345667,"debit":"123","credit":"456"}],"turnovers":[{"loc":{"a1":"v1","type":"t","namespace":"ns"},"dt_from":2112345667,"dt_to":1112345667,"debit_init":"1.0","credit_init":"2.00","debit_turnover":"3.000","credit_turnover":"4.0000"}],"detailed_turnovers":[{"loc":{"aaaaaaa":"v1","type":"dt","namespace":"ns"},"dt_from":2112345668,"dt_to":2112345669,"debit_init":"666","credit_init":"6666","debit_turnover":"66666","credit_turnover":"666666","events":[{"amount":"42","dt":42424242424,"event_id":"42","event_info":{"found":42},"event_type":"42","info":{"reason":42},"type":"huedit"}]}],"states":[{"loc":{"state":"v1","type":"s","namespace":"ns"},"state":["state"]}]}}`, resp.Body.String())
}

func TestReadBatchParseFail(t *testing.T) {
	ctrl := gomock.NewController(t)
	defer ctrl.Finish()

	mockActions := actionsmock.NewMockActions(ctrl)
	ctx := context.Background()
	reg := metricsmock.NewRegistry(metricsmock.NewRegistryOpts())

	// nolinter: lll
	body := strings.NewReader(`{"lock_timeout":666,"locks":[{"loc":{"namespace":"ns","type":"lt1","a1":"v1"},"uid":"123","mode":"init"},{"loc":{"namespace":"ns","type":"lt2","a2":"v2"},"uid":"321","mode":"get"}],"balances":[{"loc":{"namespace":"ns","type":"bt1","a1":"v1"},"dt":1700000000},{"loc":{"namespace":"ns","type":"bt2","a1":"v1","a2":null},"dt":1700000001}],"turnovers":[{"loc":{"namespace":"ns","type":"tt1","a2":"v2","a3":"v3"},"dt_from":1700000002,"dt_to":1700000003},{"gluck":{"namespace":"ns","type":"tt2","a2":null,"a3":"v3"},"dt_from":1700000004,"dt_to":1700000005}],"detailed_turnovers":[{"loc":{"namespace":"ns","type":"dt1","a2":"v2","a3":"v3"},"dt_from":1700000002,"dt_to":1700000003},{"loc":{"namespace":"ns","type":"dt2","a2":null,"a3":"v3"},"dt_from":1700000004,"dt_to":1700000005}],"states":[{"namespace":"ns","type":"st","a1":"v2","a2":"v3"},{"namespace":"ns","type":"st","a3":"v3"}]}`)
	req := httptest.NewRequest("POST", "http://localhost:9000", body)
	resp := httptest.NewRecorder()
	err := ReadBatch(ctx, resp, req, mockActions, reg)
	require.NoError(t, err)

	assert.Equal(t, http.StatusBadRequest, resp.Code)
	// nolinter: lll
	assert.Equal(t, `{"status":"error","data":{"code":"INVALID_REQUEST","description":"failed to parse request: no required 'loc' attribute"}}`, resp.Body.String())
}

func TestReadBatchActionError(t *testing.T) {
	ctrl := gomock.NewController(t)
	defer ctrl.Finish()

	mockActions := actionsmock.NewMockActions(ctrl)
	ctx := context.Background()

	mockActions.EXPECT().ReadBatch(gomock.Any(), gomock.Any()).
		Return(nil, coreerrors.NewCodedError(666, "HAIL", "EMPEROR"))

	body := strings.NewReader(`{}`)
	req := httptest.NewRequest("POST", "http://localhost:9000", body)
	resp := httptest.NewRecorder()
	err := ReadBatch(ctx, resp, req, mockActions, nil)
	require.NoError(t, err)

	assert.Equal(t, 666, resp.Code)
	assert.Equal(t, `{"status":"error","data":{"code":"HAIL","description":"EMPEROR"}}`, resp.Body.String())
}
