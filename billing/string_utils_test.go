package common

import (
	"testing"

	"github.com/stretchr/testify/assert"
)

func TestUnderscore(t *testing.T) {
	assert.Equal(t, "i_love_golang_and_json_so_much", Underscore("ILoveGolangAndJSONSoMuch"))
	assert.Equal(t, "i_love_json", Underscore("ILoveJSON"))
	assert.Equal(t, "json", Underscore("json"))
	assert.Equal(t, "json", Underscore("JSON"))
	assert.Equal(t, "привет_мир", Underscore("ПриветМир"))
}

// BenchmarkUnderscore-4           10000000               209 ns/op
func BenchmarkUnderscore(b *testing.B) {
	for n := 0; n < b.N; n++ {
		Underscore("TestTable")
	}
}
