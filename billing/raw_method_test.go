package impl

import (
	"context"
	"strings"
	"testing"

	"github.com/stretchr/testify/suite"

	"a.yandex-team.ru/billing/configdepot/pkg/commands"
	"a.yandex-team.ru/billing/configdepot/pkg/interactions"
	"a.yandex-team.ru/billing/library/go/billingo/mock"
	"a.yandex-team.ru/billing/library/go/billingo/pkg/common"
	btesting "a.yandex-team.ru/billing/library/go/billingo/pkg/testing"
)

type RawMethodSuite struct {
	btesting.BaseSuite

	r *raw

	repo commands.Repository
}

func (s *RawMethodSuite) SetupTest() {
	s.r = NewRawMethod().(*raw)

	s.repo = commands.Repository{
		Config:         nil,
		Storage:        nil,
		ConfigdepotSQS: nil,
		Clients:        &interactions.Clients{Sandbox: mock.NewMockSandboxClientProtocol(s.Ctrl())},
		Registry:       nil,
	}
}

func (s *RawMethodSuite) TestInsertContextsNotExistingPath() {

	const path = "test.inner"

	s.r.params.Target.Dst = path

	contexts := []map[string]any{
		{
			"name":     "faas",
			"function": "billing.hot.faas",
		},
		{
			"foo": "bar",
		},
	}

	paramsToAppend := common.NewSyncStringMap()

	err := s.r.Do(context.Background(), s.repo, 0, contexts, paramsToAppend)
	s.Require().NoError(err)

	value, err := paramsToAppend.GetRecursive(strings.Split(path, ".")...)
	s.Require().NoError(err)
	s.Assert().Equal(contexts, value)
}

func (s *RawMethodSuite) TestInsertContextsExistingPath() {
	const path = "test.inner"

	s.r.params.Target.Dst = path

	contexts := []map[string]any{
		{
			"name":     "faas",
			"function": "billing.hot.faas",
		},
		{
			"foo": "bar",
		},
	}

	paramsToAppend := common.NewSyncStringMapWithData(map[string]any{
		"test": map[string]any{
			"inner": "some_old_value",
		},
	})

	err := s.r.Do(context.Background(), s.repo, 0, contexts, paramsToAppend)
	s.Require().NoError(err)

	value, err := paramsToAppend.GetRecursive(strings.Split(path, ".")...)
	s.Require().NoError(err)
	s.Assert().Equal(contexts, value)
}

func TestRawMethod(t *testing.T) {
	suite.Run(t, new(RawMethodSuite))
}
