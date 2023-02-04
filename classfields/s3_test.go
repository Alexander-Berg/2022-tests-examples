package snapshot

import (
	"context"
	"github.com/YandexClassifieds/vtail/api/core"
	"github.com/YandexClassifieds/vtail/test"
	"github.com/aws/aws-sdk-go/aws"
	"github.com/aws/aws-sdk-go/aws/credentials"
	"github.com/aws/aws-sdk-go/aws/session"
	"github.com/google/uuid"
	"github.com/spf13/viper"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
	"testing"
	"time"
)

func TestNewS3SnapshotStorage(t *testing.T) {
	test.InitConfig(t)
	awsSession, err := session.NewSession(&aws.Config{
		Credentials: credentials.NewStaticCredentials(viper.GetString("S3_KEY_ID"), viper.GetString("S3_KEY_SECRET"), ""),
		Region:      aws.String("us-east-1"),
		Endpoint:    aws.String(viper.GetString("S3_ENDPOINT")),
	})
	require.NoError(t, err)
	ss := NewS3SnapshotStorage(awsSession, S3Config{
		Bucket:    viper.GetString("S3_BUCKET"),
		KeyPrefix: "test",
	})
	ctx, cancel := context.WithTimeout(context.Background(), time.Second*10)
	defer cancel()

	ll := &core.LogList{Msg: []*core.LogMessage{
		{Message: "one"},
		{Message: "two"},
	}}
	snapshotKey := uuid.New().String()

	err = ss.Put(ctx, snapshotKey, ll)
	require.NoError(t, err)
	time.Sleep(time.Second / 10)
	res, err := ss.Get(ctx, snapshotKey)
	require.NoError(t, err)
	assert.Equal(t, ll.String(), res.String())

	t.Run("not_found", func(t *testing.T) {
		_, err := ss.Get(ctx, "nonexistent-key")
		require.Equal(t, ErrNotFound, err)
	})
	t.Run("cancel", func(t *testing.T) {
		ctx, cancel := context.WithCancel(context.Background())
		cancel()
		_, err := ss.Get(ctx, snapshotKey)
		require.Equal(t, context.Canceled, err)
	})
	t.Run("deadline", func(t *testing.T) {
		ctx, cancel := context.WithDeadline(context.Background(), time.Time{})
		defer cancel()
		_, err := ss.Get(ctx, snapshotKey)
		require.Equal(t, context.DeadlineExceeded, err)
	})
}
