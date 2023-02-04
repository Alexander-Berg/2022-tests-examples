package packages

import (
	"testing"

	"github.com/stretchr/testify/require"
)

var (
	oldContent = []byte(`
---

- name: Install apt repo packages
  apt:
    install_recommends: no
    update_cache: yes
    cache_valid_time: 600
    force: yes
    pkg:
      - apt-utils=2.0.6
      - apt=2.0.6
      - python3-requests=2.22.0-2ubuntu1 #some comment
  register: aptly

- name: 'updating apt cache'
  apt: update_cache=yes
  changed_when: False
  when: aptly.changed

- name: Install vertis common packages
  apt:
    install_recommends: no
    update_cache: yes
    cache_valid_time: 600
    force: yes
    pkg:
      - logrotate=3.14.0-4ubuntu3
      - python3-requests=2.22.0-2ubuntu1
      - bind9-host=1:9.16.1
`)

	newContent = []byte(`
---

- name: Install apt repo packages
  apt:
    install_recommends: no
    update_cache: yes
    cache_valid_time: 600
    force: yes
    pkg:
      - apt-utils=2.0.6
      - apt=2.0.7
      - python3-requests=2.22.0-2ubuntu2 #some comment
  register: aptly

- name: 'updating apt cache'
  apt: update_cache=yes
  changed_when: False
  when: aptly.changed

- name: Install vertis common packages
  apt:
    install_recommends: no
    update_cache: yes
    cache_valid_time: 600
    force: yes
    pkg:
      - logrotate=3.14.0-4ubuntu3
      - python3-requests=2.22.0-2ubuntu2
      - bind9-host=1:9.16.1-0ubuntu2.10
`)

	oldPackages = Map{
		"bind9-host":       "1:9.16.1",
		"apt-utils":        "2.0.6",
		"apt":              "2.0.6",
		"python3-requests": "2.22.0-2ubuntu1",
		"logrotate":        "3.14.0-4ubuntu3",
	}

	updatePackages = Map{
		"apt":              "2.0.7",
		"python3-requests": "2.22.0-2ubuntu2",
		"bind9-host":       "1:9.16.1-0ubuntu2.10",
	}
)

func TestGreater(t *testing.T) {
	ver1 := "2.22.0-2ubuntu1"
	ver2 := "2.21.0-2ubuntu1"

	greater, err := Greater(ver1, ver2)
	require.NoError(t, err)
	require.Equal(t, true, greater)

	greater, err = Greater("", ver2)
	require.NoError(t, err)
	require.Equal(t, false, greater)

	greater, err = Greater(ver1, "")
	require.NoError(t, err)
	require.Equal(t, true, greater)

	_, err = Greater("not-valid", ver2)
	require.Error(t, err)
}

func TestParse(t *testing.T) {
	pkgs, err := Parse(oldContent)
	require.NoError(t, err)
	require.Equal(t, oldPackages, pkgs)
}

func TestUpdate(t *testing.T) {
	result := Update(oldContent, updatePackages)
	require.Equal(t, newContent, result)
}
