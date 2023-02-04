package integration

import (
	"context"
	"encoding/json"
	"os"
	"path"
	"path/filepath"
	"strconv"
	"strings"
	"sync"
	"testing"
	"time"

	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"

	"a.yandex-team.ru/billing/hot/accrualer/internal/accrualer"
	aconf "a.yandex-team.ru/billing/hot/accrualer/internal/config"
	"a.yandex-team.ru/billing/hot/accrualer/internal/entities"
	"a.yandex-team.ru/billing/hot/accrualer/internal/transforms"
	"a.yandex-team.ru/billing/hot/accrualer/internal/ytprocessor"
	bconfig "a.yandex-team.ru/billing/library/go/billingo/pkg/config"
	"a.yandex-team.ru/billing/library/go/billingo/pkg/storage/logbroker"
	"a.yandex-team.ru/billing/library/go/billingo/pkg/xlog"
	"a.yandex-team.ru/kikimr/public/sdk/go/persqueue"
	"a.yandex-team.ru/library/go/core/log"
	zaplog "a.yandex-team.ru/library/go/core/log/zap"
	"a.yandex-team.ru/library/go/test/yatest"
	"a.yandex-team.ru/yt/go/ypath"
	"a.yandex-team.ru/yt/go/yt"
	"a.yandex-team.ru/yt/go/yt/ythttp"
)

const (
	localTableName = "2006-01-02T15-04-05"
	markedEvents   = "marked_events"

	lbEndpoint = "localhost"
)

var (
	rootPath  = ypath.Path("//home/balance/test/new_billing/public")
	lbPortStr = os.Getenv("LOGBROKER_PORT")
	lbPort    = 0
)

func moveNodes(ctx context.Context, yc yt.Client) error {
	namespaces := make([]string, 0)
	rootPath := rootPath
	if err := yc.ListNode(ctx, rootPath, &namespaces, nil); err != nil {
		return err
	}

	for _, namespace := range namespaces {
		events := make([]string, 0)
		eventPath := rootPath.JoinChild(namespace, markedEvents)
		err := yc.ListNode(ctx, eventPath, &events, nil)
		if err != nil {
			return err
		}

		for _, event := range events {
			table, err := time.Parse(localTableName, event)
			if err != nil {
				if _, nErr := time.Parse(ytprocessor.TableNameFormat, event); nErr != nil {
					return err
				}
				continue
			}

			_, err = yc.MoveNode(
				ctx,
				eventPath.JoinChild(event),
				eventPath.JoinChild(table.Format(ytprocessor.TableNameFormat)),
				&yt.MoveNodeOptions{Force: true, Recursive: true},
			)
			if err != nil {
				return err
			}
		}
	}
	return nil
}

func setupServer() (*accrualer.Server, context.CancelFunc, *aconf.Config, *aconf.LogicConfig, error) {
	loc, err := time.LoadLocation("Europe/Moscow")
	if err != nil {
		return nil, nil, nil, nil, err
	}

	if lbPortStr != "" {
		lbPort, err = strconv.Atoi(lbPortStr)
		if err != nil {
			return nil, nil, nil, nil, err
		}
	}

	transforms.RegisterTransforms(loc)

	rootPath, err := filepath.Abs(yatest.SourcePath("billing/hot/accrualer/config"))
	if err != nil {
		return nil, nil, nil, nil, err
	}

	ctx := context.Background()

	configLoader, _ := bconfig.PrepareLoaderWithEnvPrefix("accrualer", path.Join(rootPath, "test", "accrualer.yaml"))
	config, err := aconf.Parse(ctx, configLoader, xlog.GlobalLogger())
	if err != nil {
		return nil, nil, nil, nil, err
	}
	config.NettingCons.SetEndpoint(lbEndpoint, lbPort)
	config.NettingProv.SetEndpoint(lbEndpoint, lbPort)
	config.Notifier.SetEndpoint(lbEndpoint, lbPort)
	config.Unknown.SetEndpoint(lbEndpoint, lbPort)
	config.Src.SetEndpoint(lbEndpoint, lbPort)
	config.Manual.SetEndpoint(lbEndpoint, lbPort)

	configLoader, _ = bconfig.PrepareLoaderWithEnvPrefix("accrualer", path.Join(rootPath, "test", "logic.yaml"))
	logicConfig, err := aconf.ParseLogic(ctx, configLoader, xlog.GlobalLogger())
	if err != nil {
		return nil, nil, nil, nil, err
	}

	for namespace, config := range logicConfig.Namespace {
		for action, providers := range config.Accruals.Types {
			for name, conf := range providers.Topics {
				conf.SetEndpoint(lbEndpoint, lbPort)
				logicConfig.Namespace[namespace].Accruals.Types[action].Topics[name] = conf
			}
		}
		for action, providers := range config.Acts {
			for name, conf := range providers.Topics {
				conf.SetEndpoint(lbEndpoint, lbPort)
				logicConfig.Namespace[namespace].Acts[action].Topics[name] = conf
			}
		}
		conf := logicConfig.Namespace[namespace]
		if conf.Accruals.MarkedEvents.Cluster != "" {
			conf.Accruals.MarkedEvents.Cluster = os.Getenv("YT_PROXY")
			logicConfig.Namespace[namespace] = conf
		}

	}
	xlog.Info(ctx, "updated logic conf", log.Any("config", logicConfig))

	formatsPath := path.Join(rootPath, "dev", "formats")
	formatsConf, err := aconf.ParseFormatsConfig(ctx, os.DirFS(formatsPath), ".")
	if err != nil {
		return nil, nil, nil, nil, err
	}
	server, cancel, err := accrualer.NewServer(ctx, loc, config, logicConfig, formatsConf)
	return server, cancel, config, logicConfig, err
}

