package tvmtool

import (
	"io"
	"net/http"
	"net/http/httptest"
	"strings"
	"testing"

	"github.com/YandexClassifieds/go-common/tvm"
	"github.com/stretchr/testify/assert"
)

var (
	_ tvm.Client = &client{}
)

func TestTvmToolClient_GetServiceTicket(t *testing.T) {
	srv := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		assert.Equal(t, "test-token", r.Header.Get("Authorization"))
		w.WriteHeader(http.StatusOK)
		responseStr := `{"qqq":{"ticket":"test-ticket","tvm_id":2}}`
		_, err := io.Copy(w, strings.NewReader(responseStr))
		assert.NoError(t, err)
	}))
	cli := &client{
		httpClient: srv.Client(),
		conf:       NewConf(srv.URL, "test-token"),
	}
	ticket, err := cli.ServiceTicket(1, 2)
	if !assert.NoError(t, err) {
		return
	}
	assert.Equal(t, "test-ticket", ticket)
}
