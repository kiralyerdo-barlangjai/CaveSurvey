package com.astoev.cave.survey.activity.main;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.util.Pair;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import com.astoev.cave.survey.R;
import com.astoev.cave.survey.activity.MainMenuActivity;
import com.astoev.cave.survey.activity.UIUtilities;
import com.astoev.cave.survey.service.bluetooth.BluetoothService;
import com.astoev.cave.survey.service.bluetooth.device.AbstractBluetoothDevice;
import com.astoev.cave.survey.util.ConfigUtil;
import com.astoev.cave.survey.util.StringUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.astoev.cave.survey.Constants.LOG_TAG_UI;

/**
 * Created with IntelliJ IDEA.
 * User: astoev
 * Date: 10/23/13
 * Time: 10:31 AM
 * To change this template use File | Settings | File Templates.
 */
public class BTActivity extends MainMenuActivity implements Refresheable {

    private AbstractBluetoothDevice deviceFilter;
    private Set<Pair<String, String>> devices = new HashSet<>();

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.bluetooth);

        BluetoothService.registerListeners(this);
        prepareUI();
    }

    private void prepareUI() {
        try {
            // BT disabled?
            if (!BluetoothService.askBluetoothOn(this)) {
                Log.i(LOG_TAG_UI, "BT disabled");
                UIUtilities.showNotification(R.string.bt_not_on);
                finish();
                return;
            }

            displaySupportedDevices();

            Spinner devicesChooser = findViewById(R.id.bt_devices);
            devicesChooser.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    selectDevice();
                    refreshDevicesList();
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {

                }
            });

            refreshDevicesList();

        } catch (Exception e) {
            Log.e(LOG_TAG_UI, "Failed during create", e);
            UIUtilities.showNotification(R.string.error);
        }
    }

    public void showDevicesHelp(View aView) {
        Log.d(LOG_TAG_UI, "Displaying the devices help");
        Intent i = new Intent(Intent.ACTION_VIEW);
        String url  = getString(R.string.bt_devices_help_url);
        i.setData(Uri.parse(url));
        startActivity(i);
    }

    private void displaySupportedDevices() {

        List<SupportedDevice> supportedDevices = new ArrayList<>();
        supportedDevices.add(new SupportedDevice(null));
        for (AbstractBluetoothDevice device : BluetoothService.getSupportedDevices()) {
            supportedDevices.add(new SupportedDevice(device));
        }

        ArrayAdapter<SupportedDevice> spinnerArrayAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, supportedDevices);
        spinnerArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        Spinner devicesChooser = findViewById(R.id.bt_supportd_devices);
        devicesChooser.setAdapter(spinnerArrayAdapter);

        devicesChooser.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

                // mark selection
                SupportedDevice device = (SupportedDevice) devicesChooser.getSelectedItem();
                deviceFilter = device.device;

                // need new LE discovery
                BluetoothService.stop();
                BluetoothService.discoverBluetoothLEDevices(deviceFilter);

                // filter the active devices
                refreshDevicesList();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        deviceFilter = null;
    }

    @Override
    protected void onResume() {
        super.onResume();

        BluetoothService.registerListeners(this);

        BluetoothService.stop();
        BluetoothService.discoverBluetoothLEDevices(deviceFilter);

        prepareUI();
    }

    @Override
    protected void onRestart() {
        super.onRestart();

        prepareUI();
        BluetoothService.registerListeners(this);
    }

    /**
     * @see com.astoev.cave.survey.activity.BaseActivity#getScreenTitle()
     */
    @Override
    protected String getScreenTitle() {
        return getString(R.string.bt_devices);
    }

    private void refreshDevicesList() {

        devices = BluetoothService.getPairedCompatibleDevices(deviceFilter);

        String selectedDeviceName = null;
        String selectedBtDeviceAddress = ConfigUtil.getStringProperty(ConfigUtil.PROP_CURR_BT_DEVICE_ADDRESS);
        if (StringUtils.isNotEmpty(selectedBtDeviceAddress)) {
            String selectedBtDeviceName = ConfigUtil.getStringProperty(ConfigUtil.PROP_CURR_BT_DEVICE_NAME);
            selectedDeviceName = buildDeviceName(new Pair<>(selectedBtDeviceName, selectedBtDeviceAddress));
        }

        List<String> devicesList = new ArrayList<>();
        int index = 0;
        int selectedDeviceIndex = -1;
        String tempName;
        for (final Pair<String, String> device : devices) {
            tempName = buildDeviceName(device);
            devicesList.add(tempName);
            if (tempName.equals(selectedDeviceName)) {
                selectedDeviceIndex = index;
            }
            index++;
        }

        ArrayAdapter<String> spinnerArrayAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, devicesList);
        spinnerArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        Spinner devicesChooser = findViewById(R.id.bt_devices);
        devicesChooser.setAdapter(spinnerArrayAdapter);
        if (selectedDeviceIndex >= 0) {
            devicesChooser.setSelection(selectedDeviceIndex);
        }

        updateDeviceStatus();
    }

    private void updateDeviceStatus() {
        // display status
        TextView status = findViewById(R.id.bt_status);
        status.setText(BluetoothService.getCurrDeviceStatusLabel(this));
    }

    public void pairNewDevice() {
        Intent intentOpenBluetoothSettings = new Intent();
        intentOpenBluetoothSettings.setAction(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS);
        startActivity(intentOpenBluetoothSettings);
    }

    private String buildDeviceName(Pair<String, String> aDevice) {
        return aDevice.first + " : " + aDevice.second;
    }

    public void selectDevice() {
        // get selected
        Spinner devicesChooser = findViewById(R.id.bt_devices);
        Pair<String, String> device = new ArrayList<>(devices).get(devicesChooser.getSelectedItemPosition());
        Log.i(LOG_TAG_UI, "Try to use " + device.first + ":" + device.second);

        UIUtilities.showNotification(R.string.bt_device_connecting, device.first);

        // store & propagate
        ConfigUtil.setStringProperty(ConfigUtil.PROP_CURR_BT_DEVICE_NAME, device.first);
        ConfigUtil.setStringProperty(ConfigUtil.PROP_CURR_BT_DEVICE_ADDRESS, device.second);
        BluetoothService.selectDevice(device.second, deviceFilter);
    }

    /**
     * @see com.astoev.cave.survey.activity.MainMenuActivity#getChildsOptionsMenu()
     */
    @Override
    protected int getChildsOptionsMenu() {
        return R.menu.btmenu;
    }

    /**
     * @see com.astoev.cave.survey.activity.MainMenuActivity#onOptionsItemSelected(android.view.MenuItem)
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.bt_new: {
                pairNewDevice();
                return true;
            }

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onPause() {
        // Unregister since the activity is not visible
        BluetoothService.unregisterListeners(this);
        if (BluetoothService.isBluetoothLESupported()) {
            BluetoothService.stopDiscoverBluetoothLEDevices();
        }
        super.onPause();
    }

    @Override
    public void refresh() {
        runOnUiThread(() -> updateDeviceStatus());
    }

    public static class SupportedDevice {
        public static final String NO_DEVICE_SELECTED = " --- ";
        AbstractBluetoothDevice device;

        public SupportedDevice(AbstractBluetoothDevice aDevice) {
            device = aDevice;
        }

        @Override
        public String toString() {
            return device != null ? device.getDescription() : NO_DEVICE_SELECTED;
        }
    }
}
