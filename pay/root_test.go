package actions

import (
	"context"
	"time"

	bconfig "a.yandex-team.ru/billing/library/go/billingo/pkg/config"
	"github.com/stretchr/testify/suite"

	"a.yandex-team.ru/payplatform/fes/collector/pkg/commands"
	"a.yandex-team.ru/payplatform/fes/collector/pkg/core"
	"a.yandex-team.ru/payplatform/fes/collector/pkg/interactions"
)

func setupContext() (*commands.Repository, func(), error) {
	loader, _ := bconfig.PrepareLoader()
	config := core.Config{}

	ctx, cancel := context.WithTimeout(context.Background(), time.Second)
	defer cancel()

	repo := &commands.Repository{}

	err := loader.Load(ctx, &config)
	if err != nil {
		return nil, nil, err
	}

	repo.Clients = &interactions.Clients{}

	return repo, func() {}, nil
}

type ActionTestSuite struct {
	suite.Suite
	repo    *commands.Repository
	cleanup func()
}

func (s *ActionTestSuite) SetupSuite() {
	var err error

	s.repo, s.cleanup, err = setupContext()
	s.Require().NoError(err)
}

func (s *ActionTestSuite) TearDownSuite() {
	if s.cleanup != nil {
		s.cleanup()
	}
}
