package nda

import (
	"bytes"
	"fmt"
	"io"
	"net/http"
	"testing"

	lru "github.com/hashicorp/golang-lru"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
)

func TestService_NdaUrl(t *testing.T) {
	cache, _ := lru.New(cacheSize)
	svc := &service{
		cache: cache,
	}

	ctr := 0
	svc.http.Transport = wrapTransport(func(request *http.Request) (*http.Response, error) {
		ctr++
		resp := &http.Response{
			StatusCode: 200,
			Body:       io.NopCloser(bytes.NewReader([]byte(fmt.Sprintf("res_%d", ctr)))),
		}
		return resp, nil
	})

	res, err := svc.NdaUrl("wtf")
	require.NoError(t, err)
	assert.Equal(t, "res_1", res)

	// check cache
	res2, err := svc.NdaUrl("wtf")
	require.NoError(t, err)
	assert.Equal(t, "res_1", res2)
}

type transportFunc func(request *http.Request) (*http.Response, error)

func wrapTransport(f transportFunc) http.RoundTripper {
	return &mockTransport{call: f}
}

type mockTransport struct {
	call transportFunc
}

func (m *mockTransport) RoundTrip(request *http.Request) (*http.Response, error) {
	return m.call(request)
}
