package actions

import (
	"testing"
	"time"

	"github.com/stretchr/testify/suite"

	"a.yandex-team.ru/billing/configdepot/pkg/core/entities"
)

type DeploySelectionActionTestSuite struct {
	BaseActionTestSuite
}

func (s *DeploySelectionActionTestSuite) TestSingleNewDeploy() {
	s.createTestDeployRow("faas", 1, entities.StatusNew, &s.SampleTimes[0])

	s.createTestConfigRow(1, "bnpl", 1, "{ 'namespace': 'bnpl' }")
	s.createTestConfigRow(1, "aero", 1, "{ 'namespace': 'aero' }")

	_, actual, err := DeploySelectionAction(s.ctx, &s.repository, 1)
	s.Require().NoError(err)

	expected := []entities.Config{
		{
			ID: 1, DeployID: 1, ConfigurationKey: "bnpl", Revision: 1, Context: "{ 'namespace': 'bnpl' }",
		},
		{
			ID: 2, DeployID: 1, ConfigurationKey: "aero", Revision: 1, Context: "{ 'namespace': 'aero' }",
		},
	}

	s.Assert().ElementsMatch(expected, actual)
}

func (s *DeploySelectionActionTestSuite) TestSameComponentSameKey() {
	s.createTestDeployRow("faas", 1, entities.StatusDone, &s.SampleTimes[0])
	s.createTestDeployRow("faas", 2, entities.StatusNew, &s.SampleTimes[0])

	s.createTestConfigRow(1, "bnpl", 1, "{ 'namespace': 'bnpl' }")
	s.createTestConfigRow(1, "aero", 1, "{ 'namespace': 'aero' }")
	s.createTestConfigRow(2, "aero", 2, "{ 'namespace': 'aero', 'endpoints': {} }")

	_, actual, err := DeploySelectionAction(s.ctx, &s.repository, 2)
	s.Require().NoError(err)

	expected := []entities.Config{
		{
			ID: 1, DeployID: 1, ConfigurationKey: "bnpl", Revision: 1, Context: "{ 'namespace': 'bnpl' }",
		},
		{
			ID: 3, DeployID: 2, ConfigurationKey: "aero", Revision: 2, Context: "{ 'namespace': 'aero', 'endpoints': {} }",
		},
	}

	s.Assert().ElementsMatch(expected, actual)
}

func (s *DeploySelectionActionTestSuite) TestSameComponentNewKey() {
	s.createTestDeployRow("faas", 1, entities.StatusDone, &s.SampleTimes[0])
	s.createTestDeployRow("faas", 2, entities.StatusNew, &s.SampleTimes[0])

	s.createTestConfigRow(1, "bnpl", 1, "{ 'namespace': 'bnpl' }")
	s.createTestConfigRow(1, "aero", 1, "{ 'namespace': 'aero' }")
	s.createTestConfigRow(2, "taxi", 1, "{ 'namespace': 'taxi' }")

	_, actual, err := DeploySelectionAction(s.ctx, &s.repository, 2)
	s.Require().NoError(err)

	expected := []entities.Config{
		{
			ID: 1, DeployID: 1, ConfigurationKey: "bnpl", Revision: 1, Context: "{ 'namespace': 'bnpl' }",
		},
		{
			ID: 2, DeployID: 1, ConfigurationKey: "aero", Revision: 1, Context: "{ 'namespace': 'aero' }",
		},
		{
			ID: 3, DeployID: 2, ConfigurationKey: "taxi", Revision: 1, Context: "{ 'namespace': 'taxi' }",
		},
	}

	s.Assert().ElementsMatch(expected, actual)
}

func (s *DeploySelectionActionTestSuite) TestNewComponentSameKey() {
	s.createTestDeployRow("faas", 1, entities.StatusDone, &s.SampleTimes[0])
	s.createTestDeployRow("processor", 1, entities.StatusNew, &s.SampleTimes[0])

	s.createTestConfigRow(1, "bnpl", 1, "{ 'namespace': 'bnpl' }")
	s.createTestConfigRow(1, "aero", 1, "{ 'namespace': 'aero' }")
	s.createTestConfigRow(2, "aero", 2, "{ 'endpoints': {} }")

	_, actual, err := DeploySelectionAction(s.ctx, &s.repository, 2)
	s.Require().NoError(err)

	expected := []entities.Config{
		{
			ID: 3, DeployID: 2, ConfigurationKey: "aero", Revision: 2, Context: "{ 'endpoints': {} }",
		},
	}

	s.Assert().ElementsMatch(expected, actual)
}

