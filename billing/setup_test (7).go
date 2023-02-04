package handlers

import (
	"context"
	"testing"

	"github.com/stretchr/testify/suite"

	pc "a.yandex-team.ru/billing/hot/payout/internal/context"
	"a.yandex-team.ru/billing/hot/payout/internal/core"
	"a.yandex-team.ru/billing/hot/payout/internal/interactions"
	oc "a.yandex-team.ru/billing/hot/payout/internal/oebsgate/config"
	"a.yandex-team.ru/billing/hot/payout/internal/storage/db"
	bconfig "a.yandex-team.ru/billing/library/go/billingo/pkg/config"
	"a.yandex-team.ru/billing/library/go/billingo/pkg/extracontext"
	"a.yandex-team.ru/billing/library/go/billingo/pkg/xlog"
	"a.yandex-team.ru/library/go/core/log"
	zaplog "a.yandex-team.ru/library/go/core/log/zap"
)

const (
	TestNamespace = "tests"
	CpfOnly       = "cpf_only"
)

// setupContext конструирует контекст для запуска тестов
func setupContext() (pc.PayoutContext, func(), error) {
	loader, _ := bconfig.PrepareLoader()
	config := core.Config{OEBSGate: map[string]*oc.NamespaceConfig{
		TestNamespace: &oc.NamespaceConfig{},
		CpfOnly:       nil,
	}}

	logger, _ := zaplog.NewDeployLogger(log.DebugLevel)
	xlog.SetGlobalLogger(logger)

	ctx := pc.PayoutContext{
		Context: extracontext.NewWithParent(context.Background()),
		Clients: &interactions.Clients{},
		Config:  &config,
	}

	err := loader.Load(ctx, &config)
	if err != nil {
		return ctx, nil, err
	}
	config.Storage.ReconnectRetries = 10

	ctx.Storage = db.NewStorage(config.Storage)
	err = ctx.Storage.Connect(ctx)
	if err != nil {
		return ctx, nil, err
	}

	return ctx, func() {
		_ = ctx.Storage.Disconnect(ctx)
	}, nil
}

// HandlersV1TestSuite набор тестов для API
type HandlersV1TestSuite struct {
	suite.Suite
	ctx     pc.PayoutContext
	cleanup func()
}

// SetupSuite вызывается перед началом тестирования
func (s *HandlersV1TestSuite) SetupSuite() {
	var err error

	s.ctx, s.cleanup, err = setupContext()
	if err != nil {
		s.T().Fatal(err)
	}
}

// TearDownSuite вызывается после тестирования
func (s *HandlersV1TestSuite) TearDownSuite() {
	if s.cleanup != nil {
		s.cleanup()
	}
}

func TestHandlersV1TestSuite(t *testing.T) {
	suite.Run(t, new(HandlersV1TestSuite))
}
