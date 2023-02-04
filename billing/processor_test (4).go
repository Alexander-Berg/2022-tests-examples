package taskapp

import (
	"context"
	"fmt"
	"io/ioutil"
	"os"
	"testing"
	"time"

	"github.com/aws/aws-sdk-go/service/sqs"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
	"golang.org/x/exp/slices"

	"a.yandex-team.ru/billing/library/go/billingo/pkg/queue"
	"a.yandex-team.ru/billing/library/go/billingo/pkg/task"
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
//      kositsyn-pa Можно только создать отдельный пакет с тестовыми функциями,
//                  при компиляции бинаря неиспользованный пакет не прилинкуется
func PrepareContext(t *testing.T) context.Context {
	logger, err := zaplog.NewDeployLogger(log.DebugLevel)
	require.NoError(t, err)
	xlog.SetGlobalLogger(logger)

	return context.Background()
}

func PrepareSQS(t *testing.T, ctx context.Context) *sqs.SQS {
	// Переменная создается в рецепте
	SQSPort, ok := os.LookupEnv(envSqsPort)
	require.Truef(t, ok, "Environment variable %s is not set", envSqsPort)

	endpoint := fmt.Sprintf("http://127.0.0.1:%s", SQSPort)

	// имя профиля - задается при старте рецепта
	require.NoError(t, os.Setenv(envAwsAccessKeyID, "my_user"))
	require.NoError(t, os.Setenv(envAwsSecretAccessKey, "unused"))
	require.NoError(t, os.Setenv(envAwsSessionToken, ""))

	//endpoint := "http://sqs.yandex.net:8771"

	svc, err := queue.NewSQSClient(ctx, endpoint, "yandex", nil)
	require.NoError(t, err)

	return svc
}

func PrepareQueue(t *testing.T, ctx context.Context) *queue.Queue {
	svc := PrepareSQS(t, ctx)

	// яндексовый sqs почему-то позволяет молча создать очередь которая уже существует, что противоречит апи
	q, err := queue.Create(ctx, svc, t.Name(), nil)
	require.NoError(t, err)

	require.NoError(t, q.Purge(ctx))

	return q
}

func NewFirstTask(name, queueName string) task.Task {
	return &FirstTask{
		name:      name,
		queueName: queueName,
		outC:      make(chan *FirstPayload, 1),
	}
}

type FirstTask struct {
	task.BaseTask

	name      string
	queueName string
	outC      chan *FirstPayload
}

func (d *FirstTask) Name() string {
	return d.name
}

func (d *FirstTask) QueueName() string {
	return d.queueName
}

func (d *FirstTask) NewPayload() any {
	return FirstPayload{}
}

func (d *FirstTask) Process(ctx context.Context, msg *queue.Message) error {
	msgPayload := FirstPayload{}
	err := task.ParsePayload(msg, &msgPayload)
	d.outC <- &msgPayload
	return err
}

type FirstPayload struct {
	First string `json:"text"`
}

func PrepareConfigFile(t *testing.T, cfg *Config) *os.File {
	tmpFile, err := ioutil.TempFile("", "tasksConfig*.yaml")
	require.NoError(t, err)
	// todo-igogor проверить что defer нормально отрабатывает после t.Fatal

	// todo-igogor эта сволота маршалит структуру игнорируя тэги, поэтому нормально решение не работает и это грустно
	//if err != nil {
	//	t.Fatal(err)
	//}
	//if _, err := tmpFile.Write(cfgBytes); err != nil {
	//	t.Fatal(err)
	//}
	cfgString := fmt.Sprintf("workers: %d\npoll:\n", cfg.Workers)
	for key, val := range cfg.PollOptions {
		cfgString += fmt.Sprintf(
			"  %s:\n    pollInterval: %d\n    messageVisibilityTimeoutSeconds: %d\n",
			key,
			val.PollInterval,
			val.MessageVisibilityTimeoutSeconds,
		)
	}
	_, err = tmpFile.WriteString(cfgString)
	require.NoError(t, err)
	return tmpFile
}

func TestTaskProcessor_OneTask(t *testing.T) {
	ctx := PrepareContext(t)
	q := PrepareQueue(t, ctx)
	j := NewFirstTask(t.Name(), q.Name)
	outC := j.(*FirstTask).outC

	cfg := Config{
		Workers: 1,
		PollOptions: map[string]PollOptions{
			q.Name: {MessageVisibilityTimeoutSeconds: 1, PollInterval: time.Second},
		},
	}
	cfgFile := PrepareConfigFile(t, &cfg)
	defer func() {
		if err := os.Remove(cfgFile.Name()); err != nil {
			t.Fatal(err)
		}
		if err := cfgFile.Close(); err != nil {
			t.Fatal(err)
		}
	}()
	provider, err := task.NewProvider(ctx, j)
	require.NoError(t, err)
	p, err := NewTaskProcessor(ctx, cfgFile.Name(), q.SQS, provider)
	require.NoError(t, err)

	go func() {
		_ = p.Start(ctx)
	}()

	expectedPayloads := []string{"One", "Two"}
	for _, field := range expectedPayloads {
		require.NoError(t, task.Enqueue(ctx, q.SQS, j, &FirstPayload{First: field}))
	}
	processedPayloads := make([]string, 0)
	timer := time.After(time.Second * 10)
Loop:
	for {
		if len(processedPayloads) >= len(expectedPayloads) {
			break Loop
		}
		select {
		case processPayload := <-outC:
			processedPayloads = append(processedPayloads, processPayload.First)
		case <-timer:
			require.Fail(t, "Processing took too long")
		}
	}
	slices.Sort(expectedPayloads)
	slices.Sort(processedPayloads)
	assert.Equal(t, expectedPayloads, processedPayloads, "Should process all messages")

	require.NoError(t, p.Stop())
}
