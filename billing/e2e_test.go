package test

import (
	"context"
	"path/filepath"
	"strconv"
	"strings"
	"testing"

	"github.com/stretchr/testify/suite"
	"gopkg.in/yaml.v2"

	"a.yandex-team.ru/billing/configshop/example"
	"a.yandex-team.ru/billing/configshop/pkg/core/entities/runtime"
	template3 "a.yandex-team.ru/billing/configshop/pkg/core/entities/template"
	"a.yandex-team.ru/billing/configshop/pkg/core/entities/tplops"
	memoryruntime "a.yandex-team.ru/billing/configshop/pkg/core/runtime/executors/memory"
	"a.yandex-team.ru/billing/configshop/pkg/core/runtime/pipeline"
	"a.yandex-team.ru/billing/configshop/pkg/core/template"
	yaml2 "a.yandex-team.ru/billing/configshop/pkg/core/yaml"
	"a.yandex-team.ru/billing/configshop/pkg/storage"
	"a.yandex-team.ru/billing/configshop/pkg/storage/db"
	"a.yandex-team.ru/billing/configshop/pkg/storage/memory"
	template2 "a.yandex-team.ru/billing/configshop/pkg/storage/template"
	storagetest "a.yandex-team.ru/billing/configshop/pkg/storage/xtest"
	bsql "a.yandex-team.ru/billing/library/go/billingo/pkg/storage/sql"
)

func integrationOutputCalcDefault(output tplops.OutputValue, block *runtime.Block) any {
	return tplops.NilValue{}
}

func integrationOutputCalc(output tplops.OutputValue, block *runtime.Block) any {
	return output.Name + ".some_value"
}

type okScenarioTestCase struct {
	filename string
	inputs   map[string]any
	outputs  map[string]any
}

var okScenarioTestCases = []okScenarioTestCase{
	{
		filename: "fun_example.yaml",
		inputs: map[string]any{
			"subject": "Petya", "subject_title": "rabotyaga",
			"master": "Danya", "master_title": "teamlead",
			"ydt": 100, "service": "configshop", "env": "prod",
			"params": map[string]any{
				"val1": "val2",
				"val3": 102,
			},
		},
		outputs: map[string]any{"ticket_id": "ticket_key.some_value"},
	},
	{
		filename: "simple_graph.yaml",
		inputs:   map[string]any{"service_name": "anything", "env_name": "123"},
		outputs:  map[string]any{"service_name": "anything", "env_name": "123"},
	},
	{
		filename: "simple_calc.yaml",
		inputs:   map[string]any{"service_name": "anything", "env_name": "test"},
		outputs:  map[string]any{"concatenated": "anything and test"},
	},
	{
		filename: "simple_validate.yaml",
		inputs:   map[string]any{"service1": "abc", "service2": "abc"},
		outputs:  map[string]any{},
	},
	{
		filename: "test_2_integrations.yaml",
		inputs:   map[string]any{"service_name": "accounter", "account_id": 123},
		outputs:  map[string]any{"service_id": "service_id.some_value"},
	},
	{
		filename: "test_n_integrations.yaml",
		inputs:   map[string]any{"id": "test"},
		outputs:  map[string]any{"id": "id.some_value"},
	},
	{
		filename: "test_no_deps.yaml",
		inputs:   map[string]any{},
		outputs: map[string]any{
			"int1id": "id.some_value", "int2id": "id.some_value",
			"int3id": "id.some_value", "int4id": "id.some_value",
			"int5id": "id.some_value", "int6id": "id.some_value",
			"int7id": "id.some_value", "int8id": "id.some_value",
			"int9id": "id.some_value", "int10id": "id.some_value",
			"int11id": "id.some_value", "int12id": "id.some_value",
			"int13id": "id.some_value", "int14id": "id.some_value",
		},
	},
	{
		filename: "test_nested_deps.yaml",
		inputs:   map[string]any{},
		outputs:  map[string]any{},
	},
	{
		filename: "uslugi_spendable.yaml",
		inputs: map[string]any{
			"service_mdh_id":      "lalala",
			"cc":                  "uslugi_spendable",
			"display_name":        "Яндех у-у-у слуги",
			"conf_cc":             "conf",
			"conf_display_name":   "display",
			"firm":                1,
			"currency":            []string{"RUB", "EUR"},
			"person_type":         []string{"ur"},
			"is_offer":            true,
			"selfemployed":        false,
			"nds":                 []int{0, 20},
			"transport_path":      "hahn/help/me",
			"oebs_contract_type":  110,
			"report_type":         "type",
			"org_code":            "code",
			"location":            "RU",
			"payment_schedule":    "EVERYDAYIMSHUFFLING",
			"payment_limit":       1,
			"payment_batch_flag":  "N",
			"payment_target":      "OEBS",
			"billing_paysys_type": "yandex",
			"schema_start_date":   "01.03.22",
			"tax_rate_osn":        "18",
			"tax_rate_usn":        "20",
			"tax_rate_prepay":     "19",
			"prepay_segment":      "seg",
			"accts_pay_segment":   "segment",
			"reward_limit":        1,
			"sf_description":      "ololo",
			"reward_type_id":      "money",
			"oebs_segments":       "RU10.20404R5054.00000000.SEMS18.RUSc.00000000.VR320.00.0000.1.000000.000000",
		},
		outputs: map[string]any{"oebs_ticket_key": "ticket_key.some_value"},
	},
	{
		filename: "embed_graph.yaml",
		inputs:   map[string]any{"service_name": "anything", "account_id": "123"},
		outputs:  map[string]any{"service_id": "service_id.some_value", "trash": "ANYTHING"},
	},
	{
		filename: "embed_embed_graph.yaml",
		inputs:   map[string]any{"service_name": "anything", "account_id": "123"},
		outputs:  map[string]any{"service_id": "service_id.some_value", "embed_trash": "ANYTHING"},
	},
	{
		filename: filepath.Join("repeat", "good_one.yaml"),
		inputs:   map[string]any{"mnogo_valenok": []any{"odin", "dva", "tri"}},
		outputs:  map[string]any{"kolvo_valenok": 3},
	},
}

