package net.zerobandwidth.android.lib.canary;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.DhcpInfo;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.util.Log;

import java.util.ArrayList;

/**
 * Listens for signals from the Android OS that network connectivity has
 * changed.
 * @since zerobandwidth-net/canary 0.0.1 (#1)
 */
@SuppressWarnings("unused")
public class Canary
extends BroadcastReceiver
{
/// Statics ////////////////////////////////////////////////////////////////////

    /** Log entry tag. */
    public static final String TAG = Canary.class.getSimpleName() ;

    /** Flag for the wifi monitoring feature. */
    public static final long WIFI =   1L ;
    /** Flag for the mobile data monitoring feature. */
    public static final long MOBILE = 2L ;

    /**
     * The value typically returned by Android services when asked to which
     * network the device is currently connected, and the answer is "none".
     */
    public static final int NETWORK_ID_UNAVAILABLE = -1 ;

    /**
     * A static method to create a new instance bound to the given context.
     * @param ctx the context in which the receiver operates
     * @return an instance bound to that context
     */
    public static Canary getBoundInstance( Context ctx )
    { return new Canary(ctx) ; }

    /**
     * Indicates whether the app is certain that it is indeed connected to a
     * wifi network.
     * @param ctx the context in which to evaluate wifi connection state
     * @return true if the algorithm thinks it's connected
     * @see #isWifiConnected(WifiManager)
     */
    public static boolean isWifiConnected( Context ctx )
    {
        if( ctx == null ) return false ;
        final WifiManager mgrWifi = ((WifiManager)
                ( ctx.getSystemService( Context.WIFI_SERVICE ) )) ;
        return isWifiConnected(mgrWifi) ;
    }

    /**
     * Indicates whether the app is certain that it is indeed connected to a
     * wifi network.
     * The algorithm requires all of the following to be true:
     * <ul>
     * <li>A {@link WifiManager} instance is available.</li>
     * <li>That manager can provide a non-null {@link WifiInfo} instance.</li>
     * <li>The info includes a network ID.</li>
     * <li>The info's "supplicant state" is "completed".</li>
     * <li>The manager can also provide non-null {@link DhcpInfo} instance.</li>
     * <li>The IP addresses in the two info objects are equal.</li>
     * </ul>
     * @param mgrWifi the device's wifi manager
     * @return true if the algorithm thinks it's connected
     * @see #isWifiConnected(Context)
     */
    public static boolean isWifiConnected( WifiManager mgrWifi )
    {
        if( mgrWifi == null ) return false ; // defensively
        WifiInfo infoWifi = mgrWifi.getConnectionInfo() ;
        if( infoWifi == null ) return false ;
        if( infoWifi.getNetworkId() == NETWORK_ID_UNAVAILABLE ) return false ;
        if( infoWifi.getSupplicantState() != SupplicantState.COMPLETED )
            return false ;
        DhcpInfo infoDHCP = mgrWifi.getDhcpInfo() ;
        if( infoDHCP == null ) return false ;
        if( infoDHCP.ipAddress != infoWifi.getIpAddress() ) return false ;
        return true ;
    }

    /**
     * Indicates whether the app is certain that it is indeed connected to a
     * mobile data network.
     * @param ctx the context in which to evaluate mobile data connection state
     * @return true if the algorithm thinks it's connected
     * @see #isMobileDataConnected(TelephonyManager)
     */
    public static boolean isMobileDataConnected( Context ctx )
    {
        if( ctx == null ) return false ;
        final TelephonyManager mgrTel = ((TelephonyManager)
                ( ctx.getSystemService( Context.TELEPHONY_SERVICE ) )) ;
        return isMobileDataConnected(mgrTel) ;
    }

    /**
     * Indicates whether the app is certain that it is indeed connected to a
     * mobile data network.
     * @param mgrTel the device's telephony manager
     * @return true if the algorithm thinks it's connected
     * @see #isMobileDataConnected(Context)
     */
    public static boolean isMobileDataConnected( TelephonyManager mgrTel )
    {
        if( mgrTel == null ) return false ; // defensively
        return ( mgrTel.getDataState() == TelephonyManager.DATA_CONNECTED ) ;
    }

/// Inner Classes //////////////////////////////////////////////////////////////

    /**
     * Objects that want the Canary to feed them signals about wifi state must
     * implement this interface.
     * @since zerobandwidth-net/canary 0.0.1 (#1)
     */
    public interface WifiStateListener
    {
        /**
         * When wifi state changes, {@link Canary} will call this method to
         * inform its wifi listeners.
         * @param nCurrent the new wifi state
         * @param nPrevious the previous wifi state
         */
        void onWifiStateChanged( int nCurrent, int nPrevious ) ;
    }

    /**
     * Objects that want the Canary to feed them signals about mobile data state
     * must implement this interface.
     * @since zerobandwidth-net/canary 0.0.1 (#1)
     */
    public interface MobileDataStateListener
    {
        /**
         * When mobile data state changes, {@link Canary} will call this method
         * to inform its mobile data state listeners.
         * @param nCurrent the new mobile data state
         * @param nPrevious the previous mobile data state
         */
        void onMobileDataStateChanged( int nCurrent, int nPrevious ) ;
    }

/// Instance Fields ////////////////////////////////////////////////////////////

    /** The context in which the receiver operates and gathers resources. */
    protected Context m_ctx = null ;

    /** A persistent reference to the device's connectivity manager. */
    protected ConnectivityManager m_mgrConn = null ;

    /** A persistent reference to the device's wifi manager. */
    protected WifiManager m_mgrWifi = null ;

    /** A persistent reference to the device's telephony manager. */
    protected TelephonyManager m_mgrTel = null ;

    /** Registry of wifi state listeners. */
    protected ArrayList<WifiStateListener> m_aWifiListeners = null ;

    /** Registry of mobile data state listeners. */
    protected ArrayList<MobileDataStateListener> m_aMobileListeners = null ;

    /** Specifies whether to notify on wifi signals. */
    protected long m_bmFeaturesEnabled = 0L ;

/// Life Cycle /////////////////////////////////////////////////////////////////

    /**
     * Binds a Canary instance to the given context, and registers it to catch
     * relevant signals.
     * @param ctx the context in which to operate
     */
    public Canary( Context ctx )
    {
        this.setContext(ctx).initListenerRegistry() ;
    }

    /**
     * Initializes the listener registry.
     * @return (fluid)
     */
    public Canary initListenerRegistry()
    {
        m_aWifiListeners = new ArrayList<>() ;
        m_aMobileListeners = new ArrayList<>() ;
        return this ;
    }

    /**
     * Registers the receiver in the context to which it is bound.
     * @return (fluid)
     */
    public Canary register()
    {
        if( m_ctx != null )
        {
            IntentFilter f = new IntentFilter() ;
            if( this.isEnabled(WIFI) )
            {
                f.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION) ;
                f.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION) ;
            }
            if( this.isEnabled(MOBILE) )
            {
                f.addAction(ConnectivityManager.CONNECTIVITY_ACTION) ;
            }
            m_ctx.registerReceiver( this, f ) ;
            Log.i( TAG, "Registered as a receiver." ) ;
        }
        else
            Log.e( TAG, "Cannot register the receiver without a context." ) ;

        return this ;
    }

    /**
     * Unregisters the receiver in the context to which it is bound.
     * @return (fluid)
     */
    public Canary unregister()
    {
        if( m_ctx != null )
        {
            try { m_ctx.unregisterReceiver( this ) ; }
            catch( IllegalArgumentException iax )
            { Log.d( TAG, "Tried to unregister, but wasn't registered yet." ) ; }
            m_mgrConn = null ;
            m_mgrWifi = null ;
            m_mgrTel = null ;
        }
        else
            Log.w( TAG, "Null context; nothing to do to unregister." ) ;

        return this ;
    }

    /**
     * The context in which this receiver is created should call this method in
     * its {@code onDestroy()} method.
     * @return (fluid)
     */
    public Canary stop()
    {
        this.unregister() ;
        m_aWifiListeners.clear() ;
        return this ;
    }

