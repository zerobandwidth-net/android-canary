package net.zerobandwidth.android.lib.canary;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.util.Log;

import java.util.ArrayList;

/**
 * Listens for signals from the Android OS that network connectivity has
 * changed.
 * @since 0.1
 */
@SuppressWarnings("unused")
public class Canary
extends BroadcastReceiver
{
    /** Log entry tag. */
    public static final String TAG = Canary.class.getSimpleName() ;

    /** Flag for the wifi monitoring feature. */
    public static final long WIFI =   1L ;
    /** Flag for the mobile data monitoring feature. */
    public static final long MOBILE = 2L ;

    /**
     * A static method to create a new instance bound to the given context.
     * @param ctx the context in which the receiver operates
     * @return an instance bound to that context
     */
    public static Canary getBoundInstance( Context ctx )
    { return new Canary(ctx) ; }

    /** The context in which the receiver operates and gathers resources. */
    protected Context m_ctx = null ;

    /** Registry of wifi listeners. */
    protected ArrayList<WifiListener> m_aWifiListeners = null ;

    /** Specifies whether to notify on wifi signals. */
    protected long m_bmFeaturesEnabled = 0L ;

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
     * Binds the instance to a context.
     * @param ctx the context in which to operate
     * @return the instance, for fluid invocation
     */
    public Canary setContext( Context ctx )
    {
        if( m_ctx != null )
            this.unregister() ;
        m_ctx = ctx ;
        if( m_ctx == null )
        {
            Log.w( TAG, "Null context was set. Cannot register a receiver." ) ;
        }
        return this ;
    }

    /**
     * Initializes the listener registry.
     * @return the instance, for fluid invocation
     */
    public Canary initListenerRegistry()
    {
        m_aWifiListeners = new ArrayList<>() ;
        return this ;
    }

    /**
     * Registers the receiver in the context to which it is bound.
     * @return the instance, for fluid invocation
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
            m_ctx.registerReceiver( this, f ) ;
        }
        else
            Log.e( TAG, "Cannot register the receiver without a context." ) ;

        return this ;
    }

    /**
     * Unregisters the receiver in the context to which it is bound.
     * @return the instance, for fluid invocation
     */
    public Canary unregister()
    {
        if( m_ctx != null )
        {
            try { m_ctx.unregisterReceiver( this ) ; }
            catch( IllegalArgumentException iax )
            { Log.d( TAG, "Tried to unregister, but wasn't registered yet." ) ; }
        }
        else
            Log.i( TAG, "Null context; nothing to do to unregister." ) ;

        return this ;
    }

    /**
     * Registers the given object as a listener for information about wifi
     * connectivity.
     * @param l the listener
     * @return the instance, for fluid invocation
     */
    public synchronized Canary addWifiListener( WifiListener l )
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
     * @return the instance, for fluid invocation
     */
    public synchronized Canary removeWifiListener( WifiListener l )
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
     * @return the instance, for fluid invocation
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
     * @return the instance, for fluid invocation
     */
    public Canary disable( long bmFlags )
    {
        m_bmFeaturesEnabled &= ~bmFlags ;
        this.unregister().register() ;
        return this ;
    }

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
                // TODO
            } break ;
            default:
            {
                Log.d( TAG, "Ignored unrecognized action." ) ;
            }
        }
    }

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
        for( WifiListener l : m_aWifiListeners )
            l.onWifiStateChanged( nCurrent, nPrevious ) ;
    }

    /**
     * The context in which this receiver is created should call this method in
     * its {@code onDestroy()} method.
     * @return the instance, for fluid invocation
     */
    public Canary stop()
    {
        this.unregister() ;
        m_aWifiListeners.clear() ;
        return this ;
    }
}
