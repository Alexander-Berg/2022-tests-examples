package main

import (
	"bytes"
	"encoding/json"
	"fmt"
	"io"
	stdlog "log"
	"mime/multipart"
	"net/http"
	"net/url"
	"os"
	"strconv"
	"strings"
	"time"

	"a.yandex-team.ru/billing/configshop/pkg/core/entities/runtime"
	"a.yandex-team.ru/billing/configshop/pkg/core/valobj"
	"a.yandex-team.ru/billing/library/go/billingo/pkg/interactions"
	"a.yandex-team.ru/billing/library/go/billingo/pkg/xjson"
	"a.yandex-team.ru/billing/library/go/billingo/pkg/xlog"
	"a.yandex-team.ru/library/go/core/log"
	"a.yandex-team.ru/library/go/core/metrics/nop"
)

func main() {
	client, err := interactions.NewClient(interactions.Config{
		BaseURL: os.Getenv("CONFIGSHOP_BASE_URL"),
		Name:    "configshop",
	}, nil, nop.Registry{})
	if err != nil {
		stdlog.Fatal(err)
	}

	logger, cancel, err := xlog.NewDeployLogger(log.DebugLevel)
	if err != nil {
		stdlog.Fatal(err)
	}
	defer cancel()
	xlog.SetGlobalLogger(logger)

	s := server{client: client}

	http.HandleFunc("/", s.handleTemplate)
	http.HandleFunc("/ping", func(w http.ResponseWriter, r *http.Request) {})
	if err := http.ListenAndServe(":"+os.Getenv("RECIPE_PORT"), nil); err != nil {
		stdlog.Fatal(err)
	}
}

type server struct {
	client *interactions.Client
}

type request struct {
	Template      string         `json:"template"`
	TemplateName  string         `json:"template_name"`
	GeneralInputs map[string]any `json:"general_inputs"`
	EnvInputs     map[string]any `json:"env_inputs"`
	Env           string         `json:"env"`
}

