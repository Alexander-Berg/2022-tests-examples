package queue

import (
	"context"
	"fmt"
	"os"
	"testing"
	"time"

	"github.com/aws/aws-sdk-go/aws"
	"github.com/aws/aws-sdk-go/service/sqs"
	"github.com/gofrs/uuid"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"

	"a.yandex-team.ru/billing/library/go/billingo/pkg/extracontext"
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

func PrepareContext(t *testing.T) extracontext.Context {
	logger, err := zaplog.NewDeployLogger(log.DebugLevel)
	require.NoError(t, err)
	xlog.SetGlobalLogger(logger)

	return extracontext.NewWithParent(context.Background())
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

	svc, err := NewSQSClient(ctx, endpoint, "yandex", nil)
	if err != nil {
		t.Fatal(err)
	}

	return svc
}

func PrepareQueue(t *testing.T, ctx extracontext.Context) *Queue {
	svc := PrepareSQS(t, ctx)

	// яндексовый sqs почему-то позволяет молча создать очередь которая уже существует, что противоречит апи
	q, err := Create(ctx, svc, t.Name(), nil)
	if err != nil {
		t.Fatal(err)
	}

	if err := q.Purge(ctx); err != nil {
		t.Fatal(err)
	}

	return q
}

func PrepareMessage(
	t *testing.T,
	ctx extracontext.Context,
	q *Queue,
) string {
	id, err := uuid.NewV4()
	if err != nil {
		t.Fatalf("Can't generate uuid: %v", err)
	}
	bodyStr := fmt.Sprintf("%s_%s", t.Name(), id.String())
	if err := q.Send(ctx, &sqs.SendMessageInput{MessageBody: aws.String(bodyStr)}); err != nil {
		t.Fatal(err)
	}
	return bodyStr
}

func TestQueue_SendReceiveDelete(t *testing.T) {
	ctx := PrepareContext(t)
	q := PrepareQueue(t, ctx)

	messageBody := PrepareMessage(t, ctx, q)

	msgList, err := q.Receive(ctx, &sqs.ReceiveMessageInput{
		WaitTimeSeconds:   aws.Int64(1),
		VisibilityTimeout: aws.Int64(0),
	})
	if err != nil {
		t.Fatal(err)
	}

	AssertMessage(t, msgList, messageBody)

	err = q.Delete(ctx, msgList[0])
	if err != nil {
		t.Fatal(err)
	}

	msgList, err = q.Receive(ctx, &sqs.ReceiveMessageInput{
		WaitTimeSeconds:   aws.Int64(1),
		VisibilityTimeout: aws.Int64(0),
	})
	if err != nil {
		t.Fatal(err)
	}
	assert.Empty(t, msgList)
}

func TestQueue_VisibilityTimeout(t *testing.T) {
	ctx := PrepareContext(t)
	q := PrepareQueue(t, ctx)
	messageBody := PrepareMessage(t, ctx, q)

	msgList, err := q.Receive(ctx, &sqs.ReceiveMessageInput{
		VisibilityTimeout: aws.Int64(1),
		WaitTimeSeconds:   aws.Int64(1),
	})
	if err != nil {
		t.Fatal(err)
	}
	AssertMessage(t, msgList, messageBody)

	msg := msgList[0]

	visibilityTimeout := int64(2)
	if err := q.ChangeVisibility(ctx, msg, visibilityTimeout); err != nil {
		t.Fatal(err)
	}

	start := time.Now()
	timer := time.After(time.Second * time.Duration(visibilityTimeout+1))
Loop:
	for {
		select {
		case <-timer:
			t.Fatal("Couldn't receive message after visibility timeout")
		default:
			msgList, err = q.Receive(ctx, &sqs.ReceiveMessageInput{WaitTimeSeconds: aws.Int64(1)})
			if err != nil {
				t.Fatal(err)
			}
			if len(msgList) != 0 {
				break Loop
			}
		}
	}

	assert.True(t,
		time.Now().After(start.Add(time.Second*time.Duration(visibilityTimeout))),
		"Should pass more than visibility timeout",
	)
	AssertMessage(t, msgList, messageBody)
	assert.Equal(t, msg.ID(), msgList[0].ID(), "Should receive the same message")

}

func AssertMessage(t *testing.T, msgList []*Message, expectedBody string) {
	require.Equal(t, 1, len(msgList), "Should receive one message")
	assert.Equal(t, expectedBody, msgList[0].Body())
}
