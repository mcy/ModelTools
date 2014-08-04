package com.xorinc.modeltools;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtil;
import org.apache.commons.lang3.ArrayUtils;

import com.google.common.base.Charsets;
import com.xorinc.modeltools.tools.Resize;
import com.xorinc.modeltools.tools.ResizeItem;
import com.xorinc.modeltools.tools.Tool;
import com.xorinc.modeltools.tools.Tool.Args;

import joptsimple.OptionException;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import joptsimple.ValueConversionException;


public class Main {

	private static boolean verbose;
	
	public static void main(String... args) throws Throwable{
		
		String[] pipe = IOUtil.toString(System.in, Charsets.UTF_8.name()).split("\n");
		
		args = ArrayUtils.addAll(args, pipe);
		
		OptionParser parser = new OptionParser();
		OptionSpec<Void> help = parser.acceptsAll(Arrays.asList("?", "help"), "Prints this message.").forHelp();
		OptionSpec<Void> verboseOpt = parser.acceptsAll(Arrays.asList("v", "verbose"), "Verbose output.");
		OptionSpec<String> suffix = parser.acceptsAll(Arrays.asList("s", "suffix"), "Suffix for output.").withRequiredArg();
		OptionSpec<File> output = parser.acceptsAll(Arrays.asList("o", "out", "output"), "Output file.").withRequiredArg().ofType(File.class);
		// TODO OptionSpec<?> recurse = parser.acceptsAll(Arrays.asList("r", "recursive"), "Applies the tool to all directories recursively.");
		OptionSpec<String> toolArg = parser.acceptsAll(Arrays.asList("t", "tool"), "The tool to use (required).").withRequiredArg();
		OptionSpec<String> toolOpt = parser.acceptsAll(Arrays.asList("opt", "toolOptions"), "Options for a tool.").withRequiredArg();
		OptionSpec<File> fileArg = parser.nonOptions("file").ofType(File.class);
		
		OptionSet options = null;
		
		try{
			options = parser.parse(args);
		} catch (OptionException ignore) {}
		
		if(options == null || options.has(help) || !options.has(toolArg)){
			
			try {
				parser.printHelpOn(System.out);
			}
			catch (IOException e) {
				System.err.println("Error printing help!");
				e.printStackTrace();
			}
			return;
		}
		
		for (File in : fileArg.values(options)){
			verbose(in);
			File out;
			File temp = File.createTempFile(".ModelToolstemp", "", in.getParentFile());
			
			if(options.has(suffix)){
				String su = suffix.value(options);
				String name = in.getName();
				String ext = FileUtils.getExtension(name);
				name = FileUtils.removeExtension(name);
				out = new File(in.getParent(), name + su + "." + ext);
			} else if(options.has(output)){
				out = output.value(options);
			} else {
				out = in;
			}
			
			verbose = options.has(verboseOpt);
			
			try(InputStream is = new FileInputStream(in); OutputStream os = new FileOutputStream(temp)){
			
				Tool t;
				
				switch(toolArg.value(options).toLowerCase()) {
				
				case "resize": t = Resize.inst; break;
				case "resizeitem": t = ResizeItem.inst; break;
				
				default: throw new ValueConversionException("Unknown tool '" + toolArg.value(options) + "'!");
				
				}
			
				if(t.getParser() != null && !options.has(toolOpt))
					throw new ValueConversionException("Tool requires option: " + Resize.inst.getParser().valuePattern());
							
				t.work(is, os, (Args) t.getParser().convert(toolOpt.value(options)));
				
				out.delete();
				out.createNewFile();
				FileUtils.copyFile(temp, out);
				
			} catch (ValueConversionException e) {
				
				System.err.println(e.getMessage());
			} catch (Exception e) {
				
				System.err.println("Error applying tool!");
				e.printStackTrace();
			}
		}
	}
	
	public static void verbose(Object o){
		
		if(verbose) System.out.println(o);
	}
}
