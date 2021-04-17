package com.astoev.cave.survey.activity.map;

import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ToggleButton;
import android.widget.ZoomControls;

import com.astoev.cave.survey.R;
import com.astoev.cave.survey.activity.MainMenuActivity;
import com.astoev.cave.survey.activity.draw.DrawingActivity;

/**
 * Created by IntelliJ IDEA.
 * User: astoev
 * Date: 1/23/12
 * Time: 3:04 PM
 * To change this template use File | Settings | File Templates.
 */
public class MapActivity extends MainMenuActivity implements View.OnTouchListener {

    private MapView map;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.map);
        map = findViewById(R.id.mapSurface);
        map.setOnTouchListener(this);

        getSupportActionBar().hide();

        final ZoomControls zoom = findViewById(R.id.mapZoom);
        zoom.setOnZoomInClickListener(aView -> {
            map.zoomIn();
            enableDisableZoomControls(zoom);
        });
        zoom.setOnZoomOutClickListener(aView -> {
            map.zoomOut();
            enableDisableZoomControls(zoom);
        });

        final ToggleButton viewSelector = findViewById(R.id.mapHorizonatalToggle);
        viewSelector.setOnCheckedChangeListener((buttonView, isChecked) -> {
            map.setHorizontalPlan(!isChecked);
            enableDisableZoomControls(zoom);
        });
    }

    @Override
    public boolean onTouch(View aView, MotionEvent aMotionEvent) {

        if (aMotionEvent.getAction() == MotionEvent.ACTION_DOWN) {
            map.resetMove(aMotionEvent.getX(), aMotionEvent.getY());
        } else if (aMotionEvent.getAction() == MotionEvent.ACTION_MOVE) {
            map.move(aMotionEvent.getX(), aMotionEvent.getY());
        }

        return true;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        map = findViewById(R.id.mapSurface);
        map.setOnTouchListener(this);
    }

    public void annotateMap(View aView) {
        Intent intent = new Intent(this, DrawingActivity.class);
        intent.putExtra(DrawingActivity.SKETCH_BASE, map.getPngDump());
        intent.putExtra(DrawingActivity.MAP_FLAG, true);
        startActivity(intent);
    }

    private void enableDisableZoomControls(ZoomControls zoom) {
        zoom.setIsZoomOutEnabled(map.canZoomOut());
        zoom.setIsZoomInEnabled(map.canZoomIn());
    }
}