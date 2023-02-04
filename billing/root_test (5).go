package lbexporter

import (
	"context"
	"os"
	"testing"
	"time"

	"github.com/golang/mock/gomock"
	"github.com/stretchr/testify/suite"

	"a.yandex-team.ru/billing/hot/accounts/mock/lbmock"
	"a.yandex-team.ru/billing/hot/accounts/mock/storagemock"
	"a.yandex-team.ru/billing/hot/accounts/pkg/core"
	"a.yandex-team.ru/billing/hot/accounts/pkg/core/entities"
	"a.yandex-team.ru/billing/hot/accounts/pkg/core/entitysettings"
	"a.yandex-team.ru/billing/hot/accounts/pkg/core/entitysettings/impl"
	"a.yandex-team.ru/billing/hot/accounts/pkg/storage"
	"a.yandex-team.ru/billing/library/go/billingo/pkg/xlog"
	"a.yandex-team.ru/kikimr/public/sdk/go/persqueue"
	"a.yandex-team.ru/library/go/core/log"
	zaplog "a.yandex-team.ru/library/go/core/log/zap"
	"a.yandex-team.ru/library/go/test/yatest"
)

type BaseTestSuite struct {
	suite.Suite
	ctx            context.Context
	entitySettings entitysettings.Settings
}

func (s *BaseTestSuite) setupAccountsSettings(ctx context.Context) entitysettings.Settings {
	settingsPath, ok := os.LookupEnv("ACCOUNTS_SETTINGS")
	if !ok {
		// todo-igogor можно наверное написать свои settings для этих тестов, чтобы не зависеть от глобальных
		settingsPath = yatest.SourcePath("billing/hot/accounts/configs/settings/dev.yaml")
	}

	settings, err := impl.NewSettings(ctx, settingsPath)
	s.Require().NoError(err)
	return settings
}

func (s *BaseTestSuite) setupLogger() {
	logger, err := zaplog.NewDeployLogger(log.DebugLevel)
	s.Require().NoError(err)
	xlog.SetGlobalLogger(logger)
}

func (s *BaseTestSuite) setupContext() context.Context {
	return context.Background()
}

func (s *BaseTestSuite) SetupTest() {
	s.setupLogger()
	s.ctx = s.setupContext()
	s.entitySettings = s.setupAccountsSettings(s.ctx)
}

type RootTestSuite struct {
	BaseTestSuite
}

func TestInitTestSuite(t *testing.T) {
	suite.Run(t, new(RootTestSuite))
}

func (s *RootTestSuite) TestGetSourceID_NotInDB() {
	ctrl := gomock.NewController(s.T())
	defer ctrl.Finish()
	dbStorageMock := storagemock.NewMockLbExporterStorage(ctrl)

	dbStorageMock.EXPECT().GetSourceID(gomock.Any()).Return("", nil)
	dbStorageMock.EXPECT().UpdateEventSourceID(gomock.Any(), gomock.AssignableToTypeOf("")).Return(nil)

	source, err := getSourceID(s.ctx, dbStorageMock)
	s.Assert().NoError(err, "must be no error")
	s.Assert().NotEqual("", source, "sourceID must not be empty")
}

func (s *RootTestSuite) TestGetSourceID_InDB() {
	ctrl := gomock.NewController(s.T())
	defer ctrl.Finish()
	dbStorageMock := storagemock.NewMockLbExporterStorage(ctrl)

	storedSourceID := "some_source_id"
	dbStorageMock.EXPECT().GetSourceID(gomock.Any()).Return(storedSourceID, nil)

	source, err := getSourceID(s.ctx, dbStorageMock)
	s.Assert().NoError(err, "must be no error")
	s.Assert().Equal(storedSourceID, source, "sourceID must be taken from db")
}

func (s *RootTestSuite) TestGetLastExportedSeqID_FromLB() {
	ctrl := gomock.NewController(s.T())
	defer ctrl.Finish()
	dbStorageMock := storagemock.NewMockLbExporterStorage(ctrl)

	maxSeqNo := uint64(1)
	seqID, err := getLastExportedSeqID(s.ctx, dbStorageMock, maxSeqNo)
	s.Assert().NoError(err, "must be no error")
	s.Assert().Equal(int64(maxSeqNo), seqID, "seqID must be taken from lb")
}

func (s *RootTestSuite) TestGetLastExportedSeqID_FromDB() {
	ctrl := gomock.NewController(s.T())
	defer ctrl.Finish()
	dbStorageMock := storagemock.NewMockLbExporterStorage(ctrl)
	expectedSeqID := int64(10)
	dbStorageMock.EXPECT().GetLastExportedEventSeqID(gomock.Any()).Return(expectedSeqID, nil)

	seqID, err := getLastExportedSeqID(s.ctx, dbStorageMock, 0)
	s.Assert().NoError(err, "must be no error")
	s.Assert().Equal(expectedSeqID, seqID, "seqID must be taken from lb")
}

