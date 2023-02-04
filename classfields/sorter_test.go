package sort_test

import (
	"testing"
	"time"

	"github.com/YandexClassifieds/vtail/api/core"
	"github.com/YandexClassifieds/vtail/cmd/backend/sort"
	"github.com/YandexClassifieds/vtail/test"
	"github.com/stretchr/testify/require"
	"go.uber.org/goleak"
	"google.golang.org/protobuf/types/known/timestamppb"
)

func TestOrder(t *testing.T) {
	defer goleak.VerifyNone(t)

	logger := test.NewTestLogger()
	sorter := sort.NewSorter(time.Millisecond, 1024*1024, logger)
	output, _ := sorter.AddSortLine("abc")

	err := sorter.Push("abc", messageFixture(t, 324))
	require.NoError(t, err)
	err = sorter.Push("abc", messageFixture(t, 323))
	require.NoError(t, err)

	msg1 := <-output
	require.Equal(t, messageFixture(t, 323), msg1)

	msg2 := <-output
	require.Equal(t, messageFixture(t, 324), msg2)

	sorter.Close()
	_, ok := <-output
	require.False(t, ok)
}

func TestSeveralFlows(t *testing.T) {
	defer goleak.VerifyNone(t)

	logger := test.NewTestLogger()
	sorter := sort.NewSorter(time.Microsecond, 1024*1024, logger)
	output, _ := sorter.AddSortLine("abc")
	output2, _ := sorter.AddSortLine("abc2")

	err := sorter.Push("abc", messageFixture(t, 323))
	require.NoError(t, err)
	err = sorter.Push("abc2", messageFixture(t, 324))
	require.NoError(t, err)

	msg1 := <-output
	require.Equal(t, messageFixture(t, 323), msg1)

	msg2 := <-output2
	require.Equal(t, messageFixture(t, 324), msg2)

	sorter.Close()
	_, ok := <-output
	require.False(t, ok)
}

func TestWindowDuration(t *testing.T) {
	defer goleak.VerifyNone(t)

	logger := test.NewTestLogger()
	sorter := sort.NewSorter(time.Second, 1024*1024, logger)
	output, _ := sorter.AddSortLine("abc")

	err := sorter.Push("abc", messageFixture(t, 324))
	require.NoError(t, err)
	err = sorter.Push("abc", messageFixture(t, 323))
	require.NoError(t, err)

	select {
	case <-output:
		t.Error("unexpected message from channel")
	case <-time.After(500 * time.Millisecond):
	}
	msg1 := <-output
	require.Equal(t, messageFixture(t, 323), msg1)
	msg2 := <-output
	require.Equal(t, messageFixture(t, 324), msg2)

	sorter.Close()
	_, ok := <-output
	require.False(t, ok)
}

func TestRemoveMissingFlow(t *testing.T) {
	defer goleak.VerifyNone(t)

	logger := test.NewTestLogger()
	sorter := sort.NewSorter(time.Microsecond, 1024*1024, logger)
	err := sorter.RemoveSortLine("abc")
	require.Error(t, err)
}

func TestPushToMissingFlow(t *testing.T) {
	defer goleak.VerifyNone(t)

	logger := test.NewTestLogger()
	sorter := sort.NewSorter(time.Microsecond, 1024*1024, logger)
	err := sorter.Push("abc", messageFixture(t, 324))
	require.Error(t, err)
}

func TestFastRemoveFlow(t *testing.T) {
	defer goleak.VerifyNone(t)

	logger := test.NewTestLogger()
	sorter := sort.NewSorter(500*time.Millisecond, 1024*1024, logger)
	output, _ := sorter.AddSortLine("abc")

	err := sorter.Push("abc", messageFixture(t, 323))
	require.NoError(t, err)

	err = sorter.RemoveSortLine("abc")
	require.NoError(t, err)

	_, ok := <-output
	require.False(t, ok)
}

func messageFixture(t *testing.T, nsec int64) *core.LogMessage {
	return &core.LogMessage{
		Timestamp: timestamppb.New(time.Unix(1580458894, nsec)),
		Service:   "srv",
		Version:   "1.2",
		Layer:     "test",
		Level:     "INFO",
		Message:   "random message",
		Rest:      `{"myField":"my field value"}`,
	}
}

func TestBigThroughput(t *testing.T) {
	defer goleak.VerifyNone(t)

	logger := test.NewTestLogger()
	sorter := sort.NewSorter(time.Millisecond, 1024*1024, logger)
	output, _ := sorter.AddSortLine("abc")

	closer := make(chan struct{})
	defer close(closer)
	go func() {
		for {
			_ = sorter.Push("abc", messageFixture(t, 324))
			select {
			case <-closer:
				return
			default:
			}
		}
	}()

	time.Sleep(sort.FlushPeriod)
	sorter.Close()
	time.Sleep(time.Millisecond)
	_, ok := <-output
	require.False(t, ok)
}

func TestBufLimitAtStart(t *testing.T) {
	defer goleak.VerifyNone(t)

	message := messageFixture(t, 324)

	logger := test.NewTestLogger()
	sorter := sort.NewSorter(time.Millisecond, sort.MessageSize(message)*2+1, logger)
	defer sorter.Close()
	_, overflow := sorter.AddSortLine("abc")

	err := sorter.Push("abc", message)
	require.NoError(t, err)
	err = sorter.Push("abc", message)
	require.NoError(t, err)
	select {
	case <-overflow:
		require.FailNow(t, "unexpected overflow")
	default:
	}

	err = sorter.Push("abc", message)
	require.Error(t, err)
	require.Equal(t, sort.ErrSizeLimitExceeded, err)
	select {
	case <-overflow:
	default:
		require.FailNow(t, "overflow expected")
	}
}

func TestBufLimitInTime(t *testing.T) {
	defer goleak.VerifyNone(t)

	message := messageFixture(t, 323)

	logger := test.NewTestLogger()
	sorter := sort.NewSorter(time.Millisecond, sort.MessageSize(message)*2, logger)
	defer sorter.Close()
	output, overflow := sorter.AddSortLine("abc")

	err := sorter.Push("abc", message)
	require.NoError(t, err)

	msg1 := <-output
	require.Equal(t, message, msg1)

	err = sorter.Push("abc", message)
	require.NoError(t, err)
	err = sorter.Push("abc", message)
	require.NoError(t, err)
	select {
	case <-overflow:
		require.FailNow(t, "unexpected overflow")
	default:
	}

	err = sorter.Push("abc", message)
	require.Error(t, err)
	require.Equal(t, sort.ErrSizeLimitExceeded, err)
	select {
	case <-overflow:
	default:
		require.FailNow(t, "overflow expected")
	}
}
