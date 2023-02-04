package queue

import (
	"testing"
	"time"

	"github.com/aws/aws-sdk-go/aws"
	"github.com/aws/aws-sdk-go/service/sqs"
	"github.com/stretchr/testify/assert"
)

// todo-igogor долгий тест (5-6 секунд)
func TestQueue_KeepInvisible(t *testing.T) {
	ctx := PrepareContext(t)
	q := PrepareQueue(t, ctx)

	messageBody := PrepareMessage(t, ctx, q)

	msgList, err := q.Receive(ctx, &sqs.ReceiveMessageInput{
		VisibilityTimeout: aws.Int64(2),
		WaitTimeSeconds:   aws.Int64(1),
	})
	if err != nil {
		t.Fatal(err)
	}
	AssertMessage(t, msgList, messageBody)
	msg := msgList[0]

	keeper, err := msg.KeepInvisible(ctx)
	if err != nil {
		t.Fatal(err)
	}

	timer := time.After(keeper.UpdatePeriod() + time.Second)
Loop:
	for {
		select {
		case <-timer:
			break Loop
		default:
			msgList, err = q.Receive(ctx, &sqs.ReceiveMessageInput{WaitTimeSeconds: aws.Int64(1)})
			if err != nil {
				t.Fatal(err)
			}
			if len(msgList) != 0 {
				t.Fatalf("Message should've been kept invisible")
			}
		}
	}

	keeper.Stop()

	msgList, err = q.Receive(
		ctx,
		&sqs.ReceiveMessageInput{WaitTimeSeconds: aws.Int64(keeper.VisibilityTimeout + 1)},
	)
	if err != nil {
		t.Fatal(err)
	}
	AssertMessage(t, msgList, messageBody)
	assert.Equal(t, msg.ID(), msgList[0].ID(), "Should receive the same message")
}
