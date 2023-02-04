package visa

import (
	"net/url"
	"testing"

	"github.com/stretchr/testify/require"
)

func BenchmarkMakeAuthHeaderForTimestamp(b *testing.B) {
	payload := []byte("{\"pay\": \"load\"}")
	url, err := url.Parse("http://domain.test/pa/th?key=value&k=v")
	require.NoError(b, err)

	b.ReportAllocs()
	b.ResetTimer()
	for i := 0; i < b.N; i++ {
		_ = MakeAuthHeaderForTimestamp(1337228322, url, payload, "secretsecretsecret")
	}
}
