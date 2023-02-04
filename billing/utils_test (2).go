package handlers

import (
	"net/http"
	"net/http/httptest"

	"github.com/stretchr/testify/require"
)

const (
	PingDBURL = "/pingdb"
	DryRunURL = "/config/dry-run"
)

func (s *HandlersV1TestSuite) TestPingDB() {
	req := httptest.NewRequest(http.MethodGet, PingDBURL, nil)
	w := httptest.NewRecorder()

	if err := GetPingDB(s.ctx, w, req); err != nil {
		require.Fail(s.T(), err.Error())
	}

	require.Equal(s.T(), http.StatusOK, w.Code)
	require.Equal(s.T(), "pong", w.Body.String())
}

func (s *HandlersV1TestSuite) TestGetConfigDryRunTrue() {
	req := httptest.NewRequest(http.MethodGet, DryRunURL, nil)
	w := httptest.NewRecorder()

	s.ctx.Config.DryRun = true

	if err := GetConfigDryRun(s.ctx, w, req); err != nil {
		require.Fail(s.T(), err.Error())
	}

	require.Equal(s.T(), http.StatusOK, w.Code)
	require.Equal(s.T(), "true", w.Body.String())
}

func (s *HandlersV1TestSuite) TestGetConfigDryRunDefault() {
	req := httptest.NewRequest(http.MethodGet, DryRunURL, nil)
	w := httptest.NewRecorder()

	if err := GetConfigDryRun(s.ctx, w, req); err != nil {
		require.Fail(s.T(), err.Error())
	}

	require.Equal(s.T(), http.StatusOK, w.Code)
	require.Equal(s.T(), "false", w.Body.String())
}
