package base

import (
	"bytes"
	"context"
	"encoding/json"
	"fmt"
	"io"
	"mime/multipart"
	"net/http"
	"os"
	"strconv"
	"testing"
	"time"

	"github.com/stretchr/testify/suite"

	"a.yandex-team.ru/billing/configshop/example"
	"a.yandex-team.ru/billing/configshop/pkg/core/actions"
	"a.yandex-team.ru/billing/configshop/pkg/core/entities"
	"a.yandex-team.ru/billing/configshop/pkg/core/entities/runtime"
	"a.yandex-team.ru/billing/configshop/pkg/core/valobj"
	"a.yandex-team.ru/billing/library/go/billingo/pkg/interactions"
	"a.yandex-team.ru/billing/library/go/billingo/pkg/xjson"
	"a.yandex-team.ru/library/go/core/metrics/solomon"
	"a.yandex-team.ru/library/go/core/xerrors"
)

type ReqArgs struct {
	body   any
	params map[string][]string
}

func (s *ServerTestSuite) putTemplate(args ReqArgs) (*interactions.RawResponse, error) {
	body, ok := args.body.([]byte)
	if !ok {
		return nil, xerrors.New("put template body must be []byte")
	}

	var b bytes.Buffer
	w := multipart.NewWriter(&b)
	wf, err := w.CreateFormFile("template", "tpl.yaml")
	if err != nil {
		return nil, xerrors.Errorf("failed to create form file: %w", err)
	}
	if _, err := io.Copy(wf, bytes.NewReader(body)); err != nil {
		return nil, xerrors.Errorf("failed to fill form file: %w", err)
	}
	if err := w.Close(); err != nil {
		return nil, xerrors.Errorf("failed to close multipart writer: %w", err)
	}

	return s.client.MakeRequestRaw(s.ctx, interactions.Request{
		APIMethod: "/v1/template",
		Method:    http.MethodPut,
		Name:      "put_template",
		Body:      b.Bytes(),
		Params:    args.params,
		Headers:   map[string]string{"Content-Type": w.FormDataContentType()},
	}), nil
}

type TemplateResp struct {
	Data struct {
		Version int `json:"version"`
	} `json:"data"`
}

func (s *ServerTestSuite) parsePutTemplateRespose(response *interactions.RawResponse) *TemplateResp {
	var templateResp TemplateResp

	s.Require().NoError(xjson.Unmarshal(response.Response, &templateResp))
	return &templateResp
}

func (s *ServerTestSuite) putConfiguration(args ReqArgs) *interactions.RawResponse {
	return s.client.MakeRequestRaw(s.ctx, interactions.Request{
		APIMethod: "/v1/configuration",
		Method:    http.MethodPut,
		Name:      "put_configuration",
		Body:      args.body,
	})
}

type ConfigResp struct {
	Data struct {
		ConfID int `json:"configuration_id"`
	} `json:"data"`
}

func (s *ServerTestSuite) parsePutConfigRespose(response *interactions.RawResponse) *ConfigResp {
	var configResp ConfigResp

	s.Require().NoError(xjson.Unmarshal(response.Response, &configResp))
	return &configResp
}

func (s *ServerTestSuite) putConfigurationVersion(args ReqArgs) *interactions.RawResponse {
	return s.client.MakeRequestRaw(s.ctx, interactions.Request{
		APIMethod: "/v1/configuration/version",
		Method:    http.MethodPut,
		Name:      "put_configuration_version",
		Body:      args.body,
	})
}

type ConfigVersionResp struct {
	Data struct {
		ConfVersionID int `json:"configuration_version_id"`
	} `json:"data"`
}

func (s *ServerTestSuite) parsePutConfigVersionRespose(response *interactions.RawResponse) *ConfigVersionResp {
	var configVersionResp ConfigVersionResp

	s.Require().NoError(xjson.Unmarshal(response.Response, &configVersionResp))
	return &configVersionResp
}

func (s *ServerTestSuite) getExport(args ReqArgs) *interactions.RawResponse {
	return s.client.MakeRequestRaw(s.ctx, interactions.Request{
		APIMethod: "/v1/export",
		Method:    http.MethodGet,
		Name:      "get_export",
		Body:      args.body,
	})
}

