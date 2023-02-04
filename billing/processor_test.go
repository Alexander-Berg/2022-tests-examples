package processor

import (
	"context"
	_ "embed"
	"strconv"
	"testing"

	"github.com/golang/mock/gomock"
	"github.com/stretchr/testify/suite"

	"a.yandex-team.ru/billing/configdepot/pkg/commands"
	"a.yandex-team.ru/billing/configdepot/pkg/core"
	processor "a.yandex-team.ru/billing/configdepot/pkg/core/actions/component_processor"
	"a.yandex-team.ru/billing/configdepot/pkg/core/entities"
	"a.yandex-team.ru/billing/configdepot/pkg/interactions"
	"a.yandex-team.ru/billing/library/go/billingo/mock"
	sbEntities "a.yandex-team.ru/billing/library/go/billingo/pkg/interactions/sandbox/entities"
	btesting "a.yandex-team.ru/billing/library/go/billingo/pkg/testing"
)

//go:embed gotest/test.yaml.tpl
var templateContent string

const (
	testOwner          = "BILLING-TEST"
	sandboxFaasTask    = "BILLING_FAAS_DEPOT_TASK"
	sandboxReleaseTask = "CREATE_RELEASE"
)

type ProcessorConfigSuite struct {
	btesting.BaseSuite

	sandboxClient *mock.MockSandboxClientProtocol

	sandboxUploader *mock.MockUploader

	repo commands.Repository

	processor Processor
}

func (s *ProcessorConfigSuite) SetupTest() {

	s.sandboxUploader = mock.NewMockUploader(s.Ctrl())

	s.sandboxClient = mock.NewMockSandboxClientProtocol(s.Ctrl())

	s.repo = commands.Repository{
		Config: &core.Config{
			Components: []entities.Component{
				{
					Key: "raw-sandbox",
					Method: entities.Method{
						Type: "Raw",
						Params: map[string]any{
							"target": map[string]any{
								"dst": "task_input.manifests",
							},
						},
					},
					Action: entities.Action{
						Type: "Sandbox",
						Params: map[string]any{
							"type":  sandboxFaasTask,
							"owner": testOwner,
							"task_input": map[string]any{
								"deploy_id": 0,
								"build_input_parameters": map[string]any{
									"minimal_revision":   8628496,
									"service":            "faas",
									"env":                "dev",
									"validate_revision":  true,
									"override_manifests": true,
								},
								// insert manifests here.
								//"manifests":       nil,
								"deploy_resource": 3244161235,
								"deploy_input_parameters": map[string]any{
									"config": map[string]any{
										"stage_id":         "billing-faas-dev-stage",
										"dry_run":          true,
										"awacs_namespace":  "faas.test.billing.yandex.net",
										"logbroker_topics": map[string]any{},
										"network_macro":    "_BILLING_DEPLOY_FAAS_TEST_NETS_",
										"clusters":         []string{"sas", "vla"},
										"release_type":     "development",
									},
									"context": map[string]any{
										"secret_uid": "sec-123321",
									},
								},
							},
						},
					},
				},
				{
					Key: "rau-sandbox",
					Method: entities.Method{
						Type: "RenderAndUpload",
						Params: map[string]any{
							"resource_type": "PROCESSOR_CONFIG",
							"owner":         testOwner,
							"template": map[string]any{
								"explicit":       templateContent,
								"serialize_type": "RAW",
							},
							"target": map[string]any{
								"dst": "task_input.sandbox_resources",
							},
						},
					},
					Action: entities.Action{
						Type: "Sandbox",
						Params: map[string]any{
							"type":  sandboxReleaseTask,
							"owner": testOwner,
							"task_input": map[string]any{
								"sandbox_resources": []any{},
								"config": map[string]any{
									"release_type": "testing",
								},
							},
						},
					},
				},
				{
					Key: "non-existing-action",
					Action: entities.Action{
						Type: "non-existing",
					},
				},
				{
					Key: "non-existing-method",
					Method: entities.Method{
						Type: "non-existing",
					},
					Action: entities.Action{
						Type: "Sandbox",
					},
				},
			},
		},
		Storage:        nil,
		ConfigdepotSQS: nil,
		Clients: &interactions.Clients{
			Sandbox:         s.sandboxClient,
			SandboxUploader: s.sandboxUploader,
		},
		Registry: nil,
	}

	s.processor = Processor{}
}

