package fifo

import (
	"math/rand"
	"testing"
	"time"
)

func BenchmarkPopList(b *testing.B) {
	pop(b, NewListFifo())
}

func BenchmarkPopSimple(b *testing.B) {
	pop(b, NewSimpleFifo())
}

func BenchmarkPopFixableChannel(b *testing.B) {
	pop(b, NewFlexibleChannelFifo())
}

func BenchmarkPopChannel(b *testing.B) {
	pop(b, NewChannelFifo(b.N))
}

func BenchmarkPopReuse(b *testing.B) {
	pop(b, NewReuseFifo())
}

func BenchmarkPopChunk(b *testing.B) {
	pop(b, NewChunk())
}

func pop(b *testing.B, fifo queue) {
	random := rand.New(rand.NewSource(time.Now().UnixNano()))
	for i := 0; i < b.N; i++ {
		r := random.Intn(1000)
		fifo.Push(&r)
	}
	b.ResetTimer()
	for i := 0; i < b.N; i++ {
		fifo.Pop()
	}
}