/// Accessors / Mutators ///////////////////////////////////////////////////////

    /**
     * Binds the instance to a context.
     * @param ctx the context in which to operate
     * @return (fluid)
     */
    public Canary setContext( Context ctx )
    {
        if( m_ctx != null )
            this.unregister() ;
        m_ctx = ctx ;
        if( m_ctx == null )
        { // Clear all context-dependent references.
            Log.w( TAG, "Null context was set. Cannot register a receiver." ) ;
            m_mgrConn = null ;
            m_mgrWifi = null ;
            m_mgrTel = null ;
        }
        else
        { // Bind to the managers provided by the specified context.
            m_mgrConn = ((ConnectivityManager)
                    ( m_ctx.getSystemService( Context.CONNECTIVITY_SERVICE ) ));
            m_mgrWifi = ((WifiManager)
                    ( m_ctx.getSystemService( Context.WIFI_SERVICE ) )) ;
            m_mgrTel = ((TelephonyManager)
                    ( m_ctx.getSystemService( Context.TELEPHONY_SERVICE ) )) ;
        }
        return this ;
    }

    /**
     * Registers the given object as a listener for information about wifi
     * connectivity.
     * @param l the listener
     * @return (fluid)
     */
    public synchronized Canary addWifiListener( WifiStateListener l )
    {
        if( ! m_aWifiListeners.contains(l) )
            m_aWifiListeners.add(l) ;
        else
            Log.d( TAG, "Skipped adding a duplicate listener." ) ;
        return this ;
    }

    /**
     * Unregisters the given object as a listener for information about wifi
     * connectivity.
     * @param l the listener to be removed
     * @return (fluid)
     */
    public synchronized Canary removeWifiListener( WifiStateListener l )
    {
        if( m_aWifiListeners.contains(l) )
            m_aWifiListeners.remove(l) ;
        else
            Log.d( TAG, "Skipped removing a listener; not found in list." ) ;
        return this ;
    }

    /**
     * Verifies that the given bitmask of features is enabled for the receiver.
     * @param bmFlags a bitmask of flags to check
     * @return true iff all features in the mask are enabled
     */
    public boolean isEnabled( long bmFlags )
    { return( ( m_bmFeaturesEnabled & bmFlags ) == bmFlags ) ; }

    /**
     * Enables all features given as flags in the bitmask.
     * The receiver will be re-registered in its original context with a new
     * intent filter.
     * @param bmFlags a bitmask of flags to enable
     * @return (fluid)
     */
    public Canary enable( long bmFlags )
    {
        m_bmFeaturesEnabled |= bmFlags ;
        this.unregister().register() ;
        return this ;
    }

    /**
     * Disables all features given as flags in the bitmask.
     * The receiver will be re-registered in its original context with a new
     * intent filter.
     * @param bmFlags a bitmask of flags to disable
     * @return (fluid)
     */
    public Canary disable( long bmFlags )
    {
        m_bmFeaturesEnabled &= ~bmFlags ;
        this.unregister().register() ;
        return this ;
    }

    /**
     * Toggles every feature that is mentioned in the bitmask.
     * @param bmFlags a bitmask of features
     * @return (fluid)
     */
    public Canary toggle( long bmFlags )
    {
        if( this.isEnabled(bmFlags) )
            return this.disable(bmFlags) ;
        else
            return this.enable(bmFlags) ;
    }

