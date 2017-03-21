package init;

import java.io.File;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.io.FileUtils;

import resourcefixer.FileProcessor;
import resourcefixer.FileRenamer;
import resourcefixer.LanguageProcessor;
import util.DataUtils;

public class Main {
	
	private static final String COPYRIGHT = "ResourceFixer \u00A92017 UpcraftLP - all righs reserved.";
	
	private static final String[] USAGE = new String[]{
			"Usage:",
			"-p <path>    specify the working directory",
			"--help       show this help",
			"-h           same as --help",
			"-c           use the current directory. overridden if you use -p"
	};
	
	private static final String[] ALLOWED_EXTENSIONS = new String[]{"json", "png", "obj", "mcmeta"}; //all currently allowed extensions, lang files won't be renamed! not allowed
	private static final String[] CODE_FILES = new String[]{"json", "obj"};
	private static final String[] LANGUAGE_FILES = new String[]{"lang"};
	
	private static final Options options;
	
	private static File rootPath;
	public static volatile int fileCount = 0;
	public static volatile int threadCount = 0;
	
	public static final ExecutorService SERVICE = Executors.newCachedThreadPool();
	
	static {
		options = new Options();
		options.addOption("h", false, "show help");
		options.addOption("p", true, "path");
		options.addOption("c", true, "use the current directory");
	}
	
	public static void main(String[] args) {
		System.out.println(COPYRIGHT);
		if(args.length == 0) {
			displayUsage();
		}
		
		CommandLineParser parser = new DefaultParser();
		try {
			CommandLine cmd = parser.parse(options, args);
			if(cmd.hasOption("h") || cmd.hasOption("-help") || cmd.hasOption("?")) {
				displayUsage();
			}
			
			//determine the working directory
			String path = cmd.getOptionValue("p");
			if(path != null) {
				rootPath = new File(path);
				if(!rootPath.exists() || !rootPath.isDirectory()) {
					System.out.println("\"" + rootPath.getAbsolutePath() + "\" does not exist or is not a directory!");
					end(1);
				}
			}
			else {
				if(cmd.hasOption("c")) rootPath = new File(System.getProperty("user.dir"));
			}
			if(rootPath != null) System.out.println("working directory: " + rootPath.getAbsolutePath());
			else {
				System.out.println("no path specified!");
				displayUsage();
			}
			
			
		} catch (ParseException e) {
			displayUsage();
		}
		
		collectAndProcess(); //do all the magic

		end(0);
	}
	
	private static void displayUsage() {
		for(String s : USAGE) System.out.println(s);
		end(0);
	}

	public static void end(int exitCode) {
		end(exitCode, false);
	}
	
	public static void end(int exitCode, boolean hardExit) {
		
		//wait for all threads to terminate
		try {
			if(hardExit) SERVICE.shutdownNow();
			else {
				SERVICE.shutdown();
				while(!SERVICE.awaitTermination(1, TimeUnit.MINUTES));
			}
		} catch (InterruptedException ignore) {}
		
		//TODO: fancy debug messages.
		switch (exitCode) {
			case 0:
				System.out.println("program terminated without errors.");
				break;
			default:
				System.out.println("program terminated with exit code " + exitCode);
		}
		System.out.println("processed " + fileCount + " files in " + threadCount + " thread(s).");
		System.exit(exitCode);
	}
	
	public static void collectAndProcess() {
		File[] allFiles;
		List<File[]> filesToChange;
		
		allFiles = FileUtils.listFiles(rootPath, null, true).toArray(new File[0]);
		System.out.println("toal files: " + allFiles.length);
		
		//rename all files to lowercase
		allFiles = FileUtils.listFiles(rootPath, ALLOWED_EXTENSIONS, true).toArray(new File[0]);
		
		filesToChange = DataUtils.<File>divideArray(allFiles, 1024);
		for(File[] array : filesToChange) {
			SERVICE.execute(new FileRenamer(array));
		}
		
		//change the code files internally
		allFiles = FileUtils.listFiles(rootPath, CODE_FILES, true).toArray(new File[0]);
		filesToChange = DataUtils.<File>divideArray(allFiles, 1024);
		for(File[] array : filesToChange) {
			SERVICE.execute(new FileProcessor(array));
		} 
		
		//change the language files
		allFiles = FileUtils.listFiles(rootPath, LANGUAGE_FILES, true).toArray(new File[0]);
		synchronized (filesToChange) {
			
		}filesToChange = DataUtils.<File>divideArray(allFiles, 1024);
		for(File[] array : filesToChange) {
			SERVICE.execute(new LanguageProcessor(array));
		}
	}
	
}
