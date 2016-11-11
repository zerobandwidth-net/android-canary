package net.zerobandwidth.android.lib.canary;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import net.zerobandwidth.android.lib.IntentUtils;
import net.zerobandwidth.android.lib.services.SimpleServiceConnection;

/**
 * A service which provides a context for the listener and can relaunch the
 * app's activity when something happens.
 * @since zerobandwidth-net/canary 0.0.1 (#1)
 */
public class CanaryService
extends Service
{
/// Statics ////////////////////////////////////////////////////////////////////

    public static final String TAG = CanaryService.class.getSimpleName() ;

    protected static final String ACTION_PREFIX =
            "net.zerobandwidth.android.lib.canary." ;
    protected static final String ACTION_KICKOFF =
            ACTION_PREFIX + "SERVICE_START" ;

    protected static Intent getBoundIntent( Context ctx )
    { return new Intent( ctx, CanaryService.class ) ; }

    public static void kickoff( Context ctx )
    { ctx.startService( getBoundIntent(ctx).setAction( ACTION_KICKOFF ) ) ; }

/// Inner Classes //////////////////////////////////////////////////////////////

    /**
     * Standard binding to the service.
     * @since zerobandwidth-net/canary 0.0.1 (#1)
     */
    public class Binder
    extends android.os.Binder
    implements SimpleServiceConnection.InstanceBinder
    {
        @Override
        @SuppressWarnings("unchecked")       // "this" can't be anything else...
        public CanaryService getServiceInstance()
        { return CanaryService.this ; }
    }

/// Instance Fields ////////////////////////////////////////////////////////////

    /**
     * A singleton, persistent binding to this service.
     * @see #onBind
     */
    public final CanaryService.Binder m_bind = new CanaryService.Binder() ;

    /**
     * A receiver which is bound to the service. The service will create and
     * register its own Canary instance when it is kicked off.
     */
    protected Canary m_rcv = null ;

/// android.app.Service ////////////////////////////////////////////////////////

    /** @see #m_bind */
    @Nullable
    @Override
    public IBinder onBind( Intent sig )
    { return m_bind ; }

    @Override
    public void onCreate()
    {
        super.onCreate() ;
        Log.i( TAG, "Canary service created." ) ;
        m_rcv = new Canary(this) ;
        m_rcv.register() ;
    }

    @Override
    public int onStartCommand( Intent sig, int bmFlags, int zStartID )
    {
        super.onStartCommand( sig, bmFlags, zStartID ) ;
        final String sAction = IntentUtils.discoverAction(sig) ;
        Log.i( TAG, (new StringBuilder())
                .append( "Received a command with ID [" )
                .append( zStartID )
                .append( "] and action [" )
                .append(( sAction == null ? "(null)" : sAction ))
                .append( "].")
                .toString()
            );
        return Service.START_STICKY ;
    }

    @Override
    public void onRebind( Intent sig )
    {
        super.onRebind(sig) ;
    }

    @Override
    public boolean onUnbind( Intent sig )
    {
        return super.onUnbind(sig) ;
    }

    @Override
    public void onDestroy()
    {
        if( m_rcv != null )
        {
            m_rcv.unregister() ;
            m_rcv = null ;
        }
        super.onDestroy() ;
    }

/// Accessors / Mutators ///////////////////////////////////////////////////////

    /**
     * Accesses the Canary receiver instance.
     * @return the service's Canary instance
     */
    public Canary getCanary()
    { return m_rcv ; }

/// Other Instance Methods /////////////////////////////////////////////////////
}
