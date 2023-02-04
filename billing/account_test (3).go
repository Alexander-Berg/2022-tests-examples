package handlers

import (
	"bytes"
	"context"
	"net/http"
	"net/http/httptest"
	"strconv"
	"strings"
	"testing"
	"time"

	"github.com/golang/mock/gomock"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"

	"a.yandex-team.ru/billing/hot/accounts/mock/actionsmock"
	"a.yandex-team.ru/billing/hot/accounts/pkg/core/constants"
	"a.yandex-team.ru/billing/hot/accounts/pkg/core/entities"
	coreerrors "a.yandex-team.ru/billing/hot/accounts/pkg/core/errors"
)

func TestGetBalanceResponse(t *testing.T) {
	ctrl := gomock.NewController(t)
	defer ctrl.Finish()

	mockActions := actionsmock.NewMockActions(ctrl)
	ctx := context.Background()

	attr1 := "v1"
	attr2 := "v2"
	attr3 := "v3"
	mockActions.EXPECT().GetBalance(
		gomock.Eq(ctx),
		gomock.Eq(time.Unix(1608091866, 0).UTC()),
		gomock.Eq(entities.LocationAttributes{
			Namespace: "some_ns",
			Type:      "some_type",
			Attributes: map[string]*string{
				"a1": &attr1,
				"a2": &attr2,
			},
		})).Return(
		[]entities.BalanceAttributes{
			{
				Loc: entities.LocationAttributes{
					Namespace: "ns1",
					Type:      "type1",
					Attributes: map[string]*string{
						"a1": &attr1,
						"a2": &attr2,
					},
				},
				Debit:  "123.456789",
				Credit: "666",
			},
			{
				Loc: entities.LocationAttributes{
					Namespace: "ns2",
					Type:      "type2",
					Attributes: map[string]*string{
						"a1": &attr3,
						"a2": &attr1,
					},
				},
				Debit:  "0.456789",
				Credit: "14.141414",
			},
		},
		nil)

	// nolint: lll
	req := httptest.NewRequest("GET", "http://localhost:9000?namespace=some_ns&type=some_type&dt=1608091866&a1=v1&a2=v2", strings.NewReader(""))
	resp := httptest.NewRecorder()
	err := GetAccountBalance(ctx, resp, req, mockActions)
	require.NoError(t, err)

	assert.Equal(t, http.StatusOK, resp.Code)
	// nolint: lll
	assert.Equal(t, `{"status":"ok","data":[{"loc":{"a1":"v1","a2":"v2","type":"type1","namespace":"ns1"},"debit":"123.456789","credit":"666"},{"loc":{"a1":"v3","a2":"v1","type":"type2","namespace":"ns2"},"debit":"0.456789","credit":"14.141414"}]}`, resp.Body.String())
}

func TestGetBalanceAttrs(t *testing.T) {
	ctrl := gomock.NewController(t)
	defer ctrl.Finish()

	mockActions := actionsmock.NewMockActions(ctrl)
	ctx := context.Background()

	attr := "v1"
	emptyAttr := ""
	mockActions.EXPECT().GetBalance(
		gomock.Any(),
		gomock.Any(),
		gomock.Eq(entities.LocationAttributes{
			Namespace: "some_ns",
			Type:      "some_type",
			Attributes: map[string]*string{
				"a1": &attr,
				"a2": &emptyAttr,
				"a3": nil,
			},
		}))

	// nolint: lll
	req := httptest.NewRequest("GET", "http://localhost:9000?namespace=some_ns&type=some_type&dt=1608091866&a1=v1&a2=&a3__empty=1", strings.NewReader(""))
	resp := httptest.NewRecorder()
	err := GetAccountBalance(ctx, resp, req, mockActions)
	require.NoError(t, err)

	assert.Equal(t, http.StatusOK, resp.Code)
}

