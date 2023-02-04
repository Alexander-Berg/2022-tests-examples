package lbexporter

import (
	"context"
	"testing"
	"time"

	"github.com/golang/mock/gomock"
	"github.com/stretchr/testify/suite"

	"a.yandex-team.ru/billing/hot/accounts/mock/lbmock"
	"a.yandex-team.ru/billing/hot/accounts/mock/storagemock"
	"a.yandex-team.ru/billing/hot/accounts/pkg/core"
	"a.yandex-team.ru/billing/hot/accounts/pkg/core/entities"
	"a.yandex-team.ru/billing/hot/accounts/pkg/core/entitysettings"
)

type ExporterTestSuite struct {
	BaseTestSuite
}

func TestExporterTestSuite(t *testing.T) {
	suite.Run(t, new(ExporterTestSuite))
}

func (s *ExporterTestSuite) prepareBatch(seqIds ...int64) *entities.LbExportBatch {
	// здесь формат событий не соответствует EntitySettings но это не важно, т.к. мы мокаем.
	a1 := "first_attr_val"
	a2 := "second_attr_val"

	entries := make([]entities.LbExportEntry, 0, len(seqIds))
	for _, seqID := range seqIds {
		entries = append(
			entries,
			entities.LbExportEntry{
				ID:    seqID,
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
						Info:   []byte(`{"key":"value"}`),
					},
					AccountID: 127,
				},
				EventBatch: entities.ExportEventBatchMeta{
					EventBatch: entities.EventBatch{
						Type:       "event_batch_type",
						Dt:         time.Now().Add(-1 * time.Minute).UTC(),
						ExternalID: "some_external_id",
						Info:       []byte(`{"some_info": "valuable_info"}`),
						EventCount: 10,
					},
					ID: 1,
				},
			},
		)
	}
	return entities.NewLbExportBatch(entries, time.Now())
}

