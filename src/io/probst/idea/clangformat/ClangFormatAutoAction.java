package io.probst.idea.clangformat;

import java.util.Arrays;
import java.util.List;

/**
 * Runs clang-format on the current selection or on the whole file if there is no selection.
 * Applies the formatting updates to the editor.
 */
public class ClangFormatAutoAction extends ClangFormatAction {
    /**
     * Builds a list of arguments to clang-format
     */
    @Override
    protected List<String> getCommandArguments(String clangFormatBinary, String filePath, int cursor, int selectionStart,
                                               int selectionLength) {
        if (selectionLength > 0) {
            // Format the selection as normal
            return super.getCommandArguments(clangFormatBinary, filePath, cursor, selectionStart, selectionLength);
        } else {
            // Format the whole file
            return Arrays.asList(clangFormatBinary, "-style=file",
                    "-output-replacements-xml", "-assume-filename=" + filePath, "-cursor=" + cursor);
        }
    }
}
