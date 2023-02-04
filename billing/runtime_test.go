package db

import (
	"context"
	"fmt"
	"path/filepath"
	"testing"

	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/suite"

	"a.yandex-team.ru/billing/configshop/example"
	"a.yandex-team.ru/billing/configshop/pkg/core/entities/runtime"
	entitiestemplate "a.yandex-team.ru/billing/configshop/pkg/core/entities/template"
	coretemplate "a.yandex-team.ru/billing/configshop/pkg/core/template"
	"a.yandex-team.ru/billing/configshop/pkg/storage"
	"a.yandex-team.ru/billing/configshop/pkg/storage/template"
	storagetest "a.yandex-team.ru/billing/configshop/pkg/storage/xtest"
	bsql "a.yandex-team.ru/billing/library/go/billingo/pkg/storage/sql"
	"a.yandex-team.ru/billing/library/go/billingo/pkg/storage/sql/backends/pg"
)

type DBBlockStorageTestSuite struct {
	storagetest.BlockStorageTestSuite
	PrepareTemplateStorage *TemplateStorage
	Code                   string
	storage                bsql.Cluster
	cleanup                func()
}

func (s *DBBlockStorageTestSuite) SetupSuite() {
	st, cleanup, err := SetupContext()
	s.Require().NoError(err)
	s.cleanup = cleanup
	s.storage = st.Cluster
}

func (s *DBBlockStorageTestSuite) SetupTest() {
	s.BlockStorageTestSuite.SetupTest()
	cache := template.NewCache()
	s.Storage = NewRuntimeStorage(s.storage, cache, s.TemplateStorage, nil)

	s.PrepareTemplateStorage = NewTemplateStorage(s.storage, cache, nil)

	s.Code = fmt.Sprintf("block_test_%d", s.ConfVersionID+1)
	s.ConfVersionID = PrepareConfiguration(s.T(), s.Code, s.Code,
		context.Background(), s.PrepareTemplateStorage,
		[]entitiestemplate.Block{{Name: "123"}}, nil)
}

func (s *DBBlockStorageTestSuite) TearDownSuite() {
	if s.cleanup != nil {
		s.cleanup()
	}
}

func TestDBBlockStorage(t *testing.T) {
	suite.Run(t, new(DBBlockStorageTestSuite))
}

type DBGraphStorageTestSuite struct {
	storagetest.GraphStorageTestSuite
	templateStorage *TemplateStorage
	code            string
	storage         bsql.Cluster
	cleanup         func()
}

func (s *DBGraphStorageTestSuite) SetupSuite() {
	st, cleanup, err := SetupContext()
	s.Require().NoError(err)
	s.cleanup = cleanup
	s.storage = st.Cluster
	cache := template.NewCache()
	s.templateStorage = NewTemplateStorage(s.storage, cache, nil)
	s.Storage = NewRuntimeStorage(s.storage, cache, s.templateStorage, nil)
}

