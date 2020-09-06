
package gabor.var.history.debug.action;

import com.intellij.debugger.DebuggerManager;
import com.intellij.debugger.engine.DebugProcessImpl;
import com.intellij.debugger.engine.JavaValue;
import com.intellij.debugger.engine.managerThread.DebuggerManagerThread;
import com.intellij.debugger.ui.impl.watch.ValueDescriptorImpl;
import com.intellij.execution.Executor;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.Project;
import com.intellij.xdebugger.XDebugSession;
import com.intellij.xdebugger.XDebuggerManager;
import com.intellij.xdebugger.frame.XValue;
import com.intellij.xdebugger.impl.ui.tree.actions.XDebuggerTreeActionBase;
import com.intellij.xdebugger.impl.ui.tree.nodes.XValueNodeImpl;
import com.sun.jdi.ObjectReference;
import com.sun.jdi.Value;
import gabor.var.history.action.CoverageContext;
import gabor.var.history.debug.DebugExtractor;
import gabor.var.history.debug.executor.CustomHistoryDebuggerExecutor;
import gabor.var.history.helper.LoggingHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.tree.TreeNode;

public class GetVariableHistoryAction extends AnAction {

    @Override
    public void update(@NotNull AnActionEvent e) {
        XDebugSession session = getCurrentSession(e);

        if (session == null) {
            return;
        }

        ExecutionEnvironment data = e.getData(LangDataKeys.EXECUTION_ENVIRONMENT);
        Executor executor = data.getExecutor();
        if(!(executor instanceof CustomHistoryDebuggerExecutor)){
            return;
        }

        Presentation presentation = e.getPresentation();

        XValueNodeImpl selectedNode = XDebuggerTreeActionBase.getSelectedNode(e.getDataContext());

        if (selectedNode == null) {
            return;
        }

        boolean enabled = false;

        TreeNode nodeParent = selectedNode.getParent();
        try {
            if (nodeParent instanceof XValueNodeImpl) {
                XValueNodeImpl parent = (XValueNodeImpl) nodeParent;

                XValue xValue = parent.getValueContainer();
                if (xValue instanceof JavaValue) {
                    JavaValue valueContainer = (JavaValue) xValue;
                    ValueDescriptorImpl descriptor = valueContainer.getDescriptor();

                    CoverageContext context = CoverageContext.getInstance(e.getProject());

                    Value value = descriptor.getValue();
                    enabled = value instanceof ObjectReference && context.getPatterns().containsKey(((ObjectReference) value).referenceType().name());
                }
            }
        } catch (Throwable e2) {
        }

        presentation.setEnabledAndVisible(enabled);
        super.update(e);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        XDebugSession session = getCurrentSession(e);

        if (session == null) {
            return;
        }

        try {
            XValueNodeImpl selectedNode = XDebuggerTreeActionBase.getSelectedNode(e.getDataContext());

            if (selectedNode == null) {
                return;
            }

            XValueNodeImpl parent = (XValueNodeImpl) selectedNode.getParent();
            JavaValue valueContainer = (JavaValue) parent.getValueContainer();
            ValueDescriptorImpl descriptor = valueContainer.getDescriptor();

            Value value = descriptor.getValue();
            ProcessHandler processHandler = session.getDebugProcess().getProcessHandler();
            DebugProcessImpl debugProcess = (DebugProcessImpl) DebuggerManager.getInstance(e.getProject()).getDebugProcess(processHandler);
            DebuggerManagerThread managerThread = debugProcess.getManagerThread();

            CoverageContext context = CoverageContext.getInstance(e.getProject());

            if (value instanceof ObjectReference) {
                managerThread.invokeCommand(new DebugExtractor(debugProcess, (ObjectReference) value, selectedNode.getName(), context.getPatterns()));
            }
        } catch (Throwable e2) {
            LoggingHelper.error(e2);
        }
    }

    @Nullable
    private static XDebugSession getCurrentSession(@NotNull AnActionEvent e) {
        final Project project = e.getProject();
        return project == null ? null : XDebuggerManager.getInstance(project).getCurrentSession();
    }
}
