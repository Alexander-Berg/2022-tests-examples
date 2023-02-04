package enqueuer

import (
	"context"
	"testing"

	"github.com/aws/aws-sdk-go/aws"
	"github.com/aws/aws-sdk-go/service/sqs"
	"github.com/golang/mock/gomock"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"

	"a.yandex-team.ru/billing/library/go/billingo/mock"
)

func TestGetQueue(t *testing.T) {
	ctx := context.Background()
	ctrl := gomock.NewController(t)
	sqsapi := mock.NewMockSQSAPI(ctrl)

	sqsapi.EXPECT().GetQueueUrlWithContext(gomock.Any(), gomock.Any()).Return(&sqs.GetQueueUrlOutput{
		QueueUrl: aws.String("url"),
	}, nil)
	sqsapi.EXPECT().GetQueueAttributesWithContext(gomock.Any(), gomock.Any()).Return(&sqs.GetQueueAttributesOutput{
		Attributes: map[string]*string{"key": aws.String("value")},
	}, nil)

	cache := sqsQueues{sqs: sqsapi}

	q, err := cache.Get(ctx, "queue")
	require.NoError(t, err)

	q2, err := cache.Get(ctx, "queue")
	require.NoError(t, err)

	assert.Equal(t, q, q2, "second time queue must be taken from cache")
}
