package net.zerobandwidth.android.apps.canary;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;

import net.zerobandwidth.android.lib.canary.Canary;
import net.zerobandwidth.android.lib.canary.WifiListener;

/**
 * Main activity for Canary.
 * @since 0.1
 */
public class CanaryMainActivity
extends AppCompatActivity
implements WifiListener
{
    /** Tag for log entries. */
    public static final String TAG = CanaryMainActivity.class.getSimpleName() ;

    /**
     * Updates the wifi button on the UI. Execute this runnable within the UI
     * thread.
     * @since 0.1
     */
    protected class UpdateWifiButtonTask
    implements Runnable
    {
        /** The ID of the drawable to be set. */
        protected int m_nDrawableID = 0 ;
        /** The string to be set. */
        protected String m_sLabel = "" ;

        public UpdateWifiButtonTask setDrawableID( int n )
        { m_nDrawableID = n ; return this ; }

        public UpdateWifiButtonTask setLabel( String s )
        { m_sLabel = s ; return this ; }

        /**
         * Updates the button's appearance. This task should be executed on the
         * UI thread so that the change takes effect immediately.
         */
        public void run()
        {
            m_btnToggleWifi.setCompoundDrawablesWithIntrinsicBounds(
                    0, m_nDrawableID, 0, 0 ) ;
            m_btnToggleWifi.setText( m_sLabel ) ;
        }
    }

    /** Receiver for the various Android OS broadcasts. */
    protected Canary m_rcv = null ;
    /** The button that indicates and toggles wifi state. */
    protected Button m_btnToggleWifi = null ;
    /** The button that indicates and toggles mobile data state. */
    protected Button m_btnToggleMobileData = null ;
    /** Persistent reference to wifi manager. */
    protected WifiManager m_mgrWifi = null ;


    @Override
    protected void onCreate( Bundle bndlState )
    {
        super.onCreate(bndlState) ;
        this.setContentView(R.layout.activity_main) ;
        this.setSupportActionBar((Toolbar)(findViewById(R.id.toolbar))) ;
        this.captureControlRefs()
            .captureSystemServices()
            .initReceiver()
            ;
/* cargo cult code created by AS
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });
*/
    }

    @Override
    public boolean onCreateOptionsMenu( Menu menu )
    {
        // Don't bother with an options menu yet.
        // this.getMenuInflater().inflate( R.menu.menu_main, menu ) ;
        return true ;
    }

    @Override
    public boolean onOptionsItemSelected( MenuItem item )
    {
        final int id = item.getItemId() ;

        //noinspection SimplifiableIfStatement
        if( id == R.id.action_settings )
        {
            return true ;
        }

        return super.onOptionsItemSelected(item) ;
    }

    /**
     * Captures references to all the UI controls that we care about in the
     * class's logic.
     * @return the activity, for fluid invocations
     */
    protected CanaryMainActivity captureControlRefs()
    {
        m_btnToggleWifi = ((Button)(this.findViewById( R.id.btnToggleWifi ))) ;
        m_btnToggleMobileData =
                ((Button)(this.findViewById( R.id.btnToggleMobileData ))) ;

        return this ;
    }

    /**
     * Captures references to the system services that will provide network
     * information.
     * @return the activity, for fluid invocations
     */
    protected CanaryMainActivity captureSystemServices()
    {
        m_mgrWifi = ((WifiManager)
                (this.getSystemService(Context.WIFI_SERVICE))) ;

        return this ;
    }

    /**
     * Initializes an instance of the broadcast receiver.
     * @return the activity, for fluid invocations
     */
    protected CanaryMainActivity initReceiver()
    {
        m_rcv = Canary.getBoundInstance(this)
                .enable( Canary.WIFI )
                .addWifiListener(this)
                .register()
                ;
        return this ;
    }

    @Override
    public void onWifiStateChanged( int nCurrent, int nPrevious )
    {
        Log.d( TAG, (new StringBuffer())
                .append( "WiFi state changed from [" )
                .append( nPrevious )
                .append( "] to [" )
                .append( nCurrent )
                .append( "]" )
                .toString()
            );
        UpdateWifiButtonTask task = new UpdateWifiButtonTask() ;
        switch( nCurrent )
        {
            case WifiManager.WIFI_STATE_DISABLED:
            {
                task.setDrawableID( R.drawable.ic_signal_wifi_off_black_48dp ) ;
                task.setLabel( this.getString(R.string.label_btnWifiDisabled) );
            } break ;
            case WifiManager.WIFI_STATE_ENABLING:
            {
                task.setDrawableID( R.drawable.ic_signal_wifi_off_black_48dp ) ;
                task.setLabel( this.getString(R.string.label_btnWifiEnabling) );
            } break ;
            case WifiManager.WIFI_STATE_ENABLED:
            {
                task.setDrawableID( R.drawable.ic_network_wifi_black_48dp ) ;
                task.setLabel( this.getString(R.string.label_btnWifiEnabled) ) ;
            } break ;
            case WifiManager.WIFI_STATE_DISABLING:
            {
                task.setDrawableID( R.drawable.ic_network_wifi_black_48dp ) ;
                task.setLabel( this.getString(R.string.label_btnWifiDisabling) );
            } break ;
            case WifiManager.WIFI_STATE_UNKNOWN:
            default:
            {
                task.setDrawableID( R.drawable.ic_help_black_48dp ) ;
                task.setLabel( this.getString(R.string.label_btnWifiUnknown) ) ;
            } break ;
        }

        this.runOnUiThread( task ) ;
    }

    @Override
    protected void onDestroy()
    {
        m_rcv.stop() ;
        super.onDestroy() ;
    }
}
