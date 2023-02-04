package actions

import (
	"context"
	"testing"

	"github.com/stretchr/testify/suite"

	btesting "a.yandex-team.ru/billing/library/go/billingo/pkg/testing"
	"a.yandex-team.ru/library/go/yandex/tvm"
)

type ServiceActionTestSuite struct {
	ActionTestSuite
}

func (s *ServiceActionTestSuite) TestCreateServiceAction() {
	ctx := context.Background()

	createdService, err := CreateServiceAction(ctx, s.repo, btesting.RandS(10))
	s.Require().NoError(err)

	service, err := s.repo.Storage.Service.Get(ctx, createdService.ServiceID)

	s.Assert().NoError(err)

	s.Assert().Equal(createdService, service)
}

func (s *ServiceActionTestSuite) TestGetServiceAction() {
	ctx := context.Background()

	createdService, err := s.repo.Storage.Service.Create(ctx, btesting.RandS(10))
	s.Require().NoError(err)

	getService, err := GetServiceAction(ctx, s.repo, createdService.ServiceID)
	s.Require().NoError(err)

	s.Assert().Equal(createdService, getService)
}

func (s *ServiceActionTestSuite) TestCreateExistingServiceAction() {
	ctx := context.Background()

	serviceName := btesting.RandS(10)

	_, err := CreateServiceAction(ctx, s.repo, serviceName)
	s.Require().NoError(err)

	_, err = CreateServiceAction(ctx, s.repo, serviceName)

	s.Assert().Errorf(err, "Service \"%s\" already exists", serviceName)
}

func (s *ServiceActionTestSuite) TestGetNotExistingService() {
	ctx := context.Background()

	_, err := GetServiceAction(ctx, s.repo, btesting.RandS(10))
	s.Assert().Errorf(err, "not found")
}

func TestServiceActionTestSuite(t *testing.T) {
	suite.Run(t, new(ServiceActionTestSuite))
}

type ServiceClientActionTestSuite struct {
	ActionTestSuite
}

func (s *ServiceClientActionTestSuite) TestCreateServiceClientAction() {
	ctx := context.Background()

	serviceID := btesting.RandS(10)
	tvmID := tvm.ClientID(btesting.RandN64())

	createdServiceClient, err := CreateServiceClientAction(ctx, s.repo, serviceID, tvmID)
	s.Require().NoError(err)

	serviceClient, err := s.repo.Storage.ServiceClient.Get(ctx, serviceID, tvmID)

	s.Assert().NoError(err)

	s.Assert().Equal(createdServiceClient, serviceClient)
}

func (s *ServiceClientActionTestSuite) TestCreateRecordsWithSameServiceID() {
	ctx := context.Background()

	serviceID := btesting.RandS(10)
	firstTvmID := tvm.ClientID(btesting.RandN64())
	secondTvmID := tvm.ClientID(btesting.RandN64())

	_, err := CreateServiceClientAction(ctx, s.repo, serviceID, firstTvmID)
	s.Require().NoError(err)

	_, err = CreateServiceClientAction(ctx, s.repo, serviceID, secondTvmID)
	s.Assert().NoError(err)
}

func (s *ServiceClientActionTestSuite) TestCreateRecordsWithSameServiceIDAndSameTvmID() {
	ctx := context.Background()

	serviceID := btesting.RandS(10)
	tvmID := tvm.ClientID(btesting.RandN64())

	_, err := CreateServiceClientAction(ctx, s.repo, serviceID, tvmID)
	s.Require().NoError(err)

	_, err = CreateServiceClientAction(ctx, s.repo, serviceID, tvmID)
	s.Assert().Error(err)
}

func TestServiceClientActionTestSuite(t *testing.T) {
	suite.Run(t, new(ServiceClientActionTestSuite))
}
