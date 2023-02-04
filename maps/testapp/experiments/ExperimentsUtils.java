package com.yandex.maps.testapp.experiments;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;

import com.yandex.mapkit.MapKitFactory;
import com.yandex.maps.testapp.Utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;

import static com.yandex.runtime.Runtime.getApplicationContext;

public class ExperimentsUtils {
    private static final String ACTIVE_EXPERIMENTS_STORAGE = "file_with_experiments";
    private static final String ACTIVE_EXPERIMENTS_LIST = "activeExperiments";

    private static String serializeToString(Object obj) throws IOException {
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        ObjectOutputStream output = new ObjectOutputStream(byteStream);
        output.writeObject(obj);
        output.flush();
        output.close();
        return Base64.encodeToString(byteStream.toByteArray(), Base64.DEFAULT);
    }

    private static Object deserializeFromString(String data)
            throws IOException, ClassNotFoundException {
        byte b[] = Base64.decode(data, Base64.DEFAULT);
        ObjectInputStream input = new ObjectInputStream(new ByteArrayInputStream(b));
        Object result = input.readObject();
        input.close();
        return result;
    }

    @SuppressWarnings("unchecked")
    public static List<Experiment> loadExperimentsList(Context context) {
        SharedPreferences sharedPref = getApplicationContext().getSharedPreferences(
                ACTIVE_EXPERIMENTS_STORAGE, Context.MODE_PRIVATE);
        String data = sharedPref.getString(ACTIVE_EXPERIMENTS_LIST, null);
        if (data == null)
            return new ArrayList<Experiment>();
        try {
            return (List<Experiment>) deserializeFromString(data);
        } catch (ClassNotFoundException e) {
            Utils.showError(context, e);
        } catch (IOException e) {
            Utils.showError(context, e);
            return new ArrayList<Experiment>();
        }
        throw new UnknownError();
    }

    public static void addExperimentToDump(Experiment experiment, Context context) {
        List<Experiment> experiments = loadExperimentsList(context);
        experiments.add(experiment);
        dumpExperiments(experiments, context);
    }

    public static void removeExperimentFromDump(Experiment experiment, Context context) {
        List<Experiment> experiments = loadExperimentsList(context);
        experiments.remove(experiment);
        dumpExperiments(experiments, context);
    }

    public static void dumpExperiments(List<Experiment> experimentsList, Context context) {
        SharedPreferences sharedPref = getApplicationContext().getSharedPreferences(
                ACTIVE_EXPERIMENTS_STORAGE, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        try {
            editor.putString(ACTIVE_EXPERIMENTS_LIST, serializeToString(experimentsList));
        } catch (IOException e) {
            Utils.showError(context, e);
        }
        editor.commit();
    }

    public static void refreshCustomExperiments(List<Experiment> experiments) {
        for (Experiment experiment : experiments) {
            refreshCustomExperiment(experiment);
        }
    }

    public static void refreshCustomExperiment(Experiment experiment) {
        MapKitFactory.getInstance()
            .getUiExperimentsManager()
            .setValue(experiment.serviceId, experiment.parameterName, experiment.parameterValue);
    }

    public static void resetCustomExperiment(Experiment experiment) {
        refreshCustomExperiment(new Experiment(experiment.serviceId, experiment.parameterName, null));
    }
}
