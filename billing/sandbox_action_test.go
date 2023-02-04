package impl

import (
	"context"
	"fmt"
	"testing"

	"github.com/golang/mock/gomock"
	"github.com/stretchr/testify/suite"

	"a.yandex-team.ru/billing/configdepot/pkg/commands"
	"a.yandex-team.ru/billing/configdepot/pkg/core"
	compproc "a.yandex-team.ru/billing/configdepot/pkg/core/actions/component_processor"
	"a.yandex-team.ru/billing/configdepot/pkg/interactions"
	"a.yandex-team.ru/billing/library/go/billingo/mock"
	"a.yandex-team.ru/billing/library/go/billingo/pkg/interactions/sandbox/entities"
	btesting "a.yandex-team.ru/billing/library/go/billingo/pkg/testing"
)

const (
	taskType = "TEST_TASK"
)

type SandboxActionSuite struct {
	btesting.BaseSuite

	action           *sandboxAction
	deployID         int
	configurationKey string

	sandboxClientMock *mock.MockSandboxClientProtocol

	repo commands.Repository
}

func (s *SandboxActionSuite) SetupTest() {
	s.deployID = int(btesting.RandN64())
	s.configurationKey = btesting.RandS(6)
	s.action = NewSandboxAction().(*sandboxAction)

	s.sandboxClientMock = mock.NewMockSandboxClientProtocol(s.Ctrl())

	s.repo = commands.Repository{
		Config:         &core.Config{},
		Storage:        nil,
		ConfigdepotSQS: nil,
		Clients:        &interactions.Clients{Sandbox: s.sandboxClientMock},
		Registry:       nil,
	}
}

func (s *SandboxActionSuite) assertCustomFields(expected []entities.TaskFieldValidateItem, got []entities.TaskFieldValidateItem) {
	s.Assert().Equal(len(expected), len(got))

	for _, item := range expected {
		found := false
		for _, gotItem := range got {
			if item.Name == gotItem.Name {
				s.Assert().Equal(item.Value, gotItem.Value)
				found = true
				break
			}
		}
		s.Assert().True(found, fmt.Sprintf("custom field `%s` does not exist", item.Name))
	}
}

func (s *SandboxActionSuite) TestCreateNewTask() {
	s.action.params = SandboxActionParams{
		TaskType: taskType,
		TaskInputParameters: map[string]any{
			"task_input": map[string]any{
				"config": map[string]any{
					"stage_id":         "billing-faas-test-stage",
					"awacs_namespace":  "faas.test.billing.yandex.net",
					"minimal_revision": 123,
				},
			},
		},
	}

	customFields := []entities.TaskFieldValidateItem{
		{
			Name: "task_input",
			Value: map[string]any{
				"config": map[string]any{
					"stage_id":         "billing-faas-test-stage",
					"awacs_namespace":  "faas.test.billing.yandex.net",
					"minimal_revision": 123,
				},
			},
		},
		{
			Name:  "deploy_id",
			Value: s.deployID,
		},
		{
			Name:  "configuration_key",
			Value: s.configurationKey,
		},
	}

	retTask := &entities.Task{Type: taskType, ID: 123, Status: entities.Success}

	s.sandboxClientMock.EXPECT().Tasks(gomock.Any(), gomock.Any(), gomock.Any(), gomock.Any(), gomock.Any()).Return(nil, nil)

	s.sandboxClientMock.EXPECT().CreateTask(gomock.Any(), gomock.Any()).Do(func(_ any, req entities.TaskCreateRequest) {
		s.assertCustomFields(customFields, req.CustomFields)
	}).Return(retTask, nil)

	s.sandboxClientMock.EXPECT().EnqueueTask(gomock.Any(), retTask).Return(nil)

	status, err := s.action.Do(context.Background(), s.repo, s.deployID, s.configurationKey)
	s.Require().NoError(err)

	s.Assert().Equal(compproc.Executing, status)
}

