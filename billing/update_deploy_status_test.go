package actions

import (
	"testing"

	"github.com/stretchr/testify/suite"

	"a.yandex-team.ru/billing/configdepot/pkg/core/entities"
)

type UpdateDeployStatusTestSuite struct {
	BaseActionTestSuite
}

func (s *UpdateDeployStatusTestSuite) TestDeployExists() {
	s.createTestRows()

	actual, err := UpdateDeployStatusAction(s.ctx, &s.repository, 3, entities.StatusInProgress)
	s.Require().NoError(err)

	expected := entities.Deploy{
		ID: 3, Component: "processor", Revision: 14,
		StartAt: s.SampleTimes[2], Status: entities.StatusInProgress,
	}

	s.Assert().Equal(&expected, actual)
}

func (s *UpdateDeployStatusTestSuite) TestDeployDoesNotExist() {
	s.createTestRows()

	_, err := UpdateDeployStatusAction(s.ctx, &s.repository, 51, entities.StatusInProgress)
	s.Assert().Error(err)
}

func TestUpdateDeployStatusActionTestSuite(t *testing.T) {
	suite.Run(t, new(UpdateDeployStatusTestSuite))
}
