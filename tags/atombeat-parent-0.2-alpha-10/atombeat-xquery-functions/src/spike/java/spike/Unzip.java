package spike;

import java.io.IOException;
import java.util.Enumeration;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;


public class Unzip {

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		Logger log = Logger.getLogger(Unzip.class.getCanonicalName());
		ZipFile zf = new ZipFile(args[0]);
		log.info(zf.getName());
		log.info(Integer.toString(zf.size()));
		log.info(Integer.toString(zf.hashCode()));
		ZipEntry e;
		Enumeration entries = zf.entries();
		for (int i=0; entries.hasMoreElements(); i++) {
			log.info(Integer.toString(i));
			e = (ZipEntry) entries.nextElement();
			log.info(e.getName());
			log.info(e.getComment());
			log.info(Boolean.toString(e.isDirectory()));
			log.info(Long.toString(e.getCompressedSize()));
			log.info(Long.toString(e.getCrc()));
			log.info(Integer.toString(e.getMethod()));
			log.info(Long.toString(e.getSize()));
			log.info(Long.toString(e.getTime()));
		}
	}

}
