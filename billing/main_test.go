package main

import (
	"encoding/json"
	"fmt"
	"io/ioutil"
	"net/http"
	"net/http/httptest"
	"testing"
	"time"

	"github.com/go-resty/resty/v2"
	"github.com/stretchr/testify/assert"
)

func TestAlert(t *testing.T) {
	date := time.Date(2021, 1, 1, 2, 3, 15, 0, time.Local)
	queue := "YAPAYPCIDSS"
	summary := "Нарушение CSP на форме Yandex.Pay"
	event := Event{
		Count:        1,
		Date:         date,
		MaxTimestamp: int(date.Unix()),
		BlockedURI: "ya.ru",
		ViolatedDirective: "script",
		Platform: "touch",
		RequestID: "123456",
	}
	server := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		body, err := ioutil.ReadAll(r.Body)
		if err != nil {
			t.Fail()
		}
		trackerRequest := &TrackerRequest{}
		if err = json.Unmarshal(body, trackerRequest); err != nil {
			t.Fail()
		}
		assert.Equal(t, queue, trackerRequest.Queue)
		assert.Equal(t, summary, trackerRequest.Summary)
		assert.Equal(t, AsSha256(trackerRequest.Description), trackerRequest.Unique)
		assert.Contains(t, trackerRequest.Description, fmt.Sprintf("Время: %s", date.Format("2006-01-02 15:04:05 -0700 MST")))
		assert.Contains(t,trackerRequest.Description, fmt.Sprintf("Директива: %s", event.ViolatedDirective))
	}))
	defer server.Close()
	trackerClient := resty.New().SetHostURL(server.URL)
	err := SendAlert(trackerClient, queue, summary, event)
	if err != nil {
		t.Error(err)
	}
}

func TestYQLResultsRetry(t *testing.T) {
	retryCount := 0
	server := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		retryCount++
		w.Header().Set("Content-Type", "application/json")
		body, err := json.Marshal(YQLResponse{
			Status: "IDLE",
		})
		if err != nil {
			t.Fatal(err)
		}
		_, err = w.Write(body)
		if err != nil {
			t.Fatal(err)
		}
	}))
	defer server.Close()
	yqlClient := resty.New().SetHostURL(server.URL)
	_, err := YQLResults(
		yqlClient,
		"123",
		RetryOptions{
			Delay: 0,
			Count: 10,
		},
	)
	if err == nil {
		t.Error("expected error")
	}
	if retryCount - 1 != 10 {
		t.Error("max retry not reached")
	}
}
