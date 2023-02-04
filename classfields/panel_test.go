package panel

import (
	"encoding/json"
	"testing"

	"github.com/mitchellh/mapstructure"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
)

var (
	jsonDashboard = `{
    "panels": [
	{
      "collapsed": false,
      "gridPos": {
        "h": 1,
        "w": 24,
        "x": 0,
        "y": 0
      },
      "panels": [],
      "title": "Status",
      "type": "row"
    },
    {
        "collapsed": true,
        "gridPos": {
          "h": 1,
          "w": 24,
          "x": 0,
          "y": 0
        },
        "datasource": {
          "uid": "$ENV"
  		},
        "panels": null,
        "title": "title",
        "type": "graph"
    },
    {
        "collapsed": true,
        "gridPos": {
          "h": 1,
          "w": 24,
          "x": 0,
          "y": 40
        },
        "panels": [
          {
            "gridPos": {
              "h": 10,
              "w": 12,
              "x": 0,
              "y": 46
            },
			"datasource": {
			  "uid": "000000028",
		      "type": "grafana-solomon-datasource"
			},
            "panels": null,
            "title": "_GC timings",
            "type": "graph"
          },
          {
            "gridPos": {
              "h": 10,
              "w": 12,
              "x": 12,
              "y": 46
            },
            "panels": null,
            "title": "_Goroutines",
            "type": "graph"
          }
        ],
        "title": "_GO",
        "type": "row"
      }
    ]
}`
)

func TestMakeFromPreferences(t *testing.T) {
	pref := Preferences{
		Title:       "title",
		Description: "description",
		Type:        TimeSeriesType,
		Collapsed:   true,
		Datasource:  &Datasource{Type: EnvDatasource},
		GridPos:     GridPos{H: 10, W: 12},
		Options: Options{
			Tooltip: TooltipOptions{
				Mode: "single",
			},
			Legend: OptionsLegend{
				DisplayMode: "list",
				Placement:   "bottom",
				Calcs:       []string{},
			},
		},
		Targets: []*Target{
			{
				RefID:  "id",
				Target: "target",
				Type:   "timeserie",
				Select: [][]Select{{}},
				GroupBy: []GroupBy{
					{
						Params: []string{"$__interval"},
						Type:   "time",
					},
				},
			},
		},
		Panels: []*Preferences{
			{Type: "pref"},
		},
		FieldConfig: FieldConfig{
			Defaults: Defaults{
				Color: Color{
					Mode: "palette-classic",
				},
				Thresholds: Thresholds{
					Mode: "absolute",
				},
				Custom: Custom{
					DrawStyle:         "line",
					LineInterpolation: "linear",
					LineWidth:         1,
					FillOpacity:       10,
					GradientMode:      "none",
					ShowPoints:        "never",
					Stacking: Stacking{
						Mode: "none",
					},
					AxisPlacement: "auto",
					SpanNulls:     true,
				},
			},
		},
	}

	p := MakeFromPreferences(pref)

	assert.NotNil(t, p)
	assert.Equal(t, pref.Title, p.rawData[titleField])
	assert.Equal(t, pref.Type, p.rawData[typeField])
	assert.Equal(t, pref.Collapsed, p.rawData[collapsedField])
	assert.Equal(t, pref.Targets, p.rawData[targetsField])
	assert.Equal(t, pref.Panels, p.rawData[panelsField])
	assert.Equal(t, pref.FieldConfig, p.rawData[fieldConfigField])
	assert.Equal(t, pref.Options, p.rawData[optionsField])
	assert.Equal(t, pref.GridPos, p.gridPos)
}

func TestMakeFromRawResponse(t *testing.T) {
	var res map[string]interface{}
	err := json.Unmarshal([]byte(jsonDashboard), &res)
	require.NoError(t, err)

	panels, err := MakeFromRawResponse(res)
	require.NoError(t, err)

	assert.Equal(t, 3, len(panels))
	AssertPanels(t, Preferences{
		Type:      "row",
		Title:     "Status",
		Collapsed: false,
		GridPos: GridPos{
			H: 1,
			W: 24,
		},
		Panels: []*Preferences{},
	}, panels[0])
	AssertPanels(t, Preferences{
		Type:      "graph",
		Title:     "title",
		Collapsed: true,
		GridPos: GridPos{
			H: 1,
			W: 24,
		},
		Datasource: &Datasource{
			Uid: EnvDatasource,
		},
	}, panels[1])
	AssertPanels(t, Preferences{
		Type:      "row",
		Title:     "_GO",
		Collapsed: true,
		GridPos: GridPos{
			H: 1,
			W: 24,
			Y: 40,
		},
		Panels: []*Preferences{
			{
				Type:  "graph",
				Title: "_GC timings",
				GridPos: GridPos{
					H: 10,
					W: 12,
					Y: 46,
				},
				Datasource: &Datasource{
					Type: SolomonDatasource,
					Uid:  "000000028",
				},
			},
			{
				Type:  "graph",
				Title: "_Goroutines",
				GridPos: GridPos{
					H: 10,
					W: 12,
					X: 12,
					Y: 46,
				},
			},
		},
	}, panels[2])
}

func TestSetCollapsed(t *testing.T) {
	pref := Preferences{
		Collapsed: false,
	}

	p := MakeFromPreferences(pref)
	p.SetCollapsed(true)

	assert.True(t, p.IsCollapsed())
	assert.True(t, p.rawData[collapsedField].(bool))
}

func TestSetGridPos(t *testing.T) {
	pref := Preferences{}

	p := MakeFromPreferences(pref)

	pos := GridPos{
		H: 1,
		W: 2,
		X: 3,
		Y: 4,
	}
	p.SetGridPos(pos)

	assert.Equal(t, pos, p.gridPos)
	assert.Equal(t, pos, p.GetGridPos())
}

func AssertPanels(t *testing.T, pref Preferences, panel Panel) {
	assert.Equal(t, pref.Title, panel.GetTitle())
	assert.Equal(t, pref.Type, panel.GetType())
	assert.Equal(t, pref.GridPos, panel.GetGridPos())
	assert.Equal(t, pref.Collapsed, panel.IsCollapsed())
	AssertDatasource(t, pref, panel)
	assert.Equal(t, len(pref.Panels), len(panel.GetPanels()))

	var childPanels []*Preferences
	err := mapstructure.Decode(panel.GetPanels(), &childPanels)
	require.NoError(t, err)

	if len(pref.Panels) == 0 {
		assert.Equal(t, 0, len(childPanels))
	} else {
		for i := range childPanels {
			AssertPanels(t, *childPanels[i], *MakeFromPreferences(*pref.Panels[i]))
		}
	}
}

func AssertDatasource(t *testing.T, pref Preferences, panel Panel) {
	var actualDatasource *Datasource
	err := mapstructure.Decode(panel.rawData[datasourceField], &actualDatasource)
	require.NoError(t, err)

	switch expectedDatasource := pref.Datasource.(type) {
	case *Datasource:
		assert.Equal(t, *expectedDatasource, *actualDatasource)
	case nil:
		assert.Nil(t, actualDatasource)
	case map[string]interface{}:
		assert.Equal(t, expectedDatasource["uid"], actualDatasource.Uid)
		assert.Equal(t, expectedDatasource["type"], actualDatasource.Type)
	default:
		assert.Fail(t, "panel datasource has %T type. Expected type: %T", expectedDatasource, actualDatasource)
	}
}