func TestGetBalanceNoDt(t *testing.T) {
	ctrl := gomock.NewController(t)
	defer ctrl.Finish()

	mockActions := actionsmock.NewMockActions(ctrl)
	ctx := context.Background()

	reqBody := strings.NewReader("")
	req := httptest.NewRequest("GET",
		"http://localhost:9000?namespace=abc&type=some_type&a1=v1&a2=v2", reqBody)
	resp := httptest.NewRecorder()
	err := GetAccountBalance(ctx, resp, req, mockActions)
	require.NoError(t, err)

	assert.Equal(t, http.StatusBadRequest, resp.Code)
	// nolint: lll
	assert.Equal(t, `{"status":"error","data":{"code":"INVALID_REQUEST","description":"no required argument with key 'dt'"}}`, resp.Body.String())
}

func TestGetBalanceInvalidDt(t *testing.T) {
	ctrl := gomock.NewController(t)
	defer ctrl.Finish()

	mockActions := actionsmock.NewMockActions(ctrl)
	ctx := context.Background()

	req := httptest.NewRequest("GET",
		"http://localhost:9000?namespace=abc&type=t&dt=2020-01-01", bytes.NewReader(nil))
	resp := httptest.NewRecorder()
	err := GetAccountBalance(ctx, resp, req, mockActions)
	require.NoError(t, err)

	assert.Equal(t, http.StatusBadRequest, resp.Code)
	// nolint: lll
	assert.Equal(t, `{"status":"error","data":{"code":"INVALID_REQUEST","description":"invalid timestamp format: strconv.ParseInt: parsing \"2020-01-01\": invalid syntax"}}`, resp.Body.String())
}

func TestGetBalanceActionError(t *testing.T) {
	ctrl := gomock.NewController(t)
	defer ctrl.Finish()

	mockActions := actionsmock.NewMockActions(ctrl)
	ctx := context.Background()

	mockActions.EXPECT().GetBalance(gomock.Any(), gomock.Any(), gomock.Any()).
		Return(nil, coreerrors.NewCodedError(423, "ALARMA_ALARMA", "alarma :("))

	reqBody := strings.NewReader("")
	req := httptest.NewRequest("GET",
		"http://localhost:9000?namespace=abc&type=some_type&dt=1608091866", reqBody)
	resp := httptest.NewRecorder()
	err := GetAccountBalance(ctx, resp, req, mockActions)
	require.NoError(t, err)

	assert.Equal(t, 423, resp.Code)
	assert.Equal(t, `{"status":"error","data":{"code":"ALARMA_ALARMA","description":"alarma :("}}`, resp.Body.String())
}

func TestGetTurnoverResponse(t *testing.T) {
	ctrl := gomock.NewController(t)
	defer ctrl.Finish()

	mockActions := actionsmock.NewMockActions(ctrl)
	ctx := context.Background()

	attr1 := "v1"
	attr2 := "v2"
	attr3 := "v3"
	mockActions.EXPECT().GetTurnover(
		gomock.Eq(ctx),
		gomock.Eq(time.Unix(1608091866, 0).UTC()),
		gomock.Eq(time.Unix(1609091866, 0).UTC()),
		gomock.Eq(entities.LocationAttributes{
			Namespace: "ns",
			Type:      "some_type",
			Attributes: map[string]*string{
				"a1": &attr1,
				"a2": &attr2,
			},
		})).Return(
		[]entities.TurnoverAttributes{
			{
				Loc: entities.LocationAttributes{
					Namespace: "ns1",
					Type:      "type1",
					Attributes: map[string]*string{
						"a1": &attr1,
						"a2": &attr2,
					},
				},
				DebitInit:      "1",
				CreditInit:     "2",
				DebitTurnover:  "3",
				CreditTurnover: "4",
			},
			{
				Loc: entities.LocationAttributes{
					Namespace: "ns2",
					Type:      "type2",
					Attributes: map[string]*string{
						"a1": &attr1,
						"a2": &attr3,
					},
				},
				DebitInit:      "5",
				CreditInit:     "6",
				DebitTurnover:  "7",
				CreditTurnover: "8",
			},
		},
		nil)

	// nolint: lll
	req := httptest.NewRequest("GET", "http://localhost:9000?namespace=ns&type=some_type&dt_from=1608091866&dt_to=1609091866&a1=v1&a2=v2", strings.NewReader(""))
	resp := httptest.NewRecorder()
	err := GetAccountTurnover(ctx, resp, req, mockActions)
	require.NoError(t, err)

	assert.Equal(t, http.StatusOK, resp.Code)
	// nolint: lll
	assert.Equal(t, `{"status":"ok","data":[{"loc":{"a1":"v1","a2":"v2","type":"type1","namespace":"ns1"},"debit_init":"1","credit_init":"2","debit_turnover":"3","credit_turnover":"4"},{"loc":{"a1":"v1","a2":"v3","type":"type2","namespace":"ns2"},"debit_init":"5","credit_init":"6","debit_turnover":"7","credit_turnover":"8"}]}`, resp.Body.String())
}

