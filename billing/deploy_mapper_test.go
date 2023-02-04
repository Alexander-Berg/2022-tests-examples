package db

import (
	"testing"

	"github.com/stretchr/testify/suite"

	"a.yandex-team.ru/billing/configdepot/pkg/core/entities"
	bsql "a.yandex-team.ru/billing/library/go/billingo/pkg/storage/sql"
)

type DeployMapperTestSuite struct {
	BaseMapperTestSuite
}

func (s *DeployMapperTestSuite) TestCreateNew() {
	actual, err := s.storage.DeployMapper.Create(s.ctx, DeployCreateParams{
		Component: "custom", Revision: 123, StartAt: &s.SampleTimes[0],
	})

	expected := entities.Deploy{
		ID: 1, Component: "custom", Revision: 123, StartAt: s.SampleTimes[0], Status: entities.StatusNew,
	}

	s.Require().NoError(err)
	s.Assert().Equal(&expected, actual)
}

func (s *DeployMapperTestSuite) TestCreateCollision() {
	s.generateTestRows()

	_, err := s.storage.DeployMapper.Create(s.ctx, DeployCreateParams{
		Component: "custom", Revision: 456, StartAt: &s.SampleTimes[1],
	})

	s.Assert().Error(err)
}

func (s *DeployMapperTestSuite) TestSave() {
	s.generateTestRows()

	actual, err := s.storage.DeployMapper.Save(s.ctx, entities.Deploy{
		ID: 2, Component: "custom", Revision: 500,
		StartAt: s.SampleTimes[2], Status: entities.StatusInProgress,
	})
	s.Require().NoError(err)

	expected := entities.Deploy{
		ID: 2, Component: "custom", Revision: 500,
		StartAt: s.SampleTimes[2], Status: entities.StatusInProgress,
	}
	s.Assert().Equal(&expected, actual)
}

func (s *DeployMapperTestSuite) TestSaveNonExistent() {
	s.generateTestRows()

	_, err := s.storage.DeployMapper.Save(s.ctx, entities.Deploy{
		ID: 51, Component: "custom", Revision: 500,
		StartAt: s.SampleTimes[5], Status: entities.StatusInProgress,
	})
	s.Require().Error(err)
}

func (s *DeployMapperTestSuite) TestFindNoConditions() {
	s.generateTestRows()

	actual, err := s.storage.DeployMapper.Find(s.ctx, DeployFindParams{})
	s.Require().NoError(err)

	expected := []entities.Deploy{
		{
			ID: 1, Component: "custom", Revision: 123,
			Status: entities.StatusNew, StartAt: s.SampleTimes[0],
		},
		{
			ID: 2, Component: "custom", Revision: 456,
			Status: entities.StatusNew, StartAt: s.SampleTimes[3],
		},
		{
			ID: 3, Component: "custom", Revision: 789,
			Status: entities.StatusNew, StartAt: s.SampleTimes[1],
		},
	}

	s.Assert().Equal(expected, actual)
}

func (s *DeployMapperTestSuite) TestFindWithRevisionFieldCondition() {
	s.generateTestRows()

	revision := 500
	actual, err := s.storage.DeployMapper.Find(s.ctx, DeployFindParams{
		FieldConditions: &DeployFieldConditions{RevisionLessOrEq: &revision},
	})
	s.Require().NoError(err)

	expected := []entities.Deploy{
		{
			ID: 1, Component: "custom", Revision: 123,
			Status: entities.StatusNew, StartAt: s.SampleTimes[0],
		},
		{
			ID: 2, Component: "custom", Revision: 456,
			Status: entities.StatusNew, StartAt: s.SampleTimes[3],
		},
	}

	s.Assert().Equal(expected, actual)
}

func (s *DeployMapperTestSuite) TestFindWithStatusFieldCondition() {
	s.generateTestRows()
	s.createTestDeployRow("custom", 741, s.SampleTimes[5], entities.StatusError)
	s.createTestDeployRow("custom", 852, s.SampleTimes[7], entities.StatusDone)

	actual, err := s.storage.DeployMapper.Find(s.ctx, DeployFindParams{
		FieldConditions: &DeployFieldConditions{Statuses: []string{entities.StatusError, entities.StatusDone}},
	})
	s.Require().NoError(err)

	expected := []entities.Deploy{
		{
			ID: 4, Component: "custom", Revision: 741,
			Status: entities.StatusError, StartAt: s.SampleTimes[5],
		},
		{
			ID: 5, Component: "custom", Revision: 852,
			Status: entities.StatusDone, StartAt: s.SampleTimes[7],
		},
	}

	s.Assert().Equal(expected, actual)
}

func (s *DeployMapperTestSuite) TestFindWithLimit() {
	s.generateTestRows()

	limit := uint(1)
	actual, err := s.storage.DeployMapper.Find(s.ctx, DeployFindParams{
		Limit: &limit,
		OrderBy: bsql.OrderByConditions{
			{Field: entities.DeployFieldRevision, IsAscending: false},
		},
	})
	s.Require().NoError(err)

	expected := []entities.Deploy{
		{
			ID: 3, Component: "custom", Revision: 789,
			Status: entities.StatusNew, StartAt: s.SampleTimes[1],
		},
	}

	s.Assert().Equal(expected, actual)
}

func (s *DeployMapperTestSuite) TestGet() {
	s.generateTestRows()

	actual, err := s.storage.DeployMapper.Get(s.ctx, 2, false)
	s.Require().NoError(err)

	expected := entities.Deploy{
		ID: 2, Component: "custom", Revision: 456,
		StartAt: s.SampleTimes[3], Status: entities.StatusNew,
	}

	s.Assert().Equal(&expected, actual)
}

func TestDeployMapperTestSuite(t *testing.T) {
	suite.Run(t, new(DeployMapperTestSuite))
}

func (s *DeployMapperTestSuite) generateTestRows() {
	s.createTestDeployRow("custom", 123, s.SampleTimes[0], entities.StatusNew)
	s.createTestDeployRow("custom", 456, s.SampleTimes[3], entities.StatusNew)
	s.createTestDeployRow("custom", 789, s.SampleTimes[1], entities.StatusNew)
}
