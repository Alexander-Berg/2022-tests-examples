package ytreferences

import (
	"context"
	"testing"
	"time"

	"github.com/golang/mock/gomock"
	"github.com/stretchr/testify/require"
	"github.com/stretchr/testify/suite"

	"a.yandex-team.ru/billing/hot/processor/pkg/storage/ytreferences/mocks"
	"a.yandex-team.ru/billing/library/go/billingo/pkg/common/cast"
	btesting "a.yandex-team.ru/billing/library/go/billingo/pkg/testing"
	"a.yandex-team.ru/library/go/core/metrics/solomon"
)

func TestIsoCurrencyRate_GeRate(t *testing.T) {
	t.Parallel()

	testCases := []struct {
		name          string
		from          string
		to            string
		expectedRate  string
		expectedError string
	}{
		{
			"Both rates are > 0",
			"5",
			"3",
			"0.6",
			"",
		},
		{
			"TO rate is 0",
			"5",
			"0",
			"",
			"Invalid value of RateTo: 0",
		},
		{
			"FROM rate is 0",
			"0",
			"3",
			"",
			"Invalid value of RateFrom: 0",
		},
		{
			"TO rate < 0",
			"5",
			"-1",
			"",
			"Invalid value of RateTo: -1",
		},
		{
			"FROM rate < 0",
			"-1",
			"3",
			"",
			"Invalid value of RateFrom: -1",
		},
		{
			"TO rate is NaN",
			"5",
			"asdf",
			"",
			"can't convert to decimal",
		},
		{
			"FROM rate is NaN",
			"zxcv",
			"3",
			"",
			"can't convert to decimal",
		},
	}

	for i := range testCases {
		c := testCases[i]

		t.Run(c.name, func(t *testing.T) {
			t.Parallel()

			rate := IsoCurrencyRate{
				1,
				1,
				time.Time{},
				"test_src_cc",
				"USD",
				"USD",
				c.from,
				c.to,
				"",
			}
			fromToRate, err := rate.GetRate()
			if c.expectedError != "" {
				require.Error(t, err, c.expectedError)
				return
			}
			require.NoError(t, err)
			require.Equal(t, c.expectedRate, fromToRate)
		})
	}
}

func Test_computeNextDay(t *testing.T) {
	MSK := time.FixedZone("MSK", 3*3600)
	t.Parallel()

	testCases := []struct {
		name       string
		dt         time.Time
		expectedDt time.Time
	}{
		{
			"Start of the UTC day is converted to the start of the next UTC day",
			time.Date(2021, 10, 1, 0, 0, 0, 0, time.UTC),
			time.Date(2021, 10, 2, 0, 0, 0, 0, time.UTC),
		},
		{
			"Start of the MSK day is converted to the start of the next MSK day",
			time.Date(2021, 10, 1, 0, 0, 0, 0, MSK),
			time.Date(2021, 10, 2, 0, 0, 0, 0, MSK),
		},
		{
			"Any time of the UTC day is converted to the start of the next UTC day",
			time.Date(2021, 10, 1, 10, 10, 10, 10, time.UTC),
			time.Date(2021, 10, 2, 0, 0, 0, 0, time.UTC),
		},
		{
			"Any time of the MSK day is converted to the start of the next MSK day",
			time.Date(2021, 10, 1, 10, 10, 10, 10, MSK),
			time.Date(2021, 10, 2, 0, 0, 0, 0, MSK),
		},
	}

	for i := range testCases {
		c := testCases[i]

		t.Run(c.name, func(t *testing.T) {
			t.Parallel()

			ts := computeNextDay(c.dt)
			require.Equal(t, c.expectedDt, ts)
		})
	}
}

type testIsoCurrencyRateSuite struct {
	btesting.BaseSuite
	ctrl        *gomock.Controller
	ctx         context.Context
	client      *mocks.MockClient
	table       YtDynamicTable
	ref         IsoCurrencyRatesRef
	tableReader *mocks.MockTableReader
}