func TestGetTurnoverAttrs(t *testing.T) {
	ctrl := gomock.NewController(t)
	defer ctrl.Finish()

	mockActions := actionsmock.NewMockActions(ctrl)
	ctx := context.Background()

	attr := "v1"
	emptyAttr := ""
	mockActions.EXPECT().GetTurnover(
		gomock.Any(),
		gomock.Any(),
		gomock.Any(),
		gomock.Eq(entities.LocationAttributes{
			Namespace: "ns",
			Type:      "some_type",
			Attributes: map[string]*string{
				"a1": &attr,
				"a2": &emptyAttr,
				"a3": nil,
			},
		}))

	reqBody := strings.NewReader("")
	// nolint: lll
	req := httptest.NewRequest("GET", "http://localhost:9000?namespace=ns&type=some_type&dt_from=1608091866&dt_to=1608091866&a1=v1&a2=&a3__empty=1", reqBody)
	resp := httptest.NewRecorder()
	err := GetAccountTurnover(ctx, resp, req, mockActions)
	require.NoError(t, err)

	assert.Equal(t, http.StatusOK, resp.Code)
}

func TestGetTurnoverNoDtFrom(t *testing.T) {
	ctrl := gomock.NewController(t)
	defer ctrl.Finish()

	mockActions := actionsmock.NewMockActions(ctrl)
	ctx := context.Background()

	reqBody := strings.NewReader("")
	req := httptest.NewRequest("GET",
		"http://localhost:9000?namespace=abc&type=t&dt_to=1608091866", reqBody)
	resp := httptest.NewRecorder()
	err := GetAccountTurnover(ctx, resp, req, mockActions)
	require.NoError(t, err)

	assert.Equal(t, http.StatusBadRequest, resp.Code)
	// nolint: lll
	assert.Equal(t, `{"status":"error","data":{"code":"INVALID_REQUEST","description":"no required argument with key 'dt_from'"}}`, resp.Body.String())
}

func TestGetTurnoverInvalidDtFrom(t *testing.T) {
	ctrl := gomock.NewController(t)
	defer ctrl.Finish()

	mockActions := actionsmock.NewMockActions(ctrl)
	ctx := context.Background()

	reqBody := strings.NewReader("")
	req := httptest.NewRequest("GET",
		"http://localhost:9000?namespace=abc&type=t&dt_from=2020-01-01&dt_to=1", reqBody)
	resp := httptest.NewRecorder()
	err := GetAccountTurnover(ctx, resp, req, mockActions)
	require.NoError(t, err)

	assert.Equal(t, http.StatusBadRequest, resp.Code)
	// nolint: lll
	assert.Equal(t, `{"status":"error","data":{"code":"INVALID_REQUEST","description":"invalid timestamp format: strconv.ParseInt: parsing \"2020-01-01\": invalid syntax"}}`, resp.Body.String())
}