/*
Тесты построены таким образом, чтобы успело произойти фиксированное кол-во вызовов чтения и записи,
а потом экспортер завершился по отмене контекста.
Вызовы моков проверяют корректность обработки seqID и то что считанный батч попадает на запись
*/
func (s *ExporterTestSuite) TestExport() {

	slowOperation := 10 * time.Millisecond
	fastOperation := 1 * time.Millisecond
	batchLimit := 10
	dontExport := int64(-1)

	type Iteration struct {
		Batch             *entities.LbExportBatch
		FromSeqID         int64
		LastExportedSeqID int64
		WriteDuration     time.Duration
		ReadDuration      time.Duration
	}

	cases := []struct {
		Name              string
		LastExportedSeqID int
		Iterations        []Iteration
	}{
		{
			Name:              "OneBatch",
			LastExportedSeqID: 0,
			Iterations: []Iteration{
				{Batch: s.prepareBatch(1, 2, 3), FromSeqID: 1, LastExportedSeqID: dontExport,
					WriteDuration: slowOperation, ReadDuration: slowOperation},
			},
			// 1 чтение батча. Запись батча не успевает сделаться до отмены контекста.
		},
		{
			Name:              "TwoBatches",
			LastExportedSeqID: 10,
			Iterations: []Iteration{
				{Batch: s.prepareBatch(11, 12, 13), FromSeqID: 11, LastExportedSeqID: 13,
					WriteDuration: slowOperation, ReadDuration: slowOperation},
				{Batch: s.prepareBatch(14, 15, 16), FromSeqID: 14, LastExportedSeqID: dontExport,
					WriteDuration: slowOperation, ReadDuration: slowOperation},
			},
			// 2 чтения батча. 1 запись идет параллельно. Запись последнего батча не делается.
		},
		{
			Name:              "ThreeBatches",
			LastExportedSeqID: 20,
			Iterations: []Iteration{
				{Batch: s.prepareBatch(21), FromSeqID: 21, LastExportedSeqID: 21,
					WriteDuration: slowOperation, ReadDuration: slowOperation},
				{Batch: s.prepareBatch(22, 23), FromSeqID: 22, LastExportedSeqID: 23,
					WriteDuration: slowOperation, ReadDuration: slowOperation},
				{Batch: s.prepareBatch(24, 25, 26), FromSeqID: 24, LastExportedSeqID: dontExport,
					WriteDuration: slowOperation, ReadDuration: slowOperation},
			},
			// 3 чтения батча. Еще 2 записи, но они идут параллельно. Запись последнего батча не происходит.
		},
		{
			Name:              "GapsBetweenBatches",
			LastExportedSeqID: 29,
			Iterations: []Iteration{
				{Batch: s.prepareBatch(30), FromSeqID: 30, LastExportedSeqID: 30,
					WriteDuration: slowOperation, ReadDuration: slowOperation},
				{Batch: s.prepareBatch(33, 34), FromSeqID: 31, LastExportedSeqID: 34,
					WriteDuration: slowOperation, ReadDuration: slowOperation},
				{Batch: s.prepareBatch(37, 38, 39), FromSeqID: 35, LastExportedSeqID: dontExport,
					WriteDuration: slowOperation, ReadDuration: slowOperation},
			},
			// 3 чтения батча. Еще 2 записи, но они идут параллельно. Запись последнего батча не происходит.
		},
		{
			Name:              "GapsInBatches",
			LastExportedSeqID: 40,
			Iterations: []Iteration{
				{Batch: s.prepareBatch(41, 43), FromSeqID: 41, LastExportedSeqID: 43,
					WriteDuration: slowOperation, ReadDuration: slowOperation},
				{Batch: s.prepareBatch(44, 45, 47), FromSeqID: 44, LastExportedSeqID: 47,
					WriteDuration: slowOperation, ReadDuration: slowOperation},
				{Batch: s.prepareBatch(48, 49, 51), FromSeqID: 48, LastExportedSeqID: dontExport,
					WriteDuration: slowOperation, ReadDuration: slowOperation},
			},
			// 3 чтения батча. Еще 2 записи, но они идут параллельно. Запись последнего батча не происходит.
		},
		{
			Name:              "EmptyBatches",
			LastExportedSeqID: 60,
			Iterations: []Iteration{
				{Batch: s.prepareBatch(), FromSeqID: 61, LastExportedSeqID: dontExport,
					WriteDuration: slowOperation, ReadDuration: slowOperation},
				{Batch: s.prepareBatch(), FromSeqID: 61, LastExportedSeqID: dontExport,
					WriteDuration: slowOperation, ReadDuration: slowOperation},
			},
			// 2 чтения батча, 2 ожидания данных. Запись пустых батчей не делается.
		},
		{
			Name:              "MixedBatches",
			LastExportedSeqID: 70,
			Iterations: []Iteration{
				{Batch: s.prepareBatch(71), FromSeqID: 71, LastExportedSeqID: 71,
					WriteDuration: slowOperation, ReadDuration: slowOperation},
				{Batch: s.prepareBatch(), FromSeqID: 72, LastExportedSeqID: dontExport,
					WriteDuration: slowOperation, ReadDuration: slowOperation},
				{Batch: s.prepareBatch(72, 73), FromSeqID: 72, LastExportedSeqID: 73,
					WriteDuration: slowOperation, ReadDuration: slowOperation},
				{Batch: s.prepareBatch(), FromSeqID: 74, LastExportedSeqID: dontExport,
					WriteDuration: slowOperation, ReadDuration: slowOperation},
				{Batch: s.prepareBatch(75, 76), FromSeqID: 74, LastExportedSeqID: dontExport,
					WriteDuration: slowOperation, ReadDuration: slowOperation},
			},
			// 5 чтений батча, 2 ожидания данных. Запись для 1го и 3его батчей делается параллельно.
		},
		{
			Name:              "SlowReadFastWrite",
			LastExportedSeqID: 80,
			Iterations: []Iteration{
				{Batch: s.prepareBatch(81), FromSeqID: 81, LastExportedSeqID: 81,
					WriteDuration: fastOperation, ReadDuration: slowOperation},
				{Batch: s.prepareBatch(82, 83), FromSeqID: 82, LastExportedSeqID: 83,
					WriteDuration: fastOperation, ReadDuration: slowOperation},
				{Batch: s.prepareBatch(84, 85, 86), FromSeqID: 84, LastExportedSeqID: dontExport,
					WriteDuration: fastOperation, ReadDuration: slowOperation},
			},
			// 3 чтения батча. Еще 2 записи, но они идут параллельно и заканчиваются быстрее
		},
		{
			Name:              "FastReadSlowWrite",
			LastExportedSeqID: 90,
			Iterations: []Iteration{
				{Batch: s.prepareBatch(91), FromSeqID: 91, LastExportedSeqID: 91,
					WriteDuration: slowOperation, ReadDuration: fastOperation},
				{Batch: s.prepareBatch(92, 93), FromSeqID: 92, LastExportedSeqID: 93,
					WriteDuration: slowOperation, ReadDuration: fastOperation},
				{Batch: s.prepareBatch(94, 95, 96), FromSeqID: 94, LastExportedSeqID: dontExport,
					WriteDuration: slowOperation, ReadDuration: fastOperation},
			},
			// 2 долгих записи, 3 быстрых чтения. Общее время упирается в запись.
		},
		{
			Name:              "MixedReadWriteTiming",
			LastExportedSeqID: 100,
			Iterations: []Iteration{
				{Batch: s.prepareBatch(101), FromSeqID: 101, LastExportedSeqID: 101,
					WriteDuration: slowOperation, ReadDuration: fastOperation},
				{Batch: s.prepareBatch(102, 103), FromSeqID: 102, LastExportedSeqID: 103,
					WriteDuration: fastOperation, ReadDuration: slowOperation},
				{Batch: s.prepareBatch(104, 105, 106), FromSeqID: 104, LastExportedSeqID: 106,
					WriteDuration: fastOperation, ReadDuration: fastOperation},
				{Batch: s.prepareBatch(107, 108, 109, 110), FromSeqID: 107, LastExportedSeqID: 110,
					WriteDuration: slowOperation, ReadDuration: slowOperation},
				{Batch: s.prepareBatch(111), FromSeqID: 111, LastExportedSeqID: dontExport,
					WriteDuration: fastOperation, ReadDuration: slowOperation},
			},
			// 2 долгих записи, 3 быстрых чтения. Общее время упирается в запись.
		},
	}

	for _, c := range cases {
		c := c
		s.Run(c.Name, func() {
			ctrl := gomock.NewController(s.T())
			defer ctrl.Finish()
			dbStorageMock := storagemock.NewMockLbExporterStorage(ctrl)
			lbStorageMock := lbmock.NewMockExportStorage(ctrl)

			ctx, cancel := context.WithCancel(s.ctx)
			defer cancel()

			for i, it := range c.Iterations {
				i := i
				currIter := it
				dbStorageMock.EXPECT().GetEventExportBatch(
					gomock.Any(),
					gomock.Any(),
					currIter.FromSeqID,
					int64(batchLimit),
				).DoAndReturn(func(
					ctx context.Context,
					accountSettings entitysettings.AccountSettings,
					fromID int64,
					limit int64,
				) (*entities.LbExportBatch, error) {
					time.Sleep(currIter.ReadDuration)
					if i == len(c.Iterations)-1 {
						cancel()
					}
					return currIter.Batch, nil
				})

				if currIter.LastExportedSeqID != dontExport {
					lbStorageMock.EXPECT().WriteEventsBatch(gomock.Any(), currIter.Batch).
						DoAndReturn(func(ctx context.Context, batch *entities.LbExportBatch) error {
							time.Sleep(currIter.WriteDuration)
							return nil
						})
					dbStorageMock.EXPECT().UpdateLastExportedEventSeqID(
						gomock.Any(),
						currIter.LastExportedSeqID,
					).Return(nil)
				}
			}

			config := &core.Config{
				LbExport: core.LbExportConfig{
					BatchSize:    int64(batchLimit),
					IdleTime:     slowOperation,
					SwitchPeriod: time.Second,
				},
			}

			exporter := NewShardExporter(
				config,
				1,
				dbStorageMock,
				lbStorageMock,
				s.entitySettings,
				int64(c.LastExportedSeqID),
				nil,
			)

			s.Assert().NoError(exporter.Start(ctx), "exporter must not return error")
		})
	}
}
