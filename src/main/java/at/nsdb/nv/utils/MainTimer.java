package at.nsdb.nv.utils;

import java.time.ZonedDateTime;

public class MainTimer {
	ZonedDateTime startTime = ZonedDateTime.now();	
	
	/*--------------------
	 * elapsed time in ms
	 */
	public long elapsedTime() {
		return ZonedDateTime.now().toInstant().toEpochMilli() - startTime.toInstant().toEpochMilli();
	}	

}
