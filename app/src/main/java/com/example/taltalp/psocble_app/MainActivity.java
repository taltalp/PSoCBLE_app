package com.example.taltalp.psocble_app;

import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.RemoteException;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.WindowManager;
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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Collection;
import java.util.Vector;

public class MainActivity extends AppCompatActivity implements BeaconConsumer {

    private static final String IBEACON_FORMAT = "m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24";
    private static final String UUID =    "00050001-0000-1000-8000-00805F9B0131";

    private BeaconManager beaconManager;
    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 1;

    private void myLog(String msg){
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    LineChart tempChart, lumiChart;
    Vector connectedMajorVector;
    int x;
    int[] Colors = new int[]{Color.RED, Color.BLUE, Color.CYAN, Color.GREEN, Color.MAGENTA, Color.YELLOW};

    private BufferedWriter bw;
    private DemoConnector demoConnector;

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
        tempChart.getXAxis().setGranularity(1f);
        tempChart.setData(new LineData());

        lumiChart =(LineChart)findViewById(R.id.lumiLineChart);
        lumiChart.setDescription("luminance");
        lumiChart.getXAxis().setGranularity(1f);
        lumiChart.setData(new LineData());

        connectedMajorVector = new Vector();
        x = 0;

        if(isExternalStorageWritable()){
            String fileName = "log.csv";
            String text = "time,major,temp,lumi\n";
            String filePath = "/sdcard/" + fileName;
            File file = new File(filePath);
            Log.d("beacon", filePath);

            try {
                FileOutputStream fileOutputStream = new FileOutputStream(file, true);
                OutputStreamWriter outputStreamWriter = new OutputStreamWriter(fileOutputStream, "UTF-8");
                bw = new BufferedWriter(outputStreamWriter);
                bw.write(text);
                bw.flush();
            } catch(Exception e){
                e.printStackTrace();
            }
        }

        demoConnector = new DemoConnector();
        demoConnector.InitConnection();
        demoConnector.Connect();

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
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
                    Log.d("BeaconValue", "UUID:" + beacon.getId1() + ", major:" + beacon.getId2()
                            + ", minor:" + beacon.getId3().toHexString() + ", Distance:" + beacon.getDistance()
                            + ",RSSI" + beacon.getRssi());

                    // iBeacon Major ID
                    int major = beacon.getId2().toInt();
                    int temp = beacon.getId3().toInt() & 0xff;
                    int lumi = beacon.getId3().toInt() >> 8;
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

                    tempData.addEntry(new Entry(x, temp), index);
                    tempData.notifyDataChanged();
                    tempChart.notifyDataSetChanged();
                    tempChart.setVisibleXRangeMaximum(50);
                    tempChart.moveViewToX(tempData.getEntryCount());

                    lumiData.addEntry(new Entry(x, lumi), index);
                    lumiData.notifyDataChanged();
                    lumiChart.notifyDataSetChanged();
                    lumiChart.setVisibleXRangeMaximum(50);
                    lumiChart.moveViewToX(lumiData.getEntryCount());

                    x++;

                    demoConnector.SendIllumination(major, lumi);

                    try {
                        bw.write(beacon.getId2().toString() + "," + Integer.toString(temp) + "," + Integer.toString(lumi) + "\n");
                        bw.flush();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

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

    /* Checks if external storage is available for read and write */
    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }

    /* Checks if external storage is available to at least read */
    public boolean isExternalStorageReadable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state) ||
                Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
            return true;
        }
        return false;
    }

}
