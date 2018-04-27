package io.probst.idea.clangformat;

import java.util.Arrays;
import java.util.List;

/**
 * Runs clang-format on the whole file, and applies the formatting updates to the editor.
 */
public class ClangFormatFileAction extends ClangFormatAction {
  /**
   * Builds a list of arguments to clang-format
   *
   * To format the whole file, we ignore the selection arguments.
   */
  @Override
  protected List<String> getCommandArguments(String clangFormatBinary, String filePath, int cursor, int selectionStart,
                                             int selectionLength) {
    return Arrays.asList(clangFormatBinary, "-style=file",
            "-output-replacements-xml", "-assume-filename=" + filePath, "-cursor=" + cursor);
  }
}
