package db

import (
	"testing"

	"github.com/stretchr/testify/suite"

	"a.yandex-team.ru/billing/configshop/pkg/storage/template"
	storagetest "a.yandex-team.ru/billing/configshop/pkg/storage/xtest"
)

type TemplateStorageTestSuite struct {
	storagetest.TemplateStorageTestSuite
	cleanup func()
}

//func (s *TemplateStorageTestSuite) TestGetTemplateOptions() {
//	ctx := context.Background()
//
//	file, err := example.TemplatesFS.ReadFile("templates/simple_calc.yaml")
//	s.Require().NoError(err)
//
//	code := "test_template_code"
//	description := "test_description"
//	raw := string(file)
//
//	version, err := s.Storage.Insert(ctx, code, raw, []templateentities.Block{{}}, &description)
//	s.Require().NoError(err)
//
//	s.Require().Greater(version, 0)
//
//	tmpl, err := s.Storage.List(ctx, code, 1)
//	s.Require().NoError(err)
//
//	s.Require().Equal(description, *tmpl[0].Description)
//
//	tpl, err := s.Storage.GetTemplate(
//		ctx,
//		storage.TemplateGetOptions{Version: version, Code: code},
//		storage.WithDescriptionField,
//		storage.WithRawField,
//	)
//	s.Require().NoError(err)
//
//	s.Require().Equal(description, *tpl.Description)
//	s.Require().Equal(raw, *tpl.Raw)
//}

func (s *TemplateStorageTestSuite) SetupSuite() {
	st, cleanup, err := SetupContext()
	s.Require().NoError(err)
	s.cleanup = cleanup

	cache := template.NewCache()
	s.Storage = NewTemplateStorage(st.Cluster, cache, nil)
	s.RuntimeStorage = NewRuntimeStorage(st.Cluster, cache, s.Storage, nil)
}

func (s *TemplateStorageTestSuite) TearDownSuite() {
	if s.cleanup != nil {
		s.cleanup()
	}
}

func TestTemplateStorage(t *testing.T) {
	suite.Run(t, new(TemplateStorageTestSuite))
}
