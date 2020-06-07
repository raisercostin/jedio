package org.jedio;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import com.google.common.base.Joiner;
import io.vavr.control.Try;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.io.Charsets;
import org.raisercostin.jedio.DirLocation;

@Data
@Getter(value = AccessLevel.NONE)
@Setter(value = AccessLevel.NONE)
public class ProcessResult {
  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(ProcessResult.class);
  private static final int EOF = -1;
  private static final int DEFAULT_BUFFER_SIZE = 1024 * 4;
  public final String command;
  public final String output;
  public final String error;
  public final boolean finished;
  private Process proc;

  public ProcessResult(DirLocation current, List<String> commandAndParams, Pattern sensibleRegex, Process proc) {
    try {
      String command = blurMessage(sensibleRegex, Joiner.on(" ").join(commandAndParams));
      boolean finished = proc.waitFor(10, TimeUnit.SECONDS);
      if (!finished) {
        proc.destroy();
      }
      int exitValue = 0;
      // BufferedReader err = new BufferedReader(new
      // InputStreamReader(proc.getErrorStream()));
      // System.out.println(toString(err));
      String error = blurMessage(sensibleRegex, toString(proc.getErrorStream(), "UTF-8").trim());
      String output = blurMessage(sensibleRegex, toString(proc.getInputStream(), "UTF-8").trim());
      message(current, exitValue, command, output, error);
      this.command = command;
      this.output = output;
      this.error = error;
      this.finished = finished;
      this.proc = proc;
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  public Try<ProcessResult> get() {
    return Try.ofCallable(() -> {
      if (finished) {
        int exitValue = proc.exitValue();
        if (exitValue != 0) {
          if (!error.isEmpty()) {
            throw new RuntimeException("Couldn't execute [" + command + "] errorCode=" + exitValue + "\nOutput:["
                + output + "]\n Error:[" + error + "]");
          }
        }
      } else {
        throw new RuntimeException("Timeout. Couldn't execute [" + command + "] errorCode=none\nOutput:[" + output
            + "]\n Error:[" + error + "]");
      }
      return this;
    });
  }

  public ProcessResult valid() {
    return get().get();
  }

  private void message(DirLocation current, int exitValue, String command, String output, String error) {
    if (exitValue == 0 && output.isEmpty() && error.isEmpty()) {
      logger.info(current.toString() + " > [" + command + "]");
    } else {
      logger.info(current.toString() + " > " + command + "\nExitValue: " + exitValue + "\nOutput:[" + print(output)
          + "]\nError:[" + print(error) + "]\n");
    }
  }

  private String print(String message) {
    if (message.isEmpty()) {
      return "";
    }
    if (message.contains("\n")) {
      return "\n" + message;
    }
    return message;
  }

  private String blurMessage(Pattern sensibleRegex, String message) {
    if (sensibleRegex != null) {
      return sensibleRegex.matcher(message).replaceAll("***");
    } else {
      return message;
    }
  }

  private static String toString(final InputStream input, final String encoding) {
    return toString(input, Charsets.toCharset(encoding));
  }

  private static String toString(final InputStream input, final Charset encoding) {
    try (final ByteArrayOutputStream sw = new ByteArrayOutputStream()) {
      copy(input, sw);
      return sw.toString(encoding.name());
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private static void copy(final InputStream input, final OutputStream output) {
    // final InputStreamReader in = new InputStreamReader(input,
    // Charsets.toCharset(inputEncoding));
    copyLarge1(input, output, new byte[DEFAULT_BUFFER_SIZE]);
  }

  private static long copyLarge1(InputStream input, OutputStream output, byte[] buffer) {
    try {
      long count = 0;
      int n;
      while (EOF != (n = input.available()) && 0 != n) {
        int n2 = input.read(buffer);
        output.write(buffer, 0, n2);
        count += n2;
      }
      return count;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
  //
  // private static String toString(final Reader input) throws IOException {
  // try (final StringBuilderWriter sw = new StringBuilderWriter()) {
  // copy(input, sw);
  // return sw.toString();
  // }
  // }

  // private static int copy(final Reader input, final Writer output) throws IOException {
  // final long count = copyLarge(input, output);
  // if (count > Integer.MAX_VALUE) {
  // return -1;
  // }
  // return (int) count;
  // }
  //
  //
  //
  // private static long copyLarge(final Reader input, final Writer output) throws IOException {
  // return copyLarge(input, output, new char[DEFAULT_BUFFER_SIZE]);
  // }
  //
  // private static long copyLarge(final Reader input, final Writer output, final char[] buffer) throws IOException {
  // long count = 0;
  // int n;
  // while (EOF != (n = input.read(buffer))) {
  // output.write(buffer, 0, n);
  // count += n;
  // }
  // return count;
  // }
}
