package resourcefixer;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Locale;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.LineIterator;

import init.Main;

public class FileProcessor extends Thread {

	private final File[] toProcess;
	
	public FileProcessor(File[] toProcess) {
		this.toProcess = toProcess;
		Main.threadCount++;
	}
	
	@Override
	public void run() {
		for(File f : toProcess) {
			if(f == null) continue;
			Main.fileCount++;
			LineIterator iterator = null;
			FileWriter writer = null;
			try {
				String[] content = new String[0];
				iterator = FileUtils.lineIterator(f);
				int index = 0;
				while(iterator.hasNext()) {
					content = Arrays.copyOf(content, content.length + 1);
					content[index++] = iterator.next().toLowerCase(Locale.ROOT);
				}
				writer = new FileWriter(f);
				for(String s : content) {
					writer.write(s);
					writer.write(System.lineSeparator());
				}
			} catch (IOException ignore) {}
			finally {
				LineIterator.closeQuietly(iterator);
				IOUtils.closeQuietly(writer);
			}
		}
		this.interrupt();
	}
}
