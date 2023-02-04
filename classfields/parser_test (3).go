package clickHouse

import (
	"database/sql/driver"
	"github.com/YandexClassifieds/logs/cmd/golf/domain"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
	"testing"
	"time"
)

func TestParseValue(t *testing.T) {
	check(t, wrapVal(Value(domain.Text, false, "Text")))
	check(t, wrapVal(Value(domain.Boolean, false, false)))
	check(t, wrapVal(Value(domain.Boolean, false, true)))
	check(t, wrapVal(Value(domain.Date, false, time.Now())))
	check(t, wrapVal(Value(domain.Date, false, "2018-10-25T16:34:00.000+05:00")), "old format failed")
	check(t, wrapVal(Value(domain.Date, false, "2018-10-25T16:34:00.000+00:00")), "zero-offset failed")
	check(t, wrapVal(Value(domain.Date, false, "2018-10-25T16:34:00.432Z")), "ms failed")
	check(t, wrapVal(Value(domain.Date, false, "2018-10-25T16:34:00.123456789Z")), "ns failed")
	check(t, wrapVal(Value(domain.Level, false, "INFO")))
	check(t, wrapVal(Value(domain.Text, false, "tuz tuz tuz {} : /b")))
	check(t, wrapVal(Value(domain.Float, false, float64(34))))
	check(t, wrapVal(Value(domain.Int, false, int(45))))
	check(t, wrapVal(Value(domain.Int, false, int64(67))))

	for _, typ := range []domain.Type{domain.Text, domain.Boolean, domain.Date, domain.Level, domain.Int, domain.Float} {
		domainVal, err := Value(typ, true, nil)
		if !assert.NoError(t, err) {
			continue
		}
		value, err := domainVal.(driver.Valuer).Value()
		require.NoError(t, err, "type %v parse failed", typ)
		assert.Nil(t, value, "type %v nil check failed", typ)
	}
}

func check(t *testing.T, v wrappedValue, msgAndArgs ...interface{}) {
	t.Helper()
	if !assert.NoError(t, v.Err) {
		return
	}
	value, err := v.Value.(driver.Valuer).Value()
	assert.NotNil(t, value, msgAndArgs...)
	assert.NoError(t, err, msgAndArgs...)
}

func TestNullValue(t *testing.T) {
	checkNull(t, domain.Text)
	checkNull(t, domain.Boolean)
	checkNull(t, domain.Date)
	checkNull(t, domain.Level)
	checkNull(t, domain.Float)
	checkNull(t, domain.Int)

	v, err := Null(domain.NewKey("", domain.Text).NotNullable()).(driver.Valuer).Value()
	assert.NoError(t, err)
	assert.NotNil(t, v)
}

func checkNull(t *testing.T, tt domain.Type) {
	k := domain.NewKey("", tt)
	value, err := Null(k).(driver.Valuer).Value()
	assert.Nil(t, value)
	assert.NoError(t, err)
}

func wrapVal(vv ...interface{}) wrappedValue {
	err, _ := vv[1].(error)
	return wrappedValue{
		Value: vv[0],
		Err:   err,
	}
}

type wrappedValue struct {
	Value interface{}
	Err   error
}
