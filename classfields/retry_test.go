package retry

import (
	"context"
	"fmt"
	"testing"
	"time"

	"github.com/stretchr/testify/require"
)

func TestDeadline(t *testing.T) {
	ctx, cancelF := context.WithTimeout(context.Background(), time.Second)
	defer cancelF()

	err := UntilSuccess(ctx, func(ctx context.Context) (retry bool, err error) {
		return true, nil
	}, BackoffLinearWithJitter(0, 0))
	require.Error(t, err)
	require.Equal(t, context.DeadlineExceeded, err)
	deadline, _ := ctx.Deadline()
	require.WithinDuration(t, deadline, time.Now(), time.Second/2)
	require.True(t, time.Now().After(deadline))
}

func TestCancel(t *testing.T) {
	ctx, cancelF := context.WithTimeout(context.Background(), time.Second)
	go func() {
		time.Sleep(time.Second / 2)
		cancelF()
	}()
	err := UntilSuccess(ctx, func(ctx context.Context) (retry bool, err error) {
		return true, nil
	}, BackoffLinearWithJitter(0, 0))

	require.Error(t, err)
	require.Equal(t, context.Canceled, err)
	deadline, _ := ctx.Deadline()
	require.True(t, time.Now().Before(deadline))
}

func TestSuccessWithinDeadline(t *testing.T) {
	ctx, cancelF := context.WithTimeout(context.Background(), time.Second)
	defer cancelF()

	after := time.After(time.Second / 2)
	err := UntilSuccess(ctx, func(ctx context.Context) (retry bool, err error) {
		select {
		case <-after:
			return false, nil
		default:
			return true, nil
		}
	}, BackoffLinearWithJitter(defaultLinearBackoffTimeout, defaultLinearBackoffJitter))

	require.NoError(t, err)
	deadline, _ := ctx.Deadline()
	require.True(t, time.Now().Before(deadline))
}

func TestSuccessAfterDeadline(t *testing.T) {
	ctx, cancelF := context.WithTimeout(context.Background(), time.Second)
	defer cancelF()

	after := time.After(time.Second * 2)
	err := UntilSuccess(ctx, func(ctx context.Context) (retry bool, err error) {
		select {
		case <-after:
			return false, nil
		default:
			return true, nil
		}
	}, BackoffLinearWithJitter(defaultLinearBackoffTimeout, defaultLinearBackoffJitter))
	require.Error(t, err)
	require.Equal(t, context.DeadlineExceeded, err)
	deadline, _ := ctx.Deadline()
	require.WithinDuration(t, deadline, time.Now(), time.Second/2)
	require.True(t, time.Now().After(deadline))
}

func TestBackoffParam(t *testing.T) {
	ctx, cancelF := context.WithTimeout(context.Background(), time.Second)
	defer cancelF()

	expected := 0
	_ = UntilSuccess(ctx, func(ctx context.Context) (retry bool, err error) {
		return true, nil
	}, func(attempt int) time.Duration {
		require.Equal(t, expected, attempt)
		expected = attempt + 1
		return 0
	})
}

func TestBackoffSuccess(t *testing.T) {
	ctx, cancelF := context.WithTimeout(context.Background(), time.Second/2)
	defer cancelF()

	i := 0
	// 50ms, 100ms, 200ms
	err := UntilSuccess(ctx, func(ctx context.Context) (retry bool, err error) {
		i++
		if i == 4 {
			return false, nil
		}
		return true, nil
	}, BackoffExponentialWithJitter(defaultExponentialBackoffBase, time.Second))
	require.NoError(t, err)
}

func TestBackoffFail(t *testing.T) {
	ctx, cancelF := context.WithTimeout(context.Background(), time.Second/2)
	defer cancelF()

	err := UntilSuccess(ctx, func(ctx context.Context) (retry bool, err error) {
		return true, fmt.Errorf("should retry")
	}, BackoffExponentialWithJitter(defaultExponentialBackoffBase, time.Second))

	require.Error(t, err)
	require.EqualError(t, err, "retry timed out: should retry")
}

func TestReturnError(t *testing.T) {
	expectedError := fmt.Errorf("my error")

	err := UntilSuccess(context.Background(), func(ctx context.Context) (retry bool, err error) {
		return false, expectedError
	}, BackoffExponentialWithJitter(defaultExponentialBackoffBase, time.Second))
	require.Error(t, err)
	require.Equal(t, expectedError, err)
}
