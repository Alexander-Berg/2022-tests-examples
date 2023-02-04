package oebsgate

import (
	"context"
	"database/sql"
	"errors"
	"os"
	"testing"

	"github.com/shopspring/decimal"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/suite"

	pc "a.yandex-team.ru/billing/hot/payout/internal/context"
	"a.yandex-team.ru/billing/hot/payout/internal/core"
	"a.yandex-team.ru/billing/hot/payout/internal/interactions"
	"a.yandex-team.ru/billing/hot/payout/internal/logbroker"
	"a.yandex-team.ru/billing/hot/payout/internal/netting"
	"a.yandex-team.ru/billing/hot/payout/internal/notifier"
	"a.yandex-team.ru/billing/hot/payout/internal/payout"
	"a.yandex-team.ru/billing/hot/payout/internal/request"
	"a.yandex-team.ru/billing/hot/payout/internal/storage/db"
	bc "a.yandex-team.ru/billing/library/go/billingo/pkg/config"
	"a.yandex-team.ru/billing/library/go/billingo/pkg/extracontext"
	interactions2 "a.yandex-team.ru/billing/library/go/billingo/pkg/interactions"
	"a.yandex-team.ru/billing/library/go/billingo/pkg/interactions/accounts"
	bt "a.yandex-team.ru/billing/library/go/billingo/pkg/testing"
	"a.yandex-team.ru/billing/library/go/billingo/pkg/xlog"
	"a.yandex-team.ru/library/go/core/log"
	zaplog "a.yandex-team.ru/library/go/core/log/zap"
	"a.yandex-team.ru/library/go/core/metrics/solomon"
)

const (
	defaultServiceID = 7
)

var (
	ErrOooops = errors.New("some error")
)

