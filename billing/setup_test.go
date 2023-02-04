package accrualer

import (
	"context"
	"os"
	"path"
	"path/filepath"
	"testing"
	"time"

	"github.com/stretchr/testify/require"
	"github.com/stretchr/testify/suite"

	aconf "a.yandex-team.ru/billing/hot/accrualer/internal/config"
	"a.yandex-team.ru/billing/hot/accrualer/internal/mocks"
	"a.yandex-team.ru/billing/hot/accrualer/internal/transforms"
	bconfig "a.yandex-team.ru/billing/library/go/billingo/pkg/config"
	"a.yandex-team.ru/billing/library/go/billingo/pkg/xlog"
	"a.yandex-team.ru/library/go/core/log"
	zaplog "a.yandex-team.ru/library/go/core/log/zap"
	"a.yandex-team.ru/library/go/test/yatest"
)

// setupContext конструирует контекст для запуска тестов
func setupContext() (*context.Context, *time.Location, *aconf.Config, *aconf.LogicConfig, aconf.Formats, func(), error) {
	logger, _ := zaplog.NewDeployLogger(log.DebugLevel)
	xlog.SetGlobalLogger(logger)

	loc, err := time.LoadLocation("Europe/Moscow")
	if err != nil {
		return nil, nil, nil, nil, nil, nil, err
	}

	transforms.RegisterTransforms(loc)

	rootPath, err := filepath.Abs(yatest.SourcePath("billing/hot/accrualer/config"))
	if err != nil {
		return nil, nil, nil, nil, nil, nil, err
	}

	ctx := context.Background()

	configLoader, _ := bconfig.PrepareLoaderWithEnvPrefix("accrualer", path.Join(rootPath, "dev", "accrualer.yaml"))
	config, err := aconf.Parse(ctx, configLoader, logger)
	if err != nil {
		return nil, nil, nil, nil, nil, nil, err
	}

	configLoader, _ = bconfig.PrepareLoaderWithEnvPrefix("accrualer", path.Join(rootPath, "dev", "logic.yaml"))
	logicConfig, err := aconf.ParseLogic(ctx, configLoader, logger)
	if err != nil {
		return nil, nil, nil, nil, nil, nil, err
	}

	formatsPath := path.Join(rootPath, "dev", "formats")
	formatsConf, err := aconf.ParseFormatsConfig(ctx, os.DirFS(formatsPath), ".")
	xlog.Info(ctx, "fail", log.Error(err))
	if err != nil {
		return nil, nil, nil, nil, nil, nil, err
	}

	return &ctx, loc, config, logicConfig, formatsConf, func() {}, nil
}

// AccrualerTestSuite набор тестов для регулярных задач
type AccrualerTestSuite struct {
	suite.Suite
	ctx *context.Context

	loc       *time.Location
	config    *aconf.Config
	logicConf *aconf.LogicConfig
	formats   aconf.Formats

	cleanup func()
}

// SetupTest вызывается перед началом тестирования
func (s *AccrualerTestSuite) SetupSuite() {
	var err error

	s.ctx, s.loc, s.config, s.logicConf, s.formats, s.cleanup, err = setupContext()
	if err != nil {
		s.T().Fatal(err)
	}
}

// TearDownSuite вызывается после тестирования
func (s *AccrualerTestSuite) TearDownSuite() {
	if s.cleanup != nil {
		s.cleanup()
	}
}

func (s *AccrualerTestSuite) newServer() *Server {
	server, _, err := NewServer(*s.ctx, s.loc, s.config, s.logicConf, s.formats)
	require.NoError(s.T(), err)
	return server
}

// TestProcessorTestSuite запуск всех тестов для Processor
func TestProcessorTestSuite(t *testing.T) {
	suite.Run(t, new(AccrualerTestSuite))
}

func mockProcProviders(s *Server) error {
	for config := range s.producers {
		xlog.Info(s.ctx, "producer", log.Any("config", config))
		producer := &mocks.MockLogBrokerProducer{}
		s.writers.Writers[config] = producer
	}
	return s.startProducers(s.ctx)
}