func (s *E2ETestSuite) TestOKScenarios() {
	for _, testCase := range okScenarioTestCases {
		tt := testCase
		s.Run(tt.filename, func() {
			ctx := context.Background()
			file, err := example.TemplatesFS.ReadFile(filepath.Join("templates", tt.filename))
			s.Require().NoError(err)

			var cfg yaml2.BlockTree
			s.Require().NoError(yaml.Unmarshal(file, &cfg))

			tplName := strings.TrimSuffix(tt.filename, ".yaml")

			enrichedCfg, err := cfg.EmbedTemplates(ctx, tplName, s.TemplateStorage)
			s.Require().NoError(err)

			_, err = s.TemplateStorage.Insert(ctx, tplName, string(file), nil, nil)
			s.Require().NoError(err)

			graph, err := template.NewBlockGraphFromYAML(enrichedCfg)
			s.Require().NoError(err)

			p := pipeline.NewPipeline(s.Storage, s.TemplateStorage, nil)
			_ = memoryruntime.NewFactory(p)
			memoryruntime.SetIntergrationCalcOutputFunc(integrationOutputCalc)

			blocks, err := graph.Blocks()
			s.Require().NoError(err)

			if s.Prepare != nil {
				s.Prepare(s.T(), blocks, tt.inputs)
			}
			const devEnv = "dev"
			graphID, err := s.Storage.CreateGraph(ctx, runtime.GraphMeta{
				ConfigurationVersion: s.ConfVersionID,
				Status:               runtime.GraphStatusNew,
				Env:                  devEnv,
			})
			s.Require().NoError(err)
			s.TemplateStorage.InitForRuntime(graphID, blocks, tt.inputs)

			s.Require().NoError(p.StartGraph(ctx, graphID, nil))

			status, err := s.Storage.GetGraphStatus(ctx, graphID)
			s.Require().NoError(err)
			if !s.Assert().Equal(runtime.GraphStatusDone, status) {
				errs, err := s.Storage.GetErrors(ctx, graphID)
				s.Require().NoError(err)
				s.Fail("failed blocks", errs)
			}

			outputs, err := p.GetOutputs(ctx, graphID)
			s.Require().NoError(err)

			s.Assert().Equal(tt.outputs, outputs)
		})
	}
}

func (s *E2ETestSuite) TestFailScenarios() {
	testCases := []struct {
		filename string
		inputs   map[string]any
		errCode  string
	}{
		{
			filename: "fail_calc.yaml",
			inputs:   map[string]any{"short_name": "abc"},
			errCode:  runtime.ErrorCodeExpr,
		},
	}

	for _, testCase := range testCases {
		tt := testCase
		s.Run(tt.filename, func() {
			ctx := context.Background()
			file, err := example.TemplatesFS.ReadFile(filepath.Join("templates", tt.filename))
			s.Require().NoError(err)

			var cfg yaml2.BlockTree
			s.Require().NoError(yaml.Unmarshal(file, &cfg))

			enrichedCfg, err := cfg.EmbedTemplates(ctx, "", s.TemplateStorage)
			s.Require().NoError(err)

			graph, err := template.NewBlockGraphFromYAML(enrichedCfg)
			s.Require().NoError(err)

			p := pipeline.NewPipeline(s.Storage, s.TemplateStorage, nil)
			_ = memoryruntime.NewFactory(p)
			memoryruntime.SetIntergrationCalcOutputFunc(integrationOutputCalc)

			blocks, err := graph.Blocks()
			s.Require().NoError(err)

			if s.Prepare != nil {
				s.Prepare(s.T(), blocks, tt.inputs)
			}
			const devEnv = "dev"
			graphID, err := s.Storage.CreateGraph(ctx, runtime.GraphMeta{
				ConfigurationVersion: s.ConfVersionID,
				Status:               runtime.GraphStatusNew,
				Env:                  devEnv,
			})
			s.Require().NoError(err)
			s.TemplateStorage.InitForRuntime(graphID, blocks, tt.inputs)

			s.Require().NoError(p.StartGraph(ctx, graphID, nil))

			blockErrors, err := s.Storage.GetErrors(ctx, graphID)
			s.Require().NoError(err)
			s.Require().Len(blockErrors, 1)
			s.Assert().Equal(tt.errCode, blockErrors[0].Code)
		})
	}
}

