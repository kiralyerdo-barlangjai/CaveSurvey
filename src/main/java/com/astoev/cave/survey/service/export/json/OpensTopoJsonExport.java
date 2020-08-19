package com.astoev.cave.survey.service.export.json;

import android.content.Context;
import android.util.Log;

import com.astoev.cave.survey.Constants;
import com.astoev.cave.survey.activity.util.UIUtilities;
import com.astoev.cave.survey.model.Location;
import com.astoev.cave.survey.model.Option;
import com.astoev.cave.survey.model.Photo;
import com.astoev.cave.survey.model.Project;
import com.astoev.cave.survey.model.Sketch;
import com.astoev.cave.survey.service.Options;
import com.astoev.cave.survey.service.export.AbstractExport;
import com.astoev.cave.survey.service.export.ExportEntityType;
import com.astoev.cave.survey.util.ConfigUtil;
import com.astoev.cave.survey.util.FileStorageUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import static com.astoev.cave.survey.service.export.ExportEntityType.LEG;

/**
 * Created by astoev on 8/28/14.
 */
public class OpensTopoJsonExport extends AbstractExport {

    private JSONArray rows;
    private JSONObject project;
    private JSONObject row;
    private JSONObject prevRow;

    public OpensTopoJsonExport(Context aContext) {
        super(aContext);
        mExtension = "_openstopo.json";
        mUseUniqueName = false;
    }

    @Override
    protected void prepare(Project aProject) {
        Log.i(Constants.LOG_TAG_SERVICE, "Start JSON export ");

        try {

            project = new JSONObject();
            project.put("name", FileStorageUtil.getNormalizedProjectName(aProject.getName()));
            project.put("northdeclination", "0");

            rows = new JSONArray();
            JSONObject headerRow = new JSONObject();

            headerRow.put("from", null);
            headerRow.put("to", null);
            headerRow.put("len", "m"); // currently only meters supported
            if (Option.UNIT_GRADS.equals(Options.getOptionValue(Option.CODE_AZIMUTH_UNITS))
                    || Option.UNIT_GRADS.equals(Options.getOptionValue(Option.CODE_SLOPE_UNITS))) {
                // opens topo does not support grads, may implement conversion in the future
                Log.i(Constants.LOG_TAG_SERVICE, "OpensTopo - conversion to grads not implemented");
                UIUtilities.showRawMessage(ConfigUtil.getContext(), "Grads not supported in OpensTopo");
                throw new RuntimeException("Unable to convert to grads");
            }
            headerRow.put("compass", "deg");
            headerRow.put("clino", "deg");
            headerRow.put("top", "m");
            headerRow.put("left", "m");
            headerRow.put("right", "m");
            headerRow.put("bottom", "m");
            headerRow.put("r", null);

            rows.put(headerRow);

            // empty initial row to shift the side measurements
            prepareEntity(0, LEG);
            prevRow = row;
            populateValue(Entities.FROM, "0");
            populateValue(Entities.TO, "A0");
            populateValue(Entities.DISTANCE, 0);
            populateValue(Entities.COMPASS, 0);
            populateValue(Entities.INCLINATION, 0);

            project.put("data", rows);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void prepareEntity(int rowCounter, ExportEntityType type) {

        try {
            prevRow = row;
            row = new JSONObject();
            row.put("from", "");
            row.put("to", "");
            row.put("len", "");
            row.put("compass", "");
            row.put("clino", "");
            row.put("top", "");
            row.put("left", "");
            row.put("right", "");
            row.put("bottom", "");
            row.put("r", "");

            rows.put(row);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected InputStream getContent() {
        try {
            return new ByteArrayInputStream(project.toString(2).getBytes());
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void setValue(Entities entityType, String aLabel) {
        populateValue(entityType, aLabel);
    }

    @Override
    protected void setValue(Entities entityType, Float aValue) {
        if (aValue != null) {
            populateValue(entityType, "" + aValue);
        }
    }

    private void populateValue(Entities entityType, Object aValue) {
        try {

            if (aValue == null) {
                return;
            }

            switch (entityType) {
                case FROM:
                    row.put("from", escapeName((String) aValue));
                    break;
                case TO:
                    row.put("to", escapeName((String) aValue));
                    break;
                case DISTANCE:
                    row.put("len", aValue);
                    break;
                case COMPASS:
                    row.put("compass", aValue);
                    break;
                case INCLINATION:
                    row.put("clino", aValue);
                    break;
                case LEFT:
                    prevRow.put("left", aValue);
                    break;
                case RIGHT:
                    prevRow.put("right", aValue);
                    break;
                case UP:
                    prevRow.put("top", aValue);
                    break;
                case DOWN:
                    prevRow.put("bottom", aValue);
                    break;
                case NOTE:
                    row.put("note", aValue);
                    break;
            }
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    private String escapeName(String aName) {
        return aName.replaceAll(Constants.MIDDLE_POINT_DELIMITER, Constants.MIDDLE_POINT_DELIMITER_EXPORT);
    }

    @Override
    protected void setPhoto(Photo photo) {
        // not needed
    }

    @Override
    protected void setLocation(Location aLocation) {
        try {
            project.putOpt("geoPoint", row.get("from"));
            project.putOpt("altitude", String.valueOf(aLocation.getAltitude()));
            project.putOpt("latitude", String.valueOf(aLocation.getLatitude()));
            project.putOpt("longitude", String.valueOf(aLocation.getLongitude()));
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void setDrawing(Sketch aSketch) {
        // not needed
    }
}
