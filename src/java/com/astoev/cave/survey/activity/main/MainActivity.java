package com.astoev.cave.survey.activity.main;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import com.astoev.cave.survey.Constants;
import com.astoev.cave.survey.R;
import com.astoev.cave.survey.activity.MainMenuActivity;
import com.astoev.cave.survey.activity.UIUtilities;
import com.astoev.cave.survey.activity.home.HomeActivity;
import com.astoev.cave.survey.activity.map.MapActivity;
import com.astoev.cave.survey.activity.map.MapUtilities;
import com.astoev.cave.survey.activity.map.opengl.Map3DActivity;
import com.astoev.cave.survey.model.Gallery;
import com.astoev.cave.survey.model.Leg;
import com.astoev.cave.survey.model.Note;
import com.astoev.cave.survey.model.Photo;
import com.astoev.cave.survey.model.Point;
import com.astoev.cave.survey.model.Sketch;
import com.astoev.cave.survey.util.DaoUtil;
import com.astoev.cave.survey.util.PointUtil;
import com.astoev.cave.survey.util.StringUtils;
import com.j256.ormlite.misc.TransactionManager;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

/**
 * Created by IntelliJ IDEA.
 * User: astoev
 * Date: 1/23/12
 * Time: 3:04 PM
 * To change this template use File | Settings | File Templates.
 */
public class MainActivity extends MainMenuActivity {

    private static final int[] ADD_ITEM_LABELS = {R.string.main_add_leg,
            R.string.main_add_branch,
//            R.string.main_add_middlepoint
//            ,R.string.main_add_custom
    };

    private Map<Integer, Integer> mGalleryColors;
    private Map<Integer, String> mGalleryNames;
    
