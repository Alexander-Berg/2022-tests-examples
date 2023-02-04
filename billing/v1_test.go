package handlers

import (
	"encoding/json"
	"fmt"
	"net/http"
	"net/http/httptest"
	"strconv"
	"strings"
	"testing"
	"time"

	"github.com/shopspring/decimal"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"

	"a.yandex-team.ru/billing/hot/payout/internal/context"
	"a.yandex-team.ru/billing/hot/payout/internal/core"
	"a.yandex-team.ru/billing/hot/payout/internal/cpf"
	"a.yandex-team.ru/billing/hot/payout/internal/payout"
	"a.yandex-team.ru/billing/hot/payout/internal/request"
	bt "a.yandex-team.ru/billing/library/go/billingo/pkg/testing"
	schema "a.yandex-team.ru/billing/library/go/billingo/pkg/web/schema/json"
	"a.yandex-team.ru/billing/library/go/billingo/pkg/xlog"
	"a.yandex-team.ru/library/go/core/log"
)

const (
	PayoutURL                = "/api/v1/payout"
	RequestURL               = "/api/v1/payout-by-client"
	BatchDataURL             = "/api/v1/batch"
	CreateCashPaymentFactURL = "/api/v1/stub/create_cash_payment_fact"
)

// createPayout создает выплату для тестирования
func createPayout(t *testing.T, body *strings.Reader, ctx context.PayoutContext) (schema.APIResponse, int) {
	req := httptest.NewRequest(http.MethodPost, PayoutURL, body)
	w := httptest.NewRecorder()

	if err := PostPayout(ctx, w, req); err != nil {
		assert.Fail(t, err.Error())
	}

	var resp schema.APIResponse
	if err := json.Unmarshal(w.Body.Bytes(), &resp); err != nil {
		assert.Fail(t, err.Error())
	}
	return resp, w.Code
}

// createPayoutByClient создает заявку на выплату для тестирования через API
func createPayoutByClient(t *testing.T, body *strings.Reader, ctx context.PayoutContext) (schema.APIResponse, int) {
	req := httptest.NewRequest(http.MethodPost, RequestURL, body)
	w := httptest.NewRecorder()

	if err := PostPayoutByClient(ctx, w, req); err != nil {
		assert.Fail(t, err.Error())
	}

	var resp schema.APIResponse
	if err := json.Unmarshal(w.Body.Bytes(), &resp); err != nil {
		assert.Fail(t, err.Error())
	}
	return resp, w.Code
}

// getCheckIDData получает информацию о oebs batch-ах для тестирования через API
func getCheckIDData(t *testing.T, body *strings.Reader, ctx context.PayoutContext) (schema.APIResponse, int) {
	req := httptest.NewRequest(http.MethodGet, BatchDataURL, body)
	w := httptest.NewRecorder()

	if err := GetBatchData(ctx, w, req); err != nil {
		require.Fail(t, err.Error())
	}

	var resp schema.APIResponse
	if err := json.Unmarshal(w.Body.Bytes(), &resp); err != nil {
		require.Fail(t, err.Error())
	}
	return resp, w.Code
}

// getPayoutInfo получает информацию о выплатах для тестирования через API
func getPayoutInfo(t *testing.T, clientID, from, to string, statuses []string, ctx context.PayoutContext) (schema.APIResponse, int) {
	// prepare request and params
	req := httptest.NewRequest(http.MethodGet, BatchDataURL, nil)
	values := req.URL.Query()
	values.Add(ClientIDParam, clientID)
	values.Add(FromParam, from)
	values.Add(ToParam, to)
	for _, status := range statuses {
		values.Add(StatusesParam, status)
	}
	req.URL.RawQuery = values.Encode()
	w := httptest.NewRecorder()

	// make request
	if err := GetPayoutInfo(ctx, w, req); err != nil {
		require.Fail(t, err.Error())
	}

	var resp schema.APIResponse
	require.NoError(t, json.Unmarshal(w.Body.Bytes(), &resp))
	return resp, w.Code
}

