package server

import (
	"context"
	"errors"
	"net/http"
	"net/http/httptest"
	"strconv"
	"testing"

	"a.yandex-team.ru/library/go/yandex/tvm"

	"github.com/go-chi/chi/v5"
	"github.com/go-resty/resty/v2"
)

func TestMiddlewareSharedKeyAuth(t *testing.T) {
	mux := chi.NewRouter()
	mux.Use(MiddlewareSharedKeyAuth("123"))
	mux.Get("/", func(w http.ResponseWriter, r *http.Request) {})

	server := httptest.NewTLSServer(mux)
	defer server.Close()

	sendRequest := func(t *testing.T, auth string, succ bool) {
		client := resty.NewWithClient(server.Client())
		req := client.R()

		if len(auth) > 0 {
			req.Header.Set("Authorization", auth)
		}

		resp, err := req.Get(server.URL)
		if err != nil {
			t.Fatal(err)
		}
		gotSuccess := resp.IsSuccess()
		if gotSuccess != succ {
			t.Errorf("Unexpected status: %s", resp.Status())
		}
	}

	for _, auth := range []string{
		"sharedkey 123",
		"SharedKey 123",
		"SharedKeySHA256 a665a45920422f9d417e4867efdc4fb8a04a1f3fff1fa07e998e86f7f7a27ae3",
		"shAreDkeySha256 a665a45920422f9d417e4867efdc4fb8a04a1f3fff1fa07e998e86f7f7a27ae3",
	} {
		t.Run("", func(t *testing.T) {
			sendRequest(t, auth, true)
		})
	}

	for _, auth := range []string{
		"",
		"SharedKey",
		"SharedKey ",
		"SharedKey 12",
		"SharedKey 1234",
		"SharedKeySHA256",
		"SharedKeySHA256 ",
		"SharedKeySHA256 123",
		"SharedKeySHA256 a665a45920422f9d417e4867efdc4fb8a04a1f3fff1fa07e998e86f7f7a27ae4",
	} {
		t.Run("", func(t *testing.T) {
			sendRequest(t, auth, false)
		})
	}
}

func TestMiddlewareTVMAuth(t *testing.T) {
	mux := chi.NewRouter()

	tvmClient := NewMockedTVMClient()
	tvmClient.CheckServiceTicketFunc = func(ctx context.Context, ticket string) (*tvm.CheckedServiceTicket, error) {
		if ticket == "" {
			return nil, errors.New("mock: empty ticket")
		}

		id, err := strconv.Atoi(ticket)
		if err != nil {
			return nil, err
		}

		return &tvm.CheckedServiceTicket{SrcID: tvm.ClientID(id)}, nil
	}

	mux.Use(MiddlewareTVMAuth(mustCreateTestLogger(), tvmClient, []tvm.ClientID{123}))
	mux.Get("/", func(w http.ResponseWriter, r *http.Request) {})

	server := httptest.NewTLSServer(mux)
	defer server.Close()

	sendRequest := func(t *testing.T, ticket string, succ bool) {
		client := resty.NewWithClient(server.Client())
		req := client.R()

		if len(ticket) > 0 {
			req.Header.Set("X-Ya-Service-Ticket", ticket)
		}

		resp, err := req.Get(server.URL)
		if err != nil {
			t.Fatal(err)
		}
		gotSuccess := resp.IsSuccess()
		if gotSuccess != succ {
			t.Errorf("Unexpected status: %s", resp.Status())
		}
	}

	for _, auth := range []string{
		"123",
	} {
		t.Run("", func(t *testing.T) {
			sendRequest(t, auth, true)
		})
	}

	for _, auth := range []string{
		"",
		"12",
		"XXX",
	} {
		t.Run("", func(t *testing.T) {
			sendRequest(t, auth, false)
		})
	}
}
