package accrualer

import (
	"context"
	"embed"
	"encoding/json"
	"path"
	"strings"
	"testing"
	"time"

	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"

	aconf "a.yandex-team.ru/billing/hot/accrualer/internal/config"
	"a.yandex-team.ru/billing/hot/accrualer/internal/entities"
	"a.yandex-team.ru/billing/hot/accrualer/internal/mocks"
	"a.yandex-team.ru/billing/hot/accrualer/internal/providers"
	"a.yandex-team.ru/billing/library/go/billingo/pkg/xlog"
	"a.yandex-team.ru/kikimr/public/sdk/go/persqueue"
	"a.yandex-team.ru/library/go/core/log"
)

//go:embed gotest/agency_accruals/*
var agencyAccrualsFS embed.FS

//go:embed gotest/agency_acts/*
var agencyActsFS embed.FS

//go:embed gotest/netting/*
var nettingFS embed.FS

const (
	agencyAccrualsPath  = "gotest/agency_accruals"
	agencyActsPath      = "gotest/agency_acts"
	nettingPath         = "gotest/netting"
	dryRunPath          = "gotest/agency_accruals/dry_run"
	tarifferPayloadPath = "gotest/agency_accruals/tariffer_payload"

	inputPrefix     = "input_"
	resultPrefix    = "result_"
	testPrefix      = "test_"
	spendTestPrefix = "spend_test_"
)

func processEventFile(ctx context.Context, t *testing.T, server *Server, lb *mocks.MockLogBrokerProducer, fs embed.FS, filename string, acc any) any {
	before := len(lb.Messages)
	errors := server.writers.Writers[server.config.Notifier].(*mocks.MockLogBrokerProducer)
	errorCount := len(errors.Messages)
	unknown := server.writers.Writers[server.config.Unknown].(*mocks.MockLogBrokerProducer)
	unknownCount := len(errors.Messages)

	sendEventFile(t, server, fs, filename, server.handleMessageBatch)
	if len(errors.Messages) > errorCount {
		for _, message := range errors.Messages[errorCount:] {
			xlog.Info(ctx, "one more error message", log.ByteString("message", message), log.String("file", filename))
		}
		require.Fail(t, "found new error message; "+filename)
	}
	if len(unknown.Messages) > unknownCount {
		for _, message := range unknown.Messages[unknownCount:] {
			xlog.Info(ctx, "one more unknown message", log.ByteString("message", message), log.String("file", filename))
		}
		require.Fail(t, "found new unknown message; "+filename)
	}

	require.Len(t, lb.Messages, before+1, "failed test file: "+filename)
	xlog.Info(ctx, "message", log.Any("msg", lb.Messages[before]))
	err := json.Unmarshal(lb.Messages[before], &acc)
	require.NoError(t, err, "failed test file: "+filename)
	return acc
}

func sendEventFile(t *testing.T, server *Server, fs embed.FS, filename string, f func(context.Context, persqueue.ReadMessage, persqueue.MessageBatch) error) {
	data, err := fs.ReadFile(filename)
	require.NoError(t, err, "failed test file: "+filename)
	m := persqueue.ReadMessage{
		Data: data,
	}
	b := persqueue.MessageBatch{
		Topic:     "test_unique",
		Partition: uint32(1),
		Messages:  []persqueue.ReadMessage{m},
	}
	if f == nil {
		f = server.handleMessageBatch
	}

	err = f(context.Background(), m, b)
	require.NoError(t, err, "failed test file: "+filename)
}

func checkResultAccrual(t *testing.T, result, processed entities.Accrual, checkEvent bool, testName string) {
	result.MessageID = processed.MessageID
	assert.NotZero(t, processed.MessageDT, "failed test file: "+testName)
	result.MessageDT = processed.MessageDT
	//result.TransactionDT = processed.TransactionDT
	if !checkEvent {
		result.Event = processed.Event
	}

	assert.True(t, result.Amount.Equal(processed.Amount),
		"failed equal Amount test file: %s; %v vs %v",
		testName,
		result.Amount.String(), processed.Amount.String(),
	)
	assert.True(t, result.ReferenceAmount.Equal(processed.ReferenceAmount),
		"failed equal Amount test file: %s; %v vs %v",
		testName,
		result.ReferenceAmount.String(), processed.ReferenceAmount.String(),
	)
	assert.True(t, result.YandexReward.Equal(processed.YandexReward),
		"failed equal Amount test file: %s; %v vs %v",
		testName,
		result.YandexReward.String(), processed.YandexReward.String(),
	)
	result.Amount = processed.Amount
	result.ReferenceAmount = processed.ReferenceAmount
	result.YandexReward = processed.YandexReward

	assert.Equal(t, result, processed, "failed test file: "+testName)
}

