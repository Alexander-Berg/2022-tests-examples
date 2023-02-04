package manifest

import (
	"context"
	"testing"

	"github.com/golang/mock/gomock"
	"github.com/stretchr/testify/require"
	"github.com/stretchr/testify/suite"

	"a.yandex-team.ru/billing/hot/accounts/pkg/core/entities"
	"a.yandex-team.ru/billing/hot/processor/pkg/interactions/common/mocks"
	"a.yandex-team.ru/billing/hot/processor/pkg/storage/ytreferences"
	"a.yandex-team.ru/billing/library/go/billingo/mock"
	btesting "a.yandex-team.ru/billing/library/go/billingo/pkg/testing"
)

type testAdaptorSuite struct {
	btesting.BaseSuite

	accounter    *mock.MockAccounter
	genericMock  *ytreferences.MockGenerics
	ytStorage    *ytreferences.Storage
	clientGetter *mocks.MockClientGetter

	adaptor ManifestInterface
}

func (s *testAdaptorSuite) SetupTest() {
	ctrl := gomock.NewController(s.T())

	s.clientGetter = mocks.NewMockClientGetter(ctrl)
	s.accounter = mock.NewMockAccounter(ctrl)

	s.genericMock = ytreferences.NewMockGenerics(ctrl)
	s.ytStorage = &ytreferences.Storage{
		Generics: map[string]ytreferences.Generics{
			"test": s.genericMock,
		},
	}

	s.adaptor = Create(s.ytStorage, s.accounter, nil, nil, s.clientGetter)
}

func (s *testAdaptorSuite) TestCallYT() {
	ctx := context.Background()

	s.genericMock.EXPECT().Get(ctx, []any{"key"}).Times(1)
	_, err := s.adaptor.YT().GetGenericRowByType(ctx, "test", "key", "string")
	require.NoError(s.T(), err)
}

func (s *testAdaptorSuite) TestCallAccounter() {
	ctx := context.Background()
	var cnvAccountLocation entities.LocationAttributes
	value := "b"

	cnvAccountLocation.Namespace = "namespace"
	cnvAccountLocation.Type = "type"
	cnvAccountLocation.Attributes = map[string]*string{"a": &value}

	s.accounter.EXPECT().GetLock(ctx, &cnvAccountLocation).Times(1)

	_, err := s.adaptor.Accounter().GetLock(ctx, &entities.LocationAttributes{
		Namespace:  "namespace",
		Type:       "type",
		Attributes: map[string]*string{"a": &value},
	})
	require.NoError(s.T(), err)
}

func (s *testAdaptorSuite) TestCallHTTP() {
	ctx := context.Background()
	client := mocks.NewMockClient(s.Ctrl())

	s.clientGetter.EXPECT().GetHTTPClient("clientMock").Times(1).Return(client, nil)
	client.EXPECT().Execute(ctx, "POST", "path", nil, nil, nil).Times(1)

	_, err := s.adaptor.HTTP().Execute(ctx, "clientMock", "POST", "path", nil, nil, nil)
	require.NoError(s.T(), err)
}

func TestAdaptorSuite(t *testing.T) {
	suite.Run(t, &testAdaptorSuite{})
}