func (s *RootTestSuite) TestTryProcessShard_Locked() {
	ctrl := gomock.NewController(s.T())
	defer ctrl.Finish()

	dbStorageMock := storagemock.NewMockLbExporterStorage(ctrl)

	shardMock := storagemock.NewMockShard(ctrl)
	shardMock.EXPECT().GetLbExporterStorage().Return(dbStorageMock)

	shardStorageMock := storagemock.NewMockShardStorage(ctrl)
	var shardID int64 = 666
	shardStorageMock.EXPECT().GetShardByID(shardID).Return(shardMock, nil)

	config := &core.Config{
		LbExport: core.LbExportConfig{
			LockID: 111,
		},
	}

	lock := storage.NewBackgroundTryLock()
	lock.SetStatus(false)
	dbStorageMock.EXPECT().TryAdvisoryBackgroundLock(gomock.Any(), config.LbExport.LockID).Return(lock)

	p := NewLbExportProcessor(shardStorageMock, nil, config, s.entitySettings, nil)
	err := p.tryProcessShard(s.ctx, shardID)
	s.Assert().NoError(err, "must not return error on locked shard")
	select {
	case <-lock.Released():
	default:
		s.Fail("lock must be released")
	}
}

func (s *RootTestSuite) TestTryProcessShard_Unlocked() {
	ctrl := gomock.NewController(s.T())
	defer ctrl.Finish()

	shardStorageMock := storagemock.NewMockShardStorage(ctrl)

	var shardID int64 = 666
	shardMock := storagemock.NewMockShard(ctrl)
	shardStorageMock.EXPECT().GetShardByID(shardID).Return(shardMock, nil)

	dbStorageMock := storagemock.NewMockLbExporterStorage(ctrl)
	shardMock.EXPECT().GetLbExporterStorage().Return(dbStorageMock)

	lbStoragesMock := lbmock.NewMockStorages(ctrl)

	lbStorageMock := lbmock.NewMockExportStorage(ctrl)
	lbStoragesMock.EXPECT().GetExportStorage(gomock.Any(), gomock.Any(), gomock.Any()).Return(lbStorageMock, nil)

	config := &core.Config{
		LbExport: core.LbExportConfig{
			LockID:       111,
			SwitchPeriod: time.Second,
			BatchSize:    10,
		},
	}

	lock := storage.NewBackgroundTryLock()
	lock.SetStatus(true)
	dbStorageMock.EXPECT().TryAdvisoryBackgroundLock(gomock.Any(), config.LbExport.LockID).Return(lock)

	dbStorageMock.EXPECT().GetSourceID(gomock.Any()).Return("some_source_id", nil)

	maxSeqNo := uint64(10)
	lbStorageMock.EXPECT().Start(gomock.Any()).Return(&persqueue.WriterInit{MaxSeqNo: maxSeqNo}, nil)

	ctx, cancel := context.WithCancel(s.ctx)
	defer cancel()

	// по этому вызову проверяем что экспортер запустился
	dbStorageMock.EXPECT().GetEventExportBatch(
		gomock.Any(),
		gomock.Any(),
		int64(maxSeqNo+1),
		config.LbExport.BatchSize,
	).DoAndReturn(func(
		ctx context.Context,
		accountSettings entitysettings.AccountSettings,
		fromID int64,
		limit int64,
	) (*entities.LbExportBatch, error) {
		cancel()
		return entities.NewLbExportBatch([]entities.LbExportEntry{}, time.Now()), nil
	})
	lbStorageMock.EXPECT().Disconnect().Return(nil)

	p := NewLbExportProcessor(shardStorageMock, lbStoragesMock, config, s.entitySettings, nil)
	err := p.tryProcessShard(ctx, shardID)
	s.Assert().NoError(err, "must not return error on locked shard")
	select {
	case <-lock.Released():
	default:
		s.Fail("lock must be released")
	}
}

