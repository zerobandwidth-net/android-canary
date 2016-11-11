package net.zerobandwidth.android.lib.canary;

/**
 * Classes that want the Canary to feed it signals about wifi state must
 * implement this interface.
 * @since zerobandwidth-net/canary 0.0.1 (#1)
 */
public interface WifiListener
{
    /**
     * When wifi state changes, {@link Canary} will call this method to inform
     * its wifi listeners.
     * @param nCurrent the new wifi state
     * @param nPrevious the previous wifi state
     */
    void onWifiStateChanged( int nCurrent, int nPrevious ) ;
}