func (s *HandlersV1TestSuite) TestPostPayoutOK() {
	externalID := bt.RandS(50)
	body := strings.NewReader(fmt.Sprintf(`{
		"external_id": "%s",
		"service_id": 124,
		"contract_id": 12345,
		"amount": 1245.66,
		"currency": "RUB",
		"payload": {
			"account_type": "1235656",
			"client_id": "113344"
		}
	}`, externalID))

	resp, code := createPayout(s.T(), body, s.ctx)
	assert.Equal(s.T(), http.StatusCreated, code)
	assert.Equal(s.T(), http.StatusCreated, resp.Code, resp.Data)

	// Проверяем, что в БД такая же информация
	m := resp.Data.(map[string]any)

	id := int64(m["id"].(float64))
	p, err := payout.Get(&s.ctx, id)
	assert.NoError(s.T(), err)

	assert.Equal(s.T(), p.ID, id)
	assert.Equal(s.T(), p.ExternalID, externalID)
	assert.Equal(s.T(), p.ServiceID, int64(124))
	assert.Equal(s.T(), p.ContractID, int64(12345))
	assert.Equal(s.T(), p.Amount, decimal.RequireFromString("1245.66"))
	assert.Equal(s.T(), p.Currency, "RUB")
	assert.Equal(s.T(), p.Status, payout.StatusNew)
	//assert.NotNil(s.T(), p.Payload)
}

func (s *HandlersV1TestSuite) TestPostPayoutFail() {
	externalID := bt.RandS(50)
	body := strings.NewReader(fmt.Sprintf(`{
		"external_id": "%s",
		"service_id": "string-but-must-be-int",
		"contract_id": 12345,
		"amount": 1245.66,
		"currency": "RUB"
	}`, externalID))

	resp, code := createPayout(s.T(), body, s.ctx)

	assert.Equal(s.T(), http.StatusBadRequest, code)
	assert.Equal(s.T(), http.StatusBadRequest, resp.Code, resp.Data)
	assert.Equal(s.T(),
		"json: cannot unmarshal string into Go struct field NewPayout.service_id of type int64",
		fmt.Sprintf("%v", resp.Data))
}

func (s *HandlersV1TestSuite) TestPostPayoutRetry() {
	externalID := bt.RandS(50)
	bodyRaw := fmt.Sprintf(`{
		"external_id": "%s",
		"service_id": 124,
		"contract_id": 12345,
		"amount": 1245.66,
		"currency": "RUB",
		"payload": {
			"account_type": "1235656",
			"client_id": "113344"
		}
	}`, externalID)

	resp, code := createPayout(s.T(), strings.NewReader(bodyRaw), s.ctx)
	assert.Equal(s.T(), http.StatusCreated, code)

	// Проверяем, что в БД такая же информация
	m := resp.Data.(map[string]any)
	id := int64(m["id"].(float64))
	p1, err := payout.Get(&s.ctx, id)
	assert.NoError(s.T(), err)
	assert.Equal(s.T(), p1.Status, payout.StatusNew)

	// Повторяем выплату, должны вернуть ID той же выплаты, что и в начале
	resp, code = createPayout(s.T(), strings.NewReader(bodyRaw), s.ctx)
	assert.Equal(s.T(), http.StatusOK, code, resp.Data)

	m = resp.Data.(map[string]any)

	id = int64(m["id"].(float64))
	p2, err := payout.Get(&s.ctx, id)
	assert.NoError(s.T(), err)
	assert.Equal(s.T(), p1.ID, p2.ID)

	// Меняем статус и проверяем, что тот же платеж вернется
	assert.NoError(s.T(), payout.Update(&s.ctx, id, "status", payout.StatusRejected))
	resp, code = createPayout(s.T(), strings.NewReader(bodyRaw), s.ctx)
	assert.Equal(s.T(), http.StatusOK, code, resp.Data)

	m = resp.Data.(map[string]any)

	id = int64(m["id"].(float64))
	p3, err := payout.Get(&s.ctx, id)
	assert.NoError(s.T(), err)

	assert.Equal(s.T(), p1.ID, p3.ID)
	assert.Equal(s.T(), p3.Status, payout.StatusRejected)
}