func (s *ProcessorConfigSuite) TestNonExistingComponent() {

	status, err := s.processor.Process(context.Background(), s.repo, 123, "qwe", "non-existing", []map[string]any{})

	s.Require().Error(err)
	s.Assert().Equal(processor.Failure, status)
}

func (s *ProcessorConfigSuite) TestRawMethodSandboxAction() {

	deployID := int(btesting.RandN64())

	task := &sbEntities.Task{ID: 123}

	s.sandboxClient.EXPECT().Tasks(gomock.Any(), gomock.Any(), gomock.Any(), gomock.Any(), gomock.Any())

	s.sandboxClient.EXPECT().CreateTask(gomock.Any(), gomock.Any()).Return(task, nil)
	s.sandboxClient.EXPECT().EnqueueTask(gomock.Any(), task).Return(nil)

	status, err := s.processor.Process(context.Background(), s.repo, deployID, "qwe", "raw-sandbox", []map[string]any{
		{"program": "/bin/bash"},
		{"args": "-c \"echo 'hello'\""},
	})

	s.Require().NoError(err)

	s.Assert().Equal(processor.Executing, status)
}

func (s *ProcessorConfigSuite) TestRenderAndUploadMethodSandboxAction() {
	deployID := int(btesting.RandN64())
	configurationKey := btesting.RandS(6)
	sandboxResource := btesting.RandN64()

	task := &sbEntities.Task{ID: 123}

	s.sandboxClient.EXPECT().Resources(gomock.Any(), gomock.Any(), gomock.Any(), gomock.Any())

	s.sandboxUploader.EXPECT().StreamUpload(gomock.Any(), testOwner, "PROCESSOR_CONFIG",
		map[string]string{"configdepot": "true", "deploy_id": strconv.Itoa(deployID)}, gomock.Any()).Return(sandboxResource, nil)

	s.sandboxClient.EXPECT().Tasks(gomock.Any(), gomock.Any(), gomock.Any(), gomock.Any(), gomock.Any())

	s.sandboxClient.EXPECT().CreateTask(gomock.Any(), gomock.Any()).Return(task, nil)
	s.sandboxClient.EXPECT().EnqueueTask(gomock.Any(), task).Return(nil)

	status, err := s.processor.Process(context.Background(), s.repo, deployID, configurationKey, "rau-sandbox", []map[string]any{
		{"foo": "bar"},
		{"foo": "bar"},
	})

	s.Require().NoError(err)

	s.Assert().Equal(processor.Executing, status)

}

func (s *ProcessorConfigSuite) TestFailedTask() {
	task := &sbEntities.Task{
		ID:     123,
		Status: sbEntities.Failure,
	}

	s.sandboxClient.EXPECT().Tasks(gomock.Any(), gomock.Any(), gomock.Any(), gomock.Any(), gomock.Any()).Return([]*sbEntities.Task{task}, nil)

	status, err := s.processor.Process(context.Background(), s.repo, 123, "qwe", "raw-sandbox", []map[string]any{})
	s.Require().Error(err)
	s.Assert().Equal(processor.Failure, status)
}

func (s *ProcessorConfigSuite) TestNonExistingMethod() {
	status, err := s.processor.Process(context.Background(), s.repo, 123, "qwe", "non-existing-method", []map[string]any{
		{"foo": "bar"},
		{"foo": "bar"},
	})

	s.Require().Error(err)

	s.Assert().Equal(processor.Failure, status)
}

func (s *ProcessorConfigSuite) TestNonExistingAction() {
	status, err := s.processor.Process(context.Background(), s.repo, 123, "qwe", "non-existing-action", []map[string]any{
		{"foo": "bar"},
		{"foo": "bar"},
	})

	s.Require().Error(err)
	s.Assert().Equal(processor.Failure, status)
}

func TestProcessorConfig(t *testing.T) {
	suite.Run(t, new(ProcessorConfigSuite))
}
