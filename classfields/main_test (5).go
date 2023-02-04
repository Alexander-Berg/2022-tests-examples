package main

import (
	vlog "github.com/YandexClassifieds/go-common/i/log"
	vlogrus "github.com/YandexClassifieds/go-common/log/logrus"
	"github.com/YandexClassifieds/nomad-metrics/pkg/config"
	"github.com/YandexClassifieds/nomad-metrics/pkg/nomad_api"
	"testing"
)

func TestService(t *testing.T) {
	var logger vlog.Logger
	logger = vlogrus.New()
	logger.Info("Start testing")

	nomadServerAddress, err := config.NewConfig()
	if err != nil {
		t.Fatalf("Error during config initialization: %s", err)
	}

	nomadClient, err := nomad_api.NewNomadClient(nomadServerAddress, logger)


	if err != nil {
		t.Fatalf("Error during client initialization: %s", err)
	}

	jobsList, err := nomadClient.GetJobList()

	if err != nil {
		t.Fatalf("Error during requesting job list: %s", err)
	}

	for _, job := range jobsList {
		if job.Name == "shiva" {
			err = nomadClient.CollectMetricsAboutJobInstancesCount(job)

			if err != nil {
				t.Fatalf("Error duting getting instances count for shiva: %s", err)
			}
			break
		}
	}
}
