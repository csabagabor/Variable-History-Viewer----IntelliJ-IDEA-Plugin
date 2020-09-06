package gabor.var.history.debug;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiManager;
import com.intellij.psi.util.ClassUtil;
import com.intellij.xdebugger.impl.XSourcePositionImpl;
import com.intellij.xdebugger.impl.ui.ExecutionPointHighlighter;
import gabor.var.history.debug.type.StackFrame;
import gabor.var.history.debug.view.DebugStackFrameListener;
import gabor.var.history.debug.view.StackFrameManager;
import gabor.var.history.helper.LoggingHelper;
import org.jetbrains.annotations.NotNull;

public class ExecutionPointManager implements DebugStackFrameListener, Disposable {
    private final Project project;
    private final ExecutionPointHighlighter executionPointHighlighter;

    public ExecutionPointManager(Project project, @NotNull ExecutionPointHighlighter executionPointHighlighter) {
        this.project = project;
        this.executionPointHighlighter = executionPointHighlighter;
    }

    @Override
    public void onChanged(@NotNull StackFrameManager stackFrameManager) {
        StackFrame currentFrame = stackFrameManager.getCurrentFrame();

        String fromClass = currentFrame.getFullClassName();

        //check for inner class
        int indexOfDollarSign = fromClass.indexOf("$");
        if (indexOfDollarSign >= 0) {
            fromClass = fromClass.substring(0, indexOfDollarSign);
        }


        PsiClass containingClass = ClassUtil.findPsiClass(PsiManager.getInstance(project), fromClass);

        if (containingClass == null) {
            return;
        }

        VirtualFile virtualFile = containingClass.getContainingFile().getVirtualFile();
        if (virtualFile == null) {
            return;
        }

        Document document = FileDocumentManager.getInstance().getDocument(virtualFile);

        if (document == null) {
            return;
        }

        int lineStartOffset = document.getLineStartOffset(currentFrame.getLine());
        try {
            FileEditorManager.getInstance(project).openTextEditor(new OpenFileDescriptor(project,
                    virtualFile, lineStartOffset), true);

            executionPointHighlighter.hide();
            executionPointHighlighter.show(XSourcePositionImpl.createByOffset(virtualFile, lineStartOffset),
                    false, null);
        } catch (Throwable e) {
            LoggingHelper.error(e);
        }
    }

    @Override
    public void dispose() {
        executionPointHighlighter.hide();
    }
}
