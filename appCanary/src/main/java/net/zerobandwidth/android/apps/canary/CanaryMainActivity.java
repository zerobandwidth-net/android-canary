package net.zerobandwidth.android.apps.canary;

import android.app.Service;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import net.zerobandwidth.android.lib.canary.Canary;
import net.zerobandwidth.android.lib.canary.CanaryService;
import net.zerobandwidth.android.lib.services.SimpleServiceConnection;

/**
 * Main activity for Canary.
 * @since zerobandwidth-net/canary 0.0.1 (#1)
 */
public class CanaryMainActivity
extends AppCompatActivity
implements Canary.WifiStateListener, SimpleServiceConnection.Listener
{
/// Statics ////////////////////////////////////////////////////////////////////

    /** Tag for log entries. */
    public static final String TAG = CanaryMainActivity.class.getSimpleName() ;

/// Inner Classes //////////////////////////////////////////////////////////////

    /**
     * Updates the wifi button on the UI. Execute this runnable within the UI
     * thread.
     * @since zerobandwidth-net/canary 0.0.1 (#1)
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
        @Override
        public void run()
        {
            m_btnToggleWifi.setCompoundDrawablesWithIntrinsicBounds(
                    0, m_nDrawableID, 0, 0 ) ;
            m_btnToggleWifi.setText( m_sLabel ) ;
        }
    }

/// Instance Fields ////////////////////////////////////////////////////////////

    /** A persistent connection to the Canary Service. */
    protected SimpleServiceConnection<CanaryService> m_conn = null ;
    /** A persistent reference to a running Canary Service. */
    protected CanaryService m_srv = null ;
    /** The button that indicates and toggles wifi state. */
    protected Button m_btnToggleWifi = null ;
    /** The button that indicates and toggles mobile data state. */
    protected Button m_btnToggleMobileData = null ;

/// android.support.v7.app.AppCompatActivity ///////////////////////////////////

    @Override
    protected void onCreate( Bundle bndlState )
    {
        super.onCreate(bndlState) ;
        this.setContentView(R.layout.activity_main) ;
        this.setSupportActionBar((Toolbar)(findViewById(R.id.toolbar))) ;
        this.captureControlRefs() ;
        CanaryService.kickoff(this) ;
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
    protected void onResume()
    {
        super.onResume() ;
        this.connectToService() ;
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

    @Override
    protected void onDestroy()
    {
        if( m_conn != null )
        {
            m_conn.disconnect(this).removeListener(this) ;
            m_conn = null ;
        }
        m_srv = null ;
        super.onDestroy() ;
    }

/// Other Initializers /////////////////////////////////////////////////////////

    /**
     * Captures references to all the UI controls that we care about in the
     * class's logic.
     * @return (fluid)
     * @see #onCreate
     */
    protected CanaryMainActivity captureControlRefs()
    {
        m_btnToggleWifi = ((Button)(this.findViewById( R.id.btnToggleWifi ))) ;
        m_btnToggleMobileData =
                ((Button)(this.findViewById( R.id.btnToggleMobileData ))) ;

        return this ;
    }

    /**
     * Initializes the connection to the Canary Service.
     * @return (fluid)
     */
    protected CanaryMainActivity connectToService()
    {
        if( m_conn == null )
            m_conn = new SimpleServiceConnection<>( CanaryService.class ) ;
        m_conn.addListener(this).connect(this) ;
        return this ;
    }

/// net.zerobandwidth.android.lib.canary.WifiStateListener //////////////////////////

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

/// net.zerobandwidth.android.lib.services.SimpleServiceConnection.Listener ////


    @Override
    public <LS extends Service> void onServiceConnected( SimpleServiceConnection<LS> conn )
    {
        if( conn.isServiceClass(CanaryService.class) )
            m_srv = ((CanaryService)(conn.getServiceInstance())) ;
        Log.i( TAG, "Activity connected to service." ) ;
    }

    @Override
    public <LS extends Service> void onServiceDisconnected( SimpleServiceConnection<LS> conn )
    {
        Log.i( TAG, "Activity disconnected from service." ) ;
        m_srv = null ;
    }

/// Accessors / Mutators ///////////////////////////////////////////////////////


/// Button Handlers ////////////////////////////////////////////////////////////

    /**
     * Handles the button-press event for the "toggle wifi" button.
     * @param w the "toggle wifi" button
     */
    public void toggleWifi( View w )
    {
        if( m_srv != null )
            m_srv.getCanary().toggle( Canary.WIFI ) ;
    }

    /**
     * Handles the button-press event for the "toggle mobile data" button.
     * @param w the "toggle mobile data" button
     */
    public void toggleMobileData( View w )
    {
        if( m_srv != null )
            m_srv.getCanary().toggle( Canary.MOBILE ) ;
    }
}
