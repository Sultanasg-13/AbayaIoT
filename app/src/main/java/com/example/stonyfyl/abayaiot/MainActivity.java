package com.example.stonyfyl.abayaiot;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import com.microsoft.band.BandClient;
import com.microsoft.band.BandClient;
import com.microsoft.band.BandClientManager;
import com.microsoft.band.BandException;
import com.microsoft.band.BandIOException;
import com.microsoft.band.BandInfo;
import com.microsoft.band.ConnectionState;
import com.microsoft.band.InvalidBandVersionException;
import com.microsoft.band.sensors.BandHeartRateEvent;
import com.microsoft.band.sensors.BandHeartRateEventListener;
import com.microsoft.band.sensors.BandPedometerEvent;
import com.microsoft.band.sensors.BandPedometerEventListener;
import com.microsoft.band.sensors.HeartRateConsentListener;
import com.microsoft.band.sensors.BandSkinTemperatureEvent;
import com.microsoft.band.sensors.BandSkinTemperatureEventListener;
import com.microsoft.band.sensors.BandUVEvent;
import com.microsoft.band.sensors.BandUVEventListener;
import com.microsoft.band.sensors.BandAccelerometerEvent;
import com.microsoft.band.sensors.BandAccelerometerEventListener;
import com.microsoft.band.UserConsent;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

import android.widget.Button;
import android.widget.TextView;
import java.lang.ref.WeakReference;

public class MainActivity extends Activity {

    private BandClient client = null;
    private Button sButton, btnConsent;
    private TextView textStatus, stepView, heartView, uvView, tempView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textStatus = (TextView) findViewById(R.id.textView);
        stepView = (TextView) findViewById(R.id.stepView);
        heartView = (TextView) findViewById(R.id.heartView);
        uvView = (TextView) findViewById(R.id.uvView);
        tempView = (TextView) findViewById(R.id.tempView);
        sButton = (Button) findViewById(R.id.startButton);

        sButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                textStatus.setText("");
                new HeartRateSubscriptionTask().execute();

            }
        });
        final WeakReference<Activity> reference = new WeakReference<Activity>(this);

        btnConsent = (Button) findViewById(R.id.btnConsent);
        btnConsent.setOnClickListener(new OnClickListener() {
            @SuppressWarnings("unchecked")
            @Override
            public void onClick(View v) {
                new HeartRateConsentTask().execute(reference);
            }
        });
    }

    private BandPedometerEventListener mBandPedometerEventListener = new BandPedometerEventListener() {
        @Override
        public void onBandPedometerChanged(BandPedometerEvent bandPedometerEvent) {
            if(bandPedometerEvent != null){
                try {
                    appendToUI(String.format("Total Steps= %f steps\n"
                            + "Steps Today = %f\n", bandPedometerEvent.getTotalSteps()*1.0, bandPedometerEvent.getStepsToday()*1.0),stepView );
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    };


    private BandHeartRateEventListener mHeartRateEventListener = new BandHeartRateEventListener() {
        @Override
        public void onBandHeartRateChanged(final BandHeartRateEvent event) {
            if (event != null) {
                appendToUI(String.format("Heart Rate = %d beats per minute\n"
                        + "Quality = %s\n", event.getHeartRate(), event.getQuality()), heartView);
            }
        }
    };

    private BandUVEventListener mUVEventListener = new BandUVEventListener() {
        @Override
        public void onBandUVChanged(BandUVEvent bandUVEvent) {
            if(bandUVEvent != null){
                try {
                    appendToUI(String.format("UV Index Level = %S\n"
                            + "UV Exposure = %d\n", bandUVEvent.getUVIndexLevel(), bandUVEvent.getUVExposureToday()), uvView);
                } catch (InvalidBandVersionException e) {
                    e.printStackTrace();
                }
            }
        }
    };


    private BandSkinTemperatureEventListener mSkinTemperatureEventListener = new BandSkinTemperatureEventListener() {
        @Override
        public void onBandSkinTemperatureChanged(BandSkinTemperatureEvent bandSkinTemperatureEvent) {
            if(bandSkinTemperatureEvent != null){
                try{

                appendToUI(String.format("%.2f ºF\n", bandSkinTemperatureEvent.getTemperature()*1.800 + 32.00), tempView);
                }
                catch(Exception e){
                    e.printStackTrace();
                }
            }
        }
    };

    private class HeartRateSubscriptionTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... params) {
            try {
                if (getConnectedBandClient()) {
                    if (client.getSensorManager().getCurrentHeartRateConsent() == UserConsent.GRANTED) {
                        client.getSensorManager().registerHeartRateEventListener(mHeartRateEventListener);
                        client.getSensorManager().registerUVEventListener(mUVEventListener);
                        client.getSensorManager().registerSkinTemperatureEventListener(mSkinTemperatureEventListener);
                        client.getSensorManager().registerPedometerEventListener(mBandPedometerEventListener);
                    } else {
                        boolean x = client.getSensorManager().getCurrentHeartRateConsent() == UserConsent.DECLINED;
                        appendToUI("You have not given this application consent to access heart rate data yet."
                                + " Please press the Heart Rate Consent button.\n"+ "what is you?"+ x,textStatus);
                    }
                } else {
                    appendToUI("Band isn't connected. Please make sure bluetooth is on and the band is in range.\n",textStatus);
                }
            } catch (BandException e) {
                String exceptionMessage="";
                switch (e.getErrorType()) {
                    case UNSUPPORTED_SDK_VERSION_ERROR:
                        exceptionMessage = "Microsoft Health BandService doesn't support your SDK Version. Please update to latest SDK.\n";
                        break;
                    case SERVICE_ERROR:
                        exceptionMessage = "Microsoft Health BandService is not available. Please make sure Microsoft Health is installed and that you have the correct permissions.\n";
                        break;
                    default:
                        exceptionMessage = "Unknown error occured: " + e.getMessage() + "\n";
                        break;
                }
                appendToUI(exceptionMessage,textStatus);

            } catch (Exception e) {
                appendToUI(e.getMessage(),textStatus);
            }
            return null;
        }
    }

    private class HeartRateConsentTask extends AsyncTask<WeakReference<Activity>, Void, Void> {
        @Override
        protected Void doInBackground(WeakReference<Activity>... params) {
            try {
                if (getConnectedBandClient()) {
                    if (params[0].get() != null) {
                        client.getSensorManager().requestHeartRateConsent(params[0].get(), new HeartRateConsentListener() {
                            @Override
                            public void userAccepted(boolean consentGiven){
                            }
                        });
                    }
                } else {
                    appendToUI("Band isn't connected. Please make sure bluetooth is on and the band is in range.\n",textStatus);
                }
            } catch (BandException e) {
                String exceptionMessage="";
                switch (e.getErrorType()) {
                    case UNSUPPORTED_SDK_VERSION_ERROR:
                        exceptionMessage = "Microsoft Health BandService doesn't support your SDK Version. Please update to latest SDK.\n";
                        break;
                    case SERVICE_ERROR:
                        exceptionMessage = "Microsoft Health BandService is not available. Please make sure Microsoft Health is installed and that you have the correct permissions.\n";
                        break;
                    default:
                        exceptionMessage = "Unknown error occured: " + e.getMessage() + "\n";
                        break;
                }
                appendToUI(exceptionMessage, textStatus);

            } catch (Exception e) {
                appendToUI(e.getMessage(), textStatus);
            }
            appendToUI("Return Null\n",textStatus);
            return null;
        }
    }

    private boolean getConnectedBandClient() throws InterruptedException, BandException {
        if (client == null) {
            BandInfo[] devices = BandClientManager.getInstance().getPairedBands();
            if (devices.length == 0) {
                appendToUI("Band isn't paired with your phone.\n", textStatus);
                return false;
            }
            client = BandClientManager.getInstance().create(getBaseContext(), devices[0]);
            appendToUI("Device\n"+ client,textStatus);
        } else if (ConnectionState.CONNECTED == client.getConnectionState()) {
            return true;
        }
        //appendToUI("Band is connecting...\n",textStatus);
        return ConnectionState.CONNECTED == client.connect().await();
    }

    private void appendToUI(final String string, final TextView v) {
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                v.setText(string);
            }
        });
    }
}


