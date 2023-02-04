package ytreferences

import (
	"context"
	"fmt"
	"testing"
	"time"

	"github.com/stretchr/testify/require"

	"a.yandex-team.ru/library/go/core/xerrors"
)

func TestRefreshersPool_Events_refreshersNotEmpty(t *testing.T) {
	t.Parallel()

	const refreshPeriod = 100 * time.Millisecond

	ctx, cancel := context.WithTimeout(context.Background(), refreshPeriod+refreshPeriod/2)

	firstRefresher := NewRefresher(
		"//test/table_1",
		refreshPeriod,
		newCacheMock(LastRefreshSucceed),
	)

	secondRefresher := NewRefresher(
		"//test/table_2",
		refreshPeriod,
		newCacheMock(LastRefreshSucceed),
	)

	thirdRefresher := NewRefresher(
		"//test/table_3",
		refreshPeriod,
		newCacheMock(LastRefreshFailed),
	)

	refreshersPool := NewRefreshersPool()
	refreshersPool.Add(firstRefresher)
	refreshersPool.Add(secondRefresher)
	refreshersPool.Add(thirdRefresher)

	refreshersPool.Run(ctx)

	// 3 запуска, два успешных один нет, плюс один по времени
	expectedStarted, expectedRefreshed, expectedFailed := 3, 4, 2
	total := expectedStarted + expectedRefreshed + expectedFailed
	started, refreshed, failed := 0, 0, 0

	for i := 0; i < total; i++ {
		e := <-refreshersPool.Events()
		switch e.Kind() {
		case RefresherStarted:
			started++
		case CacheRefreshed:
			refreshed++
		case CacheRefreshFailed:
			failed++
		}
	}

	require.Equal(t, expectedStarted, started)
	require.Equal(t, expectedRefreshed, refreshed)
	require.Equal(t, expectedFailed, failed)

	cancel()

	for i := 0; i < 3; i++ {
		e := <-refreshersPool.Events()
		require.Equal(t, RefresherStopped, e.Kind())
	}
}

func TestRefreshersPool_Events_refreshersEmpty(t *testing.T) {
	t.Parallel()

	const testTime = 100 * time.Millisecond

	refreshersPool := NewRefreshersPool()
	ctx := context.Background()
	refreshersPool.Run(ctx)

	for {
		select {
		case <-refreshersPool.Events():
			require.FailNow(t, "Event in empty refreshers pool")
		case <-time.After(testTime):
			return
		}
	}
}

func TestRefreshersPool_Status(t *testing.T) {
	t.Parallel()

	ctx := context.Background()

	const refreshPeriod = 10 * time.Millisecond

	testCases := []struct {
		name                string
		lastRefreshStatuses []LastRefreshStatus
		expectedStatus      LastRefreshStatus
	}{
		{
			name: "one `refresh failed` => all `refresh failed`",
			lastRefreshStatuses: []LastRefreshStatus{
				LastRefreshSucceed,
				LastRefreshSucceed,
				LastRefreshFailed,
				NoRefresh,
			},
			expectedStatus: LastRefreshFailed,
		},
		{
			name: "all `refresh succeed` => all `refresh succeed`",
			lastRefreshStatuses: []LastRefreshStatus{
				LastRefreshSucceed,
				LastRefreshSucceed,
			},
			expectedStatus: LastRefreshSucceed,
		},
		{
			name: "all `refresh succeeds` bot one `no refresh` => all refresh `no refresh`",
			lastRefreshStatuses: []LastRefreshStatus{
				NoRefresh,
				LastRefreshSucceed,
				LastRefreshSucceed,
			},
			expectedStatus: NoRefresh,
		},
		{
			name:                "no refreshers => `no refresh`",
			lastRefreshStatuses: nil,
			expectedStatus:      NoRefresh,
		},
	}

	for i := range testCases {
		c := testCases[i]

		t.Run(c.name, func(t *testing.T) {
			t.Parallel()
			refreshersPool := NewRefreshersPool()
			for i, status := range c.lastRefreshStatuses {
				cacheInitializer := newCacheMock(status)

				r := NewRefresher(
					fmt.Sprintf("//test/table_%d", i),
					refreshPeriod,
					cacheInitializer,
				)
				go r.Run(ctx)
				e := <-r.Events()
				require.Equal(t, RefresherStarted, e.Kind())

				if status != NoRefresh {
					e = <-r.Events()
					require.True(
						t,
						e.Kind() == CacheRefreshed ||
							e.Kind() == CacheRefreshFailed,
					)
				}
				refreshersPool.Add(r)
			}

			refreshersPool.Run(ctx)
			require.Equal(t, c.expectedStatus, refreshersPool.Status())
		})
	}
}

