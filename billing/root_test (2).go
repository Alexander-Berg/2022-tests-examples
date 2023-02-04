package db

import (
	"context"
	"fmt"
	"os"
	"time"

	sq "github.com/Masterminds/squirrel"

	"a.yandex-team.ru/billing/configdepot/pkg/core/entities"
	"a.yandex-team.ru/billing/configdepot/pkg/tests"
	"a.yandex-team.ru/billing/library/go/billingo/pkg/storage/sql/backends/pg"
)

type BaseMapperTestSuite struct {
	tests.BaseTestSuite
	storage *Storage
	ctx     context.Context
	cleanup func() error
}

func (s *BaseMapperTestSuite) setupContext() (context.Context, func() error, error) {
	ctx := context.Background()

	config := StorageConfig{
		DSN: os.Getenv("CONFIGDEPOT_DB_DSN"),
	}

	s.storage = NewStorage(config)
	err := s.storage.Connect(ctx)

	cleanUpFunc := func() error {
		return s.storage.Disconnect(ctx)
	}

	return ctx, cleanUpFunc, err
}

func (s *BaseMapperTestSuite) SetupTest() {
	ctx, cleanup, err := s.setupContext()
	s.ctx = ctx
	s.cleanup = cleanup
	s.Require().NoError(err)
	s.GenerateSampleTimes(20)

	s.Require().NoError(s.truncateAll())
}

func (s *BaseMapperTestSuite) TearDownSuite() {
	if s.cleanup != nil {
		s.Require().NoError(s.cleanup())
	}
}

func (s *BaseMapperTestSuite) createTestDeployRow(component string, revision int, startAt time.Time, status string) {
	_, err := s.storage.DeployMapper.Create(s.ctx, DeployCreateParams{
		Component: component, Revision: revision, StartAt: &startAt, Status: &status,
	})
	s.Require().NoError(err)
}

func (s *BaseMapperTestSuite) createTestConfigRow(deployID, revision int, configurationKey, context string) {
	_, err := s.storage.ConfigMapper.Create(s.ctx, ConfigCreateParams{
		DeployID: deployID, ConfigurationKey: configurationKey, Revision: revision, Context: &context,
	})
	s.Require().NoError(err)
}

func (s *BaseMapperTestSuite) truncateAll() error {
	tableNames := []string{entities.TableConfigFull, entities.TableDeployFull}

	for _, tableName := range tableNames {
		query := sq.Expr(fmt.Sprintf("TRUNCATE TABLE %s RESTART IDENTITY CASCADE", tableName))
		rows, err := s.storage.Cluster.GetDatabase(s.ctx, pg.Master).QuerySq(s.ctx, query)
		if err != nil {
			return err
		}
		if err := rows.Err(); err != nil {
			return err
		}

		err = rows.Close()
		if err != nil {
			return err
		}
	}

	return nil
}