func checkResultSpendable(t *testing.T, result, processed entities.SpendableAccrual, checkEvent bool, testName string) {
	assert.Equal(t, result, processed, "failed test file: "+testName)
}

// TestProcessMessages test processing base account events
// get input messages from dir agencyAccrualsPath with prefix inputPrefix
// process it and compare with "canonical" result from file with resultPrefix
func (s *AccrualerTestSuite) TestProcessMessages() {
	server := s.newServer()
	require.NoError(s.T(), mockProcProviders(server))

	was := false
	dirs, err := agencyAccrualsFS.ReadDir(agencyAccrualsPath)
	require.NoError(s.T(), err)
	for _, root := range dirs {
		if !root.IsDir() || !strings.HasPrefix(root.Name(), testPrefix) {
			continue
		}
		xlog.Info(*s.ctx, "run process tests", log.String("dir", root.Name()))

		s.T().Run(root.Name(), func(t *testing.T) {
			was := false
			basePath := path.Join(agencyAccrualsPath, root.Name())
			namespace := strings.TrimPrefix(root.Name(), testPrefix)

			files, err := agencyAccrualsFS.ReadDir(basePath)
			require.NoError(t, err)

			config := s.logicConf.Namespace[namespace].Accruals.Types[aconf.AgencyFull].Topics[providers.DefaultName]
			lb := server.writers.Writers[config].(*mocks.MockLogBrokerProducer)

			for _, data := range files {
				if !strings.HasPrefix(data.Name(), inputPrefix) {
					continue
				}
				xlog.Info(*s.ctx, "test file", log.String("name", data.Name()))
				testName := strings.TrimPrefix(data.Name(), inputPrefix)

				var acc entities.Accrual
				processEventFile(*s.ctx, t, server, lb, agencyAccrualsFS, path.Join(basePath, data.Name()), &acc)
				data, err := agencyAccrualsFS.ReadFile(path.Join(basePath, resultPrefix+testName))
				require.NoError(t, err, "failed test file: "+testName)

				var result entities.Accrual
				require.NoError(t, json.Unmarshal(data, &result), "failed test file: "+testName)

				checkResultAccrual(t, result, acc, false, testName)
				was = true
			}
			require.True(t, was)
		})
		was = true
	}

	require.True(s.T(), was)
}

// TestProcessMessages test processing base account events with dry-run flag
// get input messages from dir agencyAccrualsPath with prefix inputPrefix
// process it and compare with "canonical" result from file with resultPrefix
func (s *AccrualerTestSuite) TestProcessMessagesDryRun() {
	server := s.newServer()
	require.NoError(s.T(), mockProcProviders(server))

	config := s.logicConf.Namespace[entities.DefaultNamespace].Accruals.Types[aconf.AgencyFull].Topics[providers.DryRun]
	lb := server.writers.Writers[config].(*mocks.MockLogBrokerProducer)

	files, err := agencyAccrualsFS.ReadDir(dryRunPath)
	require.NoError(s.T(), err)

	was := false
	for _, data := range files {
		if !strings.HasPrefix(data.Name(), inputPrefix) {
			continue
		}
		xlog.Info(*s.ctx, "test file", log.String("name", data.Name()))
		testName := data.Name()[len(inputPrefix):len(data.Name())]

		var acc entities.Accrual
		processEventFile(*s.ctx, s.T(), server, lb, agencyAccrualsFS, path.Join(dryRunPath, data.Name()), &acc)
		// for now all tests produce the same result
		data, err := agencyAccrualsFS.ReadFile(path.Join(dryRunPath, "result.json"))
		require.NoError(s.T(), err, "failed test file: "+testName)

		var result entities.Accrual
		require.NoError(s.T(), json.Unmarshal(data, &result), "failed test file: "+testName)

		checkResultAccrual(s.T(), result, acc, false, testName)
		was = true
	}
	require.True(s.T(), was)
}

