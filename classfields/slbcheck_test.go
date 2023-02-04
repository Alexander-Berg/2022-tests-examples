package slbcheck

import (
	"gopkg.in/h2non/gock.v1"
	"net/http/httptest"
	"testing"
)

func TestResponse200(t *testing.T) {
	req := httptest.NewRequest("GET", "http://localhost/200", nil)
	w := httptest.NewRecorder()
	Response200(w, req)

	if w.Code != 200 {
		t.Errorf("Unexpected status code from /200 - '%v'", w.Code)
	}
}

func TestExternalPing1(t *testing.T) {
	defer gock.Off()

	gock.New("http://localhost").
		Get("/200").
		Reply(200)

	req := httptest.NewRequest("GET", "http://localhost/ping-haproxy", nil)
	w := httptest.NewRecorder()
	ExternalPing(w, req)

	if w.Code != 200 {
		t.Errorf("Unexpected status code from /ping-haproxy - '%v'", w.Code)
	}
}

func TestExternalPing2(t *testing.T) {
	defer gock.Off()

	gock.New("http://localhost").
		Get("/badUrl").
		Reply(500)

	req := httptest.NewRequest("GET", "http://localhost/ping-haproxy", nil)
	w := httptest.NewRecorder()
	ExternalPing(w, req)

	if w.Code != 502 {
		t.Errorf("Unexpected status code from /ping-haproxy - '%v'", w.Code)
	}
}