type GetExportResp struct {
	Data struct {
		State valobj.GraphState `json:",inline"`
	} `json:"data"`
}

func (s *ServerTestSuite) postExport(args ReqArgs) *interactions.RawResponse {
	return s.client.MakeRequestRaw(s.ctx, interactions.Request{
		APIMethod: "/v1/export",
		Method:    http.MethodPost,
		Name:      "post_export",
		Body:      args.body,
	})
}

func (s *ServerTestSuite) postExportRun(args ReqArgs) *interactions.RawResponse {
	return s.client.MakeRequestRaw(s.ctx, interactions.Request{
		APIMethod: "/v1/export/run",
		Method:    http.MethodPost,
		Name:      "post_export_run",
		Body:      args.body,
	})
}

func (s *ServerTestSuite) postExportFail(args ReqArgs) *interactions.RawResponse {
	return s.client.MakeRequestRaw(s.ctx, interactions.Request{
		APIMethod: "/v1/export/fail",
		Method:    http.MethodPost,
		Name:      "post_export_fail",
		Body:      args.body,
	})
}

func (s *ServerTestSuite) getExportDetails(args ReqArgs) *interactions.RawResponse {
	return s.client.MakeRequestRaw(s.ctx, interactions.Request{
		APIMethod: "/v1/export/details",
		Method:    http.MethodGet,
		Name:      "get_export_details",
		Params:    args.params,
	})
}

type ExportDetailsResp struct {
	Data valobj.GraphState `json:"data"`
}

func (s *ServerTestSuite) parseGetExportDetails(response *interactions.RawResponse) *ExportDetailsResp {
	var exportDetailsResp ExportDetailsResp

	s.Require().NoError(xjson.Unmarshal(response.Response, &exportDetailsResp))
	return &exportDetailsResp
}

func (s *ServerTestSuite) getConfigurationValidate(args ReqArgs) *interactions.RawResponse {
	return s.client.MakeRequestRaw(s.ctx, interactions.Request{
		APIMethod: "/v1/configuration/validate",
		Method:    http.MethodPost,
		Name:      "post_configuration_validate",
		Body:      args.body,
	})
}

type ConfigurationDiffResp struct {
	Data []actions.PresentedDiffBlock
}

func (s *ServerTestSuite) postConfigurationDiff(args ReqArgs) *interactions.RawResponse {
	return s.client.MakeRequestRaw(s.ctx, interactions.Request{
		APIMethod: "/v1/configuration/diff",
		Method:    http.MethodPost,
		Name:      "post_configuration_diff",
		Body:      args.body,
	})
}

func (s *ServerTestSuite) parsePostConfigurationDiff(response *interactions.RawResponse) *ConfigurationDiffResp {
	var configurationDiffResp ConfigurationDiffResp

	s.Require().NoError(xjson.Unmarshal(response.Response, &configurationDiffResp))
	return &configurationDiffResp
}

type ConfigurationArgs struct {
	ConfigurationData
	TemplateFilename  string
	TemplateCode      string
	ConfigurationCode string
	GeneralInputs     map[string]any
}

type ConfigurationData struct {
	TemplateVersion        int
	ConfigurationID        int
	ConfigurationVersionID int
}

type ExportArgs struct {
	ConfigurationArgs
	Environment string
	EnvInputs   map[string]any
}

