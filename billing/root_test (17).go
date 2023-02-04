package actions

import (
	"context"
	"time"

	"github.com/stretchr/testify/suite"

	bconfig "a.yandex-team.ru/billing/library/go/billingo/pkg/config"
	"a.yandex-team.ru/billing/template-project/pkg/commands"
	"a.yandex-team.ru/billing/template-project/pkg/core"
	"a.yandex-team.ru/billing/template-project/pkg/storage/db"
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
	config.Storage.ReconnectRetries = 10

	repo.Storage = db.NewStorage(config.Storage)
	err = repo.Storage.Connect(ctx)
	if err != nil {
		return nil, nil, err
	}

	return repo, func() {
		_ = repo.Storage.Disconnect(ctx)
	}, nil
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
