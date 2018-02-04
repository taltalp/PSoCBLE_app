package com.example.taltalp.psocble_app;

import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.RemoteException;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconConsumer;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.Identifier;
import org.altbeacon.beacon.MonitorNotifier;
import org.altbeacon.beacon.RangeNotifier;
import org.altbeacon.beacon.Region;

import java.util.Collection;
import java.util.Vector;

public class MainActivity extends AppCompatActivity implements BeaconConsumer {

    private static final String IBEACON_FORMAT = "m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24";
    // private static final String UUID = "74A23A96-A479-4330-AEFF-2421B6CF443C";
    private static final String UUID =    "00050001-0000-1000-8000-00805F9B0131";

    private BeaconManager beaconManager;
    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 1;

    private void myLog(String msg){
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    LineChart tempChart, lumiChart;
    Vector connectedMajorVector;

    int[] Colors = new int[]{Color.RED, Color.BLUE, Color.CYAN, Color.GREEN, Color.MAGENTA, Color.YELLOW};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (this.checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{android.Manifest.permission.ACCESS_COARSE_LOCATION},PERMISSION_REQUEST_COARSE_LOCATION);
            }
        }

        beaconManager = BeaconManager.getInstanceForApplication(this);
        beaconManager.getBeaconParsers().add(new BeaconParser().setBeaconLayout(IBEACON_FORMAT));

        tempChart =(LineChart)findViewById(R.id.tempLineChart);
        tempChart.setDescription("temperature");
        tempChart.setData(new LineData());

        lumiChart =(LineChart)findViewById(R.id.lumiLineChart);
        lumiChart.setDescription("luminance");
        lumiChart.setData(new LineData());

        connectedMajorVector = new Vector();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // サービスの開始
        beaconManager.bind(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        // サービスの停止
        beaconManager.unbind(this);
    }

    @Override
    public void onBeaconServiceConnect() {
        Identifier uuid = Identifier.parse(UUID);
        final Region mRegion = new Region("mybeacon", uuid, null, null);

        beaconManager.addMonitorNotifier(new MonitorNotifier() {
            @Override
            public void didEnterRegion(Region region) {
                // 領域侵入
                myLog("Area Intrusion!");
                try {
                    beaconManager.startRangingBeaconsInRegion(mRegion);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void didExitRegion(Region region) {
                // 領域退出
                myLog("Area Exit!");
                try {
                    beaconManager.stopRangingBeaconsInRegion(mRegion);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void didDetermineStateForRegion(int i, Region region) {
                // 領域に対する状態が変化
                myLog("Value Changed!");
            }
        });

        beaconManager.setRangeNotifier(new RangeNotifier() {
            @Override
            public void didRangeBeaconsInRegion(Collection<Beacon> beacons, Region region) {
                for (Beacon beacon : beacons) {
                    // ログの出力
                    Log.d("Beacon", "UUID:" + beacon.getId1() + ", major:" + beacon.getId2()
                            + ", minor:" + beacon.getId3().toHexString() + ", Distance:" + beacon.getDistance()
                            + ",RSSI" + beacon.getRssi());

                    // iBeacon Major ID
                    int major = beacon.getId2().toInt();
                    // Check each connection
                    int index = connectedMajorVector.indexOf(major);
                    if(index  == -1){
                        connectedMajorVector.addElement(major);
                        index = connectedMajorVector.size() - 1;
                    }

                    LineData tempData = tempChart.getLineData();
                    LineData lumiData = lumiChart.getLineData();
                    ILineDataSet tempDataSet = tempData.getDataSetByIndex(index);
                    if (tempDataSet == null){
                        tempDataSet = createSet(beacon.getId2().toString(), Colors[index%6]);
                        tempData.addDataSet(tempDataSet);
                    }

                    ILineDataSet lumiDataSet = lumiData.getDataSetByIndex(index);
                    if (lumiDataSet == null){
                        lumiDataSet = createSet(beacon.getId2().toString(), Colors[index%6]);
                        lumiData.addDataSet(lumiDataSet);
                    }

                    tempData.addEntry(new Entry(tempDataSet.getEntryCount(), beacon.getId3().toInt() >> 8), index);
                    tempData.notifyDataChanged();
                    tempChart.notifyDataSetChanged();
                    tempChart.setVisibleXRangeMaximum(50);
                    tempChart.moveViewToX(tempData.getEntryCount());

                    lumiData.addEntry(new Entry(lumiDataSet.getEntryCount(), beacon.getId3().toInt() & 0xff), index);
                    lumiData.notifyDataChanged();
                    lumiChart.notifyDataSetChanged();
                    lumiChart.setVisibleXRangeMaximum(50);
                    lumiChart.moveViewToX(lumiData.getEntryCount());
                }
            }
        });

        try {
            beaconManager.startMonitoringBeaconsInRegion(mRegion);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    private LineDataSet createSet(String label, int color){
        LineDataSet set = new LineDataSet(null, label);
        set.setLineWidth(2.5f);
        set.setColor(color);
        set.setDrawCircles(false);
        set.setDrawValues(false);

        return set;
    }

}