func (s *ServerTestSuite) prepareConfiguration(args ConfigurationArgs) ConfigurationData {
	// Push template
	if args.TemplateVersion == 0 {
		file, err := example.TemplatesFS.ReadFile(args.TemplateFilename)
		s.Require().NoError(err)

		response, err := s.putTemplate(ReqArgs{
			body:   file,
			params: map[string][]string{"code": {args.TemplateCode}},
		})
		s.Require().NoError(err)
		s.Assert().Equal(http.StatusOK, response.Code, string(response.Response))
		s.Require().NoError(response.Error)

		templateResp := s.parsePutTemplateRespose(response)
		args.TemplateVersion = templateResp.Data.Version
	}

	// push configuration
	body, err := json.Marshal(map[string]any{
		"code":          args.ConfigurationCode,
		"template_code": args.TemplateCode,
	})
	s.Require().NoError(err)

	if args.ConfigurationID == 0 {
		response := s.putConfiguration(ReqArgs{body: body})
		s.Require().Equal(http.StatusOK, response.Code, string(response.Response))

		configResp := s.parsePutConfigRespose(response)

		args.ConfigurationID = configResp.Data.ConfID
	}

	return ConfigurationData{
		TemplateVersion: args.TemplateVersion,
		ConfigurationID: args.ConfigurationID,
	}
}

func (s *ServerTestSuite) prepareConfigurationVersion(args ConfigurationArgs) ConfigurationData {
	confData := s.prepareConfiguration(args)

	// create configuration version
	body, err := json.Marshal(map[string]any{
		"code":             args.ConfigurationCode,
		"template_version": confData.TemplateVersion,
		"configuration_id": confData.ConfigurationID,
		"inputs":           args.GeneralInputs,
	})
	s.Require().NoError(err)

	response := s.putConfigurationVersion(ReqArgs{body: body})
	s.Require().Equal(http.StatusOK, response.Code, string(response.Response))

	configVersionResp := s.parsePutConfigVersionRespose(response)

	confData.ConfigurationVersionID = configVersionResp.Data.ConfVersionID
	return confData
}

func (s *ServerTestSuite) prepareExport(args ExportArgs) ConfigurationData {
	configuration := s.prepareConfigurationVersion(args.ConfigurationArgs)

	bodyMap := map[string]any{
		"configuration_version_id": configuration.ConfigurationVersionID,
		"environment":              args.Environment,
		"inputs":                   args.EnvInputs,
	}
	body, err := json.Marshal(bodyMap)
	s.Require().NoError(err)

	response := s.postExport(ReqArgs{body: body})
	s.Require().Equal(http.StatusOK, response.Code, string(response.Response))

	return configuration
}

func (s *ServerTestSuite) runExport(args ExportArgs) ConfigurationData {
	configuration := s.prepareExport(args)

	bodyMap := map[string]any{
		"configuration_version_id": configuration.ConfigurationVersionID,
		"environment":              args.Environment,
	}
	body, err := json.Marshal(bodyMap)
	s.Require().NoError(err)

	response := s.postExportRun(ReqArgs{body: body})
	s.Require().Equal(http.StatusOK, response.Code, string(response.Response))

	return configuration
}

func (s *ServerTestSuite) waitFinishGraph(configurationVersionID int, environment string) {
	args := ReqArgs{
		params: map[string][]string{
			"configuration_version_id": {fmt.Sprint(configurationVersionID)},
			"environment":              {environment},
		},
	}

	var exportDetails *ExportDetailsResp
	for pollingCounter := 0; pollingCounter < 100; pollingCounter++ {
		response := s.getExportDetails(args)
		s.Require().Equal(http.StatusOK, response.Code, string(response.Response))

		exportDetails = s.parseGetExportDetails(response)

		if runtime.GraphStatusType(exportDetails.Data.Status) == runtime.GraphStatusDone {
			break
		}

		time.Sleep(100 * time.Millisecond)
	}
	s.Require().Equal(runtime.GraphStatusType(exportDetails.Data.Status), runtime.GraphStatusDone, exportDetails)

	response := s.getExportDetails(args)
	s.Require().Equal(http.StatusOK, response.Code, string(response.Response))
}

func (s *ServerTestSuite) TestPing() {
	s.T().Parallel()

	response := s.client.MakeRequestRaw(s.ctx, interactions.Request{
		APIMethod: "/ping",
		Method:    http.MethodGet,
	})
	s.Require().Equal(http.StatusOK, response.Code)
}

func (s *ServerTestSuite) TestPutTemplateEmptyBody() {
	s.T().Parallel()

	response, err := s.putTemplate(ReqArgs{
		body:   []byte(nil),
		params: map[string][]string{"code": {"template0"}},
	})
	s.Require().NoError(err)
	s.Require().Equal(http.StatusBadRequest, response.Code)
}

