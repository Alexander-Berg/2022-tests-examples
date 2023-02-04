package metrics

import (
	"encoding/json"
	"fmt"
	"testing"
)

func MakeMetrics() *Metrics {
	m := NewMetrics()

	m.AddDGauge(nil, "uptime", 10000)
	for i := 0; i < 40; i++ {
		labels := map[string]string{
			"env":          fmt.Sprintf("env-%d", i),
			"service_name": fmt.Sprintf("service-%d", i),
		}
		m.AddIGauge(labels, "hosts.count", uint64(i))
		m.AddDGauge(labels, "data.stale_seconds", 8000)
		m.AddRate(labels, "date.updates_total", 300)
		m.AddRate(labels, "date.updates_forced", 3)
		m.AddRate(labels, "data.updates_failed", 4)
	}

	return m
}

func TestMarshalMetrics(t *testing.T) {
	var data Metrics

	m := MakeMetrics()
	x := m.Bytes()

	if err := json.Unmarshal(x, &data); err != nil {
		fmt.Println(string(x))
		t.Error(err)
	}
}

func TestSumMetrics(t *testing.T) {
	var data Metrics

	m1 := MakeMetrics()
	m2 := MakeMetrics()

	ms := MetricsSum(m1, m2)
	if err := json.Unmarshal(ms.Bytes(), &data); err != nil {
		t.Error(err)
	}
}

func BenchmarkMarshalMetrics(b *testing.B) {
	var x []byte
	m := MakeMetrics()

	x = m.Bytes()

	b.ResetTimer()
	for i := 0; i < b.N; i++ {
		_ = m.Bytes()
	}
	b.ReportMetric(float64(len(x)), "size")
	b.ReportAllocs()

	//fmt.Println(string(x))
}

func BenchmarkZMarshalMetrics(b *testing.B) {
	var z []byte
	m := MakeMetrics()

	z, _ = m.ZBytes()

	b.ResetTimer()
	for i := 0; i < b.N; i++ {
		_, _ = m.ZBytes()
	}
	b.ReportMetric(float64(len(z)), "size")
	b.ReportAllocs()
}

func BenchmarkSumMetrics(b *testing.B) {
	m1 := MakeMetrics()
	m2 := MakeMetrics()

	b.ResetTimer()
	for i := 0; i < b.N; i++ {
		_ = MetricsSum(m1, m2)
	}
	b.ReportAllocs()
}

func BenchmarkMakeMetrics(b *testing.B) {
	b.ResetTimer()
	for i := 0; i < b.N; i++ {
		_ = MakeMetrics()
	}
	b.ReportAllocs()
}
