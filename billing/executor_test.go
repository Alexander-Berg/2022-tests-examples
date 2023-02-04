package runtime

import (
	"context"
	"strconv"
	"sync/atomic"
	"testing"
	"time"

	"github.com/golang/mock/gomock"
	"github.com/stretchr/testify/suite"

	"a.yandex-team.ru/billing/configshop/pkg/core/entities"
	"a.yandex-team.ru/billing/configshop/pkg/core/entities/runtime"
	"a.yandex-team.ru/billing/configshop/pkg/core/entities/template"
	"a.yandex-team.ru/billing/configshop/pkg/core/entities/tplops"
	"a.yandex-team.ru/billing/configshop/pkg/core/runtime/controllers"
	"a.yandex-team.ru/billing/configshop/pkg/core/runtime/mock"
	mock2 "a.yandex-team.ru/billing/configshop/pkg/core/template/mock"
	"a.yandex-team.ru/billing/configshop/pkg/storage"
	"a.yandex-team.ru/billing/configshop/pkg/storage/db"
	"a.yandex-team.ru/billing/configshop/pkg/storage/memory"
	template2 "a.yandex-team.ru/billing/configshop/pkg/storage/template"
	storagetest "a.yandex-team.ru/billing/configshop/pkg/storage/xtest"
	configopstype "a.yandex-team.ru/billing/library/go/billingo/pkg/configops/type"
	bsql "a.yandex-team.ru/billing/library/go/billingo/pkg/storage/sql"
	"a.yandex-team.ru/billing/library/go/billingo/pkg/xlog"
	log2 "a.yandex-team.ru/library/go/core/log"
	zaplog "a.yandex-team.ru/library/go/core/log/zap"
	"a.yandex-team.ru/library/go/core/xerrors"
)

func (s *ExecutorTestSuite) TestExecuteFail() {
	ctx := context.Background()
	graphID := s.setupGraph1(1)
	s.setupGraph1HappyPath(true)
	s.setupGraph1HappySchedule(true)

	err := s.Executor.PrepareGraph(ctx, graphID)
	s.Require().NoError(err)

	int1, err := s.Storage.GetBlockByName(ctx, graphID, "int1")
	s.Require().NoError(err)

	int1.Inputs[entities.VarName{Block: entities.GlobalInputBlockName, Name: "inp1"}] = []any{"var1"}
	s.Require().NoError(s.Executor.graphManager.SaveBlock(ctx, int1))
	id := int1.ID
	s.Require().NoError(s.Executor.graphManager.ChangeBlockStatusTo(ctx, id, graphID, runtime.BlockStatusPrepared))

	s.Require().NoError(s.Executor.graphManager.ChangeGraphStatusTo(ctx, graphID, runtime.GraphStatusExecuting))

	err = s.Executor.ExecuteBlock(ctx, id)
	if !s.Assert().Error(err) {
		errs, err := s.Storage.GetErrors(ctx, graphID)
		s.Require().NoError(err)
		s.FailNow("failed blocks", errs)
	}
	s.Assert().Contains(err.Error(), "failed executing block")

	block, err := s.Executor.graphManager.GetBlock(ctx, id)
	s.Require().NoError(err)
	s.Assert().Equal(runtime.BlockStatusPrepared, block.Status)

	err = s.Executor.ExecuteBlock(ctx, id)
	s.Require().Error(err)
	s.Assert().Contains(err.Error(), "scheduling deps update")

	block, err = s.Executor.graphManager.GetBlock(ctx, id)
	s.Require().NoError(err)
	s.Assert().Equal(runtime.BlockStatusPrepared, block.Status)

	// Should be 4 errors in total
	for i := 0; i < 2; i++ {
		err = s.Executor.ExecuteBlock(ctx, id)
		s.Require().Error(err)
	}

	err = s.Executor.ExecuteBlock(ctx, id)
	s.Require().NoError(err)

	block, err = s.Executor.graphManager.GetBlock(ctx, id)
	s.Require().NoError(err)
	s.Assert().Equal(runtime.BlockStatusDone, block.Status, "execute synchronously finishes block")

	err = s.Executor.ExecuteBlock(ctx, id)
	s.Require().NoError(err)
}

