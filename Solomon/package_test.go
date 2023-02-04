package apt

import (
	"encoding/json"
	"testing"

	"github.com/stretchr/testify/assert"
)

func TestPackageParseAndToString(t *testing.T) {
	tests := []struct {
		name string
		pkg  string
	}{
		{"NoVersion", "yandex-solomon-fetcher"},
		{"WithVersion", "yandex-solomon-gateway=12345"},
	}
	for _, test := range tests {
		t.Run(test.name, func(t *testing.T) {
			p, err := ParsePackage(test.pkg)
			if err != nil {
				t.Errorf("Failed to parse package: %v", err)
			}
			assert.Equal(t, test.pkg, p.String())
		})
	}
}

func TestPackage_HasVersion(t *testing.T) {
	tests := []struct {
		name     string
		pkg      string
		expected bool
	}{
		{"NoVersion", "yandex-solomon-fetcher", false},
		{"EmptyVersion", "yandex-solomon-fetcher", false},
		{"WithVersion", "yandex-solomon-gateway=12345", true},
	}
	for _, test := range tests {
		t.Run(test.name, func(t *testing.T) {
			p, err := ParsePackage(test.pkg)
			if err != nil {
				t.Errorf("Failed to parse package: %v", err)
			}
			assert.Equal(t, p.HasVersion(), test.expected)
		})
	}
}

func TestPackageListToString(t *testing.T) {
	tests := []struct {
		name     string
		pkgs     []string
		version  string
		expected string
	}{
		{
			"OneNoVersion",
			[]string{"yandex-solomon-stockpile"},
			"",
			"yandex-solomon-stockpile",
		},
		{
			"ManyNoVersion",
			[]string{"yandex-solomon-stockpile", "yandex-solomon-web"},
			"",
			"yandex-solomon-stockpile yandex-solomon-web",
		},
		{
			"OneWithVersion",
			[]string{"yandex-solomon-stockpile"},
			"123",
			"yandex-solomon-stockpile=123",
		},
		{
			"ManyWithVersion",
			[]string{"yandex-solomon-stockpile", "yandex-solomon-web"},
			"123.q",
			"yandex-solomon-stockpile=123.q yandex-solomon-web=123.q",
		},
	}

	for _, test := range tests {
		t.Run(test.name, func(t *testing.T) {
			pl, err := NewPackageList(test.pkgs...)
			if err != nil {
				t.Errorf("Failed to parse package list: %v", err)
			}
			if test.version != "" {
				err = pl.SetVersion(test.version)
				if err != nil {
					t.Errorf("Failed to parse version: %v", err)
				}
			}
			assert.Equal(t, test.expected, pl.String())
		})
	}
}

func TestPackageListMarshal(t *testing.T) {
	tests := []struct {
		name    string
		pkgs    []string
		version string
	}{
		{
			"OneNoVersion",
			[]string{"yandex-solomon-stockpile"},
			"",
		},
		{
			"ManyNoVersion",
			[]string{"yandex-solomon-stockpile", "yandex-solomon-web"},
			"",
		},
		{
			"OneWithVersion",
			[]string{"yandex-solomon-stockpile"},
			"123",
		},
		{
			"ManyWithVersion",
			[]string{"yandex-solomon-stockpile", "yandex-solomon-web"},
			"123.q",
		},
		{
			"ManyWithVersion",
			[]string{"yandex-solomon-stockpile", "yandex-solomon-web"},
			"qqq~123.q",
		},
	}

	for _, test := range tests {
		t.Run(test.name, func(t *testing.T) {
			pl, err := NewPackageList(test.pkgs...)
			if err != nil {
				t.Errorf("Failed to parse package list: %v", err)
			}
			if test.version != "" {
				err = pl.SetVersion(test.version)
				if err != nil {
					t.Errorf("Failed to parse version: %v", err)
				}
			}
			plEnc, err := json.Marshal(pl)
			if err != nil {
				t.Errorf("Failed to marshal: %v", err)
			}
			var plNew PackageList
			if err := json.Unmarshal(plEnc, &plNew); err != nil {
				t.Errorf("Failed to unmarshal: %v", err)
			}
			if pl.String() != plNew.String() {
				t.Errorf("Unmarshalled and original package lists differ: %s != %s", pl.String(), plNew.String())
			}
		})
	}
}