    private static boolean isDebug = false;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
    }

    private void drawTable() {
        try {
            Leg activeLeg = getWorkspace().getLastLeg();
            getWorkspace().setActiveLeg(activeLeg);

            mGalleryColors = new HashMap<Integer, Integer>();
            mGalleryNames = new HashMap<Integer, String>();

            // prepare labels
            TextView activeLegName = (TextView) findViewById(R.id.mainActiveLeg);
            activeLegName.setText(activeLeg.buildLegDescription());

            TableLayout table = (TableLayout) findViewById(R.id.mainTable);
            Log.i(Constants.LOG_TAG_UI, "Found " + table);

            // prepare grid
            table.removeAllViews();

            List<Leg> legs = getWorkspace().getCurrProjectLegs();

            boolean currentLeg;
            for (final Leg l : legs) {
                TableRow row = new TableRow(this);
                LayoutParams params = new TableLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1);
                row.setLayoutParams(params);
                row.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View aView) {
                        Intent intent = new Intent(MainActivity.this, PointActivity.class);
                        intent.putExtra(Constants.LEG_SELECTED, l.getId());
                        getWorkspace().setActiveLegId(l.getId());
                        startActivity(intent);
                    }
                });

                currentLeg = getWorkspace().getActiveLegId().equals(l.getId());
                if (currentLeg) {
                    row.setBackgroundColor(Color.GRAY);
                }

                Point fromPoint = l.getFromPoint();
                getWorkspace().getDBHelper().getPointDao().refresh(fromPoint);
                String fromPointString = fromPoint.getName();
                
                Point toPoint = l.getToPoint();
                getWorkspace().getDBHelper().getPointDao().refresh(toPoint);
                String toPointString = toPoint.getName();
                
                if (isDebug){
                	fromPointString = fromPointString +"(" + fromPoint.getId() + ")";
                	toPointString = toPointString + "("+toPoint.getId()+")";
                }

                if (!mGalleryColors.containsKey(l.getGalleryId())) {
                    Gallery gallery = DaoUtil.getGallery(l.getGalleryId());
                    mGalleryColors.put(l.getGalleryId(), MapUtilities.getNextGalleryColor(l.getGalleryId()));
                    mGalleryNames.put(l.getGalleryId(), gallery.getName());
                }

                row.addView(createTextView(mGalleryNames.get(l.getGalleryId()), currentLeg, false, mGalleryColors.get(l.getGalleryId())));
                row.addView(createTextView(fromPointString, currentLeg, false, null));
                row.addView(createTextView(toPointString, currentLeg, false, null));
                row.addView(createTextView(l.getDistance(), currentLeg, true));
                row.addView(createTextView(l.getAzimuth(), currentLeg, true));
                row.addView(createTextView(l.getSlope(), currentLeg, true));
                
                //TODO build SNP string
                StringBuilder moreText = new StringBuilder();
                
                //TODO Debug
                if (isDebug){
                	moreText.append(l.getGalleryId()).append(" ");
                }
                
                Sketch sketch = DaoUtil.getScetchByLeg(l);
                if (sketch != null){
                	moreText.append(getString(R.string.table_sketch_prefix));
                }
                Note note = DaoUtil.getActiveLegNote(l);
                if (note != null){
                	moreText.append(getString(R.string.table_note_prefix));
                }
                Photo photo = DaoUtil.getPhotoByLeg(l);
                if (photo != null) {
                	moreText.append(getString(R.string.table_photo_prefix));
                }

                //TODO add sketch and photo here
                row.addView(createTextView(moreText.toString(), currentLeg, true, null));
                table.addView(row, params);
            }
            table.invalidate();
        } catch (Exception e) {
            Log.e(Constants.LOG_TAG_UI, "Failed to render main activity", e);
            UIUtilities.showNotification(R.string.error);
        }
    }

    private View createTextView(Float aMeasure, boolean isCurrentLeg, boolean allowEditing) {
        return createTextView(StringUtils.floatToLabel(aMeasure), isCurrentLeg, allowEditing, null);
    }

    private View createTextView(String aText, boolean isCurrentLeg, boolean allowEditing, Integer aColor) {
        TextView edit = new TextView(this);
        edit.setLines(1);
        if (aText != null) {
            edit.setText(aText);
        }
        edit.setGravity(Gravity.CENTER);
        if (aColor != null) {
            edit.setTextColor(aColor);
        }

        if (!isCurrentLeg || !allowEditing) {
            edit.setEnabled(false);
        }

        return edit;
    }

    public void addButtonClick() {

        Log.i(Constants.LOG_TAG_UI, "Adding");

        final String[] labels = new String[ADD_ITEM_LABELS.length];
        for (int i = 0; i < ADD_ITEM_LABELS.length; i++) {
            labels[i] = getString(ADD_ITEM_LABELS[i]);
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.main_add_title);

        builder.setSingleChoiceItems(labels, -1, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int item) {
                try {
                    if (0 == item) {
                        addLeg(false);
                    } else if (1 == item) {
                        addLeg(true);
//                    } else if (2 == item) {
//                        requestLengthAndAddMiddle();
//                    } else if (3 == item) {
//                        UIUtilities.showNotification(MainActivity.this, R.string.todo);
                    }
                } catch (Exception e) {
                    Log.e(Constants.LOG_TAG_UI, "Error adding", e);
                    UIUtilities.showNotification(R.string.error);
                }
            }
        });
        AlertDialog alert = builder.create();
        alert.show();
    }

    private void requestLengthAndAddMiddle() throws SQLException {

        LayoutInflater li = LayoutInflater.from(this);
        View promptsView = li.inflate(R.layout.number_popup, null);

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setView(promptsView);

        final EditText userInput = (EditText) promptsView.findViewById(R.id.popup_distance);
        final Leg currLeg = getWorkspace().getActiveOrFirstLeg();

        // set dialog message
        alertDialogBuilder
                .setCancelable(true)
                .setPositiveButton("OK",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {

                                Float distance = StringUtils.getFromEditTextNotNull(userInput);
                                if (null == distance) {
                                    UIUtilities.showNotification(R.string.popup_bad_input);
                                    dialog.cancel();
                                    return;
                                }

                                if (currLeg.getDistance() != null && currLeg.getDistance().floatValue() <= distance.floatValue()) {
                                    UIUtilities.showNotification(R.string.popup_bad_input);
                                    dialog.cancel();
                                    return;
                                }

                                try {
                                    addMiddle(distance.floatValue());
                                } catch (SQLException e) {
                                    Log.e(Constants.LOG_TAG_UI, "Error adding", e);
                                    UIUtilities.showNotification(R.string.error);
                                }
                                dialog.cancel();
                            }
                        })
                .setNegativeButton("Back",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        });

        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }

    private void addMiddle(final float atDistance) throws SQLException {

        Log.i(Constants.LOG_TAG_UI, "Creating middle point");
        Integer newLegId = TransactionManager.callInTransaction(getWorkspace().getDBHelper().getConnectionSource(),
                new Callable<Integer>() {
                    public Integer call() throws Exception {
                        try {
                            Leg activeLeg = getWorkspace().getActiveLeg();
                            float activeLegOrigDistance = activeLeg.getDistance();

                            // another leg, starting from the current leg and in new gallery
                            Point newFrom = activeLeg.getFromPoint();
                            Point oldToPoint = activeLeg.getToPoint();
                            getWorkspace().getDBHelper().getPointDao().refresh(newFrom);
                            getWorkspace().getDBHelper().getPointDao().refresh(oldToPoint);


                            // create new point
                            Point newMiddlePoint = PointUtil.generateMiddlePoint(newFrom);
                            getWorkspace().getDBHelper().getPointDao().create(newMiddlePoint);

                            // split the old leg - update the existing and add new one
                            activeLeg.setToPoint(newMiddlePoint);
                            activeLeg.setDistance(atDistance);
                            getWorkspace().getDBHelper().getLegDao().update(activeLeg);

                            Leg newLeg = new Leg(newMiddlePoint, oldToPoint, activeLeg.getProject(), activeLeg.getGalleryId());

                            // copy measurements to the new leg
                            newLeg.setAzimuth(activeLeg.getAzimuth());
                            newLeg.setDistance(activeLegOrigDistance - atDistance);
                            newLeg.setLeft(activeLeg.getLeft());
                            newLeg.setRight(activeLeg.getRight());
                            newLeg.setTop(activeLeg.getTop());
                            newLeg.setDown(activeLeg.getDown());
                            getWorkspace().getDBHelper().getLegDao().create(newLeg);

                            return newLeg.getId();
                        } catch (Exception e) {
                            Log.e(Constants.LOG_TAG_DB, "Failed to add middle point", e);
                            throw e;
                        }
                    }
                }

        );
        if (newLegId != null)

        {
            getWorkspace().setActiveLegId(newLegId);
            Intent intent = new Intent(MainActivity.this, PointActivity.class);
            intent.putExtra(Constants.LEG_SELECTED, newLegId);
            startActivity(intent);
        } else

        {
            UIUtilities.showNotification(R.string.error);
        }

    }


    private void addLeg(final boolean isDeviation) throws SQLException {
        Log.i(Constants.LOG_TAG_UI, "Creating leg");
      /*  Integer newLegId = TransactionManager.callInTransaction(getWorkspace().getDBHelper().getConnectionSource(),
                new Callable<Integer>() {
                    public Integer call() throws Exception {
                        try {
                            Leg activeLeg = (Leg) getWorkspace().getDBHelper().getLegDao().queryForId(getWorkspace().getActiveLegId());

                            if (isDeviation) {

                                // another leg, starting from the current leg and in new gallery
                                Point newFrom = activeLeg.getFromPoint();
                                getWorkspace().getDBHelper().getPointDao().refresh(newFrom);
                                Point newTo = PointUtil.generateDeviationPoint(newFrom);
                                getWorkspace().getDBHelper().getPointDao().create(newTo);


                                Gallery newGallery = new Gallery();
                                // TODO read name ?
                                newGallery.setName(newFrom.getName());
                                getWorkspace().getDBHelper().getGalleryDao().create(newGallery);

                                Leg nextLeg = new Leg(newFrom, newTo, getWorkspace().getActiveProject(), newGallery.getId());
                                getWorkspace().getDBHelper().getLegDao().create(nextLeg);
                                return nextLeg.getId();
                            } else {
                                // another leg, starting from the latest in the gallery
                                Point newFrom = (Point) getWorkspace().getLastGalleryPoint(activeLeg.getGalleryId());
                                Point newTo = PointUtil.generateNextPoint(activeLeg.getGalleryId());
                                getWorkspace().getDBHelper().getPointDao().create(newTo);

                                Leg nextLeg = new Leg(newFrom, newTo, getWorkspace().getActiveProject(), activeLeg.getGalleryId());

                                getWorkspace().getDBHelper().getLegDao().create(nextLeg);
                                return nextLeg.getId();
                            }

                        } catch (Exception e) {
                            Log.e(Constants.LOG_TAG_DB, "Failed to add leg", e);
                            throw e;
                        }
                    }
                });*/
//        if (newLegId != null) {
//            getWorkspace().setActiveLegId(newLegId);
            Intent intent = new Intent(MainActivity.this, PointActivity.class);
//            intent.putExtra(Constants.LEG_SELECTED, newLegId);
            startActivity(intent);
//        } else {
//            UIUtilities.showNotification(this, R.string.error);
//        }
    }

    @Override
    public void onResume() {  // After a pause OR at startup
        super.onResume();
        //Refresh your stuff here
        drawTable();
    }

    public void plotButton() {
        Intent intent = new Intent(this, MapActivity.class);
        startActivity(intent);
    }

    public void plot3dButton() {
        Intent intent = new Intent(this, Map3DActivity.class);
        startActivity(intent);
    }

    public void infoButton() {
        Intent intent = new Intent(this, InfoActivity.class);
        startActivity(intent);
    }

    public void changeButton() {
        Log.i(Constants.LOG_TAG_UI, "Change active leg");
        try {

            final List<Leg> legs = getWorkspace().getCurrProjectLegs();
            List<String> itemsList = new ArrayList<String>();
            int selectedItem = -1;
            int counter = 0;
            for (Leg l : legs) {
                itemsList.add(l.buildLegDescription());
                if (l.getId() == getWorkspace().getActiveLegId()) {
                    selectedItem = counter;
                } else {
                    counter++;
                }
            }
            final CharSequence[] items = itemsList.toArray(new CharSequence[itemsList.size()]);

            Log.d(Constants.LOG_TAG_UI, "Display " + items.length + " legs");
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.main_button_change_title);

            builder.setSingleChoiceItems(items, selectedItem, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int item) {

                    Log.i(Constants.LOG_TAG_UI, "Selected leg " + legs.get(item));
                    getWorkspace().setActiveLegId(legs.get(item).getId());

                    Intent intent = new Intent(MainActivity.this, MainActivity.class);
                    startActivity(intent);
                }
            });
            AlertDialog alert = builder.create();
            alert.show();
        } catch (Exception e) {
            Log.e(Constants.LOG_TAG_DB, "Failed to select leg", e);
            UIUtilities.showNotification(R.string.error);
        }

    }

    @Override
    public void onBackPressed() {
        Intent setIntent = new Intent(this, HomeActivity.class);
        startActivity(setIntent);
    }

	/**
	 * @see com.astoev.cave.survey.activity.MainMenuActivity#getChildsOptionsMenu()
	 */
	@Override
	protected int getChildsOptionsMenu() {
		return R.menu.mainmenu;
	}

	/**
	 * @see com.astoev.cave.survey.activity.MainMenuActivity#onOptionsItemSelected(android.view.MenuItem)
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		
		Log.i(Constants.LOG_TAG_UI, "Main activity's menu selected - " + item.toString());
		
		switch (item.getItemId()) {
			case R.id.main_action_add:{
				addButtonClick();
				return true;
			}
			case R.id.main_action_select : {
				changeButton();
				return true;
			}
			case R.id.main_action_map :{
				plotButton();
				return true;
			}
			case R.id.main_action_info : {
				infoButton();
				return true;
			}
			default:
				return super.onOptionsItemSelected(item);
		}
	}
    
}