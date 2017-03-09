package io.probst.idea.clangformat;

import com.google.common.base.Splitter;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.JsonSerializer;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlValue;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Runs clang-format on the current statement or selection (if any), and applies the formatting
 * updates to the editor.
 */
public class ClangFormatAction extends AnAction {
  static final ExecutorService EXECUTOR = ForkJoinPool.commonPool();
  public static final boolean IS_MAC_OS = System.getProperty("os.name").contains("Mac OS X");

  @Override
  public void update(AnActionEvent e) {
    Project project = e.getData(CommonDataKeys.PROJECT);
    Editor editor = e.getData(CommonDataKeys.EDITOR);
    e.getPresentation().setVisible(project != null && editor != null);
  }

  @Override
  public void actionPerformed(AnActionEvent actionEvent) {
    Project project = actionEvent.getData(CommonDataKeys.PROJECT);
    Editor editor = actionEvent.getData(CommonDataKeys.EDITOR);
    if (editor == null)
      return; // can happen during startup.

    Document document = editor.getDocument();
    Caret caret = editor.getCaretModel().getPrimaryCaret();

    String filePath = actionEvent.getData(CommonDataKeys.VIRTUAL_FILE).getPath();
    // IntelliJ reports a cursor at the end of the file as being at file length + 1, which breaks
    // clang-format.
    int docLength = document.getTextLength() - 1;
    int cursor = Math.min(caret.getOffset(), docLength);
    int selectionStart = 0;
    int selectionLength = docLength;

    ProcessBuilder builder = null;
    Process formatter;
    try {
      builder = getCommand(filePath, cursor, selectionStart, selectionLength);
      formatter = builder.start();
    } catch (IOException | InterruptedException | ExecutionException e) {
      e.printStackTrace();
      String command = "";
      if (builder != null) {
        command = "command: " + builder.command() + " in PATH=" + builder.environment().get("PATH");
      }
      showError(project, "running clang-format failed - not installed?<br/>"
              + "Try running 'clang-format' in a shell, or configure its location in the preferences."
              + "<br/>" + e.getMessage() + "<br/>" + command);
      return;
    }

    final OutputStream outputStream = formatter.getOutputStream();
    Future<?> outWritten = EXECUTOR.submit(() -> writeFileContents(document, outputStream));
    final InputStream inputStream = formatter.getInputStream();
    final InputStream errorStream = formatter.getErrorStream();
    Future<String> errorMessage = EXECUTOR.submit(() -> readInput(errorStream));
    EXECUTOR.submit(() -> {
      try {
        try {
          outWritten.get(5, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
          showError(project, "timed out writing source file to clang-format");
          return;
        }
        if (!formatter.waitFor(5, TimeUnit.SECONDS)) {
          formatter.destroyForcibly();
          showError(project, "timed out waiting for clang-format to finish");
          return;
        }
        if (formatter.exitValue() != 0) {
          showError(project, "clang-format failed with exit code " + formatter.exitValue()
                  + ", error: " + errorMessage.get());
          return;
        }
        Scanner s = new Scanner(inputStream).useDelimiter("\\A");
        String clangResult = s.hasNext() ? s.next() : "";
        WriteCommandAction.runWriteCommandAction(project, () -> {
          String[] results = clangResult.split("\n", 2);
          JsonElement root = new JsonParser().parse(results[0]);
          int newCursorOffset = root.getAsJsonObject().get("Cursor").getAsInt();
          CharSequence formattedCode = results[1];
          document.replaceString(0, docLength, formattedCode);
          caret.moveToOffset(newCursorOffset);
        });
      } catch (InterruptedException e) {
        showError(project, e.getMessage());
      } catch (ExecutionException e) {
        showError(project, e.getCause().getMessage());
      }
    });

  }

  static Future<String> PATH_FUTURE = null;

  /**
   * Mac OS X does not set a usable PATH for UI applications. This code spawns a login shell once,
   * has it echo the actual PATH, and stores that way for future use (running a shell on each call
   * is much too expensive).
   */
  static synchronized Future<String> getLoginShellPath() {
    if (PATH_FUTURE == null) {
      PATH_FUTURE = EXECUTOR.submit(new Callable<String>() {
        @Override
        public String call() throws Exception {
          InputStream input = new ProcessBuilder()
                                  .command("bash", "-l", "-r", "-c", "echo $PATH")
                                  .redirectErrorStream(true)
                                  .start()
                                  .getInputStream();
          return readInput(input);
        }
      });
    }
    return PATH_FUTURE;
  }

  private ProcessBuilder getCommand(String filePath, int cursor, int selectionStart,
      int selectionLength) throws ExecutionException, InterruptedException {
    Settings settings = Settings.get();
    ProcessBuilder command = new ProcessBuilder().command(settings.clangFormatBinary, "-style=file", "-cursor=" + cursor);

    if (settings.path != null) {
      command.environment().put("PATH", settings.path);
    } else if (IS_MAC_OS) {
      String path = getLoginShellPath().get();
      command.environment().put("PATH", path);
    }
    if (IS_MAC_OS && "clang-format".equals(settings.clangFormatBinary)) {
      List<String> pathEntries =
          Splitter.on(File.pathSeparatorChar).splitToList(command.environment().get("PATH"));
      for (String p : pathEntries) {
        File candidate = new File(p, "clang-format");
        if (candidate.canExecute()) {
          command.command().set(0, candidate.getPath());
          break;
        }
      }
    }
    return command;
  }

  private void writeFileContents(Document document, OutputStream outputStream) {
    try (OutputStreamWriter out = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8)) {
      CharSequence contents = document.getImmutableCharSequence();
      out.append(contents);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private static String readInput(InputStream err) throws IOException {
    try (InputStreamReader errorStream = new InputStreamReader(err, StandardCharsets.UTF_8)) {
      StringBuilder sb = new StringBuilder();
      char[] buf = new char[4096];
      int read = 0;
      while ((read = errorStream.read(buf)) != -1) {
        sb.append(buf, 0, read);
      }
      return sb.toString();
    }
  }

  public static void showError(Project project, String errorMsg) {
    Notification notification =
        new Notification("ClangFormatIJ", "Formatting Failed", errorMsg, NotificationType.ERROR);
    Notifications.Bus.notify(notification, project);
  }
}
