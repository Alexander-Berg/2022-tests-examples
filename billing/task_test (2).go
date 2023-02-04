package task

import (
	"context"
	"fmt"
	"os"
	"testing"

	"github.com/aws/aws-sdk-go/aws"
	"github.com/aws/aws-sdk-go/service/sqs"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"

	"a.yandex-team.ru/billing/library/go/billingo/pkg/queue"
	"a.yandex-team.ru/billing/library/go/billingo/pkg/xlog"
	"a.yandex-team.ru/library/go/core/log"
	zaplog "a.yandex-team.ru/library/go/core/log/zap"
)

var (
	envSqsPort            = "SQS_PORT"
	envAwsAccessKeyID     = "AWS_ACCESS_KEY_ID"
	envAwsSecretAccessKey = "AWS_SECRET_ACCESS_KEY"
	envAwsSessionToken    = "AWS_SESSION_TOKEN"
)

// todo-igogor чет я не понял как таскать за собой тестовые функции между пакетами, но не в прод.
func PrepareContext(t *testing.T) context.Context {
	logger, err := zaplog.NewDeployLogger(log.DebugLevel)
	if err != nil {
		t.Fatal(err)
	}
	xlog.SetGlobalLogger(logger)

	return context.Background()
}

func PrepareSQS(t *testing.T, ctx context.Context) *sqs.SQS {
	// Переменная создается в рецепте
	SQSPort, ok := os.LookupEnv(envSqsPort)
	if !ok {
		t.Fatalf("Environment variable %s is not set", envSqsPort)
	}
	endpoint := fmt.Sprintf("http://127.0.0.1:%s", SQSPort)

	// имя профиля - задается при старте рецепта
	if err := os.Setenv(envAwsAccessKeyID, "my_user"); err != nil {
		t.Fatal(err)
	}
	if err := os.Setenv(envAwsSecretAccessKey, "unused"); err != nil {
		t.Fatal(err)
	}
	if err := os.Setenv(envAwsSessionToken, ""); err != nil {
		t.Fatal(err)
	}

	//endpoint := "http://sqs.yandex.net:8771"

	svc, err := queue.NewSQSClient(ctx, endpoint, "yandex", nil)
	if err != nil {
		t.Fatal(err)
	}

	return svc
}

func PrepareQueue(t *testing.T, ctx context.Context) *queue.Queue {
	svc := PrepareSQS(t, ctx)

	// яндексовый sqs почему-то позволяет молча создать очередь которая уже существует, что противоречит апи
	q, err := queue.Create(ctx, svc, t.Name(), nil)
	if err != nil {
		t.Fatal(err)
	}

	if err := q.Purge(ctx); err != nil {
		t.Fatal(err)
	}

	return q
}

func PrepareTaskWithMessage(t *testing.T, ctx context.Context) (
	Task,
	*queue.Message,
	TestPayload,
	chan *TestPayload,
) {
	q := PrepareQueue(t, ctx)

	j := NewTaskForTest(t.Name(), q.Name)

	testPayload := TestPayload{Text: "text", Int: 666}
	if err := Enqueue(ctx, q.SQS, j, testPayload); err != nil {
		t.Fatal(err)
	}

	msgList, err := q.Receive(ctx, &sqs.ReceiveMessageInput{
		MessageAttributeNames: aws.StringSlice([]string{sqs.QueueAttributeNameAll}),
		WaitTimeSeconds:       aws.Int64(1),
		VisibilityTimeout:     aws.Int64(1),
	})
	if err != nil {
		t.Fatal(err)
	}

	require.Equal(t, 1, len(msgList), "Should receive one message")

	return j, msgList[0], testPayload, j.(*TaskForTest).outC
}

func NewTaskForTest(name, queueName string) Task {
	return &TaskForTest{
		name:      name,
		queueName: queueName,
		outC:      make(chan *TestPayload, 1),
	}
}

type TaskForTest struct {
	BaseTask

	name      string
	queueName string
	outC      chan *TestPayload
}

func (d *TaskForTest) Name() string {
	return d.name
}

func (d *TaskForTest) QueueName() string {
	return d.queueName
}

func (d *TaskForTest) NewPayload() any {
	return TestPayload{}
}

func (d *TaskForTest) Process(ctx context.Context, msg *queue.Message) error {
	msgPayload := TestPayload{}
	err := ParsePayload(msg, &msgPayload)
	d.outC <- &msgPayload
	return err
}

func (d *TaskForTest) LogFields() []log.Field {
	return []log.Field{
		log.String(LogTaskName, d.Name()),
	}
}

type TestPayload struct {
	Text string `json:"text"`
	Int  int    `json:"int"`
}

func TestTask_EnqueueSuccess(t *testing.T) {
	ctx := PrepareContext(t)
	task, msg, testPayload, _ := PrepareTaskWithMessage(t, ctx)

	taskName, err := ParseName(msg)
	if err != nil {
		t.Fatal(err)
	}
	assert.Equal(t, task.Name(), taskName, "Must have correct task name in message")

	msgPayload := TestPayload{}
	err = ParsePayload(msg, &msgPayload)
	assert.NoError(t, err, "Enqueued message body must be payload json")
	assert.Equal(t, testPayload, msgPayload, "Enqueued message payload must have same values")
}

func TestTask_ProcessSuccess(t *testing.T) {
	ctx := PrepareContext(t)
	task, msg, enqueuedPayload, outC := PrepareTaskWithMessage(t, ctx)

	processErr := task.Process(ctx, msg)
	assert.NoError(t, processErr, "Process must finish without error")
	select {
	case processPayload := <-outC:
		assert.Equal(t, enqueuedPayload, *processPayload, "Task ProcessorHandler had right payload")
	default:
		t.Fatal("Task ProcessHandler did no work")
	}
}