// TestPostRequestNoNamespaceFail проверяем, что без указания namespace получаем ошибку
func (s *HandlersV1TestSuite) TestPostRequestNoNamespaceFail() {
	externalID := bt.RandS(50)
	clientID := 1234
	bodySrc := fmt.Sprintf(`{
		"external_id": "%s",
		"client_id": %d,
	}`, externalID, clientID)
	body := strings.NewReader(bodySrc)

	resp, code := createPayoutByClient(s.T(), body, s.ctx)
	assert.Equal(s.T(), http.StatusBadRequest, code)
	assert.Equal(s.T(), http.StatusBadRequest, resp.Code, resp.Data)
}

// TestPostRequestUnknownNamespaceFail проверяем, что для неизвестного namespace получаем ошибку
func (s *HandlersV1TestSuite) TestPostRequestUnknownNamespaceFail() {
	externalID := bt.RandS(50)
	clientID := 1234
	bodySrc := fmt.Sprintf(`{
		"external_id": "%s",
		"client_id": %d,
		"namespace": "<namespace>"
	}`, externalID, clientID)
	body := strings.NewReader(bodySrc)

	resp, code := createPayoutByClient(s.T(), body, s.ctx)
	assert.Equal(s.T(), http.StatusBadRequest, code)
	assert.Equal(s.T(), http.StatusBadRequest, resp.Code, resp.Data)
}

// TestPostRequestCpfNamespacePayout проверяем, что c урезанным namespace получаем ошибку при создании обычной заявки
func (s *HandlersV1TestSuite) TestPostRequestCpfNamespacePayout() {
	externalID := bt.RandS(50)
	clientID := 1234
	bodySrc := fmt.Sprintf(`{
		"external_id": "%s",
		"client_id": %d,
		"namespace": "%s"
	}`, externalID, clientID, CpfOnly)
	body := strings.NewReader(bodySrc)

	resp, code := createPayoutByClient(s.T(), body, s.ctx)
	assert.Equal(s.T(), http.StatusBadRequest, code)
	assert.Equal(s.T(), http.StatusBadRequest, resp.Code, resp.Data)
}

// TestPostRequestNamespaceOK проверяем, что можем создать заявку на выплату по существующему namespace
func (s *HandlersV1TestSuite) TestPostRequestNamespaceOK() {
	externalID := bt.RandS(50)
	clientID := 1234
	bodySrc := fmt.Sprintf(`{
		"external_id": "%s",
		"client_id": %d,
		"namespace": "%s"
	}`, externalID, clientID, TestNamespace)
	body := strings.NewReader(bodySrc)

	resp, code := createPayoutByClient(s.T(), body, s.ctx)
	assert.Equal(s.T(), http.StatusCreated, code)
	require.Equal(s.T(), http.StatusCreated, resp.Code, resp.Data)

	// Проверяем, что в БД такая же информация
	m := resp.Data.(map[string]any)

	id := int64(m["id"].(float64))
	p, err := request.Get(&s.ctx, id)
	assert.NoError(s.T(), err)

	assert.Equal(s.T(), p.ID, id)
	assert.Equal(s.T(), p.ExternalID, externalID)
	assert.Equal(s.T(), p.ClientID, int64(clientID))
	assert.Equal(s.T(), p.Status, payout.StatusNew)
	assert.Equal(s.T(), p.Namespace, TestNamespace)
	assert.False(s.T(), p.CpfOnly)
}

