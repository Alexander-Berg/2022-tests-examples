package test

import (
	"context"
	"path/filepath"
	"strings"

	"a.yandex-team.ru/billing/configshop/example"
	"a.yandex-team.ru/billing/configshop/pkg/core/template"
	"a.yandex-team.ru/billing/configshop/pkg/storage"
)

func (s *E2ETestSuite) TestTemplateManagerSave() {
	testCases := []string{
		"fun_example.yaml",
		"simple_graph.yaml",
		"simple_calc.yaml",
		"simple_validate.yaml",
		"test_2_integrations.yaml",
		"test_n_integrations.yaml",
	}

	for _, filename := range testCases {
		s.Run(filename, func() {
			ctx := context.Background()
			file, err := example.TemplatesFS.ReadFile(filepath.Join("templates", filename))
			s.Require().NoError(err)

			tplName := strings.TrimSuffix(filename, ".yaml")

			tm := template.NewManager(s.TemplateStorage)

			tmpl, err := tm.Save(ctx, tplName, string(file), nil)
			s.Require().NoError(err)

			s.Require().Equal(tplName, tmpl.Code)
			s.Require().Greater(tmpl.Version, 0)

			raw, err := tm.GetRawTemplate(ctx, storage.TemplateGetOptions{
				Code:    tmpl.Code,
				Version: tmpl.Version,
			})
			s.Require().NoError(err)

			s.Require().Equal(raw, string(file))
		})
	}
}

func (s *E2ETestSuite) TestTemplateManagerSaveValidation() {
	testCases := []struct {
		filename    string
		errContains string
	}{
		{
			filename:    filepath.Join("repeat", "bad_input_type.yaml"),
			errContains: "repeat var $odin_valenok must be of array type",
		},
		{
			filename:    filepath.Join("repeat", "bad_output_type.yaml"),
			errContains: "Array contains 2 different types: array[string] and string",
		},
	}

	for _, tc := range testCases {
		tc := tc
		s.Run(tc.filename, func() {
			ctx := context.Background()
			file, err := example.TemplatesFS.ReadFile(filepath.Join("templates", tc.filename))
			s.Require().NoError(err)

			tplName := strings.TrimSuffix(tc.filename, ".yaml")

			tm := template.NewManager(s.TemplateStorage)

			_, err = tm.Save(ctx, tplName, string(file), nil)
			s.Require().Error(err)
			s.Assert().Contains(err.Error(), tc.errContains)
		})
	}
}
