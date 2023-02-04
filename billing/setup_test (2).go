package providers

import (
	"context"
	"testing"

	"github.com/stretchr/testify/suite"

	aconfig "a.yandex-team.ru/billing/hot/accrualer/internal/config"
	bconfig "a.yandex-team.ru/billing/library/go/billingo/pkg/config"
	"a.yandex-team.ru/billing/library/go/billingo/pkg/xlog"
	"a.yandex-team.ru/library/go/core/log"
	zaplog "a.yandex-team.ru/library/go/core/log/zap"
)

func updateConfigForTest(c *aconfig.Config) *aconfig.Config {

	return c
}

// setupContext конструирует контекст для запуска тестов
func setupContext() (*context.Context, *aconfig.Config, *aconfig.LogicConfig, func(), error) {
	logger, _ := zaplog.NewDeployLogger(log.DebugLevel)
	xlog.SetGlobalLogger(logger)

	loader, _ := bconfig.PrepareLoader()
	ctx := context.Background()
	config, err := aconfig.Parse(ctx, loader, logger)
	if err != nil {
		return nil, nil, nil, nil, err
	}
	updateConfigForTest(config)

	return &ctx, config, nil, func() {}, nil
}

// ProvidersTestSuite набор тестов для регулярных задач
type ProvidersTestSuite struct {
	suite.Suite
	ctx       *context.Context
	config    *aconfig.Config
	logicConf *aconfig.LogicConfig
	cleanup   func()
}

// SetupTest вызывается перед началом тестирования
func (s *ProvidersTestSuite) SetupTest() {
	var err error

	s.ctx, s.config, s.logicConf, s.cleanup, err = setupContext()
	if err != nil {
		s.T().Fatal(err)
	}
}

// TearDownSuite вызывается после тестирования
func (s *ProvidersTestSuite) TearDownTest() {
	if s.cleanup != nil {
		s.cleanup()
	}
}

// TestProcessorTestSuite запуск всех тестов для Processor
func TestProcessorTestSuite(t *testing.T) {
	suite.Run(t, new(ProvidersTestSuite))
}