// TestPostRequestCpfOnlyOK проверяем, что можем создать заявку только по CPF для обычного и урезанного namespace
func (s *HandlersV1TestSuite) TestPostRequestCpfOnlyOK() {
	externalID := bt.RandS(50)
	clientID := 1234
	bodySrc := fmt.Sprintf(`{
		"external_id": "%s",
		"client_id": %d,
		"namespace": "%s",
		"cpf_only": true
	}`, externalID, clientID, TestNamespace)
	body := strings.NewReader(bodySrc)

	resp, code := createPayoutByClient(s.T(), body, s.ctx)
	assert.Equal(s.T(), http.StatusCreated, code)
	require.Equal(s.T(), http.StatusCreated, resp.Code, resp.Data)

	m := resp.Data.(map[string]any)
	id := int64(m["id"].(float64))
	p, err := request.Get(&s.ctx, id)
	assert.NoError(s.T(), err)

	assert.Equal(s.T(), p.ID, id)
	assert.Equal(s.T(), p.ExternalID, externalID)
	assert.Equal(s.T(), p.ClientID, int64(clientID))
	assert.Equal(s.T(), p.Status, payout.StatusNew)
	assert.Equal(s.T(), p.Namespace, TestNamespace)
	assert.True(s.T(), p.CpfOnly)

	// проверяем для урезанного namespace
	externalID = bt.RandS(50)
	bodySrc = fmt.Sprintf(`{
		"external_id": "%s",
		"client_id": %d,
		"namespace": "%s",
		"cpf_only": true
	}`, externalID, clientID, CpfOnly)
	body = strings.NewReader(bodySrc)

	resp, code = createPayoutByClient(s.T(), body, s.ctx)
	assert.Equal(s.T(), http.StatusCreated, code)
	require.Equal(s.T(), http.StatusCreated, resp.Code, resp.Data)

	// Проверяем, что в БД такая же информация
	m = resp.Data.(map[string]any)

	id = int64(m["id"].(float64))
	p, err = request.Get(&s.ctx, id)
	assert.NoError(s.T(), err)

	assert.Equal(s.T(), p.ID, id)
	assert.Equal(s.T(), p.ExternalID, externalID)
	assert.Equal(s.T(), p.ClientID, int64(clientID))
	assert.Equal(s.T(), p.Status, payout.StatusNew)
	assert.Equal(s.T(), p.Namespace, CpfOnly)
	assert.True(s.T(), p.CpfOnly)
}

func (s *HandlersV1TestSuite) TestPostRequestRetry() {
	externalID := bt.RandS(50)
	clientID := 1234
	bodySrc := fmt.Sprintf(`{
		"external_id": "%s",
		"client_id": %d,
		"namespace": "tests"
	}`, externalID, clientID)

	resp, code := createPayoutByClient(s.T(), strings.NewReader(bodySrc), s.ctx)
	assert.Equal(s.T(), http.StatusCreated, code)
	assert.Equal(s.T(), http.StatusCreated, resp.Code, resp.Data)

	resp, code = createPayoutByClient(s.T(), strings.NewReader(bodySrc), s.ctx)
	assert.Equal(s.T(), http.StatusOK, code)
	assert.Equal(s.T(), http.StatusOK, resp.Code, resp.Data)
}

func (s *HandlersV1TestSuite) TestPostRequestBad() {
	clientID := 1234
	bodySrc := fmt.Sprintf(`{
		"client_id": %d, [],
	}`, clientID)

	resp, code := createPayoutByClient(s.T(), strings.NewReader(bodySrc), s.ctx)
	assert.Equal(s.T(), http.StatusBadRequest, code)
	assert.Equal(s.T(), http.StatusBadRequest, resp.Code, resp.Data)
}