func (s *DBGraphStorageTestSuite) TestCheckGraphForExecution() {
	ctx := context.Background()

	confID, err := s.templateStorage.CreateConfiguration(ctx, "check_graph_for_execution", s.code, "")
	s.Require().NoError(err)

	confVersion, err := s.templateStorage.CreateConfigurationVersion(ctx, confID, 1, nil, "")
	s.Require().NoError(err)

	confVersion2, err := s.templateStorage.CreateConfigurationVersion(ctx, confID, 1, nil, "")
	s.Require().NoError(err)

	s.Require().NotEqual(confVersion, confVersion2)

	const (
		testEnv = "test"
		devEnv  = "dev"
	)

	// Graphs aren't initialized yet, so no errors.
	for _, confV := range []int{confVersion, confVersion2} {
		check, err := s.Storage.CheckGraphForExecution(ctx, runtime.GraphMeta{
			Env:                  testEnv,
			ConfigurationVersion: confV,
		})
		s.Require().NoError(err)
		s.Assert().False(check.HasNewerGraphs)
		s.Assert().False(check.HasExecutingGraphs)
	}

	graphID2, err := s.Storage.CreateGraph(ctx, runtime.GraphMeta{
		Env:                  testEnv,
		ConfigurationVersion: confVersion2,
	})
	s.Require().NoError(err)

	// Now graph with newer version id exists
	check, err := s.Storage.CheckGraphForExecution(ctx, runtime.GraphMeta{
		Env:                  testEnv,
		ConfigurationVersion: confVersion,
	})
	s.Require().NoError(err)
	s.Assert().True(check.HasNewerGraphs)
	s.Assert().False(check.HasExecutingGraphs)

	// For that new graph everything is fine
	check, err = s.Storage.CheckGraphForExecution(ctx, runtime.GraphMeta{
		Env:                  testEnv,
		ConfigurationVersion: confVersion2,
	})
	s.Require().NoError(err)
	s.Assert().False(check.HasNewerGraphs)
	s.Assert().False(check.HasExecutingGraphs)

	// If newer graph is created, don't create old one
	_, err = s.Storage.CreateGraph(ctx, runtime.GraphMeta{
		Env:                  testEnv,
		ConfigurationVersion: confVersion,
	})
	s.Require().Error(err)
	s.Assert().ErrorIs(err, runtime.ErrNewerGraphExist)

	s.Require().NoError(s.Storage.ChangeGraphStatusTo(ctx, graphID2, runtime.GraphStatusExecuting))

	// Now graph with newer version id exists, and it is executing as well
	check, err = s.Storage.CheckGraphForExecution(ctx, runtime.GraphMeta{
		Env:                  testEnv,
		ConfigurationVersion: confVersion,
	})
	s.Require().NoError(err)
	s.Assert().True(check.HasNewerGraphs)
	s.Assert().True(check.HasExecutingGraphs)

	// For another env everything is fine
	graphID, err := s.Storage.CreateGraph(ctx, runtime.GraphMeta{
		Env:                  devEnv,
		ConfigurationVersion: confVersion,
	})
	s.Require().NoError(err)

	s.Require().NoError(s.Storage.ChangeGraphStatusTo(ctx, graphID, runtime.GraphStatusExecuting))

	// We can create new graph even if old one is still executing.
	graphID2, err = s.Storage.CreateGraph(ctx, runtime.GraphMeta{
		Env:                  devEnv,
		ConfigurationVersion: confVersion2,
	})
	s.Require().NoError(err)

	// But new graph cannot be executed until old is done.
	err = s.Storage.ChangeGraphStatusTo(ctx, graphID2, runtime.GraphStatusExecuting)
	s.Require().Error(err)
	s.Assert().ErrorIs(err, runtime.ErrExecutingGraphExist)

	s.Require().NoError(s.Storage.ChangeGraphStatusTo(ctx, graphID, runtime.GraphStatusDone))

	// Now we can execute it.
	s.Require().NoError(s.Storage.ChangeGraphStatusTo(ctx, graphID2, runtime.GraphStatusExecuting))
}

