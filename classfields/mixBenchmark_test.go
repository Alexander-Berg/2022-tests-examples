package fifo

import (
	"math/rand"
	"testing"
	"time"
)

func BenchmarkMixList(b *testing.B) {
	mix(b, NewListFifo())
}

func BenchmarkMixSimple(b *testing.B) {
	mix(b, NewSimpleFifo())
}

func BenchmarkMixFlexibleChannel(b *testing.B) {
	mix(b, NewFlexibleChannelFifo())
}

func BenchmarkMixChannel(b *testing.B) {
	mix(b, NewChannelFifo(b.N))
}

func BenchmarkMixReuse(b *testing.B) {
	mix(b, NewReuseFifo())
}

func BenchmarkMixChunk(b *testing.B) {
	mix(b, NewChunk())
}

func mix(b *testing.B, fifo queue) {

	random := rand.New(rand.NewSource(time.Now().UnixNano()))
	for i := 0; i < b.N/2; i++ {
		r := random.Intn(1000)
		fifo.Push(&r)
	}
	b.ResetTimer()
	for i := 0; i < b.N; i++ {
		b.StopTimer()
		push := random.Intn(2) == 1
		r := random.Intn(1000)
		b.StartTimer()
		if push {
			fifo.Push(&r)
		} else {
			fifo.Pop()
		}
	}
}