func (s *HandlersV1TestSuite) TestGetCheckDataOk() {
	// prepare data
	checkID := bt.RandN64()
	p, err := payout.Create(&s.ctx, &payout.NewPayout{
		ExternalID: bt.RandS(30),
		ServiceID:  124,
		ContractID: bt.RandN64(),
		Amount:     decimal.RequireFromString("124.666"),
		Currency:   "RUB",
	})
	require.NoError(s.T(), err)
	err = payout.UpdateX(&s.ctx, p.ID, core.UpdateDesc{
		{Name: payout.CheckIDCol, Value: checkID},
		{Name: payout.AmountOEBSCol, Value: p.Amount},
	})
	require.NoError(s.T(), err)

	// prepare request
	emptyCheckID := bt.RandN64()
	bodySrc := fmt.Sprintf(`{
		"checks": [%v, %v]
	}`, checkID, emptyCheckID)
	body := strings.NewReader(bodySrc)

	// check result
	resp, code := getCheckIDData(s.T(), body, s.ctx)
	require.Equal(s.T(), http.StatusOK, code)

	data := resp.Data.(map[string]any)
	xlog.Info(s.ctx, "resp data", log.Any("data", data))
	value, ok := data[strconv.FormatInt(checkID, 10)]
	require.True(s.T(), ok)

	batchData := value.(map[string]any)
	amount, _ := p.Amount.Float64()
	require.Equal(s.T(), amount, batchData["total_sum"])
	require.EqualValues(s.T(), 1, batchData["message_count"])

	value, ok = data[strconv.FormatInt(emptyCheckID, 10)]
	require.True(s.T(), ok)
	require.Nil(s.T(), value)
}

func (s *HandlersV1TestSuite) TestGetCheckDataBadRequest() {
	bodySrc := `{
		"checks": [123, "non_int"]
	}`
	body := strings.NewReader(bodySrc)
	resp, code := getCheckIDData(s.T(), body, s.ctx)
	require.Equal(s.T(), http.StatusBadRequest, code)
	assert.Equal(s.T(), http.StatusBadRequest, resp.Code, resp.Data)
}

func (s *HandlersV1TestSuite) TestGetPayoutInfoBadRequest() {
	resp, code := getPayoutInfo(s.T(), "", "", "", []string{}, s.ctx)
	require.Equal(s.T(), http.StatusBadRequest, code)
	require.Equal(s.T(), http.StatusBadRequest, resp.Code, resp.Data)

	resp, code = getPayoutInfo(s.T(), "sdvs", "", "", []string{}, s.ctx)
	require.Equal(s.T(), http.StatusBadRequest, code)
	require.Equal(s.T(), http.StatusBadRequest, resp.Code, resp.Data)

	resp, code = getPayoutInfo(s.T(), "123", "", "", []string{}, s.ctx)
	require.Equal(s.T(), http.StatusBadRequest, code)
	require.Equal(s.T(), http.StatusBadRequest, resp.Code, resp.Data)

	resp, code = getPayoutInfo(s.T(), "123", "2000-15-07", "", []string{}, s.ctx)
	require.Equal(s.T(), http.StatusBadRequest, code)
	require.Equal(s.T(), http.StatusBadRequest, resp.Code, resp.Data)

	resp, code = getPayoutInfo(s.T(), "123", "2006-06-12", "2000-15-07", []string{}, s.ctx)
	require.Equal(s.T(), http.StatusBadRequest, code)
	require.Equal(s.T(), http.StatusBadRequest, resp.Code, resp.Data)
}

