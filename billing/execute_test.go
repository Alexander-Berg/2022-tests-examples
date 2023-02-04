package impl

import (
	"context"
	"fmt"
	"net/http"
	"net/url"
	"testing"

	"github.com/go-resty/resty/v2"
	"github.com/stretchr/testify/require"
	"github.com/stretchr/testify/suite"

	"a.yandex-team.ru/billing/hot/processor/pkg/interactions/common/mocks"
	"a.yandex-team.ru/billing/library/go/billingo/mock"
	"a.yandex-team.ru/billing/library/go/billingo/pkg/interactions"
	btesting "a.yandex-team.ru/billing/library/go/billingo/pkg/testing"
)

type testExecuteSuite struct {
	btesting.BaseSuite

	clientProtocol *mock.MockClientProtocol
	client         Client
}

func (s *testExecuteSuite) SetupTest() {
	s.clientProtocol = mock.NewMockClientProtocol(s.Ctrl())
	s.client = Client{ClientProtocol: s.clientProtocol}
}

func (s *testExecuteSuite) TestExecute() {
	ctx := context.Background()

	method, path := btesting.RandS(4), btesting.RandS(16)
	params, headers, body :=
		map[string]any{btesting.RandS(4): btesting.RandS(4)},
		map[string]any{btesting.RandS(4): btesting.RandS(4)},
		map[string]any{btesting.RandS(4): btesting.RandS(4)}

	request := interactions.Request{
		APIMethod: path,
		Method:    method,
		Body:      body,
		Params:    make(url.Values),
		Headers:   make(map[string]string),
	}

	for k, v := range params {
		request.Params.Set(k, v.(string))
	}
	for k, v := range headers {
		request.Headers[k] = v.(string)
	}

	for _, suppressError := range []bool{true, false} {
		for _, statusCode := range []int{200, 400, 403, 404, 500} {
			s.T().Run(fmt.Sprintf("suppressError-%v-statusCode-%d", suppressError, statusCode), func(t *testing.T) {
				s.client.Config.SuppressError = suppressError
				s.clientProtocol.EXPECT().MakeRequestRaw(ctx, request).Times(1).Return(&interactions.RawResponse{
					RawResponse: &resty.Response{
						RawResponse: &http.Response{StatusCode: statusCode},
					},
				})

				_, err := s.client.Execute(ctx, method, path, params, body, headers)

				if statusCode == 200 || suppressError {
					require.NoError(s.T(), err)
				} else {
					require.Error(s.T(), err)
				}
			})
		}
	}
}

func (s *testExecuteSuite) TestMethods() {
	ctx := context.Background()

	for name, method := range map[string]ClientMethod{
		"GET":    s.client.Get,
		"POST":   s.client.Post,
		"PUT":    s.client.Put,
		"DELETE": s.client.Delete,
		"PATCH":  s.client.Patch,
	} {
		for _, suppressError := range []bool{true, false} {
			for _, statusCode := range []int{200, 400, 403, 404, 500} {
				s.T().Run(
					fmt.Sprintf("%s-suppressError-%v-statusCode-%d", name, suppressError, statusCode),
					func(t *testing.T) {
						client := mocks.NewMockClient(s.Ctrl())

						path := btesting.RandS(16)
						params, headers, body :=
							map[string]any{btesting.RandS(4): btesting.RandS(4)},
							map[string]any{btesting.RandS(4): btesting.RandS(4)},
							map[string]any{btesting.RandS(4): btesting.RandS(4)}

						request := interactions.Request{
							APIMethod: path,
							Method:    name,
							Body:      body,
							Params:    make(url.Values),
							Headers:   make(map[string]string),
						}

						for k, v := range params {
							request.Params.Set(k, v.(string))
						}
						for k, v := range headers {
							request.Headers[k] = v.(string)
						}

						s.client.Config.SuppressError = suppressError
						s.clientProtocol.EXPECT().MakeRequestRaw(ctx, request).Times(1).Return(&interactions.RawResponse{
							RawResponse: &resty.Response{
								RawResponse: &http.Response{StatusCode: statusCode},
							},
						})

						client.EXPECT().Execute(ctx, name, path, params, body, headers).Times(1)
						_, err := method(ctx, path, params, body, headers)

						if statusCode == 200 || suppressError {
							require.NoError(s.T(), err)
						} else {
							require.Error(s.T(), err)
						}
					},
				)
			}
		}
	}
}

func TestExecuteSuite(t *testing.T) {
	suite.Run(t, &testExecuteSuite{})
}
