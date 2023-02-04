package request

import (
	"context"
	"os"
	"testing"

	"github.com/stretchr/testify/suite"

	pc "a.yandex-team.ru/billing/hot/payout/internal/context"
	"a.yandex-team.ru/billing/hot/payout/internal/core"
	"a.yandex-team.ru/billing/hot/payout/internal/interactions"
	"a.yandex-team.ru/billing/hot/payout/internal/notifier"
	"a.yandex-team.ru/billing/hot/payout/internal/storage/db"
	bconfig "a.yandex-team.ru/billing/library/go/billingo/pkg/config"
	"a.yandex-team.ru/billing/library/go/billingo/pkg/extracontext"
	"a.yandex-team.ru/billing/library/go/billingo/pkg/xlog"
	"a.yandex-team.ru/library/go/core/log"
	zaplog "a.yandex-team.ru/library/go/core/log/zap"
)

const (
	defaultServiceID = 124
)

// setupContext конструирует контекст для запуска тестов
func setupContext() (*pc.PayoutContext, func(), error) {
	loader, _ := bconfig.PrepareLoader()
	logger, _ := zaplog.NewDeployLogger(log.InfoLevel)
	xlog.SetGlobalLogger(logger)
	config := core.Config{}

	ctx := pc.PayoutContext{
		Context: extracontext.NewWithParent(context.Background()),
		Clients: &interactions.Clients{},
		Config:  &config,
	}
	err := os.Setenv("PAYOUT_REQ_GATE_INTERVAL", "1m")
	if err != nil {
		return nil, nil, err
	}

	err = loader.Load(ctx, &config)
	if err != nil {
		return nil, nil, err
	}
	config.Storage.ReconnectRetries = 10

	ctx.Storage = db.NewStorage(config.Storage)
	err = ctx.Storage.Connect(ctx)
	if err != nil {
		return nil, nil, err
	}

	return &ctx, func() {
		_ = ctx.Storage.Disconnect(ctx)
	}, nil
}

// HandlersV1TestSuite набор тестов для API
type ProcessorTestSuite struct {
	suite.Suite
	ctx     *pc.PayoutContext
	cleanup func()
}

// SetupTest вызывается перед началом тестирования
func (s *ProcessorTestSuite) SetupTest() {
	var err error

	s.ctx, s.cleanup, err = setupContext()
	if err != nil {
		s.T().Fatal(err)
	}
}

// TearDownTest вызывается после тестирования
func (s *ProcessorTestSuite) TearDownTest() {
	if s.cleanup != nil {
		s.cleanup()
	}
}

// TestProcessorTestSuite запуск всех тестов для Processor
func TestProcessorTestSuite(t *testing.T) {
	suite.Run(t, new(ProcessorTestSuite))
}

func (s *ProcessorTestSuite) createNotifier() (*notifier.LBNotifier, *MockLogBrokerProducer, error) {
	fakeProducer := &MockLogBrokerProducer{messages: nil}
	writers := map[string]notifier.LogBrokerProducer{notifier.DefaultNamespace: fakeProducer}
	lbn, err := notifier.NewLBNotifier(nil, nil, writers)
	return lbn, fakeProducer, err
}

// SetAllowedClients добавляет срез ID разрешенных клиентов в переменную конфига
func (s *ProcessorTestSuite) SetAllowedClients() {
	s.ctx.Config.AllowedClients = []int64{84773393, 92698540}
}