// setupContext конструирует контекст для запуска тестов
func setupContext() (*pc.PayoutContext, func(), error) {
	loader, _ := bc.PrepareLoader()
	logger, _ := zaplog.NewDeployLogger(log.InfoLevel)
	xlog.SetGlobalLogger(logger)
	config := core.Config{}

	//config.OEBSGate.NewPayouts.Endpoint = "127.0.0.1:" + os.Getenv("LOGBROKER_PORT")
	//config.OEBSGate.NewPayouts.Topic = "new-payouts"

	ctx := pc.PayoutContext{
		Context: extracontext.NewWithParent(context.Background()),
		Clients: &interactions.Clients{},
		Config:  &config,
	}

	err := os.Setenv("PAYOUT_OEBS_GATE_INTERVAL", "1m")
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

// OEBSGateTestSuite набор тестов для OEBS Gate
type OEBSGateTestSuite struct {
	suite.Suite
	ctx     *pc.PayoutContext
	cleanup func()
	pDB     *payout.DB
}

// SetupSuite вызывается перед началом тестирования
func (s *OEBSGateTestSuite) SetupSuite() {
	var err error

	s.ctx, s.cleanup, err = setupContext()
	if err != nil {
		s.T().Fatal(err)
	}
	s.pDB = &payout.DB{
		Backend: s.ctx.Storage.Backend,
	}
}

// TearDownSuite вызывается после тестирования
func (s *OEBSGateTestSuite) TearDownSuite() {
	if s.cleanup != nil {
		s.cleanup()
	}
}

func (s *OEBSGateTestSuite) Tx() *sql.Tx {
	tx, err := s.ctx.Storage.Backend.BeginTx(s.ctx)
	assert.NoError(s.T(), err)
	return tx
}

// TestOEBSGateTestSuite запуск всех тестов для OEBS Gate
func TestOEBSGateTestSuite(t *testing.T) {
	suite.Run(t, new(OEBSGateTestSuite))
}

// утилиты

// createRandomPayout создает случайную выплату
func (s *OEBSGateTestSuite) createRandomPayout() (*payout.Payout, error) {
	return payout.Create(s.ctx, &payout.NewPayout{
		ExternalID: bt.RandS(30),
		ServiceID:  defaultServiceID,
		ContractID: bt.RandN64(),
		Amount:     decimal.RequireFromString("124.666"),
		Currency:   "RUB",
		Namespace:  notifier.DefaultNamespace,
	})
}

// createRandomPayoutWithRequest создает случайную выплату с заявкой
func (s *OEBSGateTestSuite) createRandomPayoutWithRequest() (*payout.Payout, *request.Request, error) {
	return s.createRandomPayoutWithRequestAmount("124.66")
}

// createRandomPayoutWithRequest создает случайную выплату с заявкой
func (s *OEBSGateTestSuite) createRandomPayoutWithRequestAmount(amount string) (*payout.Payout, *request.Request, error) {
	externalID := bt.RandS(50)
	clientID := bt.RandN64()

	nr := request.NewRequest{
		ExternalID: externalID,
		ClientID:   clientID,
	}

	r, err := request.Create(s.ctx, &nr)
	if err != nil {
		return nil, nil, err
	}

	p, err := payout.Create(s.ctx, &payout.NewPayout{
		ExternalID: bt.RandS(30),
		ServiceID:  defaultServiceID,
		ContractID: bt.RandN64(),
		Amount:     decimal.RequireFromString(amount),
		Currency:   "RUB",
		RequestID:  &r.ID,
		ClientID:   clientID,
		Namespace:  notifier.DefaultNamespace,
	})
	if err != nil {
		return nil, nil, err
	}

	return p, r, nil
}

// createRandomPayoutWithARD создает случайную выплату с заполненными полями от АРД
func (s *OEBSGateTestSuite) createRandomPayoutWithARD() (*payout.Payout, *request.Request, error) {
	externalID := bt.RandS(50)
	clientID := bt.RandN64()

	nr := request.NewRequest{
		ExternalID: externalID,
		ClientID:   clientID,
	}

	r, err := request.Create(s.ctx, &nr)
	if err != nil {
		return nil, nil, err
	}

	p, err := payout.Create(s.ctx, &payout.NewPayout{
		ExternalID: bt.RandS(30),
		ServiceID:  defaultServiceID,
		ContractID: bt.RandN64(),
		Amount:     decimal.RequireFromString("124.66"),
		Currency:   "RUB",
		RequestID:  &r.ID,
		Namespace:  notifier.DefaultNamespace,
	})
	if err != nil {
		return nil, nil, err
	}

	batchID := bt.RandN64()

	// имитируем АРД-ответ, что выплата ушла в АРД
	update := core.UpdateDesc{
		{Name: payout.StatusCol, Value: payout.StatusConfirmed},
		{Name: payout.BatchIDCol, Value: batchID},
	}
	if err := payout.UpdateX(s.ctx, p.ID, update); err != nil {
		return nil, nil, err
	}

	// т.к. ошибки нет, то в БД такие же значения
	// чтобы не делать еще один запрос, просто обновляем значения в структуре
	p.OEBSBatchID = batchID
	p.Status = payout.StatusConfirmed

	return p, r, nil
}

// createRandomPayoutWithARD создает случайную выплату с заполненными полями от АРД
func (s *OEBSGateTestSuite) createRandomNettingForPayout(p *payout.Payout, status string) (*netting.Netting, error) {

	tx, err := s.ctx.Storage.Backend.BeginTx(s.ctx)
	if err != nil {
		return nil, err
	}
	defer func() {
		_ = tx.Commit()
	}()

	externalID := bt.RandS(50)
	n, err := netting.CreateTx(s.ctx, tx, &netting.NewNetting{
		PayoutID:   p.ID,
		ExtID:      externalID,
		ClientID:   p.ClientID,
		ContractID: p.ContractID,
		ServiceID:  p.ServiceID,
	})
	if err != nil {
		return nil, err
	}

	if status != netting.StatusNew {
		where := core.WhereDesc{
			{Name: netting.IDCol, Value: n.ID},
		}
		update := core.UpdateDesc{
			{Name: netting.StatusCol, Value: status},
		}
		if err := netting.UpdateTx(s.ctx, tx, where, update); err != nil {
			return nil, err
		}
	}

	return n, err
}

func (s *OEBSGateTestSuite) createNotifier() (*notifier.LBNotifier, *logbroker.MockLogBrokerWriter, error) {
	fakeProducer := &logbroker.MockLogBrokerWriter{Messages: nil}
	writers := map[string]notifier.LogBrokerProducer{notifier.DefaultNamespace: fakeProducer}
	lbn, err := notifier.NewLBNotifier(nil, nil, writers)
	return lbn, fakeProducer, err
}

// SetProcessDryRun выставляет значение переменной ProcessDryRun в конфиге
func (s *OEBSGateTestSuite) SetProcessDryRun(val bool) {
	s.ctx.Config.ProcessDryRun = val
}

// SetAllowedNs добавляет срез ID разрешенных клиентов в переменную конфига
func (s *OEBSGateTestSuite) SetAllowedNs(ns map[string][]int64) {
	s.ctx.Config.AllowedNs = ns
}

func (s *OEBSGateTestSuite) setClients() error {
	registry := solomon.NewRegistry(solomon.NewRegistryOpts())
	config := s.ctx.Config
	baseURL := os.Getenv("ACCOUNTS_BASE_URL")
	config.Clients.Accounts = accounts.Config{Transport: interactions2.Config{BaseURL: baseURL}}

	clients, err := interactions.NewClients(config, nil, registry, MockCPFWriter{})
	if err != nil {
		return nil
	}
	s.ctx.Clients = clients

	return nil
}
