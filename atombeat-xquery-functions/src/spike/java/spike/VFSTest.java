package spike;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;

import org.apache.commons.vfs.FileObject;
import org.apache.commons.vfs.FileSystemException;
import org.apache.commons.vfs.FileSystemManager;
import org.apache.commons.vfs.FileType;
import org.apache.commons.vfs.VFS;

public class VFSTest {

	static Logger log = Logger.getLogger(VFSTest.class.getCanonicalName());

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		FileSystemManager fsManager = VFS.getManager(); 
		FileObject f = fsManager.resolveFile(args[0]);
		List<String> collect = new ArrayList<String>();
		search(f, collect);
		for (String s : collect) {
			log.info(s);
			String sf = args[0] + "!" + s;
			FileObject sff = fsManager.resolveFile(sf);
			log.info(Boolean.toString(sff.exists()));
			log.info(sff.getName().getPath());
			log.info(sff.getType().toString());
			if (sff.getType().equals(FileType.FILE)) { 
				log.info(Long.toString(sff.getContent().getSize()));
			}
		}
	}
	
	public static void search(FileObject f, Collection<String> collect) throws FileSystemException {
		log.info(f.getName().getBaseName());
		log.info(f.getName().getPath());
		collect.add(f.getName().getPath());
		log.info(f.getType().toString());
		if (f.getType().equals(FileType.FOLDER)) {
			FileObject[] entries = f.getChildren();
			log.info(Integer.toString(entries.length));
			for (int i=0; i<entries.length; i++) {
				FileObject e = entries[i];
				search(e, collect);
			}		
		}
	}
	
}