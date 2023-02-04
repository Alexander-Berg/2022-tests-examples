package test

import (
	"context"
	"path/filepath"

	"a.yandex-team.ru/billing/configshop/example"
	"a.yandex-team.ru/billing/configshop/pkg/core/actions"
	"a.yandex-team.ru/billing/configshop/pkg/core/entities/tplops"
	memoryruntime "a.yandex-team.ru/billing/configshop/pkg/core/runtime/executors/memory"
	"a.yandex-team.ru/billing/configshop/pkg/core/template"
	"a.yandex-team.ru/billing/library/go/billingo/pkg/task"
)

func (s *E2ETestSuite) TestConfigurationValidate() {
	testCases := []struct {
		filename string
		code     string
		inputs   map[string]any
		status   string
		outputs  map[string]map[string]any
		errCode  any
	}{
		{
			filename: "simple_calc.yaml",
			code:     "simple_calc",
			inputs:   map[string]any{"service_name": "test_service_name"},
			errCode:  "global input validation failed",
		},
		{
			filename: "test_2_integrations.yaml",
			code:     "test_2_integrations",
			inputs:   map[string]any{"service_name": "test_service_name", "account_id": 666},
			status:   "done",
			outputs: map[string]map[string]any{
				"global": {
					"service_id": tplops.NilValue{},
				},
			},
		},
		{
			filename: "simple_validate.yaml",
			code:     "simple_validate",
			inputs:   map[string]any{"service1": "test_service_name", "service2": tplops.NilValue{}},
			status:   "done",
		},
	}

	for _, testCase := range testCases {
		tt := testCase
		s.Run(tt.filename, func() {
			memoryruntime.SetIntergrationCalcOutputFunc(integrationOutputCalcDefault)

			ctx := context.Background()
			file, err := example.TemplatesFS.ReadFile(filepath.Join("templates", tt.filename))
			s.Require().NoError(err)

			tm := template.NewManager(s.TemplateStorage)
			tmpl, err := tm.Save(ctx, tt.code, string(file), nil)
			s.Require().NoError(err)

			action := actions.NewActions(s.Storage, s.TemplateStorage, nil, map[string]task.Task{
				"dev": nil,
			}, nil)

			confID, err := action.CreateConfiguration(ctx, "test_code", tmpl.Code, "")
			s.Require().NoError(err)

			state, err := action.ValidateConfiguration(ctx, confID, tmpl.Version, tt.inputs, "dev")
			if tt.errCode != nil {
				s.Require().Error(err)
				s.Require().Contains(err.Error(), tt.errCode)
			} else {
				s.Require().NoError(err)
			}

			s.Require().Equal(tt.status, state.Status)

			for blockName, outputs := range tt.outputs {
				for _, block := range state.Blocks {
					if block.Name == blockName {
						for outputName, value := range outputs {
							s.Require().Equal(value, block.Outputs[outputName])
						}
					}
				}
			}
		})
	}
}
