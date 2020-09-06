package gabor.var.history.debug.executor;

import com.intellij.execution.Executor;
import com.intellij.openapi.wm.ToolWindowId;
import gabor.var.history.ResourcesPlugin;
import gabor.var.history.debug.DebugResources;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class CustomHistoryDebuggerExecutor extends Executor {

    @NotNull
    @Override
    public String getToolWindowId() {
        return ToolWindowId.DEBUG;
    }

    @NotNull
    @Override
    public Icon getToolWindowIcon() {
        return ResourcesPlugin.DEBUG;
    }

    @Override
    @NotNull
    public Icon getIcon() {
        return ResourcesPlugin.DEBUG;
    }

    @Override
    public Icon getDisabledIcon() {
        return null;
    }

    @Override
    public String getDescription() {
        return DebugResources.WITH_HISTORY_DEBUGGER;
    }

    @Override
    @NotNull
    public String getActionName() {
        return DebugResources.WITH_HISTORY_DEBUGGER;
    }

    @Override
    @NotNull
    public String getId() {
        return ResourcesPlugin.DEBUGGER_NAME;
    }


    @Override
    @NotNull
    public String getStartActionText() {
        return DebugResources.WITH_HISTORY_DEBUGGER;
    }

    @Override
    @NotNull
    public String getStartActionText(@NotNull String configurationName) {
        return DebugResources.WITH_HISTORY_DEBUGGER;
    }

    @Override
    public String getContextActionId() {
        return ResourcesPlugin.DEBUGGER_ACTION;
    }

    @Override
    public String getHelpId() {
        return null;
    }

}
