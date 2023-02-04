package workerpool

import (
	"testing"
	//"time"
)

type TData struct {
	Int int
}

func TestWorkerPool(t *testing.T) {
	debug := false
	n := 10
	k := 100

	worker := func(input interface{}) {
		p := input.(*TData)
		(*p).Int += 1
		//time.Sleep(10*time.Millisecond)
	}
	task := make([]interface{}, k)
	for i := 0; i < k; i++ {
		task[i] = &TData{i}
	}
	f := NewWorkerPool("test", n, worker, debug)
	f.Do(task)
	for i := 0; i < k; i++ {
		if task[i].(*TData).Int != i+1 {
			t.Errorf("WorkerPool bad value = %d, want = %d ", task[i].(*TData).Int, i+1)
		}
	}
	f.Stop(true)
}

func BenchmarkWorkerPool(b *testing.B) {
	debug := false
	n := 10
	k := 100

	worker := func(input interface{}) {
		p := input.(*TData)
		(*p).Int += 1
	}
	task := make([]interface{}, k)
	for i := 0; i < k; i++ {
		task[i] = &TData{i}
	}
	f := NewWorkerPool("test", n, worker, debug)
	b.ResetTimer()
	for i := 0; i < b.N; i++ {
		f.Do(task)
	}
	b.ReportMetric(float64(k), "size")
	b.ReportAllocs()
	f.Stop(true)
}
