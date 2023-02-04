package actions

import (
	"testing"

	"github.com/stretchr/testify/suite"

	"a.yandex-team.ru/billing/configdepot/pkg/core/entities"
)

type ConfigurationActionTestSuite struct {
	BaseActionTestSuite
}

func (s *ConfigurationActionTestSuite) TestNewConfiguration() {
	actual, err := CreateConfigurationAction(s.ctx, &s.repository, ConfigurationData{
		ConfigurationKey: "music",
		Component:        "faas", Payload: map[string]any{"namespace": "music"},
	})
	s.Require().NoError(err)

	expected := entities.Config{
		ID: 1, DeployID: 1,
		ConfigurationKey: "music", Revision: 1, Context: "{\"namespace\":\"music\"}",
	}
	s.Assert().Equal(&expected, actual)
}

func (s *ConfigurationActionTestSuite) TestUpdateConfigurationOnNewID() {
	s.generateTestRows(entities.StatusNew)

	actual, err := CreateConfigurationAction(s.ctx, &s.repository, ConfigurationData{
		ConfigurationKey: "music",
		Component:        "faas", Payload: map[string]any{"namespace": "music"},
	})
	s.Require().NoError(err)

	expected := entities.Config{
		ID: 2, DeployID: 1, ConfigurationKey: "music", Revision: 2, Context: "{\"namespace\":\"music\"}",
	}
	s.Assert().Equal(&expected, actual)
}

func (s *ConfigurationActionTestSuite) TestUpdateConfigurationOnSameID() {
	s.generateTestRows(entities.StatusNew)

	actual, err := CreateConfigurationAction(s.ctx, &s.repository, ConfigurationData{
		ConfigurationKey: "bnpl", Component: "faas", Payload: map[string]any{"namespace": "bnpl"},
	})
	s.Require().NoError(err)

	expected := entities.Config{
		ID: 2, DeployID: 1, ConfigurationKey: "bnpl", Revision: 1, Context: "{\"namespace\":\"bnpl\"}",
	}
	s.Assert().Equal(&expected, actual)
}

func (s *ConfigurationActionTestSuite) TestUpdateConfigurationOnDoneID() {
	s.generateTestRows(entities.StatusDone)

	actual, err := CreateConfigurationAction(s.ctx, &s.repository, ConfigurationData{
		ConfigurationKey: "music", Component: "faas", Payload: map[string]any{"namespace": "music"},
	})
	s.Require().NoError(err)

	expected := entities.Config{
		ID: 2, DeployID: 2, ConfigurationKey: "music", Revision: 2, Context: "{\"namespace\":\"music\"}",
	}
	s.Assert().Equal(&expected, actual)
}

func (s *ConfigurationActionTestSuite) TestNewConfigurationOnDoneID() {
	s.generateTestRows(entities.StatusDone)

	actual, err := CreateConfigurationAction(s.ctx, &s.repository, ConfigurationData{
		ConfigurationKey: "bnpl", Component: "faas", Payload: map[string]any{"namespace": "bnpl"},
	})
	s.Require().NoError(err)

	expected := entities.Config{
		ID: 2, DeployID: 2, ConfigurationKey: "bnpl", Revision: 1, Context: "{\"namespace\":\"bnpl\"}",
	}
	s.Assert().Equal(&expected, actual)
}

func (s *ConfigurationActionTestSuite) TestNewComponentWithSameConfigurationID() {
	s.generateTestRows(entities.StatusNew)

	actual, err := CreateConfigurationAction(s.ctx, &s.repository, ConfigurationData{
		ConfigurationKey: "music", Component: "processor", Payload: map[string]any{"namespace": "music"},
	})
	s.Require().NoError(err)

	expected := entities.Config{
		ID: 2, DeployID: 2, ConfigurationKey: "music", Revision: 2, Context: "{\"namespace\":\"music\"}",
	}
	s.Assert().Equal(&expected, actual)
}

func (s *ConfigurationActionTestSuite) TestNewComponentWithNewConfigurationID() {
	s.generateTestRows(entities.StatusDone)

	actual, err := CreateConfigurationAction(s.ctx, &s.repository, ConfigurationData{
		ConfigurationKey: "music", Component: "processor", Payload: map[string]any{"namespace": "music"},
	})
	s.Require().NoError(err)

	expected := entities.Config{
		ID: 2, DeployID: 2, ConfigurationKey: "music", Revision: 2, Context: "{\"namespace\":\"music\"}",
	}
	s.Assert().Equal(&expected, actual)
}

func (s *ConfigurationActionTestSuite) generateTestRows(status string) {
	s.createTestDeployRow("faas", 1, status, &s.SampleTimes[0])

	s.createTestConfigRow(1, "music", 1, "{\"namespace\":\"music\"}")
}

func TestConfigurationActionTestSuite(t *testing.T) {
	suite.Run(t, new(ConfigurationActionTestSuite))
}
