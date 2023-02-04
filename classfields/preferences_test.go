package panel

import (
	"testing"

	"github.com/stretchr/testify/require"
)

func TestPreferences_AddPanels(t *testing.T) {
	pref := &Preferences{Type: RowType}

	pref.AddPanelsToRow([]*Preferences{
		{GridPos: GridPos{H: 7, W: 16}},
		{GridPos: GridPos{H: 3, W: 8}},
		{GridPos: GridPos{H: 12, W: 8}},
		{GridPos: GridPos{H: 6, W: 8}},
		{GridPos: GridPos{H: 10, W: 12}},
	}...)

	assertGridPos(t, GridPos{X: 0, Y: 0}, pref.Panels[0].GridPos)
	assertGridPos(t, GridPos{X: 16, Y: 0}, pref.Panels[1].GridPos)
	assertGridPos(t, GridPos{X: 0, Y: 8}, pref.Panels[2].GridPos)
	assertGridPos(t, GridPos{X: 8, Y: 8}, pref.Panels[3].GridPos)
	assertGridPos(t, GridPos{X: 0, Y: 21}, pref.Panels[4].GridPos)
}

func assertGridPos(t *testing.T, expected, actual GridPos) {
	require.Equal(t, expected.Y, actual.Y)
	require.Equal(t, expected.X, actual.X)
}
