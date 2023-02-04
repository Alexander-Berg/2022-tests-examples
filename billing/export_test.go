package logbroker

import (
	"context"
	"os"
	"strconv"
	"testing"
	"time"

	"github.com/stretchr/testify/suite"

	"a.yandex-team.ru/billing/hot/accounts/pkg/core/entities"
	"a.yandex-team.ru/billing/library/go/billingo/pkg/storage/logbroker"
	"a.yandex-team.ru/billing/library/go/billingo/pkg/xlog"
	"a.yandex-team.ru/library/go/core/log"
	zaplog "a.yandex-team.ru/library/go/core/log/zap"
)

type ExportStorageTestSuite struct {
	suite.Suite
}

func TestExportStorageTestSuite(t *testing.T) {
	suite.Run(t, new(ExportStorageTestSuite))
}

func (s *ExportStorageTestSuite) getLBPort() int {
	lbPortStr, ok := os.LookupEnv("LOGBROKER_PORT")
	if !ok {
		s.T().Fatalf("Environment variable %s is not set", "LOGBROKER_PORT")
	}

	lbPort, err := strconv.Atoi(lbPortStr)
	if err != nil {
		s.T().Fatal(err)
	}

	return lbPort
}

func (s *ExportStorageTestSuite) TestWriteEventsBatch() {
	logger, err := zaplog.NewDeployLogger(log.DebugLevel)
	if err != nil {
		s.T().Fatal(err)
	}
	xlog.SetGlobalLogger(logger)

	cases := []struct {
		Name   string
		SeqIDs []int64
	}{
		{
			Name: "ZeroEvents", SeqIDs: []int64{},
		},
		{
			Name: "OneEvent", SeqIDs: []int64{10},
		},
		{
			Name: "TwoEvents", SeqIDs: []int64{11, 12},
		},
		{
			Name: "RepeatSeqNo", SeqIDs: []int64{11, 11},
		},
	}

	for _, c := range cases {
		c := c
		s.T().Run(c.Name, func(t *testing.T) {
			storage, err := NewExportStorage(
				logbroker.ProducerConfig{
					Endpoint: "127.0.0.1",
					Port:     s.getLBPort(),
					Topic:    "/TestWriteEvents",
					SourceID: "some_source_id",
				},
				1111,
				nil,
			)
			if err != nil {
				s.T().Fatal(err)
			}

			ctx := context.Background()
			_, err = storage.Start(ctx)
			s.Assert().NoError(err)

			a1 := "first_attr_val"
			a2 := "second_attr_val"
			entries := make([]entities.LbExportEntry, 0, len(c.SeqIDs))
			for _, seqID := range c.SeqIDs {
				entries = append(entries, entities.LbExportEntry{
					ID:    seqID + 10,
					SeqID: seqID,
					Event: entities.ExportEvent{
						EventAttributes: entities.EventAttributes{
							Loc: entities.LocationAttributes{
								Type:       "loc_type",
								Attributes: map[string]*string{"attr1": &a1, "attr2": &a2},
							},
							Type:   "event_type",
							Dt:     time.Now().Add(-1 * time.Minute).UTC(),
							Amount: "",
						},
						AccountID: 127,
					},
					EventBatch: entities.ExportEventBatchMeta{
						EventBatch: entities.EventBatch{
							Type:       "event_batch_type",
							Dt:         time.Now().Add(-1 * time.Minute).UTC(),
							ExternalID: "some_external_id",
							Info:       []byte(`{"some_info": "valuable_info"}`),
						},
						ID: 1,
					},
				})
			}
			err = storage.WriteEventsBatch(ctx, entities.NewLbExportBatch(entries, time.Now()))
			s.Assert().NoError(err, "batch write must not return error")
			// todo-igogor проверять появление событий в логброкере можно было добавить, но геморрой.
			err = storage.Disconnect()
			s.Assert().NoError(err)
		})
	}
}

//logger, err := zaplog.NewDeployLogger(log.DebugLevel)
//if err != nil {
//	s.T().Fatal(err)
//}
//
//	//p, err := logbroker.NewProducer(
//	//	logbroker.ProducerConfig{
//	//		Endpoint: "127.0.0.1",
//	//		Port:     lbPort,
//	//		Topic:    "/events",
//	//		SourceID: "igogor-source-id",
//	//	},
//	//	log.With(logger, log.String("writer", "1")),
//	//	nil,
//	//)
//	//if err != nil {
//	//	s.T().Fatal(err)
//	//}
//	//
//	//_, err = p.Start(s.ctx)
//	//if err != nil {
//	//	s.T().Fatal(err)
//	//}
//
//}
