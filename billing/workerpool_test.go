package workerpool

import (
	"context"
	"testing"
	"time"

	"github.com/stretchr/testify/assert"
	"golang.org/x/exp/slices"

	"a.yandex-team.ru/billing/library/go/billingo/pkg/xlog"
	"a.yandex-team.ru/library/go/core/log"
	zaplog "a.yandex-team.ru/library/go/core/log/zap"
)

func init() {
	logger, err := zaplog.NewDeployLogger(log.DebugLevel)
	if err != nil {
		panic(err)
	}
	xlog.SetGlobalLogger(logger)
}

func Pool(t *testing.T, maxWorkers int) *WorkerPool {
	pool, err := New(context.Background(), maxWorkers, BufferCapacity(2), PoolName("test_pool"))
	if err != nil {
		t.Fatal(err)
	}
	return pool
}

func TestWorkerPool_Smoke(t *testing.T) {
	t.Parallel()

	pool := Pool(t, 2)

	in := []int{0, 1, 2}
	outC := make(chan int, len(in))
	for _, num := range in {
		// здесь нужно пересоздавать переменную т.к. цикл использует одну и ту же num на разных итерациях,
		// если в замыкании использовать num то используется значение на момент исполнения функции, а не создания
		x := num
		if err := pool.Submit(func(ctx context.Context) {
			xlog.Debug(ctx, "Inside submitted task")
			outC <- x
		}); err != nil {
			t.Fatal(err)
		}
	}

	if err := pool.Stop(); err != nil {
		t.Fatal(err)
	}

	close(outC)

	var res []int
	for val := range outC {
		res = append(res, val)
	}
	slices.Sort(res)
	assert.Equal(t, in, res)
}

func TestWorkerPool_ZeroWorkers(t *testing.T) {
	t.Parallel()

	_, err := New(context.Background(), 0)
	assert.Error(t, err)
}

func TestWorkerPool_BufferCapacity(t *testing.T) {
	t.Parallel()

	ctx := context.Background()
	_, err := New(ctx, 1, BufferCapacity(0))
	assert.Error(t, err)

	pool, err := New(ctx, 1, BufferCapacity(1))
	if err != nil {
		t.Fatal(err)
	}
	assert.Equal(t, 1, pool.Capacity())

	if err := pool.Stop(); err != nil {
		t.Fatal(err)
	}
}

func TestWorkerPool_ConcurrencyLimit(t *testing.T) {
	t.Parallel()

	pool := Pool(t, 3)

	var tasks []int
	for i := 0; i < pool.Capacity()+1; i++ {
		tasks = append(tasks, i)
	}

	startedC := make(chan int, len(tasks))
	blockC := make(chan struct{})
	finishedC := make(chan int, len(tasks))
	go func() {
		for _, num := range tasks {
			// здесь нужно пересоздавать переменную т.к. цикл использует одну и ту же num на разных итерациях,
			// если в замыкании использовать num то используется значение на момент исполнения функции, а не создания
			x := num
			// игнорю ошибку, т.к. нельзя вызвать t.Fatal в горутине
			//а передавать наружу не имеет смысла, т.к. аналогичные проверки происходят в других тестах
			_ = pool.Submit(func(ctx context.Context) {
				startedC <- x
				// не даст таске завершиться пока не закроем канал
				<-blockC
				finishedC <- x
			})
		}
	}()

	time.Sleep(100 * time.Millisecond)
	assert.Equal(t, pool.Capacity(), len(startedC))
	assert.Equal(t, 0, len(finishedC))

	close(blockC)

	if err := pool.Stop(); err != nil {
		t.Fatal(err)
	}

	assert.Equal(t, len(tasks), len(startedC))
	assert.Equal(t, len(tasks), len(finishedC))
	close(startedC)
	close(finishedC)

	var finished []int
	for val := range finishedC {
		finished = append(finished, val)
	}
	slices.Sort(finished)
	assert.Equal(t, tasks, finished)
}

func TestWorkerPool_SubmitBlocking(t *testing.T) {
	t.Parallel()

	testCases := []struct {
		bufferCapacity int
		name           string
	}{
		{
			bufferCapacity: 1,
			name:           "one task buffer",
		},
		{
			bufferCapacity: 2,
			name:           "two tasks buffer",
		},
		{
			bufferCapacity: 3,
			name:           "three tasks buffer",
		},
	}
	for _, testCase := range testCases {
		t.Run(testCase.name, func(t *testing.T) {
			pool, err := New(
				context.Background(),
				1,
				BufferCapacity(testCase.bufferCapacity),
				PoolBlockedTimeout(time.Millisecond*50),
				PoolName("test_pool"),
			)
			if err != nil {
				t.Fatal(err)
			}

			doneC := make(chan struct{})
			if err := pool.Submit(func(ctx context.Context) {
				time.Sleep(100 * time.Millisecond)
				close(doneC)
			}); err != nil {
				t.Fatal(err)
			}
			select {
			case <-doneC:
				t.Fatal("Submit did not return immediately")
			default:
			}

			blockC := make(chan struct{})
			for i := 0; i < pool.Capacity()+pool.BufferCapacity(); i++ {
				if err := pool.Submit(func(ctx context.Context) {
					time.Sleep(100 * time.Millisecond)
					<-blockC
				}); err != nil {
					t.Fatal(err)
				}
			}

			select {
			case <-doneC:
			default:
				t.Fatal("Submit did not wait for first task to execute")
			}
			close(blockC)

			if err := pool.Stop(); err != nil {
				t.Fatal(err)
			}
		})
	}
}

