package mwtvm

import (
	"context"
	"encoding/json"
	"errors"
	"io"
	"net/http"
	"net/http/httptest"
	"net/url"
	"testing"

	"github.com/golang/mock/gomock"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"

	mwtvm "a.yandex-team.ru/library/go/httputil/middleware/tvm"
	"a.yandex-team.ru/library/go/yandex/tvm"
	tvm_mock "a.yandex-team.ru/library/go/yandex/tvm/mocks"
)

func handlerOK(w http.ResponseWriter, _ *http.Request) {
	res, err := json.Marshal(nil)
	if err != nil {
		panic("cannot marshal nil???")
	}

	_, _ = w.Write(res)
}

const (
	ticketValue = "any"
	srcID       = 10
)

var _ http.HandlerFunc = handlerOK

func getJSONResponse(t *testing.T, result *http.Response) map[string]any {
	t.Helper()

	body, err := io.ReadAll(result.Body)
	require.NoError(t, err)
	var jsonBody map[string]any
	require.NoError(t, json.Unmarshal(body, &jsonBody))

	return jsonBody
}

func expectOK(t *testing.T, w *httptest.ResponseRecorder) {
	t.Helper()

	result := w.Result()

	assert.Equal(t, http.StatusOK, result.StatusCode)
	jsonBody := getJSONResponse(t, result)

	require.Empty(t, jsonBody)
}

func expectNotAllowed(t *testing.T, w *httptest.ResponseRecorder) {
	t.Helper()

	result := w.Result()

	assert.Equal(t, http.StatusUnauthorized, result.StatusCode)
	jsonBody := getJSONResponse(t, result)

	require.NotEmpty(t, jsonBody)
	data := jsonBody["data"].(map[string]any)
	assert.Equal(t, "TVM_NOT_ALLOWED", data["code"])
}

func createTest(t *testing.T, opts ...Option) (*tvm_mock.MockClient, http.Handler, *httptest.ResponseRecorder, *http.Request) {
	ctrl := gomock.NewController(t)
	tvmMock := tvm_mock.NewMockClient(ctrl)
	handler := CheckServiceTicket(tvmMock, opts...)(http.HandlerFunc(handlerOK))

	w := httptest.NewRecorder()
	r, err := http.NewRequest("POST", "dunno", nil)
	require.NoError(t, err)
	return tvmMock, handler, w, r
}

func TestDefaultsOK(t *testing.T) {
	tvmClient, handler, w, r := createTest(t)
	r.Header.Add(mwtvm.XYaServiceTicket, ticketValue)
	tvmClient.EXPECT().CheckServiceTicket(r.Context(), ticketValue).
		Return(&tvm.CheckedServiceTicket{SrcID: srcID}, nil)

	handler.ServeHTTP(w, r)

	expectOK(t, w)
}

func TestDisabled(t *testing.T) {
	_, handler, w, r := createTest(t, WithDisabled(true))
	r.Header.Add(mwtvm.XYaServiceTicket, ticketValue)

	handler.ServeHTTP(w, r)
}

func TestNotDisabled(t *testing.T) {
	tvmClient, handler, w, r := createTest(t, WithDisabled(false))
	r.Header.Add(mwtvm.XYaServiceTicket, ticketValue)
	tvmClient.EXPECT().CheckServiceTicket(r.Context(), ticketValue).
		Return(&tvm.CheckedServiceTicket{SrcID: srcID}, nil)

	handler.ServeHTTP(w, r)

	expectOK(t, w)
}

func TestAllowedClients(t *testing.T) {
	tvmClient, handler, w, r := createTest(t, WithAllowedClients([]tvm.ClientID{srcID}))
	r.Header.Add(mwtvm.XYaServiceTicket, ticketValue)
	tvmClient.EXPECT().CheckServiceTicket(r.Context(), ticketValue).
		Return(&tvm.CheckedServiceTicket{SrcID: srcID}, nil)

	handler.ServeHTTP(w, r)

	expectOK(t, w)
}

func TestNotAllowedClient(t *testing.T) {
	tvmClient, handler, w, r := createTest(t, WithAllowedClients([]tvm.ClientID{12}))
	r.Header.Add(mwtvm.XYaServiceTicket, ticketValue)
	tvmClient.EXPECT().CheckServiceTicket(r.Context(), ticketValue).
		Return(&tvm.CheckedServiceTicket{SrcID: srcID}, nil)

	handler.ServeHTTP(w, r)

	expectNotAllowed(t, w)
}

func TestAuthClientError(t *testing.T) {
	tvmClient, handler, w, r := createTest(t, WithAuthClient(func(context.Context, tvm.ClientID) error {
		return errors.New("some error")
	}))
	r.Header.Add(mwtvm.XYaServiceTicket, ticketValue)
	tvmClient.EXPECT().CheckServiceTicket(r.Context(), ticketValue).
		Return(&tvm.CheckedServiceTicket{SrcID: srcID}, nil)

	handler.ServeHTTP(w, r)

	expectNotAllowed(t, w)
}

func TestAuthClientOK(t *testing.T) {
	tvmClient, handler, w, r := createTest(t, WithAuthClient(func(context.Context, tvm.ClientID) error {
		return nil
	}))
	r.Header.Add(mwtvm.XYaServiceTicket, ticketValue)
	tvmClient.EXPECT().CheckServiceTicket(r.Context(), ticketValue).
		Return(&tvm.CheckedServiceTicket{SrcID: srcID}, nil)

	handler.ServeHTTP(w, r)

	expectOK(t, w)
}

func TestErrorHandler(t *testing.T) {
	_, handler, w, r := createTest(t, WithErrorHandler(func(w http.ResponseWriter, _ *http.Request, _ error) {
		w.WriteHeader(http.StatusBadRequest)
	}))

	// No header set

	handler.ServeHTTP(w, r)

	assert.Equal(t, http.StatusBadRequest, w.Result().StatusCode)
}

func TestURLError(t *testing.T) {
	tvmClient, handler, w, r := createTest(t)
	r.Header.Add(mwtvm.XYaServiceTicket, ticketValue)
	tvmClient.EXPECT().CheckServiceTicket(r.Context(), ticketValue).
		Return(nil, &url.Error{})

	handler.ServeHTTP(w, r)

	assert.Equal(t, http.StatusInternalServerError, w.Result().StatusCode)
}

func TestSkipFunc(t *testing.T) {
	_, handler, w, r := createTest(t, WithSkipFunc(func(*http.Request) (bool, error) {
		return true, nil
	}))
	r.Header.Add(mwtvm.XYaServiceTicket, ticketValue)

	handler.ServeHTTP(w, r)

	expectOK(t, w)
}

func TestEmptyHeader(t *testing.T) {
	_, handler, w, r := createTest(t)
	handler.ServeHTTP(w, r)

	expectNotAllowed(t, w)
}
