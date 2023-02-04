package background

import (
	"context"
	"testing"
	"time"

	"github.com/golang/mock/gomock"
	"github.com/stretchr/testify/require"

	mock2 "a.yandex-team.ru/billing/hot/scheduler/mock"
	"a.yandex-team.ru/billing/hot/scheduler/pkg/core/actions/impl"
	"a.yandex-team.ru/library/go/core/xerrors"
)

func TestCheckerError(t *testing.T) {
	ctrl := gomock.NewController(t)
	storage := mock2.NewMockStorage(ctrl)
	acts := impl.NewActions(storage)

	expectedErr := xerrors.New("anything")
	storage.EXPECT().UpdateDoneTasks(gomock.Any()).Times(4).Return(expectedErr)

	ctx, cancel := context.WithTimeout(context.Background(), 3*time.Second)
	defer cancel()

	checker := NewTaskStatusUpdater(acts, nil)
	err := checker.Run(ctx, 10*time.Second)
	require.ErrorIs(t, err, expectedErr)
}

func TestCheckerOK(t *testing.T) {
	ctrl := gomock.NewController(t)
	storage := mock2.NewMockStorage(ctrl)
	acts := impl.NewActions(storage)

	ctx, cancel := context.WithCancel(context.Background())
	defer cancel()

	storage.EXPECT().UpdateDoneTasks(gomock.Any()).DoAndReturn(func(context.Context) error {
		cancel()
		return nil
	})

	checker := NewTaskStatusUpdater(acts, nil)
	err := checker.Run(ctx, 10*time.Second)
	require.ErrorIs(t, err, context.Canceled)
}