func TestGetTurnoverNoDtTo(t *testing.T) {
	ctrl := gomock.NewController(t)
	defer ctrl.Finish()

	mockActions := actionsmock.NewMockActions(ctrl)
	ctx := context.Background()

	reqBody := strings.NewReader("")
	req := httptest.NewRequest("GET",
		"http://localhost:9000?namespace=abc&type=t&dt_from=1608091866", reqBody)
	resp := httptest.NewRecorder()
	err := GetAccountTurnover(ctx, resp, req, mockActions)
	require.NoError(t, err)

	assert.Equal(t, http.StatusBadRequest, resp.Code)
	// nolint: lll
	assert.Equal(t, `{"status":"error","data":{"code":"INVALID_REQUEST","description":"no required argument with key 'dt_to'"}}`, resp.Body.String())
}

func TestGetTurnoverInvalidDtTo(t *testing.T) {
	ctrl := gomock.NewController(t)
	defer ctrl.Finish()

	mockActions := actionsmock.NewMockActions(ctrl)
	ctx := context.Background()

	reqBody := strings.NewReader("")
	req := httptest.NewRequest("GET",
		"http://localhost:9000?namespace=abc&type=t&dt_to=2021-01-01&dt_from=1", reqBody)
	resp := httptest.NewRecorder()
	err := GetAccountTurnover(ctx, resp, req, mockActions)
	require.NoError(t, err)

	assert.Equal(t, http.StatusBadRequest, resp.Code)
	// nolint: lll
	assert.Equal(t, `{"status":"error","data":{"code":"INVALID_REQUEST","description":"invalid timestamp format: strconv.ParseInt: parsing \"2021-01-01\": invalid syntax"}}`, resp.Body.String())
}

func TestGetTurnoverActionError(t *testing.T) {
	ctrl := gomock.NewController(t)
	defer ctrl.Finish()

	mockActions := actionsmock.NewMockActions(ctrl)
	ctx := context.Background()

	mockActions.EXPECT().GetTurnover(gomock.Any(), gomock.Any(), gomock.Any(), gomock.Any()).
		Return(nil, coreerrors.NewCodedError(666, "666", "666"))

	reqBody := strings.NewReader("")
	req := httptest.NewRequest("GET",
		"http://localhost:9000?namespace=abc&type=some_type&dt_from=1&dt_to=1", reqBody)
	resp := httptest.NewRecorder()
	err := GetAccountTurnover(ctx, resp, req, mockActions)
	require.NoError(t, err)

	assert.Equal(t, 666, resp.Code)
	assert.Equal(t, `{"status":"error","data":{"code":"666","description":"666"}}`, resp.Body.String())
}