func (s *ServerTestSuite) TestPostExport() {
	s.T().Parallel()

	configuration := s.prepareConfigurationVersion(ConfigurationArgs{
		TemplateFilename:  "templates/simple_calc.yaml",
		TemplateCode:      "test_post_export",
		ConfigurationCode: "post_export_configuration",
		GeneralInputs:     map[string]any{"service_name": "test_service_name"},
	})

	// create export with invalid input
	bodyMap := map[string]any{
		"configuration_version_id": configuration.ConfigurationVersionID,
		"environment":              "dev",
		"inputs": map[string]any{
			"env_name": "test_env_name",
			"invalid":  "anything",
		},
	}
	body, err := json.Marshal(bodyMap)
	s.Require().NoError(err)

	response := s.postExport(ReqArgs{body: body})
	s.Require().Equal(http.StatusBadRequest, response.Code, string(response.Response))

	// create export without env input
	bodyMap = map[string]any{
		"configuration_version_id": configuration.ConfigurationVersionID,
		"environment":              "dev",
		"inputs":                   map[string]any{},
	}
	body, err = json.Marshal(bodyMap)
	s.Require().NoError(err)

	response = s.postExport(ReqArgs{body: body})
	s.Require().Equal(http.StatusBadRequest, response.Code, string(response.Response))

	// create export
	bodyMap = map[string]any{
		"configuration_version_id": configuration.ConfigurationVersionID,
		"environment":              "dev",
		"inputs":                   map[string]any{"env_name": "test_env_name"},
	}
	body, err = json.Marshal(bodyMap)
	s.Require().NoError(err)

	response = s.postExport(ReqArgs{body: body})
	s.Require().Equal(http.StatusOK, response.Code, string(response.Response))
}

func (s *ServerTestSuite) TestConfigurationPipeline() {
	s.T().Parallel()

	file, err := example.TemplatesFS.ReadFile("templates/simple_graph.yaml")
	s.Require().NoError(err)

	response, err := s.putTemplate(ReqArgs{
		body:   file,
		params: map[string][]string{"code": {"template1"}},
	})
	s.Require().NoError(err)
	s.Assert().Equal(http.StatusOK, response.Code, string(response.Response))
	s.Require().NoError(response.Error)

	templateResp := s.parsePutTemplateRespose(response)
	s.Require().NotEmpty(templateResp.Data.Version)

	response, err = s.putTemplate(ReqArgs{
		body:   []byte("abc"),
		params: map[string][]string{"code": {"template1"}},
	})
	s.Require().NoError(err)
	s.Assert().Equal(http.StatusBadRequest, response.Code, string(response.Response))

	response = s.client.MakeRequestRaw(s.ctx, interactions.Request{
		APIMethod: "/v1/template",
		Method:    http.MethodGet,
		Name:      "get_template",
		Params:    map[string][]string{"code": {"template1"}},
	})

	s.Require().Equal(http.StatusOK, response.Code, string(response.Response))

	// canonFileContent, err := yaml.CanonizeTree(file)
	s.Assert().NoError(err)
	s.Assert().Equal(file, response.Response)

	body, err := json.Marshal(map[string]any{
		"code":          "configuration1",
		"template_code": "template1",
		"description":   "anything",
	})
	s.Require().NoError(err)

	response = s.putConfiguration(ReqArgs{body: body})

	s.Require().Equal(http.StatusOK, response.Code, string(response.Response))

	configResp := s.parsePutConfigRespose(response)
	s.Require().NotEmpty(configResp.Data.ConfID)

	bodyMap := map[string]any{
		"code":             "template1",
		"template_version": templateResp.Data.Version,
		"configuration_id": configResp.Data.ConfID,
		"inputs":           map[string]any{"invalid": "anything"},
		"description":      "anything",
	}
	body, err = json.Marshal(bodyMap)
	s.Require().NoError(err)

	response = s.putConfigurationVersion(ReqArgs{body: body})
	s.Require().Equal(http.StatusBadRequest, response.Code, string(response.Response))

	inputParams := map[string]any{"service_name": map[string]any{
		"composite": "value",
	}}

	bodyMap["inputs"] = inputParams
	body, err = json.Marshal(bodyMap)
	s.Require().NoError(err)

	response = s.putConfigurationVersion(ReqArgs{body: body})
	s.Require().Equal(http.StatusOK, response.Code, string(response.Response))

	configVersionResp := s.parsePutConfigVersionRespose(response)
	s.Require().NotEmpty(configVersionResp.Data.ConfVersionID)

	const env = "dev"

	envParams := map[string]any{
		"env_name": 123,
	}
	bodyMap = map[string]any{
		"configuration_version_id": configVersionResp.Data.ConfVersionID,
		"environment":              env,
		"inputs":                   envParams,
	}
	body, err = json.Marshal(bodyMap)
	s.Require().NoError(err)

	response = s.postExport(ReqArgs{body: body})
	s.Require().Equal(http.StatusOK, response.Code, string(response.Response))

	response = s.client.MakeRequestRaw(s.ctx, interactions.Request{
		APIMethod: "/v1/configuration/version",
		Method:    http.MethodGet,
		Name:      "get_configuration_version",
		Params: map[string][]string{
			"configuration_code": {"configuration1"},
			"version":            {"1"},
			"input_env":          {env},
		},
	})
	s.Require().Equal(http.StatusOK, response.Code, string(response.Response))
	var configVersionGetResp struct {
		Data entities.ConfigurationVersion `json:"data"`
	}
	s.Require().NoError(xjson.Unmarshal(response.Response, &configVersionGetResp))

	descr := "anything"
	expectedEnvParams := map[string]any{
		"env_name": json.Number("123"),
	}
	s.Assert().Equal(entities.ConfigurationVersion{
		ID:              configVersionResp.Data.ConfVersionID,
		TemplateVersion: 1,
		Inputs:          inputParams,
		EnvInputs:       expectedEnvParams,
		Description:     &descr,
	}, configVersionGetResp.Data)
}

