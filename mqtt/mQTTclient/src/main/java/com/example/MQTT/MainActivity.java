package com.example.MQTT;

import android.graphics.Color;
import android.support.v7.app.ActionBarActivity;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.ToggleButton;

import java.text.NumberFormat;
import java.text.ParseException;


public class MainActivity extends ActionBarActivity
{
	private Messenger service = null;
	private final Messenger serviceHandler = new Messenger(new ServiceHandler());
	private IntentFilter intentFilter = null;
	private PushReceiver pushReceiver;
    TextView msgSubsMotor,msgSubsMoisture,waterPercentText;
	Button subscribeButton;
	Animation anim;
	ProgressBar waterTankLevel;
	int waterPercentage=0;
	ToggleButton motorControlling;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

		msgSubsMotor = (TextView) findViewById(R.id.subsMsgMotor);
		msgSubsMoisture = (TextView) findViewById(R.id.subsMsgMoisture);
		waterPercentText=(TextView) findViewById(R.id.waterPercentageText);
		waterTankLevel=(ProgressBar)findViewById(R.id.waterLevelIndicator);

    	intentFilter = new IntentFilter();
    	intentFilter.addAction("com.example.MQTT.PushReceived");
    	pushReceiver = new PushReceiver();
    	registerReceiver(pushReceiver, intentFilter, null, null);
        
        startService(new Intent(this, MQTTservice.class));
		addSubscribeButtonListener();
		addPublishButtonListener();

