package main

import (
	"log"
	"net/http"
	"time"

	"a.yandex-team.ru/infra/deploy/testing_ci_cd_processes/internal/gateways/metrics"
	"a.yandex-team.ru/infra/deploy/testing_ci_cd_processes/internal/http/handlers"
)

func main() {
	apiMetrics := metrics.NewAPI()

	http.Handle("/hello", handlers.NewHelloHandler(apiMetrics))
	http.Handle("/unistat", handlers.NewUnistatHandler(apiMetrics))

	s := &http.Server{
		Addr:           ":80",
		Handler:        http.DefaultServeMux,
		ReadTimeout:    1 * time.Second,
		WriteTimeout:   5 * time.Second,
		MaxHeaderBytes: 1 << 20,
	}

	log.Fatal(s.ListenAndServe())
}