func (s *server) handleTemplate(w http.ResponseWriter, r *http.Request) {
	ctx := r.Context()

	b, err := io.ReadAll(r.Body)
	if err != nil {
		w.WriteHeader(http.StatusBadGateway)
		return
	}

	var req request
	if err := xjson.Unmarshal(b, &req); err != nil {
		w.WriteHeader(http.StatusBadRequest)
		_, _ = w.Write([]byte("cannot parse json: " + err.Error()))
		return
	}

	var bf bytes.Buffer
	mw := multipart.NewWriter(&bf)
	wf, err := mw.CreateFormFile("template", "tpl.yaml")
	if err != nil {
		w.WriteHeader(http.StatusBadRequest)
		_, _ = w.Write([]byte("failed to create form file: " + err.Error()))
	}
	if _, err := io.Copy(wf, strings.NewReader(req.Template)); err != nil {
		w.WriteHeader(http.StatusBadRequest)
		_, _ = w.Write([]byte("failed to fill form file: " + err.Error()))
	}
	if err := mw.Close(); err != nil {
		w.WriteHeader(http.StatusBadRequest)
		_, _ = w.Write([]byte("failed to close multipart writer: " + err.Error()))
	}
	response := s.client.MakeRequestRaw(ctx, interactions.Request{
		APIMethod: "/v1/template",
		Method:    http.MethodPut,
		Name:      "put_template",
		Body:      bf.Bytes(),
		Params:    url.Values{"code": {req.TemplateName}},
		Headers:   map[string]string{"Content-Type": mw.FormDataContentType()},
	})
	if response.Code != http.StatusOK {
		w.WriteHeader(response.Code)
		_, _ = w.Write(response.RawResponse.Body())
		return
	}

	var templateResp struct {
		Data struct {
			Version int `json:"version"`
		} `json:"data"`
	}
	if err := xjson.Unmarshal(response.Response, &templateResp); err != nil {
		w.WriteHeader(http.StatusInternalServerError)
		_, _ = w.Write([]byte(err.Error()))
		return
	}

	body, err := json.Marshal(map[string]any{
		"code":          req.TemplateName,
		"template_code": req.TemplateName,
		"description":   "anything",
	})
	if err != nil {
		panic(err)
	}

	response = s.client.MakeRequestRaw(ctx, interactions.Request{
		APIMethod: "/v1/configuration",
		Method:    http.MethodPut,
		Name:      "put_configuration",
		Body:      body,
	})
	if response.Code != http.StatusOK {
		w.WriteHeader(response.Code)
		_, _ = w.Write(response.RawResponse.Body())
		return
	}

	var configResp struct {
		Data struct {
			ConfID int `json:"configuration_id"`
		} `json:"data"`
	}
	if err := xjson.Unmarshal(response.Response, &configResp); err != nil {
		w.WriteHeader(http.StatusInternalServerError)
		_, _ = w.Write([]byte(err.Error()))
		return
	}

	bodyMap := map[string]any{
		"code":             req.TemplateName,
		"template_version": templateResp.Data.Version,
		"configuration_id": configResp.Data.ConfID,
		"inputs":           req.GeneralInputs,
		"description":      "anything",
	}
	body, err = json.Marshal(bodyMap)
	if err != nil {
		panic(err)
	}

	response = s.client.MakeRequestRaw(ctx, interactions.Request{
		APIMethod: "/v1/configuration/version",
		Method:    http.MethodPut,
		Name:      "put_configuration_version",
		Body:      body,
	})
	if response.Code != http.StatusOK {
		w.WriteHeader(response.Code)
		_, _ = w.Write(response.RawResponse.Body())
		return
	}

	var configVersionResp struct {
		Data struct {
			ConfVersionID int `json:"configuration_version_id"`
		} `json:"data"`
	}
	if err := xjson.Unmarshal(response.Response, &configVersionResp); err != nil {
		w.WriteHeader(http.StatusInternalServerError)
		_, _ = w.Write([]byte(err.Error()))
		return
	}

	bodyMap = map[string]any{
		"configuration_version_id": configVersionResp.Data.ConfVersionID,
		"environment":              req.Env,
		"inputs":                   req.EnvInputs,
	}
	body, err = json.Marshal(bodyMap)
	if err != nil {
		panic(err)
	}

	response = s.client.MakeRequestRaw(ctx, interactions.Request{
		APIMethod: "/v1/export",
		Method:    http.MethodPost,
		Name:      "post_export",
		Body:      body,
	})
	if response.Code != http.StatusOK {
		w.WriteHeader(response.Code)
		_, _ = w.Write(response.RawResponse.Body())
		return
	}

	delete(bodyMap, "inputs")
	body, err = json.Marshal(bodyMap)
	if err != nil {
		panic(err)
	}

	response = s.client.MakeRequestRaw(ctx, interactions.Request{
		APIMethod: "/v1/export/run",
		Method:    http.MethodPost,
		Name:      "post_export",
		Body:      body,
	})
	if response.Code != http.StatusOK {
		w.WriteHeader(response.Code)
		_, _ = w.Write(response.RawResponse.Body())
		return
	}

	ticker := time.NewTicker(500 * time.Millisecond)
	defer ticker.Stop()

	now := time.Now()
	var exportStatus valobj.ExportStatus

	for range ticker.C {
		if time.Since(now) > 30*time.Second {
			w.WriteHeader(http.StatusInternalServerError)
			_, _ = w.Write([]byte(fmt.Sprintf("graph didn't complete in 30 seconds, last state: %+v", exportStatus)))
			return
		}

		response = s.client.MakeRequestRaw(ctx, interactions.Request{
			APIMethod: "/v1/export",
			Method:    http.MethodGet,
			Name:      "get_export",
			Params: map[string][]string{
				"configuration_version_id": {strconv.Itoa(configVersionResp.Data.ConfVersionID)},
				"environment":              {req.Env},
			},
		})
		var getExportResp struct {
			Data valobj.ExportStatus `json:"data"`
		}
		if response.Code != http.StatusOK {
			w.WriteHeader(response.Code)
			_, _ = w.Write(response.RawResponse.Body())
			return
		}
		if err := xjson.Unmarshal(response.Response, &getExportResp); err != nil {
			w.WriteHeader(http.StatusInternalServerError)
			_, _ = w.Write([]byte(err.Error()))
			return
		}
		exportStatus = getExportResp.Data

		xlog.Info(ctx, "got /v1/export result",
			log.Any("status", exportStatus),
			log.String("test", req.TemplateName),
		)

		if runtime.GraphStatusType(exportStatus.Status).IsTerminal() {
			w.WriteHeader(http.StatusOK)
			_, _ = w.Write(response.RawResponse.Body())
			return
		}
	}
}
