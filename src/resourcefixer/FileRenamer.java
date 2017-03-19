package resourcefixer;

import java.io.File;
import java.util.Locale;

import init.Main;

public class FileRenamer extends Thread {

	private File[] toRename;
	
	public FileRenamer(File[] array) {
		toRename = array;
		Main.threadCount++;
	}
	
	@Override
	public void run() {
		for(File f : toRename) {
			if(f == null) continue;
			String lowerCase = f.getName().toLowerCase(Locale.ROOT);
			if(!f.getName().equals(lowerCase)) {
				f.renameTo(new File(f.getParent(), lowerCase));
				Main.fileCount++;
			}
		}
		this.interrupt();
	}

}
