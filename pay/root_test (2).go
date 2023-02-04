package actions

import (
	"context"
	"fmt"
	"io"
	"io/fs"
	"os"
	"path/filepath"
	"strings"
	"time"

	bconfig "a.yandex-team.ru/billing/library/go/billingo/pkg/config"
	"a.yandex-team.ru/library/go/test/yatest"
	"github.com/stretchr/testify/suite"
	"github.com/ydb-platform/ydb-go-sdk/v3/table"

	"a.yandex-team.ru/payplatform/fes/fes/pkg/commands"
	"a.yandex-team.ru/payplatform/fes/fes/pkg/core"
	"a.yandex-team.ru/payplatform/fes/fes/pkg/sqs"
	"a.yandex-team.ru/payplatform/fes/fes/pkg/storage/db"
)

func setupContext() (*commands.Repository, func(), error) {

	loader, err := bconfig.PrepareLoader(yatest.SourcePath("payplatform/fes/fes/config/dev/dev.yaml"))
	if err != nil {
		return nil, nil, err
	}
	config := core.Config{}

	ctx, cancel := context.WithTimeout(context.Background(), time.Second)
	defer cancel()

	repo := &commands.Repository{}

	err = loader.Load(ctx, &config)
	if err != nil {
		return nil, nil, err
	}

	config.Storage.ConnectionString = os.Getenv("YDB_CONNECTION_STRING")

	repo.Storage = db.NewStorage(config.Storage, nil, nil)
	repo.Config = &config
	err = repo.Storage.Connect(ctx)
	if err != nil {
		return nil, nil, err
	}

	awsKeys := map[string]string{"AWS_ACCESS_KEY_ID": "my_user", "AWS_SESSION_TOKEN": "", "AWS_SECRET_ACCESS_KEY": "unused"}
	for key, value := range awsKeys {
		if err = os.Setenv(key, value); err != nil {
			return nil, nil, err
		}
	}

	SQSPort, ok := os.LookupEnv("SQS_PORT")
	if !ok {
		return nil, nil, fmt.Errorf("environment variable SQS_PORT is not set")
	}
	repo.Config.SQS.Endpoint = strings.Replace(repo.Config.SQS.Endpoint, "8771", SQSPort, 1)

	repo.SQS, err = sqs.NewSQS(repo.Config.SQS)

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

	ctx, cancel := context.WithTimeout(context.Background(), 10*time.Second)

	defer cancel()

	queryList := []string{}

	err = filepath.WalkDir(yatest.SourcePath("payplatform/fes/fes/ydb/migrations/"), func(path string, d fs.DirEntry, err error) error {

		if err != nil {
			return err
		}

		// skip dirs, including root.
		if d.IsDir() {
			return nil
		}
		if strings.HasSuffix(path, "down.sql") { // skip roll back migrations
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

		queryList = append(queryList, string(query))

		return nil
	})

	s.Require().NoError(err)

	query := strings.Join(queryList, "\n")

	err = s.repo.Storage.Cluster.Do(
		ctx,
		func(ctx context.Context, session table.Session) error {
			return session.ExecuteSchemeQuery(
				ctx,
				query,
			)
		},
	)

	s.Require().NoError(err)
}

func (s *ActionTestSuite) TearDownSuite() {
	if s.cleanup != nil {
		s.cleanup()
	}
}
