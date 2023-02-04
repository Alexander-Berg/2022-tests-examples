package handlers

import (
	"log"
	"net/http"
	"time"

	"a.yandex-team.ru/infra/deploy/testing_ci_cd_processes/internal/gateways/metrics"
	"a.yandex-team.ru/library/go/yandex/unistat"
)

type UnistatHandler struct {
	apiMetrics *metrics.API
}

func NewUnistatHandler(apiMetrics *metrics.API) *UnistatHandler {
	return &UnistatHandler{apiMetrics: apiMetrics}
}

func (h *UnistatHandler) ServeHTTP(w http.ResponseWriter, r *http.Request) {
	defer unistat.MeasureMicrosecondsSince(h.apiMetrics.Latency, time.Now())
	defer h.apiMetrics.Requests.Update(1)

	bytes, err := unistat.MarshalJSON()
	if err != nil {
		http.Error(w, err.Error(), http.StatusInternalServerError)
		defer h.apiMetrics.Errors.Update(1)
		return
	}

	w.Header().Set("Content-Type", "application/json; charset=utf-8")
	if _, err := w.Write(bytes); err != nil {
		log.Print("write:", err)
	}
}