// TestAccrualsFromYT simple happy path prepare accruals from marked event (YT-table)
func TestAccrualsFromYT(t *testing.T) {
	ctx := context.Background()
	logger, _ := zaplog.NewDeployLogger(log.DebugLevel)
	xlog.SetGlobalLogger(logger)

	yc, err := ythttp.NewClient(&yt.Config{
		Proxy:  os.Getenv("YT_PROXY"),
		Logger: xlog.GlobalLogger().Structured(),
	})
	require.NoError(t, err)
	require.NoError(t, moveNodes(ctx, yc))

	server, cancel, config, _, err := setupServer()
	require.NoError(t, err)
	go func() {
		_ = server.Run()
	}()
	defer func() {
		server.DisconnectReaders()
		cancel()
	}()

	nettingEventBytes := []byte(`{
    "event": "{\n    \"event\": {\n        \"account_id\": 3474525,\n        \"amount\": \"23060.000000\",\n        \"dt\": 1646337681,\n        \"loc\": {\n            \"client_id\": \"97170146\",\n            \"contract_id\": \"5139428\",\n            \"currency\": \"RUB\",\n            \"namespace\": \"bnpl\",\n            \"service_id\": \"1125\",\n            \"type\": \"payout\"\n        },\n        \"type\": \"credit\"\n    },\n    \"event_batch\": {\n        \"dt\": 1646337600,\n        \"event_count\": 4,\n        \"external_id\": \"bnpl/bnpl-payout/1257521844/97170146\",\n        \"id\": 2959168141,\n        \"info\": {\n            \"client_id\": 97170146,\n            \"event_time\": \"2022-03-03T20:00:00+00:00\",\n            \"payload\": {},\n            \"tariffer_payload\": {\n                \"common_ts\": 1646337600,\n                \"contract_states\": {\n                    \"invoices\": {\n                        \"5139428\": [\n                            {\n                                \"id\": 37064091,\n                                \"external_id\": \"725d06b0-8e17-4a79-b3e9-781193adf59f\",\n                                \"operation_type\": \"INSERT_NETTING\",\n                                \"amount\": 6.0\n                            }\n                        ]\n                    }\n                },\n                \"dry_run\": false,\n                \"operation_expressions\": {\n                    \"5139428\": [\n                        {\n                            \"expressions\": [\n                                {\n                                    \"account\": \"cashless\",\n                                    \"amount\": 21150,\n                                    \"operation_type\": \"DEBIT\"\n                                },\n                                {\n                                    \"account\": \"cashless_refunds\",\n                                    \"amount\": 21150,\n                                    \"operation_type\": \"CREDIT\"\n                                }\n                            ],\n                            \"name\": \"cashless_to_cashless_refunds\"\n                        },\n                        {\n                            \"expressions\": [\n                                {\n                                    \"account\": \"cashless\",\n                                    \"amount\": 23060,\n                                    \"operation_type\": \"DEBIT\"\n                                },\n                                {\n                                    \"account\": \"payout\",\n                                    \"amount\": 23060,\n                                    \"operation_type\": \"CREDIT\"\n                                }\n                            ],\n                            \"name\": \"payout_cashless\"\n                        }\n                    ]\n                }\n            },\n            \"transaction_id\": \"bnpl/bnpl-payout/1257521844/97170146\"\n        },\n        \"type\": \"bnpl:payout\"\n    },\n    \"id\": 2937382177,\n    \"seq_id\": 1055811421\n}\n",
    "processed": "2009-11-10T23:00:00Z",
    "created": "2009-11-10T23:00:00Z",
    "iteration": 0,
    "attrs": {
        "namespace": "bnpl",
        "id": 2937382177,
        "seq_id": 1055811421,
        "dry_run": false,
        "batch": "bnpl:payout",
        "loc": "payout",
        "contract_id": "5139428",
        "event_time": "2022-03-03T20:00:00+00:00",
        "transaction_id": "bnpl/bnpl-payout/1257521844/97170146",
        "bill_num": "725d06b0-8e17-4a79-b3e9-781193adf59f"
    }
}
`)

	nettingProvider, err := logbroker.NewProducer(
		logbroker.ProducerConfig{
			Endpoint:         config.NettingProv.Endpoint,
			Port:             config.NettingProv.Port,
			Database:         config.NettingProv.Database,
			Topic:            config.NettingProv.Topic,
			CompressionLevel: config.NettingProv.CompressionLevel,
			MaxMemory:        config.NettingProv.MaxMemory,
			PartitionGroup:   config.NettingProv.PartitionGroup,
			RetryOnFailure:   true,
			SourceID:         "local-testing-source-id-input",
		},
		nil,
	)
	require.NoError(t, err)
	_, err = nettingProvider.Start(ctx)
	require.NoError(t, err)
	require.NoError(t, nettingProvider.Write(ctx, nettingEventBytes))
	assert.NoError(t, nettingProvider.Disconnect())

	nettingReader, err := logbroker.NewConsumer(
		logbroker.ConsumerConfig{
			Endpoint: lbEndpoint,
			Port:     lbPort,
			Consumer: "awesome-consumer",
			Topic:    config.NettingProv.Topic,
		},
		nil)
	require.NoError(t, err)

	go func() {
		_ = nettingReader.Read(ctx, func(ctx context.Context, message persqueue.ReadMessage, batch persqueue.MessageBatch) error {
			return nil
		})
	}()
	defer func() {
		nettingReader.Disconnect()
	}()

	reader, err := logbroker.NewConsumer(
		logbroker.ConsumerConfig{
			Endpoint: lbEndpoint,
			Port:     lbPort,
			Consumer: "awesome-consumer",
			Topic:    "accruals/new-accrual-common",
		},
		nil)
	require.NoError(t, err)

	var accruals []entities.Accrual
	go func() {
		_ = reader.Read(ctx, func(ctx context.Context, message persqueue.ReadMessage, batch persqueue.MessageBatch) error {
			var accrual entities.Accrual
			require.NoError(t, json.Unmarshal(message.Data, &accrual))
			accruals = append(accruals, accrual)
			assert.Equal(t, "725d06b0-8e17-4a79-b3e9-781193adf59f", accrual.BillNum)
			return nil
		})
	}()
	defer func() {
		reader.Disconnect()
	}()

	time.Sleep(1 * time.Second)
	require.Len(t, accruals, 2)
}

