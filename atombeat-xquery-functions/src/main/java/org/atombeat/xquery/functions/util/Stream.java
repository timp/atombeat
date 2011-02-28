package org.atombeat.xquery.functions.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * See http://java.sun.com/docs/books/performance/1st_edition/html/JPIOPerformance.fm.html
 * 
 * @author aliman
 *
 */
public class Stream {

	static final int BUFF_SIZE = 100000;
	static final byte[] buffer = new byte[BUFF_SIZE];

	public static void copy(InputStream in, OutputStream out) throws IOException {
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

}
