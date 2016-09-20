/*This class is generated for describing first GUI objects*/
package com.esb_pc.udp;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

public class MainActivity extends Activity implements View.OnClickListener
{
    private Button startButton;

    protected EditText nameEditText;
    protected EditText sourcePortEditText;
    protected EditText destinationIpEditText;
    protected EditText destinationPortEditText;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        startButton = (Button) findViewById(R.id.startButton);
        startButton.setOnClickListener(this);

        nameEditText = (EditText) findViewById(R.id.nameEditText);
        sourcePortEditText = (EditText) findViewById(R.id.sourcePortEditText);
        destinationIpEditText = (EditText) findViewById(R.id.destinationIpEditText);
        destinationPortEditText = (EditText) findViewById(R.id.destinationPortEditText);
    }

    @Override
    public void onClick(View v)
    {
        String name = nameEditText.getText().toString();
        int sourcePort = Integer.parseInt(sourcePortEditText.getText().toString());
        String destinationIP = destinationIpEditText.getText().toString();
        int destinationPort = Integer.parseInt(destinationPortEditText.getText().toString());

        Intent intent = new Intent(getApplicationContext(), ChatActivity.class);
        intent.putExtra("name", name);
        intent.putExtra("sourcePort", sourcePort);
        intent.putExtra("destinationIP", destinationIP);
        intent.putExtra("destinationPort", destinationPort);

        startActivity(intent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {

        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {

        int id = item.getItemId();


        if (id == R.id.action_settings)
        {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