func TestPackageVersionComparison(t *testing.T) {
	versions := []struct {
		v1     string
		v2     string
		result int
		err    bool
	}{
		{"", "", 0, false},
		{"0:qwe", "qwe", 0, false},
		{"123", "1234", -1, false},
		{"123~", "123", -1, false},
		{"123~123", "123", -1, false},
		{"~~", "~", -1, false},
		{"~", "~~a", 1, false},
		{"~", "", -1, false},
		{"1234.trunk", "1234.branch", 1, false},
		{"1234$", "", 0, true},
		{"123-asdf", "", 1, false},
		{"345.215ubuntu0.2-456~testr355", "345.215ubuntu0.2-456~", 1, false},
		{"1s35d354f-21f.sdfg+hjj-qc2-kk.123ddf+44", "1s35d354f-21f.sdfg+hjj-qc2-kk.123ddf+441", -1, false},
	}
	for _, version := range versions {
		v1, err := MakeVersion(version.v1)
		if version.err {
			if err == nil {
				t.Errorf("Want error for %s, but got nothing", version.v1)
			}
			continue
		}
		if err != nil {
			t.Errorf("Cannot make version from %s: %v", version.v1, err)
		}
		v2, err := MakeVersion(version.v2)
		if err != nil {
			t.Errorf("Cannot make version from %s: %v", version.v2, err)
		}
		result := v1.Compare(v2)
		if (result > 0 && version.result > 0) || (version.result == result) || (result < 0 && version.result < 0) {
			continue
		}
		t.Errorf("Bad version comarison '%s'.Compare('%s'), want = %v, got = %v", version.v1, version.v2, version.result, result)
	}
}

func TestPackageVersionToString(t *testing.T) {
	for _, version := range []string{
		"34:1s35d354f-21f.sdfg+hjj-qc2-kk.12~3ddf+44",
		"None",
	} {
		v, err := MakeVersion(version)
		if err != nil {
			t.Errorf("Cannot make version from %s: %v", version, err)
		}
		if v.String() != version {
			t.Errorf("Cannot convert version to string: want = %s, got = %s", version, v.String())
		}
	}
}

func BenchmarkPackageMakeVersion(b *testing.B) {
	version := "1s35d354f-21f.sdfg+hjj-qc2-kk.123ddf+44"

	b.ResetTimer()
	for i := 0; i < b.N; i++ {
		_, err := MakeVersion(version)
		if err != nil {
			b.Errorf("Cannot make version from %s: %v", version, err)
		}
	}
	b.ReportAllocs()
}

func BenchmarkPackageVersionToString(b *testing.B) {
	version := "1s35d354f-21f.sdfg+hjj-qc2-kk.123ddf+44"

	v, err := MakeVersion(version)
	if err != nil {
		b.Errorf("Cannot make version from %s: %v", version, err)
	}

	b.ResetTimer()
	for i := 0; i < b.N; i++ {
		_ = v.String()
	}
	b.ReportAllocs()
}

func BenchmarkPackageCompareVersion(b *testing.B) {
	version1 := "1s35d354f-21f.sdfg+hjj-qc2-kk.123ddf+44"
	version2 := "1s35d354f-21f.sdfg+hjj-qc2-kk.123ddf+44~"

	v1, err := MakeVersion(version1)
	if err != nil {
		b.Errorf("Cannot make version from %s: %v", version1, err)
	}
	v2, err := MakeVersion(version2)
	if err != nil {
		b.Errorf("Cannot make version from %s: %v", version2, err)
	}

	b.ResetTimer()
	for i := 0; i < b.N; i++ {
		_ = v1.Compare(v2)
	}
	b.ReportAllocs()
}
