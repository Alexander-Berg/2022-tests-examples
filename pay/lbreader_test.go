package logbroker

import (
	"context"
	"fmt"
	"os"
	"strconv"
	"testing"
	"time"

	"a.yandex-team.ru/billing/library/go/billingo/pkg/storage/logbroker"
	"a.yandex-team.ru/billing/library/go/billingo/pkg/xlog"
	"a.yandex-team.ru/kikimr/public/sdk/go/persqueue"
	"a.yandex-team.ru/library/go/core/log"
	zaplog "a.yandex-team.ru/library/go/core/log/zap"
	"github.com/stretchr/testify/suite"

	"a.yandex-team.ru/payplatform/fes/collector/pkg/core"
)

type ExportStorageTestSuite struct {
	suite.Suite
}

func TestExportStorageTestSuite(t *testing.T) {
	suite.Run(t, new(ExportStorageTestSuite))
}

func (s *ExportStorageTestSuite) getLBPort() int {
	lbPortStr, ok := os.LookupEnv("LOGBROKER_PORT")
	s.Require().Truef(ok, "Environment variable LOGBROKER_PORT is not set")

	lbPort, err := strconv.Atoi(lbPortStr)
	s.Require().NoError(err)

	return lbPort
}

func (s *ExportStorageTestSuite) prepareTestCase(ctx context.Context, seqIDs []uint64) {
	producer, err := logbroker.NewProducer(
		logbroker.ProducerConfig{
			Endpoint: "127.0.0.1",
			Database: "/Root",
			Port:     s.getLBPort(),
			Topic:    "default-topic",
			SourceID: "some_source_id",
		},
		nil,
	)
	s.Assert().NoError(err)

	_, err = producer.Start(ctx)
	s.Assert().NoError(err)
	defer func() {
		s.Assert().NoError(producer.Disconnect())
	}()

	var messages [][]byte
	for _, v := range seqIDs {
		messages = append(
			messages,
			[]byte(fmt.Sprintf("Message for seq = %d;", v)),
		)
	}

	s.Assert().NoError(producer.WriteBatchExplicitSeqNo(ctx, messages, seqIDs))
}

func makeLoggingHandler(eventsExpected int, cancelFunc context.CancelFunc) logbroker.ConsumerMsgHandler {
	var eventsRead int
	return func(ctx context.Context, rm persqueue.ReadMessage, mb persqueue.MessageBatch) error {
		xlog.Info(ctx, "=================================Started batch=================================")
		for _, message := range mb.Messages {
			eventsRead += 1
			if eventsRead == eventsExpected {
				cancelFunc()
			}
			xlog.Info(ctx, "Message", log.String("Data", string(message.Data)))
		}
		xlog.Info(ctx, "=================================Ended batch=================================")
		return nil
	}
}

func (s *ExportStorageTestSuite) TestWriteEventsBatch() {
	logger, err := zaplog.NewDeployLogger(log.DebugLevel)
	s.Require().NoError(err)
	xlog.SetGlobalLogger(logger)

	cases := []struct {
		Name   string
		SeqIDs []uint64
	}{
		{Name: "OneEvent", SeqIDs: []uint64{10}},
		{Name: "TwoEvents", SeqIDs: []uint64{11, 12}},
		{Name: "RepeatSeqNo", SeqIDs: []uint64{11, 11}},
	}

	for _, testCase := range cases {
		testCase := testCase
		s.Run(testCase.Name, func() {
			ctx := context.Background()

			s.prepareTestCase(ctx, testCase.SeqIDs)

			storage, err := NewLBReader(
				core.ConsumerConfig{
					Endpoint: "127.0.0.1",
					Port:     s.getLBPort(),
					Topic:    "default-topic",
					Consumer: "test_client",
				},
				1111,
				nil,
			)
			s.Require().NoError(err)
			defer storage.Disconnect()

			cancelCtx, cancel := context.WithTimeout(ctx, 5*time.Second)
			s.Assert().Error(
				storage.ReadBatch(
					cancelCtx,
					makeLoggingHandler(len(testCase.SeqIDs), cancel),
				),
				context.Canceled,
			)
		})
	}
}
