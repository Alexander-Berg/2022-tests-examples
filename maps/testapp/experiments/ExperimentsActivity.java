package com.yandex.maps.testapp.experiments;

import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.yandex.mapkit.MapKitFactory;
import com.yandex.maps.testapp.Environment;
import com.yandex.maps.testapp.R;
import com.yandex.maps.testapp.TestAppActivity;
import com.yandex.maps.testapp.Utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ExperimentsActivity extends TestAppActivity {
    private List<Experiment> activeExperiments = new ArrayList<Experiment>();
    private List<ExperimentButton> experimentsButtons = new ArrayList<ExperimentButton>();

    private ExperimentButtonsAdapter experimentButtonsAdapter;
    private ExperimentsAdapter activeExperimentsAdapter;

    private CheckBox clearCacheBtn;
    private TextView inputParameter;
    private TextView inputServiceId;
    private TextView inputValue;

    @Override
    public void onResume() {
        super.onResume();
        inputServiceId = (TextView)findViewById(R.id.service_id);
        inputParameter = (TextView)findViewById(R.id.parameter_name);
        inputValue = (TextView)findViewById(R.id.parameter_value);
    }

    @Override
    protected void onStartImpl(){}
    @Override
    protected void onStopImpl(){}

    public void onAddParameter(View view) {
        String serviceId = inputServiceId.getText().toString();
        String parameterName = inputParameter.getText().toString();
        String value = inputValue.getText().toString();

        if (serviceId.isEmpty() || parameterName.isEmpty() || value.isEmpty())
            return;

        inputServiceId.setText("");
        inputParameter.setText("");
        inputValue.setText("");

        switchExperimentButtonState(true, new Experiment(serviceId, parameterName, value));
    }

    private void updateExperimentState(Boolean on, Experiment experiment){
        for (ExperimentButton button : experimentsButtons) {
            if (button.experiment.equals(experiment)){
                button.on = on;
                break;
            }
        }

        ExperimentsUtils.dumpExperiments(activeExperiments, this);
        activeExperimentsAdapter.updateList(activeExperiments);
        experimentButtonsAdapter.updateList(experimentsButtons);
    }

    private void disableExperiment(final Experiment experiment){
        activeExperiments.remove(experiment);
        ExperimentsUtils.resetCustomExperiment(experiment);

        updateExperimentState(false, experiment);
    }

    private void enableExperiment(final Experiment experiment){
        ArrayList<Experiment> willBeDisabled = new ArrayList<>();

        for (Experiment exp : activeExperiments) {
            if (exp.experimentsConflict(experiment)) {
                willBeDisabled.add(exp);
            }
        }

        for (Experiment exp : willBeDisabled) {
            disableExperiment(exp);
        }

        activeExperiments.add(experiment);
        Collections.sort(activeExperiments, new ExperimentComparator());
        ExperimentsUtils.refreshCustomExperiment(experiment);

        updateExperimentState(true, experiment);
    }

    public void switchExperimentButtonState(Boolean on, Experiment experiment){
        if (on) {
            enableExperiment(experiment);
        } else {
            disableExperiment(experiment);
        }
        if (clearCacheBtn.isChecked()) {
            MapKitFactory.getInstance().getOfflineCacheManager().clear(() -> {});
            MapKitFactory.getInstance().getStorageManager().clear(() -> {});
        }
    }

    private CompoundButton.OnCheckedChangeListener getListener(final Experiment experiment, ExperimentButton experimentButton) {
        return ((compoundButton, on) -> {
            switchExperimentButtonState(on, experiment);
        });
    }

    private void createExperimentButton(String name, String serviceId, String parameter, String curValue) {
        createExperimentButton(name, serviceId, parameter, curValue, "");
    }

    private void createExperimentButton(String name, String serviceId, String parameter, String curValue, String experimentGroup) {
        ExperimentButton experimentButton = new ExperimentButton();

        Experiment experiment = new Experiment(serviceId, parameter, curValue, experimentGroup);

        for (Experiment exp : activeExperiments) {
            if (exp.equals(experiment)) {
                experimentButton.on = true;
                break;
            }
        }

        experimentButton.name = name;
        experimentButton.on = activeExperiments.contains(experiment);
        experimentButton.experiment = experiment;
        experimentButton.listener = getListener(experiment, experimentButton);

        experimentsButtons.add(experimentButton);
        experimentButtonsAdapter.updateList(experimentsButtons);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        activeExperiments = ExperimentsUtils.loadExperimentsList(this);
        setContentView(R.layout.experiments);

        activeExperimentsAdapter = new ExperimentsAdapter(this,
                R.layout.experiment_row, new ArrayList<Experiment>(activeExperiments));
        ((ListView) findViewById(R.id.experiments_groups)).setAdapter(activeExperimentsAdapter);

        experimentButtonsAdapter = new ExperimentButtonsAdapter(this,
                R.layout.toggle_button, new ArrayList<ExperimentButton>(experimentsButtons));
        ((ListView) findViewById(R.id.btns_layout)).setAdapter(experimentButtonsAdapter);

        String env = Environment.readEnvironmentFromPreferences(this);
        if (env.equals("production")) {
            ((RadioButton)findViewById(R.id.prodRadioButton)).setChecked(true);
            createExperimentButton("dataprestable", "MAPS_CONFIG", "experimental_dataprestable", "1", "experimental_environment");
        } else if (env.equals("testing")) {
            ((RadioButton)findViewById(R.id.testingRadioButton)).setChecked(true);
            createExperimentButton("datatesting", "MAPS_CONFIG", "experimental_datatesting", "1", "experimental_environment");
            createExperimentButton("datavalidation", "MAPS_CONFIG", "experimental_datavalidation", "1", "experimental_environment");
        }

        clearCacheBtn = new CheckBox(this);
        clearCacheBtn.setText("Clear cache");
        clearCacheBtn.setChecked(true);
        ((LinearLayout)findViewById(R.id.checkbox_pole)).addView(clearCacheBtn);

        addExperimentButtons();

        ExperimentsUtils.refreshCustomExperiments(activeExperiments);

        ((RadioGroup)findViewById(R.id.environment_radio_group)).setOnCheckedChangeListener((radioGroup, i) -> {
            switch (i) {
                case R.id.prodRadioButton:
                    Environment.writeEnvironmentToPreferences("production", ExperimentsActivity.this);
                    break;
                case R.id.testingRadioButton:
                    Environment.writeEnvironmentToPreferences("testing", ExperimentsActivity.this);
                    break;
            }

            Utils.createRestartDialog(ExperimentsActivity.this).show();
        });
    }

    @Override
    protected void onSaveInstanceState(Bundle bundle) {
        super.onSaveInstanceState(bundle);
        ExperimentsUtils.dumpExperiments(activeExperiments, this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    protected void addExperimentButtons() {
        createExperimentButton("Эксперимент с маяками в индоре (определение на этаже)", "MAPS_INDOOR", "indoor_positioning_coverage_enabled", "true");
        createExperimentButton("Приоритет при отрисовке пинов подмешанного геопродукта", "MAPS_GEOSEARCH", "experimental_rearr", "scheme_Local/Geo/Adverts/InjectionBySameRubric/Enabled=1;scheme_Local/Geo/Adverts/InjectionBySameRubric/LimitMaxadvByBusinessSize=0;scheme_Local/Geo/Adverts/InjectionBySameRubric/ZoomoutEnabled=1;scheme_Local/Geo/Adverts/InjectionBySameRubric/ZoomoutMinAdverts=3;scheme_Local/Geo/Adverts/InjectionBySameRubric/InjectOnFixedList=1;scheme_Local/Geo/Adverts/InjectionBySameRubric/BottomMaxadv=5;scheme_Local/Geo/Adverts/Zoomout/Limits='17-15'");
        createExperimentButton("Проверка продвинутого состояния пинов", "MAPS_GEOSEARCH", "experimental_rearr", "scheme_Local/Geo/Adverts/InjectionBySameRubric/Enabled=1;scheme_Local/Geo/Adverts/InjectionBySameRubric/LimitMaxadvByBusinessSize=0;scheme_Local/Geo/Adverts/InjectionBySameRubric/ZoomoutEnabled=1;scheme_Local/Geo/Adverts/InjectionBySameRubric/ZoomoutMinAdverts=3;scheme_Local/Geo/Adverts/InjectionBySameRubric/InjectOnFixedList=1;scheme_Local/Geo/Adverts/InjectionBySameRubric/BottomMaxadv=5;scheme_Local/Geo/Adverts/Zoomout/Limits='19-17'");
        createExperimentButton("Локальные разговорчики 500м", "MAPKIT", "road_events_layer_locality_debug", "r=500,draw_area=1");
        createExperimentButton("Локальные разговорчики 1000м", "MAPKIT", "road_events_layer_locality_debug", "r=1000,draw_area=1");
        createExperimentButton("Раздел Guidance/After end simulation (Продолжение ведения после достижения финиша) Первый эксперимент", "MAPKIT", "guidance_allow_finish", "0");
        createExperimentButton("Раздел Guidance/After end simulation (Продолжение ведения после достижения финиша) Второй эксперимент", "MAPKIT", "guidance_finish_speed", "1");
        createExperimentButton("Раздел Guidance / Логика way точек III 1)", "MAPKIT", "guidance_max_waypoint_speed", "0.5");
        createExperimentButton("Раздел Guidance / Логика way точек III 2)", "MAPKIT", "guidance_possible_waypoint_distance", "100");
        createExperimentButton("Раздел Guidance / Логика way точек III 3)", "MAPKIT", "guidance_possible_waypoint_timeout", "0");
        createExperimentButton("Раздел Guidance / Логика way точек III 4)", "MAPKIT", "guidance_allow_jump_over_waypoint", "0");
        createExperimentButton("Раздел Selection/ Tappable area изменение области клика для иконок", "MAPKIT", "min_placemark_tappable_area_size", "100");
        createExperimentButton("Раздел Selection/ Tappable area изменение области клика для иконок , но с другим значением value (отдельной кнопкой)", "MAPKIT", "min_placemark_tappable_area_size", "0");
        createExperimentButton("Раздел Personalized Poi/Проверка карточки poi", "MAPKIT", "personalized_poi_in_mapkit_162.2", "on");
        createExperimentButton("Jams/Отключение multizoom", "MAPKIT", "jams_multizoom", "17,17");
        createExperimentButton("Jams/Изменение параметров multizoom", "MAPKIT", "jams_multizoom", "14,16,17,20");
        createExperimentButton("Раздел Selection / Изменение масштаба тайлов 1)", "MAPKIT", "map_zoomranges", "15, 19");
        createExperimentButton("Раздел Selection / Изменение масштаба тайлов 2)", "MAPKIT", "jams_multizoom", "15, 19");
        createExperimentButton("Раздел Selection / Изменение масштаба тайлов 3)", "MAPKIT", "personalized_poi_multizoom", "15, 1");
        createExperimentButton("Раздел Fitness navigation / Включение топонимов", "MAPS_MT_ROUTER", "experimental_toponyms", "1");
    }

}
