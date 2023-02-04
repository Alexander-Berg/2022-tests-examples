package actions

import (
	"context"
	"fmt"
	"os"
	"time"

	sq "github.com/Masterminds/squirrel"

	"a.yandex-team.ru/billing/configdepot/pkg/commands"
	"a.yandex-team.ru/billing/configdepot/pkg/core"
	"a.yandex-team.ru/billing/configdepot/pkg/core/entities"
	"a.yandex-team.ru/billing/configdepot/pkg/storage/db"
	"a.yandex-team.ru/billing/configdepot/pkg/tests"
	"a.yandex-team.ru/billing/library/go/billingo/pkg/storage/sql/backends/pg"
)

type BaseActionTestSuite struct {
	tests.BaseTestSuite
	ctx        context.Context
	cleanUp    func() error
	repository commands.Repository
}

func (s *BaseActionTestSuite) setupContext() (context.Context, func() error, error) {
	ctx := context.Background()

	config := core.Config{
		Storage: db.StorageConfig{
			DSN: os.Getenv("CONFIGDEPOT_DB_DSN"),
		},
	}
	config.Tasks.Interval = 1 * time.Minute

	s.repository.Storage = db.NewStorage(config.Storage)
	err := s.repository.Storage.Connect(ctx)

	s.repository.Config = &config

	cleanUpFunc := func() error {
		return s.repository.Storage.Disconnect(ctx)
	}

	return ctx, cleanUpFunc, err
}

func (s *BaseActionTestSuite) SetupTest() {
	ctx, cleanUp, err := s.setupContext()
	s.Require().NoError(err)
	s.ctx = ctx
	s.cleanUp = cleanUp
	s.GenerateSampleTimes(20)

	s.Require().NoError(s.truncateAll())
}

func (s *BaseActionTestSuite) TearDownSuite() {
	if s.cleanUp != nil {
		s.Require().NoError(s.cleanUp())
	}
}

func (s *BaseActionTestSuite) createTestDeployRow(component string, revision int, status string, startAt *time.Time) {
	_, err := s.repository.Storage.DeployMapper.Create(s.ctx, db.DeployCreateParams{
		Component: component, Revision: revision, Status: &status, StartAt: startAt,
	})
	s.Require().NoError(err)
}

func (s *BaseActionTestSuite) createTestConfigRow(
	deployID int, configurationKey string, revision int, context string,
) {
	_, err := s.repository.Storage.ConfigMapper.Create(s.ctx, db.ConfigCreateParams{
		DeployID: deployID, ConfigurationKey: configurationKey, Revision: revision, Context: &context,
	})
	s.Require().NoError(err)
}

func (s *BaseActionTestSuite) createTestRows() {
	s.createTestDeployRow("processor", 12, entities.StatusDone, &s.SampleTimes[0])
	s.createTestDeployRow("processor", 13, entities.StatusDone, &s.SampleTimes[1])
	s.createTestDeployRow("processor", 14, entities.StatusNew, &s.SampleTimes[2])
	s.createTestDeployRow("mediator", 8, entities.StatusDone, &s.SampleTimes[3])
	s.createTestDeployRow("mediator", 9, entities.StatusNew, &s.SampleTimes[4])
	s.createTestDeployRow("faas", 5, entities.StatusInProgress, &s.SampleTimes[5])

	s.createTestConfigRow(1, "bnpl", 1, "{ android: 'iphone' }")
	s.createTestConfigRow(4, "bnpl_income", 4, "{ mars: 'snickers' }")
	s.createTestConfigRow(6, "music", 2, "{ foo: 'bar' }")
}

func (s *BaseActionTestSuite) truncateAll() error {
	tableNames := []string{"configdepot.t_config", "configdepot.t_deploy"}

	for _, tableName := range tableNames {
		query := sq.Expr(fmt.Sprintf("TRUNCATE TABLE %s RESTART IDENTITY CASCADE", tableName))
		rows, err := s.repository.Storage.Cluster.GetDatabase(s.ctx, pg.Master).QuerySq(s.ctx, query)
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