func (s *ServerTestSuite) TestGetExportDetails() {
	s.T().Parallel()

	const env string = "dev"

	configuration := s.prepareExport(ExportArgs{
		ConfigurationArgs: ConfigurationArgs{
			TemplateFilename:  "templates/simple_graph.yaml",
			TemplateCode:      "test_export_details",
			ConfigurationCode: "post_export_details_configuration",
			GeneralInputs:     map[string]any{"service_name": "test_service_name"},
		},
		Environment: env,
		EnvInputs:   map[string]any{"env_name": "test_env_name"},
	})

	// get export details before run export
	response := s.getExportDetails(ReqArgs{
		params: map[string][]string{
			"configuration_version_id": {fmt.Sprint(configuration.ConfigurationVersionID)},
			"environment":              {env},
		},
	})
	s.Require().Equal(http.StatusOK, response.Code, string(response.Response))

	exportDetails := s.parseGetExportDetails(response)

	s.Require().Equal("new", exportDetails.Data.Status)
	s.Require().Empty(exportDetails.Data.Blocks)

	// run export
	bodyMap := map[string]any{
		"configuration_version_id": configuration.ConfigurationVersionID,
		"environment":              env,
	}
	body, err := json.Marshal(bodyMap)
	s.Require().NoError(err)

	response = s.postExportRun(ReqArgs{body: body})
	s.Require().Equal(http.StatusOK, response.Code, string(response.Response))

	s.waitFinishGraph(configuration.ConfigurationVersionID, env)

	response = s.getExportDetails(ReqArgs{
		params: map[string][]string{
			"configuration_version_id": {fmt.Sprint(configuration.ConfigurationVersionID)},
			"environment":              {env},
		},
	})
	s.Require().Equal(http.StatusOK, response.Code, string(response.Response))

	exportDetails = s.parseGetExportDetails(response)

	s.Require().Equal(runtime.GraphStatusDone, runtime.GraphStatusType(exportDetails.Data.Status))
	s.Require().Len(exportDetails.Data.Blocks, 1)
	s.Require().Equal(runtime.BlockStatusDone, runtime.BlockStatusType(exportDetails.Data.Blocks[0].Status))
}

