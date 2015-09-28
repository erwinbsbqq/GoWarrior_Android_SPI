package com.gowarrior.demospi;

import android.app.Activity;
import android.gowarrior.SPIDev;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;


public class testSPI extends Activity {
    static final String LOG_TAG = "TestForSPI";
    private SPIDev spidev;
    private boolean threadFlag = true;
    private boolean freezingFlag = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test_spi);

        testSPIdemo();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_test_spi, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void testSPIdemo()
    {
        // Use the MAX31855 Sensor to get the temperature.
        new Thread(new Runnable(){
            @Override
            public void run() {
                int tmpInput,tmpValue;
                int tmpInt;
                int ret = 0;
                float tmpFloat, realTemp;
                byte [] r = new byte[4];
                int number = 0;
                // Create and Open the SPI device instance
                spidev = new SPIDev();
                ret = spidev.open(1, 0);
                if( 0 == ret ){
                    while(threadFlag) {
                        Log.d(LOG_TAG, "number = " + number);
                        number++;

                        // Read the data from the Sensor
                        r = spidev.readbytes(4);
                        if(r != null && r.length > 3) {
                            // Please Refer to MAX31855 spec to learn the algorithm
                            // Get the data[31:16] in the big-endian mode
                            /*
                            for( int i = 0; i < 4; i++){
                                Log.d( LOG_TAG, " r[" + i + "], Value = " + r[i]
                                        + ", Binary = " + Integer.toBinaryString(r[i] & 0xFF));
                            }
                            */
                            if( (r[1] & 0x01) == 0x01){
                                Log.e(LOG_TAG, "Error Occur:");
                                if( (r[3] & 0x01) == 0x01)
                                    Log.e(LOG_TAG, "No connection");
                                else if((r[3] & 0x02) == 0x02)
                                    Log.e(LOG_TAG, "Short-Circuited to GND");
                                else if((r[3] & 0x04) == 0x04)
                                    Log.e(LOG_TAG, "Short-Circuited to VCC");

                                continue;
                            }

                            tmpInput = (((r[0] << 8) & 0xFF00) | (r[1] & 0xFF) ) ;
                            tmpValue = tmpInput >> 2;

                            /*
                            Log.d(LOG_TAG, " tmpInput = " + tmpInput
                                    + ", Binary = " + Integer.toBinaryString(tmpInput & 0xFFFF));
                            Log.d( LOG_TAG, " tmpValue = " + tmpValue
                                    + ", Binary = " + Integer.toBinaryString(tmpValue & 0xFFFF));
                            */

                            if( (tmpInput & 0x8000 ) == 0x8000){
                                // The temperature is below freezing
                                freezingFlag = true;
                                tmpValue = (tmpValue - 1) ^ 0x3FFF;
                            }else
                                freezingFlag = false;

                            // Get the temperature data
                            tmpInt = tmpValue >> 2;
                            tmpFloat = ( tmpValue & 0x03 ) * 0.25F;
                            realTemp = tmpInt + tmpFloat;

                            //Log.d(LOG_TAG, "tmpInt = " + tmpInt + ", tmpFloat = " + tmpFloat);

                            if( freezingFlag )
                                Log.d( LOG_TAG, "Below Freezing Point,temp = -" + realTemp + " Celsius " );
                            else
                                Log.d( LOG_TAG, "Above Freezing Point,temp = " + realTemp + " Celsius " );
                        }
                        else {
                            Log.d(LOG_TAG, "read temperature fail!");
                        }

                        try {
                            Thread.sleep(1000);
                        }catch(Exception e){
                            e.printStackTrace();
                        }
                    }
                }else{
                    Log.d( LOG_TAG, "Fail to open the SPI device");
                }
            }
        }).start();
    }
}
