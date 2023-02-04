package uhttp

import (
	"context"
	"github.com/stretchr/testify/assert"
	"go.uber.org/atomic"
	"net/http"
	"net/http/httptest"
	"testing"
	"time"
)

func TestOk(t *testing.T) {
	server := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		assert.Equal(t, "/test-get", r.URL.String())
		_, err := w.Write([]byte("GOOD JOB!"))
		assert.NoError(t, err)
	}))
	defer server.Close()

	client := NewClient(server.URL, "", nil, 2*time.Second, false)
	req, err := client.NewGetRequest(context.Background(), "/test-get")
	assert.NoError(t, err)

	resp, err := client.SendRequest(req)
	assert.NoError(t, err)
	assert.Equal(t, "GOOD JOB!", string(resp))
}

func TestHasAuthHeader(t *testing.T) {
	mux := http.NewServeMux()
	mux.Handle("/with-iam", http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		assert.Equal(t, "Bearer iam-token", r.Header.Get("Authorization"))
	}))
	mux.Handle("/with-oauth", http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		assert.Equal(t, "OAuth oauth-token", r.Header.Get("Authorization"))
	}))

	server := httptest.NewServer(mux)
	defer server.Close()

	{
		client := NewClient(server.URL, "", IamToken("iam-token"), 2*time.Second, false)
		req, err := client.NewGetRequest(context.Background(), "/with-iam")
		assert.NoError(t, err)

		resp, err := client.SendRequest(req)
		assert.NoError(t, err)
		assert.Equal(t, "", string(resp))
	}
	{
		client := NewClient(server.URL, "", OAuthToken("oauth-token"), 2*time.Second, false)
		req, err := client.NewGetRequest(context.Background(), "/with-oauth")
		assert.NoError(t, err)

		resp, err := client.SendRequest(req)
		assert.NoError(t, err)
		assert.Equal(t, "", string(resp))
	}
}

func TestRetry429(t *testing.T) {
	var counter atomic.Int32
	server := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		assert.Equal(t, "/test", r.URL.String())

		if counter.Add(1) < 3 {
			w.WriteHeader(http.StatusTooManyRequests)
			_, err := w.Write([]byte("come back later"))
			assert.NoError(t, err)
		} else {
			_, err := w.Write([]byte("GOOD JOB!"))
			assert.NoError(t, err)
		}
	}))
	defer server.Close()

	client := NewClient(server.URL, "", nil, 2*time.Second, false)
	req, err := client.NewGetRequest(context.Background(), "/test")
	assert.NoError(t, err)

	resp, err := client.SendRequest(req)
	assert.NoError(t, err)
	assert.Equal(t, "GOOD JOB!", string(resp))
}

func TestRetry5xx(t *testing.T) {
	var counter atomic.Int32
	server := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		assert.Equal(t, "/test", r.URL.String())

		if counter.Add(1) < 3 {
			w.WriteHeader(http.StatusServiceUnavailable)
			_, err := w.Write([]byte("server unavailable"))
			assert.NoError(t, err)
		} else {
			_, err := w.Write([]byte("GOOD JOB!"))
			assert.NoError(t, err)
		}
	}))
	defer server.Close()

	client := NewClient(server.URL, "", nil, 2*time.Second, false)
	req, err := client.NewGetRequest(context.Background(), "/test")
	assert.NoError(t, err)

	resp, err := client.SendRequest(req)
	assert.NoError(t, err)
	assert.Equal(t, "GOOD JOB!", string(resp))
}

func TestJsonResponse(t *testing.T) {
	mux := http.NewServeMux()
	mux.Handle("/user", http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		assert.Equal(t, "application/json", r.Header.Get("Accept"))
		_, err := w.Write([]byte(`{"login": "jamel", "uid": 1120000000012330}`))
		assert.NoError(t, err)
	}))
	mux.Handle("/project", http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		assert.Equal(t, "application/json", r.Header.Get("Accept"))
		_, err := w.Write([]byte(`{"id": "solomon", "title": "The Best Monitoring System"}`))
		assert.NoError(t, err)
	}))

	server := httptest.NewServer(mux)
	defer server.Close()

	client := NewClient(server.URL, "", nil, 2*time.Second, false)

	{
		req, err := client.NewGetRequest(context.Background(), "/user")
		assert.NoError(t, err)

		var user struct {
			Login string `json:"login"`
			UID   int    `json:"uid"`
		}

		err = client.SendJSONRequest(req, &user)
		assert.NoError(t, err)
		assert.Equal(t, "jamel", user.Login)
		assert.Equal(t, 1120000000012330, user.UID)
	}

	{
		req, err := client.NewGetRequest(context.Background(), "/project")
		assert.NoError(t, err)

		var project struct {
			ID    string `json:"id"`
			Title string `json:"title"`
		}

		err = client.SendJSONRequest(req, &project)
		assert.NoError(t, err)
		assert.Equal(t, "solomon", project.ID)
		assert.Equal(t, "The Best Monitoring System", project.Title)
	}
}
