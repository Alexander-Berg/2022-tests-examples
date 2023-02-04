package impl

import (
	"context"
	_ "embed"
	"strings"
	"testing"

	"github.com/stretchr/testify/suite"
	"gopkg.in/yaml.v2"

	"a.yandex-team.ru/billing/configdepot/pkg/commands"
	"a.yandex-team.ru/billing/library/go/billingo/pkg/common"
	btesting "a.yandex-team.ru/billing/library/go/billingo/pkg/testing"
)

//go:embed gotest/test.yaml.tpl
var templateContents string

type RenderMethodSuite struct {
	btesting.BaseSuite

	r *render

	repo commands.Repository
}

func (s *RenderMethodSuite) SetupTest() {
	s.r = NewRenderMethod().(*render)

	s.r.params.Template = Template{
		Explicit:      templateContents,
		SerializeType: rawSerializationType,
	}

}

func (s *RenderMethodSuite) TestInsertContexts() {

	const path = "test.inner"

	s.r.params.Target.Dst = path

	contexts := []map[string]any{
		{
			"key":   "foo",
			"value": "first",
		},
		{
			"key":   "bar",
			"value": "second",
		},
	}

	validYamlMap := map[string]any{
		"foo": "first",
		"bar": "second",
	}

	paramsToAppend := common.NewSyncStringMap()

	err := s.r.Do(context.Background(), s.repo, 0, contexts, paramsToAppend)
	s.Require().NoError(err)

	value, err := paramsToAppend.GetRecursive(strings.Split(path, ".")...)
	s.Require().NoError(err)

	valueStr, ok := value.(string)

	s.Require().True(ok, "value is not string")

	unmarchalledContexts := make(map[string]any)

	err = yaml.Unmarshal([]byte(valueStr), &unmarchalledContexts)

	s.Require().NoError(err)

	s.Require().Equal(validYamlMap, unmarchalledContexts)
}

func TestRenderMethod(t *testing.T) {
	suite.Run(t, new(RenderMethodSuite))
}
