package main

import (
	"context"
	"github.com/YandexClassifieds/github-actions-monitoring/pkg/config"
	"github.com/YandexClassifieds/github-actions-monitoring/pkg/github_api"
	"github.com/YandexClassifieds/github-actions-monitoring/pkg/startrek"
	vlog "github.com/YandexClassifieds/go-common/i/log"
	vlogrus "github.com/YandexClassifieds/go-common/log/logrus"
	"testing"
	"github.com/stretchr/testify/require"
)

func TestService(t *testing.T) {
	ctx := context.Background()

	var logger vlog.Logger
	logger = vlogrus.New()

	githubConfig, err := config.NewConfig()
	require.NoError(t, err)

	githubService, err := github_api.NewGithubService(logger, githubConfig)
	require.NoError(t, err)

	_, err = githubService.GetRepositoriesList(ctx)
	require.NoError(t, err)

	// Check the repository which contains external runner
	_, isContainingExternalRunners, err := githubService.GetActionsRepositoryContent(ctx, "jaeger")
	if isContainingExternalRunners != true {
		t.Fatalf("Jaeger repository contains external runner but service doesn't know about it")
	}
	require.NoError(t, err)

	// Check the repository which doesn't contain external runner
	_, isContainingExternalRunners, err = githubService.GetActionsRepositoryContent(ctx, "terraform")
	require.NoError(t, err)
	if isContainingExternalRunners != false {
		t.Fatalf("Terraform repository contains external runner but service doesn't know about it")
	}

	// Check startrek ticket creation
	workflowList := make(map[string]string)
	workflowList["testWorkflowPath"] = "repoName"

	startrekClient := startrek.NewClient(logger)
	_, err = startrekClient.CreateIssue(workflowList)
	require.NoError(t, err)
}
