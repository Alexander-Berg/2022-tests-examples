package base

import (
	"context"
	"encoding/json"
	"net/http"
	"os"
	"testing"
	"time"

	"github.com/mitchellh/mapstructure"
	"github.com/stretchr/testify/suite"

	"a.yandex-team.ru/billing/configshop/example"
	"a.yandex-team.ru/billing/configshop/pkg/core/entities/runtime"
	"a.yandex-team.ru/billing/configshop/pkg/core/valobj"
	"a.yandex-team.ru/billing/library/go/billingo/pkg/interactions"
	json2 "a.yandex-team.ru/billing/library/go/billingo/pkg/web/schema/json"
	"a.yandex-team.ru/billing/library/go/billingo/pkg/xjson"
	"a.yandex-team.ru/billing/library/go/billingo/pkg/xlog"
	"a.yandex-team.ru/library/go/core/log"
	"a.yandex-team.ru/library/go/core/metrics/solomon"
)

type graphTestCase struct {
	templateName string
	fileName     string

	inputs, envInputs map[string]any
	expectedOutputs   map[string]any
	env               string

	errorCodes []string
}

type request struct {
	Template      string         `json:"template"`
	TemplateName  string         `json:"template_name"`
	GeneralInputs map[string]any `json:"general_inputs"`
	EnvInputs     map[string]any `json:"env_inputs"`
	Env           string         `json:"env"`
}

func (s *GraphsTestSuite) TestHappyPaths() {
	testCases := []graphTestCase{
		{
			templateName:    "simple_graph",
			fileName:        "templates/simple_graph.yaml",
			inputs:          map[string]any{"service_name": "abc"},
			envInputs:       map[string]any{"env_name": "value for env"},
			expectedOutputs: map[string]any{"service_name": "abc", "env_name": "value for env"},
			env:             "dev",
		},
		{
			templateName:    "simple_calc",
			fileName:        "templates/simple_calc.yaml",
			inputs:          map[string]any{"service_name": "abcde"},
			envInputs:       map[string]any{"env_name": "env_concat"},
			expectedOutputs: map[string]any{"concatenated": "abcde and env_concat"},
			env:             "dev",
		},
		{
			templateName: "echo_and_calcs",
			fileName:     "templates/test_echo_calc.yaml",
			inputs:       map[string]any{"int_var": 100500, "str_var": "best"},
			envInputs: map[string]any{"int_array_var": []int{1, 0, 0, 5, 0, 0}, "any_var": map[string]int{
				"kekw": 100500,
			}},
			expectedOutputs: map[string]any{
				"result": "best and 100500",
				"array": []any{json.Number("1"), json.Number("0"), json.Number("0"),
					json.Number("5"), json.Number("0"), json.Number("0")},
				"anything": map[string]any{
					"int_var": json.Number("100500"),
					"str_var": "best",
					"any_var": map[string]any{
						"kekw": json.Number("100500"),
					},
				},
			},
			env: "dev",
		},
	}

	for _, tc := range testCases {
		tc := tc
		s.Run(tc.templateName, func() {
			s.T().Parallel()

			fileContent, err := example.TemplatesFS.ReadFile(tc.fileName)
			s.Require().NoError(err)
			s.Require().NotEmpty(fileContent)
			s.graphTest(fileContent, tc)
		})
	}
}

func (s *GraphsTestSuite) TestFailPaths() {
	testCases := []graphTestCase{
		{
			templateName: "fail_calc",
			fileName:     "templates/fail_calc.yaml",
			inputs:       map[string]any{"short_name": "abc"},
			env:          "dev",
			errorCodes:   []string{runtime.ErrorCodeExpr},
		},
		{
			templateName: "invalid_integration_type",
			fileName:     "templates/invalid_integration_type.yaml",
			inputs:       map[string]any{"str_var": "abc"},
			env:          "dev",
			errorCodes:   []string{runtime.ErrorCodeType},
		},
	}

	for _, tc := range testCases {
		tc := tc
		s.Run(tc.templateName, func() {
			s.T().Parallel()

			fileContent, err := example.TemplatesFS.ReadFile(tc.fileName)
			s.Require().NoError(err)
			s.Require().NotEmpty(fileContent)
			s.graphTest(fileContent, tc)
		})
	}
}

func (s *GraphsTestSuite) graphTest(fileContent []byte, tc graphTestCase) {
	req := request{
		Template:      string(fileContent),
		TemplateName:  tc.templateName,
		Env:           tc.env,
		GeneralInputs: tc.inputs,
		EnvInputs:     tc.envInputs,
	}
	reqBytes, err := json.Marshal(req)
	s.Require().NoError(err)

	response := s.client.MakeRequestRaw(s.ctx, interactions.Request{
		APIMethod: "/",
		Method:    http.MethodPost,
		Name:      "post_rungraph",
		Body:      reqBytes,
	})
	s.Assert().Equal(http.StatusOK, response.Code, string(response.Response))
	s.Require().NoError(response.Error)

	var apiResp json2.APIResponse
	s.Require().NoError(xjson.Unmarshal(response.Response, &apiResp))

	var exportStatus valobj.ExportStatus
	s.Require().NoError(mapstructure.Decode(apiResp.Data, &exportStatus))

	if len(tc.errorCodes) != 0 {
		var errorCodes []string
		for _, blockError := range exportStatus.Errors {
			errorCodes = append(errorCodes, blockError["code"].(string))
		}
		s.Assert().ElementsMatch(tc.errorCodes, errorCodes, exportStatus)
	} else {
		s.Require().Empty(exportStatus.Errors)
		s.Assert().Equal(tc.expectedOutputs, exportStatus.Outputs)
	}
}

type GraphsTestSuite struct {
	suite.Suite
	client                    *interactions.Client
	ctx                       context.Context
	cleanupTest, cancelLogger func()
}

func (s *GraphsTestSuite) SetupSuite() {
	registry := solomon.NewRegistry(nil)

	client, err := interactions.NewClient(interactions.Config{
		BaseURL: os.Getenv("CONFIGSHOP_TEST_PROXY_URL"),
		Name:    "configshop_proxy",
	}, nil, registry)
	s.Require().NoError(err)

	logger, cancel, err := xlog.NewDeployLogger(log.DebugLevel)
	s.Require().NoError(err)
	s.cancelLogger = cancel
	xlog.SetGlobalLogger(logger)

	s.client = client
}

func (s *GraphsTestSuite) SetupTest() {
	s.ctx, s.cleanupTest = context.WithTimeout(context.Background(), time.Minute)
}

func (s *GraphsTestSuite) TearDownTest() {
	s.cleanupTest()
}

func (s *GraphsTestSuite) TearDownSuite() {
	s.cancelLogger()
}

func TestGraphs(t *testing.T) {
	suite.Run(t, new(GraphsTestSuite))
}
