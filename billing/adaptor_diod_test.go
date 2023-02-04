package manifest

import (
	"context"
	"testing"
	"time"

	"github.com/golang/mock/gomock"
	"github.com/stretchr/testify/suite"

	"a.yandex-team.ru/billing/hot/diod/pkg/core/entities"
	"a.yandex-team.ru/billing/hot/processor/pkg/interactions/diod"
	"a.yandex-team.ru/billing/hot/processor/pkg/interactions/diod/mocks"
	btesting "a.yandex-team.ru/billing/library/go/billingo/pkg/testing"
	"a.yandex-team.ru/library/go/core/xerrors"
)

type testDiodAdaptorSuite struct {
	btesting.BaseSuite

	clientMock *mocks.MockClient
	adaptor    DiodManifestAdaptor
}

func (s *testDiodAdaptorSuite) SetupTest() {
	ctrl := gomock.NewController(s.T())

	s.clientMock = mocks.NewMockClient(ctrl)
	s.adaptor = DiodManifestAdaptor{client: s.clientMock}
}

func (s *testDiodAdaptorSuite) TestExecute() {
	ctx := context.Background()

	namespace := btesting.RandS(4)
	keys := []string{btesting.RandS(4), btesting.RandS(4)}

	s.clientMock.EXPECT().GetKeys(gomock.Any(), gomock.Any()).Times(1)

	_, err := s.adaptor.GetKeys(ctx, namespace, keys)
	s.Require().NoError(err)
}

func (s *testDiodAdaptorSuite) TestGetKeys() {
	ctx := context.Background()

	serviceID, namespace := btesting.RandS(4), btesting.RandS(4)
	keys := []string{btesting.RandS(4), btesting.RandS(4)}
	returnData := []entities.Data{
		{
			ServiceID: serviceID,
			Namespace: namespace,
			Key:       keys[0],
			Revision:  1,
			Value:     btesting.RandS(4),
			CreatedAt: time.Now(),
			UpdatedAt: time.Now(),
			Created:   false,
		},
		{
			ServiceID: serviceID,
			Namespace: namespace,
			Key:       keys[1],
			Revision:  1,
			Value:     btesting.RandS(4),
			CreatedAt: time.Now(),
			UpdatedAt: time.Now(),
			Created:   false,
		},
	}

	s.clientMock.EXPECT().GetKeys(ctx, gomock.Any()).Times(1).Return(diod.Response{
		Response: nil,
		Status:   "ok",
		Data: struct {
			Items []entities.Data
		}{
			Items: returnData},
	},
		nil,
	)

	res, err := s.adaptor.GetKeys(ctx, namespace, keys)
	s.Require().NoError(err)
	s.Assert().Equal(res, returnData)
}

func (s *testDiodAdaptorSuite) TestGetKeysError() {
	ctx := context.Background()

	namespace := btesting.RandS(4)
	keys := []string{btesting.RandS(4), btesting.RandS(4)}
	retErr := xerrors.New("something went wrong")

	s.clientMock.EXPECT().GetKeys(ctx, gomock.Any()).Times(1).Return(diod.Response{
		Response: nil,
		Status:   "ok",
		Data: struct {
			Items []entities.Data
		}{
			Items: nil},
	},
		retErr,
	)

	_, err := s.adaptor.GetKeys(ctx, namespace, keys)
	s.Require().Error(err)
}

func (s *testDiodAdaptorSuite) TestUpdateKeys() {
	ctx := context.Background()

	serviceID, namespace := btesting.RandS(4), btesting.RandS(4)
	keys := []string{btesting.RandS(4), btesting.RandS(4)}
	values := []string{btesting.RandS(4), btesting.RandS(4)}
	returnData := []entities.Data{
		{
			ServiceID: serviceID,
			Namespace: namespace,
			Key:       keys[0],
			Revision:  1,
			Value:     values[0],
			CreatedAt: time.Now(),
			UpdatedAt: time.Now(),
			Created:   false,
		},
		{
			ServiceID: serviceID,
			Namespace: namespace,
			Key:       keys[1],
			Revision:  1,
			Value:     values[1],
			CreatedAt: time.Now(),
			UpdatedAt: time.Now(),
			Created:   false,
		},
	}

	s.clientMock.EXPECT().UpdateKeys(ctx, gomock.Any()).Times(1).Return(diod.Response{
		Response: nil,
		Status:   "ok",
		Data: struct {
			Items []entities.Data
		}{
			Items: returnData},
	},
		nil,
	)

	res, err := s.adaptor.UpdateKeys(ctx, []map[string]any{
		{
			"namespace": namespace,
			"key":       keys[0],
			"value":     values[0],
			"immutable": false,
		},
		{
			"namespace": namespace,
			"key":       keys[1],
			"value":     values[1],
			"immutable": false,
		},
	})

	s.Require().NoError(err)
	s.Assert().Equal(res, returnData)
}

func (s *testDiodAdaptorSuite) TestUpdateKeysError() {
	ctx := context.Background()

	retErr := xerrors.New("something went wrong")

	s.clientMock.EXPECT().UpdateKeys(ctx, gomock.Any()).Times(1).Return(diod.Response{
		Response: nil,
		Status:   "ok",
		Data: struct {
			Items []entities.Data
		}{
			Items: nil},
	},
		retErr,
	)

	s.adaptor.client = s.clientMock
	_, err := s.adaptor.UpdateKeys(ctx, []map[string]any{
		{"something": 1},
	})
	s.Require().Error(err)
}

func TestDiodAdaptor(t *testing.T) {
	suite.Run(t, &testDiodAdaptorSuite{})
}

type testDiodNotSetAdaptorSuite struct {
	btesting.BaseSuite
	adaptor DiodManifestAdaptor
}

func (s *testDiodNotSetAdaptorSuite) SetupTest() {
	s.adaptor = DiodManifestAdaptor{client: nil}
}

func (s *testDiodNotSetAdaptorSuite) TestGetKeysGetClientError() {
	ctx := context.Background()
	namespace := btesting.RandS(4)
	keys := []string{btesting.RandS(4), btesting.RandS(4)}

	_, err := s.adaptor.GetKeys(ctx, namespace, keys)
	s.Require().EqualError(err, "diod client is not set")
}

func (s *testDiodNotSetAdaptorSuite) TestUpdateKeysGetClientError() {
	ctx := context.Background()

	_, err := s.adaptor.UpdateKeys(ctx, []map[string]any{})
	s.Require().EqualError(err, "diod client is not set")
}

func TestDiodNotSetAdaptor(t *testing.T) {
	suite.Run(t, &testDiodNotSetAdaptorSuite{})
}
