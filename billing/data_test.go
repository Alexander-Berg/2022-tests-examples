package actions

import (
	"context"
	"testing"

	"github.com/stretchr/testify/suite"

	"a.yandex-team.ru/billing/hot/diod/pkg/server/schemas"
	btesting "a.yandex-team.ru/billing/library/go/billingo/pkg/testing"
)

type DataActionTestSuite struct {
	ActionTestSuite
}

func createItem(namespace ...string) schemas.BatchItem {

	var n string

	if len(namespace) == 0 {
		n = btesting.RandS(10)
	} else {
		n = namespace[0]
	}

	return schemas.BatchItem{
		Namespace: n,
		Key:       btesting.RandS(5),
		Value:     btesting.RandS(5),
	}
}

func (s *DataActionTestSuite) TestGetDataAction() {
	ctx := context.Background()

	item := createItem()

	serviceID := btesting.RandS(10)

	expected, err := s.repo.Storage.Data.CreateOrUpdateBatch(ctx, serviceID, []schemas.BatchItem{item})
	s.Require().NoError(err)

	expected[0].Created = false

	data, err := GetDataAction(ctx, s.repo, serviceID, item.Namespace, item.Key)

	s.Require().NoError(err)

	s.Assert().Equal(expected[0], *data)
}

func (s *DataActionTestSuite) TestGetNotExistingDataAction() {
	ctx := context.Background()

	_, err := GetDataAction(ctx, s.repo, btesting.RandS(10), btesting.RandS(10), btesting.RandS(5))
	s.Require().NoError(err)
}

func (s *DataActionTestSuite) TestGetDataBatchAction() {
	ctx := context.Background()

	serviceID := btesting.RandS(10)

	namespace := btesting.RandS(10)

	items := []schemas.BatchItem{
		createItem(namespace),
		createItem(namespace),
	}

	expected, err := s.repo.Storage.Data.CreateOrUpdateBatch(ctx, serviceID, items)
	s.Require().NoError(err)

	for i := range expected {
		expected[i].Created = false
	}

	data, err := GetDataBatchAction(ctx, s.repo, serviceID, namespace, []string{items[0].Key, items[1].Key})

	s.Require().NoError(err)

	s.Assert().Equal(expected, data)
}

func (s *DataActionTestSuite) TestGetBatchNotExistingDataAction() {
	ctx := context.Background()
	_, err := GetDataBatchAction(ctx, s.repo, btesting.RandS(10), btesting.RandS(10), []string{"cuffs", "hoodie"})

	s.Require().NoError(err)
}

func (s *DataActionTestSuite) TestCreateDataAction() {
	ctx := context.Background()

	serviceID := btesting.RandS(10)
	namespace := btesting.RandS(10)

	item := createItem(namespace)

	expected, err := CreateDataAction(ctx, s.repo, serviceID, item)
	s.Require().NoError(err)

	expected.Created = false

	data, err := s.repo.Storage.Data.GetBatch(ctx, serviceID, namespace, []string{item.Key})

	s.Require().NoError(err)

	s.Assert().Equal(*expected, data[0])
}

func (s *DataActionTestSuite) TestUpdateDataAction() {
	ctx := context.Background()

	serviceID := btesting.RandS(10)
	namespace := btesting.RandS(10)

	item := createItem(namespace)

	_, err := CreateDataAction(ctx, s.repo, serviceID, item)
	s.Require().NoError(err)

	data, err := s.repo.Storage.Data.GetBatch(ctx, serviceID, namespace, []string{item.Key})
	s.Require().NoError(err)

	s.Assert().Equal(uint64(1), data[0].Revision)
	s.Assert().Equal(item.Value, data[0].Value)

	// update value locally.
	item.Value = btesting.RandS(5)

	// update value in diod.
	_, err = CreateDataAction(ctx, s.repo, serviceID, item)
	s.Require().NoError(err)

	data, err = s.repo.Storage.Data.GetBatch(ctx, serviceID, namespace, []string{item.Key})
	s.Require().NoError(err)

	s.Assert().Equal(uint64(2), data[0].Revision)
	s.Assert().Equal(item.Value, data[0].Value)
}

