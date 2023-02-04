package impl

import (
	"context"
	"time"

	"github.com/stretchr/testify/suite"

	"a.yandex-team.ru/billing/hot/scheduler/pkg/core"
	"a.yandex-team.ru/billing/hot/scheduler/pkg/core/actions"
	"a.yandex-team.ru/billing/hot/scheduler/pkg/storage/db"
	storageimpl "a.yandex-team.ru/billing/hot/scheduler/pkg/storage/db/storage"
	"a.yandex-team.ru/billing/hot/scheduler/pkg/storage/template"
	bconfig "a.yandex-team.ru/billing/library/go/billingo/pkg/config"
	bsql "a.yandex-team.ru/billing/library/go/billingo/pkg/storage/sql"
)

func setupContext() (bsql.Cluster, actions.Actions, func(), error) {
	loader, _ := bconfig.PrepareLoader()
	config := core.Config{}

	ctx, cancel := context.WithTimeout(context.Background(), time.Second)
	defer cancel()

	err := loader.Load(ctx, &config)
	if err != nil {
		return nil, nil, nil, err
	}
	config.Storage.ReconnectRetries = 10

	st := db.NewStorage(config.Storage)
	store := storageimpl.NewStorageImpl(st.Cluster, &template.Template{})
	acts := NewActions(store)
	err = st.Connect(ctx)
	if err != nil {
		return nil, nil, nil, err
	}

	return st.Cluster, acts, func() {
		ctx, cancel := context.WithTimeout(context.Background(), time.Second)
		defer cancel()
		_ = st.Disconnect(ctx)
	}, nil
}

type ActionTestSuite struct {
	suite.Suite
	store   bsql.Cluster
	actions actions.Actions
	cleanup func()
}

func (s *ActionTestSuite) SetupSuite() {
	var err error

	s.store, s.actions, s.cleanup, err = setupContext()
	s.Require().NoError(err)
}

func (s *ActionTestSuite) TearDownSuite() {
	if s.cleanup != nil {
		s.cleanup()
	}
}