/// android.os.BroadcastReceiver ///////////////////////////////////////////////

    @Override
    public void onReceive( Context ctx, Intent sig )
    {
        if( ctx == null )
        { Log.d( TAG, "Received signal with null context." ) ; return ; }

        if( ctx != m_ctx )
        { Log.d( TAG, "Received signal from external context." ) ; return ; }

        if( sig == null )
        { Log.d( TAG, "Received a null intent." ) ; return ; }

        final String sAction = sig.getAction() ;
        if( sAction == null )
        { Log.d( TAG, "Received intent with null action." ) ; return ; }

        Log.d( TAG, (new StringBuffer())
                .append( "Caught action [" )
                .append( sAction )
                .append( "]." )
                .toString()
            );

        final Bundle bndlExtras = sig.getExtras() ;

        switch( sAction )
        {
            case WifiManager.WIFI_STATE_CHANGED_ACTION:
            {
                this.onWifiStateChanged(bndlExtras) ;
            } break ;
            case ConnectivityManager.CONNECTIVITY_ACTION:
            {
                this.onMobileDataStateChanged(bndlExtras) ;
            } break ;
            default:
            {
                Log.d( TAG, "Ignored unrecognized action." ) ;
            }
        }
    }

/// Other Instance Methods /////////////////////////////////////////////////////

    /**
     * Notifies all listeners that the wifi state has changed.
     * @param bndlInfo a bundle of network information that was caught in a
     *                 NETWORK_STATE_CHANGED_ACTION from the Android OS.
     */
    protected synchronized void onWifiStateChanged( Bundle bndlInfo )
    {
        final int nCurrent = bndlInfo.getInt( WifiManager.EXTRA_WIFI_STATE ) ;
        final int nPrevious =
                bndlInfo.getInt( WifiManager.EXTRA_PREVIOUS_WIFI_STATE ) ;
        for( WifiStateListener l : m_aWifiListeners )
            l.onWifiStateChanged( nCurrent, nPrevious ) ;
    }

    /**
     * Notifies all listeners that the mobile data state has changed.
     * @param bndlInfo a bundle of network information that was caught in a
     *                 CONNECTIVITY_ACTION from the Android OS.
     */
    protected synchronized void onMobileDataStateChanged( Bundle bndlInfo )
    {
        // TODO How do we figure out what our state is? Previous methods are deprecated.
    }
}