func (s *DataActionTestSuite) TestCreateBatchDataAction() {
	ctx := context.Background()

	serviceID := btesting.RandS(10)

	namespace := btesting.RandS(10)

	items := []schemas.BatchItem{
		createItem(namespace),
		createItem(namespace),
	}

	expected, err := CreateDataBatchAction(ctx, s.repo, serviceID, items)
	s.Require().NoError(err)

	for i := range expected {
		expected[i].Created = false
	}

	data, err := s.repo.Storage.Data.GetBatch(ctx, serviceID, namespace, []string{items[0].Key, items[1].Key})

	s.Require().NoError(err)

	s.Assert().Equal(expected, data)
}

func (s *DataActionTestSuite) TestUpdateBatchDataAction() {
	ctx := context.Background()

	serviceID := btesting.RandS(10)

	namespace := btesting.RandS(10)

	items := []schemas.BatchItem{
		createItem(namespace),
		createItem(namespace),
	}

	_, err := CreateDataBatchAction(ctx, s.repo, serviceID, items)
	s.Require().NoError(err)

	data, err := s.repo.Storage.Data.GetBatch(ctx, serviceID, namespace, []string{items[0].Key, items[0].Key})
	s.Require().NoError(err)

	for i, element := range data {
		s.Assert().Equal(uint64(1), element.Revision)
		s.Assert().Equal(items[i].Value, element.Value)
	}

	for i := range items {
		items[i].Value = btesting.RandS(5)
	}

	_, err = CreateDataBatchAction(ctx, s.repo, serviceID, items)
	s.Require().NoError(err)

	data, err = s.repo.Storage.Data.GetBatch(ctx, serviceID, namespace, []string{items[0].Key, items[1].Key})
	s.Require().NoError(err)

	for i, element := range data {
		s.Assert().Equal(uint64(2), element.Revision)
		s.Assert().Equal(items[i].Value, element.Value)
	}
}

func (s *DataActionTestSuite) TestCreateDataWithImmutableFlag() {
	ctx := context.Background()

	serviceID := btesting.RandS(10)

	namespace := btesting.RandS(10)

	item := createItem(namespace)

	item.Immutable = true

	created, err := CreateDataAction(ctx, s.repo, serviceID, item)
	s.Require().NoError(err)
	created.Created = false

	data, err := GetDataAction(ctx, s.repo, serviceID, namespace, item.Key)
	s.Require().NoError(err)

	// check that key is created.
	s.Assert().Equal(*created, *data)

	updated, err := CreateDataAction(ctx, s.repo, serviceID, item)
	s.Require().NoError(err)

	// check that key with immutable flag does not change.
	s.Assert().Equal(*created, *updated)
}

func (s *DataActionTestSuite) TestCreateDataWithImmutableFlagAndChangingValue() {
	ctx := context.Background()

	serviceID := btesting.RandS(10)

	namespace := btesting.RandS(10)

	item := createItem(namespace)

	item.Immutable = true

	created, err := CreateDataAction(ctx, s.repo, serviceID, item)
	s.Require().NoError(err)
	created.Created = false

	data, err := GetDataAction(ctx, s.repo, serviceID, namespace, item.Key)
	s.Require().NoError(err)

	// check that key is created.
	s.Assert().Equal(*created, *data)

	item.Value = btesting.RandS(5)

	updated, err := CreateDataAction(ctx, s.repo, serviceID, item)
	s.Require().NoError(err)

	// check that key with immutable flag does not change.
	s.Assert().Equal(*created, *updated)
}

func (s *DataActionTestSuite) TestCreateBatchDataWithImmutableFlag() {
	ctx := context.Background()

	serviceID := btesting.RandS(10)

	namespace := btesting.RandS(10)

	items := []schemas.BatchItem{
		createItem(namespace),
		createItem(namespace),
	}

	for i := range items {
		items[i].Immutable = true
	}

	expected, err := CreateDataBatchAction(ctx, s.repo, serviceID, items)
	s.Require().NoError(err)

	for i := range expected {
		s.Assert().True(expected[i].Created)
		// change for next assertions.
		expected[i].Created = false
	}

	updated, err := CreateDataBatchAction(ctx, s.repo, serviceID, items)
	s.Require().NoError(err)

	// check that keys does not change.
	s.Assert().Equal(expected, updated)
}

func TestDataActionTestSuite(t *testing.T) {
	suite.Run(t, new(DataActionTestSuite))
}