func (s *ServerTestSuite) TestGetExportDetailsErrors() {
	s.T().Parallel()

	configuration := s.prepareConfigurationVersion(ConfigurationArgs{
		TemplateFilename:  "templates/invalid_integration_type.yaml",
		TemplateCode:      "test_export_details_error",
		ConfigurationCode: "post_export_details_configuration_error",
		GeneralInputs:     map[string]any{"str_var": "abc"},
	})

	// Create export.
	bodyMap := map[string]any{
		"configuration_version_id": configuration.ConfigurationVersionID,
		"environment":              "dev",
	}
	body, err := json.Marshal(bodyMap)
	s.Require().NoError(err)

	// No export - return error.
	response := s.postExportFail(ReqArgs{body: body})
	s.Require().Equal(http.StatusInternalServerError, response.Code, string(response.Response))

	response = s.postExport(ReqArgs{body: body})
	s.Require().Equal(http.StatusOK, response.Code, string(response.Response))

	// No-op because there are no running blocks.
	response = s.postExportFail(ReqArgs{body: body})
	s.Require().Equal(http.StatusOK, response.Code, string(response.Response))

	response = s.postExportRun(ReqArgs{body: body})
	s.Require().Equal(http.StatusOK, response.Code, string(response.Response))

	// wait status done
	var exportDetails *ExportDetailsResp
	for pollingCounter := 0; pollingCounter < 40; pollingCounter++ {
		response = s.getExportDetails(ReqArgs{
			params: map[string][]string{
				"configuration_version_id": {strconv.Itoa(configuration.ConfigurationVersionID)},
				"environment":              {"dev"},
			},
		})
		s.Require().Equal(http.StatusOK, response.Code, string(response.Response))

		exportDetails = s.parseGetExportDetails(response)

		if runtime.GraphStatusType(exportDetails.Data.Status) == runtime.GraphStatusFailed {
			break
		}

		time.Sleep(500 * time.Millisecond)
	}

	s.Require().Equal(runtime.GraphStatusFailed, runtime.GraphStatusType(exportDetails.Data.Status))
	s.Require().Len(exportDetails.Data.Blocks, 2)

	var foundEcho bool
	for _, block := range exportDetails.Data.Blocks {
		if block.Name == "echo" {
			foundEcho = true
			s.Require().Equal(runtime.ErrorCodeType, block.Error["code"].(string))
		}
	}
	s.Assert().True(foundEcho, "echo block must have been found")
}

func (s *ServerTestSuite) TestGetConfigurationValidate() {
	s.T().Parallel()

	configuration := s.prepareConfiguration(ConfigurationArgs{
		TemplateFilename:  "templates/simple_calc.yaml",
		TemplateCode:      "test_configuration_validate",
		ConfigurationCode: "post_configuration_validate",
	})

	// body without configuration_id
	bodyMap := map[string]any{
		"template_version": configuration.TemplateVersion,
		"inputs": map[string]any{
			"service_name": "test_name",
			"env_name":     "test_env_name",
		},
		"environment": "dev",
	}
	body, err := json.Marshal(bodyMap)
	s.Require().NoError(err)

	response := s.getConfigurationValidate(ReqArgs{body: body})
	s.Require().Equal(http.StatusBadRequest, response.Code)
	s.Require().Contains(string(response.Response), "missing required body parameters")

	bodyMap["configuration_id"] = configuration.ConfigurationID
	body, err = json.Marshal(bodyMap)
	s.Require().NoError(err)

	response = s.getConfigurationValidate(ReqArgs{body: body})
	s.Require().Equal(http.StatusOK, response.Code, string(response.Response))

	exportDetails := s.parseGetExportDetails(response)

	s.Require().Equal(string(runtime.GraphStatusDone), exportDetails.Data.Status)

	for _, block := range exportDetails.Data.Blocks {
		if block.Name == "global" {
			s.Require().Equal("test_name and test_env_name", block.Outputs["concatenated"])
		}
	}
}