		anim = new AlphaAnimation(0.0f, 1.0f);
		anim.setDuration(200); //You can manage the time of the blink with this parameter
		anim.setStartOffset(20);
		anim.setRepeatMode(Animation.REVERSE);
		anim.setRepeatCount(10);

	}
    
    @Override
	protected void onStart()
    {
    	super.onStart();
    	bindService(new Intent(this, MQTTservice.class), serviceConnection, 0);
    }
    
    @Override
 	protected void onStop()
     {
    	super.onStop();
    	unbindService(serviceConnection);
     }
    
	 @Override
	protected void onResume()
	{
		 super.onResume();
		registerReceiver(pushReceiver, intentFilter);
	}
	
	@Override
	protected void onPause()
	{
		super.onPause();
		unregisterReceiver(pushReceiver);
	}
		
	public class PushReceiver extends BroadcastReceiver
	{
		@Override
		public void onReceive(Context context, Intent i)
		{
			String topic = i.getStringExtra(MQTTservice.TOPIC);
			String message = i.getStringExtra(MQTTservice.MESSAGE);
			if(message.contains("Motor"))
			{
				msgSubsMotor.setText(message);
				msgSubsMotor.setAnimation(anim);

			}
			if(message.contains("Moisture"))
			{
				msgSubsMoisture.setText(message);
				msgSubsMoisture.setAnimation(anim);
			}
			if(message.contains("Tank")){

				String waterPercentageStr=message.replaceFirst(".*?(\\d+).*", "$1");
				waterPercentage=Integer.parseInt(waterPercentageStr);
				waterTankLevel.setProgress(waterPercentage);
				waterPercentText.setText(waterPercentage + "%");
			}
			//Toast.makeText(context, "Push message received - " + topic + ":" + message, Toast.LENGTH_LONG).show();

		}
	}
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private ServiceConnection serviceConnection = new ServiceConnection()
    {
		@Override
		public void onServiceConnected(ComponentName arg0, IBinder binder)
		{
			service = new Messenger(binder);
			Bundle data = new Bundle();
			//data.putSerializable(MQTTservice.CLASSNAME, MainActivity.class);
			data.putCharSequence(MQTTservice.INTENTNAME, "com.example.MQTT.PushReceived");
			Message msg = Message.obtain(null, MQTTservice.REGISTER);
			msg.setData(data);
			msg.replyTo = serviceHandler;
			try
			{
				service.send(msg);
			}
			catch (RemoteException e)
			{
				e.printStackTrace();
			}
		}

		@Override
		public void onServiceDisconnected(ComponentName arg0)
		{
		}
    };

	private void addSubscribeButtonListener()
	{	
	    subscribeButton = (Button) findViewById(R.id.buttonSubscribe);
		subscribeButton.setOnClickListener(new OnClickListener()
	    {
	    	InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
	    	@Override
			public void onClick(View arg0) {
				TextView result = (TextView) findViewById(R.id.textResultStatus);
				//EditText t = (EditText) findViewById(R.id.EditTextTopic);
				//String topic = t.getText().toString().trim();
				String topicMoisture = "Sensor/Moisture";
				String topicMotor = "Sensor/Motor";
				String topicTankLevel = "Capacity/Tank";
				inputMethodManager.hideSoftInputFromWindow(result.getWindowToken(), 0);
				subscriptionFunction(topicMoisture, result);
				subscriptionFunction(topicMotor, result);
				subscriptionFunction(topicTankLevel, result);
				subscribeButton.setEnabled(false);
				subscribeButton.setText("SUBSCRIBED.....");
				subscribeButton.setBackgroundColor(Color.LTGRAY);
			}
		});

	}
	public void subscriptionFunction(String topic, TextView result){


		if (topic != null && topic.isEmpty() == false)
		{
			result.setText("Subscribed Now");
			Bundle data = new Bundle();
			data.putCharSequence(MQTTservice.TOPIC, topic);
			Message msg = Message.obtain(null, MQTTservice.SUBSCRIBE);
			msg.setData(data);
			msg.replyTo = serviceHandler;
			try
			{
				service.send(msg);
			}
			catch (RemoteException e)
			{
				e.printStackTrace();
				result.setText("Subscribe failed with exception:" + e.getMessage());
			}
		}
		else
		{
			result.setText("------");
		}
	}


	private void addPublishButtonListener()
	{
	    //Button publishButton = (Button) findViewById(R.id.buttonPublish);
        motorControlling=(ToggleButton) findViewById(R.id.motorControl);
	    /*publishButton.setOnClickListener(new OnClickListener()
	    {

	    	InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
	    	@Override
			public void onClick(View arg0)
	    	{   			
	    		EditText t = (EditText) findViewById(R.id.EditTextTopic);
	    		EditText m = (EditText) findViewById(R.id.EditTextTopic);
	    		TextView result = (TextView) findViewById(R.id.textResultStatus);
	    		inputMethodManager.hideSoftInputFromWindow(result.getWindowToken(), 0);
	    		
	    		String topic = t.getText().toString().trim();
	    		String message = m.getText().toString().trim();
	    		
	    		if (topic != null && topic.isEmpty() == false && message != null && message.isEmpty() == false)
	    		{
	    			result.setText("");
	    			Bundle data = new Bundle();
	    			data.putCharSequence(MQTTservice.TOPIC, topic);
	    			data.putCharSequence(MQTTservice.MESSAGE, message);
	    			Message msg = Message.obtain(null, MQTTservice.PUBLISH);
	    			msg.setData(data);
	    			msg.replyTo = serviceHandler;
	    			try
	    			{
	    				service.send(msg);
	    			}
	    			catch (RemoteException e)
	    			{
	    				e.printStackTrace();
	    				result.setText("Publish failed with exception:" + e.getMessage());
	    			}
	    		}
	    		else
	    		{
	    			result.setText("Topic and message required.");
	    		}
			}
		});*/

        motorControlling.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				//EditText t = (EditText) findViewById(R.id.EditTextTopic);
				//EditText m = (EditText) findViewById(R.id.EditTextTopic);

				TextView result = (TextView) findViewById(R.id.textResultStatus);
				inputMethodManager.hideSoftInputFromWindow(result.getWindowToken(), 0);

				String topic = "Control/Motor";
				String messageStart = "1";
				String messageStop = "0";
				if(isChecked){
					publishFunction(topic,messageStart,result);
				}
				else{
					publishFunction(topic,messageStop,result);
				}
			}
		});

	}

	public void publishFunction(String topic, String message, TextView result ){

		if (topic != null && topic.isEmpty() == false && message != null && message.isEmpty() == false)
		{
			result.setText("Published");
			Bundle data = new Bundle();
			data.putCharSequence(MQTTservice.TOPIC, topic);
			data.putCharSequence(MQTTservice.MESSAGE, message);
			Message msg = Message.obtain(null, MQTTservice.PUBLISH);
			msg.setData(data);
			msg.replyTo = serviceHandler;
			try
			{
				service.send(msg);
			}
			catch (RemoteException e)
			{
				e.printStackTrace();
				result.setText("Publish failed with exception:" + e.getMessage());
			}
		}
		else
		{
			result.setText("Topic and message required.");
		}
	}


	class ServiceHandler extends Handler
	{
	    @Override
	    public void handleMessage(Message msg)
	    {
		   	 switch (msg.what)
		   	 {
		   	 case MQTTservice.SUBSCRIBE: 	break;
		     case MQTTservice.PUBLISH:		break;
		     case MQTTservice.REGISTER:		break;
	    	 default:
	    		 super.handleMessage(msg);
	    		 return;
		   	 }
	   	 
	  		 Bundle b = msg.getData();
	  		 if (b != null)
	  		 {
	  			 TextView result = (TextView) findViewById(R.id.textResultStatus);
	  			 Boolean status = b.getBoolean(MQTTservice.STATUS);
	  			 if (status == false)
	  			 {
	  				result.setText("Fail");
	  			 }
	  			 else
	  			 {
	  				result.setText("Success");
	  			 }
	  		 }
	    }
	}
}
