package fifo

import (
	"math/rand"
	"testing"
	"time"
)

func BenchmarkPushList(b *testing.B) {
	push(b, NewListFifo())
}

func BenchmarkPushSimple(b *testing.B) {
	push(b, NewSimpleFifo())
}

func BenchmarkPushFixableChannel(b *testing.B) {
	push(b, NewFlexibleChannelFifo())
}

func BenchmarkPushChannel(b *testing.B) {
	push(b, NewChannelFifo(b.N))
}

func BenchmarkPushReuse(b *testing.B) {
	push(b, NewReuseFifo())
}

func BenchmarkPushChunk(b *testing.B) {
	push(b, NewChunk())
}

func push(b *testing.B, fifo queue) {
	random := rand.New(rand.NewSource(time.Now().UnixNano()))
	b.ResetTimer()
	for n := 0; n < b.N; n++ {
		b.StopTimer()
		r := random.Intn(1000)
		b.StartTimer()
		fifo.Push(&r)
	}
}
