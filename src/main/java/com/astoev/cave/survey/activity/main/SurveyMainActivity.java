package com.astoev.cave.survey.activity.main;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseArray;
import android.util.SparseIntArray;
import android.view.Gravity;
import android.view.MenuItem;
import android.widget.LinearLayout.LayoutParams;
import android.widget.ScrollView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;

import com.astoev.cave.survey.Constants;
import com.astoev.cave.survey.R;
import com.astoev.cave.survey.activity.MainMenuActivity;
import com.astoev.cave.survey.activity.dialog.AddNewDialog;
import com.astoev.cave.survey.activity.dialog.AddNewSelectedHandler;
import com.astoev.cave.survey.activity.dialog.ChangeLegDialog;
import com.astoev.cave.survey.activity.dialog.MiddlePointDialog;
import com.astoev.cave.survey.activity.map.MapActivity;
import com.astoev.cave.survey.activity.map.MapUtilities;
import com.astoev.cave.survey.activity.util.UIUtilities;
import com.astoev.cave.survey.model.Gallery;
import com.astoev.cave.survey.model.Leg;
import com.astoev.cave.survey.model.Location;
import com.astoev.cave.survey.model.Note;
import com.astoev.cave.survey.model.Photo;
import com.astoev.cave.survey.model.Point;
import com.astoev.cave.survey.model.Project;
import com.astoev.cave.survey.model.Sketch;
import com.astoev.cave.survey.service.Workspace;
import com.astoev.cave.survey.util.DaoUtil;
import com.astoev.cave.survey.util.StringUtils;

import java.util.List;

import static com.astoev.cave.survey.R.string.main_add_gallery;
import static com.astoev.cave.survey.R.string.main_add_leg;
import static com.astoev.cave.survey.R.string.main_add_middlepoint;
import static com.astoev.cave.survey.model.GalleryType.GEOLOCATION;

/**
 * User: astoev
 * Date: 1/23/12
 * Time: 3:04 PM
 *
 * @author Alexander Stoev
 * @author Zhivko Mitrev
 */
public class SurveyMainActivity extends MainMenuActivity implements AddNewSelectedHandler{

