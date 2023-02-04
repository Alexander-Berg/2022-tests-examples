package db

import (
	"testing"

	"github.com/stretchr/testify/suite"

	"a.yandex-team.ru/billing/configdepot/pkg/core/entities"
	bsql "a.yandex-team.ru/billing/library/go/billingo/pkg/storage/sql"
)

type ConfigMapperTestSuite struct {
	BaseMapperTestSuite
}

func (s *ConfigMapperTestSuite) SetupTest() {
	s.BaseMapperTestSuite.SetupTest()

	s.createTestDeployRow("processor", 123, s.SampleTimes[1], entities.StatusNew)
	s.createTestDeployRow("mediator", 456, s.SampleTimes[4], entities.StatusNew)
	s.createTestDeployRow("faas", 789, s.SampleTimes[2], entities.StatusNew)
}

func (s *ConfigMapperTestSuite) TestCreateNew() {
	context := "{}"
	actual, err := s.storage.ConfigMapper.Create(s.ctx, ConfigCreateParams{
		DeployID: 1, ConfigurationKey: "test123", Revision: 123, Context: &context,
	})
	s.Require().NoError(err)

	expected := entities.Config{
		ID: 1, DeployID: 1, ConfigurationKey: "test123", Revision: 123, Context: context,
	}

	s.Assert().Equal(&expected, actual)
}

func (s *ConfigMapperTestSuite) TestCreateCollision() {
	s.generateTestConfigRows()

	context := "{}"
	_, err := s.storage.ConfigMapper.Create(s.ctx, ConfigCreateParams{
		DeployID: 1, ConfigurationKey: "test123", Revision: 123, Context: &context,
	})
	s.Require().Error(err)
}

func (s *ConfigMapperTestSuite) TestFindNoConditions() {
	s.generateTestConfigRows()

	actual, err := s.storage.ConfigMapper.Find(s.ctx, ConfigFindParams{})
	s.Require().NoError(err)

	expected := []entities.Config{
		{
			ID: 1, DeployID: 1, ConfigurationKey: "test123", Revision: 123, Context: "{ 'foo': 'bar' }",
		},
		{
			ID: 2, DeployID: 2, ConfigurationKey: "test456", Revision: 456, Context: "{ 'alice': 'bob' }",
		},
		{
			ID: 3, DeployID: 3, ConfigurationKey: "test789", Revision: 789, Context: "{ 'chip': 'dale' }",
		},
	}

	s.Assert().Equal(expected, actual)
}

func (s *ConfigMapperTestSuite) TestFindWithFieldConditions() {
	s.generateTestConfigRows()

	id := 2
	actual, err := s.storage.ConfigMapper.Find(s.ctx, ConfigFindParams{
		FieldConditions: &ConfigFieldConditions{DeployID: &id},
	})
	s.Require().NoError(err)

	expected := []entities.Config{
		{
			ID: 2, DeployID: 2, ConfigurationKey: "test456", Revision: 456, Context: "{ 'alice': 'bob' }",
		},
	}

	s.Assert().Equal(expected, actual)
}

func (s *ConfigMapperTestSuite) TestFindWithLimit() {
	s.generateTestConfigRows()

	limit := uint(1)
	actual, err := s.storage.ConfigMapper.Find(s.ctx, ConfigFindParams{
		Limit: &limit,
		OrderBy: bsql.OrderByConditions{
			{Field: entities.ConfigFieldRevision, IsAscending: false},
		},
	})
	s.Require().NoError(err)

	expected := []entities.Config{
		{
			ID: 3, DeployID: 3, ConfigurationKey: "test789", Revision: 789, Context: "{ 'chip': 'dale' }",
		},
	}

	s.Assert().Equal(expected, actual)
}

func (s *ConfigMapperTestSuite) TestFindWithJoin() {
	s.generateTestConfigRows()

	component := "mediator"
	actual, err := s.storage.ConfigMapper.Find(s.ctx, ConfigFindParams{
		JoinTDeployConditions: &DeployFieldConditions{
			Component: &component,
		},
	})
	s.Require().NoError(err)

	expected := []entities.Config{
		{
			ID: 2, DeployID: 2, ConfigurationKey: "test456", Revision: 456, Context: "{ 'alice': 'bob' }",
		},
	}

	s.Assert().Equal(expected, actual)
}

func (s *ConfigMapperTestSuite) TestFindWithAggeregate() {
	s.generateTestConfigRows()
	// Should be preffered over ID 2
	s.createTestConfigRow(2, 1000, "test456", "{ 'bonnie': 'clyde' }")

	actual, err := s.storage.ConfigMapper.Find(s.ctx, ConfigFindParams{
		Aggregate: &AggregationParams{
			Function: "(ARRAY_AGG(%s))[1]",
			GroupBy:  []string{entities.ConfigFieldConfigurationKey},
			OrderBy: bsql.OrderByConditions{
				{Field: entities.ConfigFieldRevision, IsAscending: false},
			},
		},
	})
	s.Require().NoError(err)

	expected := []entities.Config{
		{
			ID: 1, DeployID: 1, ConfigurationKey: "test123", Revision: 123, Context: "{ 'foo': 'bar' }",
		},
		{
			ID: 4, DeployID: 2, ConfigurationKey: "test456", Revision: 1000, Context: "{ 'bonnie': 'clyde' }",
		},
		{
			ID: 3, DeployID: 3, ConfigurationKey: "test789", Revision: 789, Context: "{ 'chip': 'dale' }",
		},
	}

	s.Assert().Equal(expected, actual)
}

func TestConfigMapperTestSuite(t *testing.T) {
	suite.Run(t, new(ConfigMapperTestSuite))
}

func (s *ConfigMapperTestSuite) generateTestConfigRows() {
	s.createTestConfigRow(1, 123, "test123", "{ 'foo': 'bar' }")
	s.createTestConfigRow(2, 456, "test456", "{ 'alice': 'bob' }")
	s.createTestConfigRow(3, 789, "test789", "{ 'chip': 'dale' }")
}
