package net.noha.tools.ant.yuicompressor.tasks;

import java.io.*;
import java.nio.charset.UnsupportedCharsetException;
import java.util.ArrayList;
import java.util.List;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.FileSet;
import org.mozilla.javascript.ErrorReporter;
import org.mozilla.javascript.EvaluatorException;

import com.yahoo.platform.yui.compressor.CssCompressor;
import com.yahoo.platform.yui.compressor.JavaScriptCompressor;

/**
 * 
 * Same as {@link YuiCompressorTask} but accepts &lt;fileset&gt;s inside
 * 
 * @author the-dan
 *
 */
public class YuiCompressorFilesetTask extends Task {

	protected File toDir;

	// properties with default values
	protected String charset = "ISO-8859-1";
	protected int lineBreakPosition = -1;
	protected boolean munge = false;
	protected boolean warn = true;
	protected boolean preserveAllSemiColons = true;
	protected boolean optimize = true;
	private List<FileSet> filesets = new ArrayList<FileSet>();
	private boolean doOverwrite;
	
	// suffixes
	protected String jsSuffix = "-min.js";
	protected String cssSuffix = "-min.css";

	// stats
	private CompressionStatistics stats = new CompressionStatistics();

	public void execute() {
		validateDirs();

		for (FileSet fs : filesets) { // 2
			DirectoryScanner ds = fs.getDirectoryScanner(getProject()); // 3
			
			String[] includedFiles = ds.getIncludedFiles();
			for (int i = 0; i < includedFiles.length; i++) {
				File inFile = new File(fs.getDir().getAbsolutePath(),
						includedFiles[i]);
				FileType fileType = FileType.getFileType(includedFiles[i]);
				if (fileType == null) {
					continue;
				}

				String newSuffix = (fileType.equals(FileType.JS_FILE)) ? jsSuffix
						: cssSuffix;
				File outFile = new File(toDir.getAbsolutePath(),
						includedFiles[i].replaceFirst(fileType.getSuffix()
								+ "$", newSuffix));
				compressFile(inFile, outFile, fileType);
			}
		}

		log(stats.getJsStats());
		log(stats.getCssStats());
		log(stats.getTotalStats());
	}

	private void compressFile(File inFile, File outFile, FileType fileType)
			throws EvaluatorException, BuildException {
		// do not recompress when outFile is newer
		// always recompress when outFile and inFile are exactly the same file
		if (outFile.isFile()
				&& !inFile.getAbsolutePath().equals(outFile.getAbsolutePath())) {
			if (!doOverwrite && outFile.lastModified() >= inFile.lastModified()) {
				return;
			}
		}

		try {

			// prepare input file
			Reader in = openFile(inFile);

			// prepare output file
			outFile.getParentFile().mkdirs();
			Writer out = new OutputStreamWriter(new FileOutputStream(outFile),
					charset);

			if (fileType.equals(FileType.JS_FILE)) {
				JavaScriptCompressor compressor = createJavaScriptCompressor(in);
				compressor.compress(out, lineBreakPosition, munge, warn,
						preserveAllSemiColons, !optimize);
			} else if (fileType.equals(FileType.CSS_FILE)) {
				CssCompressor compressor = new CssCompressor(in);
				compressor.compress(out, lineBreakPosition);
			}

			// close all streams
			in.close();
			in = null;
			out.close();
			out = null;

			log(stats.getFileStats(inFile, outFile, fileType));
		} catch (IOException ioe) {
			throw new BuildException("I/O Error when compressing file", ioe);
		}
	}

	private JavaScriptCompressor createJavaScriptCompressor(Reader in)
			throws IOException {
		JavaScriptCompressor compressor = new JavaScriptCompressor(in,
				new ErrorReporter() {

					private String getMessage(	String source,
												String message,
												int line,
												int lineOffset) {
						String logMessage;
						if (line < 0) {
							logMessage = (source != null) ? source + ":" : ""
									+ message;
						} else {
							logMessage = (source != null) ? source + ":" : ""
									+ line + ":" + lineOffset + ":" + message;
						}
						return logMessage;
					}

					public void warning(String message,
										String sourceName,
										int line,
										String lineSource,
										int lineOffset) {
						log(getMessage(sourceName, message, line, lineOffset),
								Project.MSG_WARN);
					}

					public void error(	String message,
										String sourceName,
										int line,
										String lineSource,
										int lineOffset) {
						log(getMessage(sourceName, message, line, lineOffset),
								Project.MSG_ERR);

					}

					public EvaluatorException runtimeError(	String message,
															String sourceName,
															int line,
															String lineSource,
															int lineOffset) {
						log(getMessage(sourceName, message, line, lineOffset),
								Project.MSG_ERR);
						return new EvaluatorException(message);
					}
				});
		return compressor;
	}

	private Reader openFile(File file) throws BuildException {
		Reader in;
		try {
			in = new InputStreamReader(new FileInputStream(file), charset);
		} catch (UnsupportedCharsetException uche) {
			throw new BuildException("Unsupported charset name: " + charset,
					uche);
		} catch (IOException ioe) {
			throw new BuildException("I/O Error when reading input file", ioe);
		}
		return in;
	}

	private void validateDirs() throws BuildException {
		if (!toDir.isDirectory())
			throw new BuildException(toDir + " is not a valid directory");
	}

	public void addFileset(FileSet fileset) {
		filesets.add(fileset);
	}

	public void setToDir(File toDir) {
		this.toDir = toDir;
	}

	public void setCharset(String charset) {
		this.charset = charset;
	}

	public void setLineBreakPosition(int lineBreakPosition) {
		this.lineBreakPosition = lineBreakPosition;
	}

	public void setMunge(boolean munge) {
		this.munge = munge;
	}

	public void setWarn(boolean warn) {
		this.warn = warn;
	}

	public void setPreserveAllSemiColons(boolean preserveAllSemiColons) {
		this.preserveAllSemiColons = preserveAllSemiColons;
	}

	public void setOptimize(boolean optimize) {
		this.optimize = optimize;
	}

	public void setJsSuffix(String jsSuffix) {
		this.jsSuffix = jsSuffix;
	}

	public void setCssSuffix(String cssSuffix) {
		this.cssSuffix = cssSuffix;
	}
	
	public void setOverwrite(boolean doOverwrite) {
		this.doOverwrite = doOverwrite;
	}
}
