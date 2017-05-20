package iot.mqtt;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import static android.content.ContentValues.TAG;

public class MainActivity extends Activity implements MqttCallback {

    private static final String TAG = "mqtt";
    MqttClient client;
    TextView msg;
    private Handler mHandler;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        try {
            msg = (TextView) findViewById(R.id.textView);
            String clientId = MqttClient.generateClientId();
            client = new MqttClient("tcp://iot.eclipse.org:1883", clientId, new MemoryPersistence());
            client.setCallback(this);
            client.connect();
            if (client.isConnected()) {
                Log.d(TAG, "connected");
            }

            String topic = "topic/sub";
            //for(int i=0;i<100;i++)
            client.subscribe(topic);
            //for(int i=0;i<10;i++)
            publishMessage();
            this.mHandler = new Handler();

            this.mHandler.postDelayed(m_Runnable, 5000);

        }
         catch (MqttException e) {
            e.printStackTrace();
        }
    }



    private final Runnable m_Runnable = new Runnable()
    {
        public void run()

        {
            String topic = "topic/sub";
            //for(int i=0;i<100;i++)
            try {
                client.subscribe(topic);
            } catch (MqttException e) {
                e.printStackTrace();
            }
            //Toast.makeText(MainActivity.this,"in runnable",Toast.LENGTH_SHORT).show();
            //task = new HttpAsyncTask1().execute("http://ec2-52-35-198-120.us-west-2.compute.amazonaws.com:3000/sensorData");
            //task2 = new HttpAsyncTask2().execute("http://ec2-52-35-198-120.us-west-2.compute.amazonaws.com:3000/health");
            MainActivity.this.mHandler.postDelayed(m_Runnable, 5000);
        }


    };//runnable


    public void publishMessage() throws MqttException {
        try {
            MqttMessage message = new MqttMessage();
            message.setPayload("hi".getBytes());
            //client.connect();
            client.publish("topic/pub", message);
        } catch (MqttException e) {
            System.err.println("Error Publishing: " + e.getMessage());
            e.printStackTrace();
        }
    }
    @Override
    public void connectionLost(Throwable cause) {
        Log.d(TAG, "connectionLost....");
    }

    @Override
    public void messageArrived(String topic, MqttMessage message) throws Exception {
        String payload = new String(message.getPayload());
        Log.d(TAG, "msg arrived" + payload);
        msg.setText("This is "+payload);
        //Toast.makeText(MainActivity.this,"payload",Toast.LENGTH_LONG).show();
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
        Log.d(TAG, "deliveryComplete....");
    }
}
