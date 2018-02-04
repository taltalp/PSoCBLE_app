package com.example.taltalp.psocble_app;

import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.RemoteException;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.TextView;
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

    TextView textView;
    LineChart mChart, mChart2;
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

        textView = (TextView)findViewById(R.id.textView);
        textView.setText("aaa");

        mChart =(LineChart)findViewById(R.id.mLineChart);
        mChart.setDescription("adc1");
        mChart.setData(new LineData());

        mChart2 =(LineChart)findViewById(R.id.mLineChart2);
        mChart2.setDescription("adc0");
        mChart2.setData(new LineData());

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

                    int major = beacon.getId2().toInt();
                    int index = connectedMajorVector.indexOf(major);
                    if(index  == -1){
                        connectedMajorVector.addElement(major);
                        index = connectedMajorVector.size() - 1;
                    }
                    // int major = 0;
                    LineData data = mChart.getLineData();
                    LineData data2 = mChart2.getLineData();
                    ILineDataSet set = data.getDataSetByIndex(index);
                    if (set == null){
                        set = createSet(beacon.getId2().toString(), Colors[index%6]);
                        data.addDataSet(set);
                    }

                    ILineDataSet set2 = data2.getDataSetByIndex(index);
                    if (set2 == null){
                        set2 = createSet(beacon.getId2().toString(), Colors[index%6]);
                        data2.addDataSet(set2);
                    }
                    data.addEntry(new Entry(set.getEntryCount(), beacon.getId3().toInt() >> 8), index);
                    data2.addEntry(new Entry(set2.getEntryCount(), beacon.getId3().toInt() & 0xff), index);
                    data.notifyDataChanged();
                    data2.notifyDataChanged();
                    mChart.notifyDataSetChanged();
                    mChart.setVisibleXRangeMaximum(50);
                    mChart.moveViewToX(data.getEntryCount());

                    mChart2.notifyDataSetChanged();
                    mChart2.setVisibleXRangeMaximum(50);
                    mChart2.moveViewToX(data2.getEntryCount());
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