func TestWorkerPool_SubmitWait(t *testing.T) {
	t.Parallel()

	pool := Pool(t, 1)

	doneC := make(chan struct{})
	if err := pool.SubmitWait(func(ctx context.Context) {
		time.Sleep(100 * time.Millisecond)
		close(doneC)
	}); err != nil {
		t.Fatal(err)
	}
	select {
	case <-doneC:
	default:
		t.Fatal("SubmitWait did not block until task finished")
	}

	if err := pool.Stop(); err != nil {
		t.Fatal(err)
	}
}

func TestWorkerPool_NilTask(t *testing.T) {
	t.Parallel()

	pool := Pool(t, 1)

	err := pool.Submit(nil)
	assert.Error(t, err)

	err = pool.SubmitWait(nil)
	assert.Error(t, err)

	if err := pool.Stop(); err != nil {
		t.Fatal(err)
	}
}

func TestWorkerPool_ReuseWorkers(t *testing.T) {
	t.Parallel()

	pool := Pool(t, 3)

	release := make(chan struct{})

	// Запускаем в пул задачу, которая завершится до следующего вызова Submit
	for i := 0; i < 10; i++ {
		if err := pool.Submit(func(ctx context.Context) {
			<-release
		}); err != nil {
			t.Fatal(err)
		}
		release <- struct{}{}
		time.Sleep(time.Millisecond)
	}
	close(release)

	assert.Equal(t, 1, pool.Size())

	if err := pool.Stop(); err != nil {
		t.Fatal(err)
	}
}

func TestWorkerPool_Size(t *testing.T) {
	t.Parallel()

	pool := Pool(t, 2)

	release := make(chan struct{})
	if err := pool.Submit(func(ctx context.Context) {
		<-release
	}); err != nil {
		t.Fatal(err)
	}
	time.Sleep(time.Millisecond)
	assert.Equal(t, 1, pool.Size())

	if err := pool.Submit(func(ctx context.Context) {
		<-release
	}); err != nil {
		t.Fatal(err)
	}
	time.Sleep(time.Millisecond)
	assert.Equal(t, 2, pool.Size())

	if err := pool.Submit(func(ctx context.Context) {
		<-release
	}); err != nil {
		t.Fatal(err)
	}
	time.Sleep(time.Millisecond)
	assert.Equal(t, 2, pool.Size())

	close(release)

	if err := pool.Stop(); err != nil {
		t.Fatal(err)
	}
	assert.Equal(t, 0, pool.Size())
}

func TestWorkerPool_KillIdle(t *testing.T) {
	t.Parallel()

	pool, err := New(context.Background(), 2, WorkerIdleTimeout(time.Millisecond*100))
	if err != nil {
		t.Fatal(err)
	}

	if err := pool.Submit(func(ctx context.Context) {}); err != nil {
		t.Fatal(err)
	}
	time.Sleep(pool.Options.WorkerIdleTimeout * 2)
	assert.Equal(t, 0, pool.Size())

	if err := pool.Stop(); err != nil {
		t.Fatal(err)
	}
}

func TestWorkerPool_ContextCancelFinishTasks(t *testing.T) {
	t.Parallel()

	ctx, cancel := context.WithCancel(context.Background())
	pool, err := New(ctx, 2)
	if err != nil {
		t.Fatal(err)
	}

	doneC := make(chan struct{})
	if err := pool.Submit(func(ctx context.Context) {
		time.Sleep(100 * time.Millisecond)
		close(doneC)
	}); err != nil {
		t.Fatal(err)
	}

	go func() {
		xlog.Debug(pool.ctx, "Cancelled context")
		cancel()
	}()
	if err := pool.WaitContext(ctx); err != nil {
		t.Fatal(err)
	}

	select {
	case <-doneC:
	default:
		t.Fatal("Didn't wait task finish after context cancel")
	}
}

func TestWorkerPool_StopTimeout(t *testing.T) {
	t.Parallel()

	pool := Pool(t, 2)

	taskDuration := pool.Options.PoolStopTimeout + (time.Millisecond * 100)
	doneC := make(chan struct{})
	if err := pool.Submit(func(ctx context.Context) {
		time.Sleep(taskDuration)
		close(doneC)
	}); err != nil {
		t.Fatal(err)
	}

	stopStarted := time.Now()
	err := pool.Stop()
	stopEnded := time.Now()
	assert.WithinDuration(t, stopStarted.Add(pool.Options.PoolStopTimeout), stopEnded, time.Millisecond*100)

	assert.Error(t, err)
}
