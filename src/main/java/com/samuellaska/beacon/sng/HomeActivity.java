package com.samuellaska.beacon.sng;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.RemoteException;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.Interpolator;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import com.estimote.sdk.Beacon;
import com.estimote.sdk.BeaconManager;
import com.estimote.sdk.Region;
import com.estimote.sdk.Utils;
import com.estimote.sdk.utils.L;
import com.melnykov.fab.FloatingActionButton;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


public class HomeActivity extends Activity {

    private static final String TAG = "HOME";

    private static final int REQUEST_ENABLE_BT = 1234;
    private static final Region ALL_ESTIMOTE_BEACONS_REGION = new Region("rid", null, null, null);
    private static final int ART_DISTANCE = 2; // metres

    private BeaconManager beaconManager;
    private Beacon beacon;

    GridView gridView;
    private FloatingActionButton discover;
    private boolean buttonAnimated = false;


    // HARDCODED grid images & art connected to beacon
    private final String[] items = new String[]{
            "O_2",
            "O_3",
            "O_310",
            "O_314",
            "O_385",
            "O_893",
            "O_1236",
            "O_1590",
            "O_1591",
            "O_1592"};

    private static final HashMap<String,String> beaconsToArtworks = new HashMap<String,String>();
    static {
        beaconsToArtworks.put("F9:22:DD:37:4A:24", "O_1236");
//        beaconsToArtworks.put("C9:14:QR:75:4A:76", "O_2");
//        beaconsToArtworks.put("0G:22:HS:32:4A:26", "O_310");
//        beaconsToArtworks.put("UP:34:FS:12:2A:23", "O_1592");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.home);

        // Hide the status bar.
//        View decorView = getWindow().getDecorView();
//        int uiOptions = View.SYSTEM_UI_FLAG_FULLSCREEN;
//        decorView.setSystemUiVisibility(uiOptions);

        // Wire views
        discover = (FloatingActionButton)findViewById(R.id.discover);

        gridView = (GridView)findViewById(R.id.gridview);
        gridView.setAdapter(new MyAdapter(this));
        gridView.setOnItemClickListener(new AdapterView.OnItemClickListener()
        {
            public void onItemClick(AdapterView<?> parent, View v, int position, long id)
            {
                // this 'mActivity' parameter is Activity object, you can send the current activity.
                Intent i = new Intent(HomeActivity.this, DisplayArtworkActivity.class);
                i.putExtra("SNG_ID", items[position]);
                Log.i("XML", items[position]);
                startActivity(i);

            }
        });

        // Configure verbose debug logging.
        L.enableDebugLogging(false);

        // Configure BeaconManager.
        beaconManager = new BeaconManager(this);
        beaconManager.setRangingListener(new BeaconManager.RangingListener() {
            @Override
            public void onBeaconsDiscovered(Region region, final List<Beacon> beacons) {
                // *beacon ranging listener is running in background
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        beacon = null;
                        for (Beacon rangedBeacon : beacons) {
                            // compare distance
                            if (beacon == null || Utils.computeAccuracy(rangedBeacon) < Utils.computeAccuracy(beacon)) {
                                beacon = rangedBeacon;
                            }
                        }

                        if (beacon != null)
                            updateDistanceView(beacon);
                    }
                });
            }
        });
    }


    /** ACTION BAR MENU **/

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.home_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.runDebug) {
            // Push intent
            Intent intent = new Intent(HomeActivity.this, ListBeaconsActivity.class);
            intent.putExtra(ListBeaconsActivity.EXTRAS_TARGET_ACTIVITY, BeaconDebugActivity.class.getName());
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }


    /** PEEK BUTTON **/

    private void updateDistanceView(Beacon foundBeacon) {
        double distance = Math.min(Utils.computeAccuracy(foundBeacon), 6.0);
        if (distance > ART_DISTANCE) {
            if (buttonAnimated)
                animatePeekButton(true);
            buttonAnimated = false;
        } else {
            if (!buttonAnimated)
                animatePeekButton(false);
            buttonAnimated = true;
        }
    }

    public void displayPeekedArt(View view) {
        if (beacon != null && beaconsToArtworks.containsKey(beacon.getMacAddress())) {
            Intent i = new Intent(HomeActivity.this, DisplayArtworkActivity.class);
            i.putExtra("SNG_ID",beaconsToArtworks.get(beacon.getMacAddress()));
            startActivity(i);
        }

    }

    public void animatePeekButton(boolean reverse) {
        Animation a = AnimationUtils.loadAnimation(this, R.anim.slide_up);
        if (reverse) {
            a.setInterpolator(new ReverseInterpolator());
            a.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {
                }

                @Override
                public void onAnimationRepeat(Animation animation) {
                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    discover.setVisibility(View.INVISIBLE);
                }
            });
        } else {
            discover.setVisibility(View.VISIBLE);
        }

        discover.startAnimation(a);
    }

    // reverse animation sequence
    public class ReverseInterpolator implements Interpolator {
        @Override
        public float getInterpolation(float paramFloat) {
            return Math.abs(paramFloat -1f);
        }
    }


    /** HOME IMAGE GRID **/

    private class MyAdapter extends BaseAdapter
    {
        private List<Item> items = new ArrayList<Item>();
        private LayoutInflater inflater;

        public MyAdapter(Context context)
        {
            inflater = LayoutInflater.from(context);

            // API does not provide thumbnails, so they are included with app for now
            // displayed items format: O_{ID}, included in drawables
            items.add(new Item("O_2", R.drawable.o2));
            items.add(new Item("O_3", R.drawable.o3));
            items.add(new Item("O_310", R.drawable.o310));
            items.add(new Item("O_314", R.drawable.o314));
            items.add(new Item("O_385", R.drawable.o385));
            items.add(new Item("O_893", R.drawable.o893));
            items.add(new Item("O_1236", R.drawable.o1236));
            items.add(new Item("O_1590", R.drawable.o1590));
            items.add(new Item("O_1591", R.drawable.o1591));
            items.add(new Item("O_1592", R.drawable.o1592));
        }

        @Override
        public int getCount() {
            return items.size();
        }

        @Override
        public Object getItem(int i)
        {
            return items.get(i);
        }

        @Override
        public long getItemId(int i)
        {
            return items.get(i).drawableId;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup)
        {
            View v = view;
            ImageView picture;
            TextView name;

            if(v == null)
            {
                v = inflater.inflate(R.layout.gridview_item, viewGroup, false);
                v.setTag(R.id.picture, v.findViewById(R.id.picture));
                v.setTag(R.id.text, v.findViewById(R.id.text));
            }

            picture = (ImageView)v.getTag(R.id.picture);
            name = (TextView)v.getTag(R.id.text);

            Item item = (Item)getItem(i);

            picture.setImageResource(item.drawableId);
            name.setText(item.name);

            return v;
        }

        private class Item
        {
            final String name;
            final int drawableId;

            Item(String name, int drawableId)
            {
                this.name = name;
                this.drawableId = drawableId;
            }
        }
    }

    /** APP LIFECYCLE **/

    @Override
    protected void onDestroy() {
        beaconManager.disconnect();

        super.onDestroy();
    }

    @Override
    protected void onStart() {
        super.onStart();

        // Check if device supports Bluetooth Low Energy.
        if (!beaconManager.hasBluetooth()) {
            Toast.makeText(this, "Device does not have Bluetooth Low Energy", Toast.LENGTH_LONG).show();
            return;
        }

        // If Bluetooth is not enabled, let user enable it.
        if (!beaconManager.isBluetoothEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        } else {
            connectToService();
        }
    }

    @Override
    protected void onStop() {
        try {
            beaconManager.stopRanging(ALL_ESTIMOTE_BEACONS_REGION);
        } catch (RemoteException e) {
            Log.d(TAG, "Error while stopping ranging", e);
        }

        super.onStop();
    }

    // coming from outside of app
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == Activity.RESULT_OK) {
                connectToService();
            } else {
                Toast.makeText(this, "Bluetooth not enabled", Toast.LENGTH_LONG).show();
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }


    /** HELPERS **/

    // start bluetooth ranging
    private void connectToService() {
        beaconManager.connect(new BeaconManager.ServiceReadyCallback() {
            @Override
            public void onServiceReady() {
                try {
                    beaconManager.startRanging(ALL_ESTIMOTE_BEACONS_REGION);
                } catch (RemoteException e) {
                    Toast.makeText(getApplicationContext(), "Cannot start ranging, something terrible happened",
                            Toast.LENGTH_LONG).show();
                    Log.e(TAG, "Cannot start ranging", e);
                }
            }
        });
    }

    // smooth the input from bluetooth distance readings
    private float exponentialSmoothing( float input, float output, float alpha ) {
        if ( output == 0 )
            return input;
        output = output + alpha * (input - output);
        return output;
    }
}
