package vault

import (
	"github.com/stretchr/testify/assert"

	"fmt"
	"net/http"
	"net/http/httptest"
	"testing"
)

func TestGetVersion(t *testing.T) {
	version := "123"
	token := "456"

	server := httptest.NewServer(http.HandlerFunc(func(rw http.ResponseWriter, req *http.Request) {
		assert.Equal(t, req.Header.Get("Authorization"), fmt.Sprintf("OAuth %s", token))
		assert.Equal(t, req.URL.Path, fmt.Sprintf("/1/versions/%s", version))

		_, _ = rw.Write([]byte(`{}`))
	}))

	defer server.Close()

	vault := Service{server.Client(), server.URL, token}

	_, err := vault.GetVersion(version)

	assert.NoError(t, err)
}