func (s *DBGraphStorageTestSuite) TestGetLastDeployedBlocks() {
	ctx := context.Background()

	file, err := example.TemplatesFS.ReadFile(filepath.Join("templates", "test_echo_calc.yaml"))
	s.Require().NoError(err)

	tm := coretemplate.NewManager(s.templateStorage)
	tmpl, err := tm.Save(ctx, "test_last_deployed_blocks", string(file), nil)
	s.Require().NoError(err)

	confID, err := s.templateStorage.CreateConfiguration(ctx, "test_code", tmpl.Code, "")
	s.Require().NoError(err)

	generalInputs := map[string]any{
		"service_name": "test_service_name",
	}

	const mainEnv = "test"

	configParams := []struct {
		env         string
		statusGraph runtime.GraphStatusType
		statusBlock runtime.BlockStatusType
	}{
		{
			env:         mainEnv,
			statusGraph: runtime.GraphStatusFailed,
			statusBlock: runtime.BlockStatusFailed,
		},
		{
			env:         mainEnv,
			statusGraph: runtime.GraphStatusFailed,
			statusBlock: runtime.BlockStatusDone,
		},
		{
			env:         "dev",
			statusGraph: runtime.GraphStatusDone,
			statusBlock: runtime.BlockStatusDone,
		},
		{
			env:         mainEnv,
			statusGraph: runtime.GraphStatusNew,
			statusBlock: runtime.BlockStatusNew,
		},
	}

	var graphID, lastGraphID runtime.GraphID
	var expectedGraphID runtime.GraphID
	var deleteBlockID runtime.BlockID

	for _, params := range configParams {
		confVersionID, err := s.templateStorage.CreateConfigurationVersion(ctx, confID, tmpl.Version, generalInputs, "")
		s.Require().NoError(err)

		envInputs := map[string]any{
			"env_name": params.env + "_env_name",
		}

		graphID, err = s.Storage.CreateGraph(ctx, runtime.GraphMeta{
			Env:                  params.env,
			EnvInputs:            envInputs,
			ConfigurationVersion: confVersionID,
			Status:               runtime.GraphStatusNew,
		})
		s.Require().NoError(err)

		err = s.Storage.InsertBlocks(ctx, graphID)
		s.Require().NoError(err)

		runtimeBlocks, err := s.Storage.GetBlocks(ctx, graphID)
		s.Require().NoError(err)

		if params.statusGraph != runtime.GraphStatusNew {
			err = s.Storage.ChangeGraphStatusTo(ctx, graphID, runtime.GraphStatusExecuting)
			s.Require().NoError(err)
		}

		if params.statusGraph == runtime.GraphStatusFailed {
			err = s.Storage.ChangeGraphStatusTo(ctx, graphID, runtime.GraphStatusFailing)
			s.Require().NoError(err)
		}

		err = s.Storage.ChangeGraphStatusTo(ctx, graphID, params.statusGraph)
		s.Require().NoError(err)

		for index, runtimeBlock := range runtimeBlocks {
			if params.statusBlock == runtime.BlockStatusNew {
				continue
			}

			err := s.Storage.ChangeBlockStatusTo(ctx, runtimeBlock.ID, runtime.BlockStatusExecuting)
			s.Require().NoError(err)

			if index == 0 && params.statusBlock != runtime.BlockStatusType(params.statusGraph) {
				err = s.Storage.ChangeBlockStatusTo(ctx, runtimeBlock.ID, runtime.BlockStatusType(params.statusGraph))
			} else {
				err = s.Storage.ChangeBlockStatusTo(ctx, runtimeBlock.ID, params.statusBlock)
			}
			s.Require().NoError(err)

		}

		if params.env == mainEnv && params.statusBlock == runtime.BlockStatusDone {
			expectedGraphID = graphID
			deleteBlockID = runtimeBlocks[1].ID
			_, err = s.storage.GetDatabase(ctx, pg.Master).
				ExecContext(ctx, "UPDATE configshop.t_configuration_deploy_block "+
					"SET change_type = $1 WHERE id = $2",
					runtime.DeleteActionType, deleteBlockID)
			s.Require().NoError(err)
		} else if params.env == mainEnv && params.statusGraph == runtime.GraphStatusNew {
			lastGraphID = graphID
		}
	}
	s.Assert().NotEqual(runtime.InvalidGraphID, expectedGraphID)
	s.Assert().NotEqual(runtime.InvalidGraphID, lastGraphID)

	blocks, err := s.Storage.GetLastDeployedConfigurationBlocks(ctx, storage.ConfigurationGetFilter{ConfigurationID: confID, Environment: mainEnv})
	s.Require().NoError(err)
	s.Require().Len(blocks, 4)

	ids := make([]runtime.BlockID, 0, len(blocks))
	for _, block := range blocks {
		s.Assert().Equal(runtime.BlockStatusDone, block.Status)
		s.Assert().Equal(expectedGraphID, block.GraphID)
		s.Assert().NotEqual(deleteBlockID, block.ID)
		ids = append(ids, block.ID)
	}

	lastGraphBlocks, err := s.Storage.GetBlocks(ctx, lastGraphID)
	s.Require().NoError(err)
	s.Require().Len(lastGraphBlocks, 6)

	var nilPrevIDCnt int
	var prevIDs []runtime.BlockID
	for _, block := range lastGraphBlocks {
		if block.PrevBlockID == nil {
			nilPrevIDCnt++
			continue
		}
		prevIDs = append(prevIDs, *block.PrevBlockID)
	}
	s.Assert().Equal(2, nilPrevIDCnt)
	s.Assert().ElementsMatch(ids, prevIDs)
}

func (s *DBGraphStorageTestSuite) SetupTest() {
	s.templateStorage = NewTemplateStorage(s.storage, template.NewCache(), nil)

	s.code = fmt.Sprintf("graph_test_%d", s.ConfVersionID)
	s.ConfVersionID = PrepareConfiguration(s.T(), s.code, s.code,
		context.Background(), s.templateStorage,
		[]entitiestemplate.Block{{Name: "123"}}, nil)
}

func (s *DBGraphStorageTestSuite) TearDownSuite() {
	if s.cleanup != nil {
		s.cleanup()
	}
}

func TestDBGraphStorageTestSuite(t *testing.T) {
	suite.Run(t, new(DBGraphStorageTestSuite))
}

func TestBlocksMatchingIDs(t *testing.T) {
	ids := []entitiestemplate.BlockID{5, 7, 2, 5, 5, 3}
	blocks := []entitiestemplate.Block{
		{ID: 3, Name: "three"},
		{ID: 2, Name: "zwei"},
		{ID: 7, Name: "sieben"},
		{ID: 5, Name: "five"},
	}

	blocks = blocksMatchingIDs(blocks, ids)
	assert.Equal(t, []entitiestemplate.Block{
		{ID: 5, Name: "five"},
		{ID: 7, Name: "sieben"},
		{ID: 2, Name: "zwei"},
		{ID: 5, Name: "five"},
		{ID: 5, Name: "five"},
		{ID: 3, Name: "three"},
	}, blocks)
}