func (s *ExecutorTestSuite) TestUpdateDeps() {
	ctx := context.Background()
	graphID := s.setupGraph1(1)
	s.setupGraph1HappyPath(true)
	s.setupGraph1HappySchedule(true)

	s.Require().NoError(s.Executor.PrepareGraph(ctx, graphID))

	int1, err := s.Storage.GetBlockByName(ctx, graphID, "int1")
	s.Require().NoError(err)

	int1.State = map[string]any{
		"int1out1": 1, "int1out2": 2,
	}
	s.Require().NoError(s.Executor.graphManager.SaveBlock(ctx, int1))
	id := int1.ID

	err = s.Executor.UpdateDeps(ctx, id)
	s.Require().Error(err)
	s.Assert().Contains(err.Error(), "'new' graph")

	s.Require().NoError(s.Executor.graphManager.ChangeGraphStatusTo(ctx, graphID, runtime.GraphStatusExecuting))

	// Expect 4 errors in total.
	for i := 0; i < 4; i++ {
		err = s.Executor.UpdateDeps(ctx, id)
		s.Require().Error(err)
	}

	// Idempotency is everything.
	for i := 0; i < 2; i++ {
		err = s.Executor.UpdateDeps(ctx, id)
		s.Require().NoError(err)
	}
}

func (s *ExecutorTestSuite) TestFinishBlockFail() {
	ctx := context.Background()
	graphID := s.setupGraph1(1)
	s.setupGraph1HappyPath(true)
	s.setupGraph1HappySchedule(true)

	s.Require().NoError(s.Executor.PrepareGraph(ctx, graphID))

	int2, err := s.Storage.GetBlockByName(ctx, graphID, "int2")
	s.Require().NoError(err)
	id := int2.ID

	err = s.Executor.FinishBlock(ctx, runtime.ExecuteOutput{
		BlockID: id,
		State:   nil,
		Error:   runtime.NewBlockError(runtime.ErrorCodeInternal, map[string]any{"error": "oi oi oi"}),
	})
	s.Require().NoError(err)

	status, err := s.Executor.graphManager.GetGraphStatus(ctx, graphID)
	s.Require().NoError(err)
	s.Assert().Equal(runtime.GraphStatusFailed, status)

	block, err := s.Executor.graphManager.GetBlock(ctx, id)
	s.Require().NoError(err)
	s.Assert().Equal(runtime.BlockStatusFailed, block.Status)
	s.Assert().Equal("oi oi oi", block.Error.Args["error"].(string))

	err = s.Executor.FinishBlock(ctx, runtime.ExecuteOutput{
		BlockID: id,
		State:   nil,
		Error:   nil,
	})
	s.Require().NoError(err)

	block, err = s.Executor.graphManager.GetBlock(ctx, id)
	s.Require().NoError(err)
	s.Assert().Equal(runtime.BlockStatusFailed, block.Status)
}

func (s *ExecutorTestSuite) TestGraphHappyPath() {
	s.testHappyPath(10)
}

func (s *ExecutorTestSuite) TestGraphHappyPathZeroRepeated() {
	s.testHappyPath(0)
}

func (s *ExecutorTestSuite) testHappyPath(numRepeatedBlocks int) {
	ctx := context.Background()
	graphID := s.setupGraph1(numRepeatedBlocks)
	s.setupGraph1HappyPath(false)
	s.setupGraph1HappySchedule(false)

	s.Require().NoError(s.Executor.PrepareGraph(ctx, graphID))

	status, err := s.Executor.graphManager.GetGraphStatus(ctx, graphID)
	s.Require().NoError(err)
	s.Assert().Equal(runtime.GraphStatusNew, status)

	ctx, cancel := context.WithTimeout(ctx, 20*time.Second)
	defer cancel()

	if !s.Assert().NoError(s.Executor.StartGraph(ctx, graphID, s.graph1GlobalInputs(numRepeatedBlocks))) {
		errs, err := s.Storage.GetErrors(context.Background(), graphID)
		s.Require().NoError(err)
		s.FailNow("failed blocks", errs)
	}

	s.Require().NoError(s.Executor.wait(ctx))

	status, err = s.Executor.graphManager.GetGraphStatus(ctx, graphID)
	s.Require().NoError(err)
	s.Assert().Equal(runtime.GraphStatusDone, status)

	outputs, err := s.Executor.graphManager.GetOutputs(ctx, graphID)
	s.Require().NoError(err)
	s.Assert().Equal(s.graph1ExpectedOutputs(numRepeatedBlocks), outputs)
}