// TestEmptyNewData test setting previous table
// 1. New events regarding tables
// 2. No data in current tables
func TestEmptyNewData(t *testing.T) {
	ctx := context.Background()
	logger, _ := zaplog.NewDeployLogger(log.DebugLevel)
	xlog.SetGlobalLogger(logger)

	yc, err := ythttp.NewClient(&yt.Config{
		Proxy:  os.Getenv("YT_PROXY"),
		Logger: xlog.GlobalLogger().Structured(),
	})
	require.NoError(t, err)
	require.NoError(t, moveNodes(ctx, yc))

	server, cancel, config, _, err := setupServer()
	require.NoError(t, err)
	go func() {
		_ = server.Run()
	}()
	defer func() {
		server.DisconnectReaders()
		cancel()
	}()

	nettingEventBytes := []byte(`{
    "event": "{\n    \"event\": {\n        \"account_id\": 3474525,\n        \"amount\": \"23060.000000\",\n        \"dt\": 1646337681,\n        \"loc\": {\n            \"client_id\": \"97170146\",\n            \"contract_id\": \"5139428\",\n            \"currency\": \"RUB\",\n            \"namespace\": \"bnpl\",\n            \"service_id\": \"1125\",\n            \"type\": \"payout\"\n        },\n        \"type\": \"credit\"\n    },\n    \"event_batch\": {\n        \"dt\": 1646337600,\n        \"event_count\": 4,\n        \"external_id\": \"bnpl/bnpl-payout/1257521844/97170146\",\n        \"id\": 2959168141,\n        \"info\": {\n            \"client_id\": 97170146,\n            \"event_time\": \"2022-03-03T20:00:00+00:00\",\n            \"payload\": {},\n            \"tariffer_payload\": {\n                \"common_ts\": 1646337600,\n                \"contract_states\": {\n                    \"invoices\": {\n                        \"5139428\": [\n                            {\n                                \"id\": 37064091,\n                                \"external_id\": \"725d06b0-8e17-4a79-b3e9-781193adf59f\",\n                                \"operation_type\": \"INSERT_NETTING\",\n                                \"amount\": 6.0\n                            }\n                        ]\n                    }\n                },\n                \"dry_run\": false,\n                \"operation_expressions\": {\n                    \"5139428\": [\n                        {\n                            \"expressions\": [\n                                {\n                                    \"account\": \"cashless\",\n                                    \"amount\": 21150,\n                                    \"operation_type\": \"DEBIT\"\n                                },\n                                {\n                                    \"account\": \"cashless_refunds\",\n                                    \"amount\": 21150,\n                                    \"operation_type\": \"CREDIT\"\n                                }\n                            ],\n                            \"name\": \"cashless_to_cashless_refunds\"\n                        },\n                        {\n                            \"expressions\": [\n                                {\n                                    \"account\": \"cashless\",\n                                    \"amount\": 23060,\n                                    \"operation_type\": \"DEBIT\"\n                                },\n                                {\n                                    \"account\": \"payout\",\n                                    \"amount\": 23060,\n                                    \"operation_type\": \"CREDIT\"\n                                }\n                            ],\n                            \"name\": \"payout_cashless\"\n                        }\n                    ]\n                }\n            },\n            \"transaction_id\": \"bnpl/bnpl-payout/1257521844/97170146\"\n        },\n        \"type\": \"bnpl:payout\"\n    },\n    \"id\": 2937382177,\n    \"seq_id\": 1055811421\n}\n",
    "processed": "2009-11-10T23:00:00Z",
    "created": "2022-03-03T20:00:00Z",
    "iteration": 0,
    "attrs": {
        "namespace": "bnpl",
        "id": 2937382177,
        "seq_id": 1055811421,
        "dry_run": false,
        "batch": "bnpl:payout",
        "loc": "payout",
        "contract_id": "5139428",
        "event_time": "2022-07-03T20:00:00+00:00",
        "transaction_id": "bnpl/bnpl-payout/1257521844/97170146",
        "bill_num": "F503594C-832B-4A2C-889E-81C450A92CFD"
    }
}
`)

	nettingProvider, err := logbroker.NewProducer(
		logbroker.ProducerConfig{
			Endpoint:         config.NettingProv.Endpoint,
			Port:             config.NettingProv.Port,
			Database:         config.NettingProv.Database,
			Topic:            config.NettingProv.Topic,
			CompressionLevel: config.NettingProv.CompressionLevel,
			MaxMemory:        config.NettingProv.MaxMemory,
			PartitionGroup:   config.NettingProv.PartitionGroup,
			RetryOnFailure:   true,
			SourceID:         "local-testing-source-id-input",
		},
		nil,
	)
	require.NoError(t, err)
	_, err = nettingProvider.Start(ctx)
	require.NoError(t, err)
	require.NoError(t, nettingProvider.Write(ctx, nettingEventBytes))

	reader, err := logbroker.NewConsumer(
		logbroker.ConsumerConfig{
			Endpoint: lbEndpoint,
			Port:     lbPort,
			Consumer: "awesome-consumer",
			Topic:    config.NettingProv.Topic,
		},
		nil)
	require.NoError(t, err)

	mu := sync.Mutex{}
	transactions := map[string]struct{}{}
	go func() {
		_ = reader.Read(ctx, func(ctx context.Context, message persqueue.ReadMessage, batch persqueue.MessageBatch) error {
			sourceID := (string)(message.SourceID)
			if strings.Contains(sourceID, "input") {
				// skip initial netting message
				return nil
			}
			xlog.Info(ctx, "check netting", log.Any("message", message))
			var netting entities.Netting
			require.NoError(t, json.Unmarshal(message.Data, &netting))

			assert.Equal(t, uint64(1), netting.Iteration)
			assert.Equal(t, "2022-05-11T10:00:00", netting.Previous)
			mu.Lock()
			transactions[netting.Attrs.TransactionID] = struct{}{}
			mu.Unlock()
			return nil
		})
	}()
	defer func() {
		reader.Disconnect()
	}()

	time.Sleep(1 * time.Second)

	nettingEventBytes = []byte(`{
	   "event": "{\n    \"event\": {\n        \"account_id\": 3474525,\n        \"amount\": \"23060.000000\",\n        \"dt\": 1646337681,\n        \"loc\": {\n            \"client_id\": \"97170146\",\n            \"contract_id\": \"5139428\",\n            \"currency\": \"RUB\",\n            \"namespace\": \"bnpl\",\n            \"service_id\": \"1125\",\n            \"type\": \"payout\"\n        },\n        \"type\": \"credit\"\n    },\n    \"event_batch\": {\n        \"dt\": 1646337600,\n        \"event_count\": 4,\n        \"external_id\": \"bnpl/bnpl-payout/1257521844/97170146\",\n        \"id\": 2959168141,\n        \"info\": {\n            \"client_id\": 97170146,\n            \"event_time\": \"2022-03-03T20:00:00+00:00\",\n            \"payload\": {},\n            \"tariffer_payload\": {\n                \"common_ts\": 1646337600,\n                \"contract_states\": {\n                    \"invoices\": {\n                        \"5139428\": [\n                            {\n                                \"id\": 37064091,\n                                \"external_id\": \"725d06b0-8e17-4a79-b3e9-781193adf59f\",\n                                \"operation_type\": \"INSERT_NETTING\",\n                                \"amount\": 6.0\n                            }\n                        ]\n                    }\n                },\n                \"dry_run\": false,\n                \"operation_expressions\": {\n                    \"5139428\": [\n                        {\n                            \"expressions\": [\n                                {\n                                    \"account\": \"cashless\",\n                                    \"amount\": 21150,\n                                    \"operation_type\": \"DEBIT\"\n                                },\n                                {\n                                    \"account\": \"cashless_refunds\",\n                                    \"amount\": 21150,\n                                    \"operation_type\": \"CREDIT\"\n                                }\n                            ],\n                            \"name\": \"cashless_to_cashless_refunds\"\n                        },\n                        {\n                            \"expressions\": [\n                                {\n                                    \"account\": \"cashless\",\n                                    \"amount\": 23060,\n                                    \"operation_type\": \"DEBIT\"\n                                },\n                                {\n                                    \"account\": \"payout\",\n                                    \"amount\": 23060,\n                                    \"operation_type\": \"CREDIT\"\n                                }\n                            ],\n                            \"name\": \"payout_cashless\"\n                        }\n                    ]\n                }\n            },\n            \"transaction_id\": \"324A57EE-DBBC-4C24-9CD5-4C907A8EF039\"\n        },\n        \"type\": \"bnpl:payout\"\n    },\n    \"id\": 2937382177,\n    \"seq_id\": 1055811421\n}\n",
	   "processed": "2009-11-10T23:00:00Z",
	   "created": "2022-03-03T20:00:00Z",
	   "iteration": 0,
	   "attrs": {
	       "namespace": "bnpl",
	       "id": 905012830,
	       "seq_id": 720953644,
	       "dry_run": false,
	       "batch": "bnpl:payout",
	       "loc": "payout",
	       "contract_id": "1761494417",
	       "event_time": "2022-03-03T20:00:00+00:00",
	       "transaction_id": "324A57EE-DBBC-4C24-9CD5-4C907A8EF039",
	       "bill_num": "D0B0AE2A-EEEC-40E9-90F7-5CEA37B1DFDD"
	   }
	}
	`)
	require.NoError(t, nettingProvider.Write(ctx, nettingEventBytes))
	require.NoError(t, nettingProvider.Disconnect())

	time.Sleep(1 * time.Second)

	require.Len(t, transactions, 2)
}
