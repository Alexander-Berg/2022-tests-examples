package tasks

import (
	"context"
	"errors"
	"testing"
	"time"

	"github.com/stretchr/testify/suite"

	pc "a.yandex-team.ru/billing/hot/payout/internal/context"
	"a.yandex-team.ru/billing/hot/payout/internal/core"
	"a.yandex-team.ru/billing/hot/payout/internal/interactions"
	"a.yandex-team.ru/billing/hot/payout/internal/storage/db"
	tc "a.yandex-team.ru/billing/hot/payout/internal/tasks/config"
	bconfig "a.yandex-team.ru/billing/library/go/billingo/pkg/config"
	"a.yandex-team.ru/billing/library/go/billingo/pkg/extracontext"
	"a.yandex-team.ru/billing/library/go/billingo/pkg/xlog"
	"a.yandex-team.ru/library/go/core/log"
	zaplog "a.yandex-team.ru/library/go/core/log/zap"
)

var (
	ErrOooops = errors.New("some error")
)

// setupContext конструирует контекст для запуска тестов
func setupContext() (*pc.PayoutContext, func(), error) {
	loader, _ := bconfig.PrepareLoader()
	config := core.Config{}

	logger, _ := zaplog.NewDeployLogger(log.DebugLevel)
	xlog.SetGlobalLogger(logger)

	ctx := pc.PayoutContext{
		Context: extracontext.NewWithParent(context.Background()),
		Clients: &interactions.Clients{},
	}
	err := loader.Load(ctx, &config)
	if err != nil {
		return nil, nil, err
	}
	config.Storage.ReconnectRetries = 10

	config.Tasks = tc.Config{
		CleanRequests: tc.CleanRequestsConfig{
			Interval:      "5m",
			IntervalValue: 5 * time.Minute,
			OldKeepDays:   3,
		},
		PartitionPayout: tc.PartitionPayoutConfig{
			Interval:      "5m",
			IntervalValue: 5 * time.Minute,
			NextPeriods:   30,
			OldKeepDays:   -1,
		},
		PartitionCpf: tc.PartitionCPFConfig{
			Interval:      "5m",
			IntervalValue: 5 * time.Minute,
			NextPeriods:   30,
			OldKeepDays:   -1,
		},
	}
	config.EmulateOEBS = true

	ctx.Config = &config

	ctx.Storage = db.NewStorage(config.Storage)
	err = ctx.Storage.Connect(ctx)
	if err != nil {
		return nil, nil, err
	}

	return &ctx, func() {
		_ = ctx.Storage.Disconnect(ctx)
	}, nil
}

// TasksTestSuite набор тестов для регулярных задач
type TasksTestSuite struct {
	suite.Suite
	ctx     *pc.PayoutContext
	cleanup func()
}

// SetupSuite вызывается перед началом тестирования
func (s *TasksTestSuite) SetupSuite() {
	var err error

	s.ctx, s.cleanup, err = setupContext()
	if err != nil {
		s.T().Fatal(err)
	}
}

// TearDownSuite вызывается после тестирования
func (s *TasksTestSuite) TearDownSuite() {
	if s.cleanup != nil {
		s.cleanup()
	}
}

// TestProcessorTestSuite запуск всех тестов для Processor
func TestProcessorTestSuite(t *testing.T) {
	suite.Run(t, new(TasksTestSuite))
}
