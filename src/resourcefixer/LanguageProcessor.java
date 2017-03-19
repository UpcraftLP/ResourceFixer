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

public class LanguageProcessor extends Thread {

	private final File[] toProcess;
	
	public LanguageProcessor(File[] array) {
		this.toProcess = array;
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
					content[index++] = iterator.next();
				}
				writer = new FileWriter(f);
				for(String s : content) {
					if(s.contains("=")) {
						String[] split = s.split("=");
						s = split[0].toLowerCase(Locale.ROOT); //only make the translation key lowercase
						s += "=";
						for(int i = 1; i < split.length; i++) {
							s += split[i];
						}
					}
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