func TestGetDetailedTurnoverResponse(t *testing.T) {
	ctrl := gomock.NewController(t)
	defer ctrl.Finish()

	mockActions := actionsmock.NewMockActions(ctrl)
	ctx := context.Background()

	attr1 := "v1"
	attr2 := "v2"
	attr3 := "v3"
	mockActions.EXPECT().GetDetailedTurnover(
		gomock.Eq(ctx),
		gomock.Eq(time.Unix(1608091866, 0).UTC()),
		gomock.Eq(time.Unix(1609091866, 0).UTC()),
		gomock.Eq(entities.LocationAttributes{
			Namespace: "ns",
			Type:      "some_type",
			Attributes: map[string]*string{
				"a1": &attr1,
				"a2": &attr2,
			},
		})).Return(
		[]entities.DetailedTurnoverAttributes{
			{
				Loc: entities.LocationAttributes{
					Namespace: "ns",
					Type:      "type1",
					Attributes: map[string]*string{
						"a1": &attr1,
						"a2": &attr2,
					},
				},
				DebitInit:      "1",
				CreditInit:     "2",
				DebitTurnover:  "3",
				CreditTurnover: "4",
				Events: []entities.EventDetails{
					{
						Type:      "t",
						Dt:        time.Unix(1234567890, 0),
						Amount:    "123",
						EventType: "44",
						EventID:   "74545",
						EventInfo: nil,
						Info:      []byte(`{"some_stuff": "stuff"}`),
					},
					{
						Type:      "tt",
						Dt:        time.Unix(1234543210, 0),
						Amount:    "321",
						EventType: "23",
						EventID:   "11",
						EventInfo: []byte(`{"event_stuff": "yep"}`),
						Info:      []byte(`{"stuff": "not_stuff"}`),
					},
				},
			},
			{
				Loc: entities.LocationAttributes{
					Namespace: "ns",
					Type:      "type2",
					Attributes: map[string]*string{
						"a1": &attr1,
						"a2": &attr3,
					},
				},
				DebitInit:      "5",
				CreditInit:     "6",
				DebitTurnover:  "7",
				CreditTurnover: "8",
				Events: []entities.EventDetails{
					{
						Type:      "1",
						Dt:        time.Unix(1234567890, 0),
						Amount:    "2",
						EventType: "3",
						EventID:   "4",
						EventInfo: []byte(`6`),
						Info:      []byte(`5`),
					},
				},
			},
		},
		nil)

	reqBody := strings.NewReader("")
	// nolint: lll
	req := httptest.NewRequest("GET", "http://localhost:9000?namespace=ns&type=some_type&dt_from=1608091866&dt_to=1609091866&a1=v1&a2=v2", reqBody)
	resp := httptest.NewRecorder()
	err := GetAccountDetailedTurnover(ctx, resp, req, mockActions)
	require.NoError(t, err)

	assert.Equal(t, http.StatusOK, resp.Code)
	// nolint: lll
	assert.Equal(t, `{"status":"ok","data":[{"loc":{"a1":"v1","a2":"v2","type":"type1","namespace":"ns"},"debit_init":"1","credit_init":"2","debit_turnover":"3","credit_turnover":"4","events":[{"amount":"123","dt":1234567890,"event_id":"74545","event_info":null,"event_type":"44","info":{"some_stuff":"stuff"},"type":"t"},{"amount":"321","dt":1234543210,"event_id":"11","event_info":{"event_stuff":"yep"},"event_type":"23","info":{"stuff":"not_stuff"},"type":"tt"}]},{"loc":{"a1":"v1","a2":"v3","type":"type2","namespace":"ns"},"debit_init":"5","credit_init":"6","debit_turnover":"7","credit_turnover":"8","events":[{"amount":"2","dt":1234567890,"event_id":"4","event_info":6,"event_type":"3","info":5,"type":"1"}]}]}`, resp.Body.String())
}

func TestGetDetailedTurnoverAttrs(t *testing.T) {
	ctrl := gomock.NewController(t)
	defer ctrl.Finish()

	mockActions := actionsmock.NewMockActions(ctrl)
	ctx := context.Background()

	attr := "v1"
	emptyAttr := ""
	mockActions.EXPECT().GetDetailedTurnover(
		gomock.Any(),
		gomock.Any(),
		gomock.Any(),
		gomock.Eq(entities.LocationAttributes{
			Namespace: "ns",
			Type:      "some_type",
			Attributes: map[string]*string{
				"a1": &attr,
				"a2": &emptyAttr,
				"a3": nil,
			},
		}))

	reqBody := strings.NewReader("")
	// nolint: lll
	req := httptest.NewRequest("GET", "http://localhost:9000?namespace=ns&type=some_type&dt_from=1608091866&dt_to=1608091866&a1=v1&a2=&a3__empty=1", reqBody)
	resp := httptest.NewRecorder()
	err := GetAccountDetailedTurnover(ctx, resp, req, mockActions)
	require.NoError(t, err)

	assert.Equal(t, http.StatusOK, resp.Code)
}

func TestGetDetailedTurnoverNoDtFrom(t *testing.T) {
	ctrl := gomock.NewController(t)
	defer ctrl.Finish()

	mockActions := actionsmock.NewMockActions(ctrl)
	ctx := context.Background()

	reqBody := strings.NewReader("")
	req := httptest.NewRequest("GET",
		"http://localhost:9000?namespace=abc&type=t&dt_to=1608091866", reqBody)
	resp := httptest.NewRecorder()
	err := GetAccountDetailedTurnover(ctx, resp, req, mockActions)
	require.NoError(t, err)

	assert.Equal(t, http.StatusBadRequest, resp.Code)
	// nolint: lll
	assert.Equal(t, `{"status":"error","data":{"code":"INVALID_REQUEST","description":"no required argument with key 'dt_from'"}}`, resp.Body.String())
}