func (s *ExecutorTestSuite) setupGraph1HappySchedule(firstError bool) {
	var firstSuccess int32
	if firstError {
		firstSuccess++
	}

	s.BlockSchedulerMock.EXPECT().Schedule(gomock.Any(), gomock.Any()).
		DoAndReturn(func(ctx context.Context, blockID runtime.BlockID) error {
			if newVal := atomic.AddInt32(&firstSuccess, -1); newVal >= 0 {
				return xerrors.New("fail")
			}
			return s.Executor.ExecuteBlock(ctx, blockID)
		}).AnyTimes()

	var firstDepsSuccess int32
	if firstError {
		firstDepsSuccess++
	}

	s.BlockSchedulerMock.EXPECT().ScheduleDepsUpdate(gomock.Any(), gomock.Any()).
		DoAndReturn(func(ctx context.Context, blockID runtime.BlockID) error {
			if newVal := atomic.AddInt32(&firstDepsSuccess, -1); newVal >= 0 {
				return xerrors.New("fail")
			}
			return s.Executor.UpdateDeps(ctx, blockID)
		}).AnyTimes()

	s.BlockSchedulerMock.EXPECT().ScheduleFinish(gomock.Any(), gomock.Any()).
		DoAndReturn(func(ctx context.Context, output runtime.ExecuteOutput) error {
			return s.Executor.FinishBlock(ctx, output)
		}).MaxTimes(100) // Just avoid infinite recursion.
}

func (s *ExecutorTestSuite) setupGraph1HappyPath(firstError bool) {
	var firstSuccess int32
	if firstError {
		firstSuccess++
	}

	var firstCommonSuccess int32
	if firstError {
		firstCommonSuccess++
	}

	s.BlockManagerMock.EXPECT().GetBlockExecFunction(gomock.Any()).
		DoAndReturn(func(blockType string) (runtime.BlockExecFunc, error) {
			if blockType == "output" {
				return func(ctx context.Context, blk *runtime.Block, _ *runtime.GraphMeta) error {
					if newVal := atomic.AddInt32(&firstSuccess, -1); newVal >= 0 {
						return xerrors.New("fail")
					}

					return s.Executor.FinishBlock(ctx, runtime.ExecuteOutput{
						BlockID: blk.ID,
						State: map[string]any{
							"out1": blk.Inputs[entities.VarName{Block: "int1", Name: "int1out2"}],
							"out2": blk.Inputs[entities.VarName{Block: "int2", Name: "int2out1"}],
						},
						Error: nil,
					})
				}, nil
			} else if blockType == "integration" {
				return func(ctx context.Context, blk *runtime.Block, _ *runtime.GraphMeta) error {
					if newVal := atomic.AddInt32(&firstCommonSuccess, -1); newVal >= 0 {
						return xerrors.New("fail")
					}

					outputs := make(map[string]any)
					for _, outName := range blk.OutputNames {
						outputs[outName.Name] = outName.Name + ".value"
					}
					return s.Executor.FinishBlock(ctx, runtime.ExecuteOutput{
						BlockID: blk.ID,
						State:   outputs,
						Error:   nil,
					})
				}, nil
			}

			return nil, nil
		}).AnyTimes()
}

// To add more graphs need to move db initialization to callback.
func (s *ExecutorTestSuite) graph1GlobalInputs(numRepeated int) map[string]any {
	vals := make([]any, numRepeated)
	for i := range vals {
		vals[i] = "val" + strconv.Itoa(i)
	}

	return map[string]any{
		"inp1": vals,
		"inp2": 5,
		"inp3": "hello",
	}
}

func (s *ExecutorTestSuite) graph1ExpectedOutputs(numRepeated int) map[string]any {
	expectedOut1 := make([]any, numRepeated)
	for i := range expectedOut1 {
		expectedOut1[i] = "int1out2.value"
	}

	return map[string]any{
		"out1": expectedOut1,
		"out2": "int2out1.value",
	}
}

func (s *ExecutorTestSuite) setupGraph1(numRepeatedBlocks int) runtime.GraphID {
	const devEnv = "dev"
	graphID, err := s.Storage.CreateGraph(context.Background(), runtime.GraphMeta{
		ConfigurationVersion: s.ConfVersionID,
		Status:               runtime.GraphStatusNew,
		Env:                  devEnv,
	})
	s.Require().NoError(err)
	s.TemplateStorage.InitForRuntime(graphID, s.graph1Blocks(), s.graph1GlobalInputs(numRepeatedBlocks))
	return graphID
}

