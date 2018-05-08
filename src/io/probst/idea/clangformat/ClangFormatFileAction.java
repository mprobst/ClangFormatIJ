package io.probst.idea.clangformat;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VirtualFile;

import java.util.Collection;
import java.util.Collections;

/**
 * Runs clang-format on the whole file, and applies the formatting updates to the editor.
 */
public class ClangFormatFileAction extends ClangFormatAction {
  @Override
  protected Collection<TextRange> getFormatRanges(Project project, Document document, Editor editor, VirtualFile virtualFile) {
    // IntelliJ reports a cursor at the end of the file as being at file length + 1, which breaks
    // clang-format.
    int docLength = document.getTextLength() - 1;

    return Collections.singletonList(new TextRange(0, docLength));
  }
}
