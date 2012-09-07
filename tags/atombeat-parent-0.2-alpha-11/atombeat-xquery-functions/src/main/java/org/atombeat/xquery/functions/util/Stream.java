package org.atombeat.xquery.functions.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * See
 * http://java.sun.com/docs/books/performance/1st_edition/html/JPIOPerformance
 * .fm.html
 * 
 * @author aliman
 * 
 */
public class Stream {

	static final int BUFF_SIZE = 100000;
	static final byte[] buffer = new byte[BUFF_SIZE];

	public static void copy(InputStream in, OutputStream out)
			throws IOException {
		try {
			while (true) {
				synchronized (buffer) {
					int amountRead = in.read(buffer);
					if (amountRead == -1) {
						break;
					}
					out.write(buffer, 0, amountRead);
				}
			}
		} finally {
			if (in != null) {
				in.close();
			}
			if (out != null) {
				out.close();
			}
		}
	}

	public static void copy(InputStream in, OutputStream out, long max)
			throws Exception {
		try {
			
			// we don't want to read any more than the maximum given
			long totalRead = 0;
			
			while (true) {
				
				// len - how much should we read?
				int len = 0;
				
				if (totalRead + BUFF_SIZE > max) {
					// we are reaching the maximum, read a partial buffer
					len = safeLongToInt(max - totalRead); // shouldn't need to worry about conversion from long to int, but just in case
				}
				else {
					// we're not yet near the maximum, read a full buffer
					len = BUFF_SIZE;
				}
				
				synchronized (buffer) {
					// read into the buffer
					int amountRead = in.read(buffer, 0, len);
					// have we reached the end?
					if (amountRead <= 0) {
						break;
					}
					// write out from the buffer
					out.write(buffer, 0, amountRead);
					// keep a tally of how much we've read in total
					totalRead = totalRead + amountRead;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			throw e; // rethrow for now
		} finally {
			if (in != null) {
				in.close();
			}
			if (out != null) {
				out.close();
			}
		}
	}
	
	public static int safeLongToInt(long l) {
	    if (l < Integer.MIN_VALUE || l > Integer.MAX_VALUE) {
	        throw new IllegalArgumentException
	            (l + " cannot be cast to int without changing its value.");
	    }
	    return (int) l;
	}

}
