package accounts

import (
	"context"
	"encoding/json"
	"fmt"
	"net/url"
	"testing"

	"github.com/go-resty/resty/v2"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"

	"a.yandex-team.ru/billing/library/go/billingo/pkg/interactions"
)

type MockIntClient struct {
	// callback задается при создании, чтобы быть вызванным в MakeRequest
	callback func(request interactions.Request, response any) error
}

func (c *MockIntClient) MakeURL(_ string) (res string, err error) {
	return "", nil
}
func (c *MockIntClient) WriteMetrics(_ *interactions.Request, _ *resty.Response, _ error) {

}
func (c *MockIntClient) MakeRequest(_ context.Context, request interactions.Request, response any) error {
	return c.callback(request, response)
}

func (c *MockIntClient) MakeRequestRaw(_ context.Context, _ interactions.Request) *interactions.RawResponse {
	return nil
}

const testVersionPrefix = "v666"

func makeClient(t *testing.T, wantURL string, data any) Client {
	return makeClientX(t, wantURL, nil, data)
}

func makeClientX(t *testing.T, wantURL string, wantParams map[string]string, data any) Client {
	callback := func(request interactions.Request, response any) error {
		// Сразу проверяем URL, куда будем ходить
		assert.Equal(t, wantURL, request.APIMethod)
		if wantParams != nil {
			wantParamsValues := url.Values{}
			for k, v := range wantParams {
				wantParamsValues.Set(k, v)
			}
			assert.Equal(t, wantParamsValues, request.Params)
		}

		var dataRaw []byte
		var err error
		dataRaw, ok := data.([]byte)
		if !ok {
			dataRaw, err = json.Marshal(data)
			require.NoError(t, err)
		}
		fmt.Printf("Json raw response: %s\n", dataRaw)

		err = json.Unmarshal(dataRaw, response)
		require.NoError(t, err)

		return nil
	}

	// Клиент аккаунтера, у которого замокан транспорт, чтобы свой ответ подложить
	return Client{
		client:        &MockIntClient{callback: callback},
		versionPrefix: testVersionPrefix,
	}
}
