package interactions

import (
	"testing"

	"github.com/stretchr/testify/assert"
)

var (
	valueInt int64 = 124
	valueStr       = "124"
	currency       = "RUB"
)

func TestParseAnalytics(t *testing.T) {
	t.Run("invoice", func(t *testing.T) {
		t.Parallel()
		attrs := map[string]*string{
			Invoice: &valueStr,
		}
		a, err := MakeAnalytics(attrs)
		assert.NoError(t, err)
		assert.Equal(t, valueInt, a.Invoice)
		assert.Zero(t, a.Service)
		assert.Zero(t, a.Client)
	})

	t.Run("service", func(t *testing.T) {
		t.Parallel()
		attrs := map[string]*string{
			Service:  &valueStr,
			Currency: &currency,
		}
		a, err := MakeAnalytics(attrs)
		assert.NoError(t, err)
		assert.Equal(t, valueInt, a.Service)
		assert.Equal(t, currency, a.Currency)
		assert.Zero(t, a.Invoice)
		assert.Zero(t, a.Contract)
	})

	t.Run("client", func(t *testing.T) {
		t.Parallel()
		attrs := map[string]*string{
			Contract: &valueStr,
			Client:   &valueStr,
		}
		a, err := MakeAnalytics(attrs)
		assert.NoError(t, err)
		assert.Equal(t, valueInt, a.Contract)
		assert.Equal(t, valueInt, a.Client)
		assert.Zero(t, a.Invoice)
	})
}

func TestParseAnalyticsFail(t *testing.T) {
	value := "sfds"
	attrs := map[string]*string{
		Invoice: &value,
	}
	a, err := MakeAnalytics(attrs)
	assert.Error(t, err)
	assert.Zero(t, a.Invoice)
	assert.Zero(t, a.Service)
	assert.Zero(t, a.Currency)
	assert.Zero(t, a.Client)
	assert.Zero(t, a.Contract)
}

func TestMakeAttrs(t *testing.T) {
	t.Run("service", func(t *testing.T) {
		t.Parallel()
		analytics := Analytics{
			Service:  valueInt,
			Invoice:  0,
			Contract: valueInt,
			Client:   valueInt,
			Currency: "",
		}
		attrs := analytics.MakeAttrs()
		if _, ok := attrs[Invoice]; ok {
			assert.Fail(t, "invoice_id found")
		}
		if _, ok := attrs[Currency]; ok {
			assert.Fail(t, "currency found")
		}
		assert.Equal(t, *attrs[Service], valueStr)
		assert.Equal(t, *attrs[Contract], valueStr)
		assert.Equal(t, *attrs[Client], valueStr)
	})

	t.Run("invoice", func(t *testing.T) {
		t.Parallel()
		analytics := Analytics{
			Invoice:  valueInt,
			Currency: currency,
		}
		attrs := analytics.MakeAttrs()
		if _, ok := attrs[Service]; ok {
			assert.Fail(t, "service_id found")
		}
		if _, ok := attrs[Contract]; ok {
			assert.Fail(t, "contract_id found")
		}
		assert.Equal(t, *attrs[Invoice], valueStr)
		assert.Equal(t, *attrs[Currency], currency)
	})
}

func TestMakeAttrsEmpty(t *testing.T) {
	analytics := Analytics{
		Service:  valueInt,
		Invoice:  0,
		Contract: valueInt,
		Client:   valueInt,
		Currency: currency,
	}

	t.Run("service", func(t *testing.T) {
		t.Parallel()
		attrs := analytics.MakeAttrsEmpty(false)
		if _, ok := attrs[Invoice]; ok {
			assert.Fail(t, "invoice_id found")
		}
		assert.Equal(t, *attrs[Service], valueStr)
		assert.Equal(t, *attrs[Contract], valueStr)
		assert.Equal(t, *attrs[Currency], currency)
		assert.Equal(t, *attrs[Client], valueStr)
	})

	t.Run("no invoice", func(t *testing.T) {
		t.Parallel()
		attrs := analytics.MakeAttrsEmpty(true)
		if value, ok := attrs[Invoice]; ok {
			assert.Nil(t, value)
		}
		if _, ok := attrs[Service]; ok {
			assert.Fail(t, "service found")
		}

		assert.Equal(t, *attrs[Contract], valueStr)
		assert.Equal(t, *attrs[Client], valueStr)
		assert.Equal(t, *attrs[Currency], currency)
		assert.Nil(t, attrs[OperationType])
	})

	t.Run("invoice", func(t *testing.T) {
		t.Parallel()
		analytics := Analytics{
			Invoice: valueInt,
		}

		attrs := analytics.MakeAttrsEmpty(true)
		assert.Equal(t, *attrs[Invoice], valueStr)
		assert.Nil(t, attrs[Currency])
		assert.Nil(t, attrs[OperationType])
	})
}
