package main

import (
	"context"
	"github.com/YandexClassifieds/github-metrics/pkg/config"
	github_api "github.com/YandexClassifieds/github-metrics/pkg/github-api"
	vlog "github.com/YandexClassifieds/go-common/i/log"
	vlogrus "github.com/YandexClassifieds/go-common/log/logrus"
	"github.com/spf13/viper"
	"testing"
)

func TestService(t *testing.T) {
	ctx := context.Background()
	viper.AutomaticEnv()

	var logger vlog.Logger
	logger = vlogrus.New()
	logger.Info("Start getting github metrics...")

	githubConfig, err := config.NewConfig()
	if err != nil {
		t.Fatalf("error while initialisation github config: %s", err)
	}

	githubClient, err := github_api.NewGithubService(logger, githubConfig)
	if err != nil {
		t.Fatal(err)
	}

	_, err = githubClient.ListRunners(ctx)
	if err != nil {
		t.Fatalf("error while getting runners list: %s", err)
	}

	_, err = githubClient.ListWorkflowRuns(ctx, []string{"autoru-frontend"})
	if err != nil {
		t.Fatalf("Error while getting workflow runs list: %s", err)
	}

}
