package init;

import java.io.File;
import java.time.Year;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionGroup;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.io.FileUtils;

import resourcefixer.FileProcessor;
import resourcefixer.FileRenamer;
import resourcefixer.LanguageProcessor;
import util.DataUtils;

public class Main {
	
	private static final String COPYRIGHT = "ResourceFixer Copyright (C)2017-" + Year.now().getValue() + " UpcraftLP - all righs reserved.";
	
	private static final String[] ALLOWED_EXTENSIONS = new String[]{"json", "png", "obj", "mcmeta", "ogg"}; //all currently allowed extensions, lang files won't be renamed! not allowed
	private static final String[] CODE_FILES = new String[]{"json", "obj"};
	private static final String[] LANGUAGE_FILES = new String[]{"lang"};
	
	private static final Options options;
	
	private static File rootPath;
	public static volatile int fileCount = 0;
	public static volatile int threadCount = 0;
	
	public static final ExecutorService SERVICE = Executors.newCachedThreadPool();
	
	static {
		options = new Options();
		
		final OptionGroup pathOptions = new OptionGroup();
		pathOptions.setRequired(true);
		pathOptions.addOption(Option.builder("p").longOpt("path").desc("specify the working directory").hasArg().required().build());
		pathOptions.addOption(Option.builder("c").longOpt("current").desc("use the current directory. overridden if you use -p").required().build());
		pathOptions.addOption(Option.builder("h").longOpt("help").desc("show this help").build());
		options.addOptionGroup(pathOptions);
	}
	
	public static void main(String[] args) {
		System.out.println(COPYRIGHT);
		
		CommandLineParser parser = new DefaultParser();
		try {
			if(args.length == 0) throw new ParseException(null);
			
			CommandLine cmd = parser.parse(options, args);
			if(cmd.hasOption("h") || cmd.hasOption("?")) throw new ParseException(null);
			
			//determine the working directory
			String path = null;
			if(cmd.hasOption("c")) path = System.getProperty("user.dir");
			if(cmd.hasOption("p")) path = cmd.getOptionValue("p");
			
			if(path != null && !path.isEmpty()) {
				rootPath = new File(path);
				if(!rootPath.exists() || !rootPath.isDirectory()) {
					throw new ParseException("\"" + rootPath.getAbsolutePath() + "\" does not exist or is not a directory!");
					
				}
				System.out.println("working directory: \"" + rootPath.getAbsolutePath() + "\"");
			}
			else throw new ParseException("no path specified!");
		} catch (ParseException e) {
			String msg = e.getMessage();
			if(msg != null) {
				System.out.println(msg);
				displayUsage(1);
			}
			else displayUsage(0);
		}
		
		collectAndProcess(); //do all the magic

		end(0);
	}
	
	private static void displayUsage(int exitCode) {
		HelpFormatter formatter = new HelpFormatter();
		formatter.printHelp("java -jar resourcefixer.jar -h", options);
		end(exitCode);
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
