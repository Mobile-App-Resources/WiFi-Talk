/* This class is created to use communication GUI.
   Audio streaming , viewing and sending messages are handled in this class*/
package com.esb_pc.udp;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

public class ChatActivity extends Activity implements View.OnClickListener, OnSocketListener, Handler.Callback,SensorEventListener
{
    private String name;
    private int sourcePort; // source port for receiving messages
    private int destinationPort; // destination port for sending messages
    private int audioSourcePort; //  source port for receiving audio packages
    private int audioDestinationPort; // destination port for sending audio packages
    private String destinationIP; // IP address to send and receive audio packages and text messages
    private InetSocketAddress address;
    private Channel channel;
    private EditText messageEditText; // definition of objects which are layed on GUI
    private Button sendButton;
    private ListView messageListView; // list view for messages
    private ArrayAdapter<String> messageAdapter;
    private Handler handler;

    AudioRecord recorder; // recorder object to record audio stream
    AudioTrack speaker; // speaker object to play audio stream
    AudioManager mplayer;

    private int sampleRate = 11025; // number of samples of a sound that are taken per second to represent the event digitally
    private int channelConfig = AudioFormat.CHANNEL_OUT_MONO; // playing channel type of audio samples
    private int audioFormat = AudioFormat.ENCODING_PCM_16BIT; // method is used to digitally represent sampled analog signals
    private int channelConfig1 = AudioFormat.CHANNEL_IN_MONO; // recording channel type of audio samples
    private boolean status = true;
    private boolean speakerStatus= false;
    private SensorManager mSensorManager; // mSensorManager and mSensor object definition of promixity sensor hardware
    private Sensor mSensor;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat); // setting content of activity_chat GUI for next processes

        Intent intent = getIntent();
        name = intent.getStringExtra("name");
        sourcePort = intent.getIntExtra("sourcePort", 1111); // default value of sourceport
        audioSourcePort= sourcePort+1; // audioSourcePort is added one for the not mixing string files with audio samples
        destinationIP = intent.getStringExtra("destinationIP");
        destinationPort = intent.getIntExtra("destinationPort", 2222); // default value of destinationport
        audioDestinationPort=destinationPort+1; // audioDestinationPort is added one for the not mixing string files with audio files
        address = new InetSocketAddress(destinationIP, destinationPort); // getting and assigning destination IP address
        messageEditText = (EditText) findViewById(R.id.messageEditText); // definition of object for reading texts from user
        sendButton = (Button) findViewById(R.id.sendButton); // definition of sending button object
        sendButton.setOnClickListener(this); // wait for the pressing button
        messageAdapter = new ArrayAdapter<String>(getApplicationContext(), R.layout.layout_message, R.id.messageTextView); // keep the written messages on GUI
        messageListView = (ListView) findViewById(R.id.messageListView);
        messageListView.setAdapter(messageAdapter);
        handler = new Handler(this);

        mplayer=(AudioManager)getSystemService(Context.AUDIO_SERVICE); // setting audio properties for earpiece speaker and speaker
        mplayer.setMode(AudioManager.MODE_IN_CALL);
        mplayer.setSpeakerphoneOn(false); // default value for audio output is earpiece speaker
        speakerStatus=false;
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE); // setting proximity sensor propertiess
        mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
    }

    @Override
    protected void onStart() // this overrided function is used for handling background audio streaming processes
    {
        super.onStart();
        if(null == channel) // if channel is not busy
        {
            try
            {
                channel = new Channel(this); // create a channel object (text message communication)
                channel.bind(sourcePort); // bind the port to specified channel
                channel.start(); //start channel
                // the codes which are at the below describes full duplex communication logic. At the same time, one device can record and also play the audio
                // files
                startStreaming(); // record audio files
                PlayAudio a=new PlayAudio(); // play them via background thread
                a.execute();
                startReceiving(); // get audio files
                RecordAudio t=new RecordAudio(); // record them via background thread
                t.execute();

                mSensorManager.registerListener(this,mSensor,SensorManager.SENSOR_DELAY_NORMAL); // when GUI starts, proximity sensor also starts running

            }
            catch (Exception e)
            {
                e.printStackTrace();
                finish();
            }
        }
    }

    @Override
    protected void onStop() // if no files are expected, release channel
    {
        super.onStop();
        if(null != channel)
        {
            channel.stop();
        }
    }

    @Override
    public void onClick(View v) // viewing written messages
    {
        String text = messageEditText.getText().toString();
        text = name + " >> " + text;

        messageEditText.setText("");

        channel.sendTo(address, text);

        messageAdapter.add(text);
        messageListView.smoothScrollToPosition(messageAdapter.getCount() - 1);
    }

    @Override
    public void onReceived(String text)
    {
        Bundle bundle = new Bundle();
        bundle.putString("text", text);

        Message msg = new Message();
        msg.setData(bundle);

        handler.sendMessage(msg);
    }

    @Override
    public boolean handleMessage(Message msg)
    {
        Bundle bundle = msg.getData();
        String text = bundle.getString("text");

        messageAdapter.add(text);
        messageListView.smoothScrollToPosition(messageAdapter.getCount() - 1);

        return false;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_chat, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        // this area describes turning on or off the speaker
      switch( item.getItemId()) {

          case R.id.speakerOff: { // when turn speaker off button is pressed, set audio output to earpiece speaker
              speakerStatus = false;
              mplayer.setMode(AudioManager.MODE_IN_CALL);
              mplayer.setSpeakerphoneOn(speakerStatus);
              break;
          }
          case R.id.speakerOn: { // when turn speaker on button is pressed, set audio output to speaker
              speakerStatus = true;
              mplayer.setMode(AudioManager.MODE_IN_CALL);
              mplayer.setSpeakerphoneOn(speakerStatus);
              break;
          }
      }
        return super.onOptionsItemSelected(item);
    }

    public void startStreaming() {

        Thread streamThread = new Thread(new Runnable() {

            @Override
            public void run() {
                try {

                    int minBufSize = 4096; // assigning value for minimum buffer size which holds audio packets(ideal size is selected 4096)
                    DatagramSocket socket = new DatagramSocket();
                    DatagramPacket packet;
                    Log.d("VS", "Socket Created");

                    byte[] buffer = new byte[minBufSize*5];
                    Log.d("VS","Buffer created of size " + minBufSize);

                    final InetAddress destination = InetAddress.getByName(destinationIP);
                    Log.d("VS", "Address retrieved");

                    //creating recorder object which records audio samples in specified format
                    recorder = new AudioRecord(MediaRecorder.AudioSource.MIC,sampleRate,channelConfig1,audioFormat,minBufSize);
                    Log.d("VS", "Recorder initialized");

                    recorder.startRecording(); // start recording audio files

                    while(status == true) {

                        //reading data from MIC into buffer
                        recorder.read(buffer, 0, buffer.length);
                        Log.d("VS", "Reading");
                        //putting buffer in the packet
                        packet = new DatagramPacket (buffer,buffer.length,destination,audioDestinationPort);
                        Log.d("VS", "creating packet");

                        socket.send(packet);


                    }



                } catch(UnknownHostException e) {
                    Log.e("VS", "UnknownHostException");
                }
                catch (Throwable t) {
                    Log.e("AudioTrack", "Playback Failed");
                }


            }

        });
        streamThread.start();
    }

    private class PlayAudio extends AsyncTask<Void, Integer, Void> { // AsyncTask class is used for not interfering with GUI. It handles its
    // jobs  on background
        @Override
        protected Void doInBackground(Void... params) {

            try {

                DatagramSocket receivingsocket = new DatagramSocket(audioSourcePort);
                Log.d("VR", "Socket Created");

                int minBufSize1 = 4096;
                byte[] buffer1 = new byte[minBufSize1*5];

                //definition of speaker object which plays audio samples in specified format
                speaker = new AudioTrack(AudioManager.STREAM_MUSIC,sampleRate,channelConfig,audioFormat,(minBufSize1*5),AudioTrack.MODE_STREAM);

                speaker.play();
                while(status == true) {
                    try {


                        DatagramPacket packet1 = new DatagramPacket(buffer1,buffer1.length);
                        receivingsocket.receive(packet1);
                        Log.d("VR", "Packet Received");

                        //reading content from packet
                        buffer1=packet1.getData();
                        Log.d("VR", "Packet data read into buffer");

                        //sending data to the Audiotrack obj i.e. speaker
                        speaker.write(buffer1, 0, minBufSize1);
                        Log.d("VR", "Writing buffer content to speaker");

                    } catch(IOException e) {
                        Log.e("VR","IOException");
                    }
                    catch(Throwable t){
                        Log.e("AudioTrack", "Playback Failed");
                    }
                }


            } catch (SocketException e) {
                Log.e("VR", "SocketException"+e.getMessage());
            }

            return null;
        }

    }
    //the processes at below uses same methods to handle audio tasks
    public void startReceiving() {

        Thread receiveThread = new Thread (new Runnable() {

            @Override
            public void run() {

                try {

                    DatagramSocket socket = new DatagramSocket(audioDestinationPort);
                    Log.d("VR", "Socket Created");

                    int minBufSize = AudioTrack.getMinBufferSize(sampleRate, channelConfig, audioFormat);
                    byte[] buffer = new byte[minBufSize*10];

                    speaker = new AudioTrack(AudioManager.STREAM_MUSIC,sampleRate,channelConfig,audioFormat,(minBufSize*10),AudioTrack.MODE_STREAM);
                    speaker.play();

                    while(status == true) {
                        try {

                            DatagramPacket packet = new DatagramPacket(buffer,buffer.length);
                            socket.receive(packet);
                            Log.d("VR", "Packet Received");

                            //reading content from packet
                            buffer=packet.getData();
                            Log.d("VR", "Packet data read into buffer");

                            //sending data to the Audiotrack obj i.e. speaker
                            speaker.write(buffer, 0, minBufSize);
                            Log.d("VR", "Writing buffer content to speaker");
                            //  Arrays.fill(buffer, Byte.parseByte(null));
                        } catch(IOException e) {
                            Log.e("VR","IOException");
                        }
                        catch(Throwable t){
                            Log.e("AudioTrack", "Playback Failed");
                        }
                    }


                } catch (SocketException e) {
                    Log.e("VR", "SocketException"+e.getMessage());
                }


            }

        });
        receiveThread.start();
    }
    private class RecordAudio extends AsyncTask<Void, Integer, Void> {
        @Override
        protected Void doInBackground(Void... params) {
            try {

                int minBufSize1 = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat);
                DatagramSocket sendingsocket = new DatagramSocket();
                Log.d("VS", "Socket Created");
                byte[] buffer1 = new byte[minBufSize1*10];

                Log.d("VS", "Buffer created of size " + minBufSize1);
                DatagramPacket packet1;

                final InetAddress destination = InetAddress.getByName(destinationIP);
                Log.d("VS", "Address retrieved");

                recorder = new AudioRecord(MediaRecorder.AudioSource.MIC, sampleRate, channelConfig, audioFormat, minBufSize1);
                Log.d("VS", "Recorder initialized");

                recorder.startRecording();

                while (status == true) {

                    //reading data from recorder object into buffer
                    recorder.read(buffer1, 0, buffer1.length);
                    Log.d("VS", "Reading");
                    //putting buffer in the packet
                    packet1 = new DatagramPacket(buffer1, buffer1.length, destination, audioSourcePort);
                    Log.d("VS", "creating packet");
                    sendingsocket.send(packet1);

                }

            } catch (UnknownHostException e) {
                Log.e("VS", "UnknownHostException");
            } catch (Throwable t) {
                Log.e("AudioTrack", "Playback Failed");
            }


            return null;
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) { // when any object(e.g. ear) is really close to proximity sensor, stop interaction with screen
        if (event.values[0]==0)
        {
            setContentView(R.layout.black); // set the layout black
        }
        else
        {
            setContentView(R.layout.activity_chat);
        }

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy)
    {

    }

}


