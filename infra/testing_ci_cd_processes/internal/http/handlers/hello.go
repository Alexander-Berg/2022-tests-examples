package handlers

import (
	"net/http"
	"time"

	"a.yandex-team.ru/infra/deploy/testing_ci_cd_processes/internal/gateways/metrics"
	"a.yandex-team.ru/infra/deploy/testing_ci_cd_processes/internal/http/common"
	"a.yandex-team.ru/library/go/yandex/unistat"
)

type HelloHandler struct {
	apiMetrics *metrics.API
}

func NewHelloHandler(apiMetrics *metrics.API) *HelloHandler {
	return &HelloHandler{apiMetrics: apiMetrics}
}

func (h *HelloHandler) ServeHTTP(w http.ResponseWriter, r *http.Request) {
	defer unistat.MeasureMicrosecondsSince(h.apiMetrics.Latency, time.Now())
	defer h.apiMetrics.Requests.Update(1)

	common.WriteResponse(w, http.StatusOK, map[string]string{"message": "hello word"}, nil)
}
