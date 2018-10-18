/*
* This file is part of ResselChain.
* Copyright Center for Secure Energy Informatics 2018
* Fabian Knirsch, Andreas Unterweger, Clemens Brunner
* This code is licensed under a modified 3-Clause BSD License. See LICENSE file for details.
*/

package at.entrust.resselchain.logging;

import at.entrust.resselchain.config.GlobalConfig;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

public enum Logger {
	FULL, STD, ERR;
	
	private static PrintWriter stdFileStream;
	private static PrintWriter fullFileStream;
	private static PrintWriter errStream;
	
	
	static {
		try {
			File stdFile = new File("stdlog.txt");
			stdFile.createNewFile();
			File fullFile = new File("fulllog.txt");
			fullFile.createNewFile();
			stdFileStream = new PrintWriter(new FileOutputStream(stdFile, false), true);
			fullFileStream = new PrintWriter(new FileOutputStream(fullFile, false), true);
			errStream = new PrintWriter(System.err, true);
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1); // so severe that we can't even handle error outputs
		}
	}

	public void logImportant(String message){
		log("\t\t >> " + message);
	}
	
	public void log(String message) {

		if ( GlobalConfig.INSTANCE == null ||  GlobalConfig.INSTANCE.DEBUG )
			System.out.println(formatMessage(message));

		switch (this) {
			case ERR:
				writeFile(errStream, message);
			case STD:
				writeFile(stdFileStream, message);
			case FULL:
				writeFile(fullFileStream, message);
				break;
		}
	}
		
	private void writeFile(PrintWriter output, String message) {
		output.println(formatMessage(message));
	}

	private String formatMessage(String message){
		return new SimpleDateFormat("dd.MM.YYYY HH:mm:ss Z").format(new Date()) + ", " + System.currentTimeMillis() + " [" + Thread.currentThread().getName() + "]: " + message;
	}
}
