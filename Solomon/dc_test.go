package hosts

import (
	"github.com/stretchr/testify/assert"
	"testing"
)

func TestParseDcs(t *testing.T) {
	tests := []struct {
		name string
		str  string
		dcs  []Dc
	}{
		{name: "EmptyString", str: "", dcs: []Dc{}},
		{name: "StringWithSpaces", str: "  ", dcs: []Dc{}},
		{name: "StringWithSpaces2", str: "\t\n", dcs: []Dc{}},
		{name: "SingleDc", str: "sas", dcs: []Dc{DcSas}},
		{name: "MultipleDcs", str: "sas,vla,man", dcs: []Dc{DcSas, DcVla, DcMan}},
		{name: "MultipleDcsWithSpaces", str: " sas, vla, man ", dcs: []Dc{DcSas, DcVla, DcMan}},
	}
	for _, test := range tests {
		t.Run(test.name, func(t *testing.T) {
			dcs, err := ParseDcs(test.str)
			assert.NoError(t, err)

			assert.Equal(t, len(test.dcs), len(dcs))
			for i, dc := range test.dcs {
				assert.Equal(t, dc, dcs[i])
			}
		})
	}
}