func (s *DeploySelectionActionTestSuite) TestNewComponentNewKey() {
	s.createTestDeployRow("faas", 1, entities.StatusDone, &s.SampleTimes[0])
	s.createTestDeployRow("processor", 1, entities.StatusNew, &s.SampleTimes[0])

	s.createTestConfigRow(1, "bnpl", 1, "{ 'namespace': 'bnpl' }")
	s.createTestConfigRow(1, "aero", 1, "{ 'namespace': 'aero' }")
	s.createTestConfigRow(2, "taxi", 2, "{ 'endpoints': {} }")

	_, actual, err := DeploySelectionAction(s.ctx, &s.repository, 2)
	s.Require().NoError(err)

	expected := []entities.Config{
		{
			ID: 3, DeployID: 2, ConfigurationKey: "taxi", Revision: 2, Context: "{ 'endpoints': {} }",
		},
	}

	s.Assert().ElementsMatch(expected, actual)
}

func (s *DeploySelectionActionTestSuite) TestTwoComponentsInOneConfigurationWithNewKey() {
	s.createTestDeployRow("faas", 1, entities.StatusNew, &s.SampleTimes[0])
	s.createTestDeployRow("processor", 1, entities.StatusNew, &s.SampleTimes[0])

	s.createTestConfigRow(1, "bnpl", 1, "{ 'namespace': 'bnpl' }")
	s.createTestConfigRow(1, "aero", 1, "{ 'namespace': 'aero' }")
	s.createTestConfigRow(2, "taxi", 2, "{ 'endpoints': {} }")

	_, actual, err := DeploySelectionAction(s.ctx, &s.repository, 2)
	s.Require().NoError(err)

	expected := []entities.Config{
		{
			ID: 3, DeployID: 2, ConfigurationKey: "taxi", Revision: 2, Context: "{ 'endpoints': {} }",
		},
	}

	s.Assert().ElementsMatch(expected, actual)
}

func (s *DeploySelectionActionTestSuite) TestTwoComponentsInOneConfigurationWithSameKey() {
	s.createTestDeployRow("faas", 1, entities.StatusNew, &s.SampleTimes[0])
	s.createTestDeployRow("processor", 1, entities.StatusNew, &s.SampleTimes[0])

	s.createTestConfigRow(1, "bnpl", 1, "{ 'namespace': 'bnpl' }")
	s.createTestConfigRow(1, "aero", 1, "{ 'namespace': 'aero' }")
	s.createTestConfigRow(2, "aero", 2, "{ 'endpoints': {} }")

	_, actual, err := DeploySelectionAction(s.ctx, &s.repository, 2)
	s.Require().NoError(err)

	expected := []entities.Config{
		{
			ID: 3, DeployID: 2, ConfigurationKey: "aero", Revision: 2, Context: "{ 'endpoints': {} }",
		},
	}

	s.Assert().ElementsMatch(expected, actual)
}

func (s *DeploySelectionActionTestSuite) TestNonExistentDeployID() {
	s.createTestRows()

	_, _, err := DeploySelectionAction(s.ctx, &s.repository, 15)
	s.Assert().Error(err)
}

func (s *DeploySelectionActionTestSuite) TestStartAtAfterNow() {
	s.createTestRows()

	t := time.Now().Add(10 * time.Minute)
	s.createTestDeployRow("processor", 4, entities.StatusNew, &t)
	s.createTestConfigRow(7, "games", 2, "{ }")

	_, _, err := DeploySelectionAction(s.ctx, &s.repository, 7)
	s.Assert().ErrorIs(err, ErrorDeployTimeNotYetCome)
}

func TestDeploySelectionActionTestSuite(t *testing.T) {
	suite.Run(t, new(DeploySelectionActionTestSuite))
}