func (s *testIsoCurrencyRateSuite) setupSelectMock(rows []map[string]any, expectedQuery string) {
	if rows == nil {
		s.client.EXPECT().SelectRows(gomock.Any(), gomock.Any(), gomock.Any()).Times(0)
		return
	}
	if expectedQuery != "" {
		s.client.EXPECT().SelectRows(gomock.Any(), gomock.Eq(expectedQuery), gomock.Any()).Return(s.tableReader, nil)
	} else {
		s.client.EXPECT().SelectRows(gomock.Any(), gomock.Any(), gomock.Any()).Return(s.tableReader, nil)
	}
	numRows := len(rows)
	nextCount := numRows + 1
	s.tableReader.EXPECT().
		Next().
		Times(nextCount).
		DoAndReturn(
			func() bool {
				nextCount--
				return nextCount > 0
			})
	scanCount := numRows
	s.tableReader.EXPECT().
		Scan(gomock.Any()).
		Times(scanCount).
		DoAndReturn(
			func(row any) error {
				if r, ok := row.(*map[string]any); ok {
					*r = rows[numRows-scanCount]
					scanCount--
				} else {
					panic("wrong row type")
				}
				return nil
			})
	s.tableReader.EXPECT().Close()
	s.tableReader.EXPECT().Err().Return(nil)
}

func (s *testIsoCurrencyRateSuite) SetupTest() {
	s.ctx = context.Background()
	s.ctrl = gomock.NewController(s.T())
	s.client = mocks.NewMockClient(s.ctrl)
	s.tableReader = mocks.NewMockTableReader(s.ctrl)

	registry := solomon.NewRegistry(nil)
	s.table = YtDynamicTable{
		path: "//test/table",
		config: YtDynamicTableConfig{
			TablePath: "//test/table",
		},
		clients: []*Client{
			{
				cluster:  "hahn",
				client:   s.client,
				registry: registry,
			},
		},
	}

	s.ref = IsoCurrencyRatesRef{YtDynamicTable: s.table}
}

func TestIsoCurrencyRateSuite(t *testing.T) {
	suite.Run(t, &testIsoCurrencyRateSuite{})
}

func (s *testIsoCurrencyRateSuite) TestIsoCurrencyRateRef_Get() {
	testCases := []struct {
		name          string
		srcCc         string
		strDt         string
		currencyFrom  string
		currencyTo    string
		expectedQuery string
		expectedError string
		rows          []map[string]any
		expectedRate  IsoCurrencyRate
	}{
		{
			"Correct query is performed; a result is returned.",
			"test_src_cc",
			"2021-10-01",
			"USD",
			"RUB",
			" * FROM [//test/table] WHERE src_cc = 'test_src_cc' AND iso_currency_from = 'USD' AND iso_currency_to = 'RUB' AND dt <= 1633132800 ORDER BY dt DESC LIMIT 1",
			"",
			[]map[string]any{{
				"obj": map[string]any{
					"dt":                "2021-10-01T05:00:00Z",
					"id":                132535235345,
					"iso_currency_from": "USD",
					"iso_currency_to":   "RUB",
					"rate_from":         "1",
					"rate_to":           "10",
					"src_cc":            "test_src_cc",
					"version_id":        1,
				},
			}},
			IsoCurrencyRate{
				ID:              132535235345,
				SrcCc:           "test_src_cc",
				Dt:              time.Date(2021, 10, 1, 5, 0, 0, 0, time.UTC),
				IsoCurrencyFrom: "USD",
				IsoCurrencyTo:   "RUB",
				RateFrom:        "1",
				RateTo:          "10",
				rateFromTo:      "",
				Version:         1,
			},
		},
		{
			"Correct query is performed; no result is returned.",
			"test_src_cc",
			"2021-10-01",
			"USD",
			"RUB",
			" * FROM [//test/table] WHERE src_cc = 'test_src_cc' AND iso_currency_from = 'USD' AND iso_currency_to = 'RUB' AND dt <= 1633132800 ORDER BY dt DESC LIMIT 1",
			"No rate corresponding to the request is found",
			[]map[string]any{},
			IsoCurrencyRate{},
		},
	}

	for _, c := range testCases {
		s.Run(c.name, func() {
			s.setupSelectMock(c.rows, c.expectedQuery)
			dt, err := cast.ToTime(c.strDt)
			require.NoError(s.T(), err)
			rate, err := s.ref.Get(s.ctx, c.srcCc, c.currencyFrom, c.currencyTo, dt)
			if c.expectedError != "" {
				require.Error(s.T(), err, c.expectedError)
				return
			}
			require.NoError(s.T(), err)
			require.Equal(s.T(), c.expectedRate, rate)
		})
	}
}