// igogor: закомментированные тесты ненадежны, но для дебага оставлю.
//func (s *RootTestSuite) TestWorker_AllLocked() {
//	ctrl := gomock.NewController(s.T())
//	defer ctrl.Finish()
//
//	s.ctx.Config = &core.Config{
//		LbExport: core.LbExportConfig{
//			LockID:      111,
//			SwitchDelay: time.Millisecond * 10,
//		},
//	}
//
//	shardStorageMock := mock.NewMockShardStorage(ctrl)
//
//	iterations := 2
//	shardIDs := []int64{1, 2}
//	locks := make([]*storage.BackgroundTryLock, 0, len(shardIDs))
//	for _, shardID := range shardIDs {
//		shardMock := mock.NewMockShard(ctrl)
//		dbStorageMock := mock.NewMockLbExporterStorage(ctrl)
//
//		for i := 0; i < iterations; i++ {
//			shardStorageMock.EXPECT().GetShardByID(shardID).Return(shardMock, nil)
//			shardMock.EXPECT().GetLbExporterStorage().Return(dbStorageMock)
//
//			lock := storage.NewBackgroundTryLock()
//			lock.SetStatus(false)
//			dbStorageMock.EXPECT().TryAdvisoryBackgroundLock(gomock.Any(), s.ctx.Config.LbExport.LockID).Return(lock)
//
//			locks = append(locks, lock)
//		}
//	}
//	s.ctx.Shards = shardStorageMock
//
//	//goland:noinspection GoLostCancel
//	tCtx, cancel := context.WithTimeout(
//		s.ctx.Parent(),
//		s.ctx.Config.LbExport.SwitchDelay*time.Duration(iterations),
//	)
//	defer cancel()
//	s.ctx.SetParent(tCtx)
//
//	go worker(s.ctx, 1, shardIDs)
//	<-tCtx.Done()
//	for _, lock := range locks {
//		select {
//		case <-lock.Released():
//		default:
//			s.Fail("lock must be released")
//		}
//	}
//}
//
//func (s *RootTestSuite) TestWorker_AllUnlocked() {
//	ctrl := gomock.NewController(s.T())
//	defer ctrl.Finish()
//
//	s.ctx.Config = &core.Config{
//		LbExport: core.LbExportConfig{
//			LockID:       111,
//			SwitchDelay:  time.Millisecond * 10,
//			SwitchPeriod: time.Millisecond,
//			BatchSize:    10,
//		},
//	}
//
//	shardStorageMock := mock.NewMockShardStorage(ctrl)
//
//	lbStoragesMock := lbmock.NewMockStorages(ctrl)
//
//	iterations := 2
//	shardIDs := []int64{1, 2}
//	locks := make([]*storage.BackgroundTryLock, 0, len(shardIDs))
//	for _, shardID := range shardIDs {
//		shardMock := mock.NewMockShard(ctrl)
//		dbStorageMock := mock.NewMockLbExporterStorage(ctrl)
//		lbStorageMock := lbmock.NewMockExportStorage(ctrl)
//
//		for i := 0; i < iterations; i++ {
//			shardStorageMock.EXPECT().GetShardByID(shardID).Return(shardMock, nil)
//			shardMock.EXPECT().GetLbExporterStorage().Return(dbStorageMock)
//
//			lock := storage.NewBackgroundTryLock()
//			lock.SetStatus(true)
//			dbStorageMock.EXPECT().TryAdvisoryBackgroundLock(gomock.Any(), s.ctx.Config.LbExport.LockID).Return(lock)
//
//			locks = append(locks, lock)
//
//			lbStoragesMock.EXPECT().GetExportStorage(gomock.Any(), gomock.Any(), gomock.Any()).
//				Return(lbStorageMock, nil)
//
//			dbStorageMock.EXPECT().GetSourceID(gomock.Any()).Return("some_source_id", nil)
//
//			maxSeqNo := uint64(10)
//			lbStorageMock.EXPECT().Start(gomock.Any()).Return(&persqueue.WriterInit{MaxSeqNo: maxSeqNo}, nil)
//
//			// по этому вызову проверяем что экспортер запустился
//			dbStorageMock.EXPECT().GetEventExportBatch(
//				gomock.Any(),
//				gomock.Any(),
//				int64(maxSeqNo+1),
//				s.ctx.Config.LbExport.BatchSize,
//			).DoAndReturn(func(
//				ctx context.Context,
//				accountSettings entitysettings.AccountSettings,
//				fromID int64,
//				limit int64,
//			) (*entities.LbExportBatch, error) {
//				time.Sleep(time.Millisecond)
//				return entities.NewLbExportBatch([]entities.LbExportEntry{}, time.Now()), nil
//			})
//			lbStorageMock.EXPECT().Disconnect().Return(nil)
//		}
//	}
//	s.ctx.Shards = shardStorageMock
//	s.ctx.Logbroker = lbStoragesMock
//
//	tCtx, cancel := context.WithTimeout(
//		s.ctx.Parent(),
//		s.ctx.Config.LbExport.SwitchDelay*time.Duration(iterations),
//	)
//	defer cancel()
//	s.ctx.SetParent(tCtx)
//
//	go worker(s.ctx, 1, shardIDs)
//	<-tCtx.Done()
//	for _, lock := range locks {
//		select {
//		case <-lock.Released():
//		default:
//			s.Fail("lock must be released")
//		}
//	}
//}

// igogor: хотелось бы еще тест где 3 воркера ковыряют 2 шарда. Но чет не хватило ума его написать.
