package handlers

import (
	"net/http"
	"net/http/httptest"

	"github.com/stretchr/testify/require"
)

const (
	PayoutMetricURL  = "/solomon/payouts"
	RequestMetricURL = "/solomon/requests"
	CpfMetricURL     = "/solomon/cpf"
)

func (s *HandlersV1TestSuite) TestRequestMetrics() {
	req := httptest.NewRequest(http.MethodGet, RequestMetricURL, nil)
	w := httptest.NewRecorder()

	err := GetRequestMetrics(s.ctx, w, req)
	require.NoError(s.T(), err)

	require.Equal(s.T(), http.StatusOK, w.Code)
}

func (s *HandlersV1TestSuite) TestPayoutMetrics() {
	req := httptest.NewRequest(http.MethodGet, PayoutMetricURL, nil)
	w := httptest.NewRecorder()

	err := GetPayoutMetrics(s.ctx, w, req)
	require.NoError(s.T(), err)

	require.Equal(s.T(), http.StatusOK, w.Code)
}

func (s *HandlersV1TestSuite) TestCpfMetrics() {
	req := httptest.NewRequest(http.MethodGet, CpfMetricURL, nil)
	w := httptest.NewRecorder()

	err := GetCpfMetrics(s.ctx, w, req)
	require.NoError(s.T(), err)

	require.Equal(s.T(), http.StatusOK, w.Code)
}
