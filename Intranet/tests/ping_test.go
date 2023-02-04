package tests

import (
	"a.yandex-team.ru/intranet/idmtool/fetcher/handles"
	"io/ioutil"
	"net/http/httptest"
	"testing"

	"github.com/stretchr/testify/assert"
)

func TestPing(t *testing.T) {
	w := httptest.NewRecorder()
	handles.Ping(w, httptest.NewRequest("GET", "http://example.com/ping", nil))

	resp := w.Result()
	body, _ := ioutil.ReadAll(resp.Body)

	assert.Equal(t, 200, resp.StatusCode, "GET /ping status code")
	assert.Equal(t, "pong\n", string(body), "GET /ping body")
}