func (s *testIsoCurrencyRateSuite) TestIsoCurrencyRateTableRef_GetRate() {
	testCases := []struct {
		name          string
		srcCc         string
		strDt         string
		currencyFrom  string
		currencyTo    string
		expectedError string
		rows          []map[string]any
		expectedRate  string
	}{
		{
			"Same TO and FROM currencies; rate is always 1, no select is made.",
			"test_src_cc",
			"2021-10-01",
			"USD",
			"USD",
			"",
			nil,
			"1",
		},
		{
			"Rate row is found with valid values; rate is computed.",
			"test_src_cc",
			"2021-10-01",
			"USD",
			"RUB",
			"",
			[]map[string]any{
				{
					"obj": map[string]any{
						"dt":                "2021-10-01T05:00:00Z",
						"id":                132535235345,
						"iso_currency_from": "USD",
						"iso_currency_to":   "RUB",
						"rate_from":         "1",
						"rate_to":           "10",
						"src_cc":            "test_src_cc",
						"version_id":        1,
					},
				},
			},
			"10",
		},
		{
			"Rate row is found with invalid FROM value; rate is NOT computed.",
			"test_src_cc",
			"2021-10-01T07:00:00+03:00",
			"USD",
			"RUB",
			"Invalid value of RateFrom",
			[]map[string]any{
				{
					"obj": map[string]any{
						"dt":                "2021-10-01T05:00:00Z",
						"id":                132535235345,
						"iso_currency_from": "USD",
						"iso_currency_to":   "RUB",
						"rate_from":         "-1",
						"rate_to":           "10",
						"src_cc":            "test_src_cc",
						"version_id":        1,
					},
				},
			},
			"",
		},
		{
			"Rate row is found with invalid TO value; rate is NOT computed.",
			"test_src_cc",
			"2021-10-01T07:00:00+03:00",
			"USD",
			"RUB",
			"Invalid value of RateTo",
			[]map[string]any{
				{
					"obj": map[string]any{
						"dt":                "2021-10-01T05:00:00Z",
						"id":                132535235345,
						"iso_currency_from": "USD",
						"iso_currency_to":   "RUB",
						"rate_from":         "1",
						"rate_to":           "-1",
						"src_cc":            "test_src_cc",
						"version_id":        1,
					},
				},
			},
			"",
		},
		{
			"Rate row is found with missing FROM value; rate is NOT computed.",
			"test_src_cc",
			"2021-10-01T07:00:00+03:00",
			"USD",
			"RUB",
			"Invalid value of RateFrom",
			[]map[string]any{
				{
					"obj": map[string]any{
						"dt":                "2021-10-01T05:00:00Z",
						"id":                132535235345,
						"iso_currency_from": "USD",
						"iso_currency_to":   "RUB",
						"rate_from":         "",
						"rate_to":           "10",
						"src_cc":            "test_src_cc",
						"version_id":        1,
					},
				},
			},
			"",
		},
		{
			"Rate row is found with missing TO value; rate is NOT computed.",
			"test_src_cc",
			"2021-10-01T07:00:00+03:00",
			"USD",
			"RUB",
			"Invalid value of RateTo",
			[]map[string]any{
				{
					"obj": map[string]any{
						"dt":                "2021-10-01T05:00:00Z",
						"id":                132535235345,
						"iso_currency_from": "USD",
						"iso_currency_to":   "RUB",
						"rate_from":         "1",
						"rate_to":           "",
						"src_cc":            "test_src_cc",
						"version_id":        1,
					},
				},
			},
			"",
		},
		{
			"No rate row is found; rate is NOT computed.",
			"test_src_cc",
			"2021-10-01T07:00:00+03:00",
			"USD",
			"RUB",
			"No rate corresponding to the request is found",
			[]map[string]any{},
			"",
		},
	}

	for _, c := range testCases {
		s.Run(c.name, func() {
			s.setupSelectMock(c.rows, "")
			dt, err := cast.ToTime(c.strDt)
			require.NoError(s.T(), err)
			rate, err := s.ref.GetRate(s.ctx, c.srcCc, c.currencyFrom, c.currencyTo, dt)
			if c.expectedError != "" {
				require.Error(s.T(), err, c.expectedError)
				return
			}
			require.NoError(s.T(), err)
			require.Equal(s.T(), c.expectedRate, rate)
		})
	}
}