type E2ETestSuite struct {
	suite.Suite
	Storage         storage.RuntimeStorage
	TemplateStorage *storagetest.MemoryTestTemplateStorage
	ConfVersionID   int
	Prepare         func(t *testing.T, blocks []template3.Block, inputs map[string]any)
}

type MemStorageE2ETestSuite struct {
	E2ETestSuite
}

func (s *MemStorageE2ETestSuite) SetupSuite() {
	s.TemplateStorage = storagetest.NewMemoryTestTemplateStorage()
	s.Storage = memory.NewRuntimeStorage(s.TemplateStorage)
}

func TestMemStorageE2E(t *testing.T) {
	suite.Run(t, new(MemStorageE2ETestSuite))
}

type DBStorageE2ETestSuite struct {
	E2ETestSuite
	templateStorage storage.TemplateStorage
	code            string
	storage         bsql.Cluster
	suiteCleanup    func()
}

func (s *DBStorageE2ETestSuite) SetupSuite() {
	store, cleanup, err := db.SetupContext()
	s.Require().NoError(err)
	s.storage = store.Cluster
	s.suiteCleanup = cleanup

	s.TemplateStorage = storagetest.NewMemoryTestTemplateStorage()

	cache := template2.NewCache()
	templateStorage := db.NewTemplateStorage(s.storage, cache, nil)
	s.templateStorage = templateStorage
	s.Storage = db.NewRuntimeStorage(s.storage, cache, s.templateStorage, nil)

	s.Prepare = func(t *testing.T, blocks []template3.Block, inputs map[string]any) {
		confName := "e2e_test_" + strconv.Itoa(s.ConfVersionID+1)
		s.code = confName
		s.ConfVersionID = db.PrepareConfiguration(s.T(), confName, "e2e_test",
			context.Background(), templateStorage, blocks, inputs)
	}
}

func (s *DBStorageE2ETestSuite) TearDownSuite() {
	if s.suiteCleanup != nil {
		s.suiteCleanup()
	}
}

func TestPersistentStorageE2E(t *testing.T) {
	suite.Run(t, new(DBStorageE2ETestSuite))
}

func (s *DBStorageE2ETestSuite) TestOKScenarios() {
	for _, testCase := range okScenarioTestCases {
		tt := testCase
		s.Run(tt.filename, func() {
			ctx := context.Background()
			file, err := example.TemplatesFS.ReadFile(filepath.Join("templates", tt.filename))
			s.Require().NoError(err)

			tplName := strings.TrimSuffix(tt.filename, ".yaml")

			tm := template.NewManager(s.templateStorage)
			tpl, err := tm.Save(ctx, tplName, string(file), nil)
			s.Require().NoError(err)

			confID, err := s.templateStorage.CreateConfiguration(ctx, "ok_scenario_"+tplName, tplName, "")
			s.Require().NoError(err)

			blocks, err := tm.GetBlocks(ctx, storage.BlockGetFilter{
				Code:            tpl.Code,
				TemplateVersion: tpl.Version,
			})
			s.Require().NoError(err)

			p := pipeline.NewPipeline(s.Storage, s.TemplateStorage, nil)
			_ = memoryruntime.NewFactory(p)
			memoryruntime.SetIntergrationCalcOutputFunc(integrationOutputCalc)

			confVersionID, err := s.templateStorage.CreateConfigurationVersion(ctx, confID, tpl.Version, nil, "")
			s.Require().NoError(err)

			s.checkGraph(ctx, confVersionID, p, tt, blocks)

			confVersionID, err = s.templateStorage.CreateConfigurationVersion(ctx, confID, tpl.Version, nil, "")
			s.Require().NoError(err)

			s.checkGraph(ctx, confVersionID, p, tt, blocks)
		})
	}
}

func (s *DBStorageE2ETestSuite) checkGraph(ctx context.Context, confVersionID int,
	p pipeline.Pipeline, tt okScenarioTestCase, blocks []template3.Block,
) {
	const devEnv = "dev"
	graphID, err := s.Storage.CreateGraph(ctx, runtime.GraphMeta{
		ConfigurationVersion: confVersionID,
		Status:               runtime.GraphStatusNew,
		Env:                  devEnv,
	})
	s.Require().NoError(err)
	s.TemplateStorage.InitForRuntime(graphID, blocks, tt.inputs)

	s.Require().NoError(p.StartGraph(ctx, graphID, nil))

	status, err := s.Storage.GetGraphStatus(ctx, graphID)
	s.Require().NoError(err)
	if !s.Assert().Equal(runtime.GraphStatusDone, status) {
		errs, err := s.Storage.GetErrors(ctx, graphID)
		s.Require().NoError(err)
		s.Fail("failed blocks", errs)
	}

	outputs, err := p.GetOutputs(ctx, graphID)
	s.Require().NoError(err)

	s.Assert().Equal(tt.outputs, outputs)
}