func (s *SandboxActionSuite) TestTaskExistsAndExecuts() {
	s.action.params = SandboxActionParams{
		TaskType: taskType,
		TaskInputParameters: map[string]any{
			"__tasklet_input__": map[string]any{
				"config": map[string]any{
					"stage_id":         "billing-faas-test-stage",
					"awacs_namespace":  "faas.test.billing.yandex.net",
					"minimal_revision": 123,
				},
			},
		},
	}

	retTask := &entities.Task{Type: taskType, ID: 123, Status: entities.Executing}
	s.sandboxClientMock.EXPECT().Tasks(gomock.Any(), gomock.Any(), gomock.Any(), gomock.Any(), gomock.Any()).Return([]*entities.Task{retTask}, nil)

	status, err := s.action.Do(context.Background(), s.repo, s.deployID, s.configurationKey)
	s.Require().NoError(err)
	s.Assert().Equal(compproc.Executing, status)

}

func (s *SandboxActionSuite) TestTaskExistsAndFailed() {
	s.action.params = SandboxActionParams{
		TaskType: taskType,
		TaskInputParameters: map[string]any{
			"__tasklet_input__": map[string]any{
				"config": map[string]any{
					"stage_id":         "billing-faas-test-stage",
					"awacs_namespace":  "faas.test.billing.yandex.net",
					"minimal_revision": 123,
				},
			},
		},
	}

	retTask := &entities.Task{Type: taskType, ID: 123, Status: entities.Failure}
	s.sandboxClientMock.EXPECT().Tasks(gomock.Any(), gomock.Any(), gomock.Any(), gomock.Any(), gomock.Any()).Return([]*entities.Task{retTask}, nil)

	status, err := s.action.Do(context.Background(), s.repo, s.deployID, s.configurationKey)
	s.Require().Error(err)
	s.Assert().Equal(compproc.Failure, status)
}

func (s *SandboxActionSuite) TestTaskExistsAndException() {
	s.action.params = SandboxActionParams{
		TaskType: taskType,
		TaskInputParameters: map[string]any{
			"__tasklet_input__": map[string]any{
				"config": map[string]any{
					"stage_id":         "billing-faas-test-stage",
					"awacs_namespace":  "faas.test.billing.yandex.net",
					"minimal_revision": 123,
				},
			},
		},
	}

	retTask := &entities.Task{Type: taskType, ID: 123, Status: entities.Exception}
	s.sandboxClientMock.EXPECT().Tasks(gomock.Any(), gomock.Any(), gomock.Any(), gomock.Any(), gomock.Any()).Return([]*entities.Task{retTask}, nil)

	status, err := s.action.Do(context.Background(), s.repo, s.deployID, s.configurationKey)
	s.Require().Error(err)
	s.Assert().Equal(compproc.Failure, status)
}

func (s *SandboxActionSuite) TestTaskExistsAndInDraft() {
	s.action.params = SandboxActionParams{
		TaskType: taskType,
		TaskInputParameters: map[string]any{
			"__tasklet_input__": map[string]any{
				"config": map[string]any{
					"stage_id":         "billing-faas-test-stage",
					"awacs_namespace":  "faas.test.billing.yandex.net",
					"minimal_revision": 123,
				},
			},
		},
	}

	retTask := &entities.Task{Type: taskType, ID: 123, Status: entities.Draft}
	s.sandboxClientMock.EXPECT().Tasks(gomock.Any(), gomock.Any(), gomock.Any(), gomock.Any(), gomock.Any()).Return([]*entities.Task{retTask}, nil)

	s.sandboxClientMock.EXPECT().EnqueueTask(gomock.Any(), retTask).Return(nil)

	status, err := s.action.Do(context.Background(), s.repo, s.deployID, s.configurationKey)
	s.Require().NoError(err)
	s.Assert().Equal(compproc.Executing, status)
}

func TestSandboxAction(t *testing.T) {
	suite.Run(t, new(SandboxActionSuite))
}
