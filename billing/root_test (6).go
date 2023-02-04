package sequencer

import (
	"context"
	"sync"
	"testing"
	"time"

	"github.com/golang/mock/gomock"
	"github.com/stretchr/testify/assert"

	"a.yandex-team.ru/billing/hot/accounts/mock/actionsmock"
	"a.yandex-team.ru/billing/hot/accounts/mock/storagemock"
	"a.yandex-team.ru/billing/hot/accounts/pkg/core"
	"a.yandex-team.ru/billing/hot/accounts/pkg/core/actions/impl"
	"a.yandex-team.ru/billing/hot/accounts/pkg/core/errors"
	"a.yandex-team.ru/billing/hot/accounts/pkg/templates"
	"a.yandex-team.ru/billing/library/go/billingo/pkg/xlog"
	"a.yandex-team.ru/library/go/core/log"
	"a.yandex-team.ru/library/go/core/log/zap"
)

func init() {
	logger, _ := zap.NewDeployLogger(log.DebugLevel)
	xlog.SetGlobalLogger(logger)
}

func TestShardAlreadyLockedWithActionMock(t *testing.T) {
	ctrl := gomock.NewController(t)
	defer ctrl.Finish()

	aggregates := actionsmock.NewMockSequencer(ctrl)

	ctx := context.Background()

	processor := &Sequencer{
		actions: aggregates,
	}

	aggregates.EXPECT().UpdateEventSeqID(
		gomock.Eq(ctx),
		gomock.Eq(int64(-1)),
	).Return(int64(0), true, nil)

	result := processor.updateShard(ctx, -1)
	assert.Equal(t, ShardAlreadyLocked, result)
}

func createSequencer(ctx context.Context, ctrl *gomock.Controller, rows int64,
	locked bool, err error, getShardsTimes int, getShardTimes int) *Sequencer {

	shards := storagemock.NewMockShardStorage(ctrl)
	shard := storagemock.NewMockShard(ctrl)
	sequencerStorage := storagemock.NewMockSequencerStorage(ctrl)

	config := core.SequencerConfig{
		UpdateLimit: 30000,
		LockID:      13,
		MaxProcess:  5,
		IdleTimeout: 5 * time.Second,
	}

	tmpl := templates.NewCache()
	sequencerActions := impl.NewSequencerActions(shards, config, tmpl)

	shards.EXPECT().GetShardIDs().Times(getShardsTimes).Return([]int64{666}, nil)
	shards.EXPECT().GetShardByID(gomock.Eq(int64(666))).Times(getShardTimes).Return(shard, nil)
	shard.EXPECT().GetSequencerStorage(tmpl).Return(sequencerStorage)
	sequencerStorage.EXPECT().UpdateEventSeqID(gomock.Eq(ctx), gomock.Eq(config)).Return(rows, locked, err)

	return NewSequencer(config, shards, sequencerActions, nil)
}

func checkShard(t *testing.T, rows int64, locked bool, err error, expectedResult UpdateShardResult) {
	ctrl := gomock.NewController(t)
	defer ctrl.Finish()

	ctx := context.Background()
	p := createSequencer(ctx, ctrl, rows, locked, err, 0, 1)

	result := p.updateShard(ctx, 666)
	assert.Equal(t, expectedResult, result)
}

func TestShardAlreadyLockedWithDatabaseMock(t *testing.T) {
	checkShard(t, int64(0), true, nil, ShardAlreadyLocked)
}

func TestLessThanExpected(t *testing.T) {
	checkShard(t, int64(10), false, nil, UpdateShardSuccess)
}

func TestShardUpdateError(t *testing.T) {
	checkShard(t, int64(0), false, errors.DatabaseError("FAIL"), UpdateShardError)
}

func TestMainExecution(t *testing.T) {
	ctrl := gomock.NewController(t)
	defer ctrl.Finish()

	var wg sync.WaitGroup

	wg.Add(1)
	ctx, cancel := context.WithCancel(context.Background())
	defer cancel()

	p := createSequencer(ctx, ctrl, 0, true, nil, 1, 1)
	shardIDs, err := p.shards.GetShardIDs()
	if err != nil {
		t.Fatal(err)
	}

	shardInfos := make([]*shardInfo, 0, len(shardIDs))
	for _, id := range shardIDs {
		shardInfos = append(shardInfos, &shardInfo{id: id})
	}

	go func() {
		defer wg.Done()
		p.main(ctx, shardInfos)
	}()

	time.AfterFunc(time.Second, cancel)

	c := make(chan struct{})

	go func() {
		defer close(c)
		wg.Wait()
	}()

	select {
	case <-c:
		return
	case <-time.After(2 * time.Second):
		t.Error("main not finished in expected time")
	}
}

func TestOnlyOneWorkerUpdatesAtTime(t *testing.T) {
	ctrl := gomock.NewController(t)
	defer ctrl.Finish()

	var wg sync.WaitGroup
	doneCh := make(chan struct{})
	inFuncCh := make(chan struct{})

	ctx, cancel := context.WithCancel(context.Background())
	defer cancel()

	shards := storagemock.NewMockShardStorage(ctrl)
	shard := storagemock.NewMockShard(ctrl)
	sequencerStorage := storagemock.NewMockSequencerStorage(ctrl)

	config := core.SequencerConfig{
		UpdateLimit: 30000,
		LockID:      13,
		MaxProcess:  10,
		IdleTimeout: 10 * time.Second,
	}

	tmpl := templates.NewCache()
	sequencerActions := impl.NewSequencerActions(shards, config, tmpl)

	shards.EXPECT().GetShardByID(gomock.Eq(int64(666))).Return(shard, nil)
	shard.EXPECT().GetSequencerStorage(tmpl).Return(sequencerStorage)
	sequencerStorage.EXPECT().UpdateEventSeqID(gomock.Eq(ctx), gomock.Eq(config)).
		DoAndReturn(func(context.Context, core.SequencerConfig) (int64, bool, error) {
			close(inFuncCh)
			<-doneCh
			return 1, false, nil
		})

	p := NewSequencer(config, shards, sequencerActions, nil)
	shardInfos := []*shardInfo{{id: 666}}

	// Run first worker to lock the shard
	var allWg sync.WaitGroup
	allWg.Add(1)
	go func() {
		defer allWg.Done()
		p.main(ctx, shardInfos)
	}()

	// Wait for shard lock
	select {
	case <-inFuncCh:
	case <-time.After(time.Second):
		t.Error("main not finished in expected time")
	}

	// Start other workers
	for i := 1; i < config.MaxProcess; i++ {
		wg.Add(1)
		go func() {
			defer wg.Done()
			p.main(ctx, shardInfos)
		}()
	}

	time.AfterFunc(100*time.Millisecond, cancel)

	go func() {
		defer close(doneCh)
		wg.Wait()
	}()

	allDoneCh := make(chan struct{})
	go func() {
		defer close(allDoneCh)
		allWg.Wait()
	}()

	// Wait for all but one workers to skip updating shards because of local lock
	select {
	case <-doneCh:
	case <-time.After(time.Second):
		t.Error("main not finished in expected time")
	}

	// Wait for the last worker
	select {
	case <-allDoneCh:
	case <-time.After(time.Second):
		t.Error("main not finished in expected time")
	}
}
