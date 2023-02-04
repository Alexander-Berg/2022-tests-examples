package queue

import (
	"testing"
	"time"

	"github.com/aws/aws-sdk-go/aws"
	"github.com/aws/aws-sdk-go/service/sqs"
	"github.com/stretchr/testify/assert"
)

func TestQueue_Poll(t *testing.T) {
	ctx := PrepareContext(t)
	q := PrepareQueue(t, ctx)

	pollInterval := time.Second
	poller, err := q.Poll(ctx, pollInterval, &sqs.ReceiveMessageInput{WaitTimeSeconds: aws.Int64(1)})
	if err != nil {
		t.Fatal(err)
	}

	sentC := make(chan string)
	go func() {
		sentC <- PrepareMessage(t, ctx, q)
		<-time.After(pollInterval)
		sentC <- PrepareMessage(t, ctx, q)

		close(sentC)
	}()

	for body := range sentC {
		select {
		case msg := <-poller.Messages():
			assert.Equal(t, body, msg.Body(), "Should poll same message")
		case <-time.After(pollInterval + time.Second):
			t.Fatalf("Didn't get message after poll interval")
		}
	}
}
