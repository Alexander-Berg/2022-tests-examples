package integration

import (
	"context"

	"github.com/aws/aws-sdk-go/service/sqs/sqsiface"

	"a.yandex-team.ru/billing/library/go/billingo/pkg/task"
	"a.yandex-team.ru/billing/library/go/billingo/pkg/taskapp"
)

func NewIntegrationTaskApp(ctx context.Context, configPath string, sqs sqsiface.SQSAPI) (*taskapp.TaskProcessor, error) {
	version, err := NewVersionTask(sqs)
	if err != nil {
		return nil, err
	}

	provider, err := task.NewProvider(ctx, &EchoTask{sqs: sqs}, version)
	if err != nil {
		return nil, err
	}
	return taskapp.NewTaskProcessor(ctx, configPath, sqs, provider)
}
