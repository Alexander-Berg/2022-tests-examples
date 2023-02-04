package manifest

import (
	"context"
	"testing"

	"github.com/golang/mock/gomock"
	"github.com/stretchr/testify/require"
	"github.com/stretchr/testify/suite"

	"a.yandex-team.ru/billing/hot/processor/pkg/interactions/common/mocks"
	btesting "a.yandex-team.ru/billing/library/go/billingo/pkg/testing"
)

type HTTPAdaptorMethod func(ctx context.Context, name string, path string, params map[string]any, json map[string]any, headers map[string]any) (any, error)

type testHTTPSuite struct {
	btesting.BaseSuite

	clientGetter *mocks.MockClientGetter

	adaptor HTTPManifestAdaptor
}

func (s *testHTTPSuite) SetupTest() {
	ctrl := gomock.NewController(s.T())

	s.clientGetter = mocks.NewMockClientGetter(ctrl)
	s.adaptor = HTTPManifestAdaptor{clientGetter: s.clientGetter}
}

func (s *testHTTPSuite) TestExecute() {
	ctx := context.Background()
	client := mocks.NewMockClient(s.Ctrl())

	clientName, method, path := btesting.RandS(4), btesting.RandS(4), btesting.RandS(16)
	params, json, headers :=
		map[string]any{btesting.RandS(4): btesting.RandS(4)},
		map[string]any{btesting.RandS(4): btesting.RandS(4)},
		map[string]any{btesting.RandS(4): btesting.RandS(4)}

	s.clientGetter.EXPECT().GetHTTPClient(clientName).Times(1).Return(client, nil)
	client.EXPECT().Execute(ctx, method, path, params, json, headers).Times(1)

	_, err := s.adaptor.Execute(ctx, clientName, method, path, params, json, headers)
	require.NoError(s.T(), err)
}

func (s *testHTTPSuite) TestMethods() {
	ctx := context.Background()

	for name, method := range map[string]HTTPAdaptorMethod{
		"GET":    s.adaptor.Get,
		"POST":   s.adaptor.Post,
		"PUT":    s.adaptor.Put,
		"DELETE": s.adaptor.Delete,
		"PATCH":  s.adaptor.Patch,
	} {
		s.T().Run(name, func(t *testing.T) {
			client := mocks.NewMockClient(s.Ctrl())

			clientName, path := btesting.RandS(4), btesting.RandS(16)
			params, json, headers :=
				map[string]any{btesting.RandS(4): btesting.RandS(4)},
				map[string]any{btesting.RandS(4): btesting.RandS(4)},
				map[string]any{btesting.RandS(4): btesting.RandS(4)}

			s.clientGetter.EXPECT().GetHTTPClient(clientName).Times(1).Return(client, nil)

			client.EXPECT().Execute(ctx, name, path, params, json, headers).Times(1)
			_, err := method(ctx, clientName, path, params, json, headers)
			require.NoError(t, err)
		})
	}
}

func TestHTTPSuite(t *testing.T) {
	suite.Run(t, &testHTTPSuite{})
}
