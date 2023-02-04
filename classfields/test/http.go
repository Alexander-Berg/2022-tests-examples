package test

import (
	"io"
	"io/ioutil"
	"net/http"
	"testing"
)

func DoHttpRequest(t *testing.T, method string, url string, body io.Reader) (responseString string, resp *http.Response) {
	t.Helper()

	req, err := http.NewRequest(method, url, body)
	if err != nil {
		t.Fatal("http error", err)
	}
	if body != nil {
		req.Header.Set("Content-Type", "application/json")
	}
	client := &http.Client{}
	resp, err = client.Do(req)
	if err != nil {
		t.Fatal("http error", err)
	}
	defer func() { _ = resp.Body.Close() }()

	bytes, err := ioutil.ReadAll(resp.Body)
	if err != nil {
		t.Fatal("can't read body", err)
	}
	responseString = string(bytes)

	return
}