// TestGetPayoutInfoOkFull проверяет корректность работы при всех заданных параметрах (включая множественный statuses)
func (s *HandlersV1TestSuite) TestGetPayoutInfoOkFull() {
	// prepare data
	client1 := bt.RandN64()
	_, err := createRandomPayout(&s.ctx, &payout.NewPayout{
		ServiceID: 124,
		Amount:    decimal.RequireFromString("124.666"),
		ClientID:  client1,
	}, payout.StatusNew)
	require.NoError(s.T(), err)
	pPending, err := createRandomPayout(&s.ctx, &payout.NewPayout{
		ServiceID: 124,
		Amount:    decimal.RequireFromString("124.666"),
		ClientID:  client1,
	}, payout.StatusPending)
	require.NoError(s.T(), err)
	pDone, err := createRandomPayout(&s.ctx, &payout.NewPayout{
		ServiceID: 124,
		Amount:    decimal.RequireFromString("124.666"),
		ClientID:  client1,
	}, payout.StatusDone)
	require.NoError(s.T(), err)
	require.NoError(s.T(), payout.Update(&s.ctx, pDone.ID, payout.CreateDTCol,
		time.Now().AddDate(0, 0, -5)))

	// make request
	resp, code := getPayoutInfo(s.T(), strconv.FormatInt(client1, 10),
		time.Now().AddDate(0, 0, -2).Format("2006-01-02"), // from
		time.Now().Format("2006-01-02"),                   // to
		[]string{payout.StatusPending, payout.StatusDone}, s.ctx)
	require.Equal(s.T(), http.StatusOK, code)
	require.Equal(s.T(), http.StatusOK, resp.Code)
	require.Equal(s.T(), "ok", resp.Status)

	// check results
	bytes, err := json.Marshal(resp.Data)
	require.NoError(s.T(), err)
	var results []payout.Payout
	require.NoError(s.T(), json.Unmarshal(bytes, &results))

	require.Len(s.T(), results, 1)
	require.Equal(s.T(), pPending.ID, results[0].ID)
	require.Equal(s.T(), pPending.Status, results[0].Status)
}

// TestGetPayoutInfoOkDefault проверяет корректность работы для дефолтных значений
func (s *HandlersV1TestSuite) TestGetPayoutInfoOkDefault() {
	client1 := bt.RandN64()
	pNew, err := createRandomPayout(&s.ctx, &payout.NewPayout{
		ServiceID: 124,
		Amount:    decimal.RequireFromString("124.666"),
		ClientID:  client1,
	}, payout.StatusNew)
	require.NoError(s.T(), err)
	pPending, err := createRandomPayout(&s.ctx, &payout.NewPayout{
		ServiceID: 124,
		Amount:    decimal.RequireFromString("124.666"),
		ClientID:  client1,
	}, payout.StatusPending)
	require.NoError(s.T(), err)
	pDone, err := createRandomPayout(&s.ctx, &payout.NewPayout{
		ServiceID: 124,
		Amount:    decimal.RequireFromString("124.666"),
		ClientID:  client1,
	}, payout.StatusDone)
	require.NoError(s.T(), err)
	require.NoError(s.T(), payout.Update(&s.ctx, pDone.ID, payout.CreateDTCol,
		time.Now().AddDate(0, 0, -5)))

	// make request
	resp, code := getPayoutInfo(s.T(), strconv.FormatInt(client1, 10),
		time.Now().AddDate(0, 0, -2).Format("2006-01-02"), // from
		"",
		[]string{}, s.ctx)
	require.Equal(s.T(), http.StatusOK, code)
	require.Equal(s.T(), http.StatusOK, resp.Code)
	require.Equal(s.T(), "ok", resp.Status)

	// check results
	bytes, err := json.Marshal(resp.Data)
	require.NoError(s.T(), err)
	var results []payout.Payout
	require.NoError(s.T(), json.Unmarshal(bytes, &results))

	require.Len(s.T(), results, 2)
	if results[0].ID > results[1].ID {
		results[0], results[1] = results[1], results[0]
	}
	require.Equal(s.T(), pNew.ID, results[0].ID)
	require.Equal(s.T(), pNew.Status, results[0].Status)

	require.Equal(s.T(), pPending.ID, results[1].ID)
	require.Equal(s.T(), pPending.Status, results[1].Status)
}

// createBalanceCPF создает CPF для тестирования
func createBalanceCPF(t *testing.T, body *strings.Reader, ctx context.PayoutContext) (string, int) {
	req := httptest.NewRequest(http.MethodPost, CreateCashPaymentFactURL, body)
	w := httptest.NewRecorder()

	if err := CreateCashPaymentFact(ctx, w, req); err != nil {
		assert.Fail(t, err.Error())
	}

	return w.Body.String(), w.Code
}

