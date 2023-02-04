package test

import (
	"fmt"
	"github.com/stretchr/testify/assert"
	"net/http"
	"net/http/httptest"
	"net/url"
	"strings"
	"testing"
)

func AssertHTTPPostBodyContains(t *testing.T, handler http.HandlerFunc, url string, postForm url.Values, str string) bool {
	t.Helper()

	w := httptest.NewRecorder()
	req, err := http.NewRequest("POST", url, nil)
	if err != nil {
		return false
	}
	req.PostForm = postForm
	handler(w, req)
	body := w.Body.String()

	contains := strings.Contains(body, str)
	if !contains {
		assert.Fail(t, fmt.Sprintf("Expected response body for \"%s\" to contain \"%s\" but found \"%s\"", url, str, body))
	}

	return contains
}

func RequireHTTPPostBodyContains(t *testing.T, handler http.HandlerFunc, url string, postForm url.Values, str string) {
	t.Helper()

	if AssertHTTPPostBodyContains(t, handler, url, postForm, str) {
		return
	}

	t.FailNow()
}