// TestProcessActMessages test processing base account events for agency acts
// get input messages from dir agencyActsPath with prefix inputPrefix
func (s *AccrualerTestSuite) TestProcessActMessages() {
	server := s.newServer()
	require.NoError(s.T(), mockProcProviders(server))

	config := s.logicConf.Namespace[entities.DefaultNamespace].Acts[aconf.Agency].Topics[providers.DefaultName]
	lb := server.writers.Writers[config].(*mocks.MockLogBrokerProducer)

	was := false
	dirs, err := agencyActsFS.ReadDir(agencyActsPath)
	require.NoError(s.T(), err)
	for _, data := range dirs {
		if !strings.HasPrefix(data.Name(), inputPrefix) {
			continue
		}
		xlog.Info(*s.ctx, "test file", log.String("name", data.Name()))
		testName := data.Name()[len(inputPrefix):len(data.Name())]

		before := len(lb.Messages)
		var processed entities.Act
		processEventFile(*s.ctx, s.T(), server, lb, agencyActsFS, path.Join(agencyActsPath, data.Name()), &processed)
		require.Len(s.T(), lb.Messages, before+1, "failed test file: "+testName)

		data, err := agencyActsFS.ReadFile(path.Join(agencyActsPath, resultPrefix+testName))
		require.NoError(s.T(), err, "failed test file: "+testName)

		var result entities.Act
		require.NoError(s.T(), json.Unmarshal(data, &result), "failed test file: "+testName)

		require.Equal(s.T(), result, processed, "failed test file: "+testName)

		was = true
	}
	require.True(s.T(), was)
}

// TestProcessResendNetting test resending netting event to specific topic
// 1. from base topic
// 2. retry if no marked events found
func (s *AccrualerTestSuite) TestProcessResendNetting() {
	server := s.newServer()
	require.NoError(s.T(), mockProcProviders(server))

	config := s.config.NettingProv
	lb := server.writers.Writers[config].(*mocks.MockLogBrokerProducer)

	yp := mocks.YTProcessorMock{BatchData: [][]entities.YTDataModel{{}}}
	p := server.processors["bnpl"]
	p.yp = &yp
	server.processors["bnpl"] = p

	before := len(lb.Messages)
	sendEventFile(s.T(), server, nettingFS, path.Join(nettingPath, "input_netting_event.json"), nil)
	require.Len(s.T(), lb.Messages, before+1, "send netting from input topic")

	var netting entities.Netting
	require.NoError(s.T(), json.Unmarshal(lb.Messages[before], &netting))

	data, err := nettingFS.ReadFile(path.Join(nettingPath, "process_netting_event.json"))
	require.NoError(s.T(), err)

	var result entities.Netting
	require.NoError(s.T(), json.Unmarshal(data, &result))
	result.Attrs.EventTime = result.Attrs.EventTime.In(time.UTC)
	result.Created = netting.Created
	result.Processed = netting.Processed

	require.Equal(s.T(), result, netting)
	assert.Zero(s.T(), netting.Iteration)
	assert.Zero(s.T(), netting.Processed)
	assert.NotZero(s.T(), netting.Created)

	before = len(lb.Messages)
	sendEventFile(s.T(), server, nettingFS, path.Join(nettingPath, "process_netting_event.json"), server.handleNettingMessageBatch)
	require.Len(s.T(), lb.Messages, before+1, "correct event with no marked events yet")

	require.NoError(s.T(), json.Unmarshal(lb.Messages[before], &netting))
	assert.Equal(s.T(), uint64(1), netting.Iteration)
	assert.NotZero(s.T(), netting.Processed)
	assert.NotZero(s.T(), netting.Created)
}

// TestUnknownNamespace test base message processing with unknown namespace
func (s *AccrualerTestSuite) TestUnknownNamespace() {
	server := s.newServer()
	require.NoError(s.T(), mockProcProviders(server))

	config := s.config.Unknown
	lb := server.writers.Writers[config].(*mocks.MockLogBrokerProducer)

	data := []byte(`{
    "event": {
        "account_id": 1000000000663972,
        "amount": "163.500000",
        "dt": 1625082664,
        "loc": {
            "client_id": "84773393",
            "contract_id": "1596599",
            "currency": "RUB",
            "namespace": "fintools",
            "terminal_id": "55413006",
            "type": "cashless"
        },
        "type": "credit"
    },
    "event_batch": {
        "dt": 1625082664,
        "event_count": 1,
        "external_id": "10862397579",
        "id": 1000000059516900,
        "info": {
            "amount": 163.5,
            "client_id": 84773393,
            "contract_id": 1596599,
            "currency": "RUB",
            "payload": {
                "terminal_id": 55413006
            },
            "service_id": 124,
            "tariffer_payload": {
                "common_ts": 1625082664,
                "contract_external_id": "760636/20",
                "dry_run": true
            },
            "transaction_id": 10862397579,
            "transaction_time": "2021-06-30T19:52:11.446457+00:00",
            "transaction_type": "payment"
        },
        "type": "fintools:cashless"
    },
    "id": 1000000067710927,
    "seq_id": 1000000043900039
}
`)

	m := persqueue.ReadMessage{Data: data}
	b := persqueue.MessageBatch{Messages: []persqueue.ReadMessage{m}}

	before := len(lb.Messages)
	require.NoError(s.T(), server.handleMessageBatch(*s.ctx, m, b))
	require.Len(s.T(), lb.Messages, before+1)
	require.Equal(s.T(), data, lb.Messages[before])
}