    private SparseIntArray mGalleryColors = new SparseIntArray();
    private SparseArray<String> mGalleryNames = new SparseArray<>();
    
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.survey);
    }

    @Override
    protected boolean showBaseOptionsMenu() {
        return false;
    }

    private void drawTable() {
        try {
            Leg activeLeg = getWorkspace().getActiveLeg();

            if (activeLeg == null) {
                activeLeg = getWorkspace().getLastLeg();
                getWorkspace().setActiveLeg(activeLeg);
            }

            mGalleryColors.clear();
            mGalleryNames.clear();

            // prepare labels
            TextView activeLegName = findViewById(R.id.mainActiveLeg);
            activeLegName.setText(activeLeg.buildLegDescription());

            TableLayout table = findViewById(R.id.mainTable);

            // prepare grid
            table.removeAllViews();

            List<Leg> legs = DaoUtil.getCurrProjectLegs(true);

            boolean currLegFlag;
            Integer lastGalleryId = null, prevGalleryId;
            TableRow activeRow = null;

            // prepare leg attributes
            String sketchPrefix = getString(R.string.table_sketch_prefix);
            String notePrefix = getString(R.string.table_note_prefix);
            String photoPrefix = getString(R.string.table_photo_prefix);
            String locationPrefix = getString(R.string.table_location_prefix);
            String vectorsPrefix = getString(R.string.table_vector_prefix);

            for (final Leg l : legs) {
                TableRow row = new TableRow(this);
                LayoutParams params = new TableLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1);
                row.setLayoutParams(params);
                row.setOnClickListener(aView -> {
                    Intent intent = new Intent(SurveyMainActivity.this, PointActivity.class);
                    intent.putExtra(Constants.LEG_SELECTED, l.getId());
                    getWorkspace().setActiveLeg(l);
                    startActivity(intent);
                });

                currLegFlag = activeLeg.getId().equals(l.getId());
                if (currLegFlag) {
                    row.setBackgroundColor(Color.parseColor("#202020"));
                    activeRow = row;
                }

                if (mGalleryColors.get(l.getGalleryId(), Constants.NOT_FOUND) == Constants.NOT_FOUND) {
                    Gallery gallery = DaoUtil.getGallery(l.getGalleryId());
                    mGalleryColors.put(l.getGalleryId(), MapUtilities.getNextGalleryColor(mGalleryColors.size()));
                    mGalleryNames.put(l.getGalleryId(), gallery.getName());
                }

                Point fromPoint = l.getFromPoint();
                DaoUtil.refreshPoint(fromPoint);
                String fromPointString = fromPoint.getName();

                if (lastGalleryId == null) {
                    lastGalleryId = l.getGalleryId();
                }

                if (l.getGalleryId().equals(lastGalleryId)) {
                    fromPointString =  mGalleryNames.get(l.getGalleryId()) + fromPointString;
                    prevGalleryId = l.getGalleryId();
                } else {
                    prevGalleryId = DaoUtil.getLegByToPoint(fromPoint).getGalleryId();
                    fromPointString =  mGalleryNames.get(prevGalleryId) + fromPointString;
                }

                
                Point toPoint = l.getToPoint();
                DaoUtil.refreshPoint(toPoint);
                String toPointString = mGalleryNames.get(l.getGalleryId()) + toPoint.getName();

                lastGalleryId = l.getGalleryId();
                
                if (l.isMiddle()) {
                    row.addView(createTextView("", currLegFlag, false, mGalleryColors.get(prevGalleryId)));
                    row.addView(createTextView("", currLegFlag, false, mGalleryColors.get(l.getGalleryId())));
                    row.addView(createTextView(Constants.MIDDLE_POINT_DELIMITER + StringUtils.floatToLabel(l.getMiddlePointDistance()), currLegFlag, true));
                } else {
                    row.addView(createTextView(fromPointString, currLegFlag, false, mGalleryColors.get(prevGalleryId)));
                    row.addView(createTextView(toPointString, currLegFlag, false, mGalleryColors.get(l.getGalleryId())));
                    row.addView(createTextView(l.getDistance(), currLegFlag, true));
                    row.addView(createTextView(l.getAzimuth(), currLegFlag, true));
                    row.addView(createTextView(l.getSlope(), currLegFlag, true));
                }

                StringBuilder moreText = new StringBuilder();
                
                if (!l.isMiddle()) {
                    Sketch sketch = DaoUtil.getScetchByLeg(l);
                    if (sketch != null){
                        moreText.append(sketchPrefix);
                    }
                    Note note = DaoUtil.getActiveLegNote(l);
                    if (note != null){
                        moreText.append(notePrefix);
                    }
                    Photo photo = DaoUtil.getPhotoByLeg(l);
                    if (photo != null) {
                        moreText.append(photoPrefix);
                    }
                    Location location = DaoUtil.getLocationByPoint(fromPoint);
                    if (location != null){
                        moreText.append(locationPrefix);
                    }
                    if (DaoUtil.hasVectorsByLeg(l)) {
                        moreText.append(vectorsPrefix);
                    }
                }

                // reset the text appearance to small
                TextView view = createTextView(moreText.toString(), currLegFlag, true);
                view.setTextAppearance(this, android.R.style.TextAppearance_Small);
                row.addView(view);
                table.addView(row, params);
            }

            table.invalidate();

            // scroll to the active leg
            final ScrollView sv = findViewById(R.id.mainTableScroll);
            final TableRow activeRowFinal = activeRow;
            sv.post(() -> sv.scrollTo(0, activeRowFinal.getTop()));

        } catch (Exception e) {
            Log.e(Constants.LOG_TAG_UI, "Failed to render survey activity", e);
            UIUtilities.showNotification(R.string.error);
        }
    }

    private TextView createTextView(Float aMeasure, boolean isCurrentLeg, boolean allowEditing) {
        return createTextView(StringUtils.floatToLabel(aMeasure), isCurrentLeg, allowEditing);
    }
    
    private TextView createTextView(String aText, boolean isCurrentLeg, boolean allowEditing) {
        TextView edit = new TextView(this);
        edit.setLines(1);
        if (aText != null) {
            edit.setText(aText);
        }
        edit.setGravity(Gravity.CENTER);

        if (!isCurrentLeg || !allowEditing) {
            edit.setEnabled(false);
        }

        //set text appearance as medium
        edit.setTextAppearance(this, android.R.style.TextAppearance_Medium);
        return edit;
    }
    
    private TextView createTextView(String aText, boolean isCurrentLeg, boolean allowEditing, int aColor) {
    	TextView edit = createTextView(aText, isCurrentLeg, allowEditing);
    	edit.setTextColor(aColor);
    	return edit;
    }

    public void addButtonClick() {

        try {
            Leg activeLeg = Workspace.getCurrentInstance().getActiveLeg();
            if (activeLeg.getDistance() == null) {
                Log.i(Constants.LOG_TAG_UI, "Go straight to the first leg, unable to create next");
                UIUtilities.showNotification(R.string.leg_not_complete);
                Intent intent = new Intent(this, PointActivity.class);
                intent.putExtra(Constants.LEG_SELECTED, activeLeg.getId());
                Workspace.getCurrentInstance().setActiveLeg(activeLeg);
                startActivity(intent);
                return;
            }


            Log.i(Constants.LOG_TAG_UI, "Adding");
            AddNewDialog addNewDialog = new AddNewDialog();
            addNewDialog.show(getSupportFragmentManager(), AddNewDialog.ADD_NEW_DIALOG);

        } catch (Exception e) {
            Log.e(Constants.LOG_TAG_UI, "Error adding", e);
            UIUtilities.showNotification(R.string.error);
        }
    }

    /**
     * Handles the selected element for AddNewDialog
     *
     * @param itemArg item selected
     */
    public void addNewSelected(int itemArg){

        // dismiss the dialog
        Fragment prev = getSupportFragmentManager().findFragmentByTag(AddNewDialog.ADD_NEW_DIALOG);
        if (prev != null) {
            DialogFragment df = (DialogFragment) prev;
            df.dismiss();
        }

        try {
            if (main_add_leg == itemArg) {
                // next leg
                addLeg();
            } else if (main_add_gallery == itemArg) {
                // next gallery
                Leg prevLeg = DaoUtil.getLegByToPoint(Workspace.getCurrentInstance().getActiveLeg().getFromPoint());
                Gallery currGallery = Workspace.getCurrentInstance().getActiveGallery();
                if (prevLeg == null && !GEOLOCATION.equals(currGallery.getType())) {
                    // not supported
                    UIUtilities.showNotification(R.string.leg_not_complete);
                } else {
                    // next gallery
                    Intent intent = new Intent(SurveyMainActivity.this, NewGalleryActivity.class);
                    startActivity(intent);
                }
            } else if (main_add_middlepoint == itemArg) {
                // middle point
                requestLengthAndAddMiddle();
            }
        } catch (Exception e) {
            Log.e(Constants.LOG_TAG_UI, "Error adding", e);
            UIUtilities.showNotification(R.string.error);
        }
    }

    private void requestLengthAndAddMiddle() {
        new MiddlePointDialog().show(getSupportFragmentManager(), "middle_point_dialog");
    }

    private void addLeg() {
        Log.i(Constants.LOG_TAG_UI, "Creating leg");

        Intent intent = new Intent(SurveyMainActivity.this, PointActivity.class);
        startActivity(intent);
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

    public void infoButton() {
        Intent intent = new Intent(this, InfoActivity.class);
        startActivity(intent);
    }

    public void changeButton() {

        Log.i(Constants.LOG_TAG_UI, "Change active leg");

        // show choose leg dialog where to choose the active leg
        ChangeLegDialog changeLedDialog = new ChangeLegDialog();
        changeLedDialog.show(getSupportFragmentManager(), ChangeLegDialog.CHANGE_LEG);
    }

    @Override
    public void onBackPressed() {
        finish();
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

	/**
	 * Add project's name as a title
	 * 
	 * @see com.astoev.cave.survey.activity.BaseActivity#getScreenTitle()
	 */
	@Override
	protected String getScreenTitle() {
    	// set the name of the chosen project as title in the action bar
        Project activeProject = getWorkspace().getActiveProject();
        if (activeProject != null){
        	return activeProject.getName();
        }
        return null;
	}
    
}