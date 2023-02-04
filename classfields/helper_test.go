package handler

import (
	"context"
	"fmt"
	"github.com/stretchr/testify/require"
	"testing"
	"time"
)

func TestRunParallel_BadInput(t *testing.T) {
	errSlice := RunParallel(context.Background(), -1, nil)
	require.Len(t, errSlice, 1)
	errSlice = RunParallel(context.Background(), 0, nil)
	require.Len(t, errSlice, 1)
	errSlice = RunParallel(context.Background(), 1, nil)
	require.Len(t, errSlice, 1)
}

func TestRunParallel_Deadline(t *testing.T) {
	ctx, cancelFunc := context.WithTimeout(context.Background(), time.Second/2)
	defer cancelFunc()
	errSlice := RunParallel(ctx, 2, func(_ context.Context, i int) error {
		if i == 0 {
			for {
				select {
				case <-ctx.Done():
					return ctx.Err()
				default:
				}
			}
		}
		return nil
	})

	require.Equal(t, context.DeadlineExceeded, ctx.Err())
	require.Len(t, errSlice, 1)
	require.Equal(t, errSlice[0], context.DeadlineExceeded)
}

func TestRunParallel_OneError(t *testing.T) {
	expectedErr := fmt.Errorf("err")
	errSlice := RunParallel(context.Background(), 2, func(_ context.Context, i int) error {
		if i == 0 {
			return expectedErr
		}
		return nil
	})

	require.Len(t, errSlice, 1)
	require.Equal(t, errSlice[0], expectedErr)
}

func TestRunParallel_Errors(t *testing.T) {
	expectedErr := fmt.Errorf("err")
	errSlice := RunParallel(context.Background(), 2, func(_ context.Context, i int) error {
		return expectedErr
	})

	require.Len(t, errSlice, 2)
	require.Equal(t, errSlice[0], expectedErr)
	require.Equal(t, errSlice[1], expectedErr)
}
