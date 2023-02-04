package fesmock

import (
	"context"
	"io"
	"log"
	"net/http"
)

type FESmock struct {
	server *http.Server
}

func (s *FESmock) Start() error {
	http.Handle("/", http.HandlerFunc(Print))
	s.server = &http.Server{Addr: ":8080", Handler: nil}
	return s.server.ListenAndServe()
}

func (s *FESmock) Stop(ctx context.Context) error {
	return s.server.Shutdown(ctx)
}

func Print(w http.ResponseWriter, req *http.Request) {
	log.Println(req.RequestURI)

	body, err := io.ReadAll(req.Body)
	if err != nil {
		http.Error(w, err.Error(), http.StatusBadRequest)
	}
	log.Println(string(body))
}
