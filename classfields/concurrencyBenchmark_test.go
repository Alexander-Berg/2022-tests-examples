package fifo

import (
	"math/rand"
	"sync"
	"testing"
	"time"
)

const concurrencyCount = 16

type TestFunc struct {
	fifo queue
	push bool
	i    *int
}

func (tf *TestFunc) start(wg *sync.WaitGroup) {
	if tf.push {
		tf.fifo.Push(tf.i)
	} else {
		tf.fifo.Pop()
	}
	wg.Done()
}

func BenchmarkConcurrencyList(b *testing.B) {
	concurrency(b, NewListFifo())
}

func BenchmarkConcurrencySimple(b *testing.B) {
	concurrency(b, NewSimpleFifo())
}

func BenchmarkConcurrencyFixableChannel(b *testing.B) {
	concurrency(b, NewFlexibleChannelFifo())
}

func BenchmarkConcurrencyChannel(b *testing.B) {
	concurrency(b, NewChannelFifo(b.N))
}

func BenchmarkConcurrencyReuse(b *testing.B) {
	concurrency(b, NewReuseFifo())
}

func BenchmarkConcurrencyChunk(b *testing.B) {
	concurrency(b, NewChunk())
}

func concurrency(b *testing.B, fifo queue) {
	random := rand.New(rand.NewSource(time.Now().UnixNano()))
	for i := 0; i < b.N/2; i++ {
		r := random.Intn(1000)
		fifo.Push(&r)
	}
	b.ResetTimer()
	for i := 0; i < b.N; i++ {
		b.StopTimer()
		wg := &sync.WaitGroup{}
		var fs []TestFunc
		start := i
		for j := start; j < b.N && j < start+concurrencyCount; j++ {
			r := random.Intn(1000)
			tf := TestFunc{
				fifo: fifo,
				push: random.Intn(2) == 1,
				i:    &r,
			}
			fs = append(fs, tf)
			i++
		}
		wg.Add(len(fs))
		b.StartTimer()
		for _, tf := range fs {
			go tf.start(wg)
		}
		wg.Wait()
	}
}
