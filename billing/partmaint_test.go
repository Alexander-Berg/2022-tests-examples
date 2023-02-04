package impl

import (
	"fmt"
	"testing"
	"time"

	"github.com/golang/mock/gomock"
	"github.com/stretchr/testify/suite"

	"a.yandex-team.ru/billing/hot/accounts/mock/storagemock"
	"a.yandex-team.ru/billing/hot/accounts/pkg/core"
	"a.yandex-team.ru/billing/hot/accounts/pkg/core/entities"
	corepart "a.yandex-team.ru/billing/hot/accounts/pkg/core/partitioning"
)

var Now = time.Date(2000, 1, 15, 0, 0, 0, 0, time.UTC)

type PartitionsMaintenanceTestSuite struct {
	ActionTestSuite
}

func TestPartitionsMaintenanceTestSuite(t *testing.T) {
	suite.Run(t, new(PartitionsMaintenanceTestSuite))
}

func (s *PartitionsMaintenanceTestSuite) TestGetTablePartitions() {
	ctrl := gomock.NewController(s.T())
	defer ctrl.Finish()

	partitioningStorageMock := storagemock.NewMockPartitioningStorage(ctrl)
	partitioningStorageMock.EXPECT().
		GetTablePartitions(gomock.Any(), "x").
		Return([]string{"tp_x_20000115_20000120", "tp_x_20000120_20000122"}, nil)

	res, err := getTablePartitions(s.ctx, partitioningStorageMock, "x")
	s.Require().NoError(err)
	s.Assert().Equal(
		[]TablePartition{
			{Name: "acc.tp_x_20000115_20000120", FromDT: Now, ToDT: Now.AddDate(0, 0, 5)},
			{Name: "acc.tp_x_20000120_20000122",
				FromDT: Now.AddDate(0, 0, 5),
				ToDT:   Now.AddDate(0, 0, 7)},
		}, res)
}

func (s *PartitionsMaintenanceTestSuite) TestGetLastProcessedEventID() {
	ctrl := gomock.NewController(s.T())
	defer ctrl.Finish()

	var randomInt64 int64 = 64

	var tests = []struct {
		lastExportedSeqID, lastRollupID, expectedRes int64
	}{
		{lastExportedSeqID: 1, lastRollupID: 3, expectedRes: 1},
		{lastExportedSeqID: 2, lastRollupID: 1, expectedRes: 1},
	}

	for _, tt := range tests {
		tt := tt
		testName := fmt.Sprintf("%d,%d", tt.lastExportedSeqID, tt.lastRollupID)
		s.Run(testName, func() {
			shardMock := storagemock.NewMockShard(ctrl)

			lbExporterStorageMock := storagemock.NewMockLbExporterStorage(ctrl)
			lbExporterStorageMock.EXPECT().GetLastExportedEventSeqID(gomock.Any()).
				Return(tt.lastExportedSeqID, nil)
			shardMock.EXPECT().GetLbExporterStorage().Return(lbExporterStorageMock)

			rollupStorageMock := storagemock.NewMockRollupStorage(ctrl)
			rollupStorageMock.EXPECT().GetRollupBounds(gomock.Any()).
				Return(tt.lastRollupID, randomInt64, nil)
			shardMock.EXPECT().GetRollupStorage(gomock.Nil()).
				Return(rollupStorageMock)

			res, _ := getLastProcessedEventID(s.ctx, nil, shardMock)
			s.Assert().Equal(tt.expectedRes, res)
		})
	}

}

func (s *PartitionsMaintenanceTestSuite) TestProcessShard() {
	ctrl := gomock.NewController(s.T())
	defer ctrl.Finish()

	shardStorageMock := storagemock.NewMockShardStorage(ctrl)

	cfg := core.Config{}
	taskConfig := entities.PartitionsMaintenanceTaskConfig{
		PartitionSize:   2,
		PrevDaysToKeep:  1,
		FutureDaysCount: 2,
	}
	pm := NewPartitionsMaintainer(shardStorageMock, &cfg, nil)

	var shardID int64 = 666
	shardMock := storagemock.NewMockShard(ctrl)
	shardStorageMock.EXPECT().GetShardByID(shardID).Return(shardMock, nil)

	partitioningStorageMock := storagemock.NewMockPartitioningStorage(ctrl)

	gomock.InOrder(
		partitioningStorageMock.EXPECT().
			CreatePartition(gomock.Any(), "event",
				Now, Now.AddDate(0, 0, 2)).
			Return(nil),
		partitioningStorageMock.EXPECT().
			CreatePartition(gomock.Any(), "event",
				Now.AddDate(0, 0, 2), Now.AddDate(0, 0, 4)).
			Return(nil))

	dropTimeFrom, _ := time.Parse(corepart.PartitionNameDateFormat, "20000111")

	gomock.InOrder(
		partitioningStorageMock.EXPECT().
			GetTablePartitions(gomock.Any(), "event").Return(
			[]string{"tp_event_20000111_20000112", "tp_event_20000112_20000113", "tp_event_20000113_20000114",
				"tp_event_20000114_20000115"}, nil),
		partitioningStorageMock.EXPECT().
			GetTablePartitions(gomock.Any(), "state_event").Return(
			[]string{"tp_state_event_20000110_20000114", "tp_state_event_20000114_20000115",
				"tp_state_event_20000115_20000116", "tp_state_event_20000116_20000117",
				"tp_state_event_20000117_20000118"}, nil),
		partitioningStorageMock.EXPECT().
			GetTablePartitions(gomock.Any(), "event").Return(
			[]string{"tp_event_20000111_20000112", "tp_event_20000112_20000113", "tp_event_20000113_20000114",
				"tp_event_20000114_20000115"}, nil),
		partitioningStorageMock.EXPECT().
			DropOldRollups(gomock.Any(), dropTimeFrom).Return(nil),
	)

	gomock.InOrder(
		partitioningStorageMock.EXPECT().
			ProcessOldPartition(gomock.Any(),
				"acc.tp_event_20000111_20000112", "t_event", gomock.Any()).
			Return(true, nil),
		partitioningStorageMock.EXPECT().
			ProcessOldPartition(gomock.Any(),
				"acc.tp_event_20000112_20000113", "t_event", gomock.Any()).
			Return(false, nil),
		partitioningStorageMock.EXPECT().
			ProcessOldPartition(gomock.Any(),
				"acc.tp_state_event_20000110_20000114", "t_state_event", gomock.Any()).
			Return(true, nil))

	shardMock.EXPECT().GetPartitioningStorage().Return(partitioningStorageMock)

	var int64Num int64 = 5
	lbExporterStorageMock := storagemock.NewMockLbExporterStorage(ctrl)
	lbExporterStorageMock.EXPECT().GetLastExportedEventSeqID(gomock.Any()).Return(int64Num, nil)
	shardMock.EXPECT().GetLbExporterStorage().Return(lbExporterStorageMock)

	rollupStorageMock := storagemock.NewMockRollupStorage(ctrl)
	rollupStorageMock.EXPECT().GetRollupBounds(gomock.Any()).Return(int64Num, int64Num, nil)
	shardMock.EXPECT().GetRollupStorage(gomock.Nil()).Return(rollupStorageMock)

	err := pm.MaintainPartitions(s.ctx, shardID, taskConfig, Now)

	s.Assert().NoError(err)
}