func (s *ServerTestSuite) TestGetConfigurationDiff() {
	s.T().Parallel()

	const env string = "dev"

	configuration := s.runExport(ExportArgs{
		ConfigurationArgs: ConfigurationArgs{
			TemplateFilename:  "templates/simple_graph.yaml",
			TemplateCode:      "test_template_diff",
			ConfigurationCode: "post_configuration_diff",
			GeneralInputs:     map[string]any{"service_name": "test_service_name"},
		},
		Environment: env,
		EnvInputs:   map[string]any{"env_name": "test_env_name"},
	})

	s.waitFinishGraph(configuration.ConfigurationVersionID, env)

	// body without environment
	bodyMap := map[string]any{
		"configuration_id": configuration.ConfigurationID,
		"template_version": configuration.TemplateVersion,
		"inputs": map[string]any{
			"service_name": "test_name",
			"env_name":     "test_env_name",
		},
	}
	body, err := json.Marshal(bodyMap)
	s.Require().NoError(err)

	response := s.postConfigurationDiff(ReqArgs{body: body})
	s.Require().Equal(http.StatusBadRequest, response.Code)
	s.Require().Contains(string(response.Response), "missing required body parameters")

	bodyMap["environment"] = env
	body, err = json.Marshal(bodyMap)
	s.Require().NoError(err)

	response = s.postConfigurationDiff(ReqArgs{body: body})
	s.Require().Equal(http.StatusOK, response.Code, string(response.Response))

	diff := s.parsePostConfigurationDiff(response)

	expectedDiffIO := map[string]actions.PresentedDiffIO{
		"service_name": {
			New:      "test_name",
			Deployed: "test_service_name",
			Verdict:  "changed",
		},
	}

	expectedDiff := []actions.PresentedDiffBlock{{
		BlockName: "simple_global",
		Verdict:   "changed",
		Inputs:    expectedDiffIO,
		Outputs:   expectedDiffIO,
	}}

	s.Require().Len(diff.Data, 1)
	s.Require().Equal(expectedDiff, diff.Data)
}

func (s *ServerTestSuite) TestBlockChange() {
	s.T().Parallel()

	const env = "dev"

	testCases := []struct {
		name                 string
		args                 ConfigurationArgs
		nextTemplateFilename string
		expectCaching        bool
	}{
		{
			name: "version no cache",
			args: ConfigurationArgs{
				TemplateFilename:  "templates/simple_version.yaml",
				TemplateCode:      "test_block_change",
				ConfigurationCode: "block_change",
			},
		},
		{
			name: "version with cache",
			args: ConfigurationArgs{
				TemplateFilename:  "templates/simple_version_cached.yaml",
				TemplateCode:      "test_block_change_cached",
				ConfigurationCode: "block_change_cached",
			},
			expectCaching: true,
		},
		{
			name: "args changed -> no cache",
			args: ConfigurationArgs{
				TemplateFilename:  "templates/simple_version_cached.yaml",
				TemplateCode:      "test_block_change_cached2",
				ConfigurationCode: "block_change_cached2",
			},
			nextTemplateFilename: "templates/simple_version_cached2.yaml",
			expectCaching:        false,
		},
	}

	for _, tc := range testCases {
		tc := tc
		s.Run(tc.name, func() {
			s.T().Parallel()

			configuration := s.runExport(ExportArgs{
				ConfigurationArgs: tc.args,
				Environment:       env,
			})

			s.waitFinishGraph(configuration.ConfigurationVersionID, env)

			response := s.getExportDetails(ReqArgs{
				params: map[string][]string{
					"configuration_version_id": {fmt.Sprint(configuration.ConfigurationVersionID)},
					"environment":              {env},
				},
			})
			s.Require().Equal(http.StatusOK, response.Code, string(response.Response))

			exportDetails := s.parseGetExportDetails(response)

			var hasGlobal bool
			for _, block := range exportDetails.Data.Blocks {
				if block.Name == "global" {
					hasGlobal = true
					s.Assert().Equal(json.Number("1"), block.Outputs["version"])
				}
			}
			s.Require().True(hasGlobal, "graph must have 'global' block")

			args := tc.args
			configuration.ConfigurationVersionID = 0
			args.ConfigurationData = configuration
			if tc.nextTemplateFilename != "" {
				args.TemplateFilename = tc.nextTemplateFilename
				args.TemplateVersion = 0
			}

			configuration = s.runExport(ExportArgs{
				ConfigurationArgs: args,
				Environment:       env,
			})
			s.waitFinishGraph(configuration.ConfigurationVersionID, env)

			response = s.getExportDetails(ReqArgs{
				params: map[string][]string{
					"configuration_version_id": {fmt.Sprint(configuration.ConfigurationVersionID)},
					"environment":              {env},
				},
			})
			s.Require().Equal(http.StatusOK, response.Code, string(response.Response))

			exportDetails = s.parseGetExportDetails(response)

			for _, block := range exportDetails.Data.Blocks {
				if block.Name == "global" {
					hasGlobal = true
					expectedVersion := json.Number("2")
					if tc.expectCaching {
						expectedVersion = "1"
					}
					s.Assert().Equal(expectedVersion, block.Outputs["version"])
				}
			}
			s.Require().True(hasGlobal, "graph must have 'global' block")
		})
	}
}

