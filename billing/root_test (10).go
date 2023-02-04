package actions

import (
	"context"
	"io"
	"io/fs"
	"os"
	"path/filepath"
	"time"

	"github.com/stretchr/testify/suite"
	"github.com/ydb-platform/ydb-go-sdk/v3/table"

	"a.yandex-team.ru/billing/hot/diod/pkg/commands"
	"a.yandex-team.ru/billing/hot/diod/pkg/core"
	"a.yandex-team.ru/billing/hot/diod/pkg/storage/db"
	bconfig "a.yandex-team.ru/billing/library/go/billingo/pkg/config"
	"a.yandex-team.ru/billing/library/go/billingo/pkg/xlog"
	"a.yandex-team.ru/library/go/core/log"
	"a.yandex-team.ru/library/go/test/yatest"
)

func setupContext() (*commands.Repository, func(), error) {
	loader, _ := bconfig.PrepareLoader()
	config := core.Config{}

	ctx, cancel := context.WithTimeout(context.Background(), 15*time.Second)
	defer cancel()

	repo := &commands.Repository{}

	err := loader.Load(ctx, &config)
	if err != nil {
		return nil, nil, err
	}

	config.Storage.ConnectionString = os.Getenv("YDB_CONNECTION_STRING")

	repo.Storage = db.NewStorage(config.Storage, nil, nil)
	err = repo.Storage.Connect(ctx)
	if err != nil {
		return nil, nil, err
	}

	return repo, func() {
		_ = repo.Storage.Disconnect(ctx)
	}, nil
}

func applyMigrations(repo *commands.Repository) error {
	ctx, cancel := context.WithTimeout(context.Background(), 1*time.Minute)

	defer cancel()

	return filepath.WalkDir(yatest.SourcePath("billing/hot/diod/ydb/migrations/"), func(path string, d fs.DirEntry, err error) error {

		if err != nil {
			return err
		}

		// skip dirs, including root.
		if d.IsDir() {
			return nil
		}

		migrationsFile, err := os.Open(path)

		if err != nil {
			return err
		}

		query, err := io.ReadAll(migrationsFile)

		if err != nil {
			return err
		}

		return repo.Storage.Cluster.Do(
			ctx,
			func(ctx context.Context, session table.Session) error {
				return session.ExecuteSchemeQuery(
					ctx,
					string(query),
				)
			},
		)
	})
}

type ActionTestSuite struct {
	suite.Suite
	repo    *commands.Repository
	cleanup func()
}

func (s *ActionTestSuite) SetupSuite() {
	var err error

	logger, _, _ := xlog.NewDeployLogger(log.DebugLevel)

	xlog.SetGlobalLogger(logger)

	s.repo, s.cleanup, err = setupContext()
	s.Require().NoError(err)

	err = applyMigrations(s.repo)
	s.Require().NoError(err)
}

func (s *ActionTestSuite) TearDownSuite() {
	if s.cleanup != nil {
		s.cleanup()
	}
}