func (s *ExecutorTestSuite) graph1Blocks() []template.Block {
	return []template.Block{
		{
			Name: "int1",
			Type: "integration",
			Inputs: []tplops.InputValue{{Name: entities.VarName{Name: "inp1", Block: entities.GlobalInputBlockName},
				Type: configopstype.ValueTypeArray{ElementType: configopstype.ValueTypeAny}}},
			Outputs: []tplops.OutputValue{
				{Name: "int1out1", Type: configopstype.ValueTypeArray{ElementType: configopstype.ValueTypeAny}},
				{Name: "int1out2", Type: configopstype.ValueTypeArray{ElementType: configopstype.ValueTypeAny}},
			},
			InverseDeps: []entities.BlockName{"int2", "int3", "out1"},
			Repeat:      &entities.BlockRepeat{Var: entities.VarName{Name: "inp1", Block: entities.GlobalInputBlockName}},
		},
		{
			Name:        "int2",
			Type:        "integration",
			Inputs:      []tplops.InputValue{{Name: entities.VarName{Name: "int1out1", Block: "int1"}}},
			Outputs:     []tplops.OutputValue{{Name: "int2out1"}},
			Deps:        []entities.BlockName{"int1"},
			InverseDeps: []entities.BlockName{"int3", "out1"},
		},
		{
			Name:   "int3",
			Type:   "integration",
			Inputs: []tplops.InputValue{{Name: entities.VarName{Name: "int1out2", Block: "int1"}}},
			Deps:   []entities.BlockName{"int1", "int2"},
		},
		{
			Name: "out1",
			Type: "output",
			Inputs: []tplops.InputValue{
				{Name: entities.VarName{Name: "int1out2", Block: "int1"}},
				{Name: entities.VarName{Name: "int2out1", Block: "int2"}},
			},
			Outputs:  []tplops.OutputValue{{Name: "out1"}, {Name: "out2"}},
			Deps:     []entities.BlockName{"int1", "int2"},
			IsOutput: true,
		},
	}
}

type ExecutorTestSuite struct {
	suite.Suite
	GraphMock          *mock2.MockGraph
	BlockSchedulerMock *mock.MockBlockScheduler
	BlockManagerMock   *mock.MockBlockInstanceManager
	Factory            *mock.MockFactory
	GraphManager       *controllers.GraphManager
	Executor           *Executor
	ConfVersionID      int
	Storage            storage.RuntimeStorage
	TemplateStorage    *storagetest.MemoryTestTemplateStorage
}

func (s *ExecutorTestSuite) SetupTest() {
	ctrl := gomock.NewController(s.T())

	s.Factory = mock.NewMockFactory(ctrl)
	s.BlockSchedulerMock = mock.NewMockBlockScheduler(ctrl)
	s.BlockManagerMock = mock.NewMockBlockInstanceManager(ctrl)
	s.Factory.EXPECT().NewBlockScheduler().Return(s.BlockSchedulerMock)
	s.Factory.EXPECT().NewBlockInstanceManager().Return(s.BlockManagerMock)
	s.GraphMock = mock2.NewMockGraph(ctrl)
}

type MemStorageExecutorTestSuite struct {
	ExecutorTestSuite
}

func (s *MemStorageExecutorTestSuite) SetupTest() {
	s.ExecutorTestSuite.SetupTest()

	s.TemplateStorage = storagetest.NewMemoryTestTemplateStorage()
	s.Storage = memory.NewRuntimeStorage(s.TemplateStorage)

	blockManager := s.Factory.NewBlockInstanceManager()
	blockScheduler := s.Factory.NewBlockScheduler()

	s.Executor = NewExecutor(s.Storage, blockManager, blockScheduler)
}

type DBStorageExecutorTestSuite struct {
	ExecutorTestSuite
	storage      bsql.Cluster
	suiteCleanup func()
}

func (s *DBStorageExecutorTestSuite) SetupSuite() {
	logger, err := zaplog.NewDeployLogger(log2.DebugLevel)
	s.Require().NoError(err)
	xlog.SetGlobalLogger(logger)

	store, cleanup, err := db.SetupContext()
	s.Require().NoError(err)
	s.storage = store.Cluster
	s.suiteCleanup = cleanup
}

func (s *DBStorageExecutorTestSuite) TearDownSuite() {
	if s.suiteCleanup != nil {
		s.suiteCleanup()
	}
}

func (s *DBStorageExecutorTestSuite) SetupTest() {
	s.ExecutorTestSuite.SetupTest()

	s.TemplateStorage = storagetest.NewMemoryTestTemplateStorage()
	cache := template2.NewCache()
	templateStorage := db.NewTemplateStorage(s.storage, cache, nil)
	s.Storage = db.NewRuntimeStorage(s.storage, cache, templateStorage, nil)

	blockManager := s.Factory.NewBlockInstanceManager()
	blockScheduler := s.Factory.NewBlockScheduler()

	s.Executor = NewExecutor(s.Storage, blockManager, blockScheduler)

	confName := "executor_test_" + strconv.Itoa(s.ConfVersionID)
	s.ConfVersionID = db.PrepareConfiguration(s.T(), confName, "executor_test",
		context.Background(), templateStorage, s.graph1Blocks(), nil)
}

func TestRuntimeMemStorage(t *testing.T) {
	suite.Run(t, new(MemStorageExecutorTestSuite))
}

func TestRuntimeDBStorage(t *testing.T) {
	suite.Run(t, new(DBStorageExecutorTestSuite))
}