func TestNewRefresher(t *testing.T) {
	t.Parallel()

	ctx := context.Background()

	testCases := []struct {
		name          string
		refreshPeriod time.Duration
		refreshCount  int
	}{
		{
			name:          "refresh time = 10ms and refresh count = 15",
			refreshPeriod: 10 * time.Millisecond,
			refreshCount:  15,
		},
		{
			name:          "refresh time = 20ms and refresh count = 10",
			refreshPeriod: 20 * time.Millisecond,
			refreshCount:  10,
		},
		{
			name:          "refresh time = 40ms and refresh count = 5",
			refreshPeriod: 40 * time.Millisecond,
			refreshCount:  5,
		},
		{
			name:          "refresh time = 80ms and refresh count = 1",
			refreshPeriod: 80 * time.Millisecond,
			refreshCount:  1,
		},
	}

	for i := range testCases {
		c := testCases[i]
		t.Run(c.name, func(t *testing.T) {
			t.Parallel()

			ctx, cancel := context.WithCancel(ctx)

			refresher := NewRefresher(
				"//test/table",
				c.refreshPeriod,
				newCacheMock(LastRefreshSucceed),
			)

			go refresher.Run(ctx)
			e := <-refresher.Events()
			require.Equal(t, RefresherStarted, e.Kind())
			e = <-refresher.Events()
			require.Equal(t, CacheRefreshed, e.Kind())

			start := time.Now()
			for i := 0; i < c.refreshCount; i++ {
				e = <-refresher.Events()
				require.Equal(t, CacheRefreshed, e.Kind())
			}

			cancel()
			e = <-refresher.Events()
			require.Equal(t, RefresherStopped, e.Kind())

			end := time.Now()

			var (
				actualDelta   = end.Sub(start)
				expectedDelta = time.Duration(c.refreshCount) * c.refreshPeriod
			)

			requireDurationsRoughlyEquals(t, expectedDelta, actualDelta)
		})
	}
}

func TestRefresher_Status(t *testing.T) {
	t.Parallel()

	ctx := context.Background()

	const refreshPeriod = 10 * time.Millisecond

	testCases := []struct {
		name           string
		expectedStatus LastRefreshStatus
	}{
		{
			name:           "last refresh failed",
			expectedStatus: LastRefreshFailed,
		},
		{
			name:           "last refresh succeed",
			expectedStatus: LastRefreshSucceed,
		},
	}

	for i := range testCases {
		c := testCases[i]

		t.Run(c.name, func(t *testing.T) {
			t.Parallel()

			refresher := NewRefresher(
				"//test/table",
				refreshPeriod,
				newCacheMock(c.expectedStatus),
			)
			go refresher.Run(ctx)
			e := <-refresher.Events()
			require.Equal(t, RefresherStarted, e.Kind())

			e = <-refresher.Events()
			require.True(t, e.Kind() == CacheRefreshed || e.Kind() == CacheRefreshFailed)

			require.Equal(t, c.expectedStatus, refresher.Status())
		})
	}
}

type cacheMock struct {
	init func() error
}

func newCacheMock(status LastRefreshStatus) cacheMock {
	var cacheInitializer cacheMock
	switch status {
	case NoRefresh:
		cacheInitializer.init = func() error {
			time.Sleep(1 * time.Hour)

			return nil
		}
	case LastRefreshSucceed:
		cacheInitializer.init = func() error {
			return nil
		}
	case LastRefreshFailed:
		cacheInitializer.init = func() error {
			return xerrors.New("cache initialization failed")
		}
	}

	return cacheInitializer
}

func (m cacheMock) Refresh(_ context.Context) error {
	return m.init()
}

func requireDurationsRoughlyEquals(t *testing.T, expected, actual time.Duration) {
	t.Helper()

	var (
		slop       = expected * 2 / 10
		lowerBound = expected - slop
		upperBound = expected + slop
	)

	require.True(
		t,
		actual > lowerBound && actual < upperBound,
		"actual delta %v not in (%v, %v)",
		actual, lowerBound, upperBound,
	)
}
