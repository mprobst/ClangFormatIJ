package io.probst.idea.clangformat;

import com.intellij.codeInsight.actions.FormatChangedTextUtil;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.codeStyle.ChangedRangesInfo;
import com.intellij.util.diff.FilesTooBigForDiffException;

import java.util.Collection;
import java.util.Collections;

/**
 * Runs clang-format on the current selection or on the whole file if there is no selection.
 * Applies the formatting updates to the editor.
 */
public class ClangFormatAutoAction extends ClangFormatAction {

    private Collection<TextRange> getVcsTextRanges(Project project, VirtualFile virtualFile) {
        final PsiManager myPsiManager = PsiManager.getInstance(project);
        PsiFile psiFile = myPsiManager.findFile(virtualFile);
        try {
            FormatChangedTextUtil textUtil = FormatChangedTextUtil.getInstance();

            // Check if the file is under VCS. If it is not then we just use the standard code
            if (!textUtil.isChangeNotTrackedForFile(project, psiFile)) {
                ChangedRangesInfo changedRangesInfo = textUtil.getChangedRangesInfo(psiFile);

                // null can be returned if there are no changes
                if (changedRangesInfo != null) {
                    return changedRangesInfo.allChangedRanges;
                }
                return Collections.emptyList();
            }
        } catch (FilesTooBigForDiffException e) {
            // Ignore and let the standard case handle this. If a file is too big to diff then a full reformat should
            // probably be fine
        }
        return null;
    }

    @Override
    protected Collection<TextRange> getFormatRanges(Project project,
                                                    Document document,
                                                    Editor editor,
                                                    VirtualFile virtualFile) {
        Settings settings = Settings.get();

        // IntelliJ reports a cursor at the end of the file as being at file length + 1, which breaks
        // clang-format.
        int docLength = document.getTextLength() - 1;
        int selectionStart = Math.min(editor.getSelectionModel().getSelectionStart(), docLength);
        int selectionLength = Math.min(editor.getSelectionModel().getSelectionEnd(), docLength) - selectionStart;

        // Formatting the VCS changed text will only be done if nothing is selected. Otherwise you can't force a reformat
        // of some code that was not changed from the VCS version
        if (selectionLength <= 0 && settings.updateOnlyChangedText) {
            // This case only formats the text changed in the VCS. If the diff is too large or we are not in a VCS file
            // then the standard behavior will be used (selection or whole file)
            Collection<TextRange> vcsRanges = getVcsTextRanges(project, virtualFile);

            // getVcsTextRanges returns null if it could return any valid ranges in which case we
            // will use the non-VCS branch
            if (vcsRanges != null) {
                // The VCS has returned some ranges to format
                return vcsRanges;
            }
        }

        if (selectionLength <= 0) {
            return Collections.singletonList(new TextRange(0, docLength));
        } else {
            return Collections.singletonList(new TextRange(selectionStart, selectionStart + selectionLength));
        }
    }
}
