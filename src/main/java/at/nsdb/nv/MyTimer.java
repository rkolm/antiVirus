package at.nsdb.nv;

import java.time.ZonedDateTime;

public class MyTimer {
	long startTime;
	
	
	/*--------------------
	 * constructor, set Timer
	 */
	public MyTimer() {
		startTime = ZonedDateTime.now().toInstant().toEpochMilli();
	}
	
	
	/*--------------------
	 * elapsed time in ms
	 */
	public long elapsedTime() {
		return ZonedDateTime.now().toInstant().toEpochMilli() - startTime;
	}
	
	
	
	/*--------------------
	 * delay in ms
	 */
	public static void delay( long ms) {
        try {
            Thread.sleep( ms);
        }
        catch(InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
	}
	
	

}
