package server

import (
	"io"
	"net/http"
	"net/http/httptest"
	"strings"
	"testing"

	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
)

func TestTvmAuthSkip(t *testing.T) {
	testCases := []struct {
		name   string
		url    string
		method string
		body   string

		skipped bool
	}{
		{
			name: "common handler",
			url:  "/ping",

			skipped: true,
		},
		{
			name: "export get",
			url:  "/v1/export",

			skipped: true,
		},
		{
			name:   "export post no env",
			url:    "/v1/export",
			method: http.MethodPost,
			body:   `{"lala":"lolo"}`,

			skipped: true,
		},
		{
			name:   "export post test env",
			url:    "/v1/export",
			method: http.MethodPost,
			body:   `{"environment":"test"}`,

			skipped: true,
		},
		{
			name:   "export run test env",
			url:    "/v1/export",
			method: http.MethodPost,
			body:   `{"environment":"test"}`,

			skipped: true,
		},
		{
			name:   "export post prod env",
			url:    "/v1/export",
			method: http.MethodPost,
			body:   `{"environment":"prod"}`,

			skipped: false,
		},
		{
			name:   "export run post prod env",
			url:    "/v1/export/run",
			method: http.MethodPost,
			body:   `{"environment":"prod"}`,

			skipped: false,
		},
	}

	for _, tc := range testCases {
		tc := tc
		t.Run(tc.name, func(t *testing.T) {
			var reader io.Reader = strings.NewReader(tc.body)
			if tc.body == "" {
				reader = nil
			}

			r := httptest.NewRequest(tc.method, tc.url, reader)

			skipped, err := tvmAuthSkip(r)
			require.NoError(t, err)

			assert.Equal(t, tc.skipped, skipped)
		})
	}
}