// TestCreateCashPaymentFact проверяет, что в базу t_cpf_dry_run записывается информация из ручки
func (s *HandlersV1TestSuite) TestCreateCashPaymentFact() {
	cpfID := bt.RandN64()
	createDt := time.Now().Format("2006-01-02")

	body := strings.NewReader(fmt.Sprintf(`{
		"id": %d,
		"amount": 101.15,
		"receipt_number": "ЛСТ-1010",
		"receipt_date": "%s",
		"operation_type": "INSERT2"
	}`, cpfID, createDt))

	resp, code := createBalanceCPF(s.T(), body, s.ctx)
	assert.Equal(s.T(), http.StatusOK, code)
	assert.Equal(s.T(), "OK", resp)

	// Проверяем, что в БД такая же информация
	CPF, err := cpf.GetBalanceCPF(&s.ctx, cpfID)
	assert.NoError(s.T(), err)

	actualCreateDt, err := time.Parse(time.RFC3339, CPF.DT)
	assert.NoError(s.T(), err)

	amount, err := decimal.NewFromString("101.15")
	assert.NoError(s.T(), err)

	assert.Equal(s.T(), CPF.ID, cpfID)
	assert.Equal(s.T(), CPF.Amount, amount)
	assert.Equal(s.T(), CPF.ExtID, "ЛСТ-1010")
	assert.Equal(s.T(), actualCreateDt.Format("2006-01-02"), createDt)
	assert.Equal(s.T(), CPF.OpType, "INSERT2")
}

// TestCreateCashPaymentFactExists проверяет, что если в ручку приходит CPF с ID, который уже есть в базе, ручка отдает 200
func (s *HandlersV1TestSuite) TestCreateCashPaymentFactExists() {
	// Отправляем CPF первый раз
	cpfID := bt.RandN64()
	createDt := time.Now().Format("2006-01-02")

	body := strings.NewReader(fmt.Sprintf(`{
		"id": %d,
		"amount": 90.09,
		"receipt_number": "ЛСТ-9009",
		"receipt_date": "%s",
		"operation_type": "INSERT2"
	}`, cpfID, createDt))

	resp, code := createBalanceCPF(s.T(), body, s.ctx)
	assert.Equal(s.T(), http.StatusOK, code)
	assert.Equal(s.T(), "OK", resp)

	// Проверяем, что в БД есть эта информация
	CPF, err := cpf.GetBalanceCPF(&s.ctx, cpfID)
	assert.NoError(s.T(), err)

	actualCreateDt, err := time.Parse(time.RFC3339, CPF.DT)
	assert.NoError(s.T(), err)

	amount, err := decimal.NewFromString("90.09")
	assert.NoError(s.T(), err)

	assert.Equal(s.T(), CPF.ID, cpfID)
	assert.Equal(s.T(), CPF.Amount, amount)
	assert.Equal(s.T(), CPF.ExtID, "ЛСТ-9009")
	assert.Equal(s.T(), actualCreateDt.Format("2006-01-02"), createDt)
	assert.Equal(s.T(), CPF.OpType, "INSERT2")

	// Отправляем такой же CPF второй раз
	body = strings.NewReader(fmt.Sprintf(`{
		"id": %d,
		"amount": 90.09,
		"receipt_number": "ЛСТ-9009",
		"receipt_date": "%s",
		"operation_type": "INSERT2"
	}`, cpfID, createDt))
	resp, code = createBalanceCPF(s.T(), body, s.ctx)
	assert.Equal(s.T(), http.StatusOK, code)
	assert.Equal(s.T(), "EXISTS", resp)
}

func createRandomPayout(ctx *context.PayoutContext, np *payout.NewPayout, status string) (*payout.Payout, error) {
	np.ExternalID = bt.RandS(30)
	np.ContractID = bt.RandN64()
	np.Currency = "RUB"
	p, err := payout.Create(ctx, np)
	if err != nil {
		return nil, err
	}
	err = payout.Update(ctx, p.ID, payout.StatusCol, status)
	if err != nil {
		return nil, err
	}
	p.Status = status
	return p, nil
}