func TestGetDetailedTurnoverInvalidDtFrom(t *testing.T) {
	ctrl := gomock.NewController(t)
	defer ctrl.Finish()

	mockActions := actionsmock.NewMockActions(ctrl)
	ctx := context.Background()

	reqBody := strings.NewReader("")
	req := httptest.NewRequest("GET",
		"http://localhost:9000?namespace=abc&type=t&dt_from=2020-01-01&dt_to=1", reqBody)
	resp := httptest.NewRecorder()
	err := GetAccountDetailedTurnover(ctx, resp, req, mockActions)
	require.NoError(t, err)

	assert.Equal(t, http.StatusBadRequest, resp.Code)
	// nolint: lll
	assert.Equal(t, `{"status":"error","data":{"code":"INVALID_REQUEST","description":"invalid timestamp format: strconv.ParseInt: parsing \"2020-01-01\": invalid syntax"}}`, resp.Body.String())
}

func TestGetDetailedTurnoverNoDtTo(t *testing.T) {
	ctrl := gomock.NewController(t)
	defer ctrl.Finish()

	mockActions := actionsmock.NewMockActions(ctrl)
	ctx := context.Background()

	reqBody := strings.NewReader("")
	req := httptest.NewRequest("GET",
		"http://localhost:9000?namespace=abc&type=t&dt_from=1608091866", reqBody)
	resp := httptest.NewRecorder()
	err := GetAccountDetailedTurnover(ctx, resp, req, mockActions)
	require.NoError(t, err)

	assert.Equal(t, http.StatusBadRequest, resp.Code)
	// nolint: lll
	assert.Equal(t, `{"status":"error","data":{"code":"INVALID_REQUEST","description":"no required argument with key 'dt_to'"}}`, resp.Body.String())
}

func TestGetDetailedTurnoverInvalidDtTo(t *testing.T) {
	ctrl := gomock.NewController(t)
	defer ctrl.Finish()

	mockActions := actionsmock.NewMockActions(ctrl)
	ctx := context.Background()

	reqBody := strings.NewReader("")
	req := httptest.NewRequest("GET",
		"http://localhost:9000?namespace=abc&type=t&dt_to=2021-01-01&dt_from=1", reqBody)
	resp := httptest.NewRecorder()
	err := GetAccountDetailedTurnover(ctx, resp, req, mockActions)
	require.NoError(t, err)

	assert.Equal(t, http.StatusBadRequest, resp.Code)
	// nolint: lll
	assert.Equal(t, `{"status":"error","data":{"code":"INVALID_REQUEST","description":"invalid timestamp format: strconv.ParseInt: parsing \"2021-01-01\": invalid syntax"}}`, resp.Body.String())
}

func TestGetDetailedTurnoverActionError(t *testing.T) {
	ctrl := gomock.NewController(t)
	defer ctrl.Finish()

	mockActions := actionsmock.NewMockActions(ctrl)
	ctx := context.Background()

	mockActions.EXPECT().GetDetailedTurnover(gomock.Any(), gomock.Any(), gomock.Any(), gomock.Any()).
		Return(nil, coreerrors.NewCodedError(418, "OPS", "oooops"))

	reqBody := strings.NewReader("")
	req := httptest.NewRequest("GET",
		"http://localhost:9000?namespace=abc&type=some_type&dt_from=1&dt_to=1", reqBody)
	resp := httptest.NewRecorder()
	err := GetAccountDetailedTurnover(ctx, resp, req, mockActions)
	require.NoError(t, err)

	assert.Equal(t, 418, resp.Code)
	assert.Equal(t, `{"status":"error","data":{"code":"OPS","description":"oooops"}}`, resp.Body.String())
}

func TestMaxDtTimeMatchesMaxDt(t *testing.T) {
	resolvedTime, err := parseDtArg(strconv.FormatInt(constants.MaxDt, 10))
	require.NoError(t, err)
	assert.Equal(t, constants.MaxDtTime, resolvedTime)
}