// TestProcessMessages test processing base account events
// get input messages from dir agencyAccrualsPath with prefix inputPrefix
// process it and compare with "canonical" result from file with resultPrefix
func (s *AccrualerTestSuite) TestProcessSpendableMessages() {
	server := s.newServer()
	require.NoError(s.T(), mockProcProviders(server))

	was := false
	dirs, err := agencyAccrualsFS.ReadDir(agencyAccrualsPath)
	require.NoError(s.T(), err)
	for _, root := range dirs {
		if !root.IsDir() || !strings.HasPrefix(root.Name(), spendTestPrefix) {
			continue
		}
		xlog.Info(*s.ctx, "run process tests", log.String("dir", root.Name()))

		s.T().Run(root.Name(), func(t *testing.T) {
			was := false
			basePath := path.Join(agencyAccrualsPath, root.Name())
			namespace := strings.TrimPrefix(root.Name(), spendTestPrefix)

			files, err := agencyAccrualsFS.ReadDir(basePath)
			require.NoError(t, err)

			config := s.logicConf.Namespace[namespace].Accruals.Types[aconf.Spendable].Topics[providers.DefaultName]
			lb := server.writers.Writers[config].(*mocks.MockLogBrokerProducer)

			for _, data := range files {
				if !strings.HasPrefix(data.Name(), inputPrefix) {
					continue
				}
				xlog.Info(*s.ctx, "test file", log.String("name", data.Name()))
				testName := strings.TrimPrefix(data.Name(), inputPrefix)

				var acc entities.SpendableAccrual
				processEventFile(*s.ctx, t, server, lb, agencyAccrualsFS, path.Join(basePath, data.Name()), &acc)
				data, err := agencyAccrualsFS.ReadFile(path.Join(basePath, resultPrefix+testName))
				require.NoError(t, err, "failed test file: "+testName)

				var result entities.SpendableAccrual
				require.NoError(t, json.Unmarshal(data, &result), "failed test file: "+testName)

				xlog.Info(*s.ctx, "processed accrual", log.Any("accrual", acc))

				checkResultSpendable(t, result, acc, false, testName)
				was = true
			}
			require.True(t, was)
		})
		was = true
	}

	require.True(s.T(), was)
}

// TestProcessTarifferPayload test processing base account events
// checks that "page_id" field from "tariffer_payload" is correctly extracted regardless of "tariffer_payload" path
func (s *AccrualerTestSuite) TestProcessTarifferPayload() {
	server := s.newServer()
	require.NoError(s.T(), mockProcProviders(server))

	namespace := "plus"

	files, err := agencyAccrualsFS.ReadDir(tarifferPayloadPath)
	require.NoError(s.T(), err)

	config := s.logicConf.Namespace[namespace].Accruals.Types[aconf.Spendable].Topics[providers.DefaultName]
	lb := server.writers.Writers[config].(*mocks.MockLogBrokerProducer)

	was := false
	data, err := agencyAccrualsFS.ReadFile(path.Join(tarifferPayloadPath, "result.json"))
	require.NoError(s.T(), err)

	var result entities.SpendableAccrual
	require.NoError(s.T(), json.Unmarshal(data, &result), "failed loading result file: "+tarifferPayloadPath+" result.json")

	for _, file := range files {
		if !strings.HasPrefix(file.Name(), inputPrefix) {
			continue
		}
		xlog.Info(*s.ctx, "test file", log.String("name", file.Name()))
		testName := strings.TrimPrefix(file.Name(), inputPrefix)

		var acc entities.SpendableAccrual
		processEventFile(*s.ctx, s.T(), server, lb, agencyAccrualsFS, path.Join(tarifferPayloadPath, file.Name()), &acc)

		xlog.Info(*s.ctx, "processed accrual", log.Any("accrual", acc))

		checkResultSpendable(s.T(), result, acc, false, testName)
		was = true
	}

	require.True(s.T(), was)
}
