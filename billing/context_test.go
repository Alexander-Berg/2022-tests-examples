package commands

import (
	"context"
	"testing"

	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/suite"

	"a.yandex-team.ru/billing/hot/mediator/pkg/core"
	btesting "a.yandex-team.ru/billing/library/go/billingo/pkg/testing"
)

type TestSuite struct {
	btesting.BaseSuite
}

func (s *TestSuite) TestCreateContext() {
	ctx := context.Background()
	cmd := StorageCommand{}
	cmd.Config = &core.Config{}

	if err := cmd.Init(ctx); err != nil {
		s.T().Fatal(err)
	}

	extraContext, err := cmd.CreateContext(ctx)
	if err != nil {
		s.T().Fatal(err)
	}

	var mediatorContext = CreateContextFrom(extraContext)

	assert.Equal(s.T(), mediatorContext.Registry, cmd.Registry)
	assert.Equal(s.T(), mediatorContext.Config, cmd.Config)
	assert.Equal(s.T(), mediatorContext.Context.Parent(), ctx)
	assert.Equal(s.T(), mediatorContext.Clients, cmd.Clients)
}

func TestContextTestSuite(t *testing.T) {
	suite.Run(t, new(TestSuite))
}