func (s *ServerTestSuite) TestCaching() {
	s.T().Parallel()

	const env = "dev"

	configuration := s.runExport(ExportArgs{
		ConfigurationArgs: ConfigurationArgs{
			TemplateFilename:  "templates/test_simple_echo_caching.yaml",
			TemplateCode:      "test_caching",
			ConfigurationCode: "caching",
			GeneralInputs: map[string]any{"var1": "unchanged", "var2": map[string]any{
				"unchanged": map[string]any{
					"nested": 12,
				},
			}},
		},
		Environment: env,
	})

	s.waitFinishGraph(configuration.ConfigurationVersionID, env)

	bodyMap := map[string]any{
		"configuration_id": configuration.ConfigurationID,
		"template_version": configuration.TemplateVersion,
		"inputs": map[string]any{
			"var1": "unchanged",
			"var2": map[string]any{
				"unchanged": map[string]any{
					"nested": 12,
				},
			},
		},
		"environment": env,
	}
	body, err := json.Marshal(bodyMap)
	s.Require().NoError(err)

	response := s.postConfigurationDiff(ReqArgs{body: body})
	s.Require().Equal(http.StatusOK, response.Code, string(response.Response))

	diff := s.parsePostConfigurationDiff(response)

	expectedDiff := []actions.PresentedDiffBlock{{
		BlockName: "echo1",
		Verdict:   "perhaps_changed",
		Outputs: map[string]actions.PresentedDiffIO{
			"var1": {
				New:      "UndefinedValue",
				Deployed: "unchanged",
				Verdict:  "perhaps_changed",
			},
		},
	}, {
		BlockName: "echo3",
		Verdict:   "perhaps_changed",
		Inputs: map[string]actions.PresentedDiffIO{
			"echo1.var1": {
				New:      "UndefinedValue",
				Deployed: "unchanged",
				Verdict:  "perhaps_changed",
			},
		},
	}}

	s.Require().NotNil(diff)
	s.Require().Len(diff.Data, 2)
	s.Require().ElementsMatch(expectedDiff, diff.Data)
}

type ServerTestSuite struct {
	suite.Suite
	client      *interactions.Client
	ctx         context.Context
	cleanupTest func()
}

func (s *ServerTestSuite) SetupSuite() {
	registry := solomon.NewRegistry(nil)

	client, err := interactions.NewClient(interactions.Config{
		BaseURL: os.Getenv("CONFIGSHOP_BASE_URL"),
		Name:    "configshop",
	}, nil, registry)
	s.Require().NoError(err)

	s.client = client
}

func (s *ServerTestSuite) SetupTest() {
	s.ctx, s.cleanupTest = context.WithTimeout(context.Background(), time.Second)
}

func (s *ServerTestSuite) TearDownTest() {
	s.cleanupTest()
}

func TestServer(t *testing.T) {
	suite.Run(t, new(ServerTestSuite))
}
