package integration

import (
	"context"
	"encoding/json"

	"github.com/aws/aws-sdk-go/service/sqs/sqsiface"

	"a.yandex-team.ru/billing/configshop/pkg/core/entities/runtime"
	"a.yandex-team.ru/billing/library/go/billingo/pkg/interactions/configshop"
	"a.yandex-team.ru/billing/library/go/billingo/pkg/queue"
	"a.yandex-team.ru/billing/library/go/billingo/pkg/task"
	"a.yandex-team.ru/billing/library/go/billingo/pkg/xlog"
	"a.yandex-team.ru/library/go/core/log"
	"a.yandex-team.ru/library/go/core/xerrors"
)

type EchoTask struct {
	task.BaseTask
	sqs sqsiface.SQSAPI
}

var _ task.Task = (*EchoTask)(nil)

func (e EchoTask) Name() string {
	return "echo"
}

func (e EchoTask) QueueName() string {
	return "integration-dev.fifo"
}

func (e EchoTask) NewPayload() any {
	return &configshop.IntegrationReceivePayload{}
}

func (e *EchoTask) Process(ctx context.Context, msg *queue.Message) error {
	payloadIface, err := task.Parse(ctx, e, msg.Body())
	if err != nil {
		return task.PermanentError(err)
	}

	payload, ok := payloadIface.(*configshop.IntegrationReceivePayload)
	if !ok {
		return task.PermanentError(xerrors.Errorf("expected type %T as payload, got %T",
			e.NewPayload(), payload))
	}

	respQueue, err := configshop.QueueForEnv("dev")
	if err != nil {
		return task.PermanentError(err)
	}
	responseTask, err := configshop.NewResponseTask(respQueue)
	if err != nil {
		return task.PermanentError(err)
	}
	responsePayload := configshop.IntegrationSendPayload{
		BlockID: payload.BlockID,
		State:   payload.Params,
	}
	return task.Enqueue(ctx, e.sqs, responseTask, responsePayload)
}

type VersionTask struct {
	task.BaseTask
	sqs      sqsiface.SQSAPI
	respTask task.Task
}

var _ task.Task = (*VersionTask)(nil)

func NewVersionTask(sqs sqsiface.SQSAPI) (*VersionTask, error) {
	respQueue, err := configshop.QueueForEnv("dev")
	if err != nil {
		return nil, err
	}
	responseTask, err := configshop.NewResponseTask(respQueue)
	if err != nil {
		return nil, err
	}

	return &VersionTask{sqs: sqs, respTask: responseTask}, nil
}

func (e VersionTask) Name() string {
	return "version"
}

func (e VersionTask) QueueName() string {
	return "integration-dev.fifo"
}

func (e VersionTask) NewPayload() any {
	return &configshop.IntegrationReceivePayload{}
}

func (e *VersionTask) Process(ctx context.Context, msg *queue.Message) error {
	payloadIface, err := task.Parse(ctx, e, msg.Body())
	if err != nil {
		return task.PermanentError(err)
	}

	payload, ok := payloadIface.(*configshop.IntegrationReceivePayload)
	if !ok {
		return task.PermanentError(xerrors.Errorf("expected type %T as payload, got %T",
			e.NewPayload(), payload))
	}
	xlog.Info(ctx, "Parsed payload", log.Any("payload", payload))

	isUpdate := runtime.ActionType(payload.Action) == runtime.UpdateActionType
	if isUpdate && payload.State == nil || !isUpdate && payload.State != nil {
		return e.SendReply(ctx, configshop.IntegrationSendPayload{
			BlockID: payload.BlockID,
			Error: &runtime.BlockError{Code: runtime.ErrorCodeValidate, Args: map[string]any{
				"action": payload.Action,
				"state":  payload.State,
			}},
		})
	}

	version := 1
	if payload.State != nil {
		version64, _ := payload.State["version"].(json.Number).Int64()
		version = int(version64) + 1
	}
	responsePayload := configshop.IntegrationSendPayload{
		BlockID: payload.BlockID,
		State:   map[string]any{"version": version},
	}
	return e.SendReply(ctx, responsePayload)
}

func (e VersionTask) SendReply(ctx context.Context, payload configshop.IntegrationSendPayload) error {
	return task.Enqueue(ctx, e.sqs, e.respTask, payload)
}
