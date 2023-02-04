package main

import (
	"testing"
)

var dnsData = []byte{
	204, 213, 1, 32, 0, 1, 0, 0, 0, 0, 0, 1, 14, 103, 97, 116, 101, 119, 97, 121, 45,
	115, 97, 115, 45, 48, 48, 3, 109, 111, 110, 6, 121, 97, 110, 100, 101, 120, 3, 110,
	101, 116, 0, 0, 28, 0, 1, 0, 0, 41, 16, 0, 0, 0, 0, 0, 0, 0}

func BenchmarkServeData(b *testing.B) {
	d, _ := NewDNSServer("", 1)

	b.ResetTimer()
	for ix := 0; ix < b.N; ix++ {
		_, _ = d.serveData(dnsData, true)
	}
	b.ReportAllocs()
}
