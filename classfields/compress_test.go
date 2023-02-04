package goLB

import (
	"bytes"
	"compress/flate"
	"testing"

	zstdc "github.com/DataDog/zstd"
	"github.com/stretchr/testify/require"
)

var (
	testJson = `{"message":"eyJfdGltZSI6IjIwMTktMTAtMjhUMjE6Mzk6NDAuMTA4KzAzOjAwIiwiQHZlcnNpb24iOiIxIiwiX21lc3NhZ2UiOiJNaW4gc3RhZ2UgZGVsYXkgZm9yIFByb2Nlc3Npbmc6IFN0YWdlIG5hbWU6IFJldmlzaXRTdGFnZS4gRGVsYXk6IER1cmF0aW9uLkluZiIsIl9jb250ZXh0IjoicnUueWFuZGV4LnZvczIuYXV0b3J1LndhdGNoaW5nLkF1dG9ydVdhdGNoaW5nRW5naW5lIiwiX3RocmVhZCI6IkZvcmtKb2luUG9vbC0yLXdvcmtlci0xMyIsIl9sZXZlbCI6IklORk8iLCJsZXZlbF92YWx1ZSI6MjAwMDAsIm9mZmVyX2lkIjoiMTA4NzA3ODI2Ny05M2Y4N2IxYSIsIl9zZXJ2aWNlIjoieWFuZGV4LXZvczItYXV0b3J1LXNjaGVkdWxlci1kZXYiLCJfY29udGFpbmVyX2lkIjoiMTBmZmI1MTg1MmJlMjlkYzQ2ZDI5OWVhZDk5NDJjNGEzNTY0YmMxZjk4ZGZkOWM1NWEwN2ZkNWNkODI4NTQ2MiIsIl9hbGxvY2F0aW9uX2lkIjoiYWEzMTllNzYtYjA0MC02NTI5LTc2NDUtMGFhZDQ2ZmY0NjJlIiwiX2ltYWdlX2lkIjoic2hhMjU2OjBhOTg4N2IwMDMzZTUxYjhmZjVhNDFiZjA1MGY2YTNmZGJiNGVlMzc3OTk4YzNjNGJhZjQ4NjVhZjFlZjQ4MzgiLCJfaW1hZ2VfbmFtZSI6InJlZ2lzdHJ5LnZlcnRpcy55YW5kZXgubmV0L3lhbmRleC12b3MyLWF1dG9ydS1zY2hlZHVsZXI6MC4xOTEuMCIsIl9jb250YWluZXJfbmFtZSI6Ii92b3MyLWF1dG9ydS1zY2hlZHVsZXItYWEzMTllNzYtYjA0MC02NTI5LTc2NDUtMGFhZDQ2ZmY0NjJlIiwiX2RjIjoic2FzIiwiX2xheWVyIjoidGVzdCIsIl91dWlkIjoiNjVkZTE5ZGYtZDRhYy00MTMzLTk1YWItYzUzODlkNjM5YTZhIiwiX3RpbWVzdGFtcCI6IjIwMTktMTAtMjhUMjE6Mzk6NDAiLCJfdGltZXpvbmUiOiIrMDM6MDAiLCJfdGltZV9uYW5vIjoiMTA4MDAwMDAwIn0=","service_name":"yandex-vos2-autoru-scheduler-dev","allocation_id":"aa319e76-b040-6529-7645-0aad46ff462e","seq_no":0,"partition":0}`
)

func TestCompressDecompress(t *testing.T) {
	gzipped, err := CompressGzip([]byte(testJson), flate.BestSpeed)
	require.NoError(t, err)
	require.NotEqual(t, testJson, gzipped)

	data, err := DecompressGzip(gzipped)
	require.NoError(t, err)
	require.Equal(t, testJson, string(data))
}

func TestCompressParallel(t *testing.T) {
	data := []byte(testJson)
	for i := 0; i < 10000; i++ {
		go func() {
			gzipped, err := CompressGzip(data, flate.BestSpeed)
			require.NoError(t, err)
			decompress, err := DecompressGzip(gzipped)
			require.NoError(t, err)
			require.Equal(t, testJson, string(decompress))
		}()
	}
}

func BenchmarkCompress_Gzip(b *testing.B) {
	input := []byte(testJson)
	for i := 0; i < b.N; i++ {
		result, err := CompressGzip(input, flate.BestSpeed)
		if err != nil {
			b.Fatalf("compress fail: %v", err)
		}
		_ = result
	}
}

//func BenchmarkCompress_Zstd_Native(b *testing.B) {
//	input := []byte(testJson)
//	w, _ := zstdn.NewWriter(nil, zstdn.WithEncoderLevel(zstdn.SpeedFastest))
//	for i := 0; i < b.N; i++ {
//		result := w.EncodeAll(input, nil)
//		if len(result) == 0 {
//			b.Fatal("empty data")
//		}
//		_ = result
//	}
//}

func BenchmarkCompress_Zstd_Cgo(b *testing.B) {
	input := []byte(testJson)
	for i := 0; i < b.N; i++ {
		result, err := zstdc.CompressLevel(nil, input, zstdc.BestSpeed)
		if err != nil {
			b.Fatalf("compress fail: %v", err)
		}
		if len(result) == 0 {
			b.Fatal("empty data")
		}
		_ = result
	}
}

func BenchmarkDecompress_Gzip(b *testing.B) {
	unzipped := []byte(testJson)
	gzipped, err := CompressGzip(unzipped, flate.BestSpeed)
	require.NoError(b, err)
	require.NotEqual(b, unzipped, gzipped)

	for i := 0; i < b.N; i++ {
		decompress, err := DecompressGzip(gzipped)
		if err != nil {
			b.Fatalf("decompress failed: %v", err)
		}
		if bytes.Compare(unzipped, decompress) != 0 {
			b.Fatal("decompress mismatch")
		}
	}
}

//func BenchmarkDecompress_Zstd_Native(b *testing.B) {
//	enc, _ := zstdn.NewWriter(nil, zstdn.WithEncoderLevel(zstdn.SpeedFastest))
//	unzipped := []byte(testJson)
//	zipped := enc.EncodeAll(unzipped, nil)
//
//	dec, _ := zstdn.NewReader(nil)
//	for i := 0; i < b.N; i++ {
//		data, err := dec.DecodeAll(zipped, nil)
//		if err != nil {
//			b.Fatalf("decode failed: %v", err)
//		}
//		if bytes.Compare(unzipped, data) != 0 {
//			b.Fatal("decompress mismatch")
//		}
//	}
//}

func BenchmarkDecompress_Zstd_Cgo(b *testing.B) {
	unzipped := []byte(testJson)
	zipped, _ := zstdc.CompressLevel(nil, unzipped, zstdc.BestSpeed)
	for i := 0; i < b.N; i++ {
		data, err := zstdc.Decompress(nil, zipped)
		if err != nil {
			b.Fatalf("decode failed: %v", err)
		}
		if bytes.Compare(unzipped, data) != 0 {
			b.Fatal("decompress mismatch")
		}
	}
}
